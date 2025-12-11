package com.tibbo.linkserver.plugin.context.models;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;

import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.server.ModelContextConstants;
import com.tibbo.aggregate.common.server.ServerContext;
import com.tibbo.linkserver.context.EditableChildrenContext;
import com.tibbo.linkserver.security.LicensedContextStatisticsRegistry;

@Ignore("#AGG-14544 #EXCEPTION")
public class TestModelInstancesLicensing extends BaseModelLicensingTest
{
  @Override
  protected void setUp() throws Exception
  {
    super.setUp();
    
    DataTable modelVariables = prepareModelVariables(1);
    
    modelContext.setVariable(ModelContextConstants.V_MODEL_VARIABLES, getCallerController(), modelVariables);
  }
  
  protected Model prepareModel()
  {
    Model model = new Model("model", "Model");
    model.setType(Model.TYPE_INSTANTIABLE);
    model.setContainerType(TestInstantiableModelContext.VALVES);
    model.setContainerName(TestInstantiableModelContext.VALVES);
    model.setObjectType("valve");
    model.setValidityExpression("{.:#type} == 'gasline'");
    
    return model;
  }
  
  public void testIgnoreModelInstancesCount() throws Exception    // ignored due to failing on Jenkins server (only)
  {
    final String namePrefix = "valve";
    final String descriptionPrefix = "Valve ";
    for (int i = 0; i < MAX_INSTANCES; i++)
    {
      createModelInstance(namePrefix + i, descriptionPrefix + i);
    }
    
    int registeredInstancesCount = LicensedContextStatisticsRegistry.getStatisticsRegistry(MODELS_INSTANCE_ID).getRegisteredContextsCount();
    Set<String> registeredInstances = LicensedContextStatisticsRegistry.getStatisticsRegistry(MODELS_INSTANCE_ID).getRegisteredContexts();
    
    Assert.assertThat("There are more instances than should be: " + registeredInstances.toString(),
        registeredInstancesCount, is(equalTo(MAX_INSTANCES)));
  }
  
  private void createModelInstance(String name, String description) throws Exception
  {
    ServerContext instantiableContainer = getContextManager().get(TestInstantiableModelContext.VALVES, getCallerController());
    
    instantiableContainer.callFunction(EditableChildrenContext.F_CREATE, getCallerController(), name, description);
    ServerContext modelInstanceContext = getContextManager().get(TestInstantiableModelContext.VALVES + "." + name, getCallerController());
    
    assertThat(modelInstanceContext.getDescription(), is(description));
  }
  
  @Override
  protected void tearDown() throws Exception
  {
    deleteInstances();
    
    super.tearDown();
  }
  
  private void deleteInstances() throws ContextException
  {
    ServerContext instantiableContainer = getContextManager().get(TestInstantiableModelContext.VALVES, getCallerController());
    if (instantiableContainer != null)
    {
      List<ServerContext> instances = instantiableContainer.getChildren(getCallerController());
      for (ServerContext instance : instances)
      {
        instantiableContainer.callFunction(EditableChildrenContext.F_DELETE, getCallerController(), instance.getName());
      }
    }
  }
}
