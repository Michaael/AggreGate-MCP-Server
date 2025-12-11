package com.tibbo.aggregate.client.macro.model;

import java.util.*;

import com.tibbo.aggregate.client.macro.*;
import com.tibbo.aggregate.client.macro.ui.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.action.*;

public class EventMacroRecorder implements MacroProcessor
{
  private Macro macro;
  private LinkedList<AtomicStep> steps = new LinkedList();
  private MacroTreeModel treeModel;
  
  public EventMacroRecorder()
  {
  }
  
  // public Macro beginMacro()
  // {
  // if(macro!=null)
  // {
  // throw new IllegalStateException("Macro already started");
  // }
  //
  // steps.clear();
  // macro = new Macro();
  // steps.add(macro);
  // treeModel = new MacroTreeModel(macro);
  // return macro;
  // }
  
  public Macro beginMacro(Macro macro)
  {
    if (this.macro != null)
    {
      throw new IllegalStateException("Macro already started");
    }
    
    this.macro = macro;
    steps.clear();
    steps.add(macro);
    treeModel = new MacroTreeModel(macro);
    return macro;
  }
  
  public void endMacro()
  {
    if (macro == null)
    {
      throw new IllegalStateException("No macro in progress");
    }
    steps.clear();
  }
  
  public void beginOperation(EventContext ctx, String invokerContext, String operationName)
  {
    InvokeOperationOperation op = new InvokeOperationOperation(invokerContext, operationName);
    addStep(op);
  }
  
  public void beginOperation(EventContext ctx, String[] sourceContext, String targetContext, String operationName)
  {
    InvokeOperationOperation op = new InvokeOperationOperation(sourceContext, targetContext, operationName);
    addStep(op);
  }
  
  public void endOperation(EventContext ctx, String invokerContext, String operationName)
  {
    removeStep(InvokeOperationOperation.class);
  }
  
  public void endOperation(EventContext ctx, String[] sourceContext, String targetContext, String operationName)
  {
    removeStep(InvokeOperationOperation.class);
  }
  
  public void beginEditProperties(EventContext ctx, String remoteContext, String propertiesGroup)
  {
    EditPropertiesOperation op = new EditPropertiesOperation(getRequestId(ctx), remoteContext, propertiesGroup);
    addStep(op);
  }
  
  private String getRequestId(EventContext ctx)
  {
    ActionCommand cmd = ctx.getActionCommand();
    if (cmd == null)
    {
      return null;
    }
    
    RequestIdentifier requestId = cmd.getRequestId();
    if (requestId == null)
    {
      return null;
    }
    
    return requestId.toString();
  }
  
  public void beginEditProperties(EventContext ctx, String remoteContext, String[] properties)
  {
    EditPropertiesOperation op = new EditPropertiesOperation(getRequestId(ctx), remoteContext, properties);
    addStep(op);
  }
  
  public void endEditProperties(EventContext ctx, String remoteContext, String[] properties)
  {
    removeStep(EditPropertiesOperation.class);
  }
  
  public void endEditProperties(EventContext ctx, String remoteContext, String propertiesGroup)
  {
    removeStep(EditPropertiesOperation.class);
  }
  
  public EditTableOperation beginEditTable(EventContext ctx)
  {
    EditTableOperation op = new EditTableOperation(getRequestId(ctx), ctx.getPropertyName());
    addStep(op);
    return op;
  }
  
  public void endEditTable(EventContext ctx)
  {
    removeStep(EditTableOperation.class);
  }
  
  public EditTableCellOperation doEditTableCell(EventContext ctx, String field, int row, String value)
  {
    EditTableCellOperation op = new EditTableCellOperation(getRequestId(ctx), field, row, value);
    addStep(op);
    return op;
  }
  
  public AddTableRowOperation doAddTableRow(EventContext ctx)
  {
    AddTableRowOperation op = new AddTableRowOperation();
    addStep(op);
    return op;
  }
  
  public RemoveTableRowOperation doRemoveTableRow(EventContext ctx)
  {
    RemoveTableRowOperation op = new RemoveTableRowOperation();
    addStep(op);
    return op;
  }
  
  public MoveTableRowOperation doMoveTableRow(EventContext ctx)
  {
    MoveTableRowOperation op = new MoveTableRowOperation();
    addStep(op);
    return op;
  }
  
  public void doConfirm(EventContext ctx, String message, int option)
  {
    ConfirmOperation op = new ConfirmOperation(getRequestId(ctx), message, option);
    addStep(op);
  }
  
  public void doMessage(EventContext ctx, String message)
  {
    MessageOperation op = new MessageOperation(getRequestId(ctx), message);
    addStep(op);
  }
  
  public void doReport(EventContext ctx)
  {
    ReportOperation op = new ReportOperation();
    addStep(op);
  }
  
  public void doErrorMessage(EventContext ctx)
  {
    ErrorMessageOperation op = new ErrorMessageOperation();
    addStep(op);
  }
  
  public AtomicStep getCurrentStep()
  {
    return steps.getLast();
  }
  
  public AtomicStep getInsertionPoint()
  {
    return getCurrentStep();
  }
  
  public List<AtomicStep> getSteps()
  {
    return new LinkedList<AtomicStep>(steps);
  }
  
  public List<AtomicStep> getInsertionPath()
  {
    return getSteps();
  }
  
  protected void addStep(Step step)
  {
    Step currentStep = getCurrentStep();
    if (!(currentStep instanceof AtomicStep))
    {
      throw new IllegalStateException("No target to add step to");
    }
    AtomicStep atomicStep = (AtomicStep) currentStep;
    
    List path = getInsertionPath();
    
    // Here is supposed that atomicStep filters step classes by itself
    atomicStep.addStep(step);
    
    if (treeModel != null)
    {
      treeModel.fireTreeNodesInserted(treeModel, path.toArray(), new int[] { atomicStep.getStepCount() - 1 }, new Object[] { step });
    }
    
    if (step instanceof AtomicStep)
    {
      steps.add((AtomicStep) step);
    }
  }
  
  protected void removeStep(Class stepClass)
  {
    Step currentStep = getCurrentStep();
    if (currentStep == null || currentStep.getClass() != stepClass)
    {
      Log.GUIDE.warn("No steps of class on queue: " + stepClass.getName());
      return;
    }
    
    steps.removeLast();
  }
  
  public MacroTreeModel getTreeModel()
  {
    return treeModel;
  }
  
  public Macro getMacro()
  {
    return macro;
  }
}
