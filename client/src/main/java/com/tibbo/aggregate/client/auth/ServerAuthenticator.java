package com.tibbo.aggregate.client.auth;

import javax.swing.*;

import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.client.gui.dialog.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.protocol.*;
import com.tibbo.aggregate.common.server.*;
import com.tibbo.aggregate.component.*;

public class ServerAuthenticator extends AbstractServerAuthenticator
{
  public ServerAuthenticator(ClientCommandLineParameters parameters)
  {
    super(parameters);
  }
  
  @Override
  public Workspace authenticate()
  {
    final SplashAuthDialog saw = getSplashAuthDialog(null);
    
    try
    {
      authenticate(saw);
    }
    catch (Exception e)
    {
      JOptionPane.showMessageDialog(ComponentHelper.getMainFrame(), getLoginFailedMessage());
    }
    
    final Workspace workspace = new Workspace();
    final RemoteServer server = createServer(saw);
    workspace.getDeviceList().add(server);
    
    ComponentHelper.setUsername(saw.getUsername());
    ComponentHelper.setPassword(saw.getPassword());
    
    return workspace;
  }
  
  @Override
  protected RemoteServer createServer(SplashAuthDialog saw)
  {
    final ClientCommandLineParameters parameters = getParameters();
    return new RemoteServer(parameters.getAddress(), parameters.getPort(), saw.getUsername(), saw.getPassword());
  }
  
  @Override
  protected void setSimpleMode(DataTable ui)
  {
    ComponentHelper.setSimpleMode(ui.rec().getBoolean(UserContextConstants.VF_UI_SIMPLE_MODE));
  }
  
  @Override
  protected int getMode()
  {
    return SplashAuthDialog.MODE_SERVER;
  }
  
  @Override
  protected SplashAuthDialog getSplashAuthDialog(ClientCommandLineParameters parameters)
  {
    return new WorkspaceSplashAuthDialog(getMode());
  }
}
