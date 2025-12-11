package com.tibbo.aggregate.common.protocol;

import java.util.*;
import java.util.concurrent.*;

public class CommandQueueManager
{
  private ExecutorService executor;
  
  private Map<String, CommandQueue> queues = new HashMap();
  
  public CommandQueueManager(ExecutorService executor)
  {
    this.executor = executor;
  }
  
  public synchronized void addCommand(String queueName, final CommandTask command)
  {
    final CommandQueue queue = createOrGetQueue(queueName);
    
    if (queue.getActivityLock().availablePermits() == 0)
    {
      queue.add(command);
    }
    else
    {
      submitCommand(queue, command);
    }
  }
  
  private CommandQueue createOrGetQueue(String name)
  {
    CommandQueue q = queues.get(name);
    
    if (q == null)
    {
      q = new CommandQueue();
      queues.put(name, q);
    }
    
    return q;
  }
  
  private void submitCommand(final CommandQueue queue, final CommandTask command)
  {
    try
    {
      queue.getActivityLock().acquire();
    }
    catch (InterruptedException ex)
    {
      throw new IllegalStateException(ex.getMessage(), ex);
    }
    
    executor.submit(new CommandTask()
    {
      @Override
      public Object call() throws Exception
      {
        try
        {
          Object res = command.call();
          
          while (true)
          {
            CommandTask cur = queue.poll();
            
            if (cur == null)
            {
              break;
            }
            
            cur.call();
          }
          
          return null;
        }
        finally
        {
          queue.getActivityLock().release();
        }
      }
    });
  }
}
