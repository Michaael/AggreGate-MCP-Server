package com.tibbo.linkserver.plugin.context.models;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.tibbo.aggregate.common.context.ContextUtils;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.SimpleDataTable;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.linkserver.Server;
import com.tibbo.linkserver.context.EditableChildrenContext;
import com.tibbo.linkserver.security.DefaultPermission;
import com.tibbo.linkserver.security.License;
import com.tibbo.linkserver.security.LicensedContextSet;
import com.tibbo.linkserver.security.LicensedContextSets;
import com.tibbo.linkserver.security.LicensingUnitsConstants;
import com.tibbo.linkserver.tests.ServerFixture;
import org.junit.Ignore;

@Ignore("#AGG-14544 #EMPTY")
public class BaseModelLicensingTest extends BaseModelContextTest
{
  protected static final String MODELS_INSTANCE_ID = "com.tibbo.linkserver.plugin.context.models.InstantiableModelContext";
  
  protected static final int MAX_MODELS = 10;
  protected static final int MAX_VARIABLES = 100;
  protected static final int MAX_FUNCTIONS = 40;
  protected static final int MAX_EVENTS = 30;
  protected static final int MAX_FUNCTION_CALLS = 30;
  protected static final int MAX_INSTANCES = 5;
  
  protected static final String EVT_PREFIX = "evt";
  protected static final String FUNC_PREFIX = "func";
  protected static final String VAR_PREFIX = "var";
  
  protected ModelContext modelContext2;
  
  @Override
  protected void setUp() throws Exception
  {
    fixture = new ServerFixture()
    {
      @Override
      protected License initLicense()
      {
        License license = new License();
        
        LicensedContextSets pluginItems = new LicensedContextSets();
        
        Map<String, String> persistent = new HashMap<String, String>();
        persistent.put(SQL_PERSISTENCE_PLUGIN_ID, SQL_PERSISTENCE_PLUGIN_ID);
        pluginItems.addSet(new LicensedContextSet("Persistent", "persistent", persistent));
        
        LinkedHashMap<String, String> modelProperties = new LinkedHashMap<>();
        modelProperties.put(LicensingUnitsConstants.LU_EVENT, String.valueOf(MAX_EVENTS));
        modelProperties.put(LicensingUnitsConstants.LU_FUNCTION, String.valueOf(MAX_FUNCTIONS));
        modelProperties.put(LicensingUnitsConstants.LU_VARIABLE, String.valueOf(MAX_VARIABLES));
        modelProperties.put(LicensingUnitsConstants.LU_CONTEXT, String.valueOf(MAX_MODELS));
        modelProperties.put(LicensingUnitsConstants.LU_FUNCTION_CALLS_PER_DAY, String.valueOf(MAX_FUNCTION_CALLS));
        
        Map<String, String> model = new HashMap<String, String>();
        model.put(MODELS_CONTEXT_PLUGIN_ID, MODELS_CONTEXT_PLUGIN_ID);
        pluginItems.addSet(new LicensedContextSet("Model", "model", model, modelProperties));
        
        LinkedHashMap<String, String> modelInstancesProperties = new LinkedHashMap<>();
        modelInstancesProperties.put(LicensingUnitsConstants.LU_CONTEXT, String.valueOf(MAX_INSTANCES));
        
        Map<String, String> modelInstance = new HashMap<String, String>();
        modelInstance.put(MODELS_INSTANCE_ID, MODELS_INSTANCE_ID);
        pluginItems.addSet(new LicensedContextSet("Model Instances", "model-instances", modelInstance, modelInstancesProperties));
        
        license.setLicensedContextSets(pluginItems);
        
        return license;
      }
    };
    
    Server.getSecurityDirector().addDefaultPermission(new DefaultPermission(ContextUtils.modelsContextPath(ContextUtils.USERNAME_PATTERN), "Models", false, false));
    
    super.setUp();
    
    modelContext2 = createModelContext(prepareModel("model2", "Model2"));
    modelContext2.install(getContextManager().getRoot());
  }
  
  @Override
  protected void tearDown() throws Exception
  {
    modelsContext.callFunction(EditableChildrenContext.F_DELETE, getCallerController(), modelContext2.getName());
    
    super.tearDown();
  }
  
  @Override
  protected Model prepareModel()
  {
    return prepareModel("model", "Model");
  }
  
  protected Model prepareModel(String name, String description)
  {
    final Model model = new Model(name, description);
    model.setType(Model.TYPE_ABSOLUTE);
    return model;
  }
  
  protected DataTable prepareModelEvts(int evtCount)
  {
    return prepareTestData(ModelContext.VFT_MODEL_VARIABLES, ModelContext.FIELD_VD_NAME, EVT_PREFIX, evtCount);
  }
  
  protected DataTable prepareModelFuncs(int funcCount)
  {
    return prepareTestData(ModelContext.VFT_MODEL_FUNCTIONS, ModelContext.FIELD_FD_NAME, FUNC_PREFIX, funcCount);
  }
  
  protected DataTable prepareModelVariables(int varCount)
  {
    return prepareTestData(ModelContext.VFT_MODEL_VARIABLES, ModelContext.FIELD_VD_NAME, VAR_PREFIX, varCount);
  }
  
  protected DataTable prepareTestData(TableFormat tableFormat, String nameField, String namePrefix, int count)
  {
    DataTable vars = new SimpleDataTable(tableFormat);
    for (int i = 0; i < count; i++)
    {
      DataRecord var = new DataRecord(tableFormat);
      var.setValue(nameField, namePrefix + i);
      vars.addRecord(var);
    }
    
    return vars;
  }
}
