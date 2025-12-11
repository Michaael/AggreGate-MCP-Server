package com.tibbo.linkserver.plugin.context.models;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;

import com.tibbo.aggregate.common.AggreGateException;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextUtils;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.DataTableConversion;
import com.tibbo.aggregate.common.datatable.SimpleDataTable;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.common.expression.Expression;
import com.tibbo.aggregate.common.expression.Reference;
import com.tibbo.aggregate.common.server.ModelContextConstants;
import com.tibbo.aggregate.common.server.ServerContext;
import com.tibbo.aggregate.common.util.TimeHelper;
import com.tibbo.linkserver.context.BaseServerContext;
import com.tibbo.linkserver.context.EditableChildContext;
import com.tibbo.linkserver.context.EditableChildrenContext;
import com.tibbo.linkserver.device.DefaultDeviceContext;
import com.tibbo.linkserver.statistics.ChannelProperties;
import com.tibbo.linkserver.statistics.ContextStatistics;

public class TestInstantiableModelContext extends BaseModelContextTest
{
  private static final String VARIABLE = "variable";
  public static final String VALVES = "valves";
  public static final String VALVE = "valve";
  private static final String TEST_COPY_PREFIX = "_test_copy";
  
  @Override
  protected void setUp() throws Exception
  {
    super.setUp();
    
    DataTable modelVariables = prepareModelVariables();
    
    modelContext.setVariable(ModelContextConstants.V_MODEL_VARIABLES, getCallerController(), modelVariables);
    
    DataTable statProperties = statisticsChannelProperties();
    
    // setup a statistics channel
    modelContext.setVariable(DefaultDeviceContext.V_STATISTICS_PROPERTIES, getCallerController(), statProperties);
  }
  
  private void createValveIntance(String name, String description) throws Exception
  {
    ServerContext instantiableContainer = getContextManager().get(TestInstantiableModelContext.VALVES, getCallerController());
    
    instantiableContainer.callFunction(EditableChildrenContext.F_CREATE, getCallerController(), name, description);
  }
  
  public void testWhenInstantiableModelIsInstalledThenInstanceContainerIsCreated() throws Exception
  {
    createValveIntance(VALVE, "Valve 1");
    
    assertThat(getContextManager().get(VALVES, getCallerController()), is(org.hamcrest.core.IsNull.notNullValue()));
    
    deleteInstances(TestInstantiableModelContext.VALVES);
  }
  
  public void testModelInstanceIsCreated() throws Exception
  {
    final String description = "Valve 1";
    
    createValveIntance(VALVE, description);
    
    ServerContext modelInstanceContext = getContextManager().get(VALVES + "." + VALVE, getCallerController());
    
    assertThat(modelInstanceContext.getDescription(), is(description));
    
    deleteInstances(TestInstantiableModelContext.VALVES);
  }
  
  public void testWhenModelInstanceIsCreatedThenStatisticsChannelsAdded() throws Exception
  {
    createValveIntance(VALVE, "Valve 1");
    
    BaseServerContext modelInstanceContext = (BaseServerContext) getContextManager().get(VALVES + "." + VALVE, getCallerController());
    
    assertThat(modelInstanceContext.getContextStatistics().statisticsFor(VARIABLE), is(Matchers.notNullValue()));
    
    deleteInstances(TestInstantiableModelContext.VALVES);
  }
  
  public void testRenameContainerModel() throws Exception
  {
    Model model = getTestModel("renameModel", "users.testUser");
    
    modelContext = createModelContext(model);
    
    Set<Context> targetContexts = modelContext.getTargetContexts(model);
    Context instModelContext = null;
    for (Context context : targetContexts)
    {
      String containerPrefixPath = context.getPath().isEmpty() ? "" : context.getPath() + ".";
      String containerName = containerPrefixPath + model.getContainerName();
      instModelContext = getContextManager().get(containerName, getCallerController());
    }
    assertNotNull(instModelContext);
    
    fillModelContainer(instModelContext);
    
    assertEquals(2, instModelContext.getChildren(getCallerController()).size());
    
    for (Context context : targetContexts)
    {
      assertNotNull(getContextManager().get(context.getPath() + "." + model.getContainerName(), getCallerController()));
    }
    
    model.setContainerName(model.getContainerName() + TEST_COPY_PREFIX);
    model.setContainerTypeDescription(model.getContainerTypeDescription() + TEST_COPY_PREFIX);
    DataTable childInfo = DataTableConversion.beanToTable(model, Model.FORMAT, true);
    
    assertNotNull(childInfo);
    
    modelContext.setVariable(EditableChildContext.V_CHILD_INFO, getCallerController(), childInfo);
    
    targetContexts = modelContext.getTargetContexts(model);
    for (Context context : targetContexts)
    {
      String containerPathPrefix = context.getPath().isEmpty() ? "" : context.getPath() + ".";
      instModelContext = getContextManager().get(containerPathPrefix + model.getContainerName(), getCallerController());
      assertNotNull(instModelContext);
      assertEquals(2, instModelContext.getChildren(getCallerController()).size());
      deleteInstances(instModelContext);
    }
  }
  
  private void fillModelContainer(Context instModelContext) throws ContextException
  {
    TableFormat createDef = instModelContext.getFunctionDefinition("create", getCallerController()).getInputFormat();
    
    DataRecord childCreate = new DataRecord(createDef, "1", "1");
    instModelContext.callFunction("create", getCallerController(), childCreate.wrap());
    
    childCreate = new DataRecord(createDef, "2", "2");
    
    instModelContext.callFunction("create", getCallerController(), childCreate.wrap());
  }
  
  private Model getTestModel(String modelName, String parentContext)
  {
    Model model = new Model(modelName, modelName);
    model.setType(Model.TYPE_INSTANTIABLE);
    model.setContainerType(modelName + "test");
    model.setContainerName(modelName + "test");
    model.setObjectType(modelName + "test");
    model.setValidityExpression("{.:} == \"" + parentContext + "\"");
    
    return model;
  }
  
  public void testMakeCopyModel() throws Exception
  {
    Model model = getTestModel("makeCopy", "users.testUser");
    modelContext = createModelContext(model);
    
    Set<Context> contexts = modelContext.getTargetContexts(model);
    Context instModelContext = null;
    for (Context context : contexts)
    {
      String containerPrefixPath = context.getPath().isEmpty() ? "" : context.getPath() + ".";
      String containerName = containerPrefixPath + model.getContainerName();
      instModelContext = getContextManager().get(containerName, getCallerController());
    }
    assertNotNull(instModelContext);
    
    fillModelContainer(instModelContext);
    
    DataRecord inputFormat = new DataRecord(EditableChildrenContext.FIFT_MAKE_COPY);
    String newName = model.getName() + TEST_COPY_PREFIX;
    String newDescription = model.getDescription() + TEST_COPY_PREFIX;
    String newContainerType = model.getContainerType() + TEST_COPY_PREFIX;
    String newContainerTypeDescription = model.getContainerTypeDescription() + TEST_COPY_PREFIX;
    String newContainerName = model.getContainerName() + TEST_COPY_PREFIX;
    inputFormat.addString(modelContext.getPath()).addString(newName).addString(newDescription).addString(newContainerType).addString(newContainerTypeDescription).addString(newContainerName)
        .addBoolean(true);
    
    modelsContext.callFunction(EditableChildrenContext.F_MAKE_COPY, getCallerController(), inputFormat.wrap());
    
    ModelContext newModel = (ModelContext) modelsContext.getChild(newName, getCallerController());
    assertNotNull(newModel);
    assertNotNull(getContextManager().get(modelContext.getPath(), getCallerController()));
    
    Model cloneModel = newModel.getModelFromVariable();
    contexts = newModel.getTargetContexts(cloneModel);
    for (Context context : contexts)
    {
      String containerPathPrefix = context.getPath().isEmpty() ? "" : context.getPath() + ".";
      Context cloneContainer = getContextManager().get(containerPathPrefix + cloneModel.getContainerName(), getCallerController());
      assertNotNull(cloneContainer);
      assertEquals(0, cloneContainer.getChildren(getCallerController()).size());
    }
    
    contexts = newModel.getTargetContexts(model);
    for (Context context : contexts)
    {
      String containerPathPrefix = context.getPath().isEmpty() ? "" : context.getPath() + ".";
      Context originalContainer = getContextManager().get(containerPathPrefix + model.getContainerName(), getCallerController());
      assertNotNull(originalContainer);
      assertEquals(2, originalContainer.getChildren(getCallerController()).size());
      deleteInstances(originalContainer);
    }
  }
  
  public void testRenameValidityExpression() throws AggreGateException
  {
    Model model = getTestModel("validityExpressionModel", "users.testUser");
    modelContext = createModelContext(model);
    
    Set<Context> targetContexts = modelContext.getTargetContexts(model);
    Context instModelContext = null;
    for (Context context : targetContexts)
    {
      String containerPrefixPath = context.getPath().isEmpty() ? "" : context.getPath() + ".";
      String containerName = containerPrefixPath + model.getContainerName();
      instModelContext = getContextManager().get(containerName, getCallerController());
    }
    assertNotNull(instModelContext);
    
    fillModelContainer(instModelContext);
    
    model = getTestModel("validityRenamed", "");
    DataTable childInfo = DataTableConversion.beanToTable(model, Model.FORMAT, true);
    
    modelContext.setVariable(EditableChildContext.V_CHILD_INFO, getCallerController(), childInfo);
    
    Context newContainer = modelContext.getRoot().getChild(model.getContainerName(), getCallerController());
    assertNotNull(newContainer);
    
    assertEquals(0, newContainer.getChildren(getCallerController()).size());
    
    deleteInstances(instModelContext);
  }
  
  protected DataTable statisticsChannelProperties() throws ContextException
  {
    final ChannelProperties channelProperties = new ChannelProperties();
    channelProperties.setMinutelyStats(TimeHelper.DAY_IN_MS);
    channelProperties.setStorage(ChannelProperties.MAPPED_STORAGE);
    channelProperties.setExpression(new Expression(new Reference(null, ContextUtils.ENTITY_VARIABLE, "{value}")).getText());
    
    DataTable dtProperties = new SimpleDataTable(ContextStatistics.VFT_CHANNEL_STATISTICS_PROPERTIES);
    dtProperties.addRecord(VARIABLE, VARIABLE, true, channelProperties.toDataTable());
    return dtProperties;
  }
  
  protected DataTable prepareModelVariables()
  {
    DataRecord modelVariable = new DataRecord(ModelContext.VFT_MODEL_VARIABLES);
    modelVariable.setValue(ModelContext.FIELD_VD_NAME, VARIABLE);
    return modelVariable.wrap();
  }
  
  @Override
  protected Model prepareModel()
  {
    Model model = new Model("model", "Model");
    model.setType(Model.TYPE_INSTANTIABLE);
    model.setContainerType(VALVES);
    model.setContainerName(VALVES);
    model.setObjectType("valve");
    model.setValidityExpression("{.:#type} == 'gasline'");
    
    return model;
  }
  
  private void deleteInstances(String instantiableContainerName) throws ContextException
  {
    Context instantiableContainer = getContextManager().get(instantiableContainerName, getCallerController());
    deleteInstances(instantiableContainer);
  }
  
  private void deleteInstances(Context instantiableContainer) throws ContextException
  {
    if (instantiableContainer != null)
    {
      List<Context> instances = instantiableContainer.getChildren(getCallerController());
      for (Context instance : instances)
      {
        instantiableContainer.callFunction(EditableChildrenContext.F_DELETE, getCallerController(), instance.getName());
      }
    }
  }
}