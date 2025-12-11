package com.tibbo.linkserver.plugin.context.models;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.tibbo.aggregate.common.Log;
import com.tibbo.aggregate.common.binding.Binding;
import com.tibbo.aggregate.common.binding.BindingEventsHelper;
import com.tibbo.aggregate.common.binding.ContextBindingProvider;
import com.tibbo.aggregate.common.binding.EvaluationOptions;
import com.tibbo.aggregate.common.binding.ExtendedBinding;
import com.tibbo.aggregate.common.context.CallerController;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextUtils;
import com.tibbo.aggregate.common.context.EntityDefinition;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.device.DisconnectionException;
import com.tibbo.aggregate.common.event.EventLevel;
import com.tibbo.aggregate.common.expression.DefaultReferenceResolver;
import com.tibbo.aggregate.common.expression.Reference;
import com.tibbo.aggregate.common.util.Util;
import com.tibbo.linkserver.event.EventHelper;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class ModelBindingProvider extends ContextBindingProvider
{
  private final ModelProcessor processor;
  private final List<ExtendedBinding> bindings;
  
  public ModelBindingProvider(ModelProcessor processor, DefaultReferenceResolver resolver, List<ExtendedBinding> bindings)
  {
    super(resolver, processor.getModelContext().getCallerController());
    this.processor = processor;
    this.bindings = bindings;
  }
  
  @Override
  public Map<Binding, EvaluationOptions> createBindings()
  {
    Map<Binding, EvaluationOptions> result = new LinkedHashMap<>();
    
    for (ExtendedBinding cur : bindings)
    {
      result.put(cur.getBinding(), cur.getEvaluationOptions());
    }
    
    return result;
  }
  
  @Override
  public CallerController select(Context context, String entity, int entityType)
  {
    EntityDefinition def;
    switch (entityType)
    {
      case ContextUtils.ENTITY_VARIABLE:
        def = context.getVariableDefinition(entity);
        break;
      case ContextUtils.ENTITY_FUNCTION:
        def = context.getFunctionDefinition(entity);
        break;
      case ContextUtils.ENTITY_EVENT:
        def = context.getEventDefinition(entity);
        break;
      default:
        throw new IllegalStateException("Unexpected entity type:  " + entityType);
    }
    
    if (def != null && def.getOwner() == getModelContext())
    {
      return context.getContextManager().getCallerController();
    }
    
    return super.select(context, entity, entityType);
  }
  
  @Override
  protected Integer getEngineHashCode()
  {
    return null;
  }
  
  @Override
  public void processError(Binding binding, int method, Reference cause, Exception error)
  {
    boolean disconnected = ExceptionUtils.indexOfType(error, DisconnectionException.class) != -1;
    
    String target = Util.equals(getModelContext(), processor.getTargetContext()) ? "" : processor.getTargetContext().toDetailedString() + " > ";
    
    String message = error.getMessage() != null ? error.getMessage() : error.toString();
    
    String fullMessage = target + "Binding error in '" + binding + "': " + message;
    
    if (disconnected)
    {
      Log.MODELS.debug(fullMessage, error);
    }
    else
    {
      Log.MODELS.debug(fullMessage, error);
      
      EventHelper.fireInfoEvent(getModelContext(), EventLevel.WARNING, target + message);
      
      String activator = cause == null ? null : cause.getImage();
      DataTable dt = BindingEventsHelper.createBindingErrorEventData(processor.getTargetContext(), binding, method, activator, error);
      getModelContext().fireEvent(BindingEventsHelper.E_BINDING_ERROR, EventLevel.WARNING, dt);
      
      if (processor.getTargetContext() != null)
      {
        EventHelper.fireInfoEvent(processor.getTargetContext(), EventLevel.WARNING, message);
      }
    }
  }
  
  @Override
  public void processExecution(int method, Binding binding, EvaluationOptions options, Reference cause, Object result)
  {
    super.processExecution(method, binding, options, cause, result);
    
    getModelContext().fireBindingQueueOverflowEventIfNeeded();
    
    if (getModelContext().getModel().isLogBindingsExecution())
    {
      final DataTable data = BindingEventsHelper.createBindingExecutionEventData(processor.getTargetContext(), method, binding, options, cause, result);
      
      getModelContext().fireEvent(BindingEventsHelper.E_BINDING_EXECUTION, EventLevel.WARNING, data);
    }
  }
  
  private ModelContext getModelContext()
  {
    return processor.getModelContext();
  }

  /**
   * Returns a list of ExtendedBindings. This method is primarily intended for testing purposes,
   * allowing convenient access to the data needed for manual activation of bindings in test scenarios.
   *
   * @return A list of ExtendedBinding objects.
   */
  @VisibleForTesting
  List<ExtendedBinding> getBindings() {
    return bindings;
  }

}
