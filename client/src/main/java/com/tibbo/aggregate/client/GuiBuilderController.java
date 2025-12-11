package com.tibbo.aggregate.client;

import java.io.IOException;

import com.tibbo.aggregate.common.device.RemoteDeviceErrorException;
import com.tibbo.aggregate.common.protocol.RemoteServer;
import com.tibbo.aggregate.common.protocol.RemoteServerController;

public class GuiBuilderController extends RemoteServerController
{
  GuiBuilderController(RemoteServer device)
  {
    super(device, true);
  }
  
  @Override
  public synchronized void disconnect() throws IOException, InterruptedException, RemoteDeviceErrorException
  {
    try
    {
      super.disconnect();
    }
    finally
    {
      removeConnectionTimerTask();
    }
  }
}
