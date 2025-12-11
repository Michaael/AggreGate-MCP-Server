package com.tibbo.aggregate.client;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import javax.swing.*;

import org.junit.*;

import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.expression.*;
import com.tibbo.aggregate.common.widget.*;
import com.tibbo.aggregate.common.widget.component.*;
import com.tibbo.aggregate.common.widget.context.*;
import com.tibbo.aggregate.common.widget.runtime.*;

public class TestWidgetBindingsActivator extends BaseWidgetTest<WButton>
{

  public static final String PANEL = "panel";
  public static final String MENU_ITEM = "menuItem";

  @Test
  public void testWidgetProperty() throws Exception
  {
    final Reference widthReference = buttonTextRef();
    widthReference.setEntity(WComponent.V_WIDTH);

    widgetTemplate.addBinding(buttonTextRef().getImage(), "true", false, widthReference.getImage());

    startEngine();

    getComponent().setWidth(123456);

    checkBindingErrors();

    assertThat(getComponent().getText(), is("true"));
  }

  @Test
  public void testServerVariable() throws Exception
  {
    // Fake server root context is RootPanelContext
    final Reference serverVariableReference = new Reference("", WRootPanelContext.V_IGNORE_BINDING_ERRORS, ContextUtils.ENTITY_VARIABLE);

    widgetTemplate.addBinding(buttonTextRef().getImage(), "true", true, serverVariableReference.getImage());

    startEngine();

    final WRootPanelContext<WRootPanel> root = (WRootPanelContext) engine.getServerContextManager().getRoot();
    root.getComponent().setIgnoreBindingErrors(false);

    checkBindingErrors();

    assertThat(getComponent().getText(), is("true"));
  }

  @Test
  public void testComponentEvent() throws Exception
  {
    widgetTemplate.addBinding(buttonTextRef().getImage(), "true", false, buttonClickRef());

    startEngine();

    clickButton();

    checkBindingErrors();

    assertThat(getComponent().getText(), is("true"));
  }

  @Test
  public void testPopupMenu() throws Exception
  {
    widgetTemplate.getRootPanel().add(new WPanel(PANEL), new WGridConstraints(0, 1));

    final Reference popupMenuReference = new Reference(PANEL, MENU_ITEM, ContextUtils.ENTITY_VARIABLE);
    popupMenuReference.setSchema(Reference.SCHEMA_MENU);
    widgetTemplate.addBinding(buttonTextRef().getImage(), "true", false, popupMenuReference.getImage());

    startEngine();

    final DataRecord popupData = new DataRecord(WAbstractContext.EFT_MENU_POPUP);
    popupData.setValue(WAbstractContext.EF_ITEM, MENU_ITEM);
    engine.getViewer().getComponentContextByName(PANEL).fireEvent(WAbstractContext.E_MENU_POPUP, popupData.wrap());

    checkBindingErrors();

    assertThat(getComponent().getText(), is("true"));
  }

  @Test
  @Ignore("Fake context manager too fake")
  public void testEventContext() throws Exception
  {
    final Reference popupMenuReference = new Reference("", WAbstractContext.E_INFO, ContextUtils.ENTITY_EVENT);
    widgetTemplate.addBinding(buttonTextRef().getImage(), "true", false, popupMenuReference.getImage());

    startEngine();

    final TableFormat eventFormat = engine.getServerContextManager().getRoot().getEventDefinition(WAbstractContext.E_INFO).getFormat();
    engine.getServerContextManager().getRoot().fireEvent(WAbstractContext.E_INFO, eventFormat);

    checkBindingErrors();

    assertThat(getComponent().getText(), is("true"));
  }

  @Override
  protected Class<WButton> componentClass()
  {
    return WButton.class;
  }
  
  protected void clickButton()
  {
    ((JButton) getComponentRepresentation()).doClick();
  }
  
  protected String buttonClickRef()
  {
    final Reference buttonClickReference = new Reference(getComponentName(), WButtonContext.E_CLICK, ContextUtils.ENTITY_EVENT);
    buttonClickReference.setSchema(Reference.SCHEMA_FORM);
    return buttonClickReference.getImage();
  }

  private Reference buttonTextRef()
  {
    final Reference buttonTextReference = new Reference(getComponentName(), WButtonContext.V_TEXT, ContextUtils.ENTITY_VARIABLE);
    buttonTextReference.setSchema(Reference.SCHEMA_FORM);
    return buttonTextReference;
  }
}
