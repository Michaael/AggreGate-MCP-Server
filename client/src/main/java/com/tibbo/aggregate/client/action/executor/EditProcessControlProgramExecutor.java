package com.tibbo.aggregate.client.action.executor;

import com.tibbo.aggregate.client.Client;
import com.tibbo.aggregate.client.guibuilder.AbstractAggreGateIDE;
import com.tibbo.aggregate.client.ide.ProcessControlIDE;
import com.tibbo.aggregate.client.operation.InvokeActionOperation;
import com.tibbo.aggregate.common.action.ActionUtils;
import com.tibbo.aggregate.common.context.ContextManager;
import com.tibbo.aggregate.component.ComponentHelper;

public class EditProcessControlProgramExecutor extends EditTemplateExecutor
{
  
  public EditProcessControlProgramExecutor()
  {
    super(ActionUtils.CMD_EDIT_PROCESS_CONTROL_PROGRAM);
  }
  
  @Override
  protected AbstractAggreGateIDE createAggreGateIDE(String title, ContextManager contextManager, InvokeActionOperation iop, String widgetContext, String defaultContext, String template,
      String uniqueScriptPrefix, int editMode)
  {
    return abstractAggreGateIDE = new ProcessControlIDE(title,
            contextManager,
            iop.getContext().get(widgetContext),
            iop.getContext().get(defaultContext),
            template,
            uniqueScriptPrefix,
            iop.getConnector(),
        ComponentHelper.getIoThreadPool(),
            editMode,
            Client.getParameters().getUiBuilderUndoLimit())
    {
      @Override
      public void stop()
      {
        EditProcessControlProgramExecutor.this.stop();
        super.stop();
      }
    };
  }
}
