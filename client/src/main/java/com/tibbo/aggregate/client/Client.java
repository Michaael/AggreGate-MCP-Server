package com.tibbo.aggregate.client;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.net.Authenticator;

import javax.swing.*;

import com.tibbo.aggregate.client.auth.*;
import com.tibbo.aggregate.client.gui.*;
import com.tibbo.aggregate.client.workspace.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.common.widget.*;
import com.tibbo.aggregate.common.widget.runtime.util.*;
import com.tibbo.aggregate.component.*;

public class Client
{
  private static final int FLAG_CHECK_PERIOD = 100;
  
  private static final ClientCommandLineParameters PARAMETERS = new ClientCommandLineParameters();
  
  static
  {
    System.setProperty("log4j.configurationFile", PARAMETERS.isRemote() || PARAMETERS.isKerberos()
        ? Client.class.getResource(Log.CLIENT_LOGGING_CONFIG_FILENAME).toString()
        : new File(System.getProperty("user.dir") + File.separator
            + Log.CLIENT_LOGGING_CONFIG_FILENAME).toURI().toString());
  }
  
  private static final Client INSTANCE = new Client();
  
  private static final WorkspaceManager WORKSPACE_MANAGER = new WorkspaceManager();
  
  private static Workspace workspace = new Workspace();
  
  static void start(final Object mainClassInstance, final ClientWindowInitializer initializer)
  {
    try
    {
      SwingUtilities.invokeAndWait(() -> startAsync(mainClassInstance, initializer));
      
      if (getParameters().getScreen() != 0)
      {
        GuiUtils.showOnScreen(getParameters().getScreen(), ComponentHelper.getMainFrame().getFrame());
      }
    }
    catch (Exception ex)
    {
      ClientHelper.showStartupErrorAndExit(ex);
    }
  }
  
  static void startAsync(Object mainClassInstance, ClientWindowInitializer initializer)
  {
    try
    {
      UIManager.put("ClassLoader", Client.class.getClassLoader());
      
      UIHelper.setup();
      
      MainContainer mainContainer = new MainContainer();
      
      initializer.initialize(mainContainer);
      
      ComponentHelper.initialize(mainClassInstance, mainContainer);
      
      // Init static widget components list
      WidgetComponentFactory.getAll();
      
      workspace = AbstractAuthenticator.getInstance(getParameters()).authenticate();
      
      if (getParameters().getTooltipDelay() != null)
      {
        ToolTipManager.sharedInstance().setInitialDelay(getParameters().getTooltipDelay().intValue());
      }
      
      mainContainer.initMenu();
      
      if (!ComponentHelper.isSimpleMode())
      {
        mainContainer.initMainDashboard();
      }
      
      Log.CORE.debug("Successful authentication of user '" + ComponentHelper.getUsername() + "'");
      ComponentHelper.getConfig().setLastUsername(ComponentHelper.getUsername());
      
      Integer newLastVersion = ComponentHelper.isDebug() ? AggreGateVersionChecker.check("client", true, getWorkspace().getLastNewVersionReported(), false, !ComponentHelper.isSteadyStateMode())
          : null;
      
      if (newLastVersion != null)
      {
        getWorkspace().setLastNewVersionReported(newLastVersion);
      }
      
      mainContainer.start();
      if (ComponentHelper.isApplet() || ComponentHelper.isRemoteMode())
      {
        mainContainer.getDockingManager().resetToDefault();
        mainContainer.getFrame().setSize(mainContainer.getSize());
        mainContainer.getFrame().repaint();
      }
      if (getParameters().getX() != null || getParameters().getY() != null || getParameters().getWidth() > 0 || getParameters().getHeight() > 0)
      {
        Rectangle bounds = mainContainer.getFrame().getBounds();
        Insets insets = mainContainer.getFrame().getInsets();
        
        int x = getParameters().getX() != null ? (getParameters().getX() - insets.left) : bounds.x;
        int y = getParameters().getY() != null ? getParameters().getY() : bounds.y;
        int width = getParameters().getWidth() > 0 ? (getParameters().getWidth() + insets.left + insets.right) : bounds.width;
        int height = getParameters().getHeight() > 0 ? (getParameters().getHeight() + insets.bottom) : bounds.height;
        
        mainContainer.getFrame().setBounds(x, y, width, height);
      }
    }
    catch (Exception ex)
    {
      ClientHelper.showStartupErrorAndExit(ex);
    }
  }
  
  static boolean shutdown()
  {
    if (ComponentHelper.getUsername() != null && getWorkspaceManager() != null)
    {
      WorkspaceManager.saveWorkspace();
    }
    
    Log.CORE.info("Stopping " + Cres.get().getString("productClient"));
    
    return ComponentHelper.shutdownMainFrame();
  }
  
  protected void execute(String[] args)
  {
    
    // The below code is located in client, server-side V_INFO should be hidden to avoid format caching problems!
    // We cannot put that into start() as it shouldn't be called from Client tests that are executed inside server VM
    do
    {
      ClientHelper.init(args, Client.getParameters(), null);
      
      ClientWindowInitializer initializer = new DefaultClientWindowInitializer();
      
      start(this, initializer);
      
      ComponentHelper.setReloginFlag(false);
      ComponentHelper.setStopFlag(false);
      
      do
      {
        try
        {
          synchronized (INSTANCE)
          {
            INSTANCE.wait(FLAG_CHECK_PERIOD);
          }
        }
        catch (InterruptedException ex)
        {
          ComponentHelper.setStopFlag(true);
        }
        
        if (ComponentHelper.isStopFlag())
        {
          if (!shutdown())
          {
            ComponentHelper.setReloginFlag(false);
            ComponentHelper.setStopFlag(false);
          }
          else
          {
            initializer.terminate();
          }
        }
      }
      while (!ComponentHelper.isStopFlag());
      
      ComponentHelper.setUsername(null);
      ComponentHelper.setPassword(null);
    }
    while (ComponentHelper.isReloginFlag());
    
    System.exit(0);
  }
  
  private static void setHttpProxyAuthenticator()
  {
    String httpProxyHost = System.getProperty("http.proxyHost", "");
    String httpProxyPort = System.getProperty("http.proxyPort", "80");
    String httpProxyUser = System.getProperty("http.proxyUser", "");
    String httpProxyPassword = System.getProperty("http.proxyPassword", "");
    
    String httpsProxyHost = System.getProperty("https.proxyHost", "");
    String httpsProxyPort = System.getProperty("https.proxyPort", "");
    String httpsProxyUser = System.getProperty("https.proxyUser", "");
    String httpsProxyPassword = System.getProperty("https.proxyPassword", "");
    
    if ((!httpProxyHost.equals("")) && (!httpProxyPort.equals("")))
    {
      Log.CORE.info("Using HTTP Proxy: " + httpProxyHost + ":" + httpProxyPort);
    }
    
    if ((!httpsProxyHost.equals("")) && (!httpsProxyPort.equals("")))
    {
      Log.CORE.info("Using HTTPS Proxy: " + httpsProxyHost + ":" + httpsProxyPort);
    }
    
    if (((!httpProxyHost.equals("")) && (!httpProxyPort.equals("")) && (!httpProxyUser.equals("")) && (!httpProxyPassword.equals("")))
        || ((!httpsProxyHost.equals("")) && (!httpsProxyPort.equals("")) && (!httpsProxyUser.equals("")) && (!httpsProxyPassword.equals(""))))
    {
      Log.CORE.info("Setting HTTP Proxy Authenticator");
      
      Authenticator.setDefault(new Authenticator()
      {
        @Override
        protected PasswordAuthentication getPasswordAuthentication()
        {
          if (getRequestorType() == RequestorType.PROXY)
          {
            String prot = getRequestingProtocol().toLowerCase();
            String host = System.getProperty(prot + ".proxyHost", "");
            String port = System.getProperty(prot + ".proxyPort", "");
            String user = System.getProperty(prot + ".proxyUser", "");
            String password = System.getProperty(prot + ".proxyPassword", "");
            if (getRequestingHost().equalsIgnoreCase(host))
            {
              if (Integer.parseInt(port) == getRequestingPort())
              {
                return new PasswordAuthentication(user, password.toCharArray());
              }
            }
          }
          return null;
        }
      });
    }
  }
  
  public static void main(String[] args)
  {
    setHttpProxyAuthenticator();
    INSTANCE.execute(args);
  }
  
  public static WorkspaceManager getWorkspaceManager()
  {
    return WORKSPACE_MANAGER;
  }
  
  public static Workspace getWorkspace()
  {
    return workspace;
  }
  
  public static ClientCommandLineParameters getParameters()
  {
    return PARAMETERS;
  }
}
