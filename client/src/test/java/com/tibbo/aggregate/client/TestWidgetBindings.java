package com.tibbo.aggregate.client;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import javax.swing.*;

import org.junit.*;

import com.tibbo.aggregate.common.binding.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.data.*;
import com.tibbo.aggregate.common.event.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.common.widget.component.*;
import com.tibbo.aggregate.common.widget.runtime.*;

public class TestWidgetBindings extends BaseWidgetTest<WButton>
{
  @Override
  public void setUp() throws Exception
  {
    super.setUp();
    
    widgetTemplate.getRootPanel().add(new WLabel("label"));
  }
  
  @Test
  public void testDoubleBindingExecution() throws Exception
  {
    // Expression type should differ from target type (form/WButton:text - String, 123 - Integer), see WComponentContext>writeReference()
    widgetTemplate.addBinding("form/WButton:text", "123", false, "form/WButton:click@");
    widgetTemplate.addBinding("form/label:text", "{form/WButton:text}", false, true);
    
    startEngine();
    
    widgetTemplate.getRootPanel().setLogBindingsExecution(true);
    
    final Event[] doubleBindingExecutionEvent = new Event[1];
    
    engine.getViewer().getRootContext().addEventListener(BindingEventsHelper.E_BINDING_EXECUTION, new DefaultContextEventListener()
    {
      public Event previousEvent;
      
      @Override
      public void handle(Event event) throws EventHandlingException
      {
        if (previousEvent != null && Util.equals(previousEvent.getData(), event.getData()))
          doubleBindingExecutionEvent[0] = event;
          
        previousEvent = event;
      }
    });
    
    clickButton();
    
    checkBindingErrors();
    
    assertThat("Double binding execution event is present.", doubleBindingExecutionEvent[0], is(nullValue()));
  }
  
  @Test
  public void testBindingExecutionOrder()
  {
    WLabel label = (WLabel) widgetTemplate.getRootPanel().getChildren().get(1);
    
    widgetTemplate.addBinding("form/WButton:text", "123", true, false);
    widgetTemplate.addBinding("form/label:text", "{form/WButton:text}", false, true);
    
    startEngine();
    
    assertEquals("123", label.getText());
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
}
