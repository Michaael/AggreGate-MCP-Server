package com.tibbo.aggregate.client;

import javax.swing.*;

import org.junit.*;

import com.tibbo.aggregate.common.binding.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.expression.*;
import com.tibbo.aggregate.common.widget.component.*;
import com.tibbo.aggregate.common.widget.context.*;
import com.tibbo.aggregate.common.widget.runtime.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class TestBindingCondition extends BaseWidgetTest<WButton>
{
  @Test
  public void testTrueCondition() throws Exception
  {
    final EvaluationOptions evaluationOptions = new EvaluationOptions(false, buttonClickRef(), "true");
    final ExtendedBinding binding = new ExtendedBinding(new Binding(buttonTextRef(), "'success'"), evaluationOptions);

    widgetTemplate.addBinding(binding);

    startEngine();

    clickButton();

    checkBindingErrors();

    assertThat(getComponent().getText(), is("success"));
  }

  @Test
  public void testFalseCondition() throws Exception
  {
    final EvaluationOptions evaluationOptions = new EvaluationOptions(false, buttonClickRef(), "false");
    final ExtendedBinding binding = new ExtendedBinding(new Binding(buttonTextRef(), "'success'"), evaluationOptions);

    widgetTemplate.addBinding(binding);

    startEngine();

    clickButton();

    checkBindingErrors();

    assertThat(getComponent().getText(), is(not("success")));
  }

  @Test
  public void testEmptyCondition() throws Exception
  {
    final EvaluationOptions evaluationOptions = new EvaluationOptions(false, buttonClickRef(), "");
    final ExtendedBinding binding = new ExtendedBinding(new Binding(buttonTextRef(), "'success'"), evaluationOptions);

    widgetTemplate.addBinding(binding);

    startEngine();

    clickButton();

    checkBindingErrors();

    assertThat(getComponent().getText(), is("success"));
  }

  @Test(expected = BindingException.class)
  public void testNotBooleanCondition() throws Exception
  {
    final EvaluationOptions evaluationOptions = new EvaluationOptions(false, buttonClickRef(), "'notBoolean'");
    final ExtendedBinding binding = new ExtendedBinding(new Binding(buttonTextRef(), "'success'"), evaluationOptions);

    widgetTemplate.addBinding(binding);

    startEngine();

    clickButton();

    checkBindingErrors();

    assertThat(getComponent().getText(), is(not("success")));
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

  protected String buttonTextRef()
  {
    final Reference buttonTextReference = new Reference(getComponentName(), WButtonContext.V_TEXT, ContextUtils.ENTITY_VARIABLE);
    buttonTextReference.setSchema(Reference.SCHEMA_FORM);
    return buttonTextReference.getImage();
  }
}
