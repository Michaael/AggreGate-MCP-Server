package com.tibbo.linkserver.plugin.context.models.rules;

import java.util.Map;

import com.tibbo.aggregate.common.context.CallerController;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextManager;
import com.tibbo.aggregate.common.datatable.DataTable;

public interface RulesProcessor
{
  Object process(ContextManager cm, Context target, CallerController caller, DataTable parameters, Map<String, Object> environment) throws RuleException;
}
