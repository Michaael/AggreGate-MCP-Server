package com.tibbo.aggregate.client.macro.model;

public class ShowDescriptionOperation extends AbstractStep implements ActionStep, TableOperationStep
{
  private String actionRequestId;
  
  public ShowDescriptionOperation()
  {
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
    return "Show description";
  }
  
  public boolean isDescription()
  {
    return true;
  }
}
