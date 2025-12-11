package com.tibbo.aggregate.client.action.executor;

import javax.swing.*;

import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.gui.dashboard.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.action.command.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.server.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.component.*;

public class ActivateDashboardExecutor extends AbstractCommandExecutor
{
  public ActivateDashboardExecutor()
  {
    super(ActionUtils.CMD_ACTIVATE_DASHBOARD);
  }
  
  @Override
  public GenericActionResponse execute(Operation originator, GenericActionCommand cmd)
  {
    DataTable parameters = cmd.getParameters();
    
    final String name = parameters.rec().getString(ActivateDashboard.CF_NAME);
    
    final String path = parameters.getFormat().hasField(ActivateDashboard.CF_PATH) ? parameters.rec().getString(ActivateDashboard.CF_PATH) : null;
    
    final DataTable actionParameters = parameters.getFormat().hasField(ActivateDashboard.CF_ACTION_PARAMETERS) ? parameters.rec().getDataTable(ActivateDashboard.CF_ACTION_PARAMETERS) : null;
    
    // Set Default Context. Get context from action parameters, if present
    // See OpenDashboardAction.java, if (ctx != null)actionParameters.setValue(DashboardUtils.FIELD_DASHBOARD_DEFAULT_CONTEXT, ctx.getPath());
    String defaultContext = null;
    if (actionParameters != null && actionParameters.hasField(DashboardUtils.FIELD_DASHBOARD_DEFAULT_CONTEXT))
    {
      defaultContext = actionParameters.rec().getString(DashboardUtils.FIELD_DASHBOARD_DEFAULT_CONTEXT);
    }
    else if (parameters.getFormat().hasField(ActivateDashboard.CF_DEFAULT_CONTEXT))
    {
      defaultContext = parameters.rec().getString(ActivateDashboard.CF_DEFAULT_CONTEXT);
    }
    
    DataTable locationData = parameters.getFormat().hasField(ActivateDashboard.CF_LOCATION) ? parameters.rec().getDataTable(ActivateDashboard.CF_LOCATION) : null;
    final WindowLocation location = locationData != null ? new WindowLocation(locationData.rec()) : null;
    
    DataTable dashboardData = parameters.getFormat().hasField(ActivateDashboard.CF_DASHBOARD) ? parameters.rec().getDataTable(ActivateDashboard.CF_DASHBOARD) : null;
    final DashboardProperties dashboard = dashboardData != null ? new DashboardProperties(dashboardData.rec()) : null;
    
    try
    {
      open(cmd, (InvokeActionOperation) originator, name, path, location, dashboard, actionParameters, defaultContext);
    }
    catch (Exception ex)
    {
      Log.DASHBOARDS.error("Error opening dashboard", ex);
    }
    
    activate(name, path);
    
    return new GenericActionResponse(null, false, cmd.getRequestId());
  }
  
  private void open(final GenericActionCommand cmd, final InvokeActionOperation iop, final String name, final String path, final WindowLocation location, final DashboardProperties dp,
      final DataTable actionParameters, String defaultContext) throws ContextException
  {
    RemoteConnector connector = iop.getConnector();
    if (connector == null)
    {
      Log.DASHBOARDS.debug("No connector specified");
      return;
    }
    
    final Context dashboardContext = connector.getContextManager().get(path);
    
    final Context defaultDashboardContext = connector.getContextManager().get(defaultContext);
    
    if (dashboardContext == null)
    {
      Log.DASHBOARDS.debug("No dashboard context found for the path " + path);
      return;
    }
    
    SwingUtilities.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        open(iop, name, path, location, dashboardContext, dp, actionParameters, defaultDashboardContext);
      }
    });
  }
  
  private void open(InvokeActionOperation iop, String name, String dashboardContextPath, WindowLocation location, Context dashboardContext, DashboardProperties dp, DataTable actionParameters,
      Context defaultDashboardContext)
  {
    if (location == null)
    {
      return;
    }
    
    RemoteConnector connector = iop.getConnector();
    
    Integer dashboardNumber = DashboardUtils.getDashboardNumber(actionParameters);
    
    String target = DashboardUtils.getTarget(actionParameters);
    String targetDashboard = DashboardUtils.getTargetDashboard(actionParameters);
    
    if (StringUtils.isEmpty(name) && !StringUtils.isEmpty(target) && dashboardContext != null)
    {
      DataRecord input = new DataRecord(DashboardUtils.REDIRECT_FORMAT);
      input.setValue(DashboardUtils.FIELD_TARGET, target);
      input.setValue(DashboardUtils.FIELD_TARGET_DASHBOARD, dashboardContext.getPath());
      input.setValue(DashboardUtils.FIELD_DASHBOARD_DEFAULT_CONTEXT, defaultDashboardContext.getPath());
      
      AbstractDashboard rootDashboard = (AbstractDashboard) ComponentHelper.getMainFrame().getDashboard(targetDashboard);
      final Context rootDashboardContext = connector.getContextManager().get(rootDashboard.getContextPath());
      
      InvokeActionOperation.invoke(DashboardContextConstants.A_OPEN, rootDashboardContext, connector, input.wrap());
      
      return;
    }
    
    WindowPath wp = new WindowPath(
        name + WindowPath.SEPARATOR + (location == null || StringUtils.isEmpty(location.getKey()) ? ContextUtils.contextPathToContextName(dashboardContextPath) : location.getKey())
            + (dashboardNumber != null && dashboardNumber > 0 ? "_" + dashboardNumber : ""));
    
    ClientDashboard rootDashboard = ComponentHelper.getMainFrame().getDashboard(wp.getRootSegment());
    
    if (rootDashboard == null)
    {
      rootDashboard = ComponentHelper.getMainFrame().createDashboard(dp, connector);
    }
    
    DataTable input = DashboardUtils.createOpenDashboardInput(wp, dp.getElementId(), DashboardUtils.getDashboardsHierarchyInfo(actionParameters), dashboardNumber, target, targetDashboard,
        defaultDashboardContext);
    
    DataTable newParams = input;
    
    if (actionParameters != null)
    {
      TableFormat newFormat = input.getFormat().clone();
      
      for (FieldFormat ff : actionParameters.getFormat().getFields())
      {
        if (!newFormat.hasField(ff.getName()))
        {
          newFormat.addField(ff);
        }
      }
      
      newParams = new SimpleDataTable(newFormat);
      DataTableReplication.copy(actionParameters, newParams);
      DataTableReplication.copy(input, newParams);
      Log.WIDGETS.debug(newParams);
    }
    
    InvokeActionOperation.invoke(DashboardContextConstants.A_OPEN, dashboardContext, connector, newParams);
  }
  
  private void activate(final String name, final String path)
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        ClientDashboard dashboard = ComponentHelper.getMainFrame().getDashboard(name);
        if (dashboard != null)
        {
          dashboard.hasStarted();
        }
        else if (path != null)
        {
          DashboardHelper.resetNestedDashboards(path);
        }
        ComponentHelper.getMainFrame().getDockingManager().activateFrame(name);
      }
    });
  }
}
