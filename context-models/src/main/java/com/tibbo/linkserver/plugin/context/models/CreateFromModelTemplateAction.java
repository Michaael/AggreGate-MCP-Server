package com.tibbo.linkserver.plugin.context.models;

import java.text.*;

import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.datatable.field.*;
import com.tibbo.aggregate.common.datatable.validator.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.linkserver.*;
import com.tibbo.linkserver.action.ActionExecutionParameters;
import com.tibbo.linkserver.action.common.*;
import com.tibbo.linkserver.context.*;

public class CreateFromModelTemplateAction extends CreateFromTemplateAction
{
  private static final String V_CONTAINER_DEFAULT_VALUE = "objects";
  
  private static final TableFormat COPY_PROPERTIES_FORMAT = CreateFromTemplateAction.COPY_PROPERTIES_FORMAT.clone();
  
  static
  {
    COPY_PROPERTIES_FORMAT
        .addField(FieldFormat.create(Model.FIELD_CONTAINER_TYPE, StringFieldFormat.STRING_FIELD, Lres.get().getString("containerType")).setHelp(Lres.get().getString("propertiesContainerTypeHelp"))
            .setDefault(V_CONTAINER_DEFAULT_VALUE).addValidator(ValidatorHelper.TYPE_LENGTH_VALIDATOR).addValidator(ValidatorHelper.TYPE_SYNTAX_VALIDATOR));
    
    COPY_PROPERTIES_FORMAT.addField(FieldFormat.create(Model.FIELD_CONTAINER_TYPE_DESCRIPTION, StringFieldFormat.STRING_FIELD, Lres.get().getString("containerTypeDescription"))
        .setHelp(Lres.get().getString("propertiesContainerTypeDescriptionHelp")).setDefault(V_CONTAINER_DEFAULT_VALUE).addValidator(ValidatorHelper.DESCRIPTION_SYNTAX_VALIDATOR)
        .addValidator(ValidatorHelper.DESCRIPTION_SYNTAX_VALIDATOR));
    
    COPY_PROPERTIES_FORMAT
        .addField(FieldFormat.create(Model.FIELD_CONTAINER_NAME, StringFieldFormat.STRING_FIELD, Lres.get().getString("containerName")).setHelp(Lres.get().getString("propertiesContainerNameHelp"))
            .setDefault(V_CONTAINER_DEFAULT_VALUE).addValidator(ValidatorHelper.NAME_LENGTH_VALIDATOR).addValidator(ValidatorHelper.NAME_SYNTAX_VALIDATOR));
  }
  
  @Override
  public ActionResult execute(ServerActionInput parameters) throws ContextException
  {
    EditableChildrenContext def = (EditableChildrenContext) getDefiningContext();
    
    ModelContext accepted = (ModelContext) getProcessor().fetchDnDSourceContext(Lsres.get().getString("ecSelectTemplate"), getActionDefinition(), parameters, getCallerController(), def.getPath());
    
    if (accepted == null)
    {
      return null;
    }
    Model originalModel = accepted.getModelFromVariable();
    
    if (originalModel.getType() != Model.TYPE_INSTANTIABLE)
      return super.execute(parameters);
    
    String newNameSource = originalModel.getName() + EditableChildrenContext.COPY_SUFFIX;
    String newName = def.generateChildName(newNameSource);
    String newDesc = MessageFormat.format(Lsres.get().getString("ecCopyOf"), originalModel.getDescription());
    String newContainerType = originalModel.getContainerType() + EditableChildrenContext.COPY_SUFFIX;
    String newContainerDesc = MessageFormat.format(Lsres.get().getString("ecCopyOf"), originalModel.getContainerTypeDescription());
    String newContainerName = originalModel.getContainerName() + EditableChildrenContext.COPY_SUFFIX;
    
    DataTable copyProps = new SimpleDataTable(COPY_PROPERTIES_FORMAT, newName, newDesc, newContainerType, newContainerDesc, newContainerName);
    
    boolean error;
    do
    {
      error = false;
      ComponentLocation componentLocation = ComponentLocation.fromDataTable(getParameter(parameters, ActionExecutionParameters.PARAMETER_COMPONENT_LOCATION));
      copyProps = getProcessor().editData(Lsres.get().getString("ecCloneProperties"), copyProps, Icons.CM_MAKE_COPY, Docs.LS_ACTIONS_CREATE_FROM_TEMPLATE, null, componentLocation);
      
      if (copyProps == null)
      {
        return null;
      }
      
      try
      {
        DataRecord funcParameters = new DataRecord(EditableChildrenContext.FIFT_MAKE_COPY);
        funcParameters.addString(accepted.getPath());
        funcParameters.addString(copyProps.rec().getString(EditableChildrenContext.FIF_MAKE_COPY_NAME));
        funcParameters.addString(copyProps.rec().getString(EditableChildrenContext.FIF_MAKE_COPY_DESCRIPTION));
        funcParameters.addString(copyProps.rec().getString(EditableChildrenContext.FIF_MAKE_COPY_CONTAINER_TYPE));
        funcParameters.addString(copyProps.rec().getString(EditableChildrenContext.FIF_MAKE_COPY_CONTAINER_TYPE_DESCRIPTION));
        funcParameters.addString(copyProps.rec().getString(EditableChildrenContext.FIF_MAKE_COPY_CONTAINER_NAME));
        funcParameters.addBoolean(copyProps.rec().getBoolean(EditableChildrenContext.FIF_MAKE_COPY_ENABLED));
        
        def.callFunction(EditableChildrenContext.F_MAKE_COPY, getCallerController(), funcParameters.wrap());
      }
      catch (Exception ex)
      {
        getProcessor().showError(ex);
        error = true;
      }
    }
    while (error);
    
    return null;
  }
}