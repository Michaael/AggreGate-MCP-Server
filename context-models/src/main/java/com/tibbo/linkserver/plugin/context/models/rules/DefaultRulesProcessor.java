package com.tibbo.linkserver.plugin.context.models.rules;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Map.Entry;

import com.tibbo.aggregate.common.Log;
import com.tibbo.aggregate.common.context.CallerController;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextManager;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.event.EventLevel;
import com.tibbo.aggregate.common.expression.EvaluationEnvironment;
import com.tibbo.aggregate.common.expression.EvaluationException;
import com.tibbo.aggregate.common.expression.Evaluator;
import com.tibbo.aggregate.common.expression.Expression;
import com.tibbo.aggregate.common.structure.Pinpoint;
import com.tibbo.aggregate.common.util.SyntaxErrorException;
import com.tibbo.aggregate.common.util.Util;
import com.tibbo.linkserver.event.EventHelper;
import com.tibbo.linkserver.plugin.context.models.Lres;
import com.tibbo.linkserver.plugin.context.models.ModelContext;

public abstract class DefaultRulesProcessor implements RulesProcessor
{
  private static final int DEFAULT_CALL_STACK_DEPTH_THRESHOLD = 100;
  
  private static final ThreadLocal<Integer> CALL_STACK_DEPTH_COUNTER = ThreadLocal.withInitial(() -> 0);
  
  private final RuleSet ruleSet;
  
  public DefaultRulesProcessor(RuleSet ruleSet)
  {
    this.ruleSet = ruleSet;
  }
  
  protected RuleSet getRuleSet()
  {
    return ruleSet;
  }
  
  protected Object getVariable(String variable, Evaluator evaluator) throws RuleException
  {
    Map<String, Object> environment = evaluator.getEnvironmentResolver().getEnvironment();
    
    if (!environment.containsKey(variable))
    {
      throw new RuleException(Lres.get().getString("ruleSetVarNotDefined") + variable);
    }
    
    return environment.get(variable);
  }
  
  protected void setVariable(String variable, Object value, Evaluator evaluator) throws RuleException
  {
    evaluator.getEnvironmentResolver().getEnvironment().put(variable, value);
  }
  
  protected boolean isVariableDefined(String variable, Evaluator evaluator)
  {
    return evaluator.getEnvironmentResolver().getEnvironment().containsKey(variable);
  }
  
  protected Evaluator createEvaluator(ContextManager cm, Context target, CallerController caller, DataTable parameters, Map<String, Object> environment)
  {
    Evaluator evaluator = new Evaluator(cm, target, parameters, caller);
    
    if (environment != null)
    {
      for (Entry<String, Object> entry : environment.entrySet())
      {
        evaluator.getEnvironmentResolver().set(entry.getKey(), entry.getValue());
      }
    }
    
    return evaluator;
  }
  
  protected EvaluationEnvironment createEvaluationEnvironment(Evaluator evaluator, Pinpoint pinpoint)
  {
    EvaluationEnvironment evalEnv = new EvaluationEnvironment(evaluator.getEnvironmentResolver().getEnvironment());
    evalEnv.assignPinpoint(pinpoint);
    return evalEnv;
  }
  
  protected boolean checkCondition(Evaluator evaluator, Rule cur, Pinpoint pinpoint) throws SyntaxErrorException, EvaluationException
  {
    String condition = cur.getCondition();
    
    if (!condition.isEmpty())
    {
      Object result = evaluator.evaluate(new Expression(condition), createEvaluationEnvironment(evaluator, pinpoint));
      
      if (result != null)
      {
        return Util.convertToBoolean(result, true, false);
      }
    }
    
    return true;
  }
  
  protected Object processRule(Evaluator evaluator, Rule cur, Pinpoint pinpoint) throws SyntaxErrorException,
      EvaluationException
  {
    return evaluator.evaluate(new Expression(cur.getExpression()), createEvaluationEnvironment(evaluator, pinpoint));
  }
  
  @Override
  public Object process(ContextManager cm, Context target, CallerController caller, DataTable parameters, Map<String, Object> environment) throws RuleException
  {
    boolean callStackDepthCounterIncremented = false;
    try
    {
      callStackDepthCounterIncremented = incrementCallStackDepthCounter(target);
      return doProcess(cm, target, caller, parameters, environment);
    }
    catch (RuleException ex)
    {
      throw ex;
    }
    catch (Exception ex)
    {
      throw new RuleException(ex.getMessage(), ex);
    }
    finally
    {
      if (callStackDepthCounterIncremented)
        decrementCallStackDepthCounter();
    }
  }
  
  protected abstract Object doProcess(ContextManager cm, Context target, CallerController caller, DataTable parameters, Map<String, Object> environment)
      throws RuleException, EvaluationException, SyntaxErrorException;
  
  private boolean incrementCallStackDepthCounter(Context target)
  {
    boolean callStackDepthCounterIncremented = false;
    final Integer prevDepth = CALL_STACK_DEPTH_COUNTER.get();
    
    try
    {
      CALL_STACK_DEPTH_COUNTER.set(prevDepth + 1);
      
      if (prevDepth >= 1) // this indicates recursive call
      {
        final int ruleSetCallStackDepthThreshold = (target instanceof ModelContext)
            ? ((ModelContext) target).getModel().getRuleSetCallStackDepthThreshold()
            : DEFAULT_CALL_STACK_DEPTH_THRESHOLD;
        
        checkForThresholdCrossing(target, ruleSetCallStackDepthThreshold);
      }
      
      callStackDepthCounterIncremented = true;
    }
    catch (Exception e)
    {
      if (prevDepth != null)
        CALL_STACK_DEPTH_COUNTER.set(prevDepth);
      
      Log.MODELS.debug("An error occurred while trying to increment call stack depth counter: " + e.getMessage(), e);
    }
    
    return callStackDepthCounterIncremented;
  }
  
  private void checkForThresholdCrossing(Context target, int ruleSetCallStackDepthThreshold)
  {
    if (CALL_STACK_DEPTH_COUNTER.get() == ruleSetCallStackDepthThreshold + 1)
    {
      final String errorMessage = MessageFormat.format(Lres.get().getString("errRuleSetCallStackOverflow"),
          target.getPath(), ruleSetCallStackDepthThreshold, getRuleSet());
      
      EventHelper.fireInfoEvent(target, EventLevel.WARNING, errorMessage);
      Log.MODELS.warn(errorMessage);
    }
  }
  
  private void decrementCallStackDepthCounter()
  {
    CALL_STACK_DEPTH_COUNTER.set(CALL_STACK_DEPTH_COUNTER.get() - 1);
  }
}
