package com.tibbo.aggregate.client;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.junit.*;

import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.binding.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.data.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.event.*;
import com.tibbo.aggregate.common.expression.*;
import com.tibbo.aggregate.common.widget.chart.*;
import com.tibbo.aggregate.common.widget.component.*;
import com.tibbo.aggregate.common.widget.context.*;
import com.tibbo.aggregate.common.widget.runtime.*;

public class TestEventBindings extends BaseWidgetTest<WButton>
{
  @Test
  public void contextClickEventIsHandled() throws InterruptedException
  {
    startEngine();
    
    final boolean[] handled = { false };
    
    final DefaultContextEventListener clickListener = new DefaultContextEventListener()
    {
      @Override
      public void handle(Event event) throws EventHandlingException
      {
        handled[0] = true;
      }
    };
    
    getComponentContext().addEventListener(WButtonContext.E_CLICK, clickListener);
    
    clickButton();
    
    assertTrue(handled[0]);
  }
  
  @Test
  public void referenceClickListenerIsPresent() throws Exception
  {
    widgetTemplate.addBinding(buttonTextRef(), "'text'", false, buttonClickRef());
    
    startEngine();
    
    final ContextEventListener eventListener = engine.getBindingProvider().getEventListener();
  
    assertTrue(getComponentContext().getEventData(WButtonContext.E_CLICK).contains(eventListener));
  }
  
  @Test
  public void clickActivatorWorks() throws Exception
  {
    widgetTemplate.addBinding(buttonTextRef(), "'click happens!'", false, buttonClickRef());
    
    startEngine();
    
    clickButton();
    
    checkBindingErrors();
    
    assertThat(getComponent().getText(), is("click happens!"));
  }
  
  @Test
  public void functionExecutes() throws Exception
  {
    final String functionIsExecuted = "function is executed!";
    
    widgetTemplate.addBinding(".:f()", "\"" + functionIsExecuted + "\"", false, buttonClickRef());
    
    startEngine();
    
    clickButton();
    
    checkBindingErrors();
    
    assertThat(lastFuncValue, equalTo(functionIsExecuted));
  }
  
  @Test
  public void closeEventOccurs()
  {
    final ArrayList<String> operations = new ArrayList<String>();
    operations.add(WRootPanelContext.CLOSE_EVENT);
    getComponent().setOperations(operations);
    
    startEngine();
    
    final boolean[] closeEventOccurs = new boolean[1];
    engine.getViewer().getRootContext().addEventListener(WRootPanelContext.CLOSE_EVENT, new DefaultContextEventListener()
    {
      @Override
      public void handle(Event event) throws EventHandlingException
      {
        closeEventOccurs[0] = true;
      }
    });
    
    clickButton();
    
    assertTrue(closeEventOccurs[0]);
  }
  
  @Test
  public void eventEnvironment() throws Exception
  {
    final String expression = "{env/" + EventUtils.ENVIRONMENT_CONTEXT + "} == {form/" + getComponentName() + ":}";
    widgetTemplate.addBinding(buttonTextRef(), expression, false, "form/" + getComponentName() + ":" + WCompassContext.E_MOUSE_PRESSED + "@");
    
    startEngine();
    
    (getComponentRepresentation()).dispatchEvent(new MouseEvent(getComponentRepresentation(), MouseEvent.MOUSE_PRESSED, 1, 0, 1, 1, 2, true, MouseEvent.BUTTON1));
    
    checkBindingErrors();
    
    assertThat(getComponent().getText(), is("true"));
  }
  
  @Test
  public void noBindingsExecutionEvent() throws Exception
  {
    final DataTable[] executionData = new DataTable[1];
    
    widgetTemplate.addBinding(buttonTextRef(), "'text'", false, buttonClickRef());
    
    widgetTemplate.getRootPanel().setLogBindingsExecution(false);
    
    startEngine();
    
    engine.getViewer().getRootContext().addEventListener(BindingEventsHelper.E_BINDING_EXECUTION, new DefaultContextEventListener()
    {
      @Override
      public void handle(Event event) throws EventHandlingException
      {
        executionData[0] = event.getData();
      }
    });
    
    clickButton();
    
    checkBindingErrors();
    
    assertThat(executionData[0], is(nullValue()));
  }
  
  @Test
  public void bindingsExecutionEvent() throws Exception
  {
    final DataTable[] executionData = new DataTable[1];
    final String expression = "'click happens!'";
    
    EvaluationOptions evaluationOptions = new EvaluationOptions(false, buttonClickRef(), "true");
    Binding binding = new Binding(buttonTextRef(), expression);
    
    widgetTemplate.addBinding(binding, evaluationOptions);
    
    widgetTemplate.getRootPanel().setLogBindingsExecution(true);
    
    startEngine();
    
    engine.getViewer().getRootContext().addEventListener(BindingEventsHelper.E_BINDING_EXECUTION, new DefaultContextEventListener()
    {
      @Override
      public void handle(Event event) throws EventHandlingException
      {
        executionData[0] = event.getData();
      }
    });
    
    clickButton();
    
    checkBindingErrors();
    
    assertThat(executionData[0], is(notNullValue()));
    
    DataRecord eventData = executionData[0].rec();
    assertThat(eventData.getString(BindingEventsHelper.EF_BINDING_TARGET), equalTo(buttonTextRef()));
    assertThat(eventData.getString(BindingEventsHelper.EF_BINDING_EXPRESSION), equalTo(expression));
    assertThat(eventData.getString(BindingEventsHelper.EF_BINDING_VALUE), equalTo("click happens!"));
    assertThat(eventData.getString(BindingEventsHelper.EF_BINDING_ACTIVATOR), equalTo(buttonClickRef()));
    assertThat(eventData.getString(BindingEventsHelper.EF_BINDING_CONDITION), equalTo("true"));
    assertThat(eventData.getString(BindingEventsHelper.EF_BINDING_EXECUTION), equalTo(Cres.get().getString("wOnEvent")));
    assertThat(eventData.getString(BindingEventsHelper.EF_BINDING_CAUSE), equalTo(buttonClickRef()));
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
