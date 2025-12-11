package com.tibbo.aggregate.client.action;

import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.client.action.executor.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.action.command.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.server.*;
import com.tibbo.aggregate.common.tests.*;
import com.tibbo.aggregate.component.systemtree.*;
import com.tibbo.aggregate.component.systemtree.context.*;

public class TestEnvCreateUserAccount extends ClientTestCase
{
  private static final String USERNAME = "cltest";
  
  public void testCreateUserAccount() throws Exception
  {
    // Wait for the System Tree to load the context...
    final SystemTreeServersContext stServers = (SystemTreeServersContext) ClientTestUtils.waitForSystemTreeContext("." + SystemTree.CTX_SERVERS);
    
    // Assert that the context exists
    if (stServers == null)
    {
      fail("System tree context not found: " + SystemTree.CTX_SERVERS);
    }
    
    Condition cond = new Condition()
    {
      public boolean check()
      {
        return stServers.getChildren().size() > 0;
      }
    };
    
    if (!AggreGateTestingUtils.wait(5000, 10, cond))
    {
      fail("No server accounts found");
    }
    
    SystemTreeDeviceRootContext stServer = (SystemTreeDeviceRootContext) stServers.getChildren().get(0);
    
    final SystemTreeDeviceContext stUsers = ClientTestUtils.getContext(stServer, Contexts.CTX_USERS);
    
    if (stUsers == null)
    {
      fail("System tree context not available for server context: " + Contexts.CTX_USERS);
    }
    
    String ucName = ContextUtils.userContextPath(USERNAME);
    
    // Deleting existing user
    Context serverUserContext = stServer.getRemoteContext().getContextManager().get(ucName);
    if (serverUserContext != null)
    {
      serverUserContext.getParent().callFunction(EditableChildrenContextConstants.F_DELETE, USERNAME);
    }
    
    // Checking existence
    SystemTreeDeviceContext stUser = ClientTestUtils.getContext(stServer, ucName);
    
    if (stUser != null)
    {
      throw new AssertionError("Server-side context already exists: " + ucName);
    }
    
    // Get current number of the servers to compare it later with the new number
    final int userCount = stUsers.getRemoteContext().getChildren().size();
    
    // Operation invoker is a special class that lets a test case
    // to react to action commands. When the operation is invoked by
    // this class it has no visual response to action command
    OperationInvoker invoker = new OperationInvoker();
    
    // Invoke the operation in non-interactive mode
    NonInteractiveExecutor exec = invoker.invokeOperation(stUsers, EditableChildrenContextConstants.A_CREATE);
    
    // Wait until the action returns the command;
    GenericActionCommand cmd = exec.waitActionCommand();
    
    // Check if the server-side action send a command
    if (cmd == null)
    {
      fail("Unexpected end of server-side action");
    }
    
    // To parse action commands it is correct to use normal executors with their parsing methods
    DataTable data = cmd.getParameters().rec().getDataTable(EditData.CF_DATA);
    data.rec().setValue(RootContextConstants.FIF_REGISTER_NAME, USERNAME);
    data.rec().setValue(RootContextConstants.FIF_REGISTER_PASSWORD, USERNAME);
    data.rec().setValue(RootContextConstants.FIF_REGISTER_PASSWORD_RE, USERNAME);
    
    // Non interactive execution will continue after we send an ActionResponse...
    // Call to createResponseData() of the appropriate executor will make a data table of the format required for you
    DataTable responseData = EditDataExecutor.createResponseData(data);
    exec.sendActionResponse(new GenericActionResponse(responseData));
    
    cond = new Condition()
    {
      public boolean check()
      {
        return stUsers.getRemoteContext().getChildren().size() - userCount == 1;
      }
    };
    
    if (!AggreGateTestingUtils.wait(5000, 10, cond))
    {
      fail("No new users found on the servers (" + (stUsers.getRemoteContext().getChildren().size() - userCount) + ")");
    }
  }
}
