package com.tibbo.linkserver.plugin.context.models;

import java.lang.ref.SoftReference;
import java.util.List;

import com.tibbo.aggregate.common.Cres;
import com.tibbo.aggregate.common.datatable.AggreGateBean;
import com.tibbo.aggregate.common.datatable.DataTableBindingProvider;
import com.tibbo.aggregate.common.datatable.FieldFormat;
import com.tibbo.aggregate.common.datatable.SimpleDataTable;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.common.datatable.field.StringFieldFormat;
import com.tibbo.aggregate.common.datatable.validator.LimitsValidator;
import com.tibbo.aggregate.common.datatable.validator.TableExpressionValidator;
import com.tibbo.aggregate.common.datatable.validator.ValidatorHelper;
import com.tibbo.aggregate.common.expression.Expression;
import com.tibbo.aggregate.common.expression.function.DefaultFunctions;
import com.tibbo.aggregate.common.server.EditableChildContextConstants;
import com.tibbo.linkserver.Lsres;
import com.tibbo.linkserver.context.InstallableContext;
import com.tibbo.linkserver.util.ValidityListenerInfo;

public class Model extends AggreGateBean
{
  public static final int TYPE_RELATIVE = 0;
  public static final int TYPE_ABSOLUTE = 1;
  public static final int TYPE_INSTANTIABLE = 2;
  
  public static final String FIELD_NAME = "name";
  public static final String FIELD_DESCRIPTION = "description";
  public static final String FIELD_TYPE = "type";
  public static final String FIELD_VALIDITY_EXPRESSION = "validityExpression";
  public static final String FIELD_VALIDITY_LISTENERS = "validityListeners";
  public static final String FIELD_GENERATE_ATTACHED_EVENTS = "generateAttachedEvents";
  
  public static final String FIELD_CONTAINER_TYPE = "containerType";
  public static final String FIELD_CONTAINER_TYPE_DESCRIPTION = "containerTypeDescription";
  public static final String FIELD_CONTAINER_NAME = "containerName";
  public static final String FIELD_DEFAULT_CONTEXT = "defaultContext";
  public static final String FIELD_OBJECT_TYPE = "objectType";
  public static final String FIELD_OBJECT_TYPE_DESCRIPTION = "objectTypeDescription";
  public static final String FIELD_OBJECT_NAMING_EXPRESSION = "objectNamingExpression";
  
  public static final String FIELD_ENABLED = "enabled";
  
  public static final String FIELD_RULE_SET_CALL_STACK_DEPTH_THRESHOLD = "ruleSetCallStackDepthThreshold";
  
  public static final String FIELD_NORMAL_CONCURRENT_BINDINGS = "normalConcurrentBindings";
  public static final String FIELD_MAXIMUM_CONCURRENT_BINDINGS = "maximumConcurrentBindings";
  public static final String FIELD_MAXIMUM_BINDING_QUEUE_LENGTH = "maximumBindingQueueLength";
  public static final String FIELD_LOG_BINGINGS_EXECUTION = "logBindingsExecution";
  public static final String FIELD_PROTECTED = EditableChildContextConstants.VF_CHILD_INFO_PROTECTED;
  
  public static final String DEFAULT_CONTEXT_EXPRESSION_OPTIONS = DefaultFunctions.EXPRESSION_EDITOR_OPTIONS + "({" + EditableChildContextConstants.V_CHILD_INFO + "$" + Model.FIELD_TYPE + "[0]} == "
          + Model.TYPE_RELATIVE + " ? {" + EditableChildContextConstants.V_CHILD_INFO + "$" + Model.FIELD_DEFAULT_CONTEXT + "[0]} : {.:})";
  
  
  public static TableFormat FORMAT = new TableFormat(1, 1);
  
  static
  {
    FieldFormat ff = FieldFormat.create("<" + FIELD_NAME + "><S><F=C><D=" + Cres.get().getString("name") + "><H=" + Lres.get().getString("propertiesNameHelp") + ">");
    ff.setHelp(Cres.get().getString("conNameChangeWarning"));
    ff.getValidators().add(ValidatorHelper.NAME_LENGTH_VALIDATOR);
    ff.getValidators().add(ValidatorHelper.NAME_SYNTAX_VALIDATOR);
    FORMAT.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_DESCRIPTION + "><S><F=C><D=" + Cres.get().getString("description") + "><H=" + Lres.get().getString("propertiesDescriptionHelp") + ">");
    ff.getValidators().add(ValidatorHelper.DESCRIPTION_LENGTH_VALIDATOR);
    ff.getValidators().add(ValidatorHelper.DESCRIPTION_SYNTAX_VALIDATOR);
    FORMAT.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_TYPE + "><I><A=" + TYPE_ABSOLUTE + "><D=" + Cres.get().getString("type") + "><H=" + Lres.get().getString("propertiesTypeHelp") + ">");
    ff.addSelectionValue(TYPE_RELATIVE, Cres.get().getString("relative"));
    ff.addSelectionValue(TYPE_ABSOLUTE, Cres.get().getString("absolute"));
    ff.addSelectionValue(TYPE_INSTANTIABLE, Lres.get().getString("instantiable"));
    FORMAT.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_VALIDITY_EXPRESSION + "><S><D=" + Lsres.get().getString("conValidityExpression") + "><H=" + Lres.get().getString("propertiesConValidityExpressionHelp")
        + "><G=" + Lres.get().getString("propertiesValidityGroupHelp") + "><E=" + StringFieldFormat.EDITOR_EXPRESSION + ">");
    ff.setEditorOptions(StringFieldFormat.encodeExpressionEditorOptions(InstallableContext.createValidityExpressionAdditionalReferences()));
    FORMAT.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_VALIDITY_LISTENERS + "><T><D=" + Lsres.get().getString("conValidityListeners") + "><H=" + Lres.get().getString("propertiesConValidityListenersHelp") + "><G="
        + Lres.get().getString("propertiesValidityGroupHelp") + ">");
    ff.setDefault(new SimpleDataTable(ValidityListenerInfo.FORMAT));
    FORMAT.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_CONTAINER_TYPE + "><S><A=objects><D=" + Lres.get().getString("containerType") + "><H=" + Lres.get().getString("propertiesContainerTypeHelp") + "><G="
        + Lres.get().getString("propertiesInstantiableModelSettingsGroupHelp") + ">");
    ff.getValidators().add(ValidatorHelper.TYPE_LENGTH_VALIDATOR);
    ff.getValidators().add(ValidatorHelper.TYPE_SYNTAX_VALIDATOR);
    FORMAT.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_CONTAINER_TYPE_DESCRIPTION + "><S><A=" + Cres.get().getString("objects") + "><D=" + Lres.get().getString("containerTypeDescription") + "><H="
        + Lres.get().getString("propertiesContainerTypeDescriptionHelp") + "><G=" + Lres.get().getString("propertiesInstantiableModelSettingsGroupHelp") + ">");
    ff.getValidators().add(ValidatorHelper.DESCRIPTION_LENGTH_VALIDATOR);
    ff.getValidators().add(ValidatorHelper.DESCRIPTION_SYNTAX_VALIDATOR);
    FORMAT.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_CONTAINER_NAME + "><S><A=objects><D=" + Lres.get().getString("containerName") + "><H=" + Lres.get().getString("propertiesContainerNameHelp") + "><G="
        + Lres.get().getString("propertiesInstantiableModelSettingsGroupHelp") + ">");
    ff.getValidators().add(ValidatorHelper.NAME_LENGTH_VALIDATOR);
    ff.getValidators().add(ValidatorHelper.NAME_SYNTAX_VALIDATOR);
    FORMAT.addField(ff);
    
    FORMAT.addField(FieldFormat.create("<" + FIELD_DEFAULT_CONTEXT + "><S><F=N><D=" + Cres.get().getString("conDefaultContext") + "><H="
        + Lres.get().getString("propertiesDefaultContextHelp") + "><G=" + Lsres.get().getString("conValidity") + "><E=" + StringFieldFormat.EDITOR_CONTEXT + ">"));
    
    ff = FieldFormat.create("<" + FIELD_OBJECT_TYPE + "><S><A=object><D=" + Lres.get().getString("objectType") + "><H=" + Lres.get().getString("propertiesObjectTypeHelp") + "><G="
        + Lres.get().getString("propertiesInstantiableModelSettingsGroupHelp") + ">");
    ff.getValidators().add(ValidatorHelper.TYPE_LENGTH_VALIDATOR);
    ff.getValidators().add(ValidatorHelper.TYPE_SYNTAX_VALIDATOR);
    FORMAT.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_OBJECT_TYPE_DESCRIPTION + "><S><A=" + Cres.get().getString("object") + "><D=" + Lres.get().getString("objectTypeDescription") + "><H="
        + Lres.get().getString("propertiesObjectTypeDescriptionHelp") + "><G=" + Lres.get().getString("propertiesInstantiableModelSettingsGroupHelp") + ">");
    FORMAT.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_OBJECT_NAMING_EXPRESSION + "><S><D=" + Lres.get().getString("objectNamingExpression") + "><H="
        + Lres.get().getString("propertiesObjectNamingExpressionHelp") + "><G=" + Lres.get().getString("propertiesInstantiableModelSettingsGroupHelp") + ">");
    ff.setEditor(StringFieldFormat.EDITOR_EXPRESSION);
    FORMAT.addField(ff);
    
    FORMAT.addField(FieldFormat.create("<" + FIELD_ENABLED + "><B><A=1><D=" + Cres.get().getString("enabled") + "><H=" + Lres.get().getString("propertiesEnabledHelp") + ">"));

    ff = FieldFormat.create("<" + FIELD_GENERATE_ATTACHED_EVENTS + "><B><A=0><D=" + Cres.get().getString("generateAttachedEvents") + "><H=" + Cres.get().getString("generateAttachedEventsHelp") + ">");
    ff.setAdvanced(true);
    FORMAT.addField(ff);

    ff = FieldFormat.create("<" + FIELD_RULE_SET_CALL_STACK_DEPTH_THRESHOLD + "><I><A=100><D="
        + Lres.get().getString("ruleSetCallStackDepthThreshold")
        + "><H=" + Lres.get().getString("propertiesRuleSetCallStackDepthThresholdHelp") + ">");
    ff.addValidator(new LimitsValidator(1, Integer.MAX_VALUE));
    ff.setAdvanced(true);
    FORMAT.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_NORMAL_CONCURRENT_BINDINGS + "><I><A=3><D=" + Cres.get().getString("wNormalConcurrentBindings") + "><H="
        + Lres.get().getString("propertiesNormalConcurrentBindingsHelp") + "><G=" + Lres.get().getString("propertiesAdvancedBindingSettingsHelp") + ">");
    FORMAT.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_MAXIMUM_CONCURRENT_BINDINGS + "><I><A=30><D=" + Cres.get().getString("wMaximumConcurrentBindings") + "><H="
        + Lres.get().getString("propertiesMaximumConcurrentBindingsHelp") + "><G=" + Lres.get().getString("propertiesAdvancedBindingSettingsHelp") + "><V=<L=1 " + Integer.MAX_VALUE + ">>");
    FORMAT.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_MAXIMUM_BINDING_QUEUE_LENGTH + "><I><A=100><D=" + Cres.get().getString("wMaximumBindingQueueLength") + "><H="
        + Lres.get().getString("propertiesMaximumBindingQueueLengthHelp") + "><G=" + Lres.get().getString("propertiesAdvancedBindingSettingsHelp") + ">");
    FORMAT.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_LOG_BINGINGS_EXECUTION + "><B><D=" + Cres.get().getString("wLogBindingsExecution") + "><H=" + Lres.get().getString("propertiesLogBindingsExecutionHelp")
        + "><G=" + Lres.get().getString("propertiesAdvancedBindingSettingsHelp") + ">");
    FORMAT.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_PROTECTED + "><B><F=H>");
    FORMAT.addField(ff);
    
    FORMAT.addTableValidator(new TableExpressionValidator("{" + FIELD_MAXIMUM_CONCURRENT_BINDINGS + "} >= {" + FIELD_NORMAL_CONCURRENT_BINDINGS + "} ? null : '"
        + Cres.get().getString("wMaximumBindingsError") + "'"));
    
    String ref = FIELD_VALIDITY_EXPRESSION + "#" + DataTableBindingProvider.PROPERTY_HIDDEN;
    String exp = "{" + FIELD_TYPE + "} == " + TYPE_ABSOLUTE;
    FORMAT.addBinding(ref, exp);
    
    ref = FIELD_VALIDITY_LISTENERS + "#" + DataTableBindingProvider.PROPERTY_HIDDEN;
    exp = "{" + FIELD_TYPE + "} == " + TYPE_ABSOLUTE;
    FORMAT.addBinding(ref, exp);
  
    ref = FIELD_GENERATE_ATTACHED_EVENTS + "#" + DataTableBindingProvider.PROPERTY_HIDDEN;
    exp = "{" + FIELD_TYPE + "} == " + TYPE_ABSOLUTE;
    FORMAT.addBinding(ref, exp);
    
    ref = FIELD_CONTAINER_TYPE + "#" + DataTableBindingProvider.PROPERTY_HIDDEN;
    exp = "{" + FIELD_TYPE + "} != " + TYPE_INSTANTIABLE;
    FORMAT.addBinding(ref, exp);
    
    ref = FIELD_CONTAINER_TYPE_DESCRIPTION + "#" + DataTableBindingProvider.PROPERTY_HIDDEN;
    exp = "{" + FIELD_TYPE + "} != " + TYPE_INSTANTIABLE;
    FORMAT.addBinding(ref, exp);
    
    ref = FIELD_CONTAINER_NAME + "#" + DataTableBindingProvider.PROPERTY_HIDDEN;
    exp = "{" + FIELD_TYPE + "} != " + TYPE_INSTANTIABLE;
    FORMAT.addBinding(ref, exp);
    
    ref = FIELD_OBJECT_TYPE + "#" + DataTableBindingProvider.PROPERTY_HIDDEN;
    exp = "{" + FIELD_TYPE + "} != " + TYPE_INSTANTIABLE;
    FORMAT.addBinding(ref, exp);
    
    ref = FIELD_OBJECT_TYPE_DESCRIPTION + "#" + DataTableBindingProvider.PROPERTY_HIDDEN;
    exp = "{" + FIELD_TYPE + "} != " + TYPE_INSTANTIABLE;
    FORMAT.addBinding(ref, exp);
    
    ref = FIELD_OBJECT_NAMING_EXPRESSION + "#" + DataTableBindingProvider.PROPERTY_HIDDEN;
    exp = "{" + FIELD_TYPE + "} != " + TYPE_INSTANTIABLE;
    FORMAT.addBinding(ref, exp);
    
    ref = FIELD_DEFAULT_CONTEXT + "#" + DataTableBindingProvider.PROPERTY_HIDDEN;
    exp = "{" + FIELD_TYPE + "} != " + TYPE_RELATIVE;
    FORMAT.addBinding(ref, exp);
    
    ref = FIELD_VALIDITY_EXPRESSION + "#" + DataTableBindingProvider.PROPERTY_OPTIONS;
    FORMAT.addBinding(ref, DefaultFunctions.EXPRESSION_EDITOR_OPTIONS + "({" + FIELD_TYPE + "} == "
        + TYPE_RELATIVE + " ? {" + FIELD_DEFAULT_CONTEXT + "} : {.:})");
  }
  
  private String name;
  private String description;
  private int type;
  private String validityExpression;
  private List<ValidityListenerInfo> validityListeners;
  private boolean generateAttachedEvents;
  private String containerType;
  private String containerTypeDescription;
  private String containerName;
  private String defaultContext;
  private String objectType;
  private String objectTypeDescription;
  private String objectNamingExpression;
  private boolean enabled;
  private int ruleSetCallStackDepthThreshold;
  private int normalConcurrentBindings;
  private int maximumConcurrentBindings;
  private int maximumBindingQueueLength;
  private boolean logBindingsExecution;
  
  private Expression cachedValidityExpression;
  private SoftReference<Expression> cachedObjectNamingExpression;
  
  public Model()
  {
    super(FORMAT);
  }
  
  public Model(String name, String description)
  {
    this();
    this.name = name;
    this.description = description;
  }
  
  public String getName()
  {
    return name;
  }
  
  public void setName(String name)
  {
    this.name = name;
  }
  
  public String getDescription()
  {
    return description;
  }
  
  public void setDescription(String description)
  {
    this.description = description;
  }
  
  public int getType()
  {
    return type;
  }
  
  public void setType(int type)
  {
    this.type = type;
  }
  
  public String getValidityExpression()
  {
    return validityExpression;
  }
  
  public void setValidityExpression(String validityExpression)
  {
    this.validityExpression = validityExpression;
    cachedValidityExpression = null;
  }
  
  public Expression getCachedValidityExpression()
  {
    if (cachedValidityExpression == null)
    {
      cachedValidityExpression = new Expression(validityExpression);
    }
    return cachedValidityExpression;
  }
  
  public List<ValidityListenerInfo> getValidityListeners()
  {
    return validityListeners;
  }
  
  public void addValidityListener(ValidityListenerInfo validityListener)
  {
    validityListeners.add(validityListener);
  }
  
  public void setValidityListeners(List<ValidityListenerInfo> validityListeners)
  {
    this.validityListeners = validityListeners;
  }
  
  public String getContainerType()
  {
    return containerType;
  }
  
  public void setContainerType(String containerType)
  {
    this.containerType = containerType;
  }
  
  public String getContainerTypeDescription()
  {
    return containerTypeDescription;
  }
  
  public void setContainerTypeDescription(String containerTypeDescription)
  {
    this.containerTypeDescription = containerTypeDescription;
  }
  
  public String getContainerName()
  {
    return containerName;
  }
  
  public void setContainerName(String containerName)
  {
    this.containerName = containerName;
  }
  
  public String getObjectType()
  {
    return objectType;
  }
  
  public void setObjectType(String objectType)
  {
    this.objectType = objectType;
  }
  
  public String getObjectTypeDescription()
  {
    return objectTypeDescription;
  }
  
  public void setObjectTypeDescription(String objectDescription)
  {
    this.objectTypeDescription = objectDescription;
  }
  
  public String getObjectNamingExpression()
  {
    return objectNamingExpression;
  }
  
  public void setObjectNamingExpression(String objectNamingExpression)
  {
    this.objectNamingExpression = objectNamingExpression;
    
    synchronized (this)
    {
      cachedObjectNamingExpression = null;
    }
  }
  
  private Expression cacheObjectNamingExpression()
  {
    synchronized (this)
    {
      Expression expression = cachedObjectNamingExpression != null ? cachedObjectNamingExpression.get() : null;
      
      if (expression == null)
      {
        expression = new Expression(objectNamingExpression);
        cachedObjectNamingExpression = new SoftReference<>(expression);
      }
      
      return expression;
    }
  }
  
  public Expression getCachedObjectNamingExpression()
  {
    Expression expression = cachedObjectNamingExpression != null ? cachedObjectNamingExpression.get() : null;
    
    return expression == null ? cacheObjectNamingExpression() : expression;
  }
  
  public boolean isEnabled()
  {
    return enabled;
  }
  
  public void setEnabled(boolean enabled)
  {
    this.enabled = enabled;
  }
  
  public int getRuleSetCallStackDepthThreshold()
  {
    return ruleSetCallStackDepthThreshold;
  }
  
  public void setRuleSetCallStackDepthThreshold(int ruleSetCallStackDepthThreshold)
  {
    this.ruleSetCallStackDepthThreshold = ruleSetCallStackDepthThreshold;
  }
  
  public int getNormalConcurrentBindings()
  {
    return normalConcurrentBindings;
  }
  
  public void setNormalConcurrentBindings(int normalConcurrentBindings)
  {
    this.normalConcurrentBindings = normalConcurrentBindings;
  }
  
  public int getMaximumConcurrentBindings()
  {
    return maximumConcurrentBindings;
  }
  
  public void setMaximumConcurrentBindings(int maximumConcurrentBindings)
  {
    this.maximumConcurrentBindings = maximumConcurrentBindings;
  }
  
  public int getMaximumBindingQueueLength()
  {
    return maximumBindingQueueLength;
  }
  
  public void setMaximumBindingQueueLength(int maximumQueueLength)
  {
    this.maximumBindingQueueLength = maximumQueueLength;
  }
  
  public boolean isLogBindingsExecution()
  {
    return logBindingsExecution;
  }
  
  public void setLogBindingsExecution(boolean logBindingsExecution)
  {
    this.logBindingsExecution = logBindingsExecution;
  }
  
  public String getDefaultContext()
  {
    return defaultContext;
  }
  
  public void setDefaultContext(String defaultContext)
  {
    this.defaultContext = defaultContext;
  }
  
  public boolean isGenerateAttachedEvents() {
    return generateAttachedEvents;
  }
  
  public void setGenerateAttachedEvents(boolean generateAttachedEvents) {
    this.generateAttachedEvents = generateAttachedEvents;
  }
}
