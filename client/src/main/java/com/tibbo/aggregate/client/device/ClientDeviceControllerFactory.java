package com.tibbo.aggregate.client.device;

import com.tibbo.aggregate.common.protocol.*;

public class ClientDeviceControllerFactory implements DeviceControllerFactory
{
  public AbstractAggreGateDeviceController createDeviceController(AggreGateDevice dev)
  {
    if (dev instanceof RemoteServer)
    {
      return new ClientSideServerController((RemoteServer) dev);
    }
    
    throw new IllegalArgumentException("Unknown device class: " + dev.getClass().getName());
  }
}
