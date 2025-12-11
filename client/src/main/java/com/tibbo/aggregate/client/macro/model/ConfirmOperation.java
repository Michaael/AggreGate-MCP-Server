package com.tibbo.aggregate.client.macro.model;

import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.action.*;

public class ConfirmOperation extends AbstractStep implements ActionStep
{
  private String message;
  private int option;
  private String actionRequestId;
  
  public ConfirmOperation()
  {
  }
  
  public ConfirmOperation(String actionRequestId, String message, int option)
  {
    this.actionRequestId = actionRequestId;
    this.message = message;
    this.option = option;
  }
  
  public String getMessage()
  {
    return message;
  }
  
  public int getOption()
  {
    return option;
  }
  
  public String getActionRequestId()
  {
    return actionRequestId;
  }
  
  public void setMessage(String message)
  {
    this.message = message;
  }
  
  public void setOption(int option)
  {
    this.option = option;
  }
  
  public void setActionRequestId(String actionRequestId)
  {
    this.actionRequestId = actionRequestId;
  }
  
  public static String getTitleForOption(int option)
  {
    switch (option)
    {
      case ActionUtils.YES_OPTION:
        return Cres.get().getString("ok") + "/" + Cres.get().getString("yes");
      case ActionUtils.NO_OPTION:
        return Cres.get().getString("no");
      case ActionUtils.CANCEL_OPTION:
        return Cres.get().getString("cancel");
      case ActionUtils.CLOSED_OPTION:
        return Cres.get().getString("close");
      default:
        return null;
    }
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("On '");
    sb.append(message);
    sb.append("' choose '");
    sb.append(getTitleForOption(option));
    sb.append("'");
    return sb.toString();
  }
}
