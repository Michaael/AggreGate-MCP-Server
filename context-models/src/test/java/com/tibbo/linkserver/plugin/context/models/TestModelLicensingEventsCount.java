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

public class TestModelLicensingEventsCount extends BaseModelLicensingTest
{
  private static final int EXPECTED_COUNT = 10;
  
  public void testEventCountForOneModel() throws Exception
  {
    DataTable modelEvts = prepareModelEvts(EXPECTED_COUNT);
    modelContext.setVariable(ModelContextConstants.V_MODEL_EVENTS, getCallerController(), modelEvts);
    
    int currentEvtCount = LicensedContextStatisticsRegistry.getStatisticsRegistry(ServerFixture.MODELS_CONTEXT_PLUGIN_ID).getLicensingUnitValue(modelContext.getPath(),
        LicensingUnitsConstants.LU_EVENT);
    
    assertThat(currentEvtCount, is(equalTo(EXPECTED_COUNT)));
  }
  
  public void testEventCountForTwoModels() throws Exception
  {
    DataTable modelEvts = prepareModelEvts(EXPECTED_COUNT);
    modelContext.setVariable(ModelContextConstants.V_MODEL_EVENTS, getCallerController(), modelEvts);
    
    int currentEvtCount = LicensedContextStatisticsRegistry.getStatisticsRegistry(ServerFixture.MODELS_CONTEXT_PLUGIN_ID).getLicensingUnitValue(modelContext.getPath(),
        LicensingUnitsConstants.LU_EVENT);
    
    assertThat(currentEvtCount, is(equalTo(EXPECTED_COUNT)));
    
    modelContext2.setVariable(ModelContextConstants.V_MODEL_EVENTS, getCallerController(), modelEvts);
    
    int currentEvtCount2 = LicensedContextStatisticsRegistry.getStatisticsRegistry(ServerFixture.MODELS_CONTEXT_PLUGIN_ID).getLicensingUnitValue(modelContext2.getPath(),
        LicensingUnitsConstants.LU_EVENT);
    
    assertThat(currentEvtCount2, is(equalTo(EXPECTED_COUNT)));
    
    assertThat(currentEvtCount + currentEvtCount2, is(equalTo(EXPECTED_COUNT * 2)));
  }
  
  public void testEventCountAfterModelRemoving() throws Exception
  {
    DataTable modelEvts = prepareModelEvts(EXPECTED_COUNT);
    modelContext2.setVariable(ModelContextConstants.V_MODEL_EVENTS, getCallerController(), modelEvts);
    
    int currentEvtCount = LicensedContextStatisticsRegistry.getStatisticsRegistry(ServerFixture.MODELS_CONTEXT_PLUGIN_ID).getLicensingUnitValue(modelContext2.getPath(),
        LicensingUnitsConstants.LU_EVENT);
    
    assertThat(currentEvtCount, is(equalTo(EXPECTED_COUNT)));
    
    modelsContext.callFunction(EditableChildrenContext.F_DELETE, getCallerController(), modelContext2.getName());
    
    currentEvtCount = LicensedContextStatisticsRegistry.getStatisticsRegistry(ServerFixture.MODELS_CONTEXT_PLUGIN_ID).getLicensingUnitValue(modelContext2.getPath(), LicensingUnitsConstants.LU_EVENT);
    
    assertThat(currentEvtCount, is(equalTo(0)));
  }
  
  public void ignoreTestEventCountExceedsLicense() throws Exception
  {
    boolean gotError = false;
    int initialCount = modelContext.getVariable(ModelContextConstants.V_MODEL_EVENTS, getCallerController()).getRecordCount();
    int maxCount = getPluginDirector().getSecurityDirector().getLicensedUnitValueForPlugin(LicensingUnitsConstants.LU_EVENT,
        ServerFixture.MODELS_CONTEXT_PLUGIN_ID);
    
    assertThat("Maximum count from license " + maxCount + " != " + MAX_EVENTS, maxCount, is(equalTo(MAX_EVENTS)));
    
    DataTable modelEvts = prepareModelEvts(maxCount + 10);
    try
    {
      modelContext.setVariable(ModelContextConstants.V_MODEL_EVENTS, getCallerController(), modelEvts);
    }
    catch (ContextException ex)
    {
      Log.DEVICE_LICENSE.debug(ex.getMessage());
      if (Util.getRootCause(ex) instanceof LicenseViolationException)
      {
        gotError = true;
      }
    }
    
    int finalCount = modelContext.getVariable(ModelContextConstants.V_MODEL_EVENTS, getCallerController()).getRecordCount();
    
    String msg = "Initial number = " + initialCount + "; final number = " + finalCount + "; maximum number = " + maxCount + "; got error = " + gotError;
    Log.DEVICE_LICENSE.debug(msg);
    
    assertTrue(msg, gotError && (initialCount == finalCount));
  }
}
