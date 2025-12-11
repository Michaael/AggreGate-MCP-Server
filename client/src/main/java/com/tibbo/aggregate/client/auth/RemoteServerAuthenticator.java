package com.tibbo.aggregate.client.auth;

import javax.swing.*;

import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.client.gui.dialog.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.protocol.*;
import com.tibbo.aggregate.component.*;

public class RemoteServerAuthenticator extends AbstractServerAuthenticator
{
  public RemoteServerAuthenticator(ClientCommandLineParameters parameters)
  {
    super(parameters);
  }
  
  @Override
  public Workspace authenticate() throws AggreGateException
  {
    final SplashAuthDialog saw = getSplashAuthDialog(getParameters());
    try
    {
      authenticate(saw);
    }
    catch (ContextException ex)
    {
      if (ex.getCode() != null && ex.getCode().equalsIgnoreCase(AggreGateCodes.REPLY_CODE_LOCKED))
        JOptionPane.showMessageDialog(ComponentHelper.getMainFrame(), ex.getMessage());
      else
        JOptionPane.showMessageDialog(ComponentHelper.getMainFrame(), getLoginFailedMessage());
    }
    catch (Exception ex)
    {
      JOptionPane.showMessageDialog(ComponentHelper.getMainFrame(), getLoginFailedMessage());
    }
    
    final Workspace workspace = new Workspace();
    final RemoteServer server = createServer(saw);
    workspace.getDeviceList().add(server);
    
    ComponentHelper.setRemoteMode(true);
    ComponentHelper.setUsername(saw.getUsername());
    ComponentHelper.setPassword(saw.getPassword());
    
    return workspace;
  }
  
  @Override
  protected RemoteServer createServer(SplashAuthDialog saw) throws AggreGateException
  {
    return new RemoteServer(saw.getIpAddress(), Integer.parseInt(saw.getPort()), saw.getUsername(), saw.getPassword());
  }
  
  @Override
  protected void setSimpleMode(DataTable ui)
  {
    ComponentHelper.setSimpleMode(getParameters().isSimpleMode());
  }
  
  @Override
  protected int getMode()
  {
    return SplashAuthDialog.MODE_REMOTE_SERVER;
  }
  
  @Override
  protected SplashAuthDialog getSplashAuthDialog(ClientCommandLineParameters parameters)
  {
    return new RemoteServerSplashAuthDialog(getMode(), parameters.getAddress(), parameters.getPort());
  }
}
