package com.tibbo.aggregate.client.action.executor;

import java.awt.event.*;

import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.gui.dialog.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.action.command.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.component.*;
import com.tibbo.aggregate.component.codeeditor.*;

public class EditCodeExecutor extends AbstractCommandExecutor
{
  public EditCodeExecutor()
  {
    super(ActionUtils.CMD_EDIT_CODE);
  }
  
  public GenericActionResponse execute(Operation originator, GenericActionCommand cmd)
  {
    DataTable params = cmd.getParameters();
    String code = params.rec().getString(EditCode.CF_CODE);
    String mode = params.rec().getString(EditCode.CF_MODE);
    final CodeEditorPanel cep = CodeEditorFactory.createEditor(code, mode, originator.getContext());
    cep.getEditor().addKeyListener(new KeyAdapter()
    {
      @Override
      public void keyReleased(KeyEvent e)
      {
        if (cep.getEditor().getCodeInspectorCount() != 0 && (e.getKeyCode() == KeyEvent.VK_SEMICOLON || e.getKeyCode() == KeyEvent.VK_ENTER))
        {
          cep.getEditor().getCodeInspector(0).inspect(cep.getEditor(), null, cep.getEditor().getMarkerModel());
        }
      }
    });
    
    OkCancelDialog dialog = new OkCancelDialog(ComponentHelper.getMainFrame().getFrame(), cmd.getTitle(), true, true);
    
    dialog.setMainComponent(cep);
    
    int result = dialog.run();
    
    return createResponseData(cmd, result == OkCancelDialog.OK_OPTION ? ActionUtils.RESPONSE_SAVED : ActionUtils.RESPONSE_CLOSED, cep.getEditor().getText());
  }
  
  public static GenericActionResponse createResponseData(GenericActionCommand cmd, String result, String code)
  {
    // String result is [ActionUtils.RESP_SAVED, ActionUtils.RESP_CLOSED, ActionUtils.RESP_ERROR]
    ActionUtils.checkResponseCode(result);
    
    DataTable resultTable = new SimpleDataTable(EditCode.RFT_EDIT_CODE);
    resultTable.addRecord().addString(result).addString(code);
    
    return new GenericActionResponse(resultTable, false, cmd.getRequestId());
  }
}
