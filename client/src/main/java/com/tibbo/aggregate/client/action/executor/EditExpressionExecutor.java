package com.tibbo.aggregate.client.action.executor;

import javax.swing.*;

import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.gui.dialog.*;
import com.tibbo.aggregate.client.gui.frame.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.expression.*;
import com.tibbo.aggregate.component.*;
import com.tibbo.aggregate.component.expressionbuilder.*;

public class EditExpressionExecutor extends AbstractCommandExecutor
{
  public EditExpressionExecutor()
  {
    super(ActionUtils.CMD_EDIT_EXPRESSION);
  }
  
  @Override
  public GenericActionResponse execute(Operation originator, GenericActionCommand cmd)
  {
    Context defaultContext = originator.getContext();
    ContextManager manager = defaultContext.getContextManager();
    
    CallerController callerController = null;
    if (originator instanceof InvokeActionOperation && ((InvokeActionOperation) originator).getConnector() != null)
    {
      callerController = ((InvokeActionOperation) originator).getConnector().getCallerController();
    }
    if (callerController == null)
    {
      callerController = manager.getCallerController();
    }
    
    ExpressionBuilder eb = new ExpressionBuilder(null, null, null, new Expression(""), manager, defaultContext,
        null, null, null, null, null,
        callerController, null, null, null);
    
    OkCancelDialogFrame dial = new ExpressionBuilderDialog(null, eb, null, null, null, null);
    
    SwingUtilities.invokeLater(() -> ComponentHelper.getMainFrame().addElement(null, dial));
    
    return new GenericActionResponse(null);
  }
}
