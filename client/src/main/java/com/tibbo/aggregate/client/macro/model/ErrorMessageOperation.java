package com.tibbo.aggregate.client.macro.model;

public class ErrorMessageOperation extends AbstractStep implements ActionStep
{
  private String actionRequestId;
  
  public ErrorMessageOperation()
  {
  }
  
  public ErrorMessageOperation(String actionRequestId)
  {
    this.actionRequestId = actionRequestId;
  }
  
  public String getActionRequestId()
  {
    return actionRequestId;
  }
  
  public void setActionRequestId(String actionRequestId)
  {
    this.actionRequestId = actionRequestId;
  }
  
  public String toString()
  {
    return "View error message";
  }
}
