package com.tibbo.aggregate.client.action.executor;

import java.util.*;
import java.util.concurrent.*;

import com.tibbo.aggregate.client.guibuilder.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.action.command.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.server.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.component.*;

public class LaunchProcessControlProgramExecutor extends LaunchWidgetExecutor
{
  private DmfComponent component;
  private CountDownLatch sync;
  
  public LaunchProcessControlProgramExecutor()
  {
    super(ActionUtils.CMD_LAUNCH_PROCESS_CONTROL_PROGRAM);
    
  }
  
  @Override
  public GenericActionResponse execute(final Operation originator, final GenericActionCommand cmd)
  {
    sync = new CountDownLatch(1);
    GenericActionResponse res = super.execute(originator, cmd);
    
    try
    {
      sync.await();
    }
    catch (InterruptedException ex)
    {
      Log.CONTEXT_ACTIONS.error("Error while executing GUI Builder", ex);
    }
    return res;
  }
  
  @Override
  protected void addShutdownListenerToFrame(final String elKey, final InvokeActionOperation iop)
  {
    frame.addShutdownListener(new FrameShutdownListener(elKey, iop)
    {
      @Override
      public void shutdown()
      {
        sync.countDown();
        super.shutdown();
      }
    });
  }
  
  @Override
  protected SwingWidget createSwingWidgetComponent(String title, ContextManager contextManager, Context widgetContext, Context defaultContext, CallerController caller,
      String template, DataTable input, Map<String, Object> environment, String uniqueScriptPrefix, RemoteConnector connector)
  {
    return new SwingWidget(title, ComponentHelper.getMainFrame().getFrame(), contextManager, new Pair<Context, Context>(widgetContext,
        defaultContext), caller, template, input, environment, uniqueScriptPrefix, connector, ACTION_EXECUTOR, scriptCompiler, true);
  }
  
  @Override
  protected DmfComponent wrapComponentIfNeeded(SwingWidget component, Context widgetContext)
  {
    int type = 0;
    try
    {
      DataTable dt = widgetContext.getVariable(EditableChildContextConstants.V_CHILD_INFO);
      type = dt.rec().getInt(ProcessControlContextConstants.VF_IMPLEMENT_LANGUAGE);
    }
    catch (ContextException e)
    {
      throw new IllegalStateException(e.getMessage(), e);
    }
    
    if (type == EditTemplate.EDIT_ST)
    {
      this.component = new StDebugger(widgetContext, component);
    }
    else if (type == EditTemplate.EDIT_FBD)
    {
      this.component = new FbdDebugger(widgetContext, component);
    }
    else if (type == EditTemplate.EDIT_LD)
    {
      this.component = new LdDebugger(widgetContext, component);
    }
    else if (type == EditTemplate.EDIT_SFC)
    {
      this.component = new SfcDebugger(widgetContext, component);
    }
    else
    {
      throw new IllegalStateException("unknown debugger type:" + type);
    }
    
    return this.component;
  }
  
  @Override
  public void cancel()
  {
    if (sync != null)
    {
      sync.countDown();
    }
    super.cancel();
  }
}
