package com.tibbo.aggregate.client;

import java.io.*;
import java.net.*;
import java.text.*;
import javax.jnlp.*;

import com.beust.jcommander.*;
import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.action.executor.*;
import com.tibbo.aggregate.client.util.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.expression.*;
import com.tibbo.aggregate.common.resource.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.component.*;
import com.tibbo.aggregate.resource.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.*;

public class ClientHelper
{

  private static final String CLIENT_CLASS_NAME = Client.class.getCanonicalName();

  static void init(String[] args, ClientCommandLineParameters parameters, URI customizationFile)
  {
    try
    {
      initVersion();

      ComponentHelper.initialStartup();
      
      parseCommandLine(args, parameters);
      
      if (ComponentHelper.isDebug())
      {
        ComponentHelper.getConfig().setDataDirectory(ComponentHelper.getConfig().getHomeDirectory());
      }
      
      initLogging(parameters);

      Log.CLIENTS.info("Data directory: " + ComponentHelper.getConfig().getDataDirectory());
      
      if (customizationFile == null)
      {
        if (parameters.isKiosk())
        {
          final BasicService bs = (BasicService) ServiceManager.lookup("javax.jnlp.BasicService");
          customizationFile = new URL(bs.getCodeBase().toExternalForm() + ResourceManager.CLIENT_CUSTOMIZATION_RESOURCES_FILENAME).toURI();
        }
        else
        {
          customizationFile = ComponentHelper.getCustomizationUrl();
        }
      }
      
      startResourcesManager(customizationFile);
      
      registerExecutors();
      
      loadDependencies();
    }
    catch (Exception ex)
    {
      showStartupErrorAndExit(ex);
    }
  }

  private static void initVersion()
  {
    SoftwareVersion.initVersion(CLIENT_CLASS_NAME);
  }

  static void parseCommandLine(String[] args, ClientCommandLineParameters parameters) throws AggreGateException
  {
    final JCommander jc = new JCommander(parameters, Pres.get());
    jc.setProgramName(Cres.get().getString("clpExecutableName"));
    try
    {
      jc.parse(args);
    }
    catch (Exception e)
    {
      throwAGExceptionWithUsage(jc, e.getMessage());
    }
    
    if (parameters.isAdminMode())
    {
      ComponentHelper.setPerms(ClientPermissionChecker.ADMIN);
    }
    
    ComponentHelper.setCreateWorkspace(parameters.isCreateWorkspace());
    ComponentHelper.setAutoLaunchReference(parameters.getAutoLaunchReference() != null ? new Reference(parameters.getAutoLaunchReference()) : null);
    ComponentHelper.setSimpleMode(parameters.isSimpleMode());
    ComponentHelper.setSteadyStateMode(parameters.isSteadyStateMode());
    ComponentHelper.setDebug(parameters.isDebug());
    ComponentHelper.setAutoConnect(parameters.getAutoConnect());
    
    if (!StringUtils.isEmpty(parameters.getUserName()) && !ComponentHelper.isReloginFlag())
    {
      ComponentHelper.setUsername(parameters.getUserName());
    }
    
    if (!StringUtils.isEmpty(parameters.getPassword()) && !ComponentHelper.isReloginFlag())
    {
      ComponentHelper.setPassword(parameters.getPassword());
    }
  }
  
  private static void throwAGExceptionWithUsage(JCommander jc, String initialMessage) throws AggreGateException
  {
    final StringBuilder out = new StringBuilder(initialMessage).append('\n');
    
    jc.usage(out);
    
    throw new AggreGateException(CommandLineHelper.jCommanderUsageI18N(out.toString()));
  }
  
  static void initLogging(ClientCommandLineParameters parameters)
  {
    if (parameters.isRemote() || parameters.isKerberos())
    {
      Log.start(Client.class.getResource(Log.CLIENT_LOGGING_CONFIG_FILENAME));
    }
    else
    {
      Log.start(ComponentHelper.getConfig().getHomeDirectory(), Log.CLIENT_LOGGING_CONFIG_FILENAME);
    }
    
    Log.CORE.info("Starting " + Cres.get().getString("productClient") + " v" + SoftwareVersion.getCurrentVersionAndBuild());
    
    try
    {
      Log.CORE.debug("Redirecting stdout and stderr to default logging facility");
      
      PrintStream ps = new PrintStream(System.out)
      {
        @Override
        public void println(String s)
        {
          Util.logWithSourceCodeLine(Log.STDOUT, Level.DEBUG, s);
        }
        
        @Override
        public void print(String s)
        {
          Util.logWithSourceCodeLine(Log.STDOUT, Level.DEBUG, s);
        }
        
        @Override
        public void print(Object o)
        {
          Util.logWithSourceCodeLine(Log.STDOUT, Level.DEBUG, o);
        }
        
        @Override
        public void println(Object o)
        {
          Util.logWithSourceCodeLine(Log.STDOUT, Level.DEBUG, o);
        }
      };
      System.setOut(ps);
      
      ps = new PrintStream(System.err)
      {
        @Override
        public void println(String s)
        {
          Util.logWithSourceCodeLine(Log.STDERR, Level.INFO, s);
        }
        
        @Override
        public void print(String s)
        {
          Util.logWithSourceCodeLine(Log.STDERR, Level.INFO, s);
        }
        
        @Override
        public void print(Object o)
        {
          Util.logWithSourceCodeLine(Log.STDERR, Level.INFO, o);
        }
        
        @Override
        public void println(Object o)
        {
          Util.logWithSourceCodeLine(Log.STDERR, Level.INFO, o);
        }
      };
      System.setErr(ps);
    }
    catch (Exception ex1)
    {
      Log.CORE.error("Error configuring stdout/stderr redirection: ", ex1);
    }
  }
  
  static void startResourcesManager(URI customizationUrl)
  {
    ResourceManager.initialize(customizationUrl, Resource.class);
  }
  
  static void loadDependencies()
  {
    try
    {
      ClassPathHelper.addToLibraryPath(ComponentHelper.getConfig().getHomeDirectory() + Constants.LIBRARIES_SUBDIR);
      ClassPathHelper.loadJars(new File(ComponentHelper.getConfig().getHomeDirectory() + Constants.LIBRARIES_SUBDIR));
    }
    catch (Exception ex)
    {
      Log.CORE.error("Error loading additional libraries", ex);
    }
  }
  
  public static void registerExecutors()
  {
    ExecutionHelper.registerExecutor(ActionUtils.CMD_SHOW_MESSAGE, ShowMessageExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_CONFIRM, ConfirmExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_SHOW_ERROR, ShowErrorExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_EDIT_DATA, EditDataExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_EDIT_PROPERTIES, EditPropertiesExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_BROWSE, BrowseExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_SHOW_REPORT, ShowReportExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_SHOW_EVENT_LOG, ShowEventLogExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_SELECT_ENTITIES, SelectEntitiesExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_EDIT_TEXT, EditTextExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_EDIT_CODE, EditCodeExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_SHOW_SYSTEM_TREE, ShowSystemTreeExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_ACTIVATE_DASHBOARD, ActivateDashboardExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_CLOSE_DASHBOARD, CloseDashboardExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_OPEN_GRID_DASHBOARD, OpenGridDashboardExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_SHOW_HTML_SNIPPET, ShowHtmlSnippetExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_EDIT_WIDGET, EditWidgetExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_LAUNCH_WIDGET, LaunchWidgetExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_SHOW_GUIDE, ShowGuideExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_EDIT_REPORT, EditReportExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_SHOW_DIFF, ShowDiffExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_LAUNCH_PROCESS_CONTROL_PROGRAM, LaunchProcessControlProgramExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_EDIT_PROCESS_CONTROL_PROGRAM, EditProcessControlProgramExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_EDIT_WORKFLOW, EditWorkflowExecutor.class);
    ExecutionHelper.registerExecutor(ActionUtils.CMD_EDIT_EXPRESSION, EditExpressionExecutor.class);
  }
  
  static void showStartupErrorAndExit(Throwable th)
  {
    ClientUtils.showError(Level.FATAL, null, MessageFormat.format(Pres.get().getString("mErrStarting"), Cres.get().getString("productClient")), th);
    System.exit(0);
  }
  
}
