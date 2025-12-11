package com.tibbo.aggregate.client.macro.ui;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.Action;

import org.apache.log4j.*;

import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.client.macro.*;
import com.tibbo.aggregate.client.macro.model.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.client.util.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.context.*;

public class EventMacroPlayer implements MacroProcessor
{
  public static final String VAR_INVOKER_CONTEXT = "invokerContext";
  
  private PlayerController playerController;
  private Macro macro;
  private List<StepContextHolder> pendingSteps = new LinkedList();
  private Step lastMatchedStep;
  private Thread processorThread;
  private RecordingListener listener;
  private LinkedHashMap<String, Object> macroVariables = new LinkedHashMap();
  private LinkedHashMap<String, Operation> operationToContextOperation = new LinkedHashMap();
  
  // The flag is used to indicate that the Player have loaded succesive steps of the macro and
  // ready to process Client events. Synchronization uses this flag as a loop criteria to
  // avoid missing events that somehow may arrive before the player waits them. These events
  // may hang the player in the incorrect state.
  private boolean pendingStepsLoaded;
  
  // Latest context manager is a global var to enable step back functionality
  // where the step is performed not on an PlatformEvent but by request
  protected ContextManager latestContextManager;
  
  public EventMacroPlayer(PlayerController playerController)
  {
    if (playerController == null)
    {
      throw new IllegalArgumentException("playerController is null");
    }
    
    this.playerController = playerController;
  }
  
  public boolean isActive()
  {
    return macro != null;
  }
  
  public void beginEditProperties(EventContext ctx, String remoteContext, String propertiesGroup)
  {
    EditPropertiesOperation op = new EditPropertiesOperation(getRequestId(ctx), remoteContext, propertiesGroup);
    
    checkPendingSteps(op, ctx, false);
  }
  
  public void beginEditProperties(EventContext ctx, String remoteContext, String[] properties)
  {
    EditPropertiesOperation op = new EditPropertiesOperation(getRequestId(ctx), remoteContext, properties);
    
    checkPendingSteps(op, ctx, false);
  }
  
  public EditTableOperation beginEditTable(EventContext ctx)
  {
    EditTableOperation op = new EditTableOperation();
    
    checkPendingSteps(op, ctx, false);
    
    return null;
  }
  
  public Macro beginMacro(Macro macro)
  {
    this.macro = macro;
    
    showStepDescription(macro);
    
    processorThread = new Thread(new Runnable()
    {
      public void run()
      {
        String navigationAction = waitForNavigation();
        if (PlayerController.CMD_PREV.equals(navigationAction))
        {
          endMacro();
          playerController.init();
        }
        else if (PlayerController.CMD_NEXT.equals(navigationAction))
        {
          pendingStepsLoaded = false;
          NavigationButtonAction navButtonAction = null;
          
          try
          {
            MacroIterator macroIterator = new MacroIterator(EventMacroPlayer.this.macro);
            
            navButtonAction = new NavigationButtonAction(EventMacroPlayer.this, macroIterator);
            playerController.addActionListener(navButtonAction);
            
            playerController.setButtonVisible(PlayerController.CMD_NEXT, true);
            playerController.setButtonEnabled(PlayerController.CMD_NEXT, true);
            playerController.setButtonVisible(PlayerController.CMD_PREV, true);
            playerController.setButtonEnabled(PlayerController.CMD_PREV, true);
            processMacro(macroIterator);
            playerController.hideButtons();
          }
          catch (InterruptedException ex)
          {
            Log.GUIDE.debug("Macro interrupted", ex);
          }
          catch (Exception ex)
          {
            ClientUtils.showError(Level.ERROR, ex);
            endMacro();
          }
          finally
          {
            // To bring eventprocessing back to life if an error occurs in the player
            synchronized (EventMacroPlayer.this)
            {
              pendingSteps.clear();
              pendingStepsLoaded = true;
              EventMacroPlayer.this.notifyAll();
            }
            
            playerController.removeActionListener(navButtonAction);
            playerController.removeActionListener(navButtonAction);
          }
        }
      }
    }, "Tutorial macro: " + macro.getFileName());
    
    processorThread.start();
    
    listener = new RecordingListener(this);
    
    PlatformEventMulticaster.addListener(listener);
    
    return macro;
  }
  
  public void beginOperation(EventContext ctx, String invokerContext, String operationName)
  {
    InvokeOperationOperation op = new InvokeOperationOperation(invokerContext, operationName);
    
    if (!operationToContextOperation.containsKey(op.getOperationName()))
    {
      operationToContextOperation.put(op.getOperationName(), ctx.getOperation());
    }
    
    checkPendingSteps(op, ctx, false);
  }
  
  public void beginOperation(EventContext ctx, String[] sourceContext, String targetContext, String operationName)
  {
    InvokeOperationOperation op = new InvokeOperationOperation(sourceContext, targetContext, operationName);
    
    if (!operationToContextOperation.containsKey(op.getOperationName()))
    {
      operationToContextOperation.put(op.getOperationName(), ctx.getOperation());
    }
    
    checkPendingSteps(op, ctx, false);
  }
  
  public AddTableRowOperation doAddTableRow(EventContext ctx)
  {
    return null;
  }
  
  public void doConfirm(EventContext ctx, String message, int option)
  {
    ConfirmOperation op = new ConfirmOperation(getRequestId(ctx), message, option);
    
    checkPendingSteps(op, ctx, false);
  }
  
  public EditTableCellOperation doEditTableCell(EventContext ctx, String field, int row, String value)
  {
    EditTableCellOperation op = new EditTableCellOperation(getRequestId(ctx), field, row, value);
    
    checkPendingSteps(op, ctx, false);
    
    return null;
  }
  
  public void doErrorMessage(EventContext ctx)
  {
    ErrorMessageOperation op = new ErrorMessageOperation(getRequestId(ctx));
    
    checkPendingSteps(op, ctx, false);
  }
  
  public void doMessage(EventContext ctx, String message)
  {
    MessageOperation op = new MessageOperation(getRequestId(ctx), message);
    
    checkPendingSteps(op, ctx, false);
  }
  
  public MoveTableRowOperation doMoveTableRow(EventContext ctx)
  {
    return null;
  }
  
  public RemoveTableRowOperation doRemoveTableRow(EventContext ctx)
  {
    return null;
  }
  
  public void doReport(EventContext ctx)
  {
  }
  
  public void endEditProperties(EventContext ctx, String remoteContext, String[] properties)
  {
    EditPropertiesOperation op = new EditPropertiesOperation(getRequestId(ctx), remoteContext, properties);
    
    checkPendingSteps(op, ctx, true);
  }
  
  public void endEditProperties(EventContext ctx, String remoteContext, String propertiesGroup)
  {
    EditPropertiesOperation op = new EditPropertiesOperation(getRequestId(ctx), remoteContext, propertiesGroup);
    
    checkPendingSteps(op, ctx, true);
  }
  
  public void endEditTable(EventContext ctx)
  {
    EditTableOperation op = new EditTableOperation();
    
    checkPendingSteps(op, ctx, true);
  }
  
  public void endMacro()
  {
    PlatformEventMulticaster.removeListener(listener);
    
    if (processorThread != null)
    {
      processorThread.interrupt();
    }
    
    macro = null;
  }
  
  public void endOperation(EventContext ctx, String invokerContext, String operationName)
  {
    InvokeOperationOperation op = new InvokeOperationOperation(invokerContext, operationName);
    
    checkPendingSteps(op, ctx, true);
    
    Operation ctxOp = operationToContextOperation.get(op.getOperationName());
    if (ctxOp == ctx.getOperation())
    {
      operationToContextOperation.remove(op.getOperationName());
    }
  }
  
  public void endOperation(EventContext ctx, String[] sourceContext, String targetContext, String operationName)
  {
    InvokeOperationOperation op = new InvokeOperationOperation(sourceContext, targetContext, operationName);
    
    checkPendingSteps(op, ctx, true);
    
    Operation ctxOp = operationToContextOperation.get(op.getOperationName());
    if (ctxOp == ctx.getOperation())
    {
      operationToContextOperation.remove(op.getOperationName());
    }
  }
  
  public void setVariable(String name, Object value)
  {
    macroVariables.put(name, value);
  }
  
  public Object getVariable(String name)
  {
    if (!macroVariables.containsKey(name))
    {
      throw new IllegalStateException("Variable '" + name + "' is not defined");
    }
    
    return macroVariables.get(name);
  }
  
  protected synchronized void processMacro(MacroIterator macroIterator) throws InterruptedException
  {
    List<StepContextHolder> options = macroIterator.next(macro, null);
    do
    {
      latestContextManager = null;
      ActionCommandContext stepContext = macroIterator.getStepContext(lastMatchedStep, false);
      Operation contextOperation = operationToContextOperation.get(stepContext.getOperationName());
      if (contextOperation != null)
      {
        Context invokerContext = contextOperation.getInvokerContext();
        if (invokerContext != null)
        {
          latestContextManager = invokerContext.getContextManager();
        }
      }
      
      if (options.isEmpty() || options.iterator().next().step == macro)
      {
        break;
      }
      
      options = waitForAll(options, macroIterator, latestContextManager);
    }
    while (true);
    
    showStepDescription(Collections.singletonMap(macro, new StepDisplayOptions(true)));
    endMacro();
  }
  
  protected synchronized List<StepContextHolder> waitForAll(List<StepContextHolder> options, MacroIterator macroIterator, ContextManager contextManager) throws InterruptedException
  {
    if (options.isEmpty())
    {
      return null;
    }
    
    Map<Step, StepDisplayOptions> steps = showStepDescription(options);
    
    pendingSteps.clear();
    pendingSteps.addAll(options);
    
    do
    {
      pendingStepsLoaded = true;
      notifyAll();
      lastMatchedStep = null;
      
      while (lastMatchedStep == null)
      {
        wait();
      }
      
      List<StepContextHolder> updatedOptions = macroIterator.next(lastMatchedStep, contextManager);
      // Here it is important to work with pendingSteps but not options since pendingSteps are being changed by
      // next/back functionality
      if (!pendingSteps.equals(updatedOptions))
      {
        return updatedOptions;
      }
      
      for (Map.Entry<Step, StepDisplayOptions> entry : steps.entrySet())
      {
        entry.getValue().setPerformed(macroIterator.isPerformed(entry.getKey()));
      }
      
      showStepDescription(steps);
    }
    while (true);
  }
  
  private Map<Step, StepDisplayOptions> showStepDescription(List<StepContextHolder> options)
  {
    Map<Step, StepDisplayOptions> steps = new LinkedHashMap();
    for (StepContextHolder elem : options)
    {
      steps.put(elem.step, new StepDisplayOptions(elem.ctx.isFinishing()));
    }
    showStepDescription(steps);
    return steps;
  }
  
  protected synchronized String waitForNavigation()
  {
    playerController.setButtonVisible(PlayerController.CMD_NEXT, true);
    playerController.setButtonEnabled(PlayerController.CMD_NEXT, true);
    playerController.setButtonVisible(PlayerController.CMD_PREV, true);
    playerController.setButtonEnabled(PlayerController.CMD_PREV, true);
    
    pendingStepsLoaded = true;
    final StringBuffer result = new StringBuffer();
    ActionListener l = new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        result.append(e.getActionCommand());
        synchronized (EventMacroPlayer.this)
        {
          EventMacroPlayer.this.notifyAll();
        }
      }
    };
    
    playerController.addActionListener(l);
    try
    {
      wait();
    }
    catch (InterruptedException ex)
    {
    }
    
    playerController.removeActionListener(l);
    playerController.hideButtons();
    
    return result.toString();
  }
  
  protected synchronized Step checkPendingSteps(Step step, EventContext ctx, boolean finishing)
  {
    while (!pendingStepsLoaded)
    {
      try
      {
        wait();
      }
      catch (InterruptedException ex)
      {
        Log.GUIDE.warn("checkPendingSteps() is interrupted", ex);
        throw new IllegalStateException(ex);
      }
    }
    
    for (StepContextHolder entry : pendingSteps)
    {
      Step evaluatedStep = entry.step;
      ActionCommandContext aCtx = entry.ctx;
      
      Operation ctxOp = operationToContextOperation.get(aCtx.getOperationName());
      
      if (ctx != null && ctx.getOperation() != ctxOp)
      {
        continue;
      }
      
      if (step.stepEquals(evaluatedStep) && (aCtx == null || ctx == null || aCtx.equals(ctx) && aCtx.isFinishing() == finishing))
      {
        // Toolkit.getDefaultToolkit().beep();
        doStep(entry.step);
        break;
      }
    }
    
    return lastMatchedStep;
  }
  
  synchronized void doStep(Step step)
  {
    waitForStepsToLoad();
    
    lastMatchedStep = step;
    pendingStepsLoaded = false;
    notifyAll();
  }
  
  private synchronized void waitForStepsToLoad()
  {
    while (!pendingStepsLoaded)
    {
      try
      {
        wait();
      }
      catch (InterruptedException ex)
      {
        Log.GUIDE.warn("doStep() is interrupted", ex);
        throw new IllegalStateException(ex);
      }
    }
  }
  
  synchronized void doStepBack(MacroIterator macroIterator)
  {
    while (!pendingStepsLoaded)
    {
      try
      {
        wait();
      }
      catch (InterruptedException ex)
      {
        Log.GUIDE.warn("doStepBack() is interrupted", ex);
        throw new IllegalStateException(ex);
      }
    }
    
    pendingStepsLoaded = false;
    List<StepContextHolder> options = macroIterator.previous(latestContextManager);
    pendingSteps.clear();
    if (options.isEmpty())
    {
      Macro m = macro;
      endMacro();
      beginMacro(m);
    }
    else
    {
      pendingSteps.addAll(options);
      showStepDescription(options);
      pendingStepsLoaded = true;
    }
  }
  
  public void refreshDescriptions()
  {
    if (pendingSteps != null)
    {
      if (pendingSteps.isEmpty())
      {
        if (macro != null)
        {
          showStepDescription(macro);
        }
      }
      else
      {
        showStepDescription(pendingSteps);
      }
    }
  }
  
  private void showStepDescription(Step step)
  {
    showStepDescription(Collections.singletonMap(step, new StepDisplayOptions()));
  }
  
  private void showStepDescription(Map<? extends Step, StepDisplayOptions> steps)
  {
    StringBuffer title = new StringBuffer();
    StringBuffer body = new StringBuffer();
    
    body.append("<table>");
    for (Iterator i = steps.entrySet().iterator(); i.hasNext();)
    {
      Map.Entry<? extends Step, StepDisplayOptions> entry = (Map.Entry) i.next();
      
      Step step = entry.getKey();
      StepDisplayOptions opts = entry.getValue();
      
      if (step.getTitle() != null && step.getTitle().length() > 0 && title.length() == 0)
      {
        title.append(MacroHelper.processResourceReferences(step.getTitle()));
      }
      
      String stepDesc = getStepDescription(step, opts.isFinishing());
      
      if (stepDesc != null)
      {
        body.append("<tr>");
        body.append("<td valign=\"top\">");
        if (step instanceof ShowDescriptionOperation || steps.size() == 1)
        {
          // Descriptions are not supplied with a "check" mark
          body.append("&nbsp;");
        }
        else
        {
          if (opts.isPerformed())
          {
            body.append("<img src=\"done.png\" alt=\"" + Pres.get().getString("macroPlayerDoneAlt") + "\">");
          }
          else
          {
            body.append("<img src=\"not_done.png\" alt=\"" + Pres.get().getString("macroPlayerTodoAlt") + "\">");
          }
        }
        body.append("</td>");
        body.append("<td>");
        body.append(stepDesc);
        body.append("</td>");
        body.append("</tr>");
      }
    }
    body.append("</table>");
    
    playerController.setTitle(title.toString());
    playerController.setHtml(body.toString());
  }
  
  public static String getStepDescription(Step step, boolean finishing)
  {
    String stepDesc = null;
    
    HtmlStepDescription htmlDesc = (HtmlStepDescription) step.getDescription(HtmlStepDescription.class, finishing);
    if (htmlDesc != null && htmlDesc.isOnFinish() == finishing)
    {
      stepDesc = MacroHelper.processResourceReferences(htmlDesc.getBody());
    }
    else
    {
      PlainTextDescription plainDesc = (PlainTextDescription) step.getDescription(PlainTextDescription.class, finishing);
      if (plainDesc != null && plainDesc.isOnFinish() == finishing)
      {
        stepDesc = MacroHelper.processResourceReferences(plainDesc.getText());
      }
    }
    
    return stepDesc;
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
  
  public Macro getMacro()
  {
    return macro;
  }
  
  class NavigationButtonAction extends javax.swing.AbstractAction
  {
    private EventMacroPlayer player;
    private MacroIterator macroIterator;
    private boolean showNextConfirmation = true;
    
    public NavigationButtonAction(EventMacroPlayer player, MacroIterator macroIterator)
    {
      this.player = player;
      this.macroIterator = macroIterator;
    }
    
    public void actionPerformed(ActionEvent e)
    {
      doNavigationAction(e.getActionCommand());
      
      boolean descriptionAbsent = true;
      
      do
      {
        waitForStepsToLoad();
        
        List<StepContextHolder> currentSteps = macroIterator.getCurrentSteps(EventMacroPlayer.this.latestContextManager);
        descriptionAbsent = true;
        
        for (StepContextHolder elem : currentSteps)
        {
          String desc = getStepDescription(elem.step, elem.ctx.isFinishing());
          if (desc != null)
          {
            descriptionAbsent = false;
            break;
          }
        }
        
        if (descriptionAbsent)
        {
          waitForStepsToLoad();
          doNavigationAction(e.getActionCommand());
        }
      }
      while (descriptionAbsent);
    }
    
    private void doNavigationAction(String actionCommand)
    {
      synchronized (player)
      {
        if (PlayerController.CMD_NEXT.equals(actionCommand))
        {
          List<StepContextHolder> currentSteps = macroIterator.next(null, EventMacroPlayer.this.latestContextManager);
          
          boolean isShowDescriptionSteps = true;
          for (StepContextHolder elem : currentSteps)
          {
            if (!elem.step.isDescription())
            {
              isShowDescriptionSteps = false;
              break;
            }
          }
          
          if (isShowDescriptionSteps || !showNextConfirmation || ClientUtils.confirm(Pres.get().getString("macroPlayerNextInMacro")))
          {
            doNext();
            showNextConfirmation = false;
          }
          
        }
        else if (PlayerController.CMD_PREV.equals(actionCommand))
        {
          doPrevious();
        }
      }
    }
    
    private void doPrevious()
    {
      player.doStepBack(macroIterator);
    }
    
    private void doNext()
    {
      if (player.pendingSteps.isEmpty())
      {
        return;
      }
      
      Step firstStep = player.pendingSteps.iterator().next().step;
      String concurrency = firstStep.getConcurrency();
      
      if (Step.CONCURRENCY_PARALLEL.equals(concurrency))
      {
        for (StepContextHolder entry : player.pendingSteps)
        {
          Step step = entry.step;
          if (macroIterator.isPerformed(step))
          {
            continue;
          }
          
          player.doStep(step);
          break;
        }
      }
      else
      {
        if (player.pendingSteps.size() == 1)
        {
          player.doStep(firstStep);
        }
        else
        {
          JPopupMenu menu = new JPopupMenu();
          for (StepContextHolder entry : player.pendingSteps)
          {
            final Step step = entry.step;
            JMenuItem item = new JMenuItem(new AbstractAction()
            {
              {
                putValue(Action.NAME, step.getTitle());
              }
              
              public void actionPerformed(ActionEvent e)
              {
                player.doStep(step);
              }
            });
            
            menu.add(item);
          }
          
          player.playerController.showNavigationMenu(menu);
        }
      }
    }
  }
}
