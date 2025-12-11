package com.tibbo.aggregate.client.device;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import javax.swing.SwingWorker;
import javax.swing.*;

import com.jidesoft.alert.*;
import com.jidesoft.animation.*;
import com.jidesoft.swing.*;
import com.jidesoft.utils.*;
import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.client.gui.dialog.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.client.util.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.data.Event;
import com.tibbo.aggregate.common.data.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.device.*;
import com.tibbo.aggregate.common.event.*;
import com.tibbo.aggregate.common.expression.*;
import com.tibbo.aggregate.common.protocol.*;
import com.tibbo.aggregate.common.resource.*;
import com.tibbo.aggregate.common.server.*;
import com.tibbo.aggregate.common.structure.OriginKind;
import com.tibbo.aggregate.common.structure.Pinpoint;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.common.util.StringUtils;
import com.tibbo.aggregate.component.*;
import com.tibbo.aggregate.component.datatableeditor.*;
import org.apache.commons.lang3.exception.*;
import org.apache.log4j.*;

import static com.tibbo.aggregate.common.structure.PinpointFactory.newPinpointFor;

public class AlertsListener extends DefaultContextEventListener
{
  private static final int SMOOTHNESS_THRESHOLD = 10;
  private static Map<Integer, Integer> TIMEOUTS = new Hashtable();
  static
  {
    TIMEOUTS.put(EventLevel.NONE, 10000);
    TIMEOUTS.put(EventLevel.NOTICE, 20000);
    TIMEOUTS.put(EventLevel.INFO, 300000);
    TIMEOUTS.put(EventLevel.WARNING, 3600000);
    TIMEOUTS.put(EventLevel.ERROR, 0);
    TIMEOUTS.put(EventLevel.FATAL, 0);
  }
  
  private static final AlertGroup GROUP = new AlertGroup();
  private static final Set<Alert> ALERTS = Collections.newSetFromMap(new WeakHashMap());
  
  private final RemoteServerController controller;
  private final ContextManager contextManager;
  private final RemoteConnector connector;
  
  public AlertsListener(RemoteServerController controller, ContextManager cm, RemoteConnector connector)
  {
    this.controller = controller;
    this.contextManager = cm;
    this.connector = connector;
  }
  
  public void handle(final Event ev) throws EventHandlingException
  {
    Log.ALERTS.debug("Received alert event: " + ev);
    
    if (!ComponentHelper.getMainFrame().isShowAlerts())
    {
      Log.ALERTS.warn("Suppressing popup alert since Alerts menu item is disabled: " + ev);
      return;
    }
    
    SwingWorker sw = new SwingWorker<AlertInstanceData, Object>()
    {
      @Override
      public AlertInstanceData doInBackground()
      {
        AlertInstanceData data = new AlertInstanceData();
        
        try
        {
          
          data.setLevel(ev.getLevel());
          
          data.setAlertContextName(ev.getContext());
          
          DataRecord rec = ev.getData().rec();
          
          data.setSource(rec.getString(AlertContextConstants.EF_ALERTNOTIFY_CONTEXT));
          
          data.setDescription(rec.getString(AlertContextConstants.EF_ALERTNOTIFY_DESCRIPTION));
          
          String msg = rec.getString(AlertContextConstants.EF_ALERTNOTIFY_MESSAGE);
          data.setMessage((msg != null && msg.length() > 0) ? msg : "");
          
          String trg = rec.getString(AlertContextConstants.EF_ALERTNOTIFY_TRIGGER);
          data.setTrigger((trg != null && trg.length() > 0) ? trg : "");
          
          data.setData(rec.getDataTable(AlertContextConstants.EF_ALERTNOTIFY_DATA));
          
          data.setCause(rec.getString(AlertContextConstants.EF_ALERTNOTIFY_CAUSE));
          
          data.setAlertEventId(rec.getLong(AlertContextConstants.EF_ALERTNOTIFY_ALERT_EVENT_ID));
          
          // Distributed: safe, since event's source contexts require no conversion
          final Context alertConetxt = contextManager.get(ev.getContext());
          
          DataTable notifications = alertConetxt.getVariable(AlertContextConstants.V_NOTIFICATIONS);
          
          data.setNotifyOwner(notifications.rec().getBoolean(AlertContextConstants.VF_NOTIFICATIONS_NOTIFY_OWNER));
          data.setNotificationNecessityExpression(notifications.rec().getString(AlertContextConstants.VF_NOTIFICATIONS_NOTIFICATION_NECESSITY_EXPRESSION));
          data.setLifetime(notifications.rec().getLong(AlertContextConstants.VF_NOTIFICATIONS_LIFETIME));
          data.setAckRequired(notifications.rec().getBoolean(AlertContextConstants.VF_NOTIFICATIONS_ACK_REQUIRED));
          
          Data sound = notifications.rec().getData(AlertContextConstants.VF_NOTIFICATIONS_SOUND);
          
          data.setSoundData(sound != null ? sound.fetchData(contextManager, contextManager.getCallerController()) : null);
          
          data.setActions(alertConetxt.getVariable(AlertContextConstants.V_INTERACTIVE_ACTIONS));
        }
        catch (Exception ex)
        {
          boolean disconnected = ExceptionUtils.indexOfType(ex, DisconnectionException.class) != -1;
          if (disconnected)
          {
            Log.ALERTS.debug("Error in alerts listener", ex);
          }
          else
          {
            ClientUtils.showError(Level.INFO, null, Cres.get().getString("error"), ex);
          }
          return null;
        }
        
        return data;
      }
      
      @Override
      public void done()
      {
        try
        {
          AlertInstanceData data = get();
          if (data != null)
          {
            handleAlert(data);
          }
        }
        catch (Exception ex)
        {
          throw new IllegalStateException(ex);
        }
      }
    };
    ComponentHelper.getIoThreadPool().submit(sw);
  }
  
  private void handleAlert(AlertInstanceData data)
  {
    boolean notify = data.isNotifyOwner();
    if (!notify && !StringUtils.isEmpty(data.getNotificationNecessityExpression()))
    {
      CallerController cc = controller.getCallerController();
      
      String userContextPath = ContextUtils.userContextPath(cc.getEffectiveUsername());
      Context userContext = contextManager.get(userContextPath, cc);
      
      Evaluator evaluator = new Evaluator(contextManager, cc);
      evaluator.setDefaultContext(userContext);
      
      try
      {
        Pinpoint expressionOriginNotificationNecessity = newPinpointFor(data.getAlertContextName(), AlertContextConstants.V_NOTIFICATIONS)
            .withOriginField(AlertContextConstants.VF_NOTIFICATIONS_NOTIFICATION_NECESSITY_EXPRESSION, OriginKind.EXPRESSION);
        Expression notificationNecessityExpression = new Expression(data.getNotificationNecessityExpression());
        notificationNecessityExpression.assignPinpoint(expressionOriginNotificationNecessity);
        notify = (Boolean) evaluator.evaluate(notificationNecessityExpression);
      }
      catch (SyntaxErrorException | EvaluationException ex)
      {
        Log.ALERTS.info("Error evaluating an expression for notified users '" + data.getNotificationNecessityExpression() + "'", ex);
      }
    }
    
    if (notify)
    {
      if (data.getSoundData() != null)
      {
        playSound(data.getSoundData());
      }
      else
      {
        Log.ALERTS.debug("No sound data loaded for: " + data.getAlertContextName());
      }
      
      if (data.isAckRequired())
      {
        acknowledgeAlert(data);
      }
      else
      {
        showAlert(data);
      }
    }
    
    executeInteractiveCorrectiveActions(data);
  }
  
  private void showAlert(AlertInstanceData data)
  {
    final Alert alert = new Alert();
    alert.setResizable(true);
    alert.setMovable(true);
    alert.setTransient(false);
    alert.setAlwaysOnTop(true);
    alert.setTimeout(data.getLifetime() != null ? data.getLifetime().intValue() : TIMEOUTS.get(data.getLevel()));
    alert.setPopupBorder(BorderFactory.createLineBorder(new Color(10, 30, 106)));
    
    int smoothness = ALERTS.size() < SMOOTHNESS_THRESHOLD ? CustomAnimation.SMOOTHNESS_VERY_SMOOTH : CustomAnimation.SMOOTHNESS_VERY_ROUGH;
    
    CustomAnimation showAnimation = new CustomAnimation(CustomAnimation.TYPE_ENTRANCE, CustomAnimation.EFFECT_FLY, smoothness, CustomAnimation.SPEED_VERY_FAST);
    showAnimation.setVisibleBounds(PortingUtils.getLocalScreenBounds());
    showAnimation.setFunctionY(CustomAnimation.FUNC_BOUNCE);
    showAnimation.setDirection(CustomAnimation.BOTTOM);
    alert.setShowAnimation(showAnimation);
    
    CustomAnimation hideAnimation = new CustomAnimation(CustomAnimation.TYPE_EXIT, CustomAnimation.EFFECT_FLY, smoothness, CustomAnimation.SPEED_VERY_FAST);
    hideAnimation.setVisibleBounds(PortingUtils.getLocalScreenBounds());
    hideAnimation.setFunctionY(CustomAnimation.FUNC_POW3);
    hideAnimation.setDirection(CustomAnimation.BOTTOM);
    alert.setHideAnimation(hideAnimation);
    
    alert.getContentPane().setLayout(new BorderLayout());
    alert.getContentPane().add(createAlertPanel(data, alert));
    
    ALERTS.add(alert);
    GROUP.add(alert);
    
    alert.showPopup(SwingConstants.NORTH_EAST);
  }
  
  private void acknowledgeAlert(final AlertInstanceData data)
  {
    String title = Cres.get().getString("alert") + " - " + controller.getDevice().toString();
    
    int type = getMessageBoxType(data.getLevel());
    
    String finalMsg = createAlertMessage(data, true);
    
    finalMsg += "<br>" + Cres.get().getString("aEnterAckText");
    
    final String ack = JOptionPane.showInputDialog(ComponentHelper.getMainFrame(), finalMsg, title, type);
    
    if (ack != null)
    {
      SwingWorker sw = new SwingWorker()
      {
        @Override
        public Object doInBackground()
        {
          try
          {
            // Distributed: alertContextName is the source context of alert event, so we can use context manager here
            contextManager.get(Contexts.CTX_EVENTS).callFunction(EventsContextConstants.F_ACKNOWLEDGE, data.getAlertContextName(), AlertContextConstants.E_ALERT, data.getAlertEventId(), ack);
          }
          catch (ContextException ex)
          {
            ClientUtils.showError(Level.INFO, null, Pres.get().getString("rlsErrSettingAck"), ex);
          }
          return null;
        }
      };
      ComponentHelper.getIoThreadPool().submit(sw);
    }
  }
  
  private void executeInteractiveCorrectiveActions(AlertInstanceData data)
  {
    for (DataRecord rec : data.getActions())
    {
      String mask = rec.getString(AlertContextConstants.VF_INTERACTIVE_ACTIONS_MASK);
      String actionName = rec.getString(AlertContextConstants.VF_INTERACTIVE_ACTIONS_ACTION);
      DataTable input = rec.getDataTable(AlertContextConstants.VF_INTERACTIVE_ACTIONS_INPUT);
      boolean runFromSource = rec.getBoolean(AlertContextConstants.VF_INTERACTIVE_ACTIONS_RUN_FROM_SOURCE);
      
      mask = runFromSource ? data.getSource() : mask;
      List<Context> contexts = ContextUtils.expandMaskToContexts(mask, contextManager, null);
      
      for (Context con : contexts)
      {
        InvokeActionOperation.invoke(actionName, con, connector.getSettings(), ActionUtils.createActionInput(input).getData());
      }
    }
  }
  
  private String createAlertMessage(AlertInstanceData data, boolean ack)
  {
    String finalMsg = "<html>";
    
    String newline = "<br>";
    
    finalMsg += "<font size='+1'>" + "<b>" + Cres.get().getString("alert") + ": " + "</b>" + data.getDescription() + "</font>" + newline;
    
    if (data.getMessage() != null && data.getMessage().length() > 0)
    {
      finalMsg += "<b>" + Cres.get().getString("message") + ": " + "</b>" + data.getMessage() + newline;
    }
    
    if (data.getTrigger() != null && data.getTrigger().length() > 0)
    {
      finalMsg += "<b>" + Cres.get().getString("trigger") + ": " + "</b>" + data.getTrigger() + newline;
    }
    
    finalMsg += "<b>" + Cres.get().getString("cause") + ": " + "</b>" + data.getCause() + newline;
    
    if (!ack)
    {
      finalMsg += "<b>" + Cres.get().getString("server") + ": " + "</b>" + controller.getDevice().toString() + newline;
    }
    
    return finalMsg;
  }
  
  public JComponent createAlertPanel(AlertInstanceData data, final Alert alert)
  {
    JPanel leftPanel = new JPanel(new BorderLayout(6, 6));
    leftPanel.add(new JLabel(ResourceManager.getImageIcon(Icons.ALERT_LOGO)));
    
    JPanel rightPanel = new JPanel(new GridLayout(1, 2, 0, 0));
    
    final JideButton optionsButton = new JideButton(ResourceManager.getImageIcon(Icons.ALERT_OPTIONS));
    optionsButton.addActionListener(new OptionsButtonAction(data, alert, optionsButton));
    rightPanel.add(optionsButton);
    
    JideButton closeButton = new JideButton(ResourceManager.getImageIcon(Icons.ALERT_CLOSE));
    closeButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        alert.hidePopupImmediately();
      }
    });
    
    rightPanel.add(closeButton);
    
    final JLabel message = new JLabel(createAlertMessage(data, false));
    
    PaintPanel panel = new PaintPanel(new BorderLayout(6, 6));
    panel.setBorder(BorderFactory.createEmptyBorder(6, 7, 7, 7));
    panel.add(message, BorderLayout.CENTER);
    JPanel topPanel = JideSwingUtilities.createTopPanel(rightPanel);
    panel.add(topPanel, BorderLayout.AFTER_LINE_ENDS);
    panel.add(leftPanel, BorderLayout.BEFORE_LINE_BEGINS);
    for (int i = 0; i < panel.getComponentCount(); i++)
    {
      JideSwingUtilities.setOpaqueRecursively(panel.getComponent(i), false);
    }
    panel.setOpaque(true);
    panel.setBackgroundPaint(new GradientPaint(0, 0, EventUtils.getEventColor(data.getLevel()).brighter(), 0, panel.getPreferredSize().height, EventUtils.getEventColor(data.getLevel())));
    return panel;
  }
  
  private void playSound(byte[] soundData)
  {
    final AudioPlayer audioPlayer = new AudioPlayer(soundData);
    
    Runnable player = new Runnable()
    {
      public void run()
      {
        try
        {
          audioPlayer.play();
        }
        catch (Exception ex)
        {
          Log.ALERTS.warn("Error playing alarm sound", ex);
        }
      }
    };
    
    ComponentHelper.getIoThreadPool().submit(player);
  }
  
  private int getMessageBoxType(int level)
  {
    switch (level)
    {
      case EventLevel.FATAL:
      case EventLevel.ERROR:
        return JOptionPane.ERROR_MESSAGE;
      
      case EventLevel.WARNING:
        return JOptionPane.WARNING_MESSAGE;
      
      case EventLevel.NOTICE:
        return JOptionPane.PLAIN_MESSAGE;
      
      case EventLevel.INFO:
      default:
        return JOptionPane.INFORMATION_MESSAGE;
    }
  }
  
  private final class OptionsButtonAction implements ActionListener
  {
    private final JideButton optionsButton;
    private final Alert alert;
    private final AlertInstanceData data;
    
    private OptionsButtonAction(AlertInstanceData data, Alert alert, JideButton optionsButton)
    {
      this.data = data;
      this.alert = alert;
      this.optionsButton = optionsButton;
    }
    
    public void actionPerformed(ActionEvent e)
    {
      JPopupMenu popup = new JPopupMenu();
      
      if (alert.getTimeout() > 0)
      {
        popup.add(new JMenuItem(new AbstractAction(Cres.get().getString("stick"))
        {
          public void actionPerformed(ActionEvent e)
          {
            alert.setTimeout(0);
          }
        }));
      }
      
      // Distributed: alertContextName is the source context of alert event, so we can use context manager here
      final Context alertContext = contextManager.get(data.getAlertContextName());
      if (alertContext != null)
      {
        popup.add(new JMenuItem(new AbstractAction(Cres.get().getString("configure"))
        {
          public void actionPerformed(ActionEvent e)
          {
            InvokeActionOperation.invoke(EditableChildContextConstants.A_CONFIGURE, alertContext, connector.getSettings(), null);
          }
        }));
      }
      
      if (data.getData() != null)
      {
        popup.add(new JMenuItem(new AbstractAction(Cres.get().getString("aShowAlertData"))
        {
          public void actionPerformed(ActionEvent e)
          {
            DataTableEditor editor = new DataTableEditor(ComponentHelper.getMainFrame().getFrame(), null, data.getData(), true);
            editor.getModel().setConnector(controller);
            editor.getModel().setContextManager(contextManager);
            editor.getModel().setContext(alertContext);
            
            DataTableEditorDialog dialog = new DataTableEditorDialog(ComponentHelper.getMainFrame().getFrame(), Cres.get().getString("aAlertData"), true, false, editor);
            
            dialog.run();
          }
        }));
      }
      
      popup.add(new JMenuItem(new AbstractAction(Pres.get().getString("rlsCloseAll"))
      {
        public void actionPerformed(ActionEvent e)
        {
          for (Alert alert : ALERTS)
          {
            if (alert != null)
            {
              alert.hidePopup();
            }
          }
        }
      }));
      
      popup.show(optionsButton, 0, 0);
    }
  }
  
}
