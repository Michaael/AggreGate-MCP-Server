package com.tibbo.linkserver.plugin.context.models;

import static com.tibbo.aggregate.common.structure.PinpointFactory.newPinpointFor;

import java.util.List;
import java.util.Timer;
import java.util.concurrent.ExecutorService;

import com.google.common.annotations.VisibleForTesting;
import com.tibbo.aggregate.common.Log;
import com.tibbo.aggregate.common.binding.Bindings;
import com.tibbo.aggregate.common.binding.DefaultBindingProcessor;
import com.tibbo.aggregate.common.binding.ExtendedBinding;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.expression.DefaultReferenceResolver;
import com.tibbo.aggregate.common.expression.Evaluator;
import com.tibbo.aggregate.common.structure.Pinpoint;
import com.tibbo.aggregate.common.util.SyntaxErrorException;

public class ModelProcessor
{
  private final DefaultBindingProcessor bindingProcessor;

  private final ModelContext modelContext;
  
  private final Context targetContext;
  
  public ModelProcessor(ModelContext modelContext, Context targetContext, Timer timer, ExecutorService executorService) throws ContextException
  {
    this.modelContext = modelContext;
    this.targetContext = targetContext;
    
    try
    {
      DefaultReferenceResolver resolver = new DefaultReferenceResolver();
      resolver.setDefaultContext(targetContext);
      resolver.setContextManager(targetContext.getContextManager());
      resolver.setCallerController(modelContext.getCallerController());

      Evaluator evaluator = new Evaluator(resolver);
      
      DataTable bindingsVariable = modelContext.getVariable(ModelContext.V_BINDINGS,
          modelContext.getCallerController());

      Pinpoint origin = newPinpointFor(modelContext.getPath(), ModelContext.V_BINDINGS);
      List<ExtendedBinding> bindings = Bindings.bindingsFromDataTable(bindingsVariable, origin);
      ModelBindingProvider bindingProvider = new ModelBindingProvider(this, resolver, bindings);
      
      bindingProcessor = new DefaultBindingProcessor(bindingProvider, evaluator, timer, executorService);
    }
    catch (SyntaxErrorException ex)
    {
      throw new ContextException(ex);
    }
  }
  
  public void start()
  {
    Log.MODELS.debug("Starting model '" + modelContext.toDetailedString() + "' for context '" + targetContext.toDetailedString() + "'");
    bindingProcessor.start();
  }
  
  public void stop()
  {
    Log.MODELS.debug("Stopping model '" + modelContext.toDetailedString() + "' for context '" + targetContext.toDetailedString() + "'");
    if (bindingProcessor != null)
    {
      bindingProcessor.stop();
    }
  }
  
  public ModelContext getModelContext()
  {
    return modelContext;
  }

  /**
   * Returns the DefaultBindingProcessor associated with this object.
   * This method is primarily intended for internal use and may not be suitable for external use.
   *
   * @return The DefaultBindingProcessor object associated with this instance.
   */
  @VisibleForTesting
  public DefaultBindingProcessor getBindingProcessor()
  {
    return bindingProcessor;
  }
  
  public Context getTargetContext() {
    return targetContext;
  }
  
  public void setEnabled(boolean enabled)
  {
    bindingProcessor.setEnabled(enabled);
  }
}
