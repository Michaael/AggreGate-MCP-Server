package com.tibbo.aggregate.client.macro.model;

import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.context.*;

public class EventContext
{
  private String context;
  private Operation operation;
  private GenericActionCommand actionCommand;
  private String propertyName;
  
  public EventContext(Operation operation)
  {
    this(operation, null);
  }
  
  public EventContext(Operation operation, GenericActionCommand actionCommand)
  {
    this(operation, actionCommand, null);
  }
  
  public EventContext(Operation operation, GenericActionCommand actionCommand, String propertyName)
  {
    this.operation = operation;
    this.actionCommand = actionCommand;
    this.propertyName = propertyName;
  }
  
  public Operation getOperation()
  {
    return operation;
  }
  
  public ActionCommand getActionCommand()
  {
    return actionCommand;
  }
  
  public String getPropertyName()
  {
    return propertyName;
  }
  
  public String getContext()
  {
    return context;
  }
  
  public String toString()
  {
    StringBuffer s = new StringBuffer();
    s.append("op: ");
    if (operation != null)
    {
      Context invokerContext = operation.getInvokerContext();
      if (invokerContext != null)
      {
        s.append(invokerContext.getPath()).append(":");
      }
      s.append(operation.getDescription());
    }
    s.append(" cmd: ");
    if (actionCommand != null)
    {
      s.append(actionCommand.getTitle());
      s.append("[");
      s.append(actionCommand.getType());
      s.append("]");
    }
    
    if (propertyName != null)
    {
      s.append(" prop: ");
      s.append(propertyName);
    }
    
    return s.toString();
  }
  
}
