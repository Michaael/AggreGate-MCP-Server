package com.tibbo.aggregate.client.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import com.jidesoft.docking.*;
import com.jidesoft.docking.event.*;
import com.jidesoft.swing.*;
import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.client.device.*;
import com.tibbo.aggregate.client.gui.dashboard.*;
import com.tibbo.aggregate.client.gui.dialog.*;
import com.tibbo.aggregate.client.gui.frame.*;
import com.tibbo.aggregate.client.macro.ui.*;
import com.tibbo.aggregate.client.util.*;
import com.tibbo.aggregate.client.workspace.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.communication.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.datatable.field.*;
import com.tibbo.aggregate.common.protocol.*;
import com.tibbo.aggregate.common.resource.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.component.*;
import com.tibbo.aggregate.component.systemtree.*;

public class MainContainer extends ClientContainer
{
  private static final int NET_ACTIVITY_CHECK_PERIOD = 200;
  private static final int NET_ACTIVITY_MONITOR_PERSISTENCE = 5;
  
  private Frame frame;
  
  private boolean working;
  
  private final JMenuBar mainMenu = new JMenuBar();
  private final JMenu fileMenu = new JMenu();
  private final JMenu helpMenu = new JMenu();
  private final JMenuItem exitMenuItem = new JMenuItem();
  private final JMenuItem reloginMenuItem = new JMenuItem();
  private final JMenuItem saveWorkspaceMenuItem = new JMenuItem();
  private final JMenuItem helpMenuItem = new JMenuItem();
  private final JMenuItem siteMenuItem = new JMenuItem();
  private final JMenuItem macroRecorderItem = new JMenuItem();
  private final JMenuItem macroGuideItem = new JMenuItem();
  private final JMenuItem aboutMenuItem = new JMenuItem();
  
  private final JMenuItem resetLayoutMenuItem = new JMenuItem();
  
  private final JCheckBoxMenuItem showSystemTreeMenuItem = new JCheckBoxMenuItem();
  private final JCheckBoxMenuItem showFavouritesMenuItem = new JCheckBoxMenuItem();
  private final JCheckBoxMenuItem showTrackersMenuItem = new JCheckBoxMenuItem();
  private final JCheckBoxMenuItem showAlertsMenuItem = new JCheckBoxMenuItem();
  private final JCheckBoxMenuItem enableAutoRunMenuItem = new JCheckBoxMenuItem();
  
  private final JMenuItem showAllEventLogsMenuItem = new JMenuItem();
  private final JMenuItem hideAllEventLogsMenuItem = new JMenuItem();
  private final JCheckBoxMenuItem preferModalDialogsMenuItem = new JCheckBoxMenuItem();
  
  private InternalFrame systemTreeFrame;
  private SystemTree systemTree;
  
  private InternalFrame favouritesFrame;
  private Favourites favourites;
  
  private InternalFrame trackersFrame;
  private Trackers trackers;
  
  private final JMenu viewMenu = new JMenu();
  private final JPanel statusBar = new JPanel();
  private final JLabel statusLabel = new JLabel();
  
  private final java.util.Timer netActivityTimer = new java.util.Timer();
  
  private Icon idleIcon;
  
  private final java.util.List<Icon> loadingIcons = new LinkedList<>();
  
  private javax.swing.Timer loadingAnimator;
  
  private final DockingManagerGroup dockingManagerGroup = new DockingManagerGroup();
  
  public MainContainer()
  {
  }
  
  @Override
  public void init()
  {
    if (!ComponentHelper.isSteadyStateMode())
    {
      setBorder(BorderFactory.createEmptyBorder(ComponentHelper.SPACING, ComponentHelper.SPACING, 0, ComponentHelper.SPACING));
    }
    
    DockableDashboard.customizeDockingManager(getDockingManager());
    
    customizeMainDockableManager();
    
    statusBar.setLayout(new BorderLayout());
    statusBar.setBorder(BorderFactory.createLineBorder(ComponentHelper.BACKGROUND_COLOR, 1));
    
    statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
    
    add(statusBar, java.awt.BorderLayout.SOUTH);
    
    statusBar.add(statusLabel, BorderLayout.WEST);
    
    idleIcon = ResourceManager.getImageIcon(OtherIcons.STATUS_IDLE);
    
    for (int i = 1; i <= 22; i++)
    {
      loadingIcons.add(ResourceManager.getImageIcon(OtherIcons.STATUS_LOADING + i));
    }
    
    loadingAnimator = new javax.swing.Timer(30, new AnimatorListener());
    
    setMessage(Pres.get().getString("mfIdle"));
    statusLabel.setIcon(idleIcon);
    
    statusLabel.addMouseListener(new ActiveCommandsViewer());
  }
  
  @Override
  public void initMainDashboard()
  {
    AbstractDashboard holder = new DockableDashboard("", MAIN_DASHBOARD_NAME, dockingManagerGroup, Client.getWorkspace(), null, null);
    
    WindowLocation location = new WindowLocation(WindowLocation.STATE_DOCKED, WindowLocation.SIDE_TOP, 0, InternalFrame.SIZE_DASHBOARD);
    
    DashboardProperties dp = new DashboardProperties();
    
    InternalFrame mainDashboard = DashboardHelper.createDashboardFrame(MAIN_DASHBOARD_NAME, MAIN_DASHBOARD_DESCRIPTION, holder, location, dp, null);
    mainDashboard.setAvailableButtons(DockableFrame.BUTTON_MAXIMIZE);
    mainDashboard.setFloatable(false);
    mainDashboard.setAutohidable(false);
    mainDashboard.setHidable(false);
    getDockingManager().addFrame(mainDashboard);
  }
  
  private void customizeMainDockableManager()
  {
    getDockingManager().setCrossDraggingAllowed(false);
    
    getDockingManager().setCrossDroppingAllowed(false);
    
    getDockingManager().setTabbedPaneCustomizer(new DefaultDockingManager.TabbedPaneCustomizer()
    {
      @Override
      public void customize(JideTabbedPane tabbedPane)
      {
        tabbedPane.setTabPlacement(SwingConstants.TOP);
        tabbedPane.setUseDefaultShowIconsOnTab(false);
        tabbedPane.setShowIconsOnTab(true);
        tabbedPane.setShowCloseButtonOnTab(true);
        tabbedPane.setTabResizeMode(JideTabbedPane.RESIZE_MODE_DEFAULT);
      }
    });
  }
  
  @Override
  public String getDefaultTitle()
  {
    return Cres.get().getString("productClient") + " v" + SoftwareVersion.getCurrentVersion();
  }
  
  public void initMenu()
  {
    mainMenu.setPreferredSize(new Dimension(500, 18));
    mainMenu.setBorder(BorderFactory.createEmptyBorder());
    
    fileMenu.setOpaque(false);
    fileMenu.setMnemonic('F');
    fileMenu.setText(Pres.get().getString("mfFile"));
    helpMenu.setOpaque(false);
    helpMenu.setMnemonic('H');
    helpMenu.setText(Cres.get().getString("help"));
    
    exitMenuItem.setText(Pres.get().getString("mfExit"));
    exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.ALT_MASK, false));
    exitMenuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        if (ClientUtils.confirm(Pres.get().getString("mfConfirmExit")))
        {
          ComponentHelper.stop();
        }
      }
    });
    
    reloginMenuItem.setText(ComponentHelper.isRemoteMode()
        ? Pres.get().getString("mfLogout")
        : Pres.get().getString("mfSwitchWorkspace"));
    reloginMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.ALT_MASK, false));
    reloginMenuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        if (ClientUtils.confirm(ComponentHelper.isRemoteMode()
            ? Pres.get().getString("mfConfirmLogout")
            : Pres.get().getString("mfConfirmSwitchWorkspace")))
        {
          ComponentHelper.relogin();
        }
      }
    });
    
    resetLayoutMenuItem.setText(Pres.get().getString("mfResetLayout"));
    resetLayoutMenuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        getDockingManager().resetToDefault();
        for (ClientDashboard dashboard : getDashboards())
        {
          dashboard.hasStarted();
        }
      }
    });
    
    helpMenuItem.setText(Pres.get().getString("mfOpenHelp"));
    helpMenuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        ClientUtils.showHelp(Docs.CLIENT);
      }
    });
    
    siteMenuItem.setText(MessageFormat.format(Pres.get().getString("mfOpenSite"), Cres.get().getString("product")));
    siteMenuItem.addActionListener(new UrlOpener(Cres.get().getString("urlProduct")));
    
    macroGuideItem.setAction(MacroUIFacade.getDefault().getStartGuideAction(Client.getWorkspace()));
    
    aboutMenuItem.setText(Pres.get().getString("mfAbout"));
    aboutMenuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        AboutDialog ad = new AboutDialog();
        ad.setLocationRelativeTo(MainContainer.this);
        ad.run();
      }
    });
    
    viewMenu.setOpaque(false);
    viewMenu.setMnemonic('V');
    viewMenu.setText(Pres.get().getString("mfView"));
    
    showSystemTreeMenuItem.setText(Pres.get().getString("mfSystemTree"));
    showSystemTreeMenuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        if (((JCheckBoxMenuItem) e.getSource()).isSelected())
        {
          ClientUtils.showFrame(systemTreeFrame.getKey());
          getDockingManager().activateFrame(MAIN_DASHBOARD_NAME);
        }
        else
        {
          ClientUtils.hideFrame(systemTreeFrame.getKey());
        }
      }
    });
    showFavouritesMenuItem.setText(Cres.get().getString("favourites"));
    showFavouritesMenuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        if (((JCheckBoxMenuItem) e.getSource()).isSelected())
        {
          ClientUtils.showFrame(favouritesFrame.getKey());
          getDockingManager().activateFrame(MAIN_DASHBOARD_NAME);
        }
        else
        {
          ClientUtils.hideFrame(favouritesFrame.getKey());
        }
      }
    });
    showTrackersMenuItem.setText(Cres.get().getString("trackers"));
    showTrackersMenuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        if (((JCheckBoxMenuItem) e.getSource()).isSelected())
        {
          ClientUtils.showFrame(trackersFrame.getKey());
          getDockingManager().activateFrame(MAIN_DASHBOARD_NAME);
        }
        else
        {
          ClientUtils.hideFrame(trackersFrame.getKey());
        }
      }
    });
    showAlertsMenuItem.setText(Cres.get().getString("alerts"));
    enableAutoRunMenuItem.setText(Cres.get().getString("autorun"));
    enableAutoRunMenuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        final boolean selected = ((JCheckBoxMenuItem) e.getSource()).isSelected();
        Client.getWorkspace().setAutoRun(selected);
      }
    });
    
    macroRecorderItem.setAction(MacroUIFacade.getDefault().getStartRecorderAction());
    
    showAllEventLogsMenuItem.setText(Pres.get().getString("mfShowAllEventLogs"));
    showAllEventLogsMenuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        for (ClientDashboard dashboard : getDashboards())
        {
          for (String key : dashboard.getElementNames())
          {
            if (key.startsWith(InternalFrame.FRAME_KEY_EVENTLOG + InternalFrame.FRAME_KEY_SEPARATOR))
            {
              ClientUtils.showFrame(key);
            }
          }
        }
      }
    });
    
    hideAllEventLogsMenuItem.setText(Pres.get().getString("mfHideAllEventLogs"));
    hideAllEventLogsMenuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        for (ClientDashboard dashboard : getDashboards())
        {
          for (String key : dashboard.getElementNames())
          {
            if (key.startsWith(InternalFrame.FRAME_KEY_EVENTLOG + InternalFrame.FRAME_KEY_SEPARATOR))
            {
              ClientUtils.hideFrame(key);
            }
          }
        }
      }
    });
    
    preferModalDialogsMenuItem.setText(Pres.get().getString("mfPreferModalDialogs"));
    preferModalDialogsMenuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        
        boolean selected = ((JCheckBoxMenuItem) e.getSource()).isSelected();
        Client.getWorkspace().setPreferModalDialogs(selected);
      }
    });
    
    saveWorkspaceMenuItem.setText(Pres.get().getString("mfSaveWorkspace"));
    saveWorkspaceMenuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        saveWorkspace();
        
        JOptionPane.showMessageDialog(MainContainer.this, Pres.get().getString("mfSaveWorkspaceSuccess"));
      }
    });
    
    String urlDocs = Cres.get().getString("urlDocumentation") + Docs.TUTORIALS + "." + Constants.DOCS_FILE_EXTENSION;
    JMenuItem tutorialsMenuItem = new JMenuItem(Pres.get().getString("mfOpenTutorials"));
    tutorialsMenuItem.addActionListener(new UrlOpener(urlDocs));
    
    if (!Client.getParameters().isKiosk())
    {
      mainMenu.add(fileMenu);
    }
    mainMenu.add(viewMenu);
    mainMenu.add(helpMenu);
    
    if (!Client.getParameters().isRemote() && !Client.getParameters().isKerberos())
    {
      fileMenu.add(saveWorkspaceMenuItem);
    }
    fileMenu.add(reloginMenuItem);
    fileMenu.add(exitMenuItem);
    
    helpMenu.add(helpMenuItem);
    helpMenu.add(siteMenuItem);
    if (!Client.getParameters().isKiosk() && MacroUIFacade.getDefault().macrosAvailable())
    {
      helpMenu.add(macroGuideItem);
    }
    helpMenu.add(tutorialsMenuItem);
    helpMenu.add(new JSeparator());
    
    String urlSupportRequest = Cres.get().getString("urlSupportRequest");
    if (urlSupportRequest != null && urlSupportRequest.length() > 0)
    {
      JMenuItem requestSupportMenuItem = new JMenuItem(Pres.get().getString("mfRequestSupport"));
      requestSupportMenuItem.addActionListener(new UrlOpener(urlSupportRequest));
      helpMenu.add(requestSupportMenuItem);
    }
    
    String urlAskQuestion = Cres.get().getString("urlAskQuestion");
    if (urlAskQuestion != null && urlAskQuestion.length() > 0)
    {
      JMenuItem askQuestionMenuItem = new JMenuItem(Pres.get().getString("mfAskQuestion"));
      askQuestionMenuItem.addActionListener(new UrlOpener(urlAskQuestion));
      helpMenu.add(askQuestionMenuItem);
    }
    
    String urlFeasibilityEvaluation = Cres.get().getString("urlFeasibilityEvaluation");
    if (urlFeasibilityEvaluation != null && urlFeasibilityEvaluation.length() > 0)
    {
      JMenuItem feasibilityEvaluationMenuItem = new JMenuItem(Pres.get().getString("mfFeasibilityEvaluation"));
      feasibilityEvaluationMenuItem.addActionListener(new UrlOpener(urlFeasibilityEvaluation));
      helpMenu.add(feasibilityEvaluationMenuItem);
    }
    
    String urlFeatureRequest = Cres.get().getString("urlFeatureRequest");
    if (urlFeatureRequest != null && urlFeatureRequest.length() > 0)
    {
      JMenuItem featureRequestMenuItem = new JMenuItem(Pres.get().getString("mfFeatureRequest"));
      featureRequestMenuItem.addActionListener(new UrlOpener(urlFeatureRequest));
      helpMenu.add(featureRequestMenuItem);
    }
    
    String urlForums = Cres.get().getString("urlSupportForum");
    if (urlForums != null && urlForums.length() > 0)
    {
      JMenuItem forumsMenuItem = new JMenuItem(Pres.get().getString("mfSupportForums"));
      forumsMenuItem.addActionListener(new UrlOpener(Cres.get().getString("urlSupportForum")));
      helpMenu.add(forumsMenuItem);
    }
    
    helpMenu.add(new JSeparator());
    helpMenu.add(aboutMenuItem);
    
    viewMenu.add(resetLayoutMenuItem);
    if (!ComponentHelper.isSimpleMode())
    {
      viewMenu.addSeparator();
      viewMenu.add(showSystemTreeMenuItem);
      viewMenu.add(showFavouritesMenuItem);
      viewMenu.add(showTrackersMenuItem);
      viewMenu.add(showAlertsMenuItem);
      viewMenu.add(enableAutoRunMenuItem);
      viewMenu.addSeparator();
      viewMenu.add(showAllEventLogsMenuItem);
      viewMenu.add(hideAllEventLogsMenuItem);
      viewMenu.addSeparator();
      viewMenu.add(preferModalDialogsMenuItem);
      if (ComponentHelper.getPermissionChecker().has(ComponentHelper.getCallerController(), ClientPermissionChecker.ADMIN, null, null))
      {
        viewMenu.addSeparator();
        viewMenu.add(macroRecorderItem);
      }
    }
  }
  
  @Override
  public void start()
  {
    startNetActivityMonitor();
    
    systemTree = new SystemTree(Client.getWorkspace().getDeviceList(), new ClientDeviceControllerFactory(), Client.getParameters().getTreeNodesLimit());
    
    showAlertsMenuItem.setSelected(true);
    
    enableAutoRunMenuItem.setSelected(Client.getWorkspace().isAutoRun());
    
    preferModalDialogsMenuItem.setSelected(Client.getWorkspace().isPreferModalDialogs());
    
    if (ComponentHelper.isSimpleMode())
    {
      return; // Skip creating frames in simple mode
    }
    
    WindowLocation stLocation = new WindowLocation(WindowLocation.STATE_DOCKED, WindowLocation.SIDE_LEFT, 0, InternalFrame.SIZE_SYSTEM_TREE);
    
    DashboardProperties stDashboard = new DashboardProperties(ClientContainer.MAIN_DASHBOARD_NAME, ClientContainer.MAIN_DASHBOARD_DESCRIPTION);
    
    systemTreeFrame = new InternalFrame(InternalFrame.FRAME_KEY_SYSTEMTREE, Pres.get().getString("mfSystem"), stLocation, stDashboard, systemTree, false, false, Docs.CL_SYSTEM_TREE,
        systemTree.getRemoteConnector());
    systemTreeFrame.setFrameIcon(ResourceManager.getImageIcon(Icons.FR_SYSTEM_TREE));
    systemTreeFrame.addDockableFrameListener(new DockableFrameAdapter()
    {
      @Override
      public void dockableFrameHidden(DockableFrameEvent dockableFrameEvent)
      {
        showSystemTreeMenuItem.setSelected(false);
      }
      
      @Override
      public void dockableFrameShown(DockableFrameEvent dockableFrameEvent)
      {
        showSystemTreeMenuItem.setSelected(true);
      }
    });
    addElement(null, systemTreeFrame);
    showSystemTreeMenuItem.setSelected(systemTreeFrame.isVisible());
    
    favourites = new Favourites(getFrame(), systemTree);
    
    WindowLocation faLocation = new WindowLocation(WindowLocation.STATE_DOCKED, WindowLocation.SIDE_TOP, 0, InternalFrame.SIZE_FAVOURITES);
    
    DashboardProperties faDashboard = new DashboardProperties(ClientContainer.MAIN_DASHBOARD_NAME, ClientContainer.MAIN_DASHBOARD_DESCRIPTION);
    
    favouritesFrame = new InternalFrame(InternalFrame.FRAME_KEY_FAVOURITES, Cres.get().getString("favourites"), faLocation, faDashboard, favourites, false, false, Docs.LS_FAVOURITES,
        systemTree.getRemoteConnector());
    favouritesFrame.setFrameIcon(ResourceManager.getImageIcon(Icons.ST_FAVOURITES));
    favouritesFrame.addDockableFrameListener(new DockableFrameAdapter()
    {
      @Override
      public void dockableFrameHidden(DockableFrameEvent dockableFrameEvent)
      {
        showFavouritesMenuItem.setSelected(false);
      }
      
      @Override
      public void dockableFrameShown(DockableFrameEvent dockableFrameEvent)
      {
        showFavouritesMenuItem.setSelected(true);
      }
    });
    addElement(null, favouritesFrame);
    showFavouritesMenuItem.setSelected(favouritesFrame.isVisible());
    
    trackers = new Trackers(getFrame(), systemTree);
    
    WindowLocation trLocation = new WindowLocation(WindowLocation.STATE_DOCKED, WindowLocation.SIDE_TOP, 0, InternalFrame.SIZE_TRACKERS);
    
    DashboardProperties trDashboard = new DashboardProperties(ClientContainer.MAIN_DASHBOARD_NAME, ClientContainer.MAIN_DASHBOARD_DESCRIPTION);
    
    trackersFrame = new InternalFrame(InternalFrame.FRAME_KEY_TRACKERS, Cres.get().getString("trackers"), trLocation, trDashboard, trackers, false, false, Docs.LS_TRACKERS,
        systemTree.getRemoteConnector());
    trackersFrame.setFrameIcon(ResourceManager.getImageIcon(Icons.ST_TRACKERS));
    trackersFrame.addDockableFrameListener(new DockableFrameAdapter()
    {
      @Override
      public void dockableFrameHidden(DockableFrameEvent dockableFrameEvent)
      {
        showTrackersMenuItem.setSelected(false);
      }
      
      @Override
      public void dockableFrameShown(DockableFrameEvent dockableFrameEvent)
      {
        showTrackersMenuItem.setSelected(true);
      }
    });
    addElement(null, trackersFrame);
    showTrackersMenuItem.setSelected(trackersFrame.isVisible());
    
    ClientUtils.hideFrame(InternalFrame.FRAME_KEY_FAVOURITES);
    ClientUtils.hideFrame(InternalFrame.FRAME_KEY_TRACKERS);
    
  }
  
  @Override
  public List<ClientDashboard> getDashboards()
  {
    List<ClientDashboard> res = new LinkedList<>();
    
    DockingManager dm = getDockingManager();
    
    Collection<String> frames = dm != null ? dm.getAllFrames() : null;
    
    if (frames == null)
    {
      return res;
    }
    
    for (String key : frames)
    {
      DashboardFrame firstLevel = (DashboardFrame) dm.getFrame(key);
      
      res.add((ClientDashboard) firstLevel.getMainComponent());
    }
    
    return res;
  }
  
  @Override
  public ClientDashboard getDashboard(String dashboardName)
  {
    return DashboardHelper.getDashboard(dashboardName, getDockingManager());
  }
  
  @Override
  public ClientDashboard createDashboard(DashboardProperties dp, RemoteConnector connector)
  {
    return DashboardHelper.addDashboard(dp.getName(), dp.getDescription(), dp, getDockingManager(), dockingManagerGroup, Client.getWorkspace(), connector,
        DashboardHelper.createStorageSession(connector, dp));
  }
  
  @Override
  public ClientDashboard addElement(RemoteConnector connector, DashboardElement element)
  {
    return addElement(connector, element, null);
  }
  
  @Override
  public ClientDashboard addElement(RemoteConnector connector, DashboardElement element, DashboardsHierarchyInfo dhInfo)
  {
    return DashboardHelper.addElement(element, getDockingManager(), dockingManagerGroup, Client.getWorkspace(), connector,
        DashboardHelper.createStorageSession(connector, element.getDashboardProperties()), dhInfo);
  }
  
  @Override
  public void removeDashboard(String dashboardName)
  {
    getDockingManager().removeFrame(dashboardName);
  }
  
  private void startNetActivityMonitor()
  {
    netActivityTimer.schedule(new NetActivityMonitorTimerTask(), 0, NET_ACTIVITY_CHECK_PERIOD);
  }
  
  @Override
  public void setMessage(String message)
  {
    statusLabel.setText(message);
  }
  
  private String getMessage()
  {
    return statusLabel.getText();
  }
  
  @Override
  public boolean hasActiveControllers()
  {
    for (AggreGateDeviceController connector : ComponentHelper.getConnectors())
    {
      if (connector.isActive())
      {
        return true;
      }
    }
    return false;
  }
  
  @Override
  public boolean shutdown()
  {
    for (Object name : getDockingManager().getAllFrames())
    {
      DockableFrame fr = getDockingManager().getFrame(name.toString());
      if (fr instanceof InternalFrame)
      {
        if (!((InternalFrame) fr).canShutdown())
        {
          return false;
        }
      }
    }
    
    return true;
  }
  
  @Override
  public void saveFrameData()
  {
    for (ClientDashboard dashboard : getDashboards())
    {
      for (String key : dashboard.getElementNames())
      {
        DashboardElement el = dashboard.getElement(key);
        if (el != null)
        {
          el.saveFrameData();
        }
      }
    }
  }
  
  @Override
  public void saveWorkspace()
  {
    WorkspaceManager.saveWorkspace();
  }
  
  @Override
  protected DockingManager createDockingManager(RootPaneContainer container)
  {
    return new CustomDockingManager(container, this);
  }
  
  private final class UrlOpener implements ActionListener
  {
    private final String url;
    
    private UrlOpener(String url)
    {
      this.url = url;
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
      try
      {
        BrowseHelper.browse(new URI(url), ComponentHelper.getConfig().getHomeDirectory());
      }
      catch (Exception ex)
      {
        Log.CORE.warn("Error opening URL: " + url, ex);
      }
    }
  }
  
  private final class ActiveCommandsViewer extends MouseAdapter
  {
    @Override
    public void mouseClicked(MouseEvent e)
    {
      TableFormat format = new TableFormat();
      format.addField("<server><S><D=" + Cres.get().getString("server") + ">");
      format.addField("<time><L><D=" + Cres.get().getString("time") + "><E=" + LongFieldFormat.EDITOR_PERIOD + ">");
      format.addField("<command><S><D=" + Cres.get().getString("command") + "><E=" + StringFieldFormat.EDITOR_TEXT_AREA + "><O=300>");
      DataTable res = new SimpleDataTable(format);
      for (AggreGateDeviceController connector : ComponentHelper.getConnectors())
      {
        for (ReplyMonitor<OutgoingAggreGateCommand, IncomingAggreGateCommand> cmd : (List<ReplyMonitor<OutgoingAggreGateCommand, IncomingAggreGateCommand>>) connector.getActiveCommands())
        {
          String commandStr = AggreGateCommand.checkCommandString(cmd.getCommand().toString());
          
          res.addRecord().addString(connector.getDevice().toString()).addLong(System.currentTimeMillis() - cmd.getStartTime()).addString(commandStr);
        }
      }
      
      DataTableEditorDialog.showData(getFrame(), Pres.get().getString("mActiveCommands"), res);
    }
  }
  
  private class AnimatorListener implements ActionListener
  {
    @Override
    public void actionPerformed(ActionEvent e)
    {
      int i = loadingIcons.indexOf(statusLabel.getIcon());
      if (i == loadingIcons.size() - 1)
      {
        statusLabel.setIcon(loadingIcons.get(0));
      }
      else
      {
        statusLabel.setIcon(loadingIcons.get(i + 1));
      }
    }
  }
  
  private class NetActivityMonitorTimerTask extends TimerTask
  {
    private int notActiveCount = 0;
    
    public NetActivityMonitorTimerTask()
    {
    }
    
    @Override
    public void run()
    {
      boolean hasActiveControllers = hasActiveControllers();
      
      if (hasActiveControllers)
      {
        SwingUtilities.invokeLater(new Runnable()
        {
          @Override
          public void run()
          {
            if (getMessage().equals(Pres.get().getString("mfIdle")))
            {
              setMessage(Pres.get().getString("mfLoading"));
            }
            loadingAnimator.start();
          }
        });
        notActiveCount = 0;
        working = true;
      }
      else
      {
        notActiveCount++;
        
        if (notActiveCount > NET_ACTIVITY_MONITOR_PERSISTENCE)
        {
          SwingUtilities.invokeLater(new Runnable()
          {
            @Override
            public void run()
            {
              setMessage(Pres.get().getString("mfIdle"));
              statusLabel.setIcon(idleIcon);
              loadingAnimator.stop();
            }
          });
          notActiveCount = 0;
          working = false;
        }
      }
    }
  }
  
  @Override
  public SystemTree getSystemTree()
  {
    return systemTree;
  }
  
  @Override
  public JMenuBar getMenuBar()
  {
    return mainMenu;
  }
  
  public boolean isWorking()
  {
    return working;
  }
  
  @Override
  public Favourites getFavourites()
  {
    return favourites;
  }
  
  @Override
  public Trackers getTrackers()
  {
    return trackers;
  }
  
  @Override
  public boolean isShowAlerts()
  {
    return showAlertsMenuItem.isSelected();
  }
  
  @Override
  public boolean isAutoRun()
  {
    return enableAutoRunMenuItem.isSelected();
  }
  
  @Override
  public File getCommonDirectory()
  {
    return Client.getWorkspace() != null && Client.getWorkspace().getCommonDirectory() != null ? new File(Client.getWorkspace().getCommonDirectory()) : null;
  }
  
  @Override
  public File getDataTableDirectory()
  {
    return Client.getWorkspace() != null && Client.getWorkspace().getDataTableDirectory() != null ? new File(Client.getWorkspace().getDataTableDirectory()) : null;
  }
  
  @Override
  public File getPropertiesDirectory()
  {
    return Client.getWorkspace() != null && Client.getWorkspace().getPropertiesDirectory() != null ? new File(Client.getWorkspace().getPropertiesDirectory()) : null;
  }
  
  @Override
  public File getDataDirectory()
  {
    return Client.getWorkspace() != null && Client.getWorkspace().getDataDirectory() != null ? new File(Client.getWorkspace().getDataDirectory()) : null;
  }
  
  @Override
  public void setCommonDirectory(File directory)
  {
    if (Client.getWorkspace() != null)
    {
      Client.getWorkspace().setCommonDirectory(directory.getAbsolutePath());
    }
  }
  
  @Override
  public void setDataTableDirectory(File directory)
  {
    if (Client.getWorkspace() != null)
    {
      Client.getWorkspace().setDataTableDirectory(directory.getAbsolutePath());
    }
  }
  
  @Override
  public void setPropertiesDirectory(File directory)
  {
    if (Client.getWorkspace() != null)
    {
      Client.getWorkspace().setPropertiesDirectory(directory.getAbsolutePath());
    }
  }
  
  @Override
  public void setDataDirectory(File directory)
  {
    if (Client.getWorkspace() != null)
    {
      Client.getWorkspace().setDataDirectory(directory.getAbsolutePath());
    }
  }
  
  @Override
  public void setFrame(Frame frame)
  {
    if (this.frame != null)
    {
      throw new IllegalStateException("Already initialized");
    }
    
    this.frame = frame;
  }
  
  @Override
  public Frame getFrame()
  {
    return frame;
  }
}
