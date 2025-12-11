package com.tibbo.linkserver.plugin.context.models.rules;

import static com.tibbo.aggregate.common.structure.OriginKind.EXPRESSION;

import java.util.List;
import java.util.Map;

import com.tibbo.aggregate.common.context.CallerController;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextManager;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.expression.EvaluationException;
import com.tibbo.aggregate.common.expression.Evaluator;
import com.tibbo.aggregate.common.structure.Pinpoint;
import com.tibbo.aggregate.common.util.SyntaxErrorException;
import com.tibbo.linkserver.plugin.context.models.Lres;

public class SequentialRulesProcessor extends DefaultRulesProcessor
{
  public SequentialRulesProcessor(RuleSet ruleSet)
  {
    super(ruleSet);
  }
  
  @Override
  public Object doProcess(ContextManager cm, Context target, CallerController caller, DataTable parameters, Map<String, Object> environment)
      throws RuleException, EvaluationException, SyntaxErrorException
  {
    RuleSet ruleSet = getRuleSet();
    final List<Rule> rules = ruleSet.getRules();
    
    if (rules.isEmpty())
      throw new RuleException(Lres.get().getString("ruleSetEmpty"));
    
    Evaluator evaluator = createEvaluator(cm, target, caller, parameters, environment);
    
    for (int ruleIndex = 0; ruleIndex < rules.size(); ruleIndex++)
    {
      Rule cur = rules.get(ruleIndex);

      Pinpoint conditionPinpoint = ruleSet.obtainPinpoint().isPresent()
          ? ruleSet.obtainPinpoint().get()
              .withOriginField(RuleSet.FIELD_RULES)
              .withNestedCell(Rule.FIELD_CONDITION, ruleIndex, EXPRESSION)
          : null;

      if (!checkCondition(evaluator, cur, conditionPinpoint))
      {
        continue;
      }

      Pinpoint expressionPinpoint = ruleSet.obtainPinpoint().isPresent()
          ? ruleSet.obtainPinpoint().get()
              .withOriginField(RuleSet.FIELD_RULES)
              .withNestedCell(Rule.FIELD_EXPRESSION, ruleIndex, EXPRESSION)
          : null;

      Object result = processRule(evaluator, cur, expressionPinpoint);

      String ruleTarget = cur.getTarget();

      if (ruleTarget.isEmpty())     // Target is rule set result
      {
        return result;
      }
      else
      {
        setVariable(ruleTarget, result, evaluator);
      }
    }
    
    throw new RuleException(Lres.get().getString("ruleSetResultUndefined"));
  }
}
