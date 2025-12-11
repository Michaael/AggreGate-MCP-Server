package com.tibbo.aggregate.client.device;

import java.awt.*;
import java.io.IOException;
import java.text.MessageFormat;
import javax.swing.*;

import com.tibbo.aggregate.client.Pres;
import com.tibbo.aggregate.client.util.ClientUtils;
import com.tibbo.aggregate.common.AlertsSubscriber;
import com.tibbo.aggregate.common.Cres;
import com.tibbo.aggregate.common.Log;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.DefaultContextEventListener;
import com.tibbo.aggregate.common.context.UncheckedCallerController;
import com.tibbo.aggregate.common.data.Event;
import com.tibbo.aggregate.common.device.RemoteDeviceErrorException;
import com.tibbo.aggregate.common.event.ContextEventListener;
import com.tibbo.aggregate.common.event.EventHandlingException;
import com.tibbo.aggregate.common.event.EventLevel;
import com.tibbo.aggregate.common.protocol.RemoteServer;
import com.tibbo.aggregate.common.protocol.RemoteServerController;
import com.tibbo.aggregate.common.security.AuthUtils;
import com.tibbo.aggregate.common.server.RootContextConstants;
import com.tibbo.aggregate.component.ComponentHelper;
import org.apache.log4j.Level;

public class ClientSideServerController extends RemoteServerController
{
  private final AlertsListener alertListener;
  
  public ClientSideServerController(RemoteServer device)
  {
    super(device, true);
    alertListener = new AlertsListener(this, getContextManager(), this);
  }
  
  @Override
  public void start() throws IOException, InterruptedException, ContextException, RemoteDeviceErrorException
  {
    UncheckedCallerController callerController = new UncheckedCallerController(getDevice().getUsername());
    String username = AuthUtils.getCorrectUsername(getDevice().getUsername(), getContextManager(), callerController);
    
    callerController = new UncheckedCallerController(username);
    callerController.setLogin(getDevice().getLogin());
    callerController.setInheritedUsername(username);
    
    setCallerController(callerController);
    
    getSettings().fillBasicProperties(getContextManager(), getCallerController());
    
    // Starting to receive feedbacks
    
    ComponentHelper.getIoThreadPool().submit(new FeedbackSubscriber());
    
    // Getting variable-related and event-related actions
    
    ComponentHelper.getIoThreadPool().submit(new EntityRelatedActionLoader());
    
    // Starting to receive alarms
    
    AlertsSubscriber subscriber = new AlertsSubscriber(alertListener, getContextManager(), getCallerController(), username);
    ComponentHelper.getIoThreadPool().submit(subscriber);
    
    // Starting to receive event delivery failures
    
    ComponentHelper.getIoThreadPool().submit(new EventDeliveryFailureSubscriber());
  }
  
  @Override
  protected String getConnectionErrorMessage()
  {
    return MessageFormat.format(Pres.get().getString("rlsConnectionFailed"), Cres.get().getString("productServer"));
  }
  
  private final ContextEventListener feedbackListener = new DefaultContextEventListener()
  {
    @Override
    public void handle(Event ev)
    {
      // Resetting timeouts for all running commands, as no method exists to match feedback event to a command in progress
      resetCommandTimeouts();
      
      String msg = ev.getData().rec().getString(RootContextConstants.EF_FEEDBACK_MESSAGE);
      
      switch (ev.getLevel())
      {
        case EventLevel.WARNING:
        case EventLevel.ERROR:
        case EventLevel.FATAL:
          Toolkit.getDefaultToolkit().beep();
          JOptionPane.showMessageDialog(ComponentHelper.getMainFrame(), msg, Cres.get().getString("message"), JOptionPane.INFORMATION_MESSAGE);
          break;
        
        default:
          ComponentHelper.getMainFrame().setMessage(msg);
      }
      
    }
  };
  
  private class FeedbackSubscriber implements Runnable
  {
    @Override
    public void run()
    {
      try
      {
        // Relying to automatic server-side listener cleanup upon client disconnection
        getContextManager().getRoot().addEventListener(RootContextConstants.E_FEEDBACK, feedbackListener);
      }
      catch (Exception ex)
      {
        Log.CLIENTS.error("Error subscribing to feedback events from " + getDevice(), ex);
      }
    }
  }
  
  private class EntityRelatedActionLoader implements Runnable
  {
    @Override
    public void run()
    {
      getSettings().fillActions(getContextManager(), null);
    }
  }
  
  private class EventDeliveryFailureSubscriber implements Runnable
  {
    @Override
    public void run()
    {
      if (getContextManager().getRoot().getEventDefinition(RootContextConstants.E_EVENT_DELIVERY_FAILURE) != null)
      {
        getContextManager().getRoot().addEventListener(RootContextConstants.E_EVENT_DELIVERY_FAILURE, new EventDeliveryFailureListener());
      }
    }
    
    private class EventDeliveryFailureListener extends DefaultContextEventListener
    {
      @Override
      public void handle(Event event) throws EventHandlingException
      {
        ClientUtils.showError(Level.ERROR, event.getName(), event.getData().rec().getString(0), new Throwable());
      }
    }
  }
}
