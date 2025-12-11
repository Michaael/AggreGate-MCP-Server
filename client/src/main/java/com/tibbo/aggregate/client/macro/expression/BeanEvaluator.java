package com.tibbo.aggregate.client.macro.expression;

import java.beans.*;
import java.lang.reflect.*;
import java.util.*;


import com.tibbo.aggregate.common.expression.*;
import com.tibbo.aggregate.common.util.*;

public class BeanEvaluator
{
  private Evaluator evaluator;
  
  public BeanEvaluator(Evaluator evaluator)
  {
    this.evaluator = evaluator;
  }
  
  public void evaluate(Object bean, Map<String, String> expressions)
  {
    if (expressions == null)
    {
      return;
    }
    
    try
    {
      BeanInfo info = Introspector.getBeanInfo(bean.getClass());
      
      PropertyDescriptor[] pds = info.getPropertyDescriptors();
      
      for (int i = 0; i < pds.length; i++)
      {
        PropertyDescriptor pd = pds[i];
        
        String name = pd.getName();
        
        if (expressions.containsKey(name))
        {
          String expr = expressions.get(name);
          
          evaluateProperty(bean, pd, expr);
        }
      }
    }
    catch (Exception ex)
    {
      throw new IllegalStateException("Error evaluating bean", ex);
    }
  }
  
  private void evaluateProperty(Object bean, PropertyDescriptor pd, String expr) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException, SyntaxErrorException,
      EvaluationException
  {
    com.tibbo.aggregate.common.expression.Expression expression = expr == null ? null : new com.tibbo.aggregate.common.expression.Expression(expr);
    
    Object evaluatedValue = expr == null ? null : evaluator.evaluate(expression);
    
    Method writer = pd.getWriteMethod();
    
    writer.invoke(bean, evaluatedValue);
  }
}
