package com.tibbo.linkserver.plugin.context.models;

import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.server.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.linkserver.context.*;
import com.tibbo.linkserver.security.*;
import com.tibbo.linkserver.tests.*;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TestModelLicensingVariablesCount extends BaseModelLicensingTest
{
  private static final int EXPECTED_COUNT = 10;
  
  public void testVariableCountForOneModel() throws Exception
  {
    DataTable modelVariables = prepareModelVariables(EXPECTED_COUNT);
    modelContext.setVariable(ModelContextConstants.V_MODEL_VARIABLES, getCallerController(), modelVariables);
    
    int currentVariableCount = LicensedContextStatisticsRegistry.getStatisticsRegistry(ServerFixture.MODELS_CONTEXT_PLUGIN_ID).getLicensingUnitValue(modelContext.getPath(),
        LicensingUnitsConstants.LU_VARIABLE);
    
    assertThat(currentVariableCount, is(equalTo(EXPECTED_COUNT)));
  }
  
  public void testVariableCountForTwoModels() throws Exception
  {
    DataTable modelVariables = prepareModelVariables(EXPECTED_COUNT);
    modelContext.setVariable(ModelContextConstants.V_MODEL_VARIABLES, getCallerController(), modelVariables);
    
    int currentVariableCount = LicensedContextStatisticsRegistry.getStatisticsRegistry(ServerFixture.MODELS_CONTEXT_PLUGIN_ID).getLicensingUnitValue(modelContext.getPath(),
        LicensingUnitsConstants.LU_VARIABLE);
    
    assertThat(currentVariableCount, is(equalTo(EXPECTED_COUNT)));
    
    DataTable modelVariables2 = prepareModelVariables(EXPECTED_COUNT);
    modelContext2.setVariable(ModelContextConstants.V_MODEL_VARIABLES, getCallerController(), modelVariables2);
    
    int currentVariableCount2 = LicensedContextStatisticsRegistry.getStatisticsRegistry(ServerFixture.MODELS_CONTEXT_PLUGIN_ID).getLicensingUnitValue(modelContext2.getPath(),
        LicensingUnitsConstants.LU_VARIABLE);
    
    assertThat(currentVariableCount2, is(equalTo(EXPECTED_COUNT)));
    
    assertThat(currentVariableCount + currentVariableCount2, is(equalTo(EXPECTED_COUNT * 2)));
  }
  
  public void testVariableCountAfterModelRemoving() throws Exception
  {
    DataTable modelVariables = prepareModelVariables(EXPECTED_COUNT);
    modelContext2.setVariable(ModelContextConstants.V_MODEL_VARIABLES, getCallerController(), modelVariables);
    
    int currentVariableCount = LicensedContextStatisticsRegistry.getStatisticsRegistry(ServerFixture.MODELS_CONTEXT_PLUGIN_ID).getLicensingUnitValue(modelContext2.getPath(),
        LicensingUnitsConstants.LU_VARIABLE);
    
    assertThat(currentVariableCount, is(equalTo(EXPECTED_COUNT)));
    
    modelsContext.callFunction(EditableChildrenContext.F_DELETE, getCallerController(), modelContext2.getName());
    
    currentVariableCount = LicensedContextStatisticsRegistry.getStatisticsRegistry(ServerFixture.MODELS_CONTEXT_PLUGIN_ID).getLicensingUnitValue(modelContext2.getPath(),
        LicensingUnitsConstants.LU_VARIABLE);
    
    assertThat(currentVariableCount, is(equalTo(0)));
  }
  
  public void testRemoveVariableForOneModel() throws Exception
  {
    DataTable modelVariables = prepareModelVariables(EXPECTED_COUNT * 2);
    modelContext.setVariable(ModelContextConstants.V_MODEL_VARIABLES, getCallerController(), modelVariables);
    
    int currentVariableCount = LicensedContextStatisticsRegistry.getStatisticsRegistry(ServerFixture.MODELS_CONTEXT_PLUGIN_ID).getLicensingUnitValue(modelContext.getPath(),
        LicensingUnitsConstants.LU_VARIABLE);
    assertThat(currentVariableCount, is(equalTo(EXPECTED_COUNT * 2)));
    
    modelVariables = prepareModelVariables(EXPECTED_COUNT);
    modelContext.setVariable(ModelContextConstants.V_MODEL_VARIABLES, getCallerController(), modelVariables);
    
    currentVariableCount = LicensedContextStatisticsRegistry.getStatisticsRegistry(ServerFixture.MODELS_CONTEXT_PLUGIN_ID).getLicensingUnitValue(modelContext.getPath(),
        LicensingUnitsConstants.LU_VARIABLE);
    assertThat(currentVariableCount, is(equalTo(EXPECTED_COUNT)));
  }
  
  public void ignoreTestVariableCountExceedsLicense() throws Exception
  {
    String errorMsg = "";
    boolean gotError = false;
    int initialCount = modelContext.getVariable(ModelContextConstants.V_MODEL_VARIABLES, getCallerController()).getRecordCount();
    int maxCount = getPluginDirector().getSecurityDirector().getLicensedUnitValueForPlugin(LicensingUnitsConstants.LU_VARIABLE,
        ServerFixture.MODELS_CONTEXT_PLUGIN_ID);
    
    assertThat("Maximum count from license " + maxCount + " != " + MAX_VARIABLES, maxCount, is(equalTo(MAX_VARIABLES)));
    
    DataTable modelVariables = prepareModelVariables(maxCount + 10);
    try
    {
      modelContext.setVariable(ModelContextConstants.V_MODEL_VARIABLES, getCallerController(), modelVariables);
    }
    catch (ContextException ex)
    {
      if (Util.getRootCause(ex) instanceof LicenseViolationException)
      {
        gotError = true;
      }
      errorMsg = ex.getMessage();
    }
    
    int finalCount = modelContext.getVariable(ModelContextConstants.V_MODEL_VARIABLES, getCallerController()).getRecordCount();
    String msg = "Initial number = " + initialCount + "; final number = " + finalCount + "; maximum number = " + maxCount + "; error msg = " + errorMsg;
    
    assertTrue(msg, gotError && (initialCount == finalCount));
  }
}
