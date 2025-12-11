package com.tibbo.aggregate.client.action.executor;

import java.util.LinkedList;
import java.util.List;
import javax.swing.*;

import com.tibbo.aggregate.client.action.AbstractCommandExecutor;
import com.tibbo.aggregate.client.action.ExecutionHelper;
import com.tibbo.aggregate.client.gui.ShutdownListener;
import com.tibbo.aggregate.client.gui.dialog.DataTableEditorDialog;
import com.tibbo.aggregate.client.gui.dialog.OkCancelDialog;
import com.tibbo.aggregate.client.gui.frame.InternalFrame;
import com.tibbo.aggregate.client.operation.InvokeActionOperation;
import com.tibbo.aggregate.client.operation.Operation;
import com.tibbo.aggregate.client.util.ClientUtils;
import com.tibbo.aggregate.common.action.ActionUtils;
import com.tibbo.aggregate.common.action.GenericActionCommand;
import com.tibbo.aggregate.common.action.GenericActionResponse;
import com.tibbo.aggregate.common.action.command.EditData;
import com.tibbo.aggregate.common.component.datatableeditor.ClassDataTableEditorModel;
import com.tibbo.aggregate.common.component.datatableeditor.DataTableEditorModel;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.DataTableConversion;
import com.tibbo.aggregate.common.datatable.DataTableException;
import com.tibbo.aggregate.common.datatable.FieldFormat;
import com.tibbo.aggregate.common.datatable.ProxyDataTable;
import com.tibbo.aggregate.common.datatable.SimpleDataTable;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.common.util.AggreGateSwingWorker;
import com.tibbo.aggregate.common.util.DashboardProperties;
import com.tibbo.aggregate.common.util.DashboardsHierarchyInfo;
import com.tibbo.aggregate.common.util.Docs;
import com.tibbo.aggregate.common.util.StringUtils;
import com.tibbo.aggregate.common.util.UserSettings;
import com.tibbo.aggregate.common.util.Util;
import com.tibbo.aggregate.common.util.WindowLocation;
import com.tibbo.aggregate.common.view.ViewFilterElement;
import com.tibbo.aggregate.component.ComponentHelper;
import com.tibbo.aggregate.component.datatableeditor.ClassDataTableEditor;
import com.tibbo.aggregate.component.datatableeditor.DataTableEditor;
import org.apache.log4j.Level;

public class EditDataExecutor extends AbstractCommandExecutor
{
  private static final String FRAME_PREFIX = "data_";
  
  private InternalFrame frame;
  private DataTableEditorDialog dialog;
  private DataTableEditor dte;
  
  public EditDataExecutor()
  {
    super(ActionUtils.CMD_EDIT_DATA);
  }
  
  @Override
  public void cancel()
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        hideEditor();
      }
    });
  }
  
  @Override
  public GenericActionResponse execute(Operation originator, final GenericActionCommand cmd)
  {
    final InvokeActionOperation iop = (originator instanceof InvokeActionOperation) ? (InvokeActionOperation) originator : null;
    
    final UserSettings userSettings = iop != null ? iop.getSettings() : null;
    
    if (iop != null)
    {
      iop.addInterruptionListener(interruptionListener);
    }
    
    Object[] parameters;
    parameters = new AggreGateSwingWorker<Object[]>()
    {
      @Override
      public void run()
      {
        try
        {
          String title = cmd.getTitle();
          
          DataRecord rec = cmd.getParameters().rec();
          DataTable instance = rec.hasField(EditData.CF_STORAGE_INSTANCE) ? rec.getDataTable(EditData.CF_STORAGE_INSTANCE) : null;
          
          DataTable data = new SimpleDataTable();
          if (instance != null)
          {
            data = instance;
            TableFormat copy = data.getFormat().clone();
            for (FieldFormat field : copy.getFields())
            {
              field.removeGroup();
            }
            copy.setMaxRecords(1);
            copy.setMinRecords(1);
            data.setFormat(copy);
          }
          else
          {
            data = rec.getDataTable(EditData.CF_DATA);
          }
          
          Long sessionId = rec.hasField(EditData.CF_STORAGE_SESSION_ID) ? rec.getLong(EditData.CF_STORAGE_SESSION_ID) : null;
          Object instanceId = rec.hasField(EditData.CF_STORAGE_INSTANCE_ID) ? rec.getValue(EditData.CF_STORAGE_INSTANCE_ID) : null;
          boolean useDockableFrame = rec.getBoolean(EditData.CF_USE_DOCKABLE_FRAME);
          
          boolean readonly = rec.getBoolean(EditData.CF_READ_ONLY);
          
          String iconId = rec.getString(EditData.CF_ICON_ID);
          String helpId = rec.getString(EditData.CF_HELP_ID);
          String help = rec.getString(EditData.CF_HELP);
          
          String defaultContextPath = rec.getString(EditData.CF_DEFAULT_CONTEXT);
          
          Context defaultContext = null;
          if (iop != null)
          {
            if (defaultContextPath != null && iop.getConnector() != null && iop.getConnector().getContextManager() != null)
            {
              defaultContext = iop.getContext().get(defaultContextPath);
            }
            else
            {
              defaultContext = iop.getContext();
            }
          }
          
          DataTable locationData = rec.getDataTable(EditData.CF_LOCATION);
          final WindowLocation location = locationData != null ? new WindowLocation(locationData.rec()) : null;
          
          DataTable dashboardData = rec.hasField(EditData.CF_DASHBOARD) ? rec.getDataTable(EditData.CF_DASHBOARD) : null;
          final DashboardProperties dashboard = dashboardData != null ? new DashboardProperties(dashboardData.rec()) : null;
          
          DataTable dhInfoData = rec.hasField(EditData.CF_DASHBOARDS_HIERARCHY_INFO) ? rec.getDataTable(EditData.CF_DASHBOARDS_HIERARCHY_INFO) : null;
          final DashboardsHierarchyInfo dhInfo = dhInfoData != null ? new DashboardsHierarchyInfo(dhInfoData.rec()) : null;
          
          String key = rec.getFormat().hasField(EditData.CF_KEY) ? rec.getString(EditData.CF_KEY) : null;
          
          String expression = rec.getFormat().hasField(EditData.CF_EXPRESSION) ? rec.getString(EditData.CF_EXPRESSION) : null;
          Long period = rec.getFormat().hasField(EditData.CF_PERIOD) ? rec.getLong(EditData.CF_PERIOD) : null;
          Boolean enablePopupMenu = rec.getFormat().hasField(EditData.CF_ENABLE_POPUP_MENU) ? rec.getBoolean(EditData.CF_ENABLE_POPUP_MENU) : null;
          
          String storageContext = rec.getFormat().hasField(EditData.CF_STORAGE_CONTEXT) ? rec.getString(EditData.CF_STORAGE_CONTEXT) : null;
          String storageView = rec.getFormat().hasField(EditData.CF_STORAGE_VIEW) ? rec.getString(EditData.CF_STORAGE_VIEW) : null;
          String storageQuery = rec.getFormat().hasField(EditData.CF_STORAGE_QUERY) ? rec.getString(EditData.CF_STORAGE_QUERY) : null;
          String storageTable = rec.getFormat().hasField(EditData.CF_STORAGE_TABLE) ? rec.getString(EditData.CF_STORAGE_TABLE) : null;
          DataTable storageColumns = rec.getFormat().hasField(EditData.CF_STORAGE_COLUMNS) ? rec.getDataTable(EditData.CF_STORAGE_COLUMNS) : null;
          
          List<ViewFilterElement> storageFilter = new LinkedList<>();
          if (StringUtils.isEmpty(storageView) && rec.getFormat().hasField(EditData.CF_STORAGE_FILTER))
          {
            storageFilter = DataTableConversion.beansFromTable(rec.getDataTable(EditData.CF_STORAGE_FILTER), ViewFilterElement.class, ViewFilterElement.FORMAT);
          }
          
          DataTable storageSorting = rec.getFormat().hasField(EditData.CF_STORAGE_SORTING) ? rec.getDataTable(EditData.CF_STORAGE_SORTING) : null;
          DataTable storageBindings = rec.getFormat().hasField(EditData.CF_STORAGE_BINDINGS) ? rec.getDataTable(EditData.CF_STORAGE_BINDINGS) : null;
          
          boolean showToolbar = rec.getFormat().hasField(EditData.CF_SHOW_TOOLBAR) ? rec.getBoolean(EditData.CF_SHOW_TOOLBAR) : true;
          Boolean showHeader = rec.getFormat().hasField(EditData.CF_SHOW_HEADER) ? rec.getBoolean(EditData.CF_SHOW_HEADER) : null;
          Boolean showLineNumbers = rec.getFormat().hasField(EditData.CF_SHOW_LINE_NUMBERS) ? rec.getBoolean(EditData.CF_SHOW_LINE_NUMBERS) : null;
          Boolean horizontalScrolling = rec.getFormat().hasField(EditData.CF_HORIZONTAL_SCROLLING) ? rec.getBoolean(EditData.CF_HORIZONTAL_SCROLLING) : null;
          String relation = rec.getFormat().hasField(EditData.CF_RELATION_FIELD) ? rec.getString(EditData.CF_RELATION_FIELD) : null;
          
          String addRowTableAction = rec.hasField(EditData.CF_ADD_ROW_TABLE_ACTION) ? rec.getString(EditData.CF_ADD_ROW_TABLE_ACTION) : null;
          String addRowTableActionInput = rec.hasField(EditData.CF_ADD_ROW_TABLE_ACTION_INPUT) ? rec.getString(EditData.CF_ADD_ROW_TABLE_ACTION_INPUT) : null;
          boolean addRowTableActionShowResult = rec.hasField(EditData.CF_ADD_ROW_TABLE_SHOW_RESULT) ? rec.getBoolean(EditData.CF_ADD_ROW_TABLE_SHOW_RESULT) : true;
          
          String removeRowTableAction = rec.hasField(EditData.CF_REMOVE_ROW_TABLE_ACTION) ? rec.getString(EditData.CF_REMOVE_ROW_TABLE_ACTION) : null;
          String removeRowTableActionInput = rec.hasField(EditData.CF_REMOVE_ROW_TABLE_ACTION_INPUT) ? rec.getString(EditData.CF_REMOVE_ROW_TABLE_ACTION_INPUT) : null;
          boolean removeRowTableActionShowResult = rec.hasField(EditData.CF_REMOVE_ROW_TABLE_SHOW_RESULT) ? rec.getBoolean(EditData.CF_REMOVE_ROW_TABLE_SHOW_RESULT) : true;
          
          String updateRowTableAction = rec.hasField(EditData.CF_UPDATE_ROW_TABLE_ACTION) ? rec.getString(EditData.CF_UPDATE_ROW_TABLE_ACTION) : null;
          String updateRowTableActionInput = rec.hasField(EditData.CF_UPDATE_ROW_TABLE_ACTION_INPUT) ? rec.getString(EditData.CF_UPDATE_ROW_TABLE_ACTION_INPUT) : null;
          boolean updateRowTableActionShowResult = rec.hasField(EditData.CF_UPDATE_ROW_TABLE_SHOW_RESULT) ? rec.getBoolean(EditData.CF_UPDATE_ROW_TABLE_SHOW_RESULT) : true;
          
          Boolean editingInNewWindow = rec.getFormat().hasField(EditData.CF_EDITING_IN_NEW_WINDOW) ? rec.getBoolean(EditData.CF_EDITING_IN_NEW_WINDOW) : false;
          
          if (storageContext == null)
          {
            if (!data.isSimple() && data instanceof ProxyDataTable && ((ProxyDataTable) data).getFunctionContext() == null)
            {
              Context rootContext = iop.getConnector().getContextManager().getRoot();
              if (rootContext != null)
              {
                ((ProxyDataTable) data).setFunctionContext(rootContext);
              }
            }
            dte = new DataTableEditor(ComponentHelper.getMainFrame().getFrame(), null, data, userSettings, readonly, true, false, true, false);
          }
          else
          {
            dte = new ClassDataTableEditor(ComponentHelper.getMainFrame().getFrame(), null, data, userSettings, readonly, true, false, true, false);
            
            Context context = null;
            if (defaultContext != null && iop.getConnector() != null)
            {
              context = iop.getConnector().getContextManager().get(storageContext, defaultContext.getContextManager().getCallerController());
            }
            
            ((ClassDataTableEditorModel) dte.getModel()).initClassInstance(context, storageFilter, instance, instanceId, sessionId, relation);
          }
          
          DataTableEditorModel model = dte.getModel();
          
          boolean batchMember = cmd.isBatchEntry();
          
          model.setHelpText(help);
          model.setHelpId(helpId);
          dte.setIconId(iconId);
          model.setContext(defaultContext);
          model.setContextManager((iop != null && iop.getConnector() != null) ? iop.getConnector().getContextManager() : null);
          model.setConnector(iop != null ? iop.getConnector() : null);
          model.setDataExpression(expression);
          model.setDataExpressionEvaluationPeriod(period);
          
          // need for lifesycles transition datatables
          model.setStorageSessionId(sessionId);
          
          if (model.getContextManager() != null)
          {
            model.setStorageContext(model.getContextManager().get(storageContext, model.getCallerController()));
          }
          
          if (StringUtils.isEmpty(storageView))
          {
            model.setStorageQuery(storageQuery);
            model.setStorageColumns(storageColumns);
            model.setStorageBindings(storageBindings);
          }
          else
          {
            model.setStorageView(storageView);
          }
          
          model.setStorageTable(storageTable);
          model.setStorageFilter(storageFilter);
          model.setStorageSorting(storageSorting);
          
          model.setShowToolbar(showToolbar);
          model.setShowHeader(showHeader);
          model.setShowLineNumbers(showLineNumbers);
          model.setHorizontalScrolling(horizontalScrolling);
          model.setAddRowTableAction(addRowTableAction);
          model.setAddRowTableActionInput(addRowTableActionInput);
          model.setAddRowTableActionShowResult(addRowTableActionShowResult);
          model.setRemoveRowTableAction(removeRowTableAction);
          model.setRemoveRowTableActionInput(removeRowTableActionInput);
          model.setRemoveRowTableActionShowResult(removeRowTableActionShowResult);
          model.setUpdateRowTableAction(updateRowTableAction);
          model.setUpdateRowTableActionInput(updateRowTableActionInput);
          model.setUpdateRowTableActionShowResult(updateRowTableActionShowResult);
          model.setEditingInNewWindow(editingInNewWindow);
          
          dte.setEnablePopupMenu(enablePopupMenu != null ? enablePopupMenu : true);
          
          dte.start();
          
          if (useDockableFrame)
          {
            if (key == null)
            {
              key = ExecutionHelper.createFrameKey(location, FRAME_PREFIX + Util.descriptionToName(cmd.getTitle()));
            }
            
            frame = InternalFrame.create(key, title, dte, true, false, location, dashboard, Docs.CL_DATA_TABLE_EDITOR, iop.getConnector(), dhInfo);
            
            frame.addShutdownListener(new ShutdownListener()
            {
              @Override
              public void shutdown()
              {
                if (iop != null)
                {
                  model.getOriginal().close();
                  iop.removeInterruptionListener(interruptionListener);
                }
              }
            });
          }
          else
          {
            dialog = new DataTableEditorDialog(ComponentHelper.getMainFrame().getFrame(), title, true, batchMember && cmd.getRequestId() != null, dte);
            
            while (true)
            {
              dialog.run();
              
              data = dte.getDataTable();
              
              if (dialog.getSelectedOption() == OkCancelDialog.CANCEL_OPTION)
              {
                break;
              }
              
              try
              {
                data.validate(defaultContext, model.getContextManager(), model.getCallerController());
              }
              catch (Exception ex)
              {
                ClientUtils.showError(Level.INFO, ex);
                continue;
              }
              
              break;
            }
            
            if (dialog.getSelectedOption() == OkCancelDialog.OK_OPTION)
            {
              set(new Object[] { data, false });
            }
            else if (dialog.getSelectedOption() == OkCancelDialog.ALL_OPTION)
            {
              set(new Object[] { data, true });
            }
          }
        }
        catch (DataTableException ex)
        {
          throw new IllegalStateException(ex.getMessage(), ex);
        }
      }
    }.execute();
    
    if (parameters == null)
    {
      return null;
    }
    else
    {
      return new GenericActionResponse((DataTable) parameters[0], (Boolean) parameters[1], cmd.getRequestId());
    }
  }
  
  public static DataTable createResponseData(DataTable data)
  {
    return data;
  }
  
  public DataTableEditor getDataTableEditor()
  {
    return dte;
  }
  
  private void hideEditor()
  {
    if (dialog != null)
    {
      dialog.dispose();
    }
    
    if (frame != null)
    {
      ClientUtils.removeFrame(frame);
    }
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
