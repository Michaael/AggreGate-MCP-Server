package com.tibbo.aggregate.client;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import javax.swing.*;

import org.junit.*;

import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.expression.*;
import com.tibbo.aggregate.common.widget.component.*;
import com.tibbo.aggregate.common.widget.context.*;
import com.tibbo.aggregate.common.widget.runtime.*;

public class TestWidgetBindingsTarget extends BaseWidgetTest<WButton>
{
  @Test
  public void testProperty() throws Exception
  {
    final Reference reference = buttonTextRef();
    reference.setField(WButtonContext.V_TEXT);
    widgetTemplate.addBinding(reference.getImage(), "'success'", false, buttonClickRef());

    startEngine();

    clickButton();

    checkBindingErrors();

    assertThat(getComponent().getText(), is("success"));
  }

  @Test
  public void testVariableAcceptsScalarValues() throws Exception
  {
    final Reference reference = new Reference("", WRootPanelContext.V_IGNORE_BINDING_ERRORS, ContextUtils.ENTITY_VARIABLE);
    widgetTemplate.addBinding(reference.getImage(), "false", false, buttonClickRef());

    startEngine();

    // Fake server Context Manager has RootPanel as root
    final WRootPanel rootPanel = ((WRootPanelContext<WRootPanel>) engine.getServerContextManager().getRoot()).getComponent();

    assertThat(rootPanel.isIgnoreBindingErrors(), is(true));

    clickButton();

    checkBindingErrors();

    assertThat(rootPanel.isIgnoreBindingErrors(), is(false));
  }

  @Test
  public void testField() throws Exception
  {
    final Reference reference = new Reference("", WRootPanelContext.V_IGNORE_BINDING_ERRORS, ContextUtils.ENTITY_VARIABLE);
    reference.setField(WRootPanelContext.V_IGNORE_BINDING_ERRORS);

    widgetTemplate.addBinding(reference.getImage(), "false", false, buttonClickRef());

    startEngine();

    // Fake server Context Manager has RootPanel as root
    final WRootPanel rootPanel = ((WRootPanelContext<WRootPanel>) engine.getServerContextManager().getRoot()).getComponent();

    assertThat(rootPanel.isIgnoreBindingErrors(), is(true));

    clickButton();

    checkBindingErrors();

    assertThat(rootPanel.isIgnoreBindingErrors(), is(false));
  }

  @Test
  public void testFunction() throws Exception
  {
    final String functionCalled = "functionCalled";
    final Reference fReference = new Reference("", "f", ContextUtils.ENTITY_FUNCTION);
    widgetTemplate.addBinding(fReference.getImage(), "'" + functionCalled + "'", false, buttonClickRef());

    startEngine();

    clickButton();

    checkBindingErrors();

    assertThat(lastFuncValue, is(functionCalled));
  }

  @Test
  public void testAction()
  {
    final Reference reference = new Reference("", "action", ContextUtils.ENTITY_ACTION);
    reference.setSchema(Reference.SCHEMA_ACTION);

    widgetTemplate.addBinding(reference.getImage(), "", false, buttonClickRef());

    startEngine();

    clickButton();

    assertThat("Exception about absent executor when trying to call action is not rise",
        bindingErrors.get(0).getCause().getCause().getMessage(), is("Action executor is not available"));
  }

  @Test
  public void testScript() throws Exception
  {
    final Reference reference = new Reference(null, "script", ContextUtils.ENTITY_FUNCTION);
    reference.setSchema(Reference.SCHEMA_FORM);

    widgetTemplate.addBinding(reference.getImage(), "", false, buttonClickRef());

    startEngine();

    final Context root = engine.getServerContextManager().getRoot();

    root.addActionDefinition(new BasicActionDefinition("action", null));

    clickButton();

    assertThat(bindingErrors.get(0).getMessage(), containsString("Failed to compile script"));
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
