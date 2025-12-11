package com.tibbo.aggregate.client.action.executor;

import javax.swing.*;

import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.gui.*;
import com.tibbo.aggregate.client.gui.frame.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.client.util.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.action.command.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.resource.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.component.eventlog.*;

public class ShowEventLogExecutor extends AbstractCommandExecutor
{
  private static final String EVENT_LOG_FRAME_KEY_PREFIX = "eventlog_";
  private InternalFrame frame;
  
  public ShowEventLogExecutor()
  {
    super(ActionUtils.CMD_SHOW_EVENT_LOG);
  }
  
  @Override
  public void cancel()
  {
    if (frame != null)
    {
      ClientUtils.removeFrame(frame);
    }
  }
  
  @Override
  public GenericActionResponse execute(Operation originator, final GenericActionCommand cmd)
  {
    if (!(originator instanceof InvokeActionOperation))
    {
      throw new IllegalArgumentException("Unsupported originator class: " + originator.getClass() + " required " + InvokeActionOperation.class.getName());
    }
    
    final InvokeActionOperation iop = (InvokeActionOperation) originator;
    
    iop.addInterruptionListener(interruptionListener);
    
    DataRecord rec = cmd.getParameters().rec();
    
    final String eventFilter = rec.getString(ShowEventLog.CF_EVENT_FILTER);
    
    final EntityList eventList = new EntityList(rec.getDataTable(ShowEventLog.CF_EVENT_LIST));
    
    final DataTable filterParameters = rec.getDataTable(ShowEventLog.CF_FILTER_PARAMETERS);
    
    DataTable locationData = rec.getDataTable(ShowEventLog.CF_LOCATION);
    final WindowLocation location = locationData != null ? new WindowLocation(locationData.rec()) : new WindowLocation();
    
    location.applyDefaultSize(InternalFrame.SIZE_EVENT_LOG);
    
    DataTable dashboardData = rec.hasField(ShowEventLog.CF_DASHBOARD) ? rec.getDataTable(ShowEventLog.CF_DASHBOARD) : null;
    final DashboardProperties dashboard = dashboardData != null ? new DashboardProperties(dashboardData.rec()) : null;
    
    DataTable dhInfoData = rec.hasField(ShowEventLog.CF_DASHBOARDS_HIERARCHY_INFO) ? rec.getDataTable(ShowEventLog.CF_DASHBOARDS_HIERARCHY_INFO) : null;
    final DashboardsHierarchyInfo dhInfo = dhInfoData != null ? new DashboardsHierarchyInfo(dhInfoData.rec()) : null;
    
    final String key = rec.getFormat().hasField(ShowEventLog.CF_KEY) ? rec.getString(ShowEventLog.CF_KEY) : null;
    
    final EventLog eventLog;
    
    final Boolean showRealTime = rec.getBoolean(ShowEventLog.CF_SHOW_REALTIME);
    final Boolean showHistory = rec.getBoolean(ShowEventLog.CF_SHOW_HISTORY);
    final Boolean showContexts = rec.getBoolean(ShowEventLog.CF_SHOW_CONTEXTS);
    final Boolean showNames = rec.getBoolean(ShowEventLog.CF_SHOW_NAMES);
    final Boolean showLevels = rec.getBoolean(ShowEventLog.CF_SHOW_LEVELS);
    final Boolean showData = rec.getBoolean(ShowEventLog.CF_SHOW_DATA);
    final Boolean showAcknowledgements = rec.getBoolean(ShowEventLog.CF_SHOW_ACKNOWLEDGEMENTS);
    final Boolean showEnrichments = rec.getBoolean(ShowEventLog.CF_SHOW_ENRICHMENTS);
    final Integer customListenerCode = rec.getInt(ShowEventLog.CF_CUSTOM_LISTENER_CODE);
    final String defaultContext = rec.hasField(ShowEventLog.CF_DEFAULT_CONTEXT) ? rec.getString(ShowEventLog.CF_DEFAULT_CONTEXT) : null;
    final String className = rec.hasField(ShowEventLog.CF_CLASS_NAME) ? rec.getString(ShowEventLog.CF_CLASS_NAME) : null;
    final Long instanceId = rec.hasField(ShowEventLog.CF_INSTANCE_ID) ? rec.getLong(ShowEventLog.CF_INSTANCE_ID) : null;
    final String defaultEvent = rec.hasField(ShowEventLog.CF_DEFAULT_EVENT) ? rec.getString(ShowEventLog.CF_DEFAULT_EVENT) : null;
    
    if (eventFilter != null)
    {
      eventLog = new EventLog(iop.getConnector(), new ClientReferredActionExecutor(), eventFilter, showRealTime, showHistory, showContexts, showNames, showLevels, showData, showAcknowledgements,
          showEnrichments, customListenerCode, defaultContext, className, instanceId, defaultEvent);
    }
    else
    {
      eventLog = new EventLog(iop.getConnector(), new ClientReferredActionExecutor(), eventList, showRealTime, showHistory, showContexts, showNames, showLevels, showData, showAcknowledgements,
          showEnrichments, customListenerCode, defaultContext, className, instanceId, defaultEvent);
      
      eventLog.setEventsContext(iop.getContext().get(Contexts.CTX_EVENTS));
    }
    
    final EventLogSection realtime = eventLog.getRealtime();
    if (realtime != null)
    {
      realtime.getModel().setParameters(filterParameters);
    }
    final EventLogSection history = eventLog.getHistory();
    if (history != null)
    {
      history.getModel().setParameters(filterParameters);
    }
    
    eventLog.start(rec.getBoolean(ShowEventLog.CF_PRELOAD_HISTORY));
    
    SwingUtilities.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        String additionalHash = iop.getInvokerContext().isDistributed() ? "_" + iop.getInvokerContext().getPath().hashCode() : "";
        String frameKey = key != null ? key : ExecutionHelper.createFrameKey(location, EVENT_LOG_FRAME_KEY_PREFIX +
            (eventFilter != null ? ContextUtils.contextPathToContextName(eventFilter) : eventList.hashCode() + additionalHash));
        
        frame = InternalFrame.create(frameKey, cmd.getTitle(), eventLog, true, true, location, dashboard, Docs.CL_EVENT_LOG, iop.getConnector(), dhInfo);
        
        frame.setFrameIcon(ResourceManager.getImageIcon(Icons.FR_EVENT_LOG));
        
        frame.addShutdownListener(new ShutdownListener()
        {
          @Override
          public void shutdown()
          {
            if (iop != null)
            {
              iop.removeInterruptionListener(interruptionListener);
            }
          }
        });
      }
    });
    
    Integer listenerCode = eventLog.getHistory() != null ? eventLog.getHistory().getListenerCode() : null;
    
    return new GenericActionResponse(createResponseData(listenerCode), false, cmd.getRequestId());
  }
  
  private static DataTable createResponseData(Integer listenerCode)
  {
    DataTable respTable = new SimpleDataTable(ShowEventLog.RFT_SHOW_EVENT_LOG);
    respTable.addRecord().addInt(listenerCode);
    return respTable;
  }
  
  private final Runnable interruptionListener = new Runnable()
  {
    @Override
    public void run()
    {
      SwingUtilities.invokeLater(new Runnable()
      {
        
        @Override
        public void run()
        {
          if (frame != null)
          {
            ClientUtils.removeFrame(frame);
          }
        }
      });
    }
  };
}
