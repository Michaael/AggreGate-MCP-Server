package com.tibbo.aggregate.client.action;

import java.util.concurrent.*;

import junit.framework.*;

import com.tibbo.aggregate.client.device.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.data.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.event.*;
import com.tibbo.aggregate.common.protocol.*;
import com.tibbo.aggregate.common.server.*;

public class TestEnvClient extends TestCase
{
  private static final String DSNAME2 = "mask2";
  private static final String DSNAME1 = "mask1";
  private static final String USERNAME2 = "TestUserII";
  private static final String USERNAME1 = "TestUser";
  
  public void testClient() throws Exception
  {
    RemoteServer server = new RemoteServer(RemoteServer.DEFAULT_ADDRESS, RemoteServer.DEFAULT_PORT, User.DEFAULT_ADMIN_USERNAME, User.DEFAULT_ADMIN_PASSWORD);
    AbstractAggreGateDeviceController dController = new ClientDeviceControllerFactory().createDeviceController(server);
    
    dController.connect();
    dController.login();
    
    ContextManager cm = dController.getContextManager();
    
    Context context = cm.get(ContextUtils.userContextPath(User.DEFAULT_ADMIN_USERNAME));
    
    Log.TEST.info("Editing context variable");
    final String testCity = "Tver";
    DataTable userInfo = context.getVariable(EditableChildContextConstants.V_CHILD_INFO);
    userInfo.rec().setValue(User.FIELD_CITY, testCity);
    context.setVariable(EditableChildContextConstants.V_CHILD_INFO, userInfo);
    userInfo = context.getVariable(EditableChildContextConstants.V_CHILD_INFO);
    String val = userInfo.rec().getValueAsString(User.FIELD_CITY);
    
    // Testing if variable edited successfully
    if (!val.equals(testCity))
    {
      fail("Context variable wasn't edited");
    }
    
    context = cm.get(Contexts.CTX_USERS);
    DataTable list = context.callFunction("list");
    if (list.getRecordCount() == 0)
    {
      fail("Context function call unsuccessfull");
    }
    
    cleanUp(cm);
    
    final CountDownLatch firstSignal = new CountDownLatch(1);
    final CountDownLatch secondSignal = new CountDownLatch(2);
    final int eventWaitRetryTimeout = 1000;
    boolean eventStatus = false;
    
    class TestContextEventListener extends DefaultContextEventListener
    {
      public void handle(Event event) throws EventHandlingException
      {
        firstSignal.countDown();
        secondSignal.countDown();
      }
    }
    
    // Creating and adding 'childAdded' event listener
    TestContextEventListener eventListener = new TestContextEventListener();
    
    context.addEventListener(AbstractContext.E_CHILD_ADDED, eventListener);
    
    // Getting root context and adding a user "TestUser"
    Context cRoot = cm.get(Contexts.CTX_ROOT);
    cRoot.callFunction(RootContextConstants.F_REGISTER, USERNAME1, "111111", "111111");
    
    eventStatus = firstSignal.await(eventWaitRetryTimeout, TimeUnit.MILLISECONDS);
    
    // Removing listener and testing if event was detected
    context.removeEventListener(AbstractContext.E_CHILD_ADDED, eventListener);
    
    if (!eventStatus)
    {
      fail("Context event wasn't detected");
    }
    
    // listener removed. Event will not be detected
    // adding a user "TestUserII"
    cRoot.callFunction(RootContextConstants.F_REGISTER, USERNAME2, "111111", "111111");
    
    // Don't forget to reset the flag
    
    eventStatus = secondSignal.await(eventWaitRetryTimeout, TimeUnit.MILLISECONDS);
    
    if (eventStatus)
    {
      fail("'childAdded' context event was detected by error");
    }
    
    final CountDownLatch latch1 = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(2);
    
    ContextEventListener cel = new DefaultContextEventListener()
    {
      public void handle(Event event) throws EventHandlingException
      {
        latch1.countDown();
        latch2.countDown();
      }
    };
    
    cm.addMaskEventListener(ContextUtils.deviceServersContextPath(ContextUtils.CONTEXT_GROUP_MASK), AbstractContext.E_INFO, cel);
    
    cm.get(ContextUtils.deviceServersContextPath(User.DEFAULT_ADMIN_USERNAME)).callFunction("add", DSNAME1, "test", "Test", false, false);
    
    if (!latch1.await(10, TimeUnit.SECONDS))
    {
      throw new Exception("'info' event not detected by mask listener");
    }
    
    cm.removeMaskEventListener(ContextUtils.deviceServersContextPath(ContextUtils.CONTEXT_GROUP_MASK), AbstractContext.E_INFO, cel);
    
    cm.get(ContextUtils.deviceServersContextPath(User.DEFAULT_ADMIN_USERNAME)).callFunction("add", DSNAME2, "test", "Test", false, false);
    
    if (latch2.await(2, TimeUnit.SECONDS))
    {
      throw new Exception("'info' event detected by mask listener after removing it from Context Manager");
    }
    
    cleanUp(cm);
  }
  
  private void cleanUp(ContextManager cm) throws ContextException
  {
    Context con = cm.get(ContextUtils.userContextPath(USERNAME1));
    if (con != null)
    {
      con.getParent().callFunction(EditableChildrenContextConstants.F_DELETE, USERNAME1);
    }
    
    con = cm.get(ContextUtils.userContextPath(USERNAME2));
    if (con != null)
    {
      con.getParent().callFunction(EditableChildrenContextConstants.F_DELETE, USERNAME2);
    }
    
    con = cm.get(ContextUtils.deviceServerContextPath(User.DEFAULT_ADMIN_USERNAME, DSNAME1));
    if (con != null)
    {
      con.callFunction("remove");
    }
    
    con = cm.get(ContextUtils.deviceServerContextPath(User.DEFAULT_ADMIN_USERNAME, DSNAME2));
    if (con != null)
    {
      con.callFunction("remove");
    }
  }
  
}
