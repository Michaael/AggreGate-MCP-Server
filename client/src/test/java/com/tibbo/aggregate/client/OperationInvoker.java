package com.tibbo.aggregate.client;

import java.util.*;

import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.macro.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.component.systemtree.*;
import com.tibbo.aggregate.component.systemtree.context.*;

public class OperationInvoker implements SystemTreeListener
{
  private HashMap<Operation, NonInteractiveExecutor> operationsToExecutors = new HashMap();
  
  public synchronized NonInteractiveExecutor invokeOperation(SystemTreeContext ctx, String operationName)
  {
    if (ctx == null)
    {
      throw new IllegalArgumentException("Context is null");
    }
    if (operationName == null)
    {
      throw new IllegalArgumentException("operationName is null");
    }
    
    PlatformEventMulticaster.addListener(this);
    
    Operation targetOperation = null;
    
    Collection<Operation> operations = ctx.getOperations();
    for (Operation op : operations)
    {
      if (operationName.equals(op.getName()))
      {
        targetOperation = op;
        break;
      }
    }
    
    if (targetOperation == null)
    {
      throw new IllegalArgumentException("No operation '" + operationName + "' in context '" + ctx.getPath() + "'");
    }
    
    if (operationsToExecutors.get(targetOperation) != null)
    {
      throw new IllegalStateException("Operation is already in progress");
    }
    
    NonInteractiveExecutor testExecutor = new NonInteractiveExecutor();
    
    operationsToExecutors.put(targetOperation, testExecutor);
    
    final Operation finalOp = targetOperation;
    
    new Thread(new Runnable()
    {
      public void run()
      {
        finalOp.execute();
      }
    }).start();
    
    return testExecutor;
  }
  
  public synchronized void operationInvoked(SystemTreeEvent e)
  {
    NonInteractiveExecutor testExecutor = operationsToExecutors.get(e.getOperation());
    if (testExecutor == null)
    {
      // Operation does not correspond for this OperationInvoker
      return;
    }
    
    ExecutionHelper.registerExecutor(e.getOperation(), testExecutor);
  }
  
  public void dndPerformed(SystemTreeEvent e)
  {
  }
  
  public synchronized void operationFinished(SystemTreeEvent e)
  {
    NonInteractiveExecutor testExecutor = operationsToExecutors.get(e.getOperation());
    
    if (testExecutor == null)
    {
      // Operation does not correspond for this OperationInvoker
      return;
    }
    ExecutionHelper.unregisterExecutor(e.getOperation(), testExecutor);
    
    operationsToExecutors.remove(e.getOperation());
    
    PlatformEventMulticaster.removeListener(this);
  }
}
