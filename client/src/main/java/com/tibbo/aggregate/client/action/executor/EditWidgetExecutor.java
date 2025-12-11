package com.tibbo.aggregate.client.action.executor;

import com.tibbo.aggregate.client.Client;
import com.tibbo.aggregate.client.guibuilder.AbstractAggreGateIDE;
import com.tibbo.aggregate.client.guibuilder.WidgetBuilder;
import com.tibbo.aggregate.client.operation.InvokeActionOperation;
import com.tibbo.aggregate.common.action.ActionUtils;
import com.tibbo.aggregate.common.context.ContextManager;
import com.tibbo.aggregate.component.ComponentHelper;

public class EditWidgetExecutor extends EditTemplateExecutor
{
  public EditWidgetExecutor()
  {
    super(ActionUtils.CMD_EDIT_WIDGET);
  }
  
  @Override
  protected AbstractAggreGateIDE createAggreGateIDE(final String title, final ContextManager contextManager, final InvokeActionOperation iop,
      final String widgetContext, final String defaultContext, final String template, final String uniqueScriptPrefix, int editMode)
  {
    return new WidgetBuilder(title,
            contextManager,
            iop.getContext().get(widgetContext),
            iop.getContext().get(defaultContext),
            uniqueScriptPrefix,
            iop.getConnector(),
            ComponentHelper.getIoThreadPool(),
            Client.getParameters().getUiBuilderUndoLimit(),
            template)
    {
      @Override
      public void stop()
      {
        EditWidgetExecutor.this.stop();
        super.stop();
      }
    };
  }
}
