package com.tibbo.aggregate.client.action.executor;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.gui.*;
import com.tibbo.aggregate.client.gui.frame.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.client.util.*;
import com.tibbo.aggregate.common.Log;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.action.command.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.expression.EvaluationEnvironment;
import com.tibbo.aggregate.common.expression.EvaluationException;
import com.tibbo.aggregate.common.expression.Evaluator;
import com.tibbo.aggregate.common.expression.Expression;
import com.tibbo.aggregate.common.protocol.*;
import com.tibbo.aggregate.common.resource.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.common.widget.context.WSystemTreeContext;
import com.tibbo.aggregate.component.ComponentHelper;
import com.tibbo.aggregate.component.systemtree.*;
import org.apache.log4j.Level;

public class ShowSystemTreeExecutor extends AbstractCommandExecutor
{
  private static final String SYSTEM_TREE_FRAME_KEY_PREFIX = "system_tree_";
  private static final String V_VALUE = "value";
  
  private InternalFrame frame;
  private Expression nodeClickExpression;
  private Evaluator evaluator;
  private Expression nodeFilterExpression;
  private LinkedBlockingQueue<ProxyContext> contextsQueue = new LinkedBlockingQueue<>();
  private SystemTreeChildThread systemTreeChildThread = null;
  
  private TreeSelectionListener selectionListener = new TreeSelectionListener()
  {
    @Override
    public void valueChanged(TreeSelectionEvent e)
    {
      try
      {
        DataTable eventTable = WSystemTreeContext.prepareSelectionEventTable(e);
        EvaluationEnvironment evaluationEnvironment = new EvaluationEnvironment();
        
        HashMap<String, Object> map = new HashMap<>();
        map.put(V_VALUE, eventTable);
        evaluationEnvironment.setEnvironment(map);
        
        evaluator.evaluate(nodeClickExpression, evaluationEnvironment);
      }
      catch (SyntaxErrorException | EvaluationException e1)
      {
        ClientUtils.showError(null, Level.WARN, e1);
      }
    }
  };
  
  public ShowSystemTreeExecutor()
  {
    super(ActionUtils.CMD_SHOW_SYSTEM_TREE);
  }
  
  @Override
  public GenericActionResponse execute(Operation originator, final GenericActionCommand cmd)
  {
    final InvokeActionOperation iop = (InvokeActionOperation) originator;
    
    List<ProxyContext> roots = new LinkedList<>();
    
    final DataRecord rec = cmd.getParameters().rec();
    
    boolean relatedActions = rec.getFormat().hasField(ShowSystemTree.CF_RELATED_ACTIONS) ? rec.getBoolean(ShowSystemTree.CF_RELATED_ACTIONS) : true;
    boolean contextMenu = rec.getFormat().hasField(ShowSystemTree.CF_CONTEXT_MENU) ? rec.getBoolean(ShowSystemTree.CF_CONTEXT_MENU) : true;
    boolean toolBar = rec.getFormat().hasField(ShowSystemTree.CF_SHOW_TOOLBAR) ? rec.getBoolean(ShowSystemTree.CF_SHOW_TOOLBAR) : true;
    String nodeClickExpression = rec.getFormat().hasField(ShowSystemTree.CF_NODE_CLICK_EXPRESSION) ?
        rec.getString(ShowSystemTree.CF_NODE_CLICK_EXPRESSION) : null;
    
    String nodeFilterExpression = rec.getFormat().hasField(ShowSystemTree.CF_NODE_FILTER_EXPRESSION) ? rec.getString(ShowSystemTree.CF_NODE_FILTER_EXPRESSION) : null;
    
    DataTable locationData = rec.getDataTable(ShowSystemTree.CF_LOCATION);
    final WindowLocation location = locationData != null ? new WindowLocation(locationData.rec()) : null;
    
    if (location != null)
    {
      location.applyDefaultSize(InternalFrame.SIZE_SYSTEM_TREE);
    }
    
    DataTable dashboardData = rec.hasField(ShowSystemTree.CF_DASHBOARD) ? rec.getDataTable(ShowSystemTree.CF_DASHBOARD) : null;
    final DashboardProperties dashboard = dashboardData != null ? new DashboardProperties(dashboardData.rec()) : null;
    
    DataTable dhInfoData = rec.hasField(ShowSystemTree.CF_DASHBOARDS_HIERARCHY_INFO) ? rec.getDataTable(ShowSystemTree.CF_DASHBOARDS_HIERARCHY_INFO) : null;
    final DashboardsHierarchyInfo dhInfo = dhInfoData != null ? new DashboardsHierarchyInfo(dhInfoData.rec()) : null;
    
    String key = rec.getFormat().hasField(ShowSystemTree.CF_KEY) ? rec.getString(ShowSystemTree.CF_KEY) : null;
    
    String root = rec.getString(ShowSystemTree.CF_ROOT);
    
    for (DataRecord rootRec : rec.getDataTable(ShowSystemTree.CF_ROOTS))
    {
      ProxyContext rootContext = (ProxyContext) iop.getContext().get(rootRec.getString(ShowSystemTree.CF_ROOTS_ROOT));
      
      if (rootContext != null)
      {
        roots.add(rootContext);
      }
    }
    
    iop.addInterruptionListener(interruptionListener);
    
    SystemTree st;
    final RemoteConnector connector = iop.getConnector();
    if (root != null)
    {
      if (nodeFilterExpression != null && !nodeFilterExpression.trim().isEmpty())
      {
        st = getFilterSystemTree(iop, root, nodeFilterExpression, relatedActions, contextMenu);
      }
      else
      {
        ProxyContext rc = (ProxyContext) iop.getContext().getContextManager().get(root);
        st = new SystemTree(connector, null, rc, relatedActions ? SystemTree.RELATED_ACTIONS_BOTTOM : SystemTree.RELATED_ACTIONS_HIDDEN, contextMenu);
      }
    }
    else
    {
      st = new SystemTree(connector, null, roots, relatedActions ? SystemTree.RELATED_ACTIONS_BOTTOM : SystemTree.RELATED_ACTIONS_HIDDEN, contextMenu);
    }
    st.setToolBarVisible(toolBar);
    
    final SystemTree systemTree = st;
    
    final String frameKey = ((key != null) && (!key.isEmpty())) ? key
        : (ExecutionHelper.createFrameKey(location, SYSTEM_TREE_FRAME_KEY_PREFIX + (root != null ? ContextUtils.contextPathToContextName(root) : roots.hashCode())));
    
    if (nodeClickExpression != null && !nodeClickExpression.trim().isEmpty())
    {
      evaluator = new Evaluator(iop.getConnector().getContextManager(), iop.getConnector().getCallerController());
      this.nodeClickExpression = new Expression(nodeClickExpression);
      systemTree.getTree().getSelectionModel().addTreeSelectionListener(selectionListener);
    }
    
    SwingUtilities.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        frame = InternalFrame.create(frameKey, cmd.getTitle(), systemTree, true, true, location, dashboard, Docs.CL_SYSTEM_TREE, connector, dhInfo);
        
        frame.setFrameIcon(ResourceManager.getImageIcon(Icons.FR_SYSTEM_TREE));
        
        frame.addShutdownListener(new ShutdownListener()
        {
          @Override
          public void shutdown()
          {
            systemTree.getTree().getSelectionModel().removeTreeSelectionListener(selectionListener);
            if (iop != null)
            {
              iop.removeInterruptionListener(interruptionListener);
            }
          }
        });
      }
    });
    return new GenericActionResponse(null, false, cmd.getRequestId());
  }
  
  private SystemTree getFilterSystemTree(InvokeActionOperation iop, String root, String nodeFilterExpression,
      boolean relatedActions, boolean contextMenu)
  {
    SystemTree st;
    loadContextList(iop.getConnector(), root, nodeFilterExpression);
    ProxyContext context = null;
    try
    {
      context = getContext();
    }
    catch (InterruptedException e)
    {
      Log.SYSTEMTREE.warn(e);
    }
    boolean loadDefault = false;
    if (context == null)
    {
      loadDefault = true;
      context = (ProxyContext) iop.getContext().get(root, iop.getConnector().getCallerController());
    }
    List<ProxyContext> contexts = Collections.singletonList(context);
    st = new SystemTree(iop.getConnector(), null, contexts, relatedActions ? SystemTree.RELATED_ACTIONS_BOTTOM : SystemTree.RELATED_ACTIONS_HIDDEN, contextMenu);
    if (!loadDefault)
    {
      systemTreeChildThread = new SystemTreeChildThread(st);
      systemTreeChildThread.start();
    }
    return st;
  }
  
  private class SystemTreeChildThread extends Thread
  {
    private final SystemTree tree;
    
    private SystemTreeChildThread(SystemTree tree)
    {
      this.tree = tree;
    }
    
    @Override
    public void run()
    {
      try
      {
        while (!Thread.currentThread().isInterrupted())
        {
          try
          {
            ProxyContext childContext = getContext();
            if (childContext == null)
              continue;
            
            tree.addChild(childContext);
          }
          catch (InterruptedException e)
          {
            Log.SYSTEMTREE.debug(e.getMessage(), e);
            break;
          }
          catch (Exception ex)
          {
            Log.SYSTEMTREE.warn(ex.getMessage(), ex);
          }
        }
      }
      catch (Exception ex)
      {
        Log.SYSTEMTREE.warn(ex.getMessage(), ex);
      }
    }
  }
  
  private ProxyContext getContext() throws InterruptedException
  {
    return contextsQueue.poll(RemoteServer.DEFAULT_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
  }
  
  private void loadContextList(RemoteConnector connector, String root, String expression)
  {
    ComponentHelper.getIoThreadPool().submit(() -> {
      ContextManager manager = connector.getContextManager();
      CallerController callerController = connector.getCallerController();
      Evaluator evaluator = new Evaluator(manager, callerController);
      Context rootContext = manager.get(root, callerController);
      setNodeFilterExpression(new Expression(expression));
      evaluator.setDefaultContext(rootContext);
      List<ProxyContext> visibleList = rootContext.getVisibleChildren(callerController);
      getContextChildList(visibleList, manager, evaluator, callerController);
    });
  }
  
  public void setNodeFilterExpression(Expression nodeFilterExpression)
  {
    this.nodeFilterExpression = nodeFilterExpression;
  }
  
  public Expression getNodeFilterExpression()
  {
    return nodeFilterExpression;
  }
  
  private void getContextChildList(List<ProxyContext> visibleList, ContextManager manager,
      Evaluator evaluator, CallerController caller)
  {
    for (ProxyContext proxyContext : visibleList)
    {
      try
      {
        evaluator.setDefaultContext(proxyContext);
        if (evaluator.evaluateToBoolean(getNodeFilterExpression()))
        {
          contextsQueue.put(proxyContext);
        }
        
        List<ProxyContext> visibleListChild = proxyContext.getVisibleChildren(caller);
        if (visibleList.size() > 0)
        {
          ComponentHelper.getIoThreadPool().submit(() ->
              getContextChildList(visibleListChild, manager, new Evaluator(manager, caller), caller));
        }
      }
      catch (Exception ex)
      {
        Log.SYSTEMTREE.debug(ex.getMessage(), ex);
      }
    }
  }
  
  private void hideTree()
  {
    if (systemTreeChildThread != null && systemTreeChildThread.isAlive())
      systemTreeChildThread.interrupt();
    
    if (frame != null)
    {
      ClientUtils.removeFrame(frame);
    }
  }
  
  private final Runnable interruptionListener = new Runnable()
  {
    @Override
    public void run()
    {
      hideTree();
    }
  };
}
