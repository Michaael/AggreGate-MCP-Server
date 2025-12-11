package com.tibbo.aggregate.client.macro.model;

public class ReportOperation extends AbstractStep implements ActionStep
{
  private String actionRequestId;
  
  public ReportOperation()
  {
  }
  
  public ReportOperation(String actionRequestId)
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
}
