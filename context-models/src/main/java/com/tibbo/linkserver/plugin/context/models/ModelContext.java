package com.tibbo.linkserver.plugin.context.models;

import static com.tibbo.aggregate.common.structure.PinpointFactory.newPinpointFor;
import static com.tibbo.linkserver.plugin.context.models.Model.DEFAULT_CONTEXT_EXPRESSION_OPTIONS;

import java.lang.ref.SoftReference;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.tibbo.aggregate.common.Cres;
import com.tibbo.aggregate.common.Log;
import com.tibbo.aggregate.common.binding.Binding;
import com.tibbo.aggregate.common.binding.BindingEventsHelper;
import com.tibbo.aggregate.common.binding.Bindings;
import com.tibbo.aggregate.common.context.AbstractContext;
import com.tibbo.aggregate.common.context.CallerController;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextRuntimeException;
import com.tibbo.aggregate.common.context.ContextSecurityException;
import com.tibbo.aggregate.common.context.ContextUtils;
import com.tibbo.aggregate.common.context.Contexts;
import com.tibbo.aggregate.common.context.DefaultContextEventListener;
import com.tibbo.aggregate.common.context.EntityDefinition;
import com.tibbo.aggregate.common.context.EventDefinition;
import com.tibbo.aggregate.common.context.FunctionDefinition;
import com.tibbo.aggregate.common.context.FunctionImplementation;
import com.tibbo.aggregate.common.context.RequestController;
import com.tibbo.aggregate.common.context.TableFieldsCompatibilityConverter;
import com.tibbo.aggregate.common.context.VariableDefinition;
import com.tibbo.aggregate.common.context.VariableGetter;
import com.tibbo.aggregate.common.context.VariableSetter;
import com.tibbo.aggregate.common.data.Event;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.DataTableBindingProvider;
import com.tibbo.aggregate.common.datatable.DataTableBuilding;
import com.tibbo.aggregate.common.datatable.DataTableConversion;
import com.tibbo.aggregate.common.datatable.DataTableReplication;
import com.tibbo.aggregate.common.datatable.FieldFormat;
import com.tibbo.aggregate.common.datatable.SimpleDataTable;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.common.datatable.encoding.ClassicEncodingSettings;
import com.tibbo.aggregate.common.datatable.field.LongFieldFormat;
import com.tibbo.aggregate.common.datatable.field.StringFieldFormat;
import com.tibbo.aggregate.common.datatable.validator.FieldValidator;
import com.tibbo.aggregate.common.datatable.validator.LimitsValidator;
import com.tibbo.aggregate.common.datatable.validator.TableKeyFieldsValidator;
import com.tibbo.aggregate.common.datatable.validator.ValidatorHelper;
import com.tibbo.aggregate.common.event.EventHandlingException;
import com.tibbo.aggregate.common.event.EventLevel;
import com.tibbo.aggregate.common.event.FireEventRequestController;
import com.tibbo.aggregate.common.expression.Evaluator;
import com.tibbo.aggregate.common.expression.Expression;
import com.tibbo.aggregate.common.expression.Function;
import com.tibbo.aggregate.common.expression.Reference;
import com.tibbo.aggregate.common.expression.function.DefaultFunctions;
import com.tibbo.aggregate.common.script.ScriptCompiler;
import com.tibbo.aggregate.common.security.Permissions;
import com.tibbo.aggregate.common.security.ServerPermissionChecker;
import com.tibbo.aggregate.common.server.ClusterCoordinatorContextConstants;
import com.tibbo.aggregate.common.server.EditableChildContextConstants;
import com.tibbo.aggregate.common.server.EditableChildrenContextConstants;
import com.tibbo.aggregate.common.server.ModelContextConstants;
import com.tibbo.aggregate.common.server.RootContextConstants;
import com.tibbo.aggregate.common.server.ServerContext;
import com.tibbo.aggregate.common.server.UtilitiesContextConstants;
import com.tibbo.aggregate.common.structure.Pinpoint;
import com.tibbo.aggregate.common.util.Docs;
import com.tibbo.aggregate.common.util.Icons;
import com.tibbo.aggregate.common.util.NamedThreadFactory;
import com.tibbo.aggregate.common.util.Pair;
import com.tibbo.aggregate.common.util.StringUtils;
import com.tibbo.aggregate.common.util.TimeHelper;
import com.tibbo.aggregate.common.util.Util;
import com.tibbo.aggregate.common.view.StorageHelper;
import com.tibbo.linkserver.Lsres;
import com.tibbo.linkserver.Server;
import com.tibbo.linkserver.context.BaseServerContext;
import com.tibbo.linkserver.context.EditableChildContext;
import com.tibbo.linkserver.context.InstallableContext;
import com.tibbo.linkserver.context.RootContext;
import com.tibbo.linkserver.context.UtilitiesContext;
import com.tibbo.linkserver.event.EventHelper;
import com.tibbo.linkserver.granulation.Granulator;
import com.tibbo.linkserver.plugin.context.models.rules.ProcessRuleSetFunction;
import com.tibbo.linkserver.plugin.context.models.rules.RuleSet;
import com.tibbo.linkserver.plugin.context.queries.sql.SQLFacade;
import com.tibbo.linkserver.security.LicenseViolationException;
import com.tibbo.linkserver.security.LicensingUnitsConstants;
import com.tibbo.linkserver.statistics.ChannelProperties;
import com.tibbo.linkserver.statistics.ContextStatistics;
import com.tibbo.linkserver.templates.TemplatableServerContext;
import com.tibbo.linkserver.user.UserContext;
import com.tibbo.linkserver.util.ValidityListenerInfo;

public class ModelContext extends InstallableContext implements ModelContextConstants
{
  public static final int MIN_DESCRIPTION_LENGTH = 1;
  public static final int MAX_DESCRIPTION_LENGTH = 100;
  public static final double MODEL_ATTACHING_LOAD_FACTOR = 0.5;

  public static final String MODEL = "model";
  
  public static final FieldValidator DESCRIPTION_LENGTH_VALIDATOR = new LimitsValidator(MIN_DESCRIPTION_LENGTH, MAX_DESCRIPTION_LENGTH);
  
  public static final long BINDING_QUEUE_OVERFLOW_NOTIFICATION_PERIOD = TimeHelper.SECOND_IN_MS * 10;
  
  private static final List<String> PROTECTED_VARIABLES_LIST = Arrays.asList(V_INFO, V_CHILDREN, V_VARIABLES, V_FUNCTIONS, V_EVENTS, V_ACTIONS, V_VARIABLE_STATUSES);
  
  private static final List<String> MODEL_VARIABLES_LIST = Arrays.asList(V_MODEL_VARIABLES, V_MODEL_FUNCTIONS, V_MODEL_EVENTS, V_BINDINGS, V_RULE_SETS, V_STATISTICS_PROPERTIES, V_GRANULATOR);
  
  public static String getDefaultImplementationText(String body)
  {
    StringBuilder sb = new StringBuilder();
    StringUtils.appendLine(sb, "import com.tibbo.aggregate.common.context.*;");
    StringUtils.appendLine(sb, "import com.tibbo.aggregate.common.datatable.*;");
    StringUtils.appendLine(sb, "import com.tibbo.aggregate.common.server.*;");
    StringUtils.appendLine(sb, "");
    StringUtils.appendLine(sb, "import com.tibbo.linkserver.*;");
    StringUtils.appendLine(sb, "import com.tibbo.linkserver.context.*;");
    StringUtils.appendLine(sb, "");
    StringUtils.appendLine(sb, "public class " + ScriptCompiler.SCRIPT_CLASS_NAME_PATTERN + " implements FunctionImplementation");
    StringUtils.appendLine(sb, "{");
    StringUtils.appendLine(sb, "  public DataTable execute(Context con, FunctionDefinition def, CallerController caller, RequestController request, DataTable parameters) throws ContextException");
    StringUtils.appendLine(sb, "  {");
    if (body != null)
    {
      StringUtils.appendLine(sb, body);
    }
    else
    {
      StringUtils.appendLine(sb, "    return null;");
    }
    StringUtils.appendLine(sb, "  }");
    StringUtils.appendLine(sb, "}");
    return sb.toString();
  }
  
  public static final TableFormat VFT_MODEL_VARIABLES = new TableFormat(true);
  
  static
  {
    FieldFormat ff = FieldFormat.create("<" + FIELD_VD_NAME + "><S><F=K><D=" + Cres.get().getString("name") + "><H=" + Lres.get().getString("variableNameHelp") + ">");
    ff.addValidator(ValidatorHelper.NAME_LENGTH_VALIDATOR);
    ff.addValidator(ValidatorHelper.NAME_SYNTAX_VALIDATOR);
    VFT_MODEL_VARIABLES.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_VD_DESCRIPTION + "><S><F=N><D=" + Cres.get().getString("description") + "><H=" + Lres.get().getString("variableDescriptionHelp") + ">");
    ff.addValidator(DESCRIPTION_LENGTH_VALIDATOR);
    ff.addValidator(ValidatorHelper.DESCRIPTION_SYNTAX_VALIDATOR);
    VFT_MODEL_VARIABLES.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_VD_FORMAT + "><T><D=" + Cres.get().getString("format") + "><H=" + Lres.get().getString("variableFormatHelp") + ">");
    ff.setDefault(new SimpleDataTable(DataTableBuilding.TABLE_FORMAT, true));
    VFT_MODEL_VARIABLES.addField(ff);
    
    VFT_MODEL_VARIABLES.addField("<" + FIELD_VD_WRITABLE + "><B><D=" + Cres.get().getString("writable") + "><H=" + Lres.get().getString("variableWritableHelp") + ">");
    
    VFT_MODEL_VARIABLES.addField("<" + FIELD_VD_HELP + "><S><F=N><D=" + Cres.get().getString("help") + "><H=" + Lres.get().getString("variableHelpHelp") + ">");
    VFT_MODEL_VARIABLES.addField("<" + FIELD_VD_GROUP + "><S><F=N><D=" + Cres.get().getString("group") + "><H=" + Lres.get().getString("variableGroupHelp") + ">");
    
    ff = FieldFormat.create("<" + FIELD_VD_READ_PERMISSIONS + "><S><A=" + ServerPermissionChecker.OBSERVER_PERMISSIONS + "><D=" + Cres.get().getString("readPermissions") + "><H="
        + Lres.get().getString("variableReadPermissionsHelp") + ">");
    ff.setSelectionValues(Server.getPermissionChecker().getPermissionLevels());
    VFT_MODEL_VARIABLES.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_VD_WRITE_PERMISSIONS + "><S><A=" + ServerPermissionChecker.MANAGER_PERMISSIONS + "><D=" + Cres.get().getString("writePermissions") + "><H="
        + Lres.get().getString("variableWritePermissionsHelp") + ">");
    ff.setSelectionValues(Server.getPermissionChecker().getPermissionLevels());
    VFT_MODEL_VARIABLES.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_VD_STORAGE_MODE + "><I><A=" + STORAGE_DATABASE + "><D=" + Lres.get().getString("storageMode") + "><H=" + Lres.get().getString("variableStorageModeHelp") + ">");
    ff.addSelectionValue(STORAGE_DATABASE, Cres.get().getString("database"));
    ff.addSelectionValue(STORAGE_MEMORY, Cres.get().getString("memory"));
    VFT_MODEL_VARIABLES.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_VD_UPDATE_HISTORY_STORAGE_TIME + "><L><F=N><D=" + Lsres.get().getString("devUpdateHistoryStorageTime") + "><H="
        + Lres.get().getString("variableHistoryStorageTimeHelp") + "><E=" + LongFieldFormat.EDITOR_PERIOD + "><O=" + LongFieldFormat.encodePeriodEditorOptions(TimeHelper.HOUR, TimeHelper.YEAR) + ">");
    VFT_MODEL_VARIABLES.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_VD_HISTORY_RATE + "><I><A=" + VariableDefinition.HISTORY_RATE_ALL + "><D=" + Lsres.get().getString("devHistoryRate") + ">");
    ff.setHelp(Lres.get().getString("variableHistoryRateHelp"));
    ff.addSelectionValue(VariableDefinition.HISTORY_RATE_ALL, Lsres.get().getString("devAllValues"));
    ff.addSelectionValue(VariableDefinition.HISTORY_RATE_CHANGES, Lsres.get().getString("devChangesOnly"));
    ff.setAdvanced(true);
    VFT_MODEL_VARIABLES.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_VD_CACHE_TIME + "><L><F=NA><D=" + Lres.get().getString("cacheTime") + "><H=" + Lres.get().getString("cacheTimeHelp") + "><E=" + LongFieldFormat.EDITOR_PERIOD
        + "><O=" + LongFieldFormat.encodePeriodEditorOptions(TimeHelper.MILLISECOND, TimeHelper.SECOND) + ">");
    VFT_MODEL_VARIABLES.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_VD_SERVER_CACHING_MODE + "><I><A=" + VariableDefinition.CACHING_HARD + "><F=A><D=" + Lres.get().getString("serverCachingMode") + "><H="
        + Lres.get().getString("serverCachingModeHelp") + ">");
    ff.setSelectionValues(serverCachingModes());
    VFT_MODEL_VARIABLES.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_VD_ADD_PREVIOUS_VALUE_TO_VARIABLE_UPDATE_EVENT + "><B><D=" + Cres.get().getString("addPreviousValueToVariableUpdateEvent") + "><H="
        + Cres.get().getString("devAddPreviousValueToVariableUpdateEvent") + ">");
    ff.setDefault(false);
    ff.setAdvanced(true);
    VFT_MODEL_VARIABLES.addField(ff);
    
    VFT_MODEL_VARIABLES.addTableValidator(new TableKeyFieldsValidator());
  }
  
  private static Map<Integer, String> serverCachingModes()
  {
    final LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
    map.put(VariableDefinition.CACHING_HARD, Lres.get().getString("serverCachingModeHard"));
    map.put(VariableDefinition.CACHING_SOFT, Lres.get().getString("serverCachingModeSoft"));
    return map;
  }

  protected static final TableFormat VFT_MODEL_FUNCTIONS = new TableFormat(true);
  
  static
  {
    FieldFormat ff = FieldFormat.create("<" + FIELD_FD_NAME + "><S><F=K><D=" + Cres.get().getString("name") + "><H=" + Lres.get().getString("functionNameHelp") + ">");
    ff.addValidator(ValidatorHelper.NAME_LENGTH_VALIDATOR);
    ff.addValidator(ValidatorHelper.NAME_SYNTAX_VALIDATOR);
    VFT_MODEL_FUNCTIONS.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_FD_DESCRIPTION + "><S><F=N><D=" + Cres.get().getString("description") + "><H=" + Lres.get().getString("functionDescriptionHelp") + ">");
    ff.addValidator(DESCRIPTION_LENGTH_VALIDATOR);
    ff.addValidator(ValidatorHelper.DESCRIPTION_SYNTAX_VALIDATOR);
    VFT_MODEL_FUNCTIONS.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_FD_INPUTFORMAT + "><T><D=" + Cres.get().getString("inputFormat") + "><H=" + Lres.get().getString("functionInputFormatHelp") + ">");
    ff.setDefault(new SimpleDataTable(DataTableBuilding.TABLE_FORMAT, true));
    VFT_MODEL_FUNCTIONS.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_FD_OUTPUTFORMAT + "><T><D=" + Cres.get().getString("outputFormat") + "><H=" + Lres.get().getString("functionOutputFormatHelp") + ">");
    ff.setDefault(new SimpleDataTable(DataTableBuilding.TABLE_FORMAT, true));
    VFT_MODEL_FUNCTIONS.addField(ff);
    
    VFT_MODEL_FUNCTIONS.addField("<" + FIELD_FD_HELP + "><S><F=N><D=" + Cres.get().getString("help") + "><H=" + Lres.get().getString("functionHelpHelp") + ">");
    VFT_MODEL_FUNCTIONS.addField("<" + FIELD_FD_GROUP + "><S><F=N><D=" + Cres.get().getString("group") + "><H=" + Lres.get().getString("functionGroupHelp") + ">");
    
    ff = FieldFormat.create("<" + FIELD_FD_PERMISSIONS + "><S><A=" + ServerPermissionChecker.OPERATOR_PERMISSIONS + "><D=" + Cres.get().getString("permissions") + "><H="
        + Lres.get().getString("functionPermissionsHelp") + ">");
    ff.setSelectionValues(Server.getPermissionChecker().getPermissionLevels());
    VFT_MODEL_FUNCTIONS.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_FD_TYPE + "><I><A=" + FUNCTION_TYPE_JAVA + "><D=" + Cres.get().getString("type") + "><H=" + Lres.get().getString("functionTypeHelp") + ">");
    ff.addSelectionValue(FUNCTION_TYPE_JAVA, Lres.get().getString("javaCode"));
    ff.addSelectionValue(FUNCTION_TYPE_EXPRESSION, Cres.get().getString("expression"));
    ff.addSelectionValue(FUNCTION_TYPE_QUERY, Cres.get().getString("query"));
    ff.setDefault(FUNCTION_TYPE_EXPRESSION);
    VFT_MODEL_FUNCTIONS.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_FD_IMPLEMENTATION + "><S><D=" + Cres.get().getString("implementation") + "><H=" + Lres.get().getString("functionImplementationHelp") + "><E="
        + StringFieldFormat.EDITOR_CODE + "><O=" + StringFieldFormat.CODE_EDITOR_MODE_JAVA + ">");
    ff.setDefault(getDefaultImplementationText(null));
    VFT_MODEL_FUNCTIONS.addField(ff);
    
    ff = FieldFormat.create(
        "<" + FIELD_FD_EXPRESSION + "><S><D=" + Cres.get().getString("expression") + "><H=" + Lres.get().getString("functionExpressionHelp") + "><E=" + StringFieldFormat.EDITOR_EXPRESSION + ">");
    VFT_MODEL_FUNCTIONS.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_FD_QUERY + "><S><D=" + Cres.get().getString("query") + "><H=" + Lres.get().getString("functionQueryHelp") + "><E=" + StringFieldFormat.EDITOR_TEXT + "><O="
        + StringFieldFormat.TEXT_EDITOR_MODE_SQL + ">");
    VFT_MODEL_FUNCTIONS.addField(ff);
    
    VFT_MODEL_FUNCTIONS.addField("<" + FIELD_FD_CONCURRENT + "><B><A=1><F=A><D=" + Cres.get().getString("concurrent") + "><H=" + Lres.get().getString("functionConcurrentHelp") + ">");
    
    VFT_MODEL_FUNCTIONS.addField("<" + FIELD_FD_PLUGIN + "><S><F=NA><D=" + Cres.get().getString("plugin") + ">");
    
    String ref = FIELD_FD_IMPLEMENTATION + "#" + DataTableBindingProvider.PROPERTY_ENABLED;
    String exp = "{" + FIELD_FD_TYPE + "} == " + FUNCTION_TYPE_JAVA;
    VFT_MODEL_FUNCTIONS.addBinding(ref, exp);
    
    ref = FIELD_FD_EXPRESSION + "#" + DataTableBindingProvider.PROPERTY_ENABLED;
    exp = "{" + FIELD_FD_TYPE + "} == " + FUNCTION_TYPE_EXPRESSION;
    VFT_MODEL_FUNCTIONS.addBinding(ref, exp);
    
    ref = FIELD_FD_QUERY + "#" + DataTableBindingProvider.PROPERTY_ENABLED;
    exp = "{" + FIELD_FD_TYPE + "} == " + FUNCTION_TYPE_QUERY;
    VFT_MODEL_FUNCTIONS.addBinding(ref, exp);
    
    ref = FIELD_FD_PLUGIN + "#" + DataTableBindingProvider.PROPERTY_ENABLED;
    exp = "{" + FIELD_FD_TYPE + "} == " + FUNCTION_TYPE_JAVA;
    VFT_MODEL_FUNCTIONS.addBinding(ref, exp);
    
    ref = FIELD_FD_EXPRESSION + "#" + DataTableBindingProvider.PROPERTY_OPTIONS;
    VFT_MODEL_FUNCTIONS.addBinding(ref, DEFAULT_CONTEXT_EXPRESSION_OPTIONS);
    
    VFT_MODEL_FUNCTIONS.addTableValidator(new TableKeyFieldsValidator());
  }
  
  protected static final TableFormat VFT_MODEL_EVENTS = new TableFormat(true);
  
  static
  {
    FieldFormat ff = FieldFormat.create("<" + FIELD_ED_NAME + "><S><F=K><D=" + Cres.get().getString("name") + "><H=" + Lres.get().getString("eventNameHelp") + ">");
    ff.addValidator(ValidatorHelper.NAME_LENGTH_VALIDATOR);
    ff.addValidator(ValidatorHelper.NAME_SYNTAX_VALIDATOR);
    VFT_MODEL_EVENTS.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_ED_DESCRIPTION + "><S><F=N><D=" + Cres.get().getString("description") + "><H=" + Lres.get().getString("eventDescriptionHelp") + ">");
    ff.addValidator(DESCRIPTION_LENGTH_VALIDATOR);
    ff.addValidator(ValidatorHelper.DESCRIPTION_SYNTAX_VALIDATOR);
    VFT_MODEL_EVENTS.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_ED_FORMAT + "><T><D=" + Cres.get().getString("format") + "><H=" + Lres.get().getString("eventFormatHelp") + ">");
    ff.setDefault(new SimpleDataTable(DataTableBuilding.TABLE_FORMAT, true));
    VFT_MODEL_EVENTS.addField(ff);
    
    VFT_MODEL_EVENTS.addField("<" + FIELD_ED_HELP + "><S><F=N><D=" + Cres.get().getString("help") + "><H=" + Lres.get().getString("eventHelpHelp") + ">");
    
    ff = FieldFormat.create("<" + FIELD_ED_LEVEL + "><I><D=" + Cres.get().getString("level") + "><H=" + Lres.get().getString("eventLevelHelp") + ">");
    ff.setSelectionValues(EventLevel.getSelectionValues());
    VFT_MODEL_EVENTS.addField(ff);
    
    VFT_MODEL_EVENTS.addField("<" + FIELD_ED_GROUP + "><S><F=N><D=" + Cres.get().getString("group") + "><H=" + Lres.get().getString("eventGroupHelp") + ">");
    
    ff = FieldFormat.create("<" + FIELD_ED_PERMISSIONS + "><S><A=" + ServerPermissionChecker.OBSERVER_PERMISSIONS + "><D=" + Cres.get().getString("permissions") + "><H="
        + Lres.get().getString("eventPermissionsHelp") + ">");
    ff.setSelectionValues(Server.getPermissionChecker().getPermissionLevels());
    VFT_MODEL_EVENTS.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_ED_FIRE_PERMISSIONS + "><S><A=" + ServerPermissionChecker.ADMIN_PERMISSIONS + "><D=" + Cres.get().getString("firePermissions") + "><H="
        + Lres.get().getString("eventFirePermissionsHelp") + ">");
    ff.setSelectionValues(Server.getPermissionChecker().getPermissionLevels());
    VFT_MODEL_EVENTS.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_ED_HISTORY_STORAGE_TIME + "><L><F=N><D=" + Lres.get().getString("historyStoragePeriod") + "><H=" + Lres.get().getString("eventHistoryStoragePeriodHelp")
        + "><E=" + LongFieldFormat.EDITOR_PERIOD + "><O=" + LongFieldFormat.encodePeriodEditorOptions(TimeHelper.HOUR, TimeHelper.YEAR) + ">");
    VFT_MODEL_EVENTS.addField(ff);
    
    VFT_MODEL_EVENTS.addTableValidator(new TableKeyFieldsValidator());
  }
  
  public static final TableFormat VFT_BINDINGS = Bindings.FORMAT.clone().setReorderable(true);
  
  static
  {
    VFT_BINDINGS.getField(Bindings.FIELD_TARGET).setEditorOptions(StringFieldFormat.EDITOR_TARGET_MODE_MODELS);
    VFT_BINDINGS.getField(Bindings.FIELD_BINDING_ID).setKeyField(false);
    VFT_BINDINGS.getField(Bindings.FIELD_QUEUE).setHidden(true);

    for (Binding binding : VFT_BINDINGS.getBindings())
    {
      if (binding.getTarget().equals(new Reference(Bindings.FIELD_EXPRESSION + "#" + DataTableBindingProvider.PROPERTY_OPTIONS)))
      {
        VFT_BINDINGS.removeBinding(binding);
        break;
      }
    }
    
    String ref = Bindings.FIELD_TARGET + "#" + DataTableBindingProvider.PROPERTY_OPTIONS;
    VFT_BINDINGS.addBinding(ref, DEFAULT_CONTEXT_EXPRESSION_OPTIONS);
    
    ref = Bindings.FIELD_EXPRESSION + "#" + DataTableBindingProvider.PROPERTY_OPTIONS;
    VFT_BINDINGS.addBinding(ref, DEFAULT_CONTEXT_EXPRESSION_OPTIONS);
    
    ref = Bindings.FIELD_ACTIVATOR + "#" + DataTableBindingProvider.PROPERTY_OPTIONS;
    VFT_BINDINGS.addBinding(ref, DEFAULT_CONTEXT_EXPRESSION_OPTIONS);
    
    ref = Bindings.FIELD_CONDITION + "#" + DataTableBindingProvider.PROPERTY_OPTIONS;
    VFT_BINDINGS.addBinding(ref, DEFAULT_CONTEXT_EXPRESSION_OPTIONS);
  }
  
  public static final TableFormat VFT_EXPRESSION_BUILDER = ChannelProperties.FORMAT.clone();
  
  static
  {
    Reference ref = new Reference(ChannelProperties.F_EXPRESSION + "#" + DataTableBindingProvider.PROPERTY_OPTIONS);
    for (Binding binding : VFT_EXPRESSION_BUILDER.getBindings())
    {
      if (binding.getTarget().equals(ref))
      {
        VFT_EXPRESSION_BUILDER.removeBinding(binding);
        break;
      }
    }
    String exp = Function.HAS_RESOLVER + "('" + Reference.SCHEMA_PARENT + "')" + " ? ({.:childInfo$type[0]} != " + Model.TYPE_ABSOLUTE + " && !variableAvailable({.:},{parent/variable}) ? cell("
        + DefaultFunctions.CALL_FUNCTION + "({.:}, '" + DefaultFunctions.EXPRESSION_EDITOR_OPTIONS + "',{.:}, {parent/variable}))" + ":" + DefaultFunctions.EXPRESSION_EDITOR_OPTIONS + "({.:}, {" + Reference.SCHEMA_PARENT
        + "/" + ContextStatistics.F_VARIABLE + "}, " + ContextUtils.ENTITY_VARIABLE + ")): null";
    
    VFT_EXPRESSION_BUILDER.addBinding(ref, new Expression(exp));
  }
  
  public static final TableFormat FOFT_EXPRESSION_BUILDER = new TableFormat(1, 1);
  
  static
  {
    FOFT_EXPRESSION_BUILDER.addField("<" + DefaultFunctions.EXPRESSION_EDITOR_OPTIONS + "><S>");
  }
  
  public static final TableFormat VFT_STATISTICS_CHANNEL = ContextStatistics.VFT_DEFAULT_CHANNEL_STATISTICS_PROPERTIES.clone();
  
  static
  {
    String ref = V_UPDATE_VARIABLE + "#" + DataTableBindingProvider.PROPERTY_CHOICES;
    String exp = DefaultFunctions.CALL_FUNCTION + "({.:}, '" + UtilitiesContextConstants.F_VARIABLES_BY_MASK + "',{.:})";
    VFT_STATISTICS_CHANNEL.addBinding(ref, exp);
    
    VFT_STATISTICS_CHANNEL.removeField(ContextStatistics.F_PARAMETERS);
    VFT_STATISTICS_CHANNEL.addField(FieldFormat
        .create("<" + ContextStatistics.F_PARAMETERS + "><T><F=N><D=" + Cres.get().getString("parameters") + "><H=" + Lsres.get().getString("spcHelpParameters") + "><I=" + Icons.VAR_STATISTICS + ">")
        .setDefault(new ChannelProperties(VFT_EXPRESSION_BUILDER).toDataTable()));
  }

  public static final TableFormat VFT_GRANULATOR_PROPERTIES = Granulator.VFT_PROPERTIES.clone();

  static
  {
    String ref = Granulator.F_VARIABLE + "#" + DataTableBindingProvider.PROPERTY_CHOICES;
    String exp = "{.:" + UtilitiesContextConstants.F_VARIABLES_BY_MASK + "({.:})}";

    for (Binding binding : VFT_GRANULATOR_PROPERTIES.getBindings())
    {
      if (ref.equals(binding.getTarget().getImage()))
      {
        VFT_GRANULATOR_PROPERTIES.removeBinding(binding);
        break;
      }
    }

    VFT_GRANULATOR_PROPERTIES.addBinding(ref, exp);
  }
  
  public static final TableFormat VFT_THREAD_POOL_STATUS = new TableFormat(1, 1);
  
  static
  {
    VFT_THREAD_POOL_STATUS.addField("<" + RootContextConstants.V_THREAD_POOLS_ACTIVE_COUNT + "><I><D=" + Lsres.get().getString("srvPoolActiveCount") + ">");
    VFT_THREAD_POOL_STATUS.addField("<" + RootContextConstants.V_THREAD_POOLS_COMPLETED_COUNT + "><L><D=" + Lsres.get().getString("srvPoolCompletedCount") + ">");
    VFT_THREAD_POOL_STATUS.addField("<" + RootContextConstants.V_THREAD_POOLS_TOTAL_COUNT + "><L><D=" + Lsres.get().getString("srvPoolTotalCount") + ">");
    VFT_THREAD_POOL_STATUS.addField("<" + RootContextConstants.V_THREAD_POOLS_CORE_SIZE + "><I><D=" + Lsres.get().getString("srvPoolCoreSize") + ">");
    VFT_THREAD_POOL_STATUS.addField("<" + RootContextConstants.V_THREAD_POOLS_LARGEST_SIZE + "><I><D=" + Lsres.get().getString("srvPoolLargestSize") + ">");
    VFT_THREAD_POOL_STATUS.addField("<" + RootContextConstants.V_THREAD_POOLS_MAXIMUM_SIZE + "><I><D=" + Lsres.get().getString("srvPoolMaximumSize") + ">");
    VFT_THREAD_POOL_STATUS.addField("<" + RootContextConstants.V_THREAD_POOLS_QUEUE_LENGTH + "><I><D=" + Lsres.get().getString("srvPoolQueueLength") + ">");
    VFT_THREAD_POOL_STATUS.addField("<" + V_THREAD_POOL_REJECTED_COUNT + "><L><D=" + Lsres.get().getString("srvPoolRejectedCount") + ">");
  }
  
  protected static final VariableDefinition VD_MODEL_VARIABLES = new VariableDefinition(V_MODEL_VARIABLES, VFT_MODEL_VARIABLES, true, true, Lres.get().getString("modelVariables"),
      ContextUtils.GROUP_DEFAULT);
  
  static
  {
    VD_MODEL_VARIABLES.setWritePermissions(ServerPermissionChecker.getEngineerPermissions());
    VD_MODEL_VARIABLES.setIconId(Icons.ES_VARIABLE);
    VD_MODEL_VARIABLES.setHelpId(Docs.LS_MODELS_CONFIGURATION_VARIABLES);
    VD_MODEL_VARIABLES.setHelp(Lres.get().getString("variableHelp"));
    VD_MODEL_VARIABLES.setLocalCachingMode(VariableDefinition.CACHING_SOFT);
    
  }
  
  protected static final VariableDefinition VD_MODEL_FUNCTIONS = new VariableDefinition(V_MODEL_FUNCTIONS, VFT_MODEL_FUNCTIONS, true, true, Lres.get().getString("modelFunctions"),
      ContextUtils.GROUP_DEFAULT);
  
  static
  {
    VD_MODEL_FUNCTIONS.setWritePermissions(ServerPermissionChecker.getEngineerPermissions());
    VD_MODEL_FUNCTIONS.setIconId(Icons.ES_FUNCTION);
    VD_MODEL_FUNCTIONS.setHelpId(Docs.LS_MODELS_CONFIGURATION_FUNCTIONS);
    VD_MODEL_FUNCTIONS.setHelp(Lres.get().getString("functionHelp"));
    VD_MODEL_FUNCTIONS.setLocalCachingMode(VariableDefinition.CACHING_SOFT);
    VD_MODEL_FUNCTIONS.addCompatibilityConverter(new TableFieldsCompatibilityConverter(ModelContextConstants.OLD_FIELD_FD_EXPRESSION,
        ModelContextConstants.FIELD_FD_EXPRESSION));
  }
  
  protected static final VariableDefinition VD_MODEL_EVENTS = new VariableDefinition(V_MODEL_EVENTS, VFT_MODEL_EVENTS, true, true, Lres.get().getString("modelEvents"), ContextUtils.GROUP_DEFAULT);
  
  static
  {
    VD_MODEL_EVENTS.setWritePermissions(ServerPermissionChecker.getEngineerPermissions());
    VD_MODEL_EVENTS.setIconId(Icons.ES_EVENT);
    VD_MODEL_EVENTS.setHelpId(Docs.LS_MODELS_CONFIGURATION_EVENTS);
    VD_MODEL_EVENTS.setHelp(Lres.get().getString("eventHelp"));
    VD_MODEL_EVENTS.setLocalCachingMode(VariableDefinition.CACHING_SOFT);
  }
  
  protected static final VariableDefinition VD_BINDINGS = new VariableDefinition(V_BINDINGS, VFT_BINDINGS, true, true, Cres.get().getString("bindings"), ContextUtils.GROUP_DEFAULT);
  
  static
  {
    VD_BINDINGS.setWritePermissions(ServerPermissionChecker.getEngineerPermissions());
    VD_BINDINGS.setIconId(Icons.DTE_BINDINGS);
    VD_BINDINGS.setHelpId(Docs.LS_MODELS_CONFIGURATION_BINDINGS);
    VD_BINDINGS.setHelp(Lres.get().getString("bindingsHelp"));
  }
  
  protected static final VariableDefinition VD_RULE_SETS = new VariableDefinition(V_RULE_SETS, RuleSet.FORMAT, true, true, Lres.get().getString("ruleSets"), ContextUtils.GROUP_DEFAULT);
  
  static
  {
    VD_RULE_SETS.setWritePermissions(ServerPermissionChecker.getEngineerPermissions());
    VD_RULE_SETS.setHelpId(Docs.LS_MODELS_CONFIGURATION_RULE_SETS);
    VD_RULE_SETS.setHelp(Lres.get().getString("ruleSetsHelp"));
  }
  
  static
  {
    String type = ContextUtils.getTypeForClass(ModelContext.class);
    Server.getMaskRepository().registerMask(type, ContextUtils.modelContextPath(ContextUtils.USERNAME_PATTERN, ContextUtils.CONTEXT_GROUP_MASK), Lsres.get().getString("conMaskUserResources"));
    Server.getMaskRepository().registerMask(type, ContextUtils.modelContextPath(ContextUtils.CONTEXT_GROUP_MASK, ContextUtils.CONTEXT_GROUP_MASK), Lsres.get().getString("conMaskAllResources"));
  }
  
  private final NamedThreadFactory executorFactory = new NamedThreadFactory()
  {
    @Override
    protected String getName()
    {
      return "ModelBindingExecutor/" + ModelContext.this;
    }
  };
  
  private Timer timer;
  
  private ThreadPoolExecutor executorService;
  
  private final Map<String, ModelProcessor> processors = new ConcurrentHashMap<>();
  
  private Model oldModel; // This field is valid only during model information update process, it represents the old instance of the model
  private Model newModel; // This field is valid only during model information update process, it represents the new instance of the model
  
  private Long rejectedCount = 0L;
  private Long rejectedCountSinceLastBindingsOverflowEvent = 0L;
  
  private Long timeSinceLastBindingsOverflowLogMessage = null;
  private Long timeSinceLastBindingsOverflowEvent = null;
  
  private final Map<String, VariableDefinition> variableDefinitionCache = Collections.synchronizedMap(new HashMap<>());
  private final Map<String, FunctionDefinition> functionDefinitionCache = Collections.synchronizedMap(new HashMap<>());
  private final Map<String, EventDefinition> eventDefinitionCache = Collections.synchronizedMap(new HashMap<>());

  private final Map<String, SoftReference<Object>> compiledMethodCache = Collections.synchronizedMap(new HashMap<>());
  
  public ModelContext(UserContext userContext, String name)
  {
    super(name, true, Log.MODELS, userContext.getCallerController());
  }
  
  public ModelContext(CallerController callerController, String name, boolean allowMakeCopy)
  {
    super(name, allowMakeCopy, Log.MODELS, callerController);
  }

  @Override
  public void setupMyself() throws ContextException
  {
    super.setupMyself();
    
    addChildInfoVariable(Model.FORMAT, false, Model.class, Docs.LS_MODELS_PROPERTIES).setHelp(Lres.get().getString("propertiesHelp"));
    
    DataTable childInfo = getVariable(V_CHILD_INFO, getContextManager().getCallerController());
    setProtected(childInfo.rec().getBoolean(EditableChildContextConstants.VF_CHILD_INFO_PROTECTED));
    
    addVariableDefinition(VD_MODEL_VARIABLES);
    addVariableDefinition(VD_MODEL_FUNCTIONS);
    addVariableDefinition(VD_MODEL_EVENTS);
    addVariableDefinition(VD_BINDINGS);
    addVariableDefinition(VD_RULE_SETS);
    
    VariableDefinition vd = new VariableDefinition(V_THREAD_POOL_STATUS, VFT_THREAD_POOL_STATUS, true, false, Lsres.get().getString("srvPoolStats"), ContextUtils.GROUP_STATUS);
    addVariableDefinition(vd);
    
    FunctionDefinition fd = StorageHelper.FD_STORAGE_OPEN.clone();
    fd.setPermissions(ServerPermissionChecker.getObserverPermissions());
    addFunctionDefinition(fd);
    
    fd = StorageHelper.FD_STORAGE_CLOSE.clone();
    fd.setPermissions(ServerPermissionChecker.getObserverPermissions());
    addFunctionDefinition(fd);
    
    fd = StorageHelper.FD_STORAGE_GET.clone();
    fd.setPermissions(ServerPermissionChecker.getObserverPermissions());
    addFunctionDefinition(fd);
    
    fd = StorageHelper.FD_STORAGE_UPDATE.clone();
    fd.setPermissions(ServerPermissionChecker.getObserverPermissions());
    addFunctionDefinition(fd);
    
    fd = StorageHelper.FD_STORAGE_DELETE.clone();
    fd.setPermissions(ServerPermissionChecker.getObserverPermissions());
    addFunctionDefinition(fd);
    
    fd = StorageHelper.FD_STORAGE_INSERT.clone();
    fd.setPermissions(ServerPermissionChecker.getObserverPermissions());
    addFunctionDefinition(fd);
    
    fd = StorageHelper.FD_STORAGE_VIEWS.clone();
    fd.setPermissions(ServerPermissionChecker.getObserverPermissions());
    addFunctionDefinition(fd);
    
    fd = StorageHelper.FD_STORAGE_TABLES.clone();
    fd.setPermissions(ServerPermissionChecker.getObserverPermissions());
    addFunctionDefinition(fd);
    
    fd = StorageHelper.FD_STORAGE_COLUMNS.clone();
    fd.setPermissions(ServerPermissionChecker.getObserverPermissions());
    addFunctionDefinition(fd);
    
    fd = StorageHelper.FD_STORAGE_RELATIONS.clone();
    fd.setPermissions(ServerPermissionChecker.getObserverPermissions());
    addFunctionDefinition(fd);
    
    fd = StorageHelper.FD_STORAGE_FILTER.clone();
    fd.setPermissions(ServerPermissionChecker.getObserverPermissions());
    addFunctionDefinition(fd);
    
    fd = StorageHelper.FD_STORAGE_SORTING.clone();
    fd.setPermissions(ServerPermissionChecker.getObserverPermissions());
    addFunctionDefinition(fd);
    
    fd = StorageHelper.FD_STORAGE_OPERATIONS.clone();
    fd.setPermissions(ServerPermissionChecker.getObserverPermissions());
    addFunctionDefinition(fd);
    
    fd = UtilitiesContext.FD_VARIABLES_BY_MASK.clone();
    fd.setPermissions(ServerPermissionChecker.getManagerPermissions());
    addFunctionDefinition(fd);
    
    fd = new FunctionDefinition(Function.EXPRESSION_EDITOR_OPTIONS, null, FOFT_EXPRESSION_BUILDER, null, ContextUtils.GROUP_DEFAULT);
    fd.setPermissions(ServerPermissionChecker.getManagerPermissions());
    addFunctionDefinition(fd);
    
    EventDefinition ed = new EventDefinition(BindingEventsHelper.E_BINDING_EXECUTION, BindingEventsHelper.EFT_BINDING_EXECUTION_EXT, Cres.get().getString("wBindingExecution"),
        ContextUtils.GROUP_DEFAULT);
    addEventDefinition(ed);
    
    ed = new EventDefinition(BindingEventsHelper.E_BINDING_ERROR, BindingEventsHelper.EFT_BINDING_ERROR_EXT, Cres.get().getString("wBindingError"), ContextUtils.GROUP_DEFAULT);
    addEventDefinition(ed);
    
    addConfigureAction(true);
    
    addDeleteAction();
    
    addReplicateAction(false, getType());
    
    setIconId(Icons.ST_MODEL);
    
    addHelpAction(Docs.LS_MODELS);
    
    enableStatus();
    
    updateStatus(getModel());
    
    enableContextStatistics(ContextUtils.GROUP_DEFAULT, VFT_STATISTICS_CHANNEL);
    
    enableGranulation(this.getCallerController(), ContextUtils.GROUP_DEFAULT);
    
    getRoot().addEventListener(RootContextConstants.E_CONTEXT_DESTROYED, new ContextDestroyedListener());
    getRoot().addEventListener(RootContextConstants.E_CONTEXT_RELOCATED, new ContextRelocatedListener());

    setValidityExpressionHolder(new Reference(V_CHILD_INFO, ContextUtils.ENTITY_VARIABLE, Model.FIELD_VALIDITY_EXPRESSION));
  }
  
  @Override
  protected void enableContextStatistics(String group, TableFormat statisticsPropertiesFormat) throws ContextException
  {
    super.enableContextStatistics(group, statisticsPropertiesFormat);
    
    if (getModel().getType() == Model.TYPE_INSTANTIABLE)
    {
      CallerController aCallerController = isProtected() ? Server.getCallerController() : getCallerController();
      
      VariableDefinition vd = getVariableDefinition(V_STATISTICS_PROPERTIES, aCallerController);
      vd.setSetter((con, def, caller, request, value) -> {
        final BaseServerContext context = (BaseServerContext) con;
        
        ContextStatistics.validateStatisticsProperties(context, value);
        
        // channel will be added, but statistics will not be updated for this model context, because is doesn't possess channel's variable
        context.getContextStatistics().setStatisticsProperties(value, null);
        context.saveStatistics();
        
        for (Context target : getTargetContexts(getModel()))
        {
          Context container = target.getChild(getModel().getContainerName(), caller);
          
          if (container != null)
          {
            List<ServerContext> instances = container.getChildren(caller);
            for (ServerContext instance : instances)
            {
              instance.setVariable(V_STATISTICS_PROPERTIES, caller, value.clone());
            }
          }
        }
        
        return true;
      });
    }
  }
  
  @Override
  public void enableGranulation(CallerController aCallerController, String groupString) throws ContextException
  {
    if (getModel().getType() != Model.TYPE_ABSOLUTE)
    {
      super.enableGranulation(aCallerController, groupString, VFT_GRANULATOR_PROPERTIES);

      if (isProtected())
      {
        aCallerController = Server.getCallerController();
      }
      
      getVariableDefinition(V_GRANULATOR, aCallerController).setSetter((con, def, caller, request, value) -> {
        EditableChildContext editableChildContext = (EditableChildContext) con;
        
        editableChildContext.executeDefaultSetter(def, caller, value);
        
        for (Context target : getTargetContexts(getModel()))
        {
          Context container = target.getChild(getModel().getContainerName(), getContextManager().getCallerController());
          
          if (container != null)
          {
            List<ServerContext> instances = container.getChildren(caller);
            for (ServerContext instance : instances)
            {
              InstantiableModelContext instanceContext = (InstantiableModelContext) instance;
              
              // Instance granulator gets caller controller from model owner (not from user who changes variable)
              instanceContext.setupGranulator(getCallerController());
            }
          }
        }
        
        return true;
      });
    }
    else
    {
      super.enableGranulation(aCallerController, groupString);
    }
  }
  
  @Override
  public void start()
  {
    timer = new Timer("ModelTimer/" + this, true);
    
    Model model = getModel();
    
    executorService = makeExecutorService(model);
    
    super.start();
    
    installToContext(model);
  }
  
  protected void installToContext(Model model)
  {
    try
    {
      if (model.getType() == Model.TYPE_ABSOLUTE)
      {
        installToContext(this, model, getContextManager().getCallerController());
      }
    }
    catch (Exception ex)
    {
      logInstallationError(this, ex);
    }
  }
  
  protected ThreadPoolExecutor makeExecutorService(Model model)
  {
    ThreadPoolExecutor es = new ThreadPoolExecutor(
        model.getNormalConcurrentBindings(),
        model.getMaximumConcurrentBindings(),
        60L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue(model.getMaximumBindingQueueLength()),
        executorFactory,
        new ModelBindingRejectedExecutionHandler());
    es.allowCoreThreadTimeOut(true);
    return es;
  }
  
  @Override
  public void stop()
  {
    if (timer != null)
    {
      timer.cancel();
    }
    
    if (executorService != null)
    {
      executorService.shutdownNow();
    }
    
    super.stop();
  }
  
  @Override
  protected boolean isGenerateAttachedEvents()
  {
    return getModel().isGenerateAttachedEvents();
  }

  private void updateStatus(Model model)
  {
    int status = model.isEnabled() ? model.getType() : STATUS_DISABLED;
    
    String comment;
    if (model.isEnabled())
    {
      switch (model.getType())
      {
        case Model.TYPE_RELATIVE:
          comment = Cres.get().getString("relative");
          break;
        
        case Model.TYPE_ABSOLUTE:
          comment = Cres.get().getString("absolute");
          break;
        
        case Model.TYPE_INSTANTIABLE:
          comment = Lres.get().getString("instantiable");
          break;
        
        default:
          throw new IllegalStateException("Unknown model type: " + model.getType());
      }
    }
    else
    {
      comment = Cres.get().getString("disabled");
    }
    
    setStatus(status, comment);
  }
  
  public DataTable getVthreadPoolStatus(VariableDefinition def, CallerController caller, RequestController request) throws ContextException
  {
    DataTable res = new SimpleDataTable(def.getFormat());
    
    DataRecord addedRecord = res.addRecord();
    RootContext.initThreadPoolRecord(addedRecord, null, executorService);
    addedRecord.setValue(V_THREAD_POOL_REJECTED_COUNT, rejectedCount);
    return res;
  }
  
  @Override
  protected Expression getCurrentValidityExpression()
  {
    return getModel().getType() != Model.TYPE_ABSOLUTE ? getModel().getCachedValidityExpression() : null;
  }
  
  @Override
  protected List<ValidityListenerInfo> getCurrentValidityListeners()
  {
    return getModel().getValidityListeners();
  }
  
  @Override
  protected void install(ServerContext context) throws ContextException
  {
    installToContext(context, getModel(), getContextManager().getCallerController());
  }
  
  @Override
  protected void uninstall(ServerContext context, boolean onDestroy) throws ContextException
  {
    uninstallFromContext(context, oldModel != null ? oldModel : getModel(), getContextManager().getCallerController(), onDestroy);
  }
  
  @Override
  protected boolean canBeInstalled(Context context)
  {
    return context.isInstallationAllowed(getModel().getContainerName());
  }

  @Override
  public synchronized void setVchildInfo(VariableDefinition def, CallerController caller, RequestController request, DataTable value) throws ContextException
  {
    oldModel = getModelFromVariable();
    
    try
    {
      checkChildInfo(value);
      
      super.setVchildInfo(def, caller, request, value);
      
      variableDefinitionCache.clear();
      functionDefinitionCache.clear();
      eventDefinitionCache.clear();

      setProtected(value.rec().getBoolean(EditableChildContextConstants.VF_CHILD_INFO_PROTECTED));
      
      newModel = getModelFromVariable();
      
      Expression newValidityExpression = newModel.getType() != Model.TYPE_ABSOLUTE ? newModel.getCachedValidityExpression() : null;
      Expression oldValidityExpression = oldModel.getType() != Model.TYPE_ABSOLUTE ? oldModel.getCachedValidityExpression() : null;
      
      updateValidityExpression(newValidityExpression, oldValidityExpression, caller);
      
      List<ValidityListenerInfo> newValidityListeners = newModel.getType() != Model.TYPE_ABSOLUTE ? newModel.getValidityListeners() : null;
      List<ValidityListenerInfo> oldValidityListeners = oldModel.getType() != Model.TYPE_ABSOLUTE ? oldModel.getValidityListeners() : null;
      
      updateValidityListeners(newValidityListeners, oldValidityListeners);
      
      if (!Util.equals(newModel.getType(), oldModel.getType()) || (!Util.equals(newModel.getDescription(), oldModel.getDescription()) && newModel.getType() != Model.TYPE_INSTANTIABLE))
      {
        Set<Context> contexts = getTargetContexts(oldModel);
        for (Context con : contexts)
        {
          super.performUninstall(con, false);
        }
        
        contexts = getTargetContexts(newModel);
        for (Context con : contexts)
        {
          performInstall(con);
        }
      }
      
      if (executorService != null)
      {
        if (newModel.getMaximumBindingQueueLength() != oldModel.getMaximumBindingQueueLength())
        {
          executorService.shutdownNow();
          executorService = makeExecutorService(newModel);
          installToContext(newModel);
          
          for (Context con : getTargetContexts(newModel))
            updateProcessor(con);
        }
        else
        {
          if (newModel.getMaximumConcurrentBindings() > oldModel.getMaximumConcurrentBindings())
          {
            // Maximum first
            executorService.setMaximumPoolSize(newModel.getMaximumConcurrentBindings());
            executorService.setCorePoolSize(newModel.getNormalConcurrentBindings());
          }
          else if (newModel.getMaximumConcurrentBindings() < oldModel.getMaximumConcurrentBindings())
          {
            // Core first
            executorService.setCorePoolSize(newModel.getNormalConcurrentBindings());
            executorService.setMaximumPoolSize(newModel.getMaximumConcurrentBindings());
          }
          else
          {
            executorService.setCorePoolSize(newModel.getNormalConcurrentBindings());
          }
        }
      }
      
      if (!Util.equals(oldModel.isEnabled(), newModel.isEnabled()))
      {
        updateModelProcessors();
      }
      
      String newObjectNamingExpression = newModel.getType() == Model.TYPE_INSTANTIABLE ? newModel.getObjectNamingExpression() : null;
      String oldObjectNamingExpression = oldModel.getType() == Model.TYPE_INSTANTIABLE ? oldModel.getObjectNamingExpression() : null;
      
      if (!Util.equals(newObjectNamingExpression, oldObjectNamingExpression))
      {
        Set<Context> contexts = getTargetContexts(new Expression("{.:#type} == \"" + newModel.getObjectType() + "\""));
        for (Context context : contexts)
        {
          if (context instanceof InstantiableModelContext)
          {
            ((InstantiableModelContext) context).updateDescriptionByNamingExpression(caller);
          }
        }
      }
      
      if (newModel.getType() == Model.TYPE_INSTANTIABLE && !Util.equals(oldModel.getContainerName(), newModel.getContainerName()))
      {
        moveInstantiableContainer();
      }
      
      updateStatus(newModel);
    }
    finally
    {
      oldModel = null;
      newModel = null;
    }
  }
  
  private void checkChildInfo(DataTable value) throws ContextException
  {
    // This check should prevent multiple models from being mounted in one container
    DataRecord record = value.rec();
    
    if (record.getInt(Model.FIELD_TYPE) != Model.TYPE_INSTANTIABLE)
      return;
    
    String newContainerName = record.getString(Model.FIELD_CONTAINER_NAME);
    String oldContainerName = oldModel.getContainerName();
    
    if (Objects.equals(oldContainerName, newContainerName))
      return;
    
    Expression validityExpression = new Expression(record.getString(Model.FIELD_VALIDITY_EXPRESSION));
    
    checkIfContainerAlreadyExists(newContainerName, validityExpression);
  }
  
  private void checkIfContainerAlreadyExists(String containerName, Expression validityExpression) throws ContextException
  {
    Set<Context> targetContexts = getTargetContexts(validityExpression);
    
    for (Context parentContext : targetContexts)
    {
      Context containerContext = parentContext.getChild(containerName, getContextManager().getCallerController());
      if (containerContext != null)
      {
        throw new ContextException(MessageFormat.format(Cres.get().getString("conChildExists"), containerName, parentContext));
      }
    }
  }
  
  @Override
  public boolean isEntityProtected(EntityDefinition ed)
  {
    if (!isProtected())
      return false;
    
    String entityGroup = ed != null ? ContextUtils.getBaseGroup(ed.getGroup()) : null;
    Integer entityType = ed != null ? ed.getEntityType() : null;
    
    if (Objects.equals(ContextUtils.ENTITY_VARIABLE, entityType) && Objects.equals(ContextUtils.GROUP_DEFAULT, entityGroup))
    {
      return true;
    }
    
    String entityName = ed != null ? ed.getName() : null;
    
    if (Objects.equals(ContextUtils.ENTITY_ACTION, entityType))
    {
      return Objects.equals(A_MAKE_COPY, entityName) || Objects.equals(A_REPLICATE, entityName) || Objects.equals(A_CONFIGURE, entityName);
    }
    
    return false;
  }
  
  private void updateModelProcessors() throws ContextException
  {
    if (oldModel.isEnabled())
    {
      processors.values().forEach(v -> v.setEnabled(newModel.isEnabled()));
    }
    else
    {
      for (ModelProcessor modelProcessor : processors.values())
      {
        updateProcessor(modelProcessor.getTargetContext());
      }
    }
  }
  
  private void moveInstantiableContainer() throws ContextException
  {
    Set<Context> oldContexts = getTargetContexts(oldModel);
    Set<Context> newContexts = getTargetContexts(newModel);
    
    for (Context parentContext : oldContexts)
    {
      if (!newContexts.contains(parentContext))
        continue;
      
      Context containerContext = parentContext.getChild(newModel.getContainerName(), getContextManager().getCallerController());
      if (containerContext == null)
      {
        containerContext = new InstantiableModelContainer(this, newModel.getContainerName(), newModel.getContainerType(), newModel.getContainerTypeDescription(), newModel.getObjectTypeDescription(),
            newModel.getObjectType());
        
        parentContext.addChild(containerContext);
        ((ServerContext) parentContext).addVisibleChild(containerContext.getPath());
      }
      Context oldContainer = parentContext.getChild(oldModel.getContainerName(), getContextManager().getCallerController());
      
      if (oldContainer == null)
        continue;
      
      moveChildrenToNewContainer(oldContainer, containerContext);
    }
  }
  
  private void moveChildrenToNewContainer(Context oldContainer, Context containerContext) throws ContextException
  {
    List<Context> childList = oldContainer.getChildren(getCallerController());
    
    for (Context child : childList)
    {
      try
      {
        BaseServerContext childContext = ((BaseServerContext) child);
        String childName = childContext.getName();
        childContext.move((ServerContext) containerContext, childName);
      }
      catch (Exception cce)
      {
        Log.MODELS.warn("Couldn't move child context from: " + oldContainer.getName() + " to: " + containerContext.getName(), cce);
      }
    }
    containerContext.setVariable(EditableChildrenContextConstants.V_CHILD_LIST, getCallerController(), oldContainer.getVariable(EditableChildrenContextConstants.V_CHILD_LIST, getCallerController()));
    oldContainer.setVariable(EditableChildrenContextConstants.V_CHILD_LIST, getCallerController(), new SimpleDataTable());
    oldContainer.destroy(false);
  }
  
  protected Set<Context> getTargetContexts(Model model) throws ContextException
  {
    return model.getType() == Model.TYPE_ABSOLUTE ? Collections.singleton((Context) this) : getTargetContexts(model.getCachedValidityExpression());
  }
  
  public synchronized void setVmodelVariables(VariableDefinition def, CallerController caller, RequestController request, DataTable value) throws ContextException
  {
    checkAndUpdateLicensedItems(value, FIELD_VD_NAME, LicensingUnitsConstants.LU_VARIABLE);
    
    variableDefinitionCache.clear();

    executeDefaultSetter(def, caller, value);
    
    updateVariables(caller);
  }
  
  public synchronized void setVmodelFunctions(VariableDefinition def, CallerController caller, RequestController request, DataTable value) throws ContextException
  {
    checkAndUpdateLicensedItems(value, FIELD_FD_NAME, LicensingUnitsConstants.LU_FUNCTION);
    
    functionDefinitionCache.clear();

    DataTable oldFunctions = executeDefaultGetter(def.getName(), caller, false, false);
    
    checkIfFunctionsChangeIsAllowed(oldFunctions, value, caller);
    
    executeDefaultSetter(def, caller, value);
    
    updateFunctions(caller, value);
  }

  public synchronized void setVmodelEvents(VariableDefinition def, CallerController caller, RequestController request, DataTable value) throws ContextException
  {
    checkAndUpdateLicensedItems(value, FIELD_ED_NAME, LicensingUnitsConstants.LU_EVENT);

    eventDefinitionCache.clear();

    executeDefaultSetter(def, caller, value);

    updateEvents(caller);
  }

  private void updateVariables(CallerController caller) throws ContextException
  {
    long startTime = System.currentTimeMillis();

    DataTable modelVariables = getVariable(V_MODEL_VARIABLES, caller);

    updateTargetContexts(caller, new ModelTargetUpdater()
    {
      @Override
      public void updateInstance(ServerContext instance) throws ContextException
      {
        updateVariables(instance, getModel(), modelVariables, caller);
      }

      @Override
      public void updateTarget(Context target) throws ContextException
      {
        updateVariables((ServerContext) target, getModel(), modelVariables, caller);
      }
    });

    Log.PERFORMANCE.debug("Update variables in model: " + "'" + getModel().getName() + "' took: " + (System.currentTimeMillis() - startTime) + " milliseconds");
  }

  private void updateFunctions(CallerController caller, DataTable value) throws ContextException
  {
    long startTime = System.currentTimeMillis();

    DataTable modelFunctions = getVariable(V_MODEL_FUNCTIONS, caller);
    DataTable modelRuleSets = getVariable(V_RULE_SETS, caller);

    updateTargetContexts(caller, new ModelTargetUpdater()
    {
      @Override
      public void updateInstance(ServerContext instance) throws ContextException
      {
        updateFunctions(instance, getModel(), modelFunctions, modelRuleSets, caller, null);
      }

      @Override
      public void updateTarget(Context target) throws ContextException
      {
        updateFunctions(target, getModel(), modelFunctions, modelRuleSets, caller, value);
      }
    });

    Log.PERFORMANCE.debug("Update functions in model: " + "'" + getModel().getName() + "' took: " + (System.currentTimeMillis() - startTime) + " milliseconds");
  }

  private void updateEvents(CallerController caller) throws ContextException
  {
    long startTime = System.currentTimeMillis();

    DataTable modelEvents = getVariable(V_MODEL_EVENTS, caller);

    updateTargetContexts(caller, new ModelTargetUpdater()
    {
      @Override
      public void updateInstance(ServerContext instance) throws ContextException
      {
        updateEvents(instance, getModel(), modelEvents);
      }

      @Override
      public void updateTarget(Context target) throws ContextException
      {
        updateEvents(target, getModel(), modelEvents);
      }
    });

    Log.PERFORMANCE.debug("Update events in model: " + "'" + getModel().getName() + "' took: " + (System.currentTimeMillis() - startTime) + " milliseconds");
  }

  private void updateTargetContexts(CallerController caller, ModelTargetUpdater modelTargetUpdater) throws ContextException
  {
    Set<Context> targetContexts = getTargetContexts(getModel());
    List<Callable<Object>> tasks = new LinkedList();
    for (Context target : targetContexts)
    {
      tasks.add(() -> {
        if (getModel().getType() == Model.TYPE_INSTANTIABLE)
        {
          Context container = target.getChild(getModel().getContainerName(), getContextManager().getCallerController());
          if (container != null)
          {
            List<ServerContext> instances = container.getChildren(caller);
            List<Callable<Object>> subtasks = new LinkedList();
            for (ServerContext instance : instances)
            {
              subtasks.add(() -> {
                modelTargetUpdater.updateInstance(instance);
                return null;
              });
            }
            executeTasksConcurrently(subtasks, MODEL_ATTACHING_LOAD_FACTOR);
          }
        }
        else
        {
          modelTargetUpdater.updateTarget(target);
        }
        return null;
      });
    }
    try
    {
      executeTasksConcurrently(tasks, MODEL_ATTACHING_LOAD_FACTOR);
    }
    catch (Exception e)
    {
      throw new ContextException(e);
    }
  }
  
  private void checkIfFunctionsChangeIsAllowed(DataTable oldFunctions, DataTable newFunctions, CallerController caller) throws ContextException
  {
    if (!caller.isPermissionCheckingEnabled())
    {
      // Some import modes use Server.getCallerController(). They do not pass validation in this method.
      // TODO Implement import with checking permissions for variables or improve verification below.
      return;
    }
    
    String permissionLevel = caller != null
        ? Server.getPermissionChecker().getLevel(caller.getPermissions(), getPath(), ContextUtils.ENTITY_VARIABLE, ModelContextConstants.V_MODEL_FUNCTIONS, null, getContextManager())
        : null;
    
    List<DataRecord> oldFunctionsList = getJavaFunctions(oldFunctions);
    List<DataRecord> newFunctionsList = getJavaFunctions(newFunctions);
    Iterator<DataRecord> oldFunctionIterator = oldFunctionsList.iterator();
    
    while (oldFunctionIterator.hasNext())
    {
      DataRecord record = oldFunctionIterator.next();
      if (newFunctionsList.remove(record))
      {
        oldFunctionIterator.remove();
      }
    }
    
    if ((newFunctionsList.size() > 0 || oldFunctionsList.size() > 0) && !Util.equals(ServerPermissionChecker.ADMIN_PERMISSIONS, permissionLevel))
    {
      throw new ContextSecurityException(
          MessageFormat.format(Cres.get().getString("conAccessDenied"), getPath(), caller != null ? caller.getPermissions() : "", ServerPermissionChecker.ADMIN_PERMISSIONS));
    }
  }
  
  private List<DataRecord> getJavaFunctions(DataTable functions)
  {
    if (functions == null)
      return new LinkedList<DataRecord>();
    
    return functions.stream().filter(v -> v.hasField(FIELD_FD_TYPE) && v.getInt(FIELD_FD_TYPE) == FUNCTION_TYPE_JAVA).collect(Collectors.toList());
  }

  private void checkAndUpdateLicensedItems(DataTable value, String fieldName, String licensingUnit) throws LicenseViolationException
  {
    Set<String> names = new HashSet<>();
    value.forEach(rec -> names.add(rec.getString(fieldName)));
    getLicenseContextStatisticsRegistry().checkAndUpdateLicensedItems(this.getPath(), names, licensingUnit);
  }
  
  private void checkAndUpdateLicensedItems(Set<String> value, String licensingUnit) throws LicenseViolationException
  {
    Set<String> names = new HashSet<>(value);
    getLicenseContextStatisticsRegistry().checkAndUpdateLicensedItems(this.getPath(), names, licensingUnit);
  }
  
  public synchronized void setVbindings(VariableDefinition def, CallerController caller, RequestController request, DataTable value) throws ContextException
  {
    executeDefaultSetter(def, caller, value);
    
    for (Context target : getTargetContexts(getModel()))
    {
      if (getModel().getType() == Model.TYPE_INSTANTIABLE)
      {
        Context container = target.getChild(getModel().getContainerName(), getContextManager().getCallerController());
        if (container != null)
        {
          List<ServerContext> instances = container.getChildren(caller);
          for (ServerContext instance : instances)
          {
            updateProcessor(instance);
          }
        }
      }
      else
      {
        updateProcessor(target);
      }
    }
  }
  
  public synchronized void setVruleSets(VariableDefinition def, CallerController caller, RequestController request, DataTable value) throws ContextException
  {
    functionDefinitionCache.clear();

    executeDefaultSetter(def, caller, value);
    
    updateFunctions(caller, null);
  }
  
  private boolean preparedToUpdate = false;
  
  @Override
  public void updatePrepare()
  {
    super.updatePrepare();
    
    preparedToUpdate = true;
  }
  
  private void installToContext(ServerContext target, Model model, CallerController caller) throws ContextException
  {
    if (model.getType() == Model.TYPE_INSTANTIABLE)
    {

      if (!isAllowedOperation(target))
      {
        String errorMessage = MessageFormat.format(Lres.get().getString("errInstallingModel"), target.toString());
        throw new ContextRuntimeException(errorMessage);
      }

      ServerContext container = (ServerContext) target.getChild(model.getContainerName(), caller);
      if (container == null)
      {
        container = new InstantiableModelContainer(this, model.getContainerName(), model.getContainerType(), model.getContainerTypeDescription(), model.getObjectTypeDescription(),
            model.getObjectType(), getUserContext());
        
        target.addChild(container);
        
        target.addVisibleChild(container.getPath());
      }
      else
      {
        Log.MODELS.warn("Model '" + this.getPath() + "' reports possible container conflict: '" + model.getContainerName() + "' already exists in '" + container.getParent() + "'");
      }
      
      List<ServerContext> instances = container.getChildren(caller);
      
      for (ServerContext instance : instances)
      {
        if (instance.getType().equals(model.getObjectType()))
        {
          updateModelTarget(instance, model, caller);
        }
      }
      uninstallFromContext(this, getModel(), caller, true);
    }
    else
    {
      updateModelTarget(target, model, caller);
    }
  }
  
  private boolean isAllowedOperation(ServerContext target)
  {
    if (!(target instanceof BaseServerContext))
    {
      return true;
    }
    return !((BaseServerContext<?>) target).isVisibleChildrenAutoManagement() || Server.getConfig().isAllowAttachingModelsToContainers();
  }

  public void updateModelTarget(ServerContext target, Model model, CallerController caller) throws ContextException
  {
    if (isProtected())
    {
      caller = Server.getCallerController();
    }
    
    if (needInstallation(target.getPath()))
    {
      DataTable modelVariables = getVariable(V_MODEL_VARIABLES, caller);

      updateVariables(target, model, modelVariables, caller);
      checkAndUpdateLicensedItems(getVariablesForUpdate(target, model, modelVariables).keySet(), LicensingUnitsConstants.LU_VARIABLE);
      
      DataTable modelFunctions = getVariable(V_MODEL_FUNCTIONS, caller);
      DataTable modelRuleSets = getVariable(V_RULE_SETS, caller);
      
      updateFunctions(target, model, modelFunctions, modelRuleSets, caller, null);
      checkAndUpdateLicensedItems(getFunctionsForUpdate(target, model, modelFunctions, null).keySet(), LicensingUnitsConstants.LU_FUNCTION);

      DataTable modelEvents = getVariable(V_MODEL_EVENTS, caller);
      updateEvents(target, model, modelEvents);
      checkAndUpdateLicensedItems(getEventsForUpdate(target, model, modelEvents).keySet(), LicensingUnitsConstants.LU_EVENT);
      
      updateProcessor(target);
    }
  }
  
  private void uninstallFromContext(ServerContext target, Model model, CallerController caller, boolean onDestroy) throws ContextException
  {
    if (model.getType() == Model.TYPE_INSTANTIABLE)
    {
      Context container = target.getChild(model.getContainerName(), getContextManager().getCallerController());
      if (container != null)
      {
        if (onDestroy)
        {
          container.destroy(false);
        }
        else
        {
          List<ServerContext> instances = container.getChildren(caller);
          for (ServerContext instance : instances)
          {
            cleanupModelTarget(instance, model, caller, false);
          }
          target.removeChild(model.getContainerName());
          target.removeVisibleChild(container.getPath());
        }
      }
    }
    else
    {
      cleanupModelTarget(target, model, caller, onDestroy);
    }
  }
  
  protected void cleanupModelTarget(ServerContext target, Model model, CallerController caller, boolean onDestroy) throws ContextException
  {
    if (!preparedToUpdate)
    {
      removeVariables(target, model, caller, onDestroy);
      
      removeFunctions(target, model, caller);
      
      removeEvents(target, model, caller);
      
      ModelProcessor processor = getProcessor(target.getPath());
      
      if (processor != null)
      {
        processor.stop();
      }
    }
  }
  
  private Map<String, VariableDefinition> getVariablesForUpdate(Context target, Model model, DataTable modelVariables) throws ContextException
  {
    Map<String, VariableDefinition> variables = new ModelEntityMap<>(target);
    
    for (DataRecord rec : modelVariables)
    {
      VariableDefinition vd = createModelVariableDefinition(model, rec);
      
      if (!PROTECTED_VARIABLES_LIST.contains(vd.getName().toLowerCase()))
      {
        variables.put(vd.getName(), vd);
      }
      else
      {
        EventHelper.fireInfoEvent(target, EventLevel.ERROR, Lsres.get().getString("devIllegalDefinition") + vd.getName());
      }
    }
    
    return variables;
  }
  
  private void updateVariables(ServerContext target, Model model, DataTable modelVariables, CallerController caller) throws ContextException
  {
    Map<String, VariableDefinition> variablesForUpdate = getVariablesForUpdate(target, model, modelVariables);
    
    if (model.getType() == Model.TYPE_INSTANTIABLE)
    {
      VariableDefinition vd = target.getVariableDefinition(EditableChildContextConstants.V_CHILD_INFO);
      if (vd != null)
      {
        variablesForUpdate.put(vd.getName(), vd);
      }
    }

    String baseGroup = getBaseGroup(model);

    for (VariableDefinition vd1 : ((Context<? extends Context>) target).getVariableDefinitions(target.getContextManager().getCallerController(), baseGroup))
    {
      if (Util.equals(vd1.getName(), TemplatableServerContext.V_TEMPLATES))
        continue;

      if (Util.equals(vd1.getName(), ClusterCoordinatorContextConstants.V_RESOURCE_TYPE))
        continue;

      if (this.equals(vd1.getOwner()) && !variablesForUpdate.containsKey(vd1.getName()))
      {
        if (MODEL.equals(target.getType()) && MODEL_VARIABLES_LIST.contains(vd1.getName()))
        {
          continue;
        }
        target.removeVariableValue(vd1.getName());
        target.removeVariableDefinition(vd1.getName());
      }
    }

    List<VariableDefinition> added = target.updateVariableDefinitions(variablesForUpdate, getBaseGroup(model), false, true, this);
    
    if (model.getType() == Model.TYPE_INSTANTIABLE && target.getVariableDefinition(V_STATISTICS_PROPERTIES, caller) != null)
    {
      target.setVariable(V_STATISTICS_PROPERTIES, caller, getContextStatistics().getStatisticsProperties());
    }
    for (VariableDefinition vd : added)
    {
      target.createDefaultStatisticsChannels(vd);
    }
  }
  
  private Map<String, Pair<FunctionDefinition, Boolean>> getFunctionsForUpdate(Context target, Model model, DataTable modelFunctions, DataTable value) throws ContextException
  {
    Map<String, Pair<FunctionDefinition, Boolean>> functions = new ModelEntityMap<>(target);
    
    if (value == null)
      value = modelFunctions;
    
    for (DataRecord rec : value)
    {
      FunctionDefinition fd = createModelFunctionDefinition(model, rec);
      functions.put(fd.getName(), new Pair<>(fd, true));
    }
    
    return functions;
  }
  
  private void updateFunctions(Context target, Model model, DataTable modelFunctions, DataTable modelRuleSets, CallerController caller, DataTable value) throws ContextException
  {
    Map<String, Pair<FunctionDefinition, Boolean>> functions = getFunctionsForUpdate(target, model, modelFunctions, value);
    
    String baseGroup = getBaseGroup(model);
    List<FunctionDefinition> functionDefinitions = target.getFunctionDefinitions(getCallerController(), baseGroup);
    
    for (FunctionDefinition fd : functionDefinitions)
    {
      if (fd != null && fd.getGroup().equals(baseGroup))
      {
        checkFunctionAndActionDefinition(target, fd, functions);
      }
    }

    Pinpoint ruleSetsOrigin = newPinpointFor(this.getPath(), V_RULE_SETS);
    List<RuleSet> ruleSets = DataTableConversion.beansFromTable(modelRuleSets, RuleSet.class,
        RuleSet.FORMAT, true, ruleSetsOrigin);
    
    for (RuleSet ruleSet : ruleSets)
    {
      FunctionDefinition fd = createRuleSetFunctionDefinition(model, ruleSet);
      functions.put(fd.getName(), new Pair<>(fd, false));
    }

    target.updateFunctionDefinitions(functions, getBaseGroup(model), false, this);
  }
  
  private void checkFunctionAndActionDefinition(Context target, FunctionDefinition fd, Map<String, Pair<FunctionDefinition, Boolean>> functions)
  {
    if (!functions.containsKey(fd.getName()) || !functions.get(fd.getName()).getFirst().getDescription().equals(fd.getDescription()))
    {
      if (target.getActionDefinition(fd.getName(), getCallerController()) != null)
      {
        target.removeActionDefinition(fd.getName());
      }
      target.removeFunctionDefinition(fd.getName());
    }
  }
  
  private Map<String, EventDefinition> getEventsForUpdate(Context target, Model model, DataTable modelEvents) throws ContextException
  {
    Map<String, EventDefinition> events = new ModelEntityMap<>(target);
    
    for (DataRecord rec : modelEvents)
    {
      EventDefinition ed = createModelEventDefinition(model, rec);
      events.put(ed.getName(), ed);
    }
    
    return events;
  }
  
  private void updateEvents(Context target, Model model, DataTable modelEvents) throws ContextException
  {
    Map<String, EventDefinition> events = getEventsForUpdate(target, model, modelEvents);
    
    if (model.getType() == Model.TYPE_INSTANTIABLE)
    {
      EventDefinition ed = target.getEventDefinition(AbstractContext.E_INFO);
      events.put(ed.getName(), ed);
    }

    target.updateEventDefinitions(events, getBaseGroup(model), false, this);
  }
  
  private void updateProcessor(Context target) throws ContextException
  {
    ModelProcessor existingProcessor = getProcessor(target.getPath());
    if (existingProcessor != null)
    {
      existingProcessor.stop();
    }
    
    if (timer == null || executorService == null)
    {
      return;
    }
    
    ModelProcessor processor = new ModelProcessor(this, target, timer, executorService);
    
    processor.setEnabled(getModel().isEnabled());
    
    processor.start();
    processors.put(target.getPath(), processor);
  }
  
  private void removeVariables(ServerContext target, Model model, CallerController caller, boolean onDestroy) throws ContextException
  {
    target.updateVariableDefinitions(new LinkedHashMap<>(), getBaseGroup(model), false, onDestroy, this);
  }
  
  private void removeFunctions(ServerContext target, Model model, CallerController caller) throws ContextException
  {
    target.updateFunctionDefinitions(new LinkedHashMap<>(), getBaseGroup(model), false, this);
  }
  
  private void removeEvents(ServerContext target, Model model, CallerController caller) throws ContextException
  {
    target.updateEventDefinitions(new LinkedHashMap<>(), getBaseGroup(model), false, this);
  }
  
  public Model getModel()
  {
    return newModel != null ? newModel : getModelFromVariable();
  }
  
  protected Model getModelFromVariable()
  {
    return (Model) getVariableObject(EditableChildContextConstants.V_CHILD_INFO, getContextManager().getCallerController());
  }
  
  private VariableDefinition createModelVariableDefinition(Model model, DataRecord rec) throws ContextException
  {
    final String variable = rec.getString(FIELD_VD_NAME);
    
    VariableDefinition cached = variableDefinitionCache.get(variable);
    if (cached != null)
    {
      return cached;
    }

    boolean writable = rec.getBoolean(FIELD_VD_WRITABLE);
    
    TableFormat format = DataTableBuilding.createTableFormat(rec.getDataTable(FIELD_VD_FORMAT), new ClassicEncodingSettings(true), true);
    format = Server.getFormatCache().getCachedVersion(format);
    
    String group = ContextUtils.createGroup(getBaseGroup(model), rec.getString(FIELD_VD_GROUP));
    
    String description = rec.getString(FIELD_VD_DESCRIPTION) != null ? rec.getString(FIELD_VD_DESCRIPTION) : variable;
    
    VariableDefinition def = new VariableDefinition(variable, format, true, writable, description, group);
    
    def.setHelp(rec.getString(FIELD_VD_HELP));
    
    def.setReadPermissions(new Permissions(rec.getString(FIELD_VD_READ_PERMISSIONS)));
    def.setWritePermissions(new Permissions(rec.getString(FIELD_VD_WRITE_PERMISSIONS), true));
    
    def.setAllowUpdateEvents(true); // For safety, in fact should be set from VariableDefinition.setGroup()
    
    def.setPersistent(rec.getInt(FIELD_VD_STORAGE_MODE) == STORAGE_DATABASE);
    
    def.setHistoryRate(rec.getInt(FIELD_VD_HISTORY_RATE));
    
    def.setChangeEventsExpirationPeriod(rec.getLong(FIELD_VD_UPDATE_HISTORY_STORAGE_TIME));
    
    def.setRemoteCacheTime(rec.getLong(FIELD_VD_CACHE_TIME));
    
    def.setLocalCachingMode(rec.getInt(FIELD_VD_SERVER_CACHING_MODE));
    
    def.setAddPreviousValueToVariableUpdateEvent(rec.getBoolean(FIELD_VD_ADD_PREVIOUS_VALUE_TO_VARIABLE_UPDATE_EVENT));
    
    def.setGetter(new VariableGetter()
    {
      @Override
      public DataTable get(Context con, VariableDefinition def, CallerController caller, RequestController request) throws ContextException
      {
        AbstractContext ac = (AbstractContext) con;
        DataTable storedValue = ac.executeDefaultGetter(def.getName(), caller);
        
        if (def.getFormat() != null && !Util.equals(def.getFormat(), storedValue.getFormat()))
        {
          DataTable result = new SimpleDataTable(def.getFormat());
          
          DataTableReplication.copy(storedValue, result, true, true, true, true, true);
          
          return result;
        }
        return storedValue;
      }
    });
    
    def.setSetter(new VariableSetter()
    {
      @Override
      public boolean set(Context con, VariableDefinition def, CallerController caller, RequestController request, DataTable value) throws ContextException
      {
        AbstractContext ac = (AbstractContext) con;
        ac.executeDefaultSetter(def.getName(), caller, value);
        return true;
      }
    });
    
    def.setOwner(this);
    
    variableDefinitionCache.put(variable, def);

    return def;
  }
  
  public DataTable callFvariablesByMask(FunctionDefinition def, CallerController caller, RequestController request, DataTable parameters) throws ContextException
  {
    DataTable result = getContextManager().get(Contexts.CTX_UTILITIES, caller).callFunction(UtilitiesContextConstants.F_VARIABLES_BY_MASK, caller, parameters);
    if (getModel().getType() == Model.TYPE_ABSOLUTE)
      return result;
    
    DataTable modelVariables = getVariable(V_MODEL_VARIABLES, caller);
    Map<String, VariableDefinition> variableDefinitions = getVariablesForUpdate(this, getModel(), modelVariables);
    
    variableDefinitions.forEach((key, value) -> {
      DataRecord variable = result.addRecord();
      variable.setValue(DataTableBuilding.FIELD_SELECTION_VALUES_VALUE, key);
      variable.setValue(DataTableBuilding.FIELD_SELECTION_VALUES_DESCRIPTION, value.toDetailedString());
    });
    
    return result;
  }
  
  public DataTable callFexpressionEditorOptions(FunctionDefinition def, CallerController caller, RequestController request, DataTable parameters) throws ContextException
  {
    String var = parameters.rec().getString(1);
    Map<Reference, String> references = new LinkedHashMap<>();

    DataTable modelVariables = getVariable(V_MODEL_VARIABLES, caller);

    Map<String, VariableDefinition> variableDefinitions = getVariablesForUpdate(this, getModel(), modelVariables);
    Optional<Entry<String, VariableDefinition>> variableDefinition = variableDefinitions.entrySet().stream().filter(entry -> entry.getKey().equals(var)).findFirst();
    
    variableDefinition.ifPresent(variable -> {
      variable.getValue().getFormat().getFields().stream().filter(ff -> !ff.isHidden()).forEach(ff -> references.put(new Reference(ff.getName()), ff.toString()));
    });
    
    SimpleDataTable res = new SimpleDataTable(FOFT_EXPRESSION_BUILDER, true);
    res.rec().setValue(DefaultFunctions.EXPRESSION_EDITOR_OPTIONS.getName(), StringFieldFormat.encodeExpressionEditorOptions(this, new SimpleDataTable(), references));

    return res;
  }
  
  private FunctionDefinition createModelFunctionDefinition(Model model, DataRecord rec) throws ContextException
  {
    final String function = rec.getString(FIELD_FD_NAME);
    
    FunctionDefinition cached = functionDefinitionCache.get(function);
    if (cached != null)
    {
      return cached;
    }

    TableFormat inputFormat = DataTableBuilding.createTableFormat(rec.getDataTable(FIELD_FD_INPUTFORMAT), new ClassicEncodingSettings(true), true);
    inputFormat = Server.getFormatCache().getCachedVersion(inputFormat);
    
    TableFormat outputFormat = DataTableBuilding.createTableFormat(rec.getDataTable(FIELD_FD_OUTPUTFORMAT), new ClassicEncodingSettings(true), true);
    outputFormat = Server.getFormatCache().getCachedVersion(outputFormat);
    
    String group = ContextUtils.createGroup(getBaseGroup(model), rec.getString(FIELD_FD_GROUP));
    
    String description = rec.getString(FIELD_FD_DESCRIPTION) != null ? rec.getString(FIELD_FD_DESCRIPTION) : function;
    
    FunctionDefinition def = new FunctionDefinition(function, inputFormat, outputFormat, description, group);
    
    def.setHelp(rec.getString(FIELD_FD_HELP));
    
    def.setIconId(Icons.ES_FUNCTION);
    
    def.setPermissions(new Permissions(rec.getString(FIELD_FD_PERMISSIONS)));
    
    createModelFunctionImplementation(this, rec, def, compiledMethodCache);

    def.setConcurrent(rec.getBoolean(FIELD_FD_CONCURRENT));

    def.setOwner(this);

    functionDefinitionCache.put(function, def);

    return def;
  }

  public static void createModelFunctionImplementation(Context modelContext, DataRecord rec, FunctionDefinition def,
      Map<String, SoftReference<Object>> compiledMethodCache) throws ContextException
  {
    FunctionImplementation implementation;
    
    switch (rec.getInt(FIELD_FD_TYPE))
    {
      case FUNCTION_TYPE_JAVA:
        implementation = createJavaFunctionImplementation(def, rec.getString(FIELD_FD_IMPLEMENTATION), rec.getString(FIELD_FD_PLUGIN), modelContext, compiledMethodCache);
        break;
      
      case FUNCTION_TYPE_EXPRESSION:
        implementation = new ExpressionFunctionImplementation(rec.getString(FIELD_FD_EXPRESSION));
        break;
      
      case FUNCTION_TYPE_QUERY:
        implementation = new QueryFunctionImplementation(rec.getString(FIELD_FD_QUERY));
        break;
      
      default:
        throw new IllegalStateException("Unknown function type: " + rec.getInt(FIELD_FD_TYPE));
      
    }
    
    def.setImplementation(implementation);
  }
  
  private EventDefinition createModelEventDefinition(Model model, DataRecord rec) throws ContextException
  {
    final String event = rec.getString(FIELD_ED_NAME);
    
    EventDefinition cached = eventDefinitionCache.get(event);
    if (cached != null)
    {
      return cached;
    }

    TableFormat format = DataTableBuilding.createTableFormat(rec.getDataTable(FIELD_ED_FORMAT), new ClassicEncodingSettings(true), true);
    format = Server.getFormatCache().getCachedVersion(format);
    
    String group = ContextUtils.createGroup(getBaseGroup(model), rec.getString(FIELD_ED_GROUP));
    
    String description = rec.getString(FIELD_ED_DESCRIPTION) != null ? rec.getString(FIELD_ED_DESCRIPTION) : event;
    
    EventDefinition def = new EventDefinition(event, format, description, group);
    
    def.setLevel(rec.getInt(FIELD_ED_LEVEL));
    def.setHelp(rec.getString(FIELD_ED_HELP));
    
    def.setPermissions(new Permissions(rec.getString(FIELD_ED_PERMISSIONS)));
    def.setFirePermissions(new Permissions(rec.getString(FIELD_ED_FIRE_PERMISSIONS)));
    
    Long historyStorageTime = rec.getLong(FIELD_ED_HISTORY_STORAGE_TIME);
    def.setExpirationPeriod(historyStorageTime != null ? historyStorageTime : 0);
    
    def.setOwner(this);
    
    eventDefinitionCache.put(event, def);

    return def;
  }
  
  private static FunctionImplementation createJavaFunctionImplementation(FunctionDefinition def, String code, String pluginId,
      Context modelContext, Map<String, SoftReference<Object>> compiledMethodCache) throws ContextException
  {
    String implementationClassName = ContextUtils.contextPathToContextName(modelContext.getPath()) + "_" + def.getName();
    
    try
    {
      ClassLoader cl = pluginId != null ? Server.getPluginDirector().getPluginClassLoader(pluginId) : null;
      ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
      if (cl != null)
      {
        Thread.currentThread().setContextClassLoader(cl);
      }
      try
      {
        String cacheKey = implementationClassName + code + pluginId;
        
        SoftReference<Object> compiledMethodReference = compiledMethodCache.get(cacheKey);
        
        if (compiledMethodReference == null || compiledMethodReference.get() == null)
        {
          Object compiledMethod = ScriptCompiler.compile(code, implementationClassName, Server.getRuntimeConfig().getHomeDirectory(), cl);
          compiledMethodReference = new SoftReference<>(compiledMethod);
          compiledMethodCache.put(cacheKey, compiledMethodReference);
        }
        
        return (FunctionImplementation) compiledMethodReference.get();
      }
      finally
      {
        if (cl != null)
        {
          Thread.currentThread().setContextClassLoader(oldCl);
        }
      }
      
    }
    catch (Exception ex)
    {
      EventHelper.fireInfoEvent(modelContext, EventLevel.ERROR, MessageFormat.format(Lres.get().getString("errCompilingFuncImpl"), def.getName()) + ex.getMessage());
      return null;
    }
  }
  
  private FunctionDefinition createRuleSetFunctionDefinition(Model model, RuleSet ruleSet) throws ContextException
  {
    String group = getBaseGroup(model);
    
    FunctionDefinition def = new FunctionDefinition(ruleSet.getName(), null, null, ruleSet.getDescription(), group);
    
    def.setConcurrent(true);
    
    def.setIconId(Icons.ST_MODEL);
    
    def.setImplementation(new ProcessRuleSetFunction(ruleSet));
    
    return def;
  }
  
  private static String getBaseGroup(Model model)
  {
    String text = model.getDescription().length() > 0 ? model.getDescription() : model.getName();
    return model.getType() == Model.TYPE_ABSOLUTE ? ContextUtils.GROUP_CUSTOM
        : (model.getType() == Model.TYPE_RELATIVE ? ContextUtils.createGroup(ContextUtils.GROUP_CUSTOM, text) : ContextUtils.GROUP_DEFAULT);
  }
  
  @Override
  public CallerController getCallerController()
  {
    return getUserContext() != null ? getUserContext().getCallerController() : super.getCallerController();
  }

  ModelProcessor getProcessor(String contextPath)
  {
    return processors.get(contextPath);
  }
  
  /*
   * This method should be only executed inside Biding Processor execution service (not in rejected mode). Other way it may produce a deadlock.
   */
  public void fireBindingQueueOverflowEventIfNeeded()
  {
    if (!Util.equals(rejectedCountSinceLastBindingsOverflowEvent, rejectedCount))
    {
      final long currentTime = System.currentTimeMillis();
      if (timeSinceLastBindingsOverflowEvent == null || (currentTime - timeSinceLastBindingsOverflowEvent > BINDING_QUEUE_OVERFLOW_NOTIFICATION_PERIOD))
      {
        final FireEventRequestController request = new FireEventRequestController();
        request.setSuppressIfNotEnoughMemory(true);
        EventHelper.fireInfoEvent(ModelContext.this, EventLevel.FATAL, Lres.get().getString("bindingExecutionRejected"), request);
        
        timeSinceLastBindingsOverflowEvent = currentTime;
        rejectedCountSinceLastBindingsOverflowEvent = rejectedCount;
      }
    }
  }
  
  @Override
  protected DataTable callFunction(FunctionDefinition def, CallerController caller, RequestController request, DataTable parameters) throws ContextException
  {
    DataTable functions = getVariable(V_MODEL_FUNCTIONS, isProtected() ? Server.getCallerController() : caller);
    
    for (DataRecord rec : functions)
    {
      if (rec.getString(ModelContext.FIELD_FD_NAME).equals(def.getName()))
      {
        getLicenseContextStatisticsRegistry().checkAndIncrementCountableUnitToSet(getPath(), LicensingUnitsConstants.LU_FUNCTION_CALLS_PER_DAY);
        break;
      }
    }
    
    return super.callFunction(def, caller, request, parameters);
  }
  
  @Override
  public Collection<Context> getDependentContexts(CallerController callerController) throws ContextException
  {
    LinkedList<Context> contexts = new LinkedList<>();
    getVariable(V_VALIDITY, callerController);
    for (DataRecord record : getVariable(V_VALIDITY, callerController))
    {
      contexts.add(getContextManager().get(record.getString(0), callerController));
    }
    return contexts;
  }
  
  @Override
  public Map<Context, List<String>> getDependentVariables(CallerController callerController) throws ContextException
  {
    LinkedList<Context> contexts = (LinkedList<Context>) getDependentContexts(callerController);
    DataTable variables = getVariable(V_MODEL_VARIABLES, callerController);
    Map<Context, List<String>> dependentVariables = new HashMap<>();
    List<String> variableList = new LinkedList<>();
    for (DataRecord record : variables)
    {
      String variable = record.getString(FIELD_VD_NAME);
      variableList.add(variable);
    }
    for (Context context : contexts)
    {
      dependentVariables.put(context, variableList);
    }
    return dependentVariables;
  }
  
  private static final class ExpressionFunctionImplementation implements FunctionImplementation
  {
    private final Expression expression;
    
    private ExpressionFunctionImplementation(String expression)
    {
      this.expression = new Expression(expression);
    }
    
    @Override
    public DataTable execute(Context con, FunctionDefinition def, CallerController caller, RequestController request, DataTable parameters) throws ContextException
    {
      try
      {
        Evaluator ev = new Evaluator(con.getContextManager(), con, parameters, caller);
        return ev.evaluateToDataTable(expression);
      }
      catch (Exception ex)
      {
        throw new ContextException(ex.getMessage(), ex);
      }
    }
  }
  
  private static final class QueryFunctionImplementation implements FunctionImplementation
  {
    private final String query;
    
    private QueryFunctionImplementation(String query)
    {
      this.query = query;
    }
    
    @Override
    public DataTable execute(Context con, FunctionDefinition def, CallerController caller, RequestController request, DataTable parameters) throws ContextException
    {
      try
      {
        List queryParameters = new LinkedList();
        
        for (DataRecord rec : parameters)
        {
          for (FieldFormat ff : parameters.getFormat())
          {
            queryParameters.add(rec.getValue(ff.getName()));
          }
        }

        return SQLFacade.executeSQLSelect(query, null, def.getOutputFormat(), con.getContextManager(), caller, queryParameters.toArray());
      }
      catch (NoClassDefFoundError e)
      {
        throw new ContextException(Lres.get().getString("noPluginErr"));
      }
      catch (Exception ex)
      {
        throw new ContextException(ex.getMessage(), ex);
      }
    }
  }
  
  private class ModelBindingRejectedExecutionHandler implements RejectedExecutionHandler
  {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor)
    {
      rejectedCount++;
      
      final long currentTime = System.currentTimeMillis();
      if (timeSinceLastBindingsOverflowLogMessage == null || (currentTime - timeSinceLastBindingsOverflowLogMessage > BINDING_QUEUE_OVERFLOW_NOTIFICATION_PERIOD))
      {
        Log.MODELS.error("Binding execution rejected for model '" + ModelContext.this.toDetailedString() + "'. Check model configuration of fine-tune model concurrency settings.");
        timeSinceLastBindingsOverflowLogMessage = currentTime;
      }
      else
      {
        if (Log.MODELS.isDebugEnabled())
        {
          Log.MODELS.debug("Binding execution rejected for model '" + ModelContext.this.toDetailedString() + "'");
        }
      }
    }
  }
  
  private static class ModelEntityMap<K, V> extends HashMap<K, V>
  {
    Context target;
    
    public ModelEntityMap(Context target)
    {
      super();
      this.target = target;
    }
    
    @Override
    public V put(K key, V value)
    {
      final V presentValue = get(key);
      final boolean presentValueDiffersFromNewOne = presentValue != null && !presentValue.equals(value);
      if (presentValueDiffersFromNewOne)
      {
        EventHelper.fireInfoEvent(target, EventLevel.ERROR, Lsres.get().getString("devIllegalDefinition") + key);
        return null;
      }
      
      return super.put(key, value);
    }
  }
  
  private final class ContextDestroyedListener extends DefaultContextEventListener<CallerController>
  {
    ContextDestroyedListener()
    {
      super(ModelContext.this.getContextManager().getCallerController());
    }
    
    @Override
    public void handle(Event event) throws EventHandlingException
    {
      final String removedContextName = event.getData().rec().getString(RootContextConstants.EF_CONTEXT_DESTROYED_CONTEXT);
      
      final Set<String> removeSet = processors.entrySet().stream()
          .filter(e -> removedContextName.equals(ContextUtils.getContextName(e.getKey())) && e.getValue().getTargetContext().getParent() == null).map(Entry::getKey).collect(Collectors.toSet());
      
      for (String contextPath : removeSet)
      {
        final ModelProcessor removedProcessor = processors.remove(contextPath);
        if (removedProcessor != null)
          removedProcessor.stop();
      }
    }
  }
  
  private final class ContextRelocatedListener extends DefaultContextEventListener<CallerController>
  {
    ContextRelocatedListener()
    {
      super(ModelContext.this.getContextManager().getCallerController());
    }
    
    @Override
    public void handle(Event event) throws EventHandlingException
    {
      final String oldContextPath = event.getData().rec().getString(RootContextConstants.EF_CONTEXT_RELOCATED_OLD_PATH);
      
      final ModelProcessor removedProcessor = processors.remove(oldContextPath);
      if (removedProcessor != null)
        removedProcessor.stop();
    }
  }

  private interface ModelTargetUpdater
  {
    void updateInstance(ServerContext instance) throws ContextException;

    void updateTarget(Context target) throws ContextException;
  }
}
