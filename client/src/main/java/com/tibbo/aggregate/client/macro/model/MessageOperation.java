package com.tibbo.aggregate.client.macro.model;

public class MessageOperation extends AbstractStep implements ActionStep
{
  private String message;
  private String actionRequestId;
  
  public MessageOperation()
  {
  }
  
  public MessageOperation(String actionRequestId, String message)
  {
    this.actionRequestId = actionRequestId;
    this.message = message;
  }
  
  public String getMessage()
  {
    return message;
  }
  
  public String getActionRequestId()
  {
    return actionRequestId;
  }
  
  public void setMessage(String message)
  {
    this.message = message;
  }
  
  public void setActionRequestId(String actionRequestId)
  {
    this.actionRequestId = actionRequestId;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("View message '");
    sb.append(message);
    sb.append("'");
    return sb.toString();
  }
}
