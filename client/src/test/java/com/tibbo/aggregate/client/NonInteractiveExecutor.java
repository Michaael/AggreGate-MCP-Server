package com.tibbo.aggregate.client;

import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.common.action.*;

public class NonInteractiveExecutor implements ActionCommandExecutor
{
  private GenericActionCommand receivedCommand;
  private GenericActionResponse response;
  
  public boolean canExecute(GenericActionCommand cmd)
  {
    return true;
  }
  
  public void cancel()
  {
    
  }
  
  public synchronized GenericActionResponse execute(Operation originator, GenericActionCommand cmd)
  {
    receivedCommand = cmd;
    notifyAll();
    
    while (response == null)
    {
      try
      {
        wait();
      }
      catch (InterruptedException ex)
      {
        return null;
      }
    }
    
    GenericActionResponse resp = response;
    response = null;
    return resp;
  }
  
  public synchronized GenericActionCommand waitActionCommand()
  {
    while (receivedCommand == null)
    {
      try
      {
        wait();
      }
      catch (InterruptedException ex)
      {
        return null;
      }
    }
    
    GenericActionCommand cmd = receivedCommand;
    receivedCommand = null;
    return cmd;
  }
  
  public synchronized void sendActionResponse(GenericActionResponse resp)
  {
    response = resp;
    notifyAll();
  }
}
