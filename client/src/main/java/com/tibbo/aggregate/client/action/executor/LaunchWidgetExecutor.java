package com.tibbo.aggregate.client.action.executor;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import com.tibbo.aggregate.client.gui.frame.*;
import org.apache.log4j.*;

import com.jidesoft.swing.*;
import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.gui.*;
import com.tibbo.aggregate.client.guibuilder.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.client.util.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.action.command.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.resource.*;
import com.tibbo.aggregate.common.script.*;
import com.tibbo.aggregate.common.server.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.common.widget.*;
import com.tibbo.aggregate.component.*;
import com.tibbo.aggregate.component.eventlog.*;

public class LaunchWidgetExecutor extends AbstractCommandExecutor
{
  private static final String EVENT_LOG_FRAME_KEY_SUFFIX = "_log";
  
  protected InternalFrame frame;
  
  protected ScriptCompiler scriptCompiler = WidgetUtils.defaultScriptCompiler();
  
  protected static final ReferredActionExecutor ACTION_EXECUTOR = new ClientReferredActionExecutor();
  
  public LaunchWidgetExecutor()
  {
    super(ActionUtils.CMD_LAUNCH_WIDGET);
  }
  
  public LaunchWidgetExecutor(String commandType)
  {
    super(commandType);
  }
  
  @Override
  public GenericActionResponse execute(final Operation originator, final GenericActionCommand cmd)
  {
    if (!(originator instanceof InvokeActionOperation))
    {
      throw new IllegalArgumentException("Unsupported originator class: " + originator.getClass() + " required " + InvokeActionOperation.class.getName());
    }
    final InvokeActionOperation iop = (InvokeActionOperation) originator;
    
    iop.addInterruptionListener(interruptionListener);
    
    final ContextManager contextManager = iop.getContext().getContextManager();
    
    DataRecord rec = cmd.getParameters().rec();
    
    RequestIdentifier requestId = cmd.getRequestId();
    
    final String titleInCommand = cmd.getTitle();
    
    final String template = rec.getString(LaunchWidget.CF_TEMPLATE);
    
    final String widgetContextPath = rec.getString(LaunchWidget.CF_WIDGET_CONTEXT);
    
    final Context widgetContext = iop.getContext().get(widgetContextPath);
    if (widgetContext == null)
    {
      OptionPane.showMessageDialog(ComponentHelper.getMainFrame().getFrame(), Cres.get().getString("conNotAvail") + widgetContextPath, Cres.get().getString("error"), JOptionPane.ERROR_MESSAGE);
      return new GenericActionResponse(null, false, requestId);
    }
    
    final Context defaultContext = iop.getContext().get(rec.getString(LaunchWidget.CF_DEFAULT_CONTEXT));
    
    final String title = widgetContext.toString();
    
    final String widgetKey = rec.hasField(LaunchWidget.CF_KEY) ? rec.getString(LaunchWidget.CF_KEY) : null;
    final DataTable locationData = rec.getDataTable(LaunchWidget.CF_LOCATION);
    
    final DataTable dashboardData = rec.hasField(LaunchWidget.CF_DASHBOARD) ? rec.getDataTable(LaunchWidget.CF_DASHBOARD) : null;
    
    DataTable dhInfoData = rec.hasField(LaunchWidget.CF_DASHBOARDS_HIERARCHY_INFO) ? rec.getDataTable(LaunchWidget.CF_DASHBOARDS_HIERARCHY_INFO) : null;
    final DashboardsHierarchyInfo dhInfo = dhInfoData != null ? new DashboardsHierarchyInfo(dhInfoData.rec()) : null;
    
    final DataTable input = rec.hasField(LaunchWidget.CF_INPUT) ? rec.getDataTable(LaunchWidget.CF_INPUT) : null;
    
    final RemoteConnector connector = iop.getConnector();
    
    final CallerController caller = connector != null ? connector.getCallerController() : null;
    
    final boolean editingAllowed = widgetContext != null && widgetContext.getActionDefinition(WidgetContextConstants.A_EDIT_TEMPLATE) != null;
    
    final Map<String, Object> environment = new HashMap();
    environment.put(LaunchWidget.CF_LOCATION, locationData);
    environment.put(LaunchWidget.CF_DASHBOARD, dashboardData);
    
    SwingUtilities.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          long startTime = System.currentTimeMillis();
          
          WindowLocation location = locationData != null ? new WindowLocation(locationData.rec()) : null;
          
          DashboardProperties dashboard = dashboardData != null ? new DashboardProperties(dashboardData.rec()) : null;
          
          final String uniqueScriptPrefix = WidgetUtils.uniqueScriptPrefixFor(widgetContext != null ? widgetContext : defaultContext);
          
          final SwingWidget widgetComponent = createSwingWidgetComponent(title, contextManager, widgetContext, defaultContext, caller, template, input, environment, uniqueScriptPrefix, connector);
          
          widgetComponent.start();
          
          widgetComponent.getEngine().resetMainComponent();
          
          widgetComponent.setMinimumSize(new Dimension(0, 0));
          
          final Collection<String> allFrames = ComponentHelper.getMainFrame().getDockingManager().getAllFrames();
          final String firstFrameKey = allFrames.size() > 0 ? allFrames.iterator().next().toString() : null;
          final InternalFrame firstFrame = firstFrameKey != null ? ClientUtils.findFrame(InternalFrame.FRAME_KEY_SYSTEMTREE) : null;
          
          int widgetWidth = widgetComponent.getEngine().getWidget().getRootPanel().getWidth();
          int widgetHeight = widgetComponent.getEngine().getWidget().getRootPanel().getHeight();
          
          Dimension size = ClientUtils.getWidgetFrameSize(firstFrame, new Dimension(widgetWidth, widgetHeight));
          
          if (location == null)
          {
            location = new WindowLocation(WindowLocation.STATE_FLOATING, WindowLocation.SIDE_RIGHT, 0, size);
          }
          else
          {
            location.applyDefaultSize(size);
          }
          
          String widgetFrameKey = widgetKey != null ? widgetKey : (ExecutionHelper.createFrameKey(location, createDefaultFrameKey(widgetContext, defaultContext)));
          
          DmfComponent content = wrapComponentIfNeeded(widgetComponent, widgetContext);
          
          frame = InternalFrame.create(widgetFrameKey, titleInCommand, content, true, false, location, dashboard, Docs.LS_WIDGETS, connector, dhInfo);
          
          widgetComponent.setInternalFrame(frame);
          
          frame.setFrameIcon(ResourceManager.getImageIcon(Icons.FR_WIDGET));
          
          final String eventLogKey = widgetFrameKey + EVENT_LOG_FRAME_KEY_SUFFIX;
          
          addShutdownListenerToFrame(eventLogKey, iop);
          
          if (editingAllowed)
          {
            JideButton editButton = new JideButton(ResourceManager.getImageIcon(Icons.FR_EDIT));
            
            editButton.setButtonStyle(ButtonStyle.TOOLBAR_STYLE);
            editButton.setDefaultForeground(new Color(0, 0, 0, 0));
            editButton.setOpaque(false);
            
            editButton.setToolTipText(Cres.get().getString("edit"));
            editButton.addActionListener(new ActionListener()
            {
              @Override
              public void actionPerformed(ActionEvent e)
              {
                iop.removeInterruptionListener(interruptionListener);
                ClientUtils.removeFrame(frame);
                final DataTable parameters = defaultContext != null ? new SimpleDataTable(EditWidgetTemplateFormat.AFT_EDIT_TEMPLATE, defaultContext.getPath()) : null;
                InvokeActionOperation.invoke(WidgetContextConstants.A_EDIT_TEMPLATE, widgetContext, iop.getConnector(), parameters);
              }
            });
            frame.getToolbar().add(editButton);
          }
          
          JideButton eLogButton = new JideButton(ResourceManager.getImageIcon(Icons.FR_WIDGET_EVENT_LOG));
          eLogButton.setButtonStyle(ButtonStyle.TOOLBAR_STYLE);
          eLogButton.setDefaultForeground(new Color(0, 0, 0, 0));
          eLogButton.setOpaque(false);
          
          eLogButton.setToolTipText(Cres.get().getString("wOpenEventLog"));
          eLogButton.addActionListener(new ActionListener()
          {
            @Override
            public void actionPerformed(ActionEvent e)
            {
              InternalFrame elFrame = ClientUtils.findFrame(eventLogKey);
              boolean firstTime = elFrame == null;
              if (firstTime)
              {
                final WindowLocation logLocation = new WindowLocation(InternalFrame.SIZE_WIDGET_EVENT_LOG);
                
                elFrame = new InternalFrame(eventLogKey, Cres.get().getString("elEventLog"), logLocation, null, true, true, Docs.LS_WIDGETS_EVENT_LOG, connector);
              }
              
              DefaultContextManager cm = widgetComponent.getEngine().getViewer().getComponentContextManager();
              EventLog el = AbstractAggreGateIDE.createWidgetEventLog(cm);
              el.start(false);
              
              elFrame.getContentPane().setLayout(new GridBagLayout());
              JScrollPane elScrollPane = new JScrollPane(el, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
              GridBagConstraints constraints = new GridBagConstraints();
              constraints.fill = GridBagConstraints.BOTH;
              constraints.weightx = 1;
              constraints.weighty = 1;
              elFrame.getContentPane().removeAll();
              elFrame.getContentPane().add(elScrollPane, constraints);
              elFrame.revalidate();
              
              if (firstTime)
              {
                ComponentHelper.getMainFrame().addElement(connector, elFrame);
              }
              
              ComponentHelper.getMainFrame().getDockingManager().showFrame(eventLogKey);
            }
          });
          frame.getToolbar().add(eLogButton);
          
          if (dashboard != null && dashboard.getName() != null)
          {
            ComponentHelper.getMainFrame().getDockingManager().activateFrame(dashboard.getName());
          }
          
          Log.WIDGETS.debug("Started widget in " + (System.currentTimeMillis() - startTime) + " ms");
        }
        catch (Exception ex)
        {
          ClientUtils.showError(Level.ERROR, ex);
        }
      }
    });
    
    return new GenericActionResponse(null, false, requestId);
  }
  
  protected SwingWidget createSwingWidgetComponent(String title, ContextManager contextManager, Context widgetContext, Context defaultContext, CallerController caller, String template,
      DataTable input, Map<String, Object> environment, String uniqueScriptPrefix, RemoteConnector connector)
  {
    return new SwingWidget(title, ComponentHelper.getMainFrame().getFrame(), contextManager, new Pair<Context, Context>(widgetContext, defaultContext), caller, template, input, environment,
        uniqueScriptPrefix, connector, ACTION_EXECUTOR, scriptCompiler);
    
  }
  
  protected DmfComponent wrapComponentIfNeeded(SwingWidget component, Context widgetContext)
  {
    return component;
  }
  
  protected void addShutdownListenerToFrame(final String elKey, final InvokeActionOperation iop)
  {
    frame.addShutdownListener(new FrameShutdownListener(elKey, iop));
  }
  
  @Override
  public void cancel()
  {
    if (frame != null)
    {
      ClientUtils.removeFrame(frame);
    }
  }
  
  private String createDefaultFrameKey(Context widgetContext, Context defaultContext)
  {
    // Previously, we've generated an unique key in the end of this method. But so far it seems that we need to close previous copy of a widget if it's reopened for the same context.
    return ClientUtils.createWidgetFrameKey(widgetContext != null ? widgetContext.getPath() : null, defaultContext != null ? defaultContext.getPath() : null);
  }
  
  protected final Runnable interruptionListener = new Runnable()
  {
    @Override
    public void run()
    {
      if (frame != null)
      {
        ClientUtils.removeFrame(frame);
      }
    }
  };
  
  class FrameShutdownListener implements ShutdownListener
  {
    final private String elKey;
    final private InvokeActionOperation iop;
    
    public FrameShutdownListener(String elKey, InvokeActionOperation iop)
    {
      this.elKey = elKey;
      this.iop = iop;
    }
    
    @Override
    public void shutdown()
    {
      frame.removeShutdownListener(this);
      
      if (iop != null)
      {
        iop.removeInterruptionListener(interruptionListener);
      }
      
      InternalFrame el = ClientUtils.findFrame(elKey);
      if (el != null)
      {
        ClientUtils.removeFrame(el);
      }
    }
    
  }
}
