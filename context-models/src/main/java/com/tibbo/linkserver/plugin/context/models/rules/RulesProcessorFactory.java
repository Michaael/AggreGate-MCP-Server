package com.tibbo.linkserver.plugin.context.models.rules;

import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.lang3.reflect.ConstructorUtils;

public class RulesProcessorFactory
{
  private static final Map<Integer, Class<? extends RulesProcessor>> TYPES = new Hashtable();
  static
  {
    TYPES.put(RuleSet.TYPE_SEQUENTIAL, SequentialRulesProcessor.class);
    TYPES.put(RuleSet.TYPE_DEPENDENT, DependentRulesProcessor.class);
  }
  
  public static RulesProcessor createProcessor(RuleSet ruleSet) throws RuleException
  {
    Class<? extends RulesProcessor> clazz = TYPES.get(ruleSet.getType());
    
    if (clazz == null)
    {
      throw new RuleException("Unknown rules processor type: " + ruleSet.getType());
    }
    
    try
    {
      return ConstructorUtils.invokeConstructor(clazz, ruleSet);
    }
    catch (Exception ex)
    {
      throw new RuleException(ex.getMessage(), ex);
    }
  }
}
