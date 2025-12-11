package com.tibbo.aggregate.client.action.executor;

import java.awt.*;

import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.gui.dialog.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.action.command.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.component.*;
import com.tibbo.aggregate.component.texteditor.*;

public class EditTextExecutor extends AbstractCommandExecutor
{
  public EditTextExecutor()
  {
    super(ActionUtils.CMD_EDIT_TEXT);
  }
  
  public GenericActionResponse execute(Operation originator, GenericActionCommand cmd)
  {
    DataTable params = cmd.getParameters();
    String text = params.rec().getString(EditText.CF_TEXT);
    String mode = params.rec().getString(EditText.CF_MODE);
    
    TextEditor editor = new TextEditor(text, mode);
    
    editor.setPreferredSize(new Dimension(900, 500));
    
    OkCancelDialog dialog = new OkCancelDialog(ComponentHelper.getMainFrame().getFrame(), cmd.getTitle(), true, true);
    
    dialog.setMainComponent(editor);
    
    int result = dialog.run();
    
    return createResponseData(cmd, result == OkCancelDialog.OK_OPTION ? ActionUtils.RESPONSE_SAVED : ActionUtils.RESPONSE_CLOSED, editor.getText());
  }
  
  public static GenericActionResponse createResponseData(GenericActionCommand cmd, String result, String code)
  {
    // String result is [ActionUtils.RESP_SAVED, ActionUtils.RESP_CLOSED, ActionUtils.RESP_ERROR]
    ActionUtils.checkResponseCode(result);
    
    DataTable resultTable = new SimpleDataTable(EditText.RFT_EDIT_TEXT);
    resultTable.addRecord().addString(result).addString(code);
    
    return new GenericActionResponse(resultTable, false, cmd.getRequestId());
  }
}
