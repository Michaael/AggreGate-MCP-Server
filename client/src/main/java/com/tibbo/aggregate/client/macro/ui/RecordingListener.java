package com.tibbo.aggregate.client.macro.ui;

import java.awt.*;
import java.util.*;

import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.action.executor.*;
import com.tibbo.aggregate.client.macro.*;
import com.tibbo.aggregate.client.macro.model.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.action.command.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.component.datatableeditor.*;
import com.tibbo.aggregate.component.propertieseditor.*;
import com.tibbo.aggregate.component.systemtree.*;

public class RecordingListener implements TableEditorListener, PropertiesEditorListener, ExecutorListener, SystemTreeListener
{
  private final MacroProcessor recorder;
  
  private final Map<GenericActionCommand, Operation> actionCommand2Operation = new HashMap();
  private final Map<ActionCommandExecutor, GenericActionCommand> executor2ActionCommand = new WeakHashMap();
  
  private SystemTreeEvent currentOperation;
  
  public RecordingListener(MacroProcessor recorder)
  {
    if (recorder == null)
    {
      throw new IllegalArgumentException("recorder is null");
    }
    
    this.recorder = recorder;
  }
  
  public void cellEditingStarted(TableEditorEvent e)
  {
  }
  
  public void cellEditingStopped(TableEditorEvent e)
  {
    TableEditorControllable tableEditor = e.getSource();
    EventContext ctx = getContext(tableEditor);
    Log.GUIDE.debug("Stopped editing " + e.getField() + "[" + e.getRow() + "] " + " = " + e.getValue() + " context: " + ctx);
    FieldFormat ff = tableEditor.getDataTable().getFormat(e.getField());
    recorder.doEditTableCell(ctx, e.getField(), e.getRow(), ff.valueToString(e.getValue()));
    // AtomicStep parent = recorder.getInsertionPoint();
    // fireStepInserted(op, parent);
  }
  
  public void editorHidden(TableEditorEvent e)
  {
    Log.GUIDE.debug("DTE: hidden");
  }
  
  public void editorShown(TableEditorEvent e)
  {
    Log.GUIDE.debug("DTE: shown");
    // AtomicStep parent = recorder.getInsertionPoint();
    // Step op = recorder.beginEditTable();
    // fireStepInserted(op, parent);
  }
  
  public void rowsAdded(TableEditorEvent e)
  {
    EventContext ctx = getContext(e.getSource());
    Log.GUIDE.debug("DTE: add rows " + toString(e.getSelection()) + " " + ctx);
    recorder.doAddTableRow(ctx);
    // AtomicStep parent = recorder.getInsertionPoint();
    // fireStepInserted(op, parent);
  }
  
  public void rowsMoved(TableEditorEvent e)
  {
    EventContext ctx = getContext(e.getSource());
    Log.GUIDE.debug("DTE: move rows " + toString(e.getSelection()) + " " + e.getShift() + " " + ctx);
    recorder.doMoveTableRow(ctx);
    // AtomicStep parent = recorder.getInsertionPoint();
    // fireStepInserted(op, parent);
  }
  
  public void rowsRemoved(TableEditorEvent e)
  {
    EventContext ctx = getContext(e.getSource());
    Log.GUIDE.debug("DTE: remove rows " + toString(e.getSelection()) + " " + ctx);
    recorder.doRemoveTableRow(ctx);
    // AtomicStep parent = recorder.getInsertionPoint();
    // fireStepInserted(op, parent);
  }
  
  public void dataChanged(TableEditorEvent e)
  {
  }
  
  public void closed(PropertiesEditorEvent e)
  {
    Log.GUIDE.debug("PE: closed");
  }
  
  public void editorHidden(PropertiesEditorEvent e)
  {
    Log.GUIDE.debug("PE: hidden");
  }
  
  public void editorShown(PropertiesEditorEvent e)
  {
    Log.GUIDE.debug("PE: shown");
  }
  
  public void error(PropertiesEditorEvent e)
  {
    Log.GUIDE.debug("PE: error");
  }
  
  public void loaded(PropertiesEditorEvent e)
  {
    Log.GUIDE.debug("PE: loaded");
  }
  
  public void saved(PropertiesEditorEvent e)
  {
    Log.GUIDE.debug("PE: saved");
  }
  
  private String toString(int[] arr)
  {
    StringBuffer sb = new StringBuffer();
    
    for (int i = 0; i < arr.length; i++)
    {
      sb.append(arr[i]);
      if (i < arr.length - 1)
      {
        sb.append(", ");
      }
    }
    
    return sb.toString();
  }
  
  public void commandReceived(ActionCommandExecutor source, Operation initiator, GenericActionCommand cmd)
  {
    Log.GUIDE.debug("actionCommand " + cmd.getType() + " from " + initiator.getDescription());
    
    checkCommand(source, initiator, cmd);
    
    executor2ActionCommand.put(source, cmd);
    actionCommand2Operation.put(cmd, initiator);
    
    String cmdId = cmd.getType();
    
    EventContext ctx = new EventContext(initiator, cmd);
    
    if (ActionUtils.CMD_EDIT_DATA.equals(cmdId))
    {
      recorder.beginEditTable(ctx);
    }
    else if (ActionUtils.CMD_EDIT_PROPERTIES.equals(cmdId))
    {
      if (EditPropertiesExecutor.getPropertiesGroup(cmd) != null)
      {
        recorder.beginEditProperties(ctx, EditPropertiesExecutor.getRemoteContextPath(cmd), EditPropertiesExecutor.getPropertiesGroup(cmd));
      }
      else
      {
        recorder.beginEditProperties(ctx, EditPropertiesExecutor.getRemoteContextPath(cmd), EditPropertiesExecutor.getProperties(cmd));
      }
    }
    else if (ActionUtils.CMD_SHOW_REPORT.equals(cmdId))
    {
      recorder.doReport(ctx);
    }
  }
  
  public void commandFulfilled(ActionCommandExecutor source, Operation initiator, GenericActionCommand cmd, GenericActionResponse resp)
  {
    Log.GUIDE.debug("actionCommand " + cmd.getType() + " from " + initiator.getDescription());
    
    checkCommand(source, initiator, cmd);
    
    executor2ActionCommand.remove(source);
    actionCommand2Operation.remove(cmd);
    
    EventContext ctx = new EventContext(initiator, cmd);
    
    String cmdId = cmd.getType();
    if (ActionUtils.CMD_EDIT_DATA.equals(cmdId))
    {
      if (resp != null)
      {
        DataTable dt = resp.getParameters();
        if (dt != null)
        {
          recorder.endEditTable(ctx);
        }
      }
    }
    else if (ActionUtils.CMD_EDIT_PROPERTIES.equals(cmdId))
    {
      if (resp != null)
      {
        String result = EditPropertiesResult.parse(resp).getCode();
        if (ActionUtils.RESPONSE_SAVED.equals(result))
        {
          if (EditPropertiesExecutor.getPropertiesGroup(cmd) != null)
          {
            recorder.endEditProperties(ctx, EditPropertiesExecutor.getRemoteContextPath(cmd), EditPropertiesExecutor.getPropertiesGroup(cmd));
          }
          else
          {
            recorder.endEditProperties(ctx, EditPropertiesExecutor.getRemoteContextPath(cmd), EditPropertiesExecutor.getProperties(cmd));
          }
        }
      }
    }
    else if (ActionUtils.CMD_CONFIRM.equals(cmdId))
    {
      ConfirmExecutor exec = (ConfirmExecutor) source;
      recorder.doConfirm(ctx, exec.getMessage(), exec.getOption());
    }
    else if (ActionUtils.CMD_SHOW_MESSAGE.equals(cmdId))
    {
      recorder.doMessage(ctx, cmd.getParameters().rec().getString(ShowMessage.CF_MESSAGE));
    }
    else if (ActionUtils.CMD_SHOW_ERROR.equals(cmdId))
    {
      recorder.doErrorMessage(ctx);
    }
  }
  
  public void operationInvoked(SystemTreeEvent e)
  {
    Log.GUIDE.debug("operation invoked " + e.getOperation().getDescription() + " from " + e.getInvokerContext());
    
    finishOperation(currentOperation);
    
    currentOperation = e;
    
    EventContext ctx = new EventContext(e.getOperation());
    
    recorder.beginOperation(ctx, e.getInvokerContext(), e.getOperation().getName());
  }
  
  public void dndPerformed(SystemTreeEvent e)
  {
    Log.GUIDE.debug("dnd performed " + e.getOperation().getDescription() + " from " + e.getInvokerContext());
    
    finishOperation(currentOperation);
    
    currentOperation = e;
    
    EventContext ctx = new EventContext(e.getOperation());
    
    recorder.beginOperation(ctx, e.getSourceContext(), e.getTargetContext(), e.getOperation().toString());
  }
  
  public void operationFinished(SystemTreeEvent e)
  {
    Log.GUIDE.debug("operation finished " + e.getOperation().getDescription() + " from " + e.getInvokerContext());
    
    finishOperation(e);
    
    currentOperation = null;
  }
  
  private void finishOperation(SystemTreeEvent e)
  {
    if (e == null)
    {
      return;
    }
    
    EventContext ctx = new EventContext(e.getOperation());
    
    if (e != null)
    {
      if (e.getSourceContext() == null)
      {
        recorder.endOperation(ctx, e.getInvokerContext(), e.getOperation().getName());
      }
      else
      {
        recorder.endOperation(ctx, e.getSourceContext(), e.getTargetContext(), e.getOperation().getName());
      }
    }
  }
  
  private void checkCommand(ActionCommandExecutor source, Operation initiator, ActionCommand cmd) throws IllegalArgumentException
  {
    if (source == null)
    {
      throw new IllegalArgumentException("source is null");
    }
    if (initiator == null)
    {
      throw new IllegalArgumentException("source is null");
    }
    if (cmd == null)
    {
      throw new IllegalArgumentException("source is null");
    }
  }
  
  private PropertiesEditor getPeByDte(DataTableEditor dte)
  {
    Container parent = dte.getParent();
    while (!(parent instanceof PropertiesEditor))
    {
      if (parent == null)
      {
        break;
      }
      parent = parent.getParent();
    }
    
    return (PropertiesEditor) parent;
  }
  
  private EventContext getContext(TableEditorControllable c)
  {
    if (!(c instanceof DataTableEditor))
    {
      return new EventContext(null, null);
    }
    DataTableEditor dte = (DataTableEditor) c;
    PropertiesEditor pe = getPeByDte(dte);
    
    GenericActionCommand cmd = null;
    for (Map.Entry<ActionCommandExecutor, GenericActionCommand> entry : executor2ActionCommand.entrySet())
    {
      if (entry.getKey() instanceof EditDataExecutor)
      {
        EditDataExecutor ede = (EditDataExecutor) entry.getKey();
        if (ede.getDataTableEditor() == dte)
        {
          cmd = executor2ActionCommand.get(ede);
          break;
        }
      }
      else if (entry.getKey() instanceof EditPropertiesExecutor)
      {
        EditPropertiesExecutor epe = (EditPropertiesExecutor) entry.getKey();
        if (epe.getEditor() == pe)
        {
          cmd = executor2ActionCommand.get(epe);
          break;
        }
      }
    }
    
    Operation op = actionCommand2Operation.get(cmd);
    
    String propertyName = null;
    if (pe != null)
    {
      propertyName = pe.getPropertyName(dte);
    }
    
    EventContext ctx = new EventContext(op, cmd, propertyName);
    return ctx;
  }
}
