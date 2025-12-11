package com.tibbo.aggregate.client.action.executor;

import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.gui.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.action.command.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.component.*;

public class ConfirmExecutor extends AbstractCommandExecutor
{
  private String message;
  private int option;
  
  public ConfirmExecutor()
  {
    super(ActionUtils.CMD_CONFIRM);
  }
  
  public GenericActionResponse execute(Operation originator, GenericActionCommand cmd)
  {
    DataTable params = cmd.getParameters();
    
    String title = cmd.getTitle();
    
    message = params.rec().getString(Confirm.CF_MESSAGE);
    
    int optionType = params.rec().getInt(Confirm.CF_OPTION_TYPE);
    int messageType = params.rec().getInt(Confirm.CF_MESSAGE_TYPE);
    boolean batchMember = cmd.isBatchEntry();
    
    OptionPane.ConfirmationResult result = OptionPane.showConfirmDialog(ComponentHelper.getMainFrame(), message, title, optionType, messageType, batchMember && cmd.getRequestId() != null);
    
    option = result.getOption();
    
    DataTable respTable = new DataRecord(Confirm.RFT_CONFIRM).addInt(option).wrap();
    
    return new GenericActionResponse(respTable, result.isApplyToAll(), cmd.getRequestId());
  }
  
  public int getOption()
  {
    return option;
  }
  
  public String getMessage()
  {
    return message;
  }
}
