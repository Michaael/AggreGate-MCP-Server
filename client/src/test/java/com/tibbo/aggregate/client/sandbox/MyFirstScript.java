package com.tibbo.aggregate.client.sandbox;

import java.util.*;

import com.tibbo.aggregate.common.binding.*;
import com.tibbo.aggregate.common.widget.*;
import com.tibbo.aggregate.common.widget.context.*;

public class MyFirstScript implements WidgetScript
{
  public Object execute(WidgetScriptExecutionEnvironment environment, Object... parameters)
  {
    Map<Binding, EvaluationOptions> bgs = environment.getEngine().getBindingProvider().createBindings();
    WListContext list1 = (WListContext) environment.getComponentContext("list1");
    Map<String, Object> listItems = new LinkedHashMap<String, Object>();
    int i = 0;
    for (Binding bg : bgs.keySet())
    {
      listItems.put(bg.toString(), i);
      i++;
    }
    list1.getComponent().setListItems(listItems);
    return null;
  }
}