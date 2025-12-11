package com.tibbo.aggregate.client;

import java.io.*;

import com.tibbo.aggregate.client.workspace.*;
import com.tibbo.aggregate.common.data.*;
import com.tibbo.aggregate.common.protocol.*;
import com.tibbo.aggregate.common.tests.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.component.*;

public class ClientFixture
{
  private static final String CLIENT_DIR = "client";
  private static final String WORKSPACE_NAME = "test";
  private static final String WORKSPACE_PASSWORD = "test";
  
  private static boolean STARTED;
  
  @SuppressWarnings("unused")
  private Client client;
  
  public ClientFixture()
  {
  }
  
  public void setUp()
  {
    try
    {
      if (!STARTED)
      {
        STARTED = true;
        
        ComponentHelper.setDebug(true);
        
        ClientHelper.init(new String[] { "-u", WORKSPACE_NAME, "-p", WORKSPACE_PASSWORD }, Client.getParameters(), ComponentHelper.getCustomizationUrl());
        
        deleteWorkspace(WORKSPACE_NAME);
        
        client = new Client();
        
        if (!ComponentHelper.getConfig().getHomeDirectory().endsWith(CLIENT_DIR))
        {
          File home = new File(ComponentHelper.getConfig().getHomeDirectory());
          String corePath = home.getParentFile().getAbsolutePath() + File.separator + CLIENT_DIR;
          ComponentHelper.getConfig().setHomeDirectory(corePath);
          ComponentHelper.getConfig().setHomeDirectory(corePath);
        }
        
        Workspace workspace = new Workspace();
        
        workspace.getDeviceList().add(new RemoteServer(RemoteServer.DEFAULT_ADDRESS, RemoteServer.DEFAULT_PORT, User.DEFAULT_ADMIN_USERNAME, User.DEFAULT_ADMIN_PASSWORD));
        
        Client.getWorkspaceManager().saveWorkspace(workspace, WORKSPACE_NAME, WORKSPACE_PASSWORD);
        
        Client.start(this, new DefaultClientWindowInitializer());
        
        AggreGateTestingUtils.wait(60000, 100, new Condition()
        {
          public boolean check()
          {
            if (ComponentHelper.getMainFrame() != null && !ComponentHelper.getMainFrame().hasActiveControllers())
            {
              return true;
            }

            return false;
          }
        });
      }
    }
    catch (Exception ex)
    {
      throw new RuntimeException(ex.getMessage(), ex);
    }
  }
  
  private void deleteWorkspace(String name)
  {
    String dataDir = ComponentHelper.getConfig().getDataDirectory();
    if (dataDir != null && dataDir.length() > 0)
    {
      FileUtils.deleteDirectory(new File(ComponentHelper.getConfig().getDataDirectory() + WorkspaceManager.WORKSPACES_SUBDIR + name));
    }
  }
  
  public void tearDown()
  {
  }
}
