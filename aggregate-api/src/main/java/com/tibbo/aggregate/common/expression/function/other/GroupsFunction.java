package com.tibbo.aggregate.common.expression.function.other;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tibbo.aggregate.common.Cres;
import com.tibbo.aggregate.common.datatable.DataTableUtils;
import com.tibbo.aggregate.common.expression.EvaluationEnvironment;
import com.tibbo.aggregate.common.expression.EvaluationException;
import com.tibbo.aggregate.common.expression.Evaluator;
import com.tibbo.aggregate.common.expression.Function;
import com.tibbo.aggregate.common.expression.function.AbstractFunction;

public class GroupsFunction extends AbstractFunction
{
  public GroupsFunction()
  {
    super("groups", Function.GROUP_STRING_PROCESSING, "String source, String regex", "Object", Cres.get().getString("fDescGroups"));
  }
  
  @Override
  public Object execute(Evaluator evaluator, EvaluationEnvironment environment, Object... parameters) throws EvaluationException
  {
    checkParameters(2, false, parameters);
    
    String source = parameters[0].toString();
    String regex = parameters[1].toString();
    
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(source);
    
    List<Object> result = new LinkedList();
    
    while (matcher.find())
    {
      for (int i = 0; i < matcher.groupCount(); i++)
        result.add(matcher.group(i + 1));
    }
    
    return result.size() == 1 ? result.get(0) : DataTableUtils.wrapToTable(result);
  }
}
