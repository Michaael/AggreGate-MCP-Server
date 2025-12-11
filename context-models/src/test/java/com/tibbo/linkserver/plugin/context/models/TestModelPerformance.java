package com.tibbo.linkserver.plugin.context.models;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Ignore;
import org.junit.Test;

import com.tibbo.aggregate.common.Log;
import com.tibbo.aggregate.common.binding.Bindings;
import com.tibbo.aggregate.common.context.AbstractContext;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.linkserver.config.DefaultServerConfig;

public class TestModelPerformance extends BaseModelContextTest
{
  @Override
  protected Model prepareModel()
  {
    Model model = new Model("model", "Model");
    model.setType(Model.TYPE_ABSOLUTE);
    return model;
  }
  
  private Model prepareModel(int number)
  {
    Model model = new Model("model_" + number, "Model");
    model.setType(Model.TYPE_ABSOLUTE);
    return model;
  }
  
  @Override
  protected void patchConfig(DefaultServerConfig config)
  {
    super.patchConfig(config);
    config.setExtCoreContextOperationThreads(100);
  }
  
  @Ignore
  @Test
  public void testPerformanceModel() throws Exception
  {
    long start = System.currentTimeMillis();
    ExecutorService serve = Executors.newFixedThreadPool(100);
    List<Future<String>> tasks = new LinkedList<>();
    for (int i = 0; i < 1; i++)
    {
      int finalI = i;
      tasks.add(serve.submit(() -> {
        try
        {
          long localstart = System.currentTimeMillis();
          if (finalI % 500 == 0)
          {
            Log.DEBUG.warn("i = " + finalI + " start" + localstart);
          }
          Model model = prepareModel(finalI);
          ModelContext context = createModelContext(model);
          if (finalI % 500 == 0)
          {
            Log.DEBUG.warn("createModel i = " + finalI + " take = " + (System.currentTimeMillis() - localstart));
          }
          localstart = System.currentTimeMillis();
          DataRecord events = new DataRecord(ModelContext.VFT_MODEL_EVENTS);
          events.setValue(AbstractContext.FIELD_ED_NAME, "test");
          context.setVariable(ModelContext.V_MODEL_EVENTS, getCallerController(), events.wrap());
          
          if (finalI % 500 == 0)
          {
            Log.DEBUG.warn("setEvents i = " + finalI + " take = " + (System.currentTimeMillis() - localstart));
          }
          localstart = System.currentTimeMillis();
          DataRecord bindings = new DataRecord(ModelContext.VFT_BINDINGS);
          bindings.setValue(Bindings.FIELD_EXPRESSION, "sleep(100)");
          bindings.setValue(Bindings.FIELD_ONSTARTUP, Boolean.FALSE);
          bindings.setValue(Bindings.FIELD_ONEVENT, Boolean.TRUE);
          bindings.setValue(Bindings.FIELD_PERIODICALLY, Boolean.FALSE);
          bindings.setValue(Bindings.FIELD_ACTIVATOR, ".:" + model.getName() + "@");
          context.setVariable(ModelContext.V_BINDINGS, getCallerController(), bindings.wrap());
          
          if (finalI % 500 == 0)
          {
            Log.DEBUG.warn("setBindings i = " + finalI + " take = " + (System.currentTimeMillis() - localstart));
          }
        }
        catch (Exception ex)
        {
          Log.DEBUG.warn(ex);
        }
        return null;
      }));
    }
    tasks.forEach(v -> {
      try
      {
        v.get();
      }
      catch (InterruptedException | ExecutionException e)
      {
        Log.DEBUG.warn(e);
      }
    });
    Log.DEBUG.warn("time = " + (System.currentTimeMillis() - start));
  }
}
