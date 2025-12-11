package com.tibbo.aggregate.client.macro;

import com.tibbo.aggregate.client.macro.model.*;

public interface MacroProcessor
{
  public void beginEditProperties(EventContext eventContext, String remoteContext, String propertiesGroup);

  public void beginEditProperties(EventContext eventContext, String remoteContext, String[] properties);

  public EditTableOperation beginEditTable(EventContext ctx);

  public Macro beginMacro(Macro macro);

  public void beginOperation(EventContext ctx, String invokerContext, String operationName);

  public void beginOperation(EventContext ctx, String[] sourceContext, String targetContext, String operationName);

  public AddTableRowOperation doAddTableRow(EventContext ctx);

  public void doConfirm(EventContext eventContext, String message, int option);

  public EditTableCellOperation doEditTableCell(EventContext eventContext, String field, int row, String value);

  public void doErrorMessage(EventContext eventContext);

  public void doMessage(EventContext eventContext, String message);

  public MoveTableRowOperation doMoveTableRow(EventContext ctx);

  public RemoveTableRowOperation doRemoveTableRow(EventContext ctx);

  public void doReport(EventContext eventContext);

  public void endEditProperties(EventContext eventContext, String remoteContext, String[] properties);

  public void endEditProperties(EventContext eventContext, String remoteContext, String propertiesGroup);

  public void endEditTable(EventContext ctx);

  public void endMacro();

  public void endOperation(EventContext ctx, String invokerContext, String operationName);

  public void endOperation(EventContext ctx, String[] sourceContext, String targetContext, String operationName);
}
