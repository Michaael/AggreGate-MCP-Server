package com.tibbo.aggregate.client.auth;

import javax.swing.*;

import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.client.gui.dialog.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.protocol.*;
import com.tibbo.aggregate.common.server.*;
import com.tibbo.aggregate.component.*;

public abstract class AbstractServerAuthenticator extends AbstractAuthenticator
{
  public AbstractServerAuthenticator(ClientCommandLineParameters parameters)
  {
    super(parameters);
  }
  
  protected void authenticate(SplashAuthDialog saw) throws Exception
  {
    boolean authSuccess = false;
    while (!authSuccess)
    {
      if (saw.run() != SplashAuthDialog.OK)
        if (!showStartupErrorAndExit(Pres.get().getString("mAuthCancelled")))
          continue;
        
      // Testing connection
      final RemoteServer server = createServer(saw);
      server.setCountAttempts(true);
      RemoteServerController controller = new RemoteServerController(server, false);
      
      try
      {
        controller.connect();
      }
      catch (Exception ex)
      {
        JOptionPane.showMessageDialog(ComponentHelper.getMainFrame(), Pres.get().getString("rlsConnectionFailed"));
        continue;
      }
      
      controller.login();
      
      final Context userContext = controller.getContextManager().get(ContextUtils.userContextPath(server.getUsername()));
      
      if (userContext != null)
      {
        final DataTable ui = userContext.getVariable(UserContextConstants.V_UI);
        
        setSimpleMode(ui);
        
        int tooltipDelay = ui.rec().getLong(UserContextConstants.VF_UI_TOOLTIP_DELAY).intValue();
        ToolTipManager.sharedInstance().setInitialDelay(tooltipDelay);
        ToolTipManager.sharedInstance().setDismissDelay(30000);
        
        Log.CORE.info("UI preferences: " + ui);
      }
      
      controller.disconnect();
      
      Log.CORE.info("Authorized as user '" + server.getUsername() + "'");
      
      authSuccess = true;
    }
  }

  protected String getLoginFailedMessage()
  {
    return Pres.get().getString("mLoginFailed");
  }
  
  protected abstract RemoteServer createServer(SplashAuthDialog saw) throws AggreGateException;
  
  protected abstract void setSimpleMode(DataTable ui);
}
