package com.tibbo.aggregate.client.macro.model;

import java.util.*;

import com.tibbo.aggregate.client.macro.expression.*;
import com.tibbo.aggregate.client.macro.ui.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.context.*;

import com.tibbo.aggregate.common.expression.*;
import com.tibbo.aggregate.common.util.*;

public class MacroIterator
{
  private Macro macro;
  private LinkedHashMap<String, Object> macroVariables = new LinkedHashMap();
  private LinkedList<LinkedList<Step>> currentPath = new LinkedList();
  private LinkedList<Step> performedSteps = new LinkedList();
  
  public MacroIterator(Macro macro)
  {
    if (macro == null)
    {
      throw new IllegalArgumentException("macro is null");
    }
    
    this.macro = macro;
    
    LinkedList<Step> macroWrapper = new LinkedList();
    macroWrapper.add(macro);
    currentPath.add(macroWrapper);
  }
  
  public boolean isPerformed(Step step)
  {
    return contains(step, performedSteps);
  }
  
  public List<StepContextHolder> previous(ContextManager contextManager)
  {
    LinkedList<Step> currentSteps = currentPath.peekLast();
    
    // We've probably finished the macro and should give client app a chance to process this state
    // For example by displaying menu of all availible macros
    if (currentSteps == null)
    {
      return Collections.emptyList();
    }
    
    // Current path has only one level, i.e. current step has no parent and so it seems to be Macro
    if (currentPath.size() == 1)
    {
      return Collections.emptyList();
    }
    
    // The firstStep of the group being the current steps. It is the boundary step folowing previous group
    Step curStep = currentSteps.peekFirst();
    LinkedList<Step> currentParentSteps = (LinkedList) currentPath.get(currentPath.size() - 2);
    
    AtomicStep curParentStep = findParentStep(currentParentSteps, curStep);
    if (curParentStep == null)
    {
      throw new AssertionError();
    }
    
    currentPath.removeLast();
    
    // Clean up the state as if the steps was never performed
    cleanupPerformed(currentSteps, currentParentSteps);
    
    List<StepContextHolder> options = preparePreviousOptions(curStep, curParentStep, contextManager);
    
    if (!options.isEmpty())
    {
      return options;
    }
    
    // ...and do the second iteration
    if (currentPath.size() == 1)
    {
      return Collections.emptyList();
    }
    
    curStep = curParentStep;
    currentParentSteps = (LinkedList) currentPath.get(currentPath.size() - 2);
    
    curParentStep = findParentStep(currentParentSteps, curStep);
    if (curParentStep == null)
    {
      throw new AssertionError();
    }
    
    currentPath.removeLast();
    
    options = preparePreviousOptions(curStep, curParentStep, contextManager);
    if (options.isEmpty())
    {
      throw new AssertionError();
    }
    
    currentSteps = currentPath.peekLast();
    
    // This should follow preparePreviousOptions() since the last uses the performed state of a step
    cleanupPerformed(currentSteps, currentParentSteps);
    
    return options;
  }
  
  private void cleanupPerformed(List<Step> currentSteps, List<Step> currentParentSteps)
  {
    currentSteps = new LinkedList(currentSteps);
    currentSteps.removeAll(currentParentSteps); // To remove parent step ending that may be found here if concurrency="optional" is set on the last child step
    
    for (Step step : currentSteps)
    {
      performedSteps.remove(step);
      if (step instanceof AtomicStep)
      {
        AtomicStep aStep = (AtomicStep) step;
        cleanupPerformed((aStep).listSteps(), Collections.singletonList(step));
      }
    }
  }
  
  public List<StepContextHolder> getCurrentSteps(ContextManager contextManager)
  {
    return next(null, contextManager);
  }
  
  public List<StepContextHolder> next(Step firedStep, ContextManager contextManager)
  {
    List<StepContextHolder> steps = nextImpl(firedStep, contextManager);
    
    for (StepContextHolder holder : steps)
    {
      if (holder.step.isDescription())
      {
        performedSteps.add(holder.step);
      }
    }
    
    return steps;
  }
  
  private List nextImpl(Step firedStep, ContextManager contextManager) throws AssertionError, IllegalStateException
  {
    LinkedList<Step> currentSteps = currentPath.peekLast();
    
    if (currentSteps == null)
    {
      return Collections.emptyList();
    }
    
    if (firedStep == null)
    {
      return supplyWithContext(currentSteps, false);
    }
    
    if (!contains(firedStep, currentSteps))
    {
      throw new IllegalStateException("Fired step isn't within current state");
    }
    
    // Check if we should go into the fired step or atomic step finishing
    if (firedStep instanceof AtomicStep)
    {
      if (!contains(firedStep, performedSteps))
      {
        performedSteps.add(firedStep);
        return prepareNextOptions((Step) null, (AtomicStep) firedStep, contextManager);
      }
    }
    
    // A redundant checking but it proptects us from changing performedSteps type from Set to other Collection type
    if (!contains(firedStep, performedSteps))
    {
      performedSteps.add(firedStep);
    }
    
    // Check if current group of steps is parallel and may be performed in any order not as listed in macro
    if (Step.CONCURRENCY_PARALLEL.equals(currentSteps.get(0).getConcurrency()) || Step.CONCURRENCY_PARALLEL_OPTIONAL.equals(currentSteps.get(0).getConcurrency()))
    {
      // Clean out description steps which shouldn't be executed
      LinkedList<Step> currentStepsWithoutDesc = (LinkedList<Step>) currentSteps.clone();
      for (Iterator i = currentStepsWithoutDesc.iterator(); i.hasNext();)
      {
        Step step = (Step) i.next();
        if (Step.CONCURRENCY_PARALLEL_OPTIONAL.equals(step.getConcurrency()) || step instanceof ShowDescriptionOperation && currentStepsWithoutDesc.size() > 1)
        {
          i.remove();
        }
      }
      // Check if we've performed all the steps from the current parallel group
      if (!performedSteps.containsAll(currentStepsWithoutDesc))
      {
        // Some finishing steps may go with 'true' as finishing
        return supplyWithContext(currentSteps, false);
      }
    }
    
    // Current path has only one level, i.e. current step has no parent and so it seems to be Macro
    if (currentPath.size() == 1)
    {
      return Collections.emptyList();
    }
    
    LinkedList<Step> currentParentSteps = (LinkedList) currentPath.get(currentPath.size() - 2);
    
    // Script language allows to set concurrency = OPTIONAL to the last child step. This means that
    // parent's finishing should be considered as option. Here we process this case by moving a level upper.
    if (contains(firedStep, currentParentSteps))
    {
      currentPath.removeLast();
      currentSteps = currentPath.peekLast();
      currentParentSteps = (LinkedList) currentPath.get(currentPath.size() - 2);
    }
    
    // Here we know that all current steps are either executed(if PARALLEL) or one of them executed and others skipped(OPTIONAL or only one step)
    // So the last step of the currentSteps is truly the last step iterated
    Step curStep = currentSteps.getLast();
    // Again back to last optional step. To not confuse someone with the fact that step is it's one parent
    // we should get it's last child
    if (contains(curStep, currentParentSteps))
    {
      curStep = currentSteps.get(currentSteps.size() - 2);
      if (curStep == null)
      {
        throw new AssertionError();
      }
    }
    // Remove all the steps that are already executed
    currentPath.removeLast();
    
    // Now we're ready to find who of currentParentSteps is the real parent of curStep
    AtomicStep curParentStep = findParentStep(currentParentSteps, curStep);
    if (curParentStep == null)
    {
      throw new AssertionError();
    }
    
    List<StepContextHolder> options = prepareNextOptions(curStep, curParentStep, contextManager);
    
    return options;
  }
  
  private AtomicStep findParentStep(List<Step> currentParentSteps, Step curStep)
  {
    AtomicStep curParentStep = null;
    for (Step parentStepIter : currentParentSteps)
    {
      if (!(parentStepIter instanceof AtomicStep))
      {
        // When parent step is optional and followed by non-atomic step
        continue;
      }
      AtomicStep curParentStepIter = (AtomicStep) parentStepIter;
      List<Step> curParentStepChildren = curParentStepIter.listSteps();
      if (curParentStepChildren != null)
      {
        for (Step s : curParentStepChildren)
        {
          if (s == curStep) // We can't use list.contains() method since we need entity equals.
          {
            // Found!
            curParentStep = curParentStepIter;
            break;
          }
        }
      }
    }
    
    return curParentStep;
  }
  
  private boolean contains(Object o, Collection currentSteps)
  {
    for (Object s : currentSteps)
    {
      if (s == o)
      {
        return true;
      }
    }
    return false;
  }
  
  private List<StepContextHolder> prepareNextOptions(Step curStep, AtomicStep curParentStep, ContextManager contextManager)
  {
    List<StepContextHolder> options = new LinkedList();
    List<Step> curChildren = curParentStep.listSteps();
    
    String curConcurrency = null;
    
    // Iterate through the children list until the curStep found and check the current step to be the last
    boolean isSkippingSteps = true;
    for (Iterator<Step> i = curChildren.iterator(); i.hasNext();)
    {
      Step curChild = i.next();
      
      // After the this checking 'i' points to the next step to look at
      if (isSkippingSteps)
      {
        if (curChild == curStep)
        {
          isSkippingSteps = false;
          continue;
        }
        if (curStep == null)
        {
          isSkippingSteps = false;
        }
        else
        {
          continue;
        }
      }
      
      BeanEvaluator beanEvaluator = new BeanEvaluator(getEvaluator(contextManager));
      beanEvaluator.evaluate(curChild, curChild.getExpressions());
      
      boolean disabled = isStepDisabled(contextManager, curChild);
      if (disabled)
      {
        continue;
      }
      
      if (curConcurrency == null)
      {
        options.add(new StepContextHolder(curChild, getStepContext(curChild, false)));
        if (Step.CONCURRENCY_OPTIONAL.equalsIgnoreCase(curChild.getConcurrency()) || Step.CONCURRENCY_PARALLEL.equalsIgnoreCase(curChild.getConcurrency())
            || Step.CONCURRENCY_PARALLEL_OPTIONAL.equalsIgnoreCase(curChild.getConcurrency()))
        {
          if (Step.CONCURRENCY_PARALLEL_OPTIONAL.equalsIgnoreCase(curChild.getConcurrency()))
          {
            curConcurrency = Step.CONCURRENCY_PARALLEL;
          }
          else
          {
            curConcurrency = curChild.getConcurrency();
          }
        }
        else
        {
          break;
        }
      }
      else
      {
        if (curConcurrency.equalsIgnoreCase(curChild.getConcurrency()) || Step.CONCURRENCY_PARALLEL.equalsIgnoreCase(curConcurrency)
            && Step.CONCURRENCY_PARALLEL_OPTIONAL.equalsIgnoreCase(curChild.getConcurrency()))
        {
          options.add(new StepContextHolder(curChild, getStepContext(curChild, false)));
        }
        else if (curChild.getConcurrency() == null)
        {
          options.add(new StepContextHolder(curChild, getStepContext(curChild, false)));
          break;
        }
      }
      
      // If the last child has concurrency set to OPTIONAL we should add parent finishing as an option;
      if (!i.hasNext() && (Step.CONCURRENCY_OPTIONAL.equalsIgnoreCase(curChild.getConcurrency()) || Step.CONCURRENCY_PARALLEL_OPTIONAL.equalsIgnoreCase(curChild.getConcurrency())))
      {
        options.add(new StepContextHolder(curParentStep, getStepContext(curParentStep, true)));
      }
    }
    
    if (options.isEmpty())
    {
      // Now we've processed all the children but found nothing to execute
      // We should probably add curParent finishing step to the queue
      options.add(new StepContextHolder(curParentStep, getStepContext(curParentStep, true)));
    }
    else
    {
      LinkedList<Step> curOptions = new LinkedList();
      for (StepContextHolder elem : options)
      {
        curOptions.add(elem.step);
      }
      currentPath.add(curOptions);
    }
    
    return options;
  }
  
  private List<StepContextHolder> preparePreviousOptions(Step curStep, AtomicStep curParentStep, ContextManager contextManager)
  {
    List<StepContextHolder> options = new LinkedList();
    List<Step> curChildren = curParentStep.listSteps();
    
    String curConcurrency = null;
    
    // Iterate through the children list until the curStep found
    boolean isSkippingSteps = true;
    for (ListIterator<Step> i = curChildren.listIterator(curChildren.size()); i.hasPrevious();)
    {
      Step curChild = i.previous();
      
      // After the this checking 'i' points to the previous step to look at
      if (isSkippingSteps)
      {
        if (curChild == curStep)
        {
          isSkippingSteps = false;
          // A little trick here. We should skip the step if it is in pending state now
          // i.e. the player waits for the user to perform that step
          if (!contains(curChild, performedSteps))
          {
            continue;
          }
        }
        else if (curStep == null)
        {
          isSkippingSteps = false;
        }
        else
        {
          continue;
        }
      }
      
      BeanEvaluator beanEvaluator = new BeanEvaluator(getEvaluator(contextManager));
      beanEvaluator.evaluate(curChild, curChild.getExpressions());
      
      boolean disabled = isStepDisabled(contextManager, curChild);
      if (disabled)
      {
        continue;
      }
      
      if (options.isEmpty())
      {
        options.add(new StepContextHolder(curChild, getStepContext(curChild, false)));
        curConcurrency = curChild.getConcurrency();
      }
      else
      {
        if (curConcurrency == null)
        {
          if (Step.CONCURRENCY_OPTIONAL.equalsIgnoreCase(curChild.getConcurrency()) || Step.CONCURRENCY_PARALLEL.equalsIgnoreCase(curChild.getConcurrency())
              || Step.CONCURRENCY_PARALLEL_OPTIONAL.equalsIgnoreCase(curChild.getConcurrency()))
          {
            options.add(0, new StepContextHolder(curChild, getStepContext(curChild, false)));
            if (Step.CONCURRENCY_PARALLEL_OPTIONAL.equalsIgnoreCase(curChild.getConcurrency()))
            {
              curConcurrency = Step.CONCURRENCY_PARALLEL;
            }
            else
            {
              curConcurrency = curChild.getConcurrency();
            }
          }
          else
          {
            break;
          }
        }
        else
        {
          if (curConcurrency.equalsIgnoreCase(curChild.getConcurrency()) || curConcurrency.equals(Step.CONCURRENCY_PARALLEL)
              && Step.CONCURRENCY_PARALLEL_OPTIONAL.equalsIgnoreCase(curChild.getConcurrency()))
          {
            options.add(0, new StepContextHolder(curChild, getStepContext(curChild, false)));
          }
          else
          {
            break;
          }
        }
      }
    }
    
    if (!options.isEmpty())
    {
      LinkedList<Step> curOptions = new LinkedList();
      for (StepContextHolder elem : options)
      {
        curOptions.add(elem.step);
      }
      currentPath.add(curOptions);
    }
    
    return options;
  }
  
  private List<StepContextHolder> supplyWithContext(LinkedList<Step> currentSteps, boolean finishing)
  {
    List<StepContextHolder> result = new LinkedList();
    
    for (Step step : currentSteps)
    {
      result.add(new StepContextHolder(step, getStepContext(step, finishing)));
    }
    
    return result;
  }
  
  public ActionCommandContext getStepContext(Step step, boolean finishing)
  {
    if (macro == null)
    {
      return null;
    }
    
    ActionCommandContext ctx = new ActionCommandContext();
    
    getStepContext(ctx, macro, step);
    
    ctx.setFinishing(finishing);
    
    return ctx;
  }
  
  private boolean getStepContext(ActionCommandContext ctx, Step currentStep, Step requiredStep)
  {
    if (currentStep == requiredStep)
    {
      addContextInfo(ctx, currentStep);
      return true;
    }
    
    if (currentStep instanceof AtomicStep)
    {
      AtomicStep as = (AtomicStep) currentStep;
      List<Step> children = as.listSteps();
      for (Step child : children)
      {
        boolean flag = getStepContext(ctx, child, requiredStep);
        if (flag)
        {
          addContextInfo(ctx, currentStep);
          return true;
        }
      }
    }
    
    return false;
  }
  
  private void addContextInfo(ActionCommandContext ctx, Step step)
  {
    if (step instanceof ActionStep)
    {
      ActionStep s = (ActionStep) step;
      if (ctx.getActionRequestId() == null)
      {
        ctx.setActionRequestId(s.getActionRequestId());
      }
    }
    
    if (step instanceof InvokeOperationOperation)
    {
      InvokeOperationOperation s = (InvokeOperationOperation) step;
      if (ctx.getContext() == null)
      {
        ctx.setContext(s.getInvokerContext());
      }
      if (ctx.getOperationName() == null)
      {
        ctx.setOperationName(s.getOperationName());
      }
    }
    else if (step instanceof EditTableOperation)
    {
      EditTableOperation s = (EditTableOperation) step;
      if (ctx.getPropertyName() == null)
      {
        ctx.setOperationName(s.getPropertyName());
      }
    }
  }
  
  private Evaluator getEvaluator(ContextManager contextManager)
  {
    MacroReferenceResolver res = new MacroReferenceResolver(contextManager, macroVariables);
    Evaluator e = new Evaluator(res);
    
    return e;
  }
  
  private Object evaluateExpression(ContextManager contextManager, String expr) throws SyntaxErrorException
  {
    Evaluator e = getEvaluator(contextManager);
    
    try
    {
      return e.evaluate(new Expression(expr));
    }
    catch (Exception ex)
    {
      throw new SyntaxErrorException(ex.getMessage(), ex);
    }
  }
  
  private boolean isStepDisabled(ContextManager ctxManager, Step child)
  {
    boolean disabled = false;
    String enabledExpr = child.getEnabled();
    if (enabledExpr != null)
    {
      try
      {
        Object value = evaluateExpression(ctxManager, enabledExpr);
        if (value instanceof Boolean)
        {
          if (!(Boolean) value)
          {
            disabled = true;
          }
        }
        else
        {
          Log.GUIDE.warn("Error processing step \"enabled\" expression " + enabledExpr + " returned non-boolean, setting step \"enabled\"=true by default");
        }
      }
      catch (SyntaxErrorException ex)
      {
        Log.GUIDE.warn("Error processing step enabled expression " + enabledExpr + ", setting step enabled=true by default", ex);
      }
    }
    return disabled;
  }
  
}
