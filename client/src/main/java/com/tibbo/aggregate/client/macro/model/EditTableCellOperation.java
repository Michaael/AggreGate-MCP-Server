package com.tibbo.aggregate.client.macro.model;

public class EditTableCellOperation extends AbstractStep implements TableOperationStep
{
  private String field;
  private String value;
  private int row;
  private String actionRequestId;
  private boolean undefinedValue;
  
  public EditTableCellOperation()
  {
  }
  
  public EditTableCellOperation(String actionRequestId, String field, int row, String value)
  {
    this.actionRequestId = actionRequestId;
    this.field = field;
    this.row = row;
    this.value = value;
  }
  
  public String getField()
  {
    return field;
  }
  
  public String getValue()
  {
    return value;
  }
  
  public int getRow()
  {
    return row;
  }
  
  public String getActionRequestId()
  {
    return actionRequestId;
  }
  
  public boolean isUndefinedValue()
  {
    return undefinedValue;
  }
  
  public void setField(String field)
  {
    this.field = field;
  }
  
  public void setValue(String value)
  {
    this.value = value;
  }
  
  public void setRow(int row)
  {
    this.row = row;
  }
  
  public void setActionRequestId(String actionRequestId)
  {
    this.actionRequestId = actionRequestId;
  }
  
  public void setUndefinedValue(boolean undefinedValue)
  {
    this.undefinedValue = undefinedValue;
  }
  
  public boolean stepEquals(Object o)
  {
    if (!getClass().isInstance(o))
    {
      return false;
    }
    
    EditTableCellOperation op = (EditTableCellOperation) o;
    
    return (field == null ? op.field == null : field.equals(op.field)) && row == op.row && (undefinedValue || op.undefinedValue || (value == null ? op.value == null : value.equals(op.value)));
  }
  
  public int hashCode()
  {
    return (field + '[' + row + "]=" + (undefinedValue ? "?" : value)).hashCode();
  }
  
  public String toString()
  {
    return field + '[' + row + "]=" + value;
  }
}
