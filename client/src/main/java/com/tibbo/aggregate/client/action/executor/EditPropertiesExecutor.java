package com.tibbo.aggregate.client.action.executor;

import java.util.*;
import java.util.concurrent.*;

import javax.swing.*;

import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.gui.*;
import com.tibbo.aggregate.client.gui.dialog.*;
import com.tibbo.aggregate.client.gui.frame.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.client.util.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.action.command.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.property.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.component.*;
import com.tibbo.aggregate.component.propertieseditor.*;

public class EditPropertiesExecutor extends AbstractCommandExecutor
{
  private static final String FRAME_PREFIX = "properties_";
  
  private boolean useDockableFrame;
  
  private PropertiesEditor editor;
  private InternalFrame frame;
  private PropertiesDialog propertiesDialog;
  private CountDownLatch sync;
  private String result;
  
  public EditPropertiesExecutor()
  {
    super(ActionUtils.CMD_EDIT_PROPERTIES);
  }
  
  @Override
  public GenericActionResponse execute(Operation originator, GenericActionCommand cmd)
  {
    if (!(originator instanceof InvokeActionOperation))
    {
      throw new IllegalArgumentException("Unsupported originator class: " + originator.getClass() + " required " + InvokeActionOperation.class.getName());
    }
    
    final InvokeActionOperation iop = (InvokeActionOperation) originator;
    
    final String title = cmd.getTitle();
    
    final UserSettings userSettings = iop.getSettings();
    
    DataRecord rec = cmd.getParameters().rec();
    
    final Context context = iop.getContext().get(getRemoteContextPath(cmd));
    
    if (context == null) // Context was destroyed or not available for some reason
    {
      return new GenericActionResponse(null);
    }
    
    boolean preferModalDialogs = Client.getWorkspace().isPreferModalDialogs();
    
    final boolean readOnly = rec.getBoolean(EditProperties.CF_READ_ONLY);
    final boolean dynamic = rec.getBoolean(EditProperties.CF_DYNAMIC);
    final boolean async = rec.getFormat().hasField(EditProperties.CF_ASYNC) ? rec.getBoolean(EditProperties.CF_ASYNC) : false;
    useDockableFrame = preferModalDialogs ? rec.getBoolean(EditProperties.CF_USE_DOCKABLE_FRAME) : true;
    final boolean singleWindowMode = rec.getBoolean(EditProperties.CF_SINGLE_WINDOW_MODE);
    
    DataTable locationData = rec.getDataTable(EditProperties.CF_LOCATION);
    final WindowLocation location = locationData != null ? new WindowLocation(locationData.rec()) : null;
    
    DataTable dashboardData = rec.hasField(EditProperties.CF_DASHBOARD) ? rec.getDataTable(EditProperties.CF_DASHBOARD) : null;
    final DashboardProperties dashboard = dashboardData != null ? new DashboardProperties(dashboardData.rec()) : null;
    
    DataTable dhInfoData = rec.hasField(EditProperties.CF_DASHBOARDS_HIERARCHY_INFO) ? rec.getDataTable(EditProperties.CF_DASHBOARDS_HIERARCHY_INFO) : null;
    final DashboardsHierarchyInfo dhInfo = dhInfoData != null ? new DashboardsHierarchyInfo(dhInfoData.rec()) : null;
    
    final String key = rec.getFormat().hasField(EditProperties.CF_KEY) ? rec.getString(EditProperties.CF_KEY) : null;
    
    final List<Context> slaves = EditProperties.getSlaves(cmd, context, null);
    final String propertiesGroup = getPropertiesGroup(cmd);
    final String[] properties = getProperties(cmd);
    
    context.getIconId(); // Will initialize context icon to prevent I/O from Swing thread
    
    iop.addInterruptionListener(interruptionListener);
    
    sync = new CountDownLatch(1);
    
    SwingUtilities.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        if (propertiesGroup != null)
        {
          editor = new PropertiesEditor(ComponentHelper.getMainFrame().getFrame(), null, context, slaves, userSettings, readOnly, singleWindowMode, dynamic, true, propertiesGroup);
        }
        else
        {
          editor = new PropertiesEditor(ComponentHelper.getMainFrame().getFrame(), null, context, slaves, userSettings, readOnly, singleWindowMode, dynamic, true, properties);
        }
        
        editor.getModel().setContextManager((iop != null && iop.getConnector() != null) ? iop.getConnector().getContextManager() : null);
        
        editor.getModel().setDefaultContext(context);
        
        editor.getModel().setConnector(iop != null ? iop.getConnector() : null);
        
        editor.addListener(listener);
        
        if (useDockableFrame)
        {
          editor.run();
          
          String frameKey = (key == null || key.isEmpty()) ? ExecutionHelper.createFrameKey(location, FRAME_PREFIX + ContextUtils.contextPathToContextName(context.getPath()) + "_"
              + (propertiesGroup != null ? propertiesGroup : StringUtils.print(properties, "_"))) : key;
          
          frame = InternalFrame.create(frameKey, title, editor, true, false, location, dashboard, Docs.CL_PROPERTIES_EDITOR, null, dhInfo);
          
          frame.addShutdownListener(new ShutdownListener()
          {
            @Override
            public void shutdown()
            {
              if (iop != null)
              {
                iop.removeInterruptionListener(interruptionListener);
                
              }
            }
          });
          
          editor.setOwnerFrame(frame);
        }
        else
        {
          propertiesDialog = new PropertiesDialog(ComponentHelper.getMainFrame().getFrame(), title, true, editor);
          propertiesDialog.run();
        }
      }
    });
    
    if (!async)
    {
      try
      {
        sync.await();
      }
      catch (InterruptedException ex)
      {
      }
    }
    
    DataTable resultTable = createResponseData(result);
    
    return new GenericActionResponse(resultTable, false, cmd.getRequestId());
  }
  
  public DataTable createResponseData(String result)
  {
    if (result == null)
    {
      result = ActionUtils.RESPONSE_CLOSED;
    }
    
    ActionUtils.checkResponseCode(result);
    
    DataRecord res = new DataRecord(EditProperties.RFT_EDIT_PROPERTIES);
    
    res.addString(result);
    
    DataTable propertiesTable = new SimpleDataTable(EditProperties.FT_PROPERTIES);
    if (editor != null)
    {
      for (String property : editor.getModel().getSavedProperties())
      {
        propertiesTable.addRecord().addString(property);
      }
    }
    res.addDataTable(propertiesTable);
    
    return res.wrap();
  }
  
  @Override
  public void cancel()
  {
    hideEditor();
  }
  
  public static String getRemoteContextPath(GenericActionCommand cmd)
  {
    return cmd.getParameters().rec().getString(EditProperties.CF_CONTEXT);
  }
  
  public static synchronized String getPropertiesGroup(GenericActionCommand cmd)
  {
    final DataRecord parameters = cmd.getParameters().rec();
    return parameters.getFormat().hasField(EditProperties.CF_PROPERTIES_GROUP) ? parameters.getString(EditProperties.CF_PROPERTIES_GROUP) : null;
  }
  
  private void hideEditor()
  {
    if (propertiesDialog != null)
    {
      propertiesDialog.dispose();
    }
    if (frame != null)
    {
      ClientUtils.removeFrame(frame);
    }
  }
  
  public static String[] getProperties(GenericActionCommand cmd)
  {
    DataTable params = cmd.getParameters();
    if (params.getFormat().hasField(EditProperties.CF_PROPERTIES))
    {
      DataTable data = params.rec().getDataTable(EditProperties.CF_PROPERTIES);
      LinkedList propertiesList = new LinkedList();
      for (DataRecord rec : data)
      {
        propertiesList.add(rec.getString(EditProperties.FIELD_PROPERTIES_PROPERTY));
      }
      return (String[]) propertiesList.toArray(new String[propertiesList.size()]);
    }
    return null;
  }
  
  private final PropertiesEditorListener listener = new PropertiesEditorListener()
  {
    @Override
    public void saved(PropertiesEditorEvent e)
    {
      result = ActionUtils.RESPONSE_SAVED;
      if (useDockableFrame)
      {
        // Do not continue processing when "Save" button pressed. Only "OK" will cause the action to proceed
        sync.countDown();
      }
    }
    
    @Override
    public void closed(PropertiesEditorEvent e)
    {
      editor.unlockIfLockedByThisUser();
      
      result = result == null ? ActionUtils.RESPONSE_CLOSED : result;
      if (useDockableFrame)
      {
        // Remove listener only if the editor is shown in common frame and CLOSED event won't be followed by HIDDEN (only for dialog)
        editor.removeListener(listener);
      }
      sync.countDown();
    }
    
    @Override
    public void loaded(PropertiesEditorEvent e)
    {
    }
    
    @Override
    public void editorShown(PropertiesEditorEvent e)
    {
    }
    
    @Override
    public void editorHidden(PropertiesEditorEvent e)
    {
    }
    
    @Override
    public void error(PropertiesEditorEvent e)
    {
      result = ActionUtils.RESPONSE_ERROR;
    }
  };
  
  public PropertiesManager getEditor()
  {
    return editor;
  }
  
  private final Runnable interruptionListener = new Runnable()
  {
    @Override
    public void run()
    {
      hideEditor();
    }
  };
}
