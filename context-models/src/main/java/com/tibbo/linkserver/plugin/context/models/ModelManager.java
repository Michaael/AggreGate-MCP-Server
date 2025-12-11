package com.tibbo.linkserver.plugin.context.models;

import static com.tibbo.aggregate.common.context.AbstractContext.*;
import static com.tibbo.aggregate.common.server.ModelContextConstants.*;

import com.tibbo.aggregate.common.Cres;
import com.tibbo.aggregate.common.binding.Bindings;
import com.tibbo.aggregate.common.binding.ExtendedBinding;
import com.tibbo.aggregate.common.context.CallerController;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextUtils;
import com.tibbo.aggregate.common.context.EventDefinition;
import com.tibbo.aggregate.common.context.FunctionDefinition;
import com.tibbo.aggregate.common.context.VariableDefinition;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.DataTableBuilding;
import com.tibbo.aggregate.common.datatable.DataTableConversion;
import com.tibbo.aggregate.common.datatable.encoding.ClassicEncodingSettings;
import com.tibbo.aggregate.common.security.Permissions;
import com.tibbo.aggregate.common.security.ServerPermissionChecker;
import com.tibbo.aggregate.common.server.EditableChildrenContextConstants;
import com.tibbo.linkserver.Server;
import com.tibbo.linkserver.context.EditableChildrenContext;
import com.tibbo.linkserver.plugin.context.models.rules.RuleSet;

public class ModelManager
{
  private static final ModelManager INSTANCE = new ModelManager();
  
  private ModelManager()
  {
  }
  
  public static ModelManager get()
  {
    return INSTANCE;
  }
  
  public boolean exists(String username, String model)
  {
    String path = ContextUtils.modelContextPath(username, model);
    return Server.getContextManager().get(path, Server.getCallerController()) != null;
  }
  
  public ModelContext create(String username, Model model) throws ContextException
  {
    ModelsContext modelsContext = getModelsContext(username);
    
    if (modelsContext == null)
    {
      throw new ContextException(Cres.get().getString("conNotAvail") + ContextUtils.modelsContextPath(username));
    }
    
    modelsContext.callFunction(EditableChildrenContext.F_CREATE, Server.getCallerController(), DataTableConversion.beanToTable(model, Model.FORMAT, true));
    
    return (ModelContext) modelsContext.getChild(model.getName(), Server.getCallerController());
  }
  
  public InstantiableModelContext createInstantiableContext(Context instantiableModelContainer, String name, String description) throws ContextException
  {
    if (!(instantiableModelContainer instanceof InstantiableModelContainer))
      throw new IllegalArgumentException("Model '" + instantiableModelContainer + "' should be Instantiable Model Container'");
    
    DataRecord rec = new DataRecord(InstantiableModelContainer.VFT_CHILD_INFO);
    rec.setValue(InstantiableModelContainer.VF_CHILD_INFO_NAME, name);
    rec.setValue(InstantiableModelContainer.VF_CHILD_INFO_DESCRIPTION, description);
    
    instantiableModelContainer.callFunction(EditableChildrenContext.F_CREATE, Server.getCallerController(), rec.wrap());
    
    return (InstantiableModelContext) instantiableModelContainer.getChild(name, Server.getCallerController());
  }
  
  public ModelsContext getModelsContext(String username)
  {
    String path = ContextUtils.modelsContextPath(username);
    return (ModelsContext) Server.getContextManager().get(path, Server.getCallerController());
  }
  
  public void delete(String username, String model) throws ContextException
  {
    ModelsContext con = getModelsContext(username);
    con.callFunction(EditableChildrenContextConstants.F_DELETE, Server.getCallerController(), model);
  }
  
  public void addVariable(ModelContext modelContext, VariableDefinition vd) throws ContextException
  {
    DataRecord rec = new DataRecord(ModelContext.VFT_MODEL_VARIABLES);
    
    rec.setValue(FIELD_VD_NAME, vd.getName());
    rec.setValue(FIELD_VD_DESCRIPTION, vd.getDescription());
    rec.setValue(FIELD_VD_FORMAT, DataTableBuilding.formatToTable(vd.getFormat(), new ClassicEncodingSettings(true), false));
    rec.setValue(FIELD_VD_WRITABLE, vd.isWritable());
    rec.setValue(FIELD_VD_HELP, vd.getHelp());
    rec.setValue(FIELD_VD_GROUP, vd.getGroup());
    rec.setValue(FIELD_VD_READ_PERMISSIONS, vd.getReadPermissions() != null ? vd.getReadPermissions().encode() : ServerPermissionChecker.OBSERVER_PERMISSIONS);
    rec.setValue(FIELD_VD_WRITE_PERMISSIONS, vd.getWritePermissions() != null ? vd.getWritePermissions().encode() : ServerPermissionChecker.MANAGER_PERMISSIONS);
    rec.setValue(FIELD_VD_STORAGE_MODE, vd.isPersistent() ? STORAGE_DATABASE : STORAGE_MEMORY);
    rec.setValue(FIELD_VD_UPDATE_HISTORY_STORAGE_TIME, vd.getChangeEventsExpirationPeriod());
    rec.setValue(FIELD_VD_HISTORY_RATE, vd.getHistoryRate());
    rec.setValue(FIELD_VD_CACHE_TIME, vd.getRemoteCacheTime());
    rec.setValue(FIELD_VD_SERVER_CACHING_MODE, vd.getLocalCachingMode());
    rec.setValue(FIELD_VD_ADD_PREVIOUS_VALUE_TO_VARIABLE_UPDATE_EVENT, vd.isAddPreviousValueToVariableUpdateEvent());
    
    modelContext.addVariableRecord(ModelContext.V_MODEL_VARIABLES, Server.getCallerController(), rec);
  }
  
  public void addJavaFunction(ModelContext modelContext, FunctionDefinition fd, String implementationCode) throws ContextException
  {
    addJavaFunction(modelContext, fd, implementationCode, null);
  }
  
  public void addJavaFunction(ModelContext modelContext, FunctionDefinition fd, String implementationCode, String pluginId) throws ContextException
  {
    DataRecord rec = new DataRecord(ModelContext.VFT_MODEL_FUNCTIONS);
    rec.setValue(FIELD_FD_NAME, fd.getName());
    rec.setValue(FIELD_FD_DESCRIPTION, fd.getDescription());
    rec.setValue(FIELD_FD_INPUTFORMAT, DataTableBuilding.formatToTable(fd.getInputFormat(), new ClassicEncodingSettings(true)));
    rec.setValue(FIELD_FD_OUTPUTFORMAT, DataTableBuilding.formatToTable(fd.getOutputFormat(), new ClassicEncodingSettings(true)));
    rec.setValue(FIELD_FD_HELP, fd.getHelp());
    rec.setValue(FIELD_FD_GROUP, fd.getGroup());
    rec.setValue(FIELD_FD_PERMISSIONS, fd.getPermissions() != null ? fd.getPermissions().encode() : ServerPermissionChecker.OPERATOR_PERMISSIONS);
    rec.setValue(FIELD_FD_TYPE, ModelContext.FUNCTION_TYPE_JAVA);
    rec.setValue(FIELD_FD_IMPLEMENTATION, implementationCode);
    rec.setValue(FIELD_FD_EXPRESSION, new String());
    rec.setValue(FIELD_FD_QUERY, new String());
    rec.setValue(FIELD_FD_CONCURRENT, fd.isConcurrent());
    rec.setValue(FIELD_FD_PLUGIN, pluginId);
    modelContext.addVariableRecord(ModelContext.V_MODEL_FUNCTIONS, Server.getCallerController(), rec);
  }
  
  public void addExpressionFunction(ModelContext modelContext, FunctionDefinition fd, String expression, String pluginId) throws ContextException
  {
    DataRecord rec = new DataRecord(ModelContext.VFT_MODEL_FUNCTIONS);
    rec.setValue(FIELD_FD_NAME, fd.getName());
    rec.setValue(FIELD_FD_DESCRIPTION, fd.getDescription());
    rec.setValue(FIELD_FD_INPUTFORMAT, DataTableBuilding.formatToTable(fd.getInputFormat(), new ClassicEncodingSettings(true)));
    rec.setValue(FIELD_FD_OUTPUTFORMAT, DataTableBuilding.formatToTable(fd.getOutputFormat(), new ClassicEncodingSettings(true)));
    rec.setValue(FIELD_FD_HELP, fd.getHelp());
    rec.setValue(FIELD_FD_GROUP, fd.getGroup());
    rec.setValue(FIELD_FD_PERMISSIONS, fd.getPermissions() != null ? fd.getPermissions().encode() : ServerPermissionChecker.OPERATOR_PERMISSIONS);
    rec.setValue(FIELD_FD_TYPE, ModelContext.FUNCTION_TYPE_EXPRESSION);
    rec.setValue(FIELD_FD_IMPLEMENTATION, new String());
    rec.setValue(FIELD_FD_EXPRESSION, expression);
    rec.setValue(FIELD_FD_QUERY, new String());
    rec.setValue(FIELD_FD_CONCURRENT, fd.isConcurrent());
    rec.setValue(FIELD_FD_PLUGIN, pluginId);
    modelContext.addVariableRecord(ModelContext.V_MODEL_FUNCTIONS, Server.getCallerController(), rec);
  }
  
  public void addExpressionFunction(ModelContext modelContext, FunctionDefinition fd, String expression) throws ContextException
  {
    DataRecord rec = new DataRecord(ModelContext.VFT_MODEL_FUNCTIONS);
    rec.setValue(FIELD_FD_NAME, fd.getName());
    rec.setValue(FIELD_FD_DESCRIPTION, fd.getDescription());
    rec.setValue(FIELD_FD_INPUTFORMAT, DataTableBuilding.formatToTable(fd.getInputFormat(), new ClassicEncodingSettings(true)));
    rec.setValue(FIELD_FD_OUTPUTFORMAT, DataTableBuilding.formatToTable(fd.getOutputFormat(), new ClassicEncodingSettings(true)));
    rec.setValue(FIELD_FD_HELP, fd.getHelp());
    rec.setValue(FIELD_FD_GROUP, fd.getGroup());
    rec.setValue(FIELD_FD_PERMISSIONS, fd.getPermissions() != null ? fd.getPermissions().encode() : ServerPermissionChecker.OPERATOR_PERMISSIONS);
    rec.setValue(FIELD_FD_TYPE, ModelContext.FUNCTION_TYPE_EXPRESSION);
    rec.setValue(FIELD_FD_IMPLEMENTATION, new String());
    rec.setValue(FIELD_FD_EXPRESSION, expression);
    rec.setValue(FIELD_FD_QUERY, new String());
    rec.setValue(FIELD_FD_CONCURRENT, fd.isConcurrent());
    modelContext.addVariableRecord(ModelContext.V_MODEL_FUNCTIONS, Server.getCallerController(), rec);
  }
  
  public void addEvent(ModelContext modelContext, EventDefinition ed, Permissions firePermissions) throws ContextException
  {
    DataRecord rec = new DataRecord(ModelContext.VFT_MODEL_EVENTS);
    rec.setValue(FIELD_ED_NAME, ed.getName());
    rec.setValue(FIELD_ED_DESCRIPTION, ed.getDescription());
    rec.setValue(FIELD_ED_FORMAT, DataTableBuilding.formatToTable(ed.getFormat(), new ClassicEncodingSettings(true)));
    rec.setValue(FIELD_ED_HELP, ed.getHelp());
    rec.setValue(FIELD_ED_LEVEL, ed.getLevel());
    rec.setValue(FIELD_ED_GROUP, ed.getGroup());
    rec.setValue(FIELD_ED_PERMISSIONS, ed.getPermissions() != null ? ed.getPermissions().encode() : ServerPermissionChecker.OBSERVER_PERMISSIONS);
    rec.setValue(FIELD_ED_FIRE_PERMISSIONS, firePermissions.encode());
    rec.setValue(FIELD_ED_HISTORY_STORAGE_TIME, null);
    modelContext.addVariableRecord(ModelContext.V_MODEL_EVENTS, Server.getCallerController(), rec);
  }
  
  public void addBinding(ModelContext modelContext, ExtendedBinding binding) throws ContextException
  {
    addBinding(modelContext, binding, Server.getCallerController());
  }
  
  public void addBinding(ModelContext modelContext, ExtendedBinding binding, CallerController caller) throws ContextException
  {
    modelContext.addVariableRecord(ModelContext.V_BINDINGS, caller, Bindings.bindingToDataRecord(binding, modelContext.getVariableDefinition(ModelContext.V_BINDINGS).getFormat()));
  }
  
  public void addRuleSet(ModelContext modelContext, RuleSet ruleSet) throws ContextException
  {
    DataRecord rec = DataTableConversion.beanToRecord(ruleSet, RuleSet.FORMAT, true, false);
    modelContext.addVariableRecord(ModelContext.V_RULE_SETS, Server.getCallerController(), rec);
  }
}
