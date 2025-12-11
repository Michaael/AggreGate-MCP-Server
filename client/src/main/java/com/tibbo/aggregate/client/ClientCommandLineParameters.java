package com.tibbo.aggregate.client;

import com.beust.jcommander.Parameter;
import com.tibbo.aggregate.common.protocol.DefaultClientController;
import com.tibbo.aggregate.common.protocol.RemoteServer;
import com.tibbo.aggregate.common.security.kerberos.configuration.AbstractKerberosConfiguration;
import com.tibbo.aggregate.component.systemtree.SystemTree;

public class ClientCommandLineParameters
{
  @Parameter(names = "-u", descriptionKey = "clUserName")
  private String userName;
  
  @Parameter(names = "-p", descriptionKey = "clPassword")
  private String password;
  
  @Parameter(names = "-a", descriptionKey = "clAdmin")
  private boolean adminMode;
  
  @Parameter(names = "-c", descriptionKey = "clCreateWorkspace")
  private boolean createWorkspace;
  
  @Parameter(names = "-l", descriptionKey = "clLaunchReference")
  private String autoLaunchReference;
  
  @Parameter(names = "-s", descriptionKey = "clSimpleMode")
  private boolean simpleMode;
  
  @Parameter(names = "-t", descriptionKey = "clSteadyStateMode")
  private boolean steadyStateMode;
  
  @Parameter(names = "-k", description = "Remote kiosk", hidden = true)
  private boolean kiosk;
  
  @Parameter(names = "-r", description = "Remote connection")
  private boolean remote;
  
  @Parameter(names = "-kerberos", description = "Kerberos authentication")
  private boolean kerberos;
  
  @Parameter(names = "-address", description = "Address", hidden = true)
  private String address = RemoteServer.DEFAULT_ADDRESS;
  
  @Parameter(names = "-port", description = "Port", hidden = true)
  private int port = RemoteServer.DEFAULT_PORT;
  
  @Parameter(names = "-kdc", description = "Key Distribution Center", hidden = true)
  private String kdc;
  
  @Parameter(names = "-realm", description = "Realm", hidden = true)
  private String realm;
  
  @Parameter(names = "-servicePrincipal", description = "Service Principal", hidden = true)
  private String servicePrincipal = AbstractKerberosConfiguration.DEFAULT_SERVICE_PRINCIPAL_NAME;
  
  @Parameter(names = "-numConnectionAttempts", description = "Number of Connection Attempts", hidden = true)
  private int numConnectionAttempts = AbstractKerberosConfiguration.DEFAULT_NUM_CONNECTION_ATTEMPTS;
  
  @Parameter(names = "-waitingTime", description = "Waiting Time between Connection Attempts, seconds", hidden = true)
  private long waitingTime = AbstractKerberosConfiguration.DEFAULT_WAITING_TIME;
  
  @Parameter(names = "-d", description = "Debug", hidden = true)
  private boolean debug;
  
  @Parameter(names = "-scr", descriptionKey = "clScreen")
  private int screen = 0;
  
  @Parameter(names = "-x", description = "Frame X Position")
  private Integer x;
  
  @Parameter(names = "-y", description = "Frame Y Position")
  private Integer y;
  
  @Parameter(names = "-w", description = "Frame Width")
  private int width = 0;
  
  @Parameter(names = "-h", description = "Frame Height")
  private int height = 0;
  
  @Parameter(names = "-td", descriptionKey = "clTooltipDelay")
  private final Long tooltipDelay = null;
  
  @Parameter(names = "-ac", descriptionKey = "clAutomaticConnection")
  private long autoConnect = DefaultClientController.KEEP_ALIVE_PERIOD;
  
  @Parameter(names = {"-treeLimit", "-treelimit"}, descriptionKey = "clTreeNodesLimit")
  private int treeNodesLimit = SystemTree.DEFAULT_LIMIT;

  @Parameter(names = "-uiBuilderUndoLimit", descriptionKey = "clUiBuilderUndoHistoryLimit")
  private int uiBuilderUndoLimit = 20;

  public String getUserName()
  {
    return userName;
  }
  
  public String getPassword()
  {
    return password;
  }
  
  public boolean isAdminMode()
  {
    return adminMode;
  }
  
  public boolean isCreateWorkspace()
  {
    return createWorkspace;
  }
  
  public boolean isSimpleMode()
  {
    return simpleMode;
  }
  
  public boolean isSteadyStateMode()
  {
    return steadyStateMode;
  }
  
  public boolean isDebug()
  {
    return debug;
  }
  
  public boolean isKiosk()
  {
    return kiosk;
  }
  
  public boolean isRemote()
  {
    return remote;
  }
  
  public boolean isKerberos()
  {
    return kerberos;
  }
  
  public String getAutoLaunchReference()
  {
    return autoLaunchReference;
  }
  
  public String getAddress()
  {
    return address;
  }
  
  public int getPort()
  {
    return port;
  }
  
  public String getKdc()
  {
    return kdc;
  }
  
  public String getRealm()
  {
    return realm;
  }
  
  public String getServicePrincipal()
  {
    return servicePrincipal;
  }
  
  public int getNumConnectionAttempts()
  {
    return numConnectionAttempts;
  }
  
  public long getWaitingTime()
  {
    return waitingTime;
  }
  
  public int getScreen()
  {
    return screen;
  }
  
  public Integer getX()
  {
    return x;
  }
  
  public Integer getY()
  {
    return y;
  }
  
  public int getWidth()
  {
    return width;
  }
  
  public int getHeight()
  {
    return height;
  }
  
  public Long getTooltipDelay()
  {
    return tooltipDelay;
  }
  
  public long getAutoConnect()
  {
    return autoConnect;
  }
  
  public int getTreeNodesLimit()
  {
    return treeNodesLimit;
  }

  public int getUiBuilderUndoLimit()
  {
    return uiBuilderUndoLimit;
  }
}
