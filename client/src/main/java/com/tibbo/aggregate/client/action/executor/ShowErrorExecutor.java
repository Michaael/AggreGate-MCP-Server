package com.tibbo.aggregate.client.action.executor;

import org.apache.log4j.*;

import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.client.util.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.action.command.ShowError;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.util.*;

public class ShowErrorExecutor extends AbstractCommandExecutor
{
  public ShowErrorExecutor()
  {
    super(ActionUtils.CMD_SHOW_ERROR);
  }
  
  public GenericActionResponse execute(Operation originator, GenericActionCommand cmd)
  {
    String title = cmd.getTitle();
    
    DataRecord rec = cmd.getParameters().rec();
    String message = rec.getString(ShowError.CF_MESSAGE);
    int level = rec.getInt(ShowError.CF_LEVEL);
    String details = rec.getString(ShowError.CF_EXCEPTION);
    
    ContextException ex = new ContextException(message, null, details);
    try
    {
      ClientUtils.showErrorAndWait(Level.toLevel(Log4jLevelHelper.getLog4jLevelByAggreGateLevel(level)), title, null, ex);
    }
    catch (Exception e)
    {
      Log.CONTEXT_ACTIONS.error(e.getMessage(), e);
    }
    
    return new GenericActionResponse(null, false, cmd.getRequestId());
  }
  
}
