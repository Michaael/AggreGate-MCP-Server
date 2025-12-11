package com.tibbo.aggregate.client.macro.ui;

import com.tibbo.aggregate.client.macro.model.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.context.*;

public class ActionCommandContext
{
  private String context;
  private String operationName;
  private String actionRequestId;
  private String propertyName;
  private boolean finishing;
  
  public ActionCommandContext()
  {
    super();
  }
  
  public String getActionRequestId()
  {
    return actionRequestId;
  }
  
  public String getContext()
  {
    return context;
  }
  
  public String getOperationName()
  {
    return operationName;
  }
  
  public String getPropertyName()
  {
    return propertyName;
  }
  
  public boolean isFinishing()
  {
    return finishing;
  }
  
  public void setActionRequestId(String actionRequestId)
  {
    this.actionRequestId = actionRequestId;
  }
  
  public void setContext(String context)
  {
    this.context = context;
  }
  
  public void setOperationName(String operationName)
  {
    this.operationName = operationName;
  }
  
  public void setPropertyName(String propertyName)
  {
    this.propertyName = propertyName;
  }
  
  public void setFinishing(boolean finishing)
  {
    this.finishing = finishing;
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof ActionCommandContext))
    {
      return false;
    }
    
    return equals((ActionCommandContext) o);
  }
  
  public boolean equals(ActionCommandContext ctx)
  {
    return equals(context, ctx.context) && equals(operationName, ctx.operationName) && equals(propertyName, ctx.propertyName) && finishing == ctx.finishing;
  }
  
  public int hashCode()
  {
    return (context + ":" + operationName + ":" + propertyName + ":" + finishing).hashCode();
  }
  
  public boolean equals(EventContext ctx)
  {
    if (ctx == null)
    {
      return false;
    }
    
    String ctxOpName = null;
    String ctxContext = null;
    
    Operation ctxOp = ctx.getOperation();
    if (ctxOp != null)
    {
      ctxOpName = ctxOp.getName();
      Context ctxCon = ctxOp.getInvokerContext();
      ctxContext = ctxCon == null ? null : ctxCon.getPath();
    }
    
    String ctxReqId = null;
    ActionCommand ctxCmd = ctx.getActionCommand();
    if (ctxCmd != null)
    {
      RequestIdentifier reqId = ctxCmd.getRequestId();
      ctxReqId = reqId == null ? null : reqId.toString();
    }
    
    String ctxProperty = ctx.getPropertyName();
    
    // For this parameter null and empty value mean the same. Server sends nulls but macro text file contains blank lines
    actionRequestId = "".equals(actionRequestId) ? null : actionRequestId;
    ctxReqId = "".equals(ctxReqId) ? null : ctxReqId;
    
    return equals(operationName, ctxOpName) && ContextUtils.masksIntersect(context, ctxContext, false, false) && equals(actionRequestId, ctxReqId) && equals(propertyName, ctxProperty);
  }
  
  private boolean equals(Object a, Object b)
  {
    return a == null ? true : a.equals(b);
  }
  
  public String toString()
  {
    return context + ":" + operationName + " [" + actionRequestId + "] " + propertyName + " end: " + finishing;
  }
}
