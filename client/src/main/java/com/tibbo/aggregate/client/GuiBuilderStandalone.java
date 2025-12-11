package com.tibbo.aggregate.client;

import static com.tibbo.aggregate.common.server.WidgetContextConstants.*;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import javax.swing.*;

import org.apache.commons.codec.binary.Base64;

import com.tibbo.aggregate.client.gui.dialog.*;
import com.tibbo.aggregate.client.guibuilder.*;
import com.tibbo.aggregate.client.guibuilder.workflow.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.datatable.field.*;
import com.tibbo.aggregate.common.datatable.validator.*;
import com.tibbo.aggregate.common.device.*;
import com.tibbo.aggregate.common.protocol.*;
import com.tibbo.aggregate.common.resource.*;
import com.tibbo.aggregate.common.server.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.common.widget.*;
import com.tibbo.aggregate.common.widget.component.*;
import com.tibbo.aggregate.common.widget.context.*;
import com.tibbo.aggregate.common.widget.runtime.util.*;
import com.tibbo.aggregate.component.*;
import com.tibbo.aggregate.component.datatableeditor.*;
import com.tibbo.aggregate.resource.*;

public class GuiBuilderStandalone
{
  private static final String V_CHILD_INFO = "childInfo";
  private static final String V_TEMPLATE = "template";
  
  private static final String V_EDIT_TYPE = "editType";
  private static final String V_WIDGET = "widget";
  private static final String V_WORKFLOW = "workflow";
  
  static
  {
    System.setProperty("log4j.configurationFile", new File(System.getProperty("user.dir") + File.separator + Log.CLIENT_LOGGING_CONFIG_FILENAME).toURI().toString());
  }
  
  private static final TableFormat VFT_LOGIN_FORMAT = new TableFormat(1, 1);
  
  static
  {
    VFT_LOGIN_FORMAT.addField(FieldFormat.create(AG_BUILDER_USERNAME, FieldFormat.STRING_FIELD, Cres.get().getString("username")));
    VFT_LOGIN_FORMAT.addField(FieldFormat.create(AG_BUILDER_PASSWORD, FieldFormat.STRING_FIELD, Cres.get().getString("password")).setEditor(StringFieldFormat.EDITOR_PASSWORD));
    VFT_LOGIN_FORMAT.addField(FieldFormat.create(AG_BUILDER_ADDRESS, FieldFormat.STRING_FIELD, Cres.get().getString("ipOrHost")));
    VFT_LOGIN_FORMAT.addField(FieldFormat.create(AG_BUILDER_PORT, FieldFormat.INTEGER_FIELD, Cres.get().getString("port")).addValidator(ValidatorHelper.PORT_VALIDATOR));
    VFT_LOGIN_FORMAT.addField(FieldFormat.create(AG_BUILDER_CONTEXT_TEMPLATE, FieldFormat.STRING_FIELD, Cres.get().getString("contextTemplate")));
    VFT_LOGIN_FORMAT.addField(FieldFormat.create(AG_BUILDER_DEFAULT_CONTEXT, FieldFormat.STRING_FIELD, Cres.get().getString("conDefaultContext")));
  }
  
  private static final TableFormat VFT_STANDALONE_FORMAT = new TableFormat(1, 1);
  
  static
  {
    VFT_STANDALONE_FORMAT.addField(FieldFormat.create(V_EDIT_TYPE, FieldFormat.STRING_FIELD, Cres.get().getString("editType"))
        .addSelectionValue(V_WORKFLOW, Cres.get().getString("workflow")).addSelectionValue(V_WIDGET, Cres.get().getString("widget")).setDefault(V_WIDGET));
  }
  
  private String templateContextStr = null;
  private String defaultContextStr = null;
  private DataTable loginTable = new DataRecord(VFT_LOGIN_FORMAT).wrap();
  private static GuiBuilderStandalone instance = new GuiBuilderStandalone();
  
  public static void main(String[] args)
  {
    instance.launch(args);
  }
  
  private void launch(String[] args)
  {
    try
    {
      Log.start();
      
      ComponentHelper.initialStartup();
      
      ClientHelper.registerExecutors();
      
      SwingUtilities.invokeAndWait(() -> {
        try
        {
          UIHelper.setup();
        }
        catch (Exception ex)
        {
          Log.GUIBUILDER.warn(ex.getMessage(), ex);
        }
      });
      try
      {
        ResourceManager.initialize(ComponentHelper.getCustomizationUrl(), Resource.class);
      }
      catch (Throwable throwable)
      {
        Log.GUIBUILDER.warn(throwable.getMessage(), throwable);
      }
      RemoteServer server = null;
      
      boolean confirmMode = false;
      ContextManager cm = null;
      GuiBuilderController rlc = null;
      Context templateContext = null;
      Context defaultContext = null;
      String templateStr = null;
      String name = null;
      
      String error = null;
      do
      {
        try
        {
          if (server == null && args.length > 0 && args[0].startsWith(WidgetContextConstants.AG_BUILDER_PROTOCOL))
          {
            server = parseInputArgs(args);
          }
          else
          {
            server = showRemoteServerTable(error);
            if (server == null)
            {
              cm = null;
              break;
            }
          }
          error = null;
          
          rlc = getGuiBuilderController(rlc, server);
          
          rlc.connect();
          rlc.sendCommand(rlc.getCommandBuilder().startMessage());
          rlc.login();
          
          cm = rlc.getContextManager();
          CallerController callerController = new UncheckedCallerController();
          
          if (this.templateContextStr != null)
          {
            templateContext = cm.get(this.templateContextStr, callerController);
            if (templateContext != null)
            {
              name = templateContext.getName();
              DataTable widgetTemplate = templateContext.getVariable(V_CHILD_INFO, callerController);
              if (widgetTemplate != null)
              {
                if (widgetTemplate.hasField(V_TEMPLATE))
                {
                  templateStr = widgetTemplate.rec().getString(V_TEMPLATE);
                  templateContext.callFunction(InstallableContextConstants.F_EXECUTE_LOCK, callerController);
                }
                else
                {
                  throw new ContextException(MessageFormat.format(Cres.get().getString("uiBuilderNotSupport"), templateContext.getPath()));
                }
              }
            }
            else
            {
              throw new ContextException(Cres.get().getString("conNotAvail") + this.templateContextStr);
            }
          }
          if (defaultContextStr != null)
          {
            defaultContext = cm.get(defaultContextStr, callerController);
          }
          confirmMode = true;
        }
        catch (Exception e)
        {
          confirmMode = false;
          error = e.getMessage();
        }
      }
      while (error != null);
      
      if (cm == null)
      {
        name = null;
        final DefaultWidgetTemplate widget = new DefaultWidgetTemplate();
        final WRootPanelContext<WRootPanel> stubContext = new WRootPanelContext<>(widget.getRootPanel(), widget);
        cm = new DefaultContextManager<>(stubContext, false);
        rlc = getGuiBuilderController(rlc, null);
      }
      
      AbstractAggreGateIDE guiBuilder;
      String templateContextType = templateContext != null ? templateContext.getType() : showStandAloneTable();
      if (templateContextType.equals(V_WORKFLOW))
      {
        guiBuilder = new WorkflowIDE(
                Cres.get().getString("uiBuilder"),
                cm,
                templateContext,
                defaultContext,
                confirmMode,
                null,
                rlc,
                ComponentHelper.getIoThreadPool(),
                Client.getParameters().getUiBuilderUndoLimit());
        guiBuilder.setStandAlone(true);
      }
      else
      {
        guiBuilder = new WidgetBuilder(
                name,
                cm,
                templateContext,
                defaultContext != null ? defaultContext : cm.getRoot(),
                null,
                rlc,
                ComponentHelper.getIoThreadPool(),
                Client.getParameters().getUiBuilderUndoLimit(),
                true,
                confirmMode);
      }
      
      if (rlc != null)
      {
        rlc.startConnectionTimerTask(!name.isEmpty() ? name : String.valueOf(hashCode()));
      }
      
      try
      {
        DefaultWidgetTemplate template = null;
        if (templateStr != null)
          template = WidgetExport.importWidget(templateStr, null, null);
        
        if (template != null)
          guiBuilder.openTemplate(template, true);
      }
      catch (Exception e)
      {
        Log.GUIBUILDER.warn(e.getMessage(), e);
      }
      
      guiBuilder.start();
    }
    catch (Exception ex)
    {
      Log.GUIBUILDER.warn(ex.getMessage(), ex);
    }
  }
  
  private GuiBuilderController getGuiBuilderController(GuiBuilderController controller, RemoteServer server)
  {
    if (controller != null && controller.isConnected())
    {
      try
      {
        controller.disconnect();
      }
      catch (IOException | InterruptedException | RemoteDeviceErrorException e)
      {
        Log.GUIBUILDER.warn(e.getMessage(), e);
      }
    }
    if (server != null)
      return new GuiBuilderController(server);
    else
      return null;
  }
  
  private String showStandAloneTable()
  {
    DataTable standaloneTable = new SimpleDataTable(VFT_STANDALONE_FORMAT);
    DataTableEditor dte = new DataTableEditor(null, null, standaloneTable, false);
    DataTableEditorDialog dialog = new DataTableEditorDialog(null, Cres.get().getString("properties"), true, false, dte);
    dialog.setIconImage(ResourceManager.getImageIcon(OtherIcons.CLIENT_16).getImage());
    
    String result = "";
    if (dialog.run() == OkCancelDialog.OK_OPTION)
    {
      result = dialog.getDataTableEditor().getDataTable().rec().getString(V_EDIT_TYPE);
    }
    else
    {
      System.exit(0);
    }
    return result;
    
  }
  
  private RemoteServer showRemoteServerTable(String error)
  {
    RemoteServer server = new RemoteServer();
    DataTableEditor dte = new DataTableEditor(null, null, loginTable, false);
    DataTableEditorDialog dialog = new DataTableEditorDialog(null, error == null ? Cres.get().getString("connectionProperties") : error, true, false, dte);
    dialog.setIconImage(ResourceManager.getImageIcon(OtherIcons.CLIENT_16).getImage());
    
    JButton standAloneButton = new JButton(Cres.get().getString("launchUiBuilder"));
    AtomicBoolean runStanAlone = new AtomicBoolean(false);
    standAloneButton.addActionListener(e1 -> {
      runStanAlone.set(true);
      dialog.onOk();
    });
    
    dialog.addButton(standAloneButton);
    if (dialog.run() == OkCancelDialog.OK_OPTION)
    {
      if (!runStanAlone.get())
      {
        loginTable = dialog.getDataTableEditor().getDataTable();
        server.setUsername(loginTable.rec().getString(AG_BUILDER_USERNAME));
        server.setPassword(loginTable.rec().getString(AG_BUILDER_PASSWORD));
        server.setAddress(loginTable.rec().getString(AG_BUILDER_ADDRESS));
        server.setPort(loginTable.rec().getInt(AG_BUILDER_PORT));
        templateContextStr = loginTable.rec().getString(AG_BUILDER_CONTEXT_TEMPLATE);
        defaultContextStr = loginTable.rec().getString(AG_BUILDER_DEFAULT_CONTEXT);
      }
      else
        return null;
    }
    else
    {
      System.exit(0);
    }
    return server;
  }
  
  private RemoteServer parseInputArgs(String[] args)
  {
    String PROPERTIES_VALUE_SEPARATOR = "=";
    int PROPERTIES_DEFAULT_COUNT = 2;
    int PROPERTY_VALUE_INDEX = 1;
    String PROPERTIES_HTML_SEPARATOR = "&";
    String PROPERTIES_COMMA_SEPARATOR = ",";
    
    RemoteServer server = new RemoteServer();
    String propertiesStr = args[0].substring(WidgetContextConstants.AG_BUILDER_PROTOCOL.length());
    
    List<String> propertiesArray = new ArrayList<>();
    if (propertiesStr.contains(PROPERTIES_HTML_SEPARATOR))
      propertiesArray = Arrays.asList(propertiesStr.split(PROPERTIES_HTML_SEPARATOR));
    else
      propertiesArray.add(propertiesStr);
    
    for (String property : propertiesArray)
    {
      String[] connectionConfiguration = new String(Base64.decodeBase64(property)).split(PROPERTIES_COMMA_SEPARATOR);
      for (String config : connectionConfiguration)
      {
        if (config.startsWith(AG_BUILDER_USERNAME))
        {
          String[] usernameConfig = config.split(PROPERTIES_VALUE_SEPARATOR);
          if (usernameConfig.length == PROPERTIES_DEFAULT_COUNT)
          {
            server.setUsername(usernameConfig[PROPERTY_VALUE_INDEX]);
            loginTable.rec().setValue(AG_BUILDER_USERNAME, server.getUsername());
          }
        }
        else if (config.startsWith(WidgetContextConstants.AG_BUILDER_PASSWORD))
        {
          String[] passwordConfig = config.split(PROPERTIES_VALUE_SEPARATOR);
          if (passwordConfig.length == PROPERTIES_DEFAULT_COUNT)
          {
            server.setPassword(passwordConfig[PROPERTY_VALUE_INDEX]);
          }
        }
        else if (config.startsWith(WidgetContextConstants.AG_BUILDER_ADDRESS))
        {
          String[] addressConfig = config.split(PROPERTIES_VALUE_SEPARATOR);
          if (addressConfig.length == PROPERTIES_DEFAULT_COUNT)
          {
            server.setAddress(addressConfig[PROPERTY_VALUE_INDEX]);
          }
          loginTable.rec().setValue(AG_BUILDER_ADDRESS, server.getAddress());
        }
        else if (config.startsWith(WidgetContextConstants.AG_BUILDER_PORT))
        {
          String[] portConfig = config.split(PROPERTIES_VALUE_SEPARATOR);
          if (portConfig.length == PROPERTIES_DEFAULT_COUNT)
          {
            server.setPort(Integer.parseInt(portConfig[1]));
          }
          
          loginTable.rec().setValue(AG_BUILDER_PORT, server.getPort());
        }
        else if (config.startsWith(WidgetContextConstants.AG_BUILDER_CONTEXT_TEMPLATE))
        {
          String[] templateConfig = config.split(PROPERTIES_VALUE_SEPARATOR);
          if (templateConfig.length == PROPERTIES_DEFAULT_COUNT)
          {
            templateContextStr = templateConfig[PROPERTY_VALUE_INDEX];
            loginTable.rec().setValue(AG_BUILDER_CONTEXT_TEMPLATE, templateContextStr);
          }
        }
        else if (config.startsWith(WidgetContextConstants.AG_BUILDER_DEFAULT_CONTEXT))
        {
          String[] defaultConfig = config.split(PROPERTIES_VALUE_SEPARATOR);
          if (defaultConfig.length == PROPERTIES_DEFAULT_COUNT)
          {
            defaultContextStr = defaultConfig[PROPERTY_VALUE_INDEX];
            loginTable.rec().setValue(AG_BUILDER_DEFAULT_CONTEXT, defaultContextStr);
          }
        }
      }
    }
    return server;
  }
}
