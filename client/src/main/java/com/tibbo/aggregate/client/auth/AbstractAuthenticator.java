package com.tibbo.aggregate.client.auth;

import javax.swing.*;

import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.client.auth.kerberos.*;
import com.tibbo.aggregate.client.gui.dialog.*;
import com.tibbo.aggregate.component.*;

public abstract class AbstractAuthenticator implements Authenticator
{
  private final ClientCommandLineParameters parameters;
  
  public AbstractAuthenticator(ClientCommandLineParameters parameters)
  {
    this.parameters = parameters;
  }
  
  protected ClientCommandLineParameters getParameters()
  {
    return parameters;
  }
  
  protected boolean showStartupErrorAndExit(String msg)
  {
    JOptionPane.showMessageDialog(null, msg);
    
    if (ComponentHelper.isApplet())
    {
      return false;
    }
    else
    {
      System.exit(0);
      return true; // Will never happen
    }
  }
  
  public static Authenticator getInstance(ClientCommandLineParameters parameters)
  {
    if (parameters.isKiosk())
      return new ServerAuthenticator(parameters);
    
    if (parameters.isRemote())
      return new RemoteServerAuthenticator(parameters);
    
    if (parameters.isKerberos())
      return new KerberosRemoteServerAuthenticator(parameters);
    
    return new WorkspaceAuthenticator(parameters);
  }

  protected abstract int getMode();

  protected abstract SplashAuthDialog getSplashAuthDialog(ClientCommandLineParameters parameters);
}
