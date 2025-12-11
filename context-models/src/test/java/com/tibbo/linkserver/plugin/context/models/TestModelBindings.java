package com.tibbo.linkserver.plugin.context.models;

import com.tibbo.aggregate.common.binding.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.expression.*;
import com.tibbo.linkserver.context.*;
import com.tibbo.linkserver.group.*;

public class TestModelBindings extends BaseModelContextTest
{
  public void testWhenVariableAddedBackThenListenerResubscribes() throws Exception
  {
    
    final Context userGroups = getContextManager().getRoot().getChild("users_groups", getCallerController());
    
    final Group userGroup = new Group("userGroup", "User Group", false);
    
    userGroups.callFunction(EditableChildrenContext.F_CREATE, getCallerController(), DataTableConversion.beanToTable(userGroup, Group.FORMAT, true));
    
    final Context group = userGroups.get("users_groups", getCallerController());
    
    final EvaluationOptions evaluationOptions = new EvaluationOptions(false, new Reference(group.getPath(), "info", ContextUtils.ENTITY_EVENT).getImage());
    final ExtendedBinding binding = new ExtendedBinding(new Binding("info@", "'It Works!'"), evaluationOptions);
    
    ModelManager.get().addBinding(modelContext, binding, getCallerController());
  }
  
  @Override
  protected Model prepareModel()
  {
    final Model model = new Model("model", "Model");
    model.setType(Model.TYPE_ABSOLUTE);
    model.setLogBindingsExecution(true);
    return model;
  }
}