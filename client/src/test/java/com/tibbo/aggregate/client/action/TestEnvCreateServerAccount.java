package com.tibbo.aggregate.client.action;

import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.client.action.executor.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.action.command.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.tests.*;
import com.tibbo.aggregate.component.systemtree.*;
import com.tibbo.aggregate.component.systemtree.context.*;

public class TestEnvCreateServerAccount extends ClientTestCase
{
  public void test() throws Exception
  {
    // Wait for the System Tree to load the context...
    final SystemTreeServersContext stServers = (SystemTreeServersContext) ClientTestUtils.waitForSystemTreeContext(SystemTree.CTX_SERVERS);
    
    // Assert that the context exists
    if (stServers == null)
    {
      fail("Context '" + SystemTree.CTX_SERVERS + "' is not found");
    }
    
    // Get current number of the servers to compare it later with the new number
    final int accountCount = stServers.getChildren().size();
    
    // Operation invoker is a special class that lets a test case
    // to react to action commands. When the operation is invoked by
    // this class it has no visual response to action command
    OperationInvoker invoker = new OperationInvoker();
    
    // Invoke the operation in non-interactive mode
    NonInteractiveExecutor exec = invoker.invokeOperation(stServers, CreateServerAccountOperation.NAME);
    
    // Wait until the action returns the command;
    GenericActionCommand cmd = exec.waitActionCommand();
    
    // Check if the server-side action send a command
    if (cmd == null)
    {
      fail("Unexpected end of server-side action");
    }
    
    // To parse action commands it is correct to use normal executors with their parsing methods
    DataTable data = cmd.getParameters().rec().getDataTable(EditData.CF_DATA);
    data.rec().setValue(CreateServerAccountOperation.FIELD_USERNAME, "admin");
    data.rec().setValue(CreateServerAccountOperation.FIELD_PASSWORD, "admin");
    
    // Non interactive execution will continue after we send an ActionResponse...
    // Call to createResponseData() of the appropriate executor will make a datatable of the format required for you
    DataTable responseData = EditDataExecutor.createResponseData(data);
    exec.sendActionResponse(new GenericActionResponse(responseData));
    
    Condition cond = new Condition()
    {
      public boolean check()
      {
        return stServers.getChildren().size() - accountCount == 1;
      }
    };
    
    if (!AggreGateTestingUtils.wait(5000, 10, cond))
    {
      fail("No servers where added");
    }
  }
}
