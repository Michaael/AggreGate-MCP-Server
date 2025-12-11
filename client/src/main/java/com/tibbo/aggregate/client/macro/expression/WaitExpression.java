package com.tibbo.aggregate.client.macro.expression;

import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.expression.*;

public class WaitExpression implements MacroFunction
{
  private static final String F_WAIT = "wait";
  
  private static final TableFormat FIFT_WAIT = new TableFormat();
  static
  {
    FIFT_WAIT.addField("<expression><S>");
    FIFT_WAIT.addField("<timeoutSec><I><F=O>");
  }
  
  private static final TableFormat FOFT_WAIT = FieldFormat.create("<status><B>").wrap();
  
  public static final int DEFAULT_TIMEOUT_SEC = 5;
  public static final int DEFAULT_INTERVAL_SEC = 5;
  
  public Boolean execute(ContextManager ctxManager, Object... params)
  {
    if (params[0] instanceof Boolean)
    {
      return (Boolean) params[0];
    }
    else if (params[0] instanceof String)
    {
      String expr = (String) params[0];
      Expression expression = new Expression(expr);
      
      int timeoutSec = DEFAULT_TIMEOUT_SEC;
      if (params.length > 1)
      {
        if (params[1] instanceof Number)
        {
          timeoutSec = ((Number) params[1]).intValue();
        }
        else
        {
          throw new IllegalArgumentException("Timeout must be an integer. Found " + params[1]);
        }
      }
      
      DefaultReferenceResolver res = new DefaultReferenceResolver();
      res.setContextManager(ctxManager);
      Evaluator e = new Evaluator(res);
      
      try
      {
        int repeatCount = timeoutSec / DEFAULT_INTERVAL_SEC + 1;
        for (int i = 0; i < repeatCount; i++)
        {
          Object value = e.evaluate(expression);
          if (value instanceof Boolean)
          {
            boolean val = (Boolean) value;
            if (val)
            {
              return val;
            }
          }
          else
          {
            throw new IllegalArgumentException("Expression '" + expr + "' returned non-boolean result: " + value);
          }
          
          try
          {
            Thread.sleep(1000 * DEFAULT_INTERVAL_SEC);
          }
          catch (InterruptedException ex)
          {
            throw new RuntimeException(ex);
          }
        }
        
        return false;
      }
      catch (Exception ex)
      {
        throw new IllegalArgumentException(ex);
      }
    }
    else
    {
      throw new IllegalArgumentException("Unsupported expression: " + params[0]);
    }
  }
  
  public FunctionDefinition getFunctionDefinition()
  {
    return new FunctionDefinition(F_WAIT, FIFT_WAIT, FOFT_WAIT, "Check function value is true or false");
  }
}
