package com.tibbo.aggregate.client.auth.kerberos;

import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.client.auth.*;
import com.tibbo.aggregate.client.gui.dialog.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.protocol.*;
import com.tibbo.aggregate.common.security.kerberos.*;
import com.tibbo.aggregate.common.util.*;

public class KerberosRemoteServerAuthenticator extends RemoteServerAuthenticator
{
  public KerberosRemoteServerAuthenticator(ClientCommandLineParameters parameters)
  {
    super(parameters);
  }
  
  @Override
  protected RemoteServer createServer(SplashAuthDialog saw) throws AggreGateException
  {
    final KerberosSplashAuthDialog kerberosSplashAuthDialog = (KerberosSplashAuthDialog) saw;
    
    final ClientCommandLineParameters parameters = getParameters();
    final String realm = parameters.getRealm();
    final String kdc = parameters.getKdc();
    final String servicePrincipal = parameters.getServicePrincipal();
    final int numConnectionAttempts = parameters.getNumConnectionAttempts();
    final long waitingTime = parameters.getWaitingTime() * TimeHelper.SECOND_IN_MS;
    
    final KerberosTokenProvider authenticator = kerberosSplashAuthDialog.isSso()
        ? new KerberosTokenProvider(realm, kdc, servicePrincipal, numConnectionAttempts, waitingTime)
        : new KerberosTokenProvider(realm, kdc, servicePrincipal, numConnectionAttempts, waitingTime,
            saw.getUsername(), saw.getPassword());
    return new RemoteServer(saw.getIpAddress(), Integer.parseInt(saw.getPort()), authenticator);
  }
  
  @Override
  protected int getMode()
  {
    return SplashAuthDialog.MODE_KERBEROS;
  }
  
  @Override
  protected SplashAuthDialog getSplashAuthDialog(ClientCommandLineParameters parameters)
  {
    return new KerberosSplashAuthDialog(getMode(), parameters.getAddress(), parameters.getPort());
  }
  
  @Override
  protected String getLoginFailedMessage()
  {
    return Pres.get().getString("mLoginFailedKerberos");
  }
}
