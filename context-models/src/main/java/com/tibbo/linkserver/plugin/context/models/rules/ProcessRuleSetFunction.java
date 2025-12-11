package com.tibbo.linkserver.plugin.context.models.rules;

import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;

public class ProcessRuleSetFunction implements FunctionImplementation
{
  private final RuleSet ruleSet;
  
  public ProcessRuleSetFunction(RuleSet ruleSet)
  {
    this.ruleSet = ruleSet;
  }
  
  @Override
  public DataTable execute(Context con, FunctionDefinition def, CallerController caller, RequestController request, DataTable parameters) throws ContextException
  {
    try
    {
      RulesProcessor processor = RulesProcessorFactory.createProcessor(ruleSet);
      
      return DataTableConstruction.constructTable(processor.process(con.getContextManager(), con, caller, parameters, null));
    }
    catch (Exception ex)
    {
      throw new ContextException(ex);
    }
  }
}
