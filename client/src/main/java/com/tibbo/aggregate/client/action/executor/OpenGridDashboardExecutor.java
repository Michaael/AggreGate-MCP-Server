package com.tibbo.aggregate.client.action.executor;

import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.common.action.*;

public class OpenGridDashboardExecutor extends AbstractCommandExecutor
{
  public OpenGridDashboardExecutor()
  {
    super(ActionUtils.CMD_OPEN_GRID_DASHBOARD);
  }
  
  @Override
  public GenericActionResponse execute(Operation originator, GenericActionCommand cmd)
  {
    // do nothing
    return null;
  }
  
}
