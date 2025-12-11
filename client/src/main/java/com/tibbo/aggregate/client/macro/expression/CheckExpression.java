package com.tibbo.aggregate.client.macro.expression;

import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.expression.*;

public class CheckExpression implements MacroFunction
{
  private static final String F_CHECK = "check";
  
  private static final TableFormat FOFT_CHECK = FieldFormat.create("<status><B>").wrap();
  private static final TableFormat FIFT_CHECK = FieldFormat.create("<expression><S>").wrap();
  
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
      
      DefaultReferenceResolver res = new DefaultReferenceResolver();
      res.setContextManager(ctxManager);
      Evaluator e = new Evaluator(res);
      
      try
      {
        Object value = e.evaluate(expression);
        if (value instanceof Boolean)
        {
          return (Boolean) value;
        }
        else
        {
          throw new IllegalArgumentException("Expression '" + expr + "' returned non-boolean result: " + value);
        }
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
    return new FunctionDefinition(F_CHECK, FIFT_CHECK, FOFT_CHECK, "Check function value is true or false");
  }
}
