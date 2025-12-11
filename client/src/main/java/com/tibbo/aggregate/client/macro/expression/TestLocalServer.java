package com.tibbo.aggregate.client.macro.expression;

import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.data.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.protocol.*;

public class TestLocalServer implements MacroFunction
{
  private static final String F_TEST = "test";
  
  private static final TableFormat FOFT_TEST = FieldFormat.create("<status><B>").wrap();
  
  public static final String ADDRESS = "localhost";
  public static final int PORT = 6460;
  public static final int SOCKET_TIMEOUT = 1000; // ms
  
  public Object execute(ContextManager ctxManager, Object... params)
  {
    boolean connected = testConnection(ADDRESS, PORT);
    return connected;
  }
  
  public FunctionDefinition getFunctionDefinition()
  {
    FunctionDefinition def = new FunctionDefinition(F_TEST, TableFormat.EMPTY_FORMAT, FOFT_TEST, "Test Local Server");
    
    return def;
  }
  
  private boolean testConnection(String address, int port)
  {
    try
    {
      RemoteServer server = new RemoteServer(address, port, User.DEFAULT_ADMIN_USERNAME, User.DEFAULT_ADMIN_PASSWORD);
      RemoteServerController controller = new RemoteServerController(server, false);
      controller.connect();
      controller.disconnect();
      return true;
    }
    catch (Exception ex)
    {
      return false;
    }
  }
}
