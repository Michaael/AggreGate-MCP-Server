package com.tibbo.aggregate.client.action.executor;

import com.tibbo.aggregate.client.Client;
import com.tibbo.aggregate.client.guibuilder.AbstractAggreGateIDE;
import com.tibbo.aggregate.client.guibuilder.workflow.WorkflowIDE;
import com.tibbo.aggregate.client.operation.InvokeActionOperation;
import com.tibbo.aggregate.common.action.ActionUtils;
import com.tibbo.aggregate.common.context.ContextManager;
import com.tibbo.aggregate.component.ComponentHelper;

public class EditWorkflowExecutor extends EditTemplateExecutor
{
  
  public EditWorkflowExecutor()
  {
    super(ActionUtils.CMD_EDIT_WORKFLOW);
  }
  
  @Override
  protected AbstractAggreGateIDE createAggreGateIDE(String title, ContextManager contextManager, InvokeActionOperation iop, String widgetContext, String defaultContext, String template,
      String uniqueScriptPrefix, int editMode)
  {
    return new WorkflowIDE(title,
            contextManager,
            iop.getContext().get(widgetContext),
            iop.getContext().get(defaultContext),
            template,
            uniqueScriptPrefix,
            iop.getConnector(),
            ComponentHelper.getIoThreadPool(),
            Client.getParameters().getUiBuilderUndoLimit())
    {
      @Override
      public void stop()
      {
        EditWorkflowExecutor.this.stop();
        super.stop();
      }
    };
  }
}
