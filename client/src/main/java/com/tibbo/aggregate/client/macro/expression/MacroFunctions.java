package com.tibbo.aggregate.client.macro.expression;

import java.util.*;

import com.tibbo.aggregate.common.context.*;

public class MacroFunctions
{
  private Map<String, MacroFunction> functions = new HashMap();
  
  public MacroFunctions()
  {
    initFunctions();
  }
  
  public MacroFunction getFunction(String name)
  {
    MacroFunction f = functions.get(name);
    
    return f;
  }
  
  protected void initFunctions()
  {
    addFunction(new TestLocalServer());
    addFunction(new CheckExpression());
    addFunction(new WaitExpression());
  }
  
  protected void addFunction(MacroFunction f)
  {
    if (f == null)
    {
      return;
    }
    
    FunctionDefinition fDef = f.getFunctionDefinition();
    
    if (fDef == null)
    {
      throw new IllegalArgumentException("Function definition is null");
    }
    
    if (functions.containsKey(fDef.getName()))
    {
      throw new IllegalArgumentException("Function " + fDef.getName() + " is already exists");
    }
    
    functions.put(fDef.getName(), f);
  }
}
