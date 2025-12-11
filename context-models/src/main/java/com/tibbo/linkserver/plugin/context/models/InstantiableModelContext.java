package com.tibbo.linkserver.plugin.context.models;

import java.text.MessageFormat;
import java.util.Iterator;

import com.tibbo.aggregate.common.context.CallerController;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextManager;
import com.tibbo.aggregate.common.context.ContextUtils;
import com.tibbo.aggregate.common.context.FunctionDefinition;
import com.tibbo.aggregate.common.context.RequestController;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.FieldFormat;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.common.datatable.field.StringFieldFormat;
import com.tibbo.aggregate.common.expression.Evaluator;
import com.tibbo.aggregate.common.expression.Expression;
import com.tibbo.aggregate.common.security.ServerPermissionChecker;
import com.tibbo.aggregate.common.server.EditableChildrenContextConstants;
import com.tibbo.aggregate.common.server.ModelContextConstants;
import com.tibbo.aggregate.common.server.ServerContext;
import com.tibbo.aggregate.common.util.Docs;
import com.tibbo.aggregate.common.util.Icons;
import com.tibbo.aggregate.common.util.StringUtils;
import com.tibbo.linkserver.action.CallFunctionAction;
import com.tibbo.linkserver.context.EditableChildContext;
import com.tibbo.linkserver.context.EditableChildrenContext;
import com.tibbo.linkserver.security.LicensedContextStatisticsRegistry;
import com.tibbo.linkserver.statistics.ContextStatistics;

public class InstantiableModelContext extends EditableChildContext
{
  private final ModelContext modelContext;
  
  private static final TableFormat FIF_MOVE_TO_CONTAINER = new TableFormat(1, 1);
  
  static
  {
    FieldFormat ff = FieldFormat.create(ModelContext.FIF_CONTAINER_PATH, FieldFormat.STRING_FIELD, Lres.get().getString("containerPath"))
        .setEditor(StringFieldFormat.EDITOR_CONTEXT);
    FIF_MOVE_TO_CONTAINER.addField(ff);
  }
  
  public InstantiableModelContext(ModelContext modelContext, String name, String type)
  {
    super(name, true);
    
    this.modelContext = modelContext;
    
    setType(type);
  }
  
  @Override
  public void setupMyself() throws ContextException
  {
    setLicensedContextStatisticsRegistry(LicensedContextStatisticsRegistry.getOrCreate(this.getClass().getName(), getPluginDirector().getSecurityDirector()));
    
    super.setupMyself();
    
    addChildInfoVariable(InstantiableModelContainer.VFT_CHILD_INFO, false, null, null);

    FunctionDefinition fd = new FunctionDefinition(ModelContextConstants.F_MOVE_TO_CONTAINER, FIF_MOVE_TO_CONTAINER,
        null, Lres.get().getString("moveToContainer"));
    fd.setConcurrent(false);
    addFunctionDefinition(fd);
    
    CallFunctionAction moveToAction = new CallFunctionAction(ModelContextConstants.A_MOVE_TO_CONTAINER,
        Lres.get().getString("moveToContainer"), this, ModelContextConstants.F_MOVE_TO_CONTAINER);
    
    moveToAction.setShowResult(false);
    moveToAction.setShowParamsDialog(true);
    moveToAction.setPermissions(ServerPermissionChecker.getManagerPermissions());
    moveToAction.setIconId(Icons.CM_REMOTE);
    addActionDefinition(moveToAction);
    
    addConfigureAction(false);
    
    addDeleteAction();
    
    addReplicateAction(false, getType());
    
    setIconId(Icons.ST_MODEL_INSTANCE);
    
    addHelpAction(Docs.LS_MODELS);
    
    enableContextStatistics(null, ContextStatistics.VFT_CHANNEL_STATISTICS_PROPERTIES);
    
    enableGranulation(modelContext.getCallerController(), ContextUtils.GROUP_DEFAULT);
    
    modelContext.updateModelTarget(this, modelContext.getModel(), getContextManager().getCallerController());
  }
  
  @Override
  public void stop()
  {
    super.stop();
    
    try
    {
      modelContext.cleanupModelTarget(this, modelContext.getModel(), getContextManager().getCallerController(), false);
    }
    catch (ContextException ex)
    {
      throw new IllegalStateException(ex.getMessage(), ex);
    }
  }
  
  private String evaluateNamingExpression(Context context, ContextManager contextManager, CallerController caller, Expression expression)
  {
    final Evaluator evaluator = new Evaluator(contextManager, context, null, caller);
    
    try
    {
      String result = evaluator.evaluateToStringOrNull(expression);
      if (result != null)
      {
        return result;
      }
    }
    catch (Exception ignore)
    {
    }
    
    return expression.getText();
  }
  
  public void updateDescriptionByNamingExpression(CallerController caller) throws ContextException
  {
    String objectNamingExpression = modelContext.getModel().getObjectNamingExpression();
    if (!StringUtils.isEmpty(objectNamingExpression))
    {
      String expressionResult = evaluateNamingExpression(this, this.getContextManager(), caller, modelContext.getModel().getCachedObjectNamingExpression());
      
      if (!expressionResult.equals(getDescription()))
      {
        setDescription(expressionResult);
      }
    }
  }
  
  @Override
  public void setVariable(String name, CallerController caller, RequestController request, DataTable value) throws ContextException
  {
    super.setVariable(name, caller, request, value);
    
    updateDescriptionByNamingExpression(caller);
  }
  
  @Override
  public DataTable getVariable(String name, CallerController caller) throws ContextException
  {
    if (V_GRANULATOR.equals(name))
    {
      return modelContext.getVariable(name, caller);
    }
    
    return super.getVariable(name, caller);
  }
  
  public DataTable callFmoveToContainer(FunctionDefinition def, CallerController caller, RequestController request, DataTable parameters) throws ContextException
  {
    DataRecord parametersRecord = parameters.rec();
    String newParentPath = parametersRecord.getString(ModelContextConstants.FIF_CONTAINER_PATH);
    Context newParentContext = get(newParentPath, caller);
    Context oldParent = getParent();
    if (newParentContext == null)
    {
      String errorMessage = MessageFormat.format(Lres.get().getString("conDoesNotExist"), newParentPath);
      throw new ContextException(errorMessage);
    }
    
    if (!newParentContext.getType().equals(getParent().getType()))
    {
      String errorMessage = MessageFormat.format(Lres.get().getString("conContainerTypeMismatch"),
          getParent().getType(), newParentContext.getType());
      throw new ContextException(errorMessage);
    }
    DataTable childListOld = oldParent.getVariableClone(EditableChildrenContext.V_CHILD_LIST, caller);
    Iterator<DataRecord> recordIterator = childListOld.iterator();
    DataRecord record = null;
    while (recordIterator.hasNext())
    {
      record = recordIterator.next();
      if (record.getString(EditableChildrenContextConstants.VF_CHILD_LIST_NAME).equals(getName()))
      {
        recordIterator.remove();
        break;
      }
    }
    if (record == null)
    {
      String errorMessage = MessageFormat.format(Lres.get().getString("conDoesNotExist"), newParentPath);
      throw new ContextException(errorMessage);
    }
    
    DataTable childListNew = newParentContext.getVariableClone(EditableChildrenContext.V_CHILD_LIST, caller);
    childListNew.addRecord(record);
    
    move((ServerContext) newParentContext, getName());
    oldParent.setVariable(EditableChildrenContextConstants.V_CHILD_LIST, caller, childListOld);
    newParentContext.setVariable(EditableChildrenContextConstants.V_CHILD_LIST, caller, childListNew);
    
    return null;
  }
  
  @Override
  public boolean isAllowChangeParent()
  {
    return true;
  }
}
