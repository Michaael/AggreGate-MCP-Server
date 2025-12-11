package com.tibbo.linkserver.plugin.context.models;

import static com.google.common.collect.Lists.newArrayList;
import static com.tibbo.aggregate.common.server.EditableChildContextConstants.V_CHILD_INFO;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Assert;
import org.junit.Ignore;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.tibbo.aggregate.common.Log;
import com.tibbo.aggregate.common.binding.Bindings;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.VariableDefinition;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.DataTableBuilding;
import com.tibbo.aggregate.common.datatable.DataTableConversion;
import com.tibbo.aggregate.common.datatable.SimpleDataTable;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.common.datatable.encoding.ClassicEncodingSettings;
import com.tibbo.aggregate.common.server.ModelContextConstants;
import com.tibbo.linkserver.config.DefaultServerConfig;
import com.tibbo.linkserver.context.BaseServerContext;
import com.tibbo.linkserver.context.InstallableContext;
import com.tibbo.linkserver.util.ValidityListenerInfo;

@Ignore("#AGG-14544 #SANDBOX")
public class ConcurrentRelativeModelsSandbox extends BaseRelativeModelContextTest
{
  public static final String SUB_RELATIVE_BASE_MODEL = "subRelativeBaseModel";
  
  public static final String SUB_RELATIVE_MODEL = "subRelativeModel";
  
  public static final int STRESS_COUNT = 500;
  
  public static final String CHANGE_TYPE_MODEL = "changeTypeModel";
  
  public static final String TYPE_MODEL = "typeModel";
  
  public static final String TYPE_NUMBER = "typeNumber";
  
  public static final String VAR = "VAR";
  
  public static final int SUB_RELATIVE_MODELS_COUNT = 317;
  public static final int PARTITION_SIZE = 150;
  
  public void testMixerConcurrentRelativeModels() throws InterruptedException, ContextException
  {
    testConcurrentRelativeModels();
    
    for (int i = 0; i < 10; i++)
    {
      runWithDifferentConditions();
    }
  }
  
  private void runWithDifferentConditions() throws ContextException, InterruptedException
  {
    
    for (int i = 0; i < SUB_RELATIVE_MODELS_COUNT; i++)
    {
      ModelContext modelContext = (ModelContext) modelsContext.getChild(SUB_RELATIVE_MODEL + i, getCallerController());
      Model model = modelContext.getModel();
      model.setValidityExpression("{.:typeModel$typeNumber}==" + i);
      DataTable dataTable = DataTableConversion.beanToTable(model, Model.FORMAT, true);
      modelContext.setVariable(V_CHILD_INFO, getCallerController(), dataTable);
    }
    
    Multiset<String> expected = HashMultiset.create();
    for (int i = 0; i < STRESS_COUNT; i++)
    {
      int number = ThreadLocalRandom.current().nextInt(1, 317);
      expected.add("VAR" + number);
      Context context = userContext.getChild(MODEL_ABSOLUTE + i, getCallerController());
      context.setVariable(TYPE_MODEL, getCallerController(), number);
    }
    
    Thread.sleep(10_000);
    
    Multiset<String> actual = HashMultiset.create();
    for (int i = 0; i < STRESS_COUNT; i++)
    {
      Context context = userContext.getChild(MODEL_ABSOLUTE + i, getCallerController());
      List<VariableDefinition> variableDefinitions = context.getVariableDefinitions(getCallerController());
      String name = variableDefinitions
          .stream()
          .filter(vd -> vd.getName().startsWith(VAR)).findAny().get().getName();
      actual.add(name);
    }
    for (String s : actual.elementSet())
    {
      Assert.assertEquals(expected.count(s), actual.count(s));
    }
  }
  
  public void testConcurrentRelativeModels() throws InterruptedException, ContextException
  {
    // Prepare Models and contexts.
    
    List<Callable<Void>> tasks = newArrayList();
    tasks.addAll(createContexts());
    tasks.addAll(createModels());
    
    Log.CORE.info("Start creating...");
    
    List<List<Callable<Void>>> partition = Lists.partition(tasks, PARTITION_SIZE);
    for (List<Callable<Void>> callables : partition)
    {
      getContextManager().getExecutorService().invokeAll(callables);
      
    }
    Log.CORE.info("Models and Contexts were created");
    
    // Awaiting some time for events to be processed. Therefore it can require different timeouts for different machines.
    // FIXME Probably we have to handle events instead of waiting.
    Thread.sleep(10_000);
    while (getContextManager().getEventQueueLength() > 0)
    {
      Thread.sleep(1_000);
    }
    
    // Check that all variables were attached to the contexts
    assertTypeModelVariableAvailability();
    
    assertRelativeVarsAvailability();
  }
  
  private void assertRelativeVarsAvailability()
  {
    HashMap<String, Integer> result = new HashMap<>();
    for (int i = 0; i < STRESS_COUNT; i++)
    {
      Context context = userContext.getChild(MODEL_ABSOLUTE + i, getCallerController());
      int failedIterations = 0;
      for (int j = 0; j < SUB_RELATIVE_MODELS_COUNT; j++)
      {
        VariableDefinition variableDefinition = context.getVariableDefinition(VAR + j, getCallerController());
        if (variableDefinition == null)
        {
          failedIterations++;
        }
      }
      result.put(MODEL_RELATIVE + i, failedIterations);
    }
    
    StringBuilder error = new StringBuilder();
    for (String s : result.keySet())
    {
      if (result.get(s) > 0)
      {
        error.append("Context: ").append(s).append("; error count: ").append(result.get(s)).append(System.lineSeparator());
      }
    }
    if (!error.toString().isEmpty())
    {
      Assert.fail(error.toString());
    }
  }
  
  private void assertTypeModelVariableAvailability() throws ContextException
  {
    for (int i = 0; i < STRESS_COUNT; i++)
    {
      Context context = userContext.getChild(MODEL_ABSOLUTE + i, getCallerController());
      DataTable var = context.getVariable(TYPE_MODEL, getCallerController());
      Assert.assertNotNull("Failed on the: " + context.getPath() + " iteration ", var);
      Assert.assertEquals(42, var.rec().getInt(TYPE_NUMBER).intValue());
    }
  }
  
  private List<Callable<Void>> createContexts() throws InterruptedException
  {
    List<Callable<Void>> tasks;
    
    tasks = newArrayList();
    
    for (int i = 0; i < STRESS_COUNT; i++)
    {
      int finalI = i;
      tasks.add(() -> {
        userContext.addChild(new BaseServerContext(MODEL_ABSOLUTE + finalI));
        return null;
      });
    }
    return tasks;
  }
  
  /**
   * Create models one-by-one as it cannot be created in ContextOperations pool as models uses this pool by themselves.
   * 
   * @return
   * @throws ContextException
   */
  private List<Callable<Void>> createModels() throws ContextException
  {
    List<Callable<Void>> tasks = newArrayList();
    
    ModelContext model = createModel(SUB_RELATIVE_BASE_MODEL, SUB_RELATIVE_BASE_MODEL,
        "startsWith({.:#name}, '" + MODEL_ABSOLUTE + "')", prepareModelVariables(TYPE_MODEL));
    model.setVariable(ModelContextConstants.V_MODEL_EVENTS, getCallerController(), prepareChangeTypeEvent());
    model.setVariable(ModelContextConstants.V_BINDINGS, getCallerController(), prepareModelBindings());
    DataTable variable = model.getVariable(V_CHILD_INFO, getCallerController()).cloneIfImmutable();
    variable.rec().setValue(Model.FIELD_GENERATE_ATTACHED_EVENTS, true);
    model.setVariable(V_CHILD_INFO, getCallerController(), variable);
    
    for (int i = 0; i < SUB_RELATIVE_MODELS_COUNT; i++)
    {
      int finalI = i;
      Callable<Void> task = () -> {
        createModel(SUB_RELATIVE_MODEL + finalI, SUB_RELATIVE_MODEL + finalI, "{.:typeModel$typeNumber}==42", prepareModelVariables(VAR + finalI));
        return null;
      };
      tasks.add(task);
    }
    return tasks;
  }
  
  @Override
  protected Model prepareModel()
  {
    return prepareModel(MODEL_ABSOLUTE, MODEL_ABSOLUTE);
  }
  
  private Model prepareModel(String name, String description)
  {
    Model modelAbs = new Model(name, description);
    modelAbs.setType(Model.TYPE_ABSOLUTE);
    return modelAbs;
  }
  
  protected void setUpRelativeModel() throws ContextException
  {
  }
  
  private DataTable prepareModelVariables(String name)
  {
    DataRecord modelVariable = new DataRecord(ModelContext.VFT_MODEL_VARIABLES);
    modelVariable.setValue(ModelContext.FIELD_VD_NAME, name);
    TableFormat tableFormat = new TableFormat(1, 1);
    tableFormat.addField("<" + TYPE_NUMBER + "><I><A=42>");
    modelVariable.setValue(ModelContext.FIELD_VD_FORMAT, DataTableBuilding.formatToTable(tableFormat, new ClassicEncodingSettings(true), false));
    return modelVariable.wrap();
  }
  
  private DataTable prepareChangeTypeEvent()
  {
    DataTable dataTable = new SimpleDataTable(ModelContext.VFT_MODEL_EVENTS);
    DataRecord dataRecord = dataTable.addRecord();
    dataRecord.setValue(ModelContext.FIELD_ED_NAME, CHANGE_TYPE_MODEL);
    return dataTable;
  }
  
  private DataTable prepareModelBindings()
  {
    DataRecord bindings = new DataRecord(ModelContext.VFT_BINDINGS);
    bindings.setValue(Bindings.FIELD_EXPRESSION, "fireEvent(dc(), \"changeTypeModel\", 1)");
    bindings.setValue(Bindings.FIELD_ACTIVATOR, ".:typeModel");
    bindings.setValue(Bindings.FIELD_ONEVENT, true);
    return bindings.wrap();
  }
  
  @Override
  protected void updateValidityListeners(Model model)
  {
    List<ValidityListenerInfo> objects = newArrayList();
    ValidityListenerInfo validityListenerInfo = new ValidityListenerInfo();
    validityListenerInfo.setMask(userContext.getPath() + ".*");
    validityListenerInfo.setEvent(CHANGE_TYPE_MODEL);
    
    objects.add(validityListenerInfo);
    
    validityListenerInfo = new ValidityListenerInfo();
    validityListenerInfo.setMask(modelsContext.getPath() + ".*");
    validityListenerInfo.setEvent(InstallableContext.E_ATTACHED);
    validityListenerInfo.setExpression("{path}");
    
    objects.add(validityListenerInfo);
    
    model.setValidityListeners(objects);
  }
  
  @Override
  protected DataTable prepareRelativeModelVariables()
  {
    return new SimpleDataTable();
  }
  
  @Override
  protected void patchConfig(DefaultServerConfig config)
  {
    config.setExtMaxEventQueueLength(1_000_000);
    config.setExtCoreContextOperationThreads(200);
  }
}
