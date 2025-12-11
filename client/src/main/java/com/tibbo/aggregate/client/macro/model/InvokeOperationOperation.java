package com.tibbo.aggregate.client.macro.model;

import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.util.*;

public class InvokeOperationOperation extends AbstractAtomicStep<ActionStep>
{
  private String invokerContext;
  private String operationName;
  private String[] sourceContext;
  private String targetContext;
  
  public InvokeOperationOperation()
  {
  }
  
  public InvokeOperationOperation(String invokerContext, String operationName)
  {
    this.invokerContext = invokerContext;
    this.operationName = operationName;
  }
  
  public InvokeOperationOperation(String[] sourceContext, String targetContext, String operationName)
  {
    this.sourceContext = sourceContext;
    this.targetContext = targetContext;
    this.invokerContext = targetContext;
    this.operationName = operationName;
  }
  
  public String getInvokerContext()
  {
    return invokerContext;
  }
  
  public String getOperationName()
  {
    return operationName;
  }
  
  public String[] getSourceContext()
  {
    return sourceContext;
  }
  
  public String getTargetContext()
  {
    return targetContext;
  }
  
  public void setInvokerContext(String invokerContext)
  {
    this.invokerContext = invokerContext;
  }
  
  public void setOperationName(String operationName)
  {
    this.operationName = operationName;
  }
  
  public void setSourceContext(String[] sourceContext)
  {
    this.sourceContext = sourceContext;
  }
  
  public void setTargetContext(String targetContext)
  {
    this.targetContext = targetContext;
  }
  
  public String toString()
  {
    StringBuffer s = new StringBuffer();
    if (sourceContext == null)
    {
      s.append("Do '");
      s.append(operationName);
      s.append("' in '");
      s.append(invokerContext);
      s.append("'");
    }
    else
    {
      s.append("Drag '");
      s.append(StringUtils.print(sourceContext));
      s.append("' to '");
      s.append(targetContext);
      s.append("' [");
      s.append(operationName);
      s.append("]");
    }
    
    return s.toString();
  }
  
  public boolean stepEquals(Object o)
  {
    if (!getClass().isInstance(o))
    {
      return false;
    }
    
    InvokeOperationOperation op = (InvokeOperationOperation) o;
    
    if (operationName == null ? op.operationName != null : !operationName.equals(op.operationName))
    {
      return false;
    }
    
    if (targetContext == null ? op.targetContext != null : !ContextUtils.masksIntersect(targetContext, op.targetContext, false, false))
    {
      return false;
    }
    
    if (sourceContext == null)
    {
      if (!ContextUtils.masksIntersect(invokerContext, op.invokerContext, false, false))
      {
        return false;
      }
    }
    else
    {
      if (op.sourceContext == null || sourceContext.length != op.sourceContext.length)
      {
        return false;
      }
      
      for (int i = 0; i < sourceContext.length; i++)
      {
        if (!ContextUtils.masksIntersect(sourceContext[i], op.sourceContext[i], false, false))
        {
          return false;
        }
      }
    }
    
    return true;
  }
  
  public int hashCode()
  {
    return toString().hashCode();
  }
}
