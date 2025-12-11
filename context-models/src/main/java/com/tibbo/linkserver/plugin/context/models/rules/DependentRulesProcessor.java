package com.tibbo.linkserver.plugin.context.models.rules;

import static com.tibbo.aggregate.common.structure.OriginKind.EXPRESSION;
import static com.tibbo.linkserver.plugin.context.models.rules.Rule.FIELD_EXPRESSION;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.tibbo.aggregate.common.context.CallerController;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextManager;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.expression.AbstractReferenceResolver;
import com.tibbo.aggregate.common.expression.EvaluationEnvironment;
import com.tibbo.aggregate.common.expression.EvaluationException;
import com.tibbo.aggregate.common.expression.Evaluator;
import com.tibbo.aggregate.common.expression.Reference;
import com.tibbo.aggregate.common.expression.ReferenceResolver;
import com.tibbo.aggregate.common.structure.Pinpoint;
import com.tibbo.aggregate.common.util.SyntaxErrorException;
import com.tibbo.aggregate.common.util.Util;
import com.tibbo.linkserver.plugin.context.models.Lres;

public class DependentRulesProcessor extends DefaultRulesProcessor
{
  private final Map<String, Object> variables = new HashMap();
  
  public DependentRulesProcessor(RuleSet ruleSet)
  {
    super(ruleSet);
  }
  
  @Override
  protected Object getVariable(String variable, Evaluator evaluator) throws RuleException
  {
    if (!variables.containsKey(variable))
    {
      throw new RuleException(Lres.get().getString("ruleSetVarNotDefined") + variable);
    }
    
    return variables.get(variable);
  }
  
  @Override
  protected void setVariable(String variable, Object value, Evaluator evaluator) throws RuleException
  {
    variables.put(variable, value);
  }
  
  @Override
  protected boolean isVariableDefined(String variable, Evaluator evaluator)
  {
    return variables.containsKey(variable);
  }
  
  @Override
  protected EvaluationEnvironment createEvaluationEnvironment(Evaluator evaluator, Pinpoint pinpoint)
  {
    EvaluationEnvironment evalEnv = new EvaluationEnvironment(variables);
    evalEnv.assignPinpoint(pinpoint);
    return evalEnv;
  }
  
  @Override
  public Object doProcess(ContextManager cm, Context target, CallerController caller, DataTable parameters, Map<String, Object> environment)
      throws RuleException, EvaluationException, SyntaxErrorException
  {
    RuleSet ruleSet = getRuleSet();
    List<Rule> rules = ruleSet.getRules();

    Evaluator evaluator = createEvaluator(cm, target, caller, parameters, environment);
    
    ReferenceResolver ruleVariableResolver = ruleVariableResolver(target, caller, evaluator);
    
    evaluator.setResolver(Reference.SCHEMA_ENVIRONMENT, ruleVariableResolver);

    Pinpoint conditionPinpointBase = ruleSet.obtainPinpoint().orElse(null);

    Rule rule = findRule(Rule.TARGET_RESULT, target, caller, parameters, rules, conditionPinpointBase);

    Pinpoint expressionPinpoint = ruleSet.obtainPinpoint()
        .map(p -> p.withNestedCell(FIELD_EXPRESSION, rules.indexOf(rule), EXPRESSION))
        .orElse(null);

    return processRule(evaluator, rule, expressionPinpoint);
  }
  
  @Override
  protected Evaluator createEvaluator(ContextManager cm, Context target, CallerController caller, DataTable parameters, Map<String, Object> environment)
  {
    Evaluator evaluator = super.createEvaluator(cm, target, caller, parameters, environment);
    evaluator.setResolver(Reference.SCHEMA_ENVIRONMENT, ruleVariableResolver(target, caller, evaluator));
    return evaluator;
  }
  
  private ReferenceResolver ruleVariableResolver(Context target, CallerController caller, Evaluator evaluator)
  {
    ReferenceResolver ruleVariableResolver = new RulesEnvironmentResolver();
    ruleVariableResolver.setContextManager(target.getContextManager());
    ruleVariableResolver.setDefaultContext(target);
    ruleVariableResolver.setEvaluator(evaluator);
    ruleVariableResolver.setCallerController(caller);
    return ruleVariableResolver;
  }
  
  private Rule findRule(String variable, Context target, CallerController caller, DataTable parameters, List<Rule> rules,
      Pinpoint pinpointBase) throws SyntaxErrorException, EvaluationException, RuleException
  {
    List<Rule> allRules = findRules(variable, rules);
    
    return chooseRule(variable, target, caller, parameters, allRules, pinpointBase);
  }
  
  private Rule chooseRule(String variable, Context target, CallerController caller, DataTable parameters, List<Rule> rules,
      Pinpoint pinpointBase) throws SyntaxErrorException, EvaluationException, RuleException
  {
    List<Rule> found = new LinkedList();
    
    for (Rule cur : rules)
    {
      if (cur.getCondition().isEmpty())
      {
        found.add(cur);
      }
      else
      {
        Evaluator evaluator = createEvaluator(target.getContextManager(), target, caller, parameters, null);

        Pinpoint exactPinpoint = (pinpointBase != null)
            ? pinpointBase.withNestedCell(Rule.FIELD_CONDITION,
                                          getRuleSet().getRules().indexOf(cur),
                                          EXPRESSION)
            : null;

        if (checkCondition(evaluator, cur, exactPinpoint))
        {
          found.add(cur);
        }
      }
    }
    
    if (found.isEmpty())
    {
      throw new RuleException(Lres.get().getString("foundNoRulesToCalculate") + variable);
    }
    else if (found.size() > 1)
    {
      throw new RuleException(Lres.get().getString("foundMultipleRulesToCalculate") + variable);
    }
    else
    {
      return found.get(0);
    }
  }
  
  private List<Rule> findRules(String variable, List<Rule> rules)
  {
    List<Rule> res = new LinkedList();
    
    for (Rule cur : rules)
    {
      if (Util.equals(cur.getTarget(), variable))
      {
        res.add(cur);
      }
    }
    
    return res;
  }
  
  private class RulesEnvironmentResolver extends AbstractReferenceResolver
  {
    @Override
    public Object resolveReference(Reference ref, EvaluationEnvironment environment) throws SyntaxErrorException, EvaluationException
    {
      try
      {
        if (!Reference.SCHEMA_ENVIRONMENT.equals(ref.getSchema())
            || ref.getServer() != null
            || ref.getContext() != null
            || ref.getEntity() != null
            || ref.getProperty() != null
            || ref.getRow() != null)
        {
          throw new EvaluationException("Unexpected reference syntax: " + ref);
        }
        
        final String variable = ref.getField();
        
        if (isVariableDefined(variable, getEvaluator()))
        {
          return getVariable(variable, getEvaluator());
        }

        RuleSet ruleSet = getRuleSet();
        List<Rule> rules = ruleSet.getRules();

        Pinpoint conditionPinpointBase = ruleSet.obtainPinpoint().orElse(null);

        Rule rule = findRule(variable, getDefaultContext(), getCallerController(), getDefaultTable(), rules,
            conditionPinpointBase);

        Pinpoint expressionPinpoint = ruleSet.obtainPinpoint()
            .map(p -> p.withNestedCell(FIELD_EXPRESSION, rules.indexOf(rule), EXPRESSION))
            .orElse(null);

        Object result = processRule(getEvaluator(), rule, expressionPinpoint);
        
        setVariable(variable, result, getEvaluator());
        
        return result;
      }
      catch (RuleException ex)
      {
        throw new EvaluationException(ex);
      }
    }
    
  }
}
