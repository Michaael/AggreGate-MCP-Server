package com.tibbo.aggregate.client;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

import javax.swing.*;

import org.jmock.*;
import org.jmock.integration.junit4.*;
import org.jmock.lib.legacy.*;
import org.junit.*;
import org.junit.runner.*;

import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.expression.*;
import com.tibbo.aggregate.common.server.*;
import com.tibbo.aggregate.common.tests.*;
import com.tibbo.aggregate.common.widget.*;
import com.tibbo.aggregate.common.widget.component.*;
import com.tibbo.aggregate.common.widget.engine.*;
import com.tibbo.aggregate.common.widget.runtime.*;
import com.tibbo.aggregate.common.widget.runtime.renderers.*;

@RunWith(JMock.class)
public class TestWSubWidget extends BaseWidgetTest<WSubWidget>
{
  public static final String SOME_PROPERTY = "someProperty";
  public static final String PATH_TO_SUB_WIDGET = "some.path.to.widget";
  
  private Mockery mockery;
  private DefaultContextManager contextManager;
  
  @Test
  public void testRendererAndSupport()
  {
    final AbstractSwingRenderer componentRenderer = (AbstractSwingRenderer) engine.getViewer().getComponentRenderer(getComponentName());
    
    assertThat(componentRenderer, is(instanceOf(SubWidgetSwingRenderer.class)));
    assertThat(componentRenderer.getRendererSupport(), is(instanceOf(SwingRendererSupport.class)));
  }
  
  @Test
  public void testHasReferenceVariable()
  {
    assertThat(getComponentContext().getVariableDefinition(WSubWidget.V_REFERENCE), is(notNullValue()));
  }
  
  @Test
  public void testSubWidgetLoadsWidget() throws Exception
  {
    mockEmbeddableWidgetContext();
    
    getComponentContext().setVariable(WSubWidget.V_REFERENCE, PATH_TO_SUB_WIDGET);
    
    final Condition cond = new Condition()
    {
      @Override
      public boolean check()
      {
        final JComponent mainPanel = (JComponent) engine.getViewer().getComponentRenderer(getComponentName()).getRender();
        
        if (mainPanel.getComponentCount() == 0)
          return false;
        
        final JComponent subWidgetPanel = (JComponent) mainPanel.getComponent(0);
        return subWidgetPanel.getComponents().length > 0;
      }
    };
    assertThat("Subwidget was not loaded", AggreGateTestingUtils.wait(2000, 200, cond), is(Boolean.TRUE));
    
    final JComponent mainPanel = (JComponent) engine.getViewer().getComponentRenderer(getComponentName()).getRender();
    final JComponent subWidgetPanel = (JComponent) mainPanel.getComponent(0);
    
    assertThat("First subwidget expected to be a Label", subWidgetPanel.getComponent(0), is(instanceOf(JLabel.class)));
  }
  
  @Test
  public void testSubWidgetLoadsInnerProperties() throws Exception
  {
    mockEmbeddableWidgetContext();
    
    getComponentContext().setVariable(WSubWidget.V_REFERENCE, PATH_TO_SUB_WIDGET);
    
    final Condition cond = new Condition()
    {
      @Override
      public boolean check()
      {
        return getComponentContext().getVariableDefinition(SOME_PROPERTY) != null;
      }
    };
    assertThat(AggreGateTestingUtils.wait(2000, 200, cond), is(Boolean.TRUE));
  }
  
  @Test
  public void testChangingSubWidgetCustomPropertyChangesInnerWidgetProperty() throws Exception
  {
    mockEmbeddableWidgetContext();
    
    loadSubwidget();
    
    final SubWidgetSwingRenderer componentRenderer = (SubWidgetSwingRenderer) engine.getViewer().getComponentRenderer(getComponentName());
    
    final WidgetEngine embeddedEngine = componentRenderer.getEmbeddedEngine();
    DataTable customProperty = embeddedEngine.getWidget().getRootPanel().getCustomProperty(SOME_PROPERTY);
    
    assertThat(customProperty.rec().getString("name"), is(not("newValue")));
    
    getComponentContext().setVariable(SOME_PROPERTY, "newValue");
    
    customProperty = embeddedEngine.getWidget().getRootPanel().getCustomProperty(SOME_PROPERTY);
    assertThat(customProperty.rec().getString("name"), is("newValue"));
  }
  
  @Test
  public void testChangesInSubwidgetCustomPropertyIsVisibleInEnclosedWidget() throws Exception
  {
    mockEmbeddableWidgetContext();
    
    loadSubwidget();
    
    DataTable customProperty = getComponentContext().getVariable(SOME_PROPERTY);
    
    assertThat(customProperty.rec().getString("name"), is(not("newValue")));
    
    final SubWidgetSwingRenderer componentRenderer = (SubWidgetSwingRenderer) engine.getViewer().getComponentRenderer(getComponentName());
    
    final WidgetEngine embeddedEngine = componentRenderer.getEmbeddedEngine();
    
    embeddedEngine.getViewer().getRootContext().setVariable(SOME_PROPERTY, "newValue");
    
    customProperty = getComponentContext().getVariable(SOME_PROPERTY);
    
    assertThat(customProperty.rec().getString("name"), is("newValue"));
  }
  
  @Test
  @Ignore("It works but we should omit tests with thread sleeps")
  public void testBindingHearsChangesInSubwidget() throws Exception
  {
    mockEmbeddableWidgetContext();
    
    loadSubwidget();
    
    DataTable customProperty = engine.getViewer().getRootContext().getVariable(WAbstractComponent.V_TOOLTIP);
    
    assertThat(customProperty.rec().getString(0), is(not("newValue")));
    
    Thread.sleep(1000);
    
    AggreGateTestingUtils.wait(10000, 200, new Condition()
    {
      @Override
      public boolean check() throws Exception
      {
        final SubWidgetSwingRenderer componentRenderer = (SubWidgetSwingRenderer) engine.getViewer().getComponentRenderer(getComponentName());
        componentRenderer.getEmbeddedEngine().getViewer().getRootContext().setVariable(SOME_PROPERTY, "newValue");
        
        DataTable customProperty = engine.getViewer().getRootContext().getVariable(WAbstractComponent.V_TOOLTIP);
        
        return "newValue".equals(customProperty.rec().getString(0));
      }
    });
    
    customProperty = engine.getViewer().getRootContext().getVariable(WAbstractComponent.V_TOOLTIP);
    
    assertThat("Binding should set 'tooltip' value to the 'someProperty' value", customProperty.rec().getString(0), is("newValue"));
  }
  
  @Override
  public void setUp() throws Exception
  {
    super.setUp();
    mockery = new JUnit4Mockery()
    {
      {
        setImposteriser(ClassImposteriser.INSTANCE);
      }
    };
    
    final Reference customPropertyRef = new Reference(getComponentName(), SOME_PROPERTY, ContextUtils.ENTITY_VARIABLE);
    customPropertyRef.setSchema(Reference.SCHEMA_FORM);
    
    final Reference tooltipRef = new Reference("", WAbstractComponent.V_TOOLTIP, ContextUtils.ENTITY_VARIABLE);
    tooltipRef.setSchema(Reference.SCHEMA_FORM);
    
    widgetTemplate.addBinding(tooltipRef.getImage(), new Expression(customPropertyRef).getText(), false, true);
    
    startEngine();
  }
  
  @Override
  protected DefaultContextManager<Context> createContextManager()
  {
    final CallerController controller = new UncheckedCallerController();
    contextManager = mockery.mock(DefaultContextManager.class);
    
    mockery.checking(new Expectations()
    {
      {
        allowing(contextManager).setRoot(with(any(Context.class)));
        allowing(contextManager).getRoot();
        
        allowing(contextManager).get(null, null);
        will(returnValue(null));
        
        allowing(contextManager).get(".", null);
        will(returnValue(any(Context.class)));
        
        allowing(contextManager).getCallerController();
        will(returnValue(controller));
      }
    });
    
    return contextManager;
  }
  
  @Override
  protected Class<WSubWidget> componentClass()
  {
    return WSubWidget.class;
  }
  
  private void mockEmbeddableWidgetContext() throws ContextException
  {
    final String widgetString = createWidgetAsXML();
    
    final AbstractContext subWidgetContext = mockery.mock(AbstractContext.class);
    
    final DataRecord childInfo = new DataRecord(FieldFormat.create(WidgetContextConstants.FIELD_WIDGET_TEMPLATE, 'S').wrap());
    childInfo.setValue(WidgetContextConstants.FIELD_WIDGET_TEMPLATE, widgetString);
    
    final DataRecord template = new DataRecord(FieldFormat.create(WidgetContextConstants.F_VALUE, 'S').wrap());
    template.setValue(WidgetContextConstants.F_VALUE, widgetString);
    
    mockery.checking(new Expectations()
    {
      {
        allowing(contextManager).get(PATH_TO_SUB_WIDGET, null);
        will(returnValue(subWidgetContext));
        
        allowing(subWidgetContext).getVariable(EditableChildContextConstants.V_CHILD_INFO, null);
        will(returnValue(childInfo.wrap()));
        
        allowing(subWidgetContext).getName();
        will(returnValue(getComponentName()));
        
        allowing(subWidgetContext).getPath();
        will(returnValue(PATH_TO_SUB_WIDGET));
        
        allowing(subWidgetContext).getVariable(WidgetContextConstants.V_TEMPLATE, null);
        will(returnValue(template.wrap()));
      }
    });
  }
  
  private String createWidgetAsXML()
  {
    final DefaultWidgetTemplate embeddableWidget = DefaultWidgetTemplate.createEmptyWidget();
    embeddableWidget.getRootPanel().setOrCreateCustomProperty(SOME_PROPERTY, new SimpleDataTable(FieldFormat.create("name", 'S').wrap(), 1), null, null);
    embeddableWidget.getRootPanel().add(new WLabel("label"));
    return WidgetExport.exportToXML(embeddableWidget);
  }
  
  private void loadSubwidget() throws Exception
  {
    getComponentContext().setVariable(WSubWidget.V_REFERENCE, PATH_TO_SUB_WIDGET);
    
    final Condition cond = new Condition()
    {
      @Override
      public boolean check()
      {
        return getComponentContext().getVariableDefinition(SOME_PROPERTY) != null;
      }
    };
    
    AggreGateTestingUtils.wait(2000, 200, cond);
  }
}
