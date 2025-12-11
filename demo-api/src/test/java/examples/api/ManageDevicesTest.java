package examples.api;

import static org.junit.Assert.assertNotEquals;

import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextManager;
import com.tibbo.aggregate.common.context.ContextUtils;
import com.tibbo.aggregate.common.context.EventDefinition;
import com.tibbo.aggregate.common.examples.AbstractTestExamples;
import com.tibbo.aggregate.common.server.DevicesContextConstants;

import java.util.List;

@SuppressWarnings("rawtypes")
public class ManageDevicesTest extends AbstractTestExamples
{
  private final static String DEVICE_NAME = "virtualDevice";
  private final static String DEVICE_DESCRIPTION = "Virtual Device";
  private final static String DRIVER_ID = "com.tibbo.linkserver.plugin.device.virtual";
  private final static String DEVICE_PATH = "users.admin.devices.virtualDevice";
  
  private Context context;
  
  @Override
  protected void setUp() throws Exception
  {
    super.setUp();
    context = getContext(ContextUtils.devicesContextPath(USERNAME));
  }
  
  @Override
  protected void tearDown() throws Exception
  {
    prepareForTesting();
    super.tearDown();
  }
  
  public void testListDeviceAccounts() throws Exception
  {
    ContextManager cm = context.getContextManager();
    createDeviceContext();
    List<Context> deviceContexts = ContextUtils.expandMaskToContexts(ContextUtils.deviceContextPath(USERNAME, ContextUtils.CONTEXT_GROUP_MASK), cm);
    
    assertNotNull(deviceContexts);
    assertNotEquals(0, deviceContexts.size());
  }
  
  public void testCreateDeviceAccount() throws Exception
  {
    Context deviceContext = createDeviceContext();
    
    assertEquals(DEVICE_NAME, deviceContext.getName());
  }
  
  public void testListDeviceSettings() throws Exception
  {
    List settings = createDeviceContext().getVariableDefinitions();
    
    assertNotNull(settings);
    assertNotEquals(0, settings.size());
  }
  
  public void testCallAddFunction() throws Exception
  {
    createDeviceContext();
    
    assertNotEquals(0, context.getFunctionData(DevicesContextConstants.F_ADD).getExecutionCount());
  }
  
  public void testExecuteDeviceOperation() throws Exception
  {
    final Context deviceContext = createDeviceContext();
    Thread.sleep(1000);
    ManageDevices.executeDeviceOperation(deviceContext);
    
    assertEquals(1, deviceContext.getFunctionData("generateEvent").getExecutionCount());
  }
  
  public void testDeleteDeviceAccount() throws Exception
  {
    Context deviceContext = createDeviceContext();
    ManageDevices.deleteDeviceAccount(deviceContext);
    
    assertNotEquals(0, context.getFunctionData("delete").getExecutionCount());
  }
  
  public void testListenerForDeviceEvents() throws Exception
  {
    Context deviceContext = createDeviceContext();
    Thread.sleep(1000);
    ManageDevices.listenerForDeviceEvents(deviceContext);
    EventDefinition ed = deviceContext.getEventDefinition("event1");
    assertNotNull(ed);
    
    long listenersSize = deviceContext.getEventData("event1").getListenersCount();
    assertNotEquals(0, listenersSize);
  }
  
  private void prepareForTesting() throws Exception
  {
    Context deviceContext = context.get(DEVICE_PATH);
    if (deviceContext != null)
    {
      ManageDevices.deleteDeviceAccount(deviceContext);
    }
  }
  
  private Context createDeviceContext() throws Exception
  {
    return ManageDevices.createDeviceAccount(USERNAME, DEVICE_NAME, DEVICE_DESCRIPTION, DRIVER_ID, context.getContextManager());
  }
  
}
