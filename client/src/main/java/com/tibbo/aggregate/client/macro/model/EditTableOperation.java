package com.tibbo.aggregate.client.macro.model;

public class EditTableOperation extends AbstractAtomicStep<TableOperationStep> implements ActionStep
{
  private String propertyName;
  private String actionRequestId;
  
  public EditTableOperation()
  {
  }
  
  public EditTableOperation(String actionRequestId, String propertyName)
  {
    this.actionRequestId = actionRequestId;
    this.propertyName = propertyName;
  }
  
  public String getPropertyName()
  {
    return propertyName;
  }
  
  public String getActionRequestId()
  {
    return actionRequestId;
  }
  
  public void setPropertyName(String propertyName)
  {
    this.propertyName = propertyName;
  }
  
  public void setActionRequestId(String actionRequestId)
  {
    this.actionRequestId = actionRequestId;
  }
  
  public String toString()
  {
    return "Edit table" + (propertyName == null ? "" : " for property '" + propertyName + "'");
  }
  
  public boolean stepEquals(Object o)
  {
    return (o instanceof EditTableOperation);
  }
  
  public int hashCode()
  {
    return getClass().hashCode();
  }
}
