package com.tibbo.aggregate.client.macro.expression;

import com.tibbo.aggregate.common.context.*;

public interface MacroFunction
{
  public FunctionDefinition getFunctionDefinition();
  
  public Object execute(ContextManager ctxManager, Object... params);
}
