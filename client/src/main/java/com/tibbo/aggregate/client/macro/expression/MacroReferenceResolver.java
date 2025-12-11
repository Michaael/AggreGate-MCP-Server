package com.tibbo.aggregate.client.macro.expression;

import java.util.*;

import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.context.*;

import com.tibbo.aggregate.common.expression.*;
import com.tibbo.aggregate.common.util.*;

public class MacroReferenceResolver extends AbstractReferenceResolver
{
  private Map<String, Object> environment;
  private Evaluator evaluator;
  private MacroFunctions functions = new MacroFunctions();
  private ContextManager ctxManager;
  
  public MacroReferenceResolver(ContextManager ctxManager, Map<String, Object> environment)
  {
    this.environment = environment;
    this.ctxManager = ctxManager;
  }
  
  public Object resolveReference(Reference ref, EvaluationEnvironment resolvingEnvironment) throws SyntaxErrorException, EvaluationException, ContextException
  {
    if (ref.getEntityType() == ContextUtils.ENTITY_FUNCTION)
    {
      MacroFunction f = functions.getFunction(ref.getEntity());
      
      if (f == null)
      {
        throw new IllegalStateException("Function '" + ref.getEntity() + "' is not defined");
      }
      
      List params = resolveParameters(ref.getParameters(), resolvingEnvironment);
      
      return f.execute(ctxManager, params.toArray());
    }
    
    if (environment == null)
    {
      throw new IllegalStateException(Cres.get().getString("exprEnvNotDefined"));
    }
    
    return environment.get(ref.getField());
  }
  
  private List resolveParameters(List parameters, EvaluationEnvironment environment) throws SyntaxErrorException, EvaluationException
  {
    List params = new LinkedList();
    for (Object param : parameters)
    {
      if (param instanceof Expression)
      {
        if (evaluator == null)
        {
          throw new IllegalStateException("Evaluator not defined");
        }
        
        params.add(evaluator.evaluate((Expression) param, environment));
      }
      else
      {
        params.add(param);
      }
    }
    return params;
  }
  
  public void setEvaluator(Evaluator evaluator)
  {
    this.evaluator = evaluator;
  }
  
  public Evaluator getEvaluator()
  {
    return evaluator;
  }
}
