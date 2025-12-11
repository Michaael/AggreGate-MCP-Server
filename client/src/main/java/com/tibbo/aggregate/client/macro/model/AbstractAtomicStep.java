package com.tibbo.aggregate.client.macro.model;

import java.lang.reflect.*;
import java.util.*;

public abstract class AbstractAtomicStep<T extends Step> extends AbstractStep implements AtomicStep<T>
{
  LinkedList<T> steps = new LinkedList();
  
  public void addStep(T step) throws IllegalArgumentException
  {
    if (step == null)
    {
      throw new IllegalArgumentException("step is null");
    }
    
    Class stepClass = getTypeParameter();
    if (!stepClass.isInstance(step))
    {
      throw new IllegalArgumentException("step is not instance of " + stepClass.getName() + " actual class " + step.getClass().getName());
    }
    
    if (steps.contains(step))
    {
      throw new IllegalArgumentException("Attempt to add step twice: " + step);
    }
    
    steps.add(step);
  }
  
  private Class getTypeParameter() throws AssertionError
  {
    Class klass = this.getClass();
    while (klass.getGenericSuperclass() == null || !klass.getSuperclass().equals(AbstractAtomicStep.class))
    {
      klass = klass.getSuperclass();
      if (klass == null)
      {
        throw new AssertionError();
      }
    }
    Type[] params = ((ParameterizedType) klass.getGenericSuperclass()).getActualTypeArguments();
    if (params.length == 0)
    {
      return Step.class;
    }
    return (Class) params[0];
  }
  
  public void removeStep(T step)
  {
    steps.remove(step);
  }
  
  public List<T> listSteps()
  {
    return (List<T>) steps.clone();
  }
  
  public int getStepCount()
  {
    return steps.size();
  }
}
