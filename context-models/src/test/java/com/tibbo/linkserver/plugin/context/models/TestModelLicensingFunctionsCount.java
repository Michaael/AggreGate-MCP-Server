package com.tibbo.linkserver.plugin.context.models;

import com.tibbo.aggregate.common.*;
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

public class TestModelLicensingFunctionsCount extends BaseModelLicensingTest
{
  private static final int EXPECTED_COUNT = 10;
  
  public void testFuncCountForOneModel() throws Exception
  {
    DataTable modelFuncs = prepareModelFuncs(EXPECTED_COUNT);
    modelContext.setVariable(ModelContextConstants.V_MODEL_FUNCTIONS, getCallerController(), modelFuncs);
    
    int currentFuncCount = LicensedContextStatisticsRegistry.getStatisticsRegistry(ServerFixture.MODELS_CONTEXT_PLUGIN_ID).getLicensingUnitValue(modelContext.getPath(),
        LicensingUnitsConstants.LU_FUNCTION);
    
    assertThat(currentFuncCount, is(equalTo(EXPECTED_COUNT)));
  }
  
  public void testFuncCountForTwoModels() throws Exception
  {
    DataTable modelFuncs = prepareModelFuncs(EXPECTED_COUNT);
    modelContext.setVariable(ModelContextConstants.V_MODEL_FUNCTIONS, getCallerController(), modelFuncs);
    
    int currentFuncCount = LicensedContextStatisticsRegistry.getStatisticsRegistry(ServerFixture.MODELS_CONTEXT_PLUGIN_ID).getLicensingUnitValue(modelContext.getPath(),
        LicensingUnitsConstants.LU_FUNCTION);
    
    assertThat(currentFuncCount, is(equalTo(EXPECTED_COUNT)));
    
    modelContext2.setVariable(ModelContextConstants.V_MODEL_FUNCTIONS, getCallerController(), modelFuncs);
    
    int currentFuncCount2 = LicensedContextStatisticsRegistry.getStatisticsRegistry(ServerFixture.MODELS_CONTEXT_PLUGIN_ID).getLicensingUnitValue(modelContext2.getPath(),
        LicensingUnitsConstants.LU_FUNCTION);
    
    assertThat(currentFuncCount2, is(equalTo(EXPECTED_COUNT)));
    
    assertThat(currentFuncCount + currentFuncCount2, is(equalTo(EXPECTED_COUNT * 2)));
  }
  
  public void testFuncCountAfterModelRemoving() throws Exception
  {
    DataTable modelFuncs = prepareModelFuncs(EXPECTED_COUNT);
    modelContext2.setVariable(ModelContextConstants.V_MODEL_FUNCTIONS, getCallerController(), modelFuncs);
    
    int currentFuncCount = LicensedContextStatisticsRegistry.getStatisticsRegistry(ServerFixture.MODELS_CONTEXT_PLUGIN_ID).getLicensingUnitValue(modelContext2.getPath(),
        LicensingUnitsConstants.LU_FUNCTION);
    
    assertThat(currentFuncCount, is(equalTo(EXPECTED_COUNT)));
    
    modelsContext.callFunction(EditableChildrenContext.F_DELETE, getCallerController(), modelContext2.getName());
    
    currentFuncCount = LicensedContextStatisticsRegistry.getStatisticsRegistry(ServerFixture.MODELS_CONTEXT_PLUGIN_ID).getLicensingUnitValue(modelContext2.getPath(),
        LicensingUnitsConstants.LU_FUNCTION);
    
    assertThat(currentFuncCount, is(equalTo(0)));
  }
  
  public void ignoreTestFunctionCountExceedsLicense() throws Exception
  {
    boolean gotError = false;
    int initialCount = modelContext.getVariable(ModelContextConstants.V_MODEL_FUNCTIONS, getCallerController()).getRecordCount();
    int maxCount = getPluginDirector().getSecurityDirector().getLicensedUnitValueForPlugin(LicensingUnitsConstants.LU_FUNCTION,
        ServerFixture.MODELS_CONTEXT_PLUGIN_ID);
    
    assertThat("Maximum count from license " + maxCount + " != " + MAX_FUNCTIONS, maxCount, is(equalTo(MAX_FUNCTIONS)));
    
    DataTable modelFuncs = prepareModelFuncs(maxCount + 10);
    try
    {
      modelContext.setVariable(ModelContextConstants.V_MODEL_FUNCTIONS, getCallerController(), modelFuncs);
    }
    catch (ContextException ex)
    {
      Log.DEVICE_LICENSE.debug(ex.getMessage());
      if (Util.getRootCause(ex) instanceof LicenseViolationException)
      {
        gotError = true;
      }
    }
    
    int finalCount = modelContext.getVariable(ModelContextConstants.V_MODEL_FUNCTIONS, getCallerController()).getRecordCount();
    
    String msg = "Initial number = " + initialCount + "; final number = " + finalCount + "; maximum number = " + maxCount + "; got error = " + gotError;
    Log.DEVICE_LICENSE.debug(msg);
    
    assertTrue(msg, gotError && (initialCount == finalCount));
  }
  
  public void testFuncCountForOneModel1() throws Exception
  {
    DataTable modelFuncs = prepareModelFuncs(EXPECTED_COUNT);
    modelContext.setVariable(ModelContextConstants.V_MODEL_FUNCTIONS, getCallerController(), modelFuncs);
    
    LicensedContextStatisticsRegistry reg = LicensedContextStatisticsRegistry.getStatisticsRegistry(ServerFixture.MODELS_CONTEXT_PLUGIN_ID);
    
    int currentFuncCount = reg.getLicensingUnitValue(modelContext.getPath(), LicensingUnitsConstants.LU_FUNCTION);
    assertThat(currentFuncCount, is(equalTo(EXPECTED_COUNT)));
    
    final int expectedFunctionCallCount = 10;
    for (int i = 0; i < expectedFunctionCallCount; i++)
    {
      modelContext.callFunction(modelFuncs.rec().getString(ModelContext.FIELD_FD_NAME), getCallerController());
    }
    
    int currentFunctionCallCount = reg.getLicensingUnitValue(modelContext.getPath(), LicensingUnitsConstants.LU_FUNCTION_CALLS_PER_DAY);
    
    assertThat(currentFunctionCallCount, is(equalTo(expectedFunctionCallCount)));
  }
  
}
