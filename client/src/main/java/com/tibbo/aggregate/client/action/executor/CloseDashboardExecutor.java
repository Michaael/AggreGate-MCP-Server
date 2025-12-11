package com.tibbo.aggregate.client.action.executor;

import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.gui.dashboard.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.action.command.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.component.*;

public class CloseDashboardExecutor extends AbstractCommandExecutor
{
  public CloseDashboardExecutor()
  {
    super(ActionUtils.CMD_CLOSE_DASHBOARD);
  }
  
  @Override
  public GenericActionResponse execute(Operation originator, GenericActionCommand cmd)
  {
    if (cmd.getParameters().rec() == null)
      return null;
    
    DataTable dashPropertiesTable = cmd.getParameters().rec().getDataTable(CloseDashboard.CF_DASHBOARD);
    String dashName = dashPropertiesTable.rec().getString(DashboardProperties.FIELD_NAME);
    String targetElement = cmd.getParameters().rec().getString(CloseDashboard.CF_TARGET_ELEMENT);
    Boolean closeAll = cmd.getParameters().rec().getBoolean(CloseDashboard.CF_CLOSE_ALL);
    Boolean deepSearch = cmd.getParameters().rec().getBoolean(CloseDashboard.CF_DEEP_SEARCH);
    ClientDashboard clientDashboard = ComponentHelper.getMainFrame().getDashboard(dashName);
    
    if (clientDashboard != null)
    {
      boolean removed = false;
      if (clientDashboard.getElementNames().contains(targetElement))
      {
        removed = clientDashboard.removeElement(targetElement);
      }
      else if (deepSearch)
      {
        removed = removeElementByID(clientDashboard, targetElement);
      }
      
      if (!removed && closeAll)
      {
        closeAllElements(clientDashboard, dashName);
      }
    }
    
    return null;
  }
  
  private void closeAllElements(ClientDashboard clientDashboard, String dashName)
  {
    for (String key : clientDashboard.getElementNames())
    {
      Log.DASHBOARDS.debug("Removing element with key " + key + " from dashboard " + dashName);
      clientDashboard.removeElement(key);
    }
  }
  
  private boolean removeElementByID(ClientDashboard clientDashboard, String targetElement)
  {
    for (String elementName : clientDashboard.getElementNames())
    {
      if (clientDashboard.getElement(elementName).getId().equals(targetElement))
      {
        Log.DASHBOARDS.debug("Removing element with key " + elementName + " from dashboard " + targetElement);
        return clientDashboard.removeElement(elementName);
      }
    }
    return false;
  }
  
}
