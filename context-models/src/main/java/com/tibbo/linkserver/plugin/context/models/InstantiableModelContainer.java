package com.tibbo.linkserver.plugin.context.models;

import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.datatable.validator.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.linkserver.context.*;
import com.tibbo.linkserver.user.*;

public class InstantiableModelContainer extends EditableChildrenContext
{
  public static final String VF_CHILD_INFO_NAME = "name";
  public static final String VF_CHILD_INFO_DESCRIPTION = "description";
  
  public static TableFormat VFT_CHILD_INFO = new TableFormat(1, 1);
  static
  {
    FieldFormat ff = FieldFormat.create("<" + VF_CHILD_INFO_NAME + "><S><F=C><D=" + Cres.get().getString("name") + ">");
    ff.setHelp(Cres.get().getString("conNameChangeWarning"));
    ff.getValidators().add(ValidatorHelper.NAME_LENGTH_VALIDATOR);
    ff.getValidators().add(ValidatorHelper.NAME_SYNTAX_VALIDATOR);
    VFT_CHILD_INFO.addField(ff);
    
    ff = FieldFormat.create("<" + VF_CHILD_INFO_DESCRIPTION + "><S><F=C><D=" + Cres.get().getString("description") + ">");
    ff.getValidators().add(ValidatorHelper.DESCRIPTION_LENGTH_VALIDATOR);
    ff.getValidators().add(ValidatorHelper.DESCRIPTION_SYNTAX_VALIDATOR);
    VFT_CHILD_INFO.addField(ff);
  }
  
  private final ModelContext modelContext;
  private final String containerType;
  private final UserContext userContext;
  
  public InstantiableModelContainer(ModelContext modelContext, String containerName, String containerType, String containerDescription, String objectTypeDescription, String objectType,
      UserContext userContext)
  {
    super(containerName, objectTypeDescription, objectType, VFT_CHILD_INFO);
    this.modelContext = modelContext;
    this.containerType = containerType;
    this.userContext = userContext;
    setDescription(containerDescription);
  }
  
  public InstantiableModelContainer(ModelContext modelContext, String containerName, String containerType, String containerDescription, String objectTypeDescription, String objectType)
  {
    this(modelContext, containerName, containerType, containerDescription, objectTypeDescription, objectType, null);
  }
  
  @Override
  protected void enableGrouping(String groupsContextDescription)
  {
    super.enableGrouping(groupsContextDescription);
    
    UserContext userContext = getUserContext();
    CallerController caller = userContext != null ? userContext.getCallerController() : null;
    
    Context groups = getParent().getChild(getGroupsContextName(), caller);
    
    if (groups != null && !groups.isStarted())
    {
      // start group context to add validity listeners
      groups.start();
    }
  }
  
  @Override
  protected UserContext getUserContext()
  {
    // InstantiableModelContainer can be mounted anywhere.
    // In this situation, standard implementation of getUserContext() may not work, so we use userContext from parent model.
    UserContext userContext = super.getUserContext();
    return userContext != null ? userContext : this.userContext;
  }
  
  @Override
  public void setupMyself() throws ContextException
  {
    super.setupMyself();
    
    addCreateAction(true, null, null);
    
    allowGrouping(Cres.get().getString("groups") + " - " + getObjectName());
    
    setIconId(Icons.ST_MODEL_INSTANCES);
    
    addHelpAction(Docs.LS_MODELS);
  }
  
  @Override
  public String getType()
  {
    return containerType;
  }
  
  @Override
  protected EditableChildContext buildChild(String cname, boolean readOnly, String type) throws ContextException
  {
    return new InstantiableModelContext(modelContext, cname, getChildType());
  }
  
  @Override
  public boolean isInstallationAllowed(String installableItemName)
  {
    return !installableItemName.equals(containerType);
  }
}
