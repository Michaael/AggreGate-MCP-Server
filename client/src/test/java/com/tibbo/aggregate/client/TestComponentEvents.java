package com.tibbo.aggregate.client;

import java.awt.event.*;
import java.util.concurrent.*;

import org.junit.*;

import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.data.*;
import com.tibbo.aggregate.common.event.*;
import com.tibbo.aggregate.common.widget.component.*;
import com.tibbo.aggregate.common.widget.context.*;
import com.tibbo.aggregate.common.widget.runtime.*;

import static org.junit.Assert.*;

public class TestComponentEvents extends BaseWidgetTest<WLabel>
{
  private CountDownLatch occurred;
  
  @Override
  public void setUp() throws Exception
  {
    super.setUp();
    
    occurred = new CountDownLatch(1);
    
    widgetTemplate.addBinding("", "", false, "form/" + getComponentName() + ":hidden@");
    widgetTemplate.addBinding("", "", false, "form/" + getComponentName() + ":mouseClicked@");
    widgetTemplate.addBinding("", "", false, "form/" + getComponentName() + ":mouseMoved@");
    widgetTemplate.addBinding("", "", false, "form/" + getComponentName() + ":mouseWheelMoved@");
    widgetTemplate.addBinding("", "", false, "form/" + getComponentName() + ":mouseKeyPressed@");
    widgetTemplate.addBinding("", "", false, "form/" + getComponentName() + ":mouseFocusLost@");
    
    startEngine();
  }
  
  @Test
  public void componentEvent() throws InterruptedException
  {
    getComponentContext().addEventListener(WAbstractContext.E_HIDDEN, new DefaultContextEventListener()
    {
      @Override
      public void handle(Event event) throws EventHandlingException
      {
        occurred.countDown();
      }
    });
    
    getComponentRepresentation().setVisible(false);
    
    assertTrue(occurred());
  }
  
  @Test
  public void mouseEvent() throws InterruptedException
  {
    getComponentContext().addEventListener(WAbstractContext.E_MOUSE_CLICKED, new DefaultContextEventListener()
    {
      @Override
      public void handle(Event event) throws EventHandlingException
      {
        occurred.countDown();
      }
    });
    
    getComponentRepresentation().dispatchEvent(new MouseEvent(getComponentRepresentation(), MouseEvent.MOUSE_CLICKED, 1234, 0, 1, 1, 1, false, 1));
    
    assertTrue("Mouse click event should fired because there are references to it", occurred());
  }
  
  @Test
  public void mouseMotionEvent() throws InterruptedException
  {
    getComponentContext().addEventListener(WAbstractContext.E_MOUSE_MOVED, new DefaultContextEventListener()
    {
      @Override
      public void handle(Event event) throws EventHandlingException
      {
        occurred.countDown();
      }
    });
    
    getComponentRepresentation().dispatchEvent(new MouseEvent(getComponentRepresentation(), MouseEvent.MOUSE_MOVED, 1234, 0, 1, 1, 1, false, 1));
    
    assertTrue(occurred());
  }
  
  @Test
  public void mouseWheelEvent() throws InterruptedException
  {
    getComponentContext().addEventListener(WAbstractContext.E_MOUSE_WHEEL_MOVED, new DefaultContextEventListener()
    {
      @Override
      public void handle(Event event) throws EventHandlingException
      {
        occurred.countDown();
      }
    });
    
    getComponentRepresentation().dispatchEvent(new MouseWheelEvent(getComponentRepresentation(), MouseWheelEvent.MOUSE_WHEEL, 1234, 0, 1, 1, 1, false, MouseWheelEvent.WHEEL_BLOCK_SCROLL, 1, 1));
    
    assertTrue(occurred());
  }

  @Test
  @Ignore("do not know how to dispatch event properly")
  public void keyEvent() throws InterruptedException
  {
    getComponentContext().addEventListener(WAbstractContext.E_KEY_PRESSED, new DefaultContextEventListener()
    {
      @Override
      public void handle(Event event) throws EventHandlingException
      {
        occurred.countDown();
      }
    });
    
    getComponentRepresentation().dispatchEvent(new KeyEvent(getComponentRepresentation(), KeyEvent.KEY_PRESSED, 1, 0, KeyEvent.VK_UNDEFINED, 'c'));
    
    assertTrue(occurred());
  }
  
  @Test
  @Ignore("do not know how to dispatch event properly")
  public void focusEvent() throws InterruptedException
  {
    getComponentContext().addEventListener(WAbstractContext.E_FOCUS_LOST, new DefaultContextEventListener()
    {
      @Override
      public void handle(Event event) throws EventHandlingException
      {
        occurred.countDown();
      }
    });
    
    getComponentRepresentation().dispatchEvent(new FocusEvent(getComponentRepresentation(), FocusEvent.FOCUS_LOST));
    
    assertTrue(occurred());
  }
  
  @Override
  protected Class<WLabel> componentClass()
  {
    return WLabel.class;
  }
  
  private boolean occurred() throws InterruptedException
  {
    return occurred.await(100, TimeUnit.MILLISECONDS);
  }
}
