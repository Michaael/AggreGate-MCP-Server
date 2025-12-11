package com.tibbo.linkserver.plugin.context.models;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.server.*;
import com.tibbo.linkserver.plugin.*;
import com.tibbo.linkserver.security.*;
import com.tibbo.linkserver.tests.*;

public class TestModelBillingStatistics extends BaseModelLicensingTest
{
  private static final int EXPECTED_EVT_COUNT = 10;
  private static final int EXPECTED_FUNC_COUNT = 9;
  private static final int EXPECTED_VAR_COUNT = 8;
  
  protected SecurityDirector securityDirector;
  
  @Override
  public void setUp() throws Exception
  {
    super.setUp();

    securityDirector = ((ServerPluginDirector)getContextManager().getPluginDirector()).getSecurityDirector();
  }
  
  public void testEventCountForOneModel() throws Exception
  {     
    BillingStatistics stat = new BillingStatistics(securityDirector);
    
    DataTable modelEvts = prepareModelEvts(EXPECTED_EVT_COUNT);
    modelContext.setVariable(ModelContextConstants.V_MODEL_EVENTS, getCallerController(), modelEvts);
    assertThat(EXPECTED_EVT_COUNT, is(equalTo(modelEvts.getRecordCount())));
    
    int currentEvtCount = LicensedContextStatisticsRegistry.getStatisticsRegistry(ServerFixture.MODELS_CONTEXT_PLUGIN_ID).getLicensingUnitValue(modelContext.getPath(), LicensingUnitsConstants.LU_EVENT);     
    assertThat(currentEvtCount, is(equalTo(EXPECTED_EVT_COUNT)));
    
    stat.calculateEveryMinuteValues();
    
    Float currentStatVariableCount = stat.getMinuteValues().getVal(ServerFixture.MODELS_CONTEXT_PLUGIN_ID, LicensingUnitsConstants.LU_EVENT);  
    assertThat(currentStatVariableCount, is(equalTo(new Float(EXPECTED_EVT_COUNT))));
  }
  
  public void testFuncCountForOneModel() throws Exception
  {     
    BillingStatistics stat = new BillingStatistics(securityDirector);
    
    DataTable modelFuncs = prepareModelFuncs(EXPECTED_FUNC_COUNT);
    modelContext.setVariable(ModelContextConstants.V_MODEL_FUNCTIONS, getCallerController(), modelFuncs);
    assertThat(EXPECTED_FUNC_COUNT, is(equalTo(modelFuncs.getRecordCount())));
    
    int currentFuncCount = LicensedContextStatisticsRegistry.getStatisticsRegistry(ServerFixture.MODELS_CONTEXT_PLUGIN_ID).getLicensingUnitValue(modelContext.getPath(), LicensingUnitsConstants.LU_FUNCTION);    
    assertThat(currentFuncCount, is(equalTo(EXPECTED_FUNC_COUNT)));
    
    stat.calculateEveryMinuteValues();
    
    Float currentStatVariableCount = stat.getMinuteValues().getVal(ServerFixture.MODELS_CONTEXT_PLUGIN_ID, LicensingUnitsConstants.LU_FUNCTION);  
    assertThat(currentStatVariableCount, is(equalTo(new Float(EXPECTED_FUNC_COUNT))));
  }
    
  public void testVariableCountForOneModel() throws Exception
  {
    BillingStatistics stat = new BillingStatistics(securityDirector);
    
    DataTable modelVariables = prepareModelVariables(EXPECTED_VAR_COUNT);
    modelContext.setVariable(ModelContextConstants.V_MODEL_VARIABLES, getCallerController(), modelVariables);
    assertThat(EXPECTED_VAR_COUNT, is(equalTo(modelVariables.getRecordCount())));
   
    int currentVarCount = LicensedContextStatisticsRegistry.getStatisticsRegistry(ServerFixture.MODELS_CONTEXT_PLUGIN_ID).getLicensingUnitValue(modelContext.getPath(), LicensingUnitsConstants.LU_VARIABLE);     
    assertThat(currentVarCount, is(equalTo(EXPECTED_VAR_COUNT)));
    
    stat.calculateEveryMinuteValues();
    
    Float currentStatVariableCount = stat.getMinuteValues().getVal(ServerFixture.MODELS_CONTEXT_PLUGIN_ID, LicensingUnitsConstants.LU_VARIABLE);  
    assertThat(currentStatVariableCount, is(equalTo(new Float(EXPECTED_VAR_COUNT))));
  }
   
  public void testHourEventCountForOneModel() throws Exception
  {
    runOneHourStatisticsTest(ModelContextConstants.V_MODEL_EVENTS, LicensingUnitsConstants.LU_EVENT);
  }
  
  public void testHourFuncCountForOneModel() throws Exception
  {
    runOneHourStatisticsTest(ModelContextConstants.V_MODEL_FUNCTIONS, LicensingUnitsConstants.LU_FUNCTION);
  }
  
  public void testHourVariableCountForOneModel() throws Exception
  {
    runOneHourStatisticsTest(ModelContextConstants.V_MODEL_VARIABLES, LicensingUnitsConstants.LU_VARIABLE);
  }

  public void testTwoHourEvtCountForOneModel() throws Exception
  {
    runTwoHourStatisticsTest(ModelContextConstants.V_MODEL_EVENTS, LicensingUnitsConstants.LU_EVENT);
  }
  
  public void testTwoHourFuncCountForOneModel() throws Exception
  {
    runTwoHourStatisticsTest(ModelContextConstants.V_MODEL_FUNCTIONS, LicensingUnitsConstants.LU_FUNCTION);
  }
  
  public void testTwoHourVariableCountForOneModel() throws Exception
  {
    runTwoHourStatisticsTest(ModelContextConstants.V_MODEL_VARIABLES, LicensingUnitsConstants.LU_VARIABLE);
  }
  
  private void runOneHourStatisticsTest(String modelVarName, String licensingUnit) throws ContextException
  {
    BillingStatistics stat = new BillingStatistics(securityDirector);
    LicensedContextStatisticsRegistry reg = LicensedContextStatisticsRegistry.getStatisticsRegistry(ServerFixture.MODELS_CONTEXT_PLUGIN_ID);   
    
    final int repeatsCount = 10;
    final int expectedVarCount = 2;
    
    Float realRepeatsCount = 0f;
    Float sumOfVarCounts = 0f;
    
    for(int i = 1; i < repeatsCount + 1; i++)
    {         
      updateOneMinuteValues(stat, reg, expectedVarCount * i, modelVarName, licensingUnit);
      
      sumOfVarCounts += (expectedVarCount * i);
      realRepeatsCount ++;
    }
    
    for(int i = repeatsCount + 1; i > 0; i--)
    {         
      updateOneMinuteValues(stat, reg, expectedVarCount * i, modelVarName, licensingUnit);
      
      sumOfVarCounts += (expectedVarCount * i);
      realRepeatsCount ++;
    }
    
    stat.calculateEveryHourValues();
    
    Float currentStatVarCount = stat.getHourValues().getVal(ServerFixture.MODELS_CONTEXT_PLUGIN_ID, licensingUnit);
    assertThat(currentStatVarCount, is(equalTo(new Float(sumOfVarCounts/realRepeatsCount))));
  }
  
  private void runTwoHourStatisticsTest(String modelVarName, String licensingUnit) throws ContextException
  {
    BillingStatistics stat = new BillingStatistics(securityDirector);
    LicensedContextStatisticsRegistry reg = LicensedContextStatisticsRegistry.getStatisticsRegistry(ServerFixture.MODELS_CONTEXT_PLUGIN_ID);   
    
    final int repeatsCount = 10;
    final int expectedVarCount = 2;
    
    Float realRepeatsCount = 0f;
    Float sumOfVarCounts = 0f;
    
    for(int i = 1; i < repeatsCount + 1; i++)
    {         
      updateOneMinuteValues(stat, reg, expectedVarCount * i, modelVarName, licensingUnit);
      
      sumOfVarCounts += (expectedVarCount * i);
      realRepeatsCount ++;
    }
    
    stat.calculateEveryHourValues();
    
    Float currentStatVarCount = stat.getHourValues().getVal(ServerFixture.MODELS_CONTEXT_PLUGIN_ID, licensingUnit);
    assertThat(currentStatVarCount, is(equalTo(new Float(sumOfVarCounts/realRepeatsCount))));
    
    realRepeatsCount = 0f;
    sumOfVarCounts = 0f;
    
    for(int i = repeatsCount + 1; i > 0; i--)
    {         
      updateOneMinuteValues(stat, reg, expectedVarCount * i, modelVarName, licensingUnit);
      
      sumOfVarCounts += (expectedVarCount * i);
      realRepeatsCount ++;
    }
    
    stat.calculateEveryHourValues();
    
    currentStatVarCount = stat.getHourValues().getVal(ServerFixture.MODELS_CONTEXT_PLUGIN_ID, licensingUnit);
    assertThat(currentStatVarCount, is(equalTo(new Float(sumOfVarCounts/realRepeatsCount))));
  }
 
  private void updateOneMinuteValues(BillingStatistics stat, LicensedContextStatisticsRegistry reg, final int expectedCount, 
      String modelVarName, String licensingUnit ) throws ContextException
  {
    DataTable modelVar = null;
    
    if(modelVarName.equals(ModelContextConstants.V_MODEL_VARIABLES))
    {
      modelVar = prepareModelVariables(expectedCount);
    }
    else if(modelVarName.equals(ModelContextConstants.V_MODEL_FUNCTIONS))
    {
      modelVar = prepareModelFuncs(expectedCount);
    }
    else if(modelVarName.equals(ModelContextConstants.V_MODEL_EVENTS))
    {
      modelVar = prepareModelEvts(expectedCount);
    }
    else
    {
      return;
    }
    
    modelContext.setVariable(modelVarName, getCallerController(), modelVar);
        
    int currentCount = reg.getLicensingUnitValue(modelContext.getPath(), licensingUnit);      
    assertThat(currentCount, is(equalTo(expectedCount)));
    
    stat.calculateEveryMinuteValues();
    
    Float currentStatCount = stat.getMinuteValues().getVal(ServerFixture.MODELS_CONTEXT_PLUGIN_ID, licensingUnit);
    assertThat(currentStatCount, is(equalTo(new Float(expectedCount))));
  }
  
}
