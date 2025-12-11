package com.tibbo.aggregate.client.macro.model;

import java.util.*;

public abstract class AbstractStep implements Step
{
  private String title;
  private LinkedHashSet<StepDescription> descriptions = new LinkedHashSet();
  private String enabled;
  private String concurrency;
  private Map<String, String> expressions;
  
  public void addDescription(StepDescription desc)
  {
    if (desc == null)
    {
      throw new IllegalArgumentException("desc is null");
    }
    
    descriptions.add(desc);
  }
  
  public void removeDescription(Class descriptionClass, boolean finishing)
  {
    for (Iterator i = descriptions.iterator(); i.hasNext();)
    {
      StepDescription desc = (StepDescription) i.next();
      if (descriptionClass.isInstance(desc) && desc.isOnFinish() == finishing)
      {
        i.remove();
      }
    }
  }
  
  public void removeDescription(StepDescription desc)
  {
    descriptions.remove(desc);
  }
  
  public Set<StepDescription> getDescriptions()
  {
    return (Set<StepDescription>) descriptions.clone();
  }
  
  public void setDescriptions(Set<StepDescription> descs)
  {
    this.descriptions.clear();
    this.descriptions.addAll(descs);
  }
  
  public StepDescription getDescription(Class descriptionClass, boolean finishing)
  {
    for (StepDescription d : descriptions)
    {
      if (descriptionClass.isInstance(d) && d.isOnFinish() == finishing)
      {
        return d;
      }
    }
    
    return null;
  }
  
  public String getTitle()
  {
    return title;
  }
  
  public void setTitle(String title)
  {
    this.title = title;
  }
  
  public String getEnabled()
  {
    return enabled;
  }
  
  public void setEnabled(String expr)
  {
    this.enabled = expr;
  }
  
  public String getConcurrency()
  {
    return concurrency;
  }
  
  public void setConcurrency(String c)
  {
    if (c != null && !CONCURRENCY_OPTIONAL.equals(c) && !CONCURRENCY_PARALLEL.equals(c) && !CONCURRENCY_PARALLEL_OPTIONAL.equals(c))
    {
      throw new IllegalArgumentException("Illegal value for concurrency: " + c);
    }
    
    this.concurrency = c;
  }
  
  public Map<String, String> getExpressions()
  {
    return expressions;
  }
  
  public void setExpressions(Map<String, String> expressions)
  {
    this.expressions = expressions;
  }
  
  public boolean stepEquals(Object o)
  {
    if (!getClass().isInstance(o))
    {
      return false;
    }
    
    return toString().equals(o.toString());
  }
  
  public String toString()
  {
    return title == null ? "Macro step" : title;
  }
  
  public int hashCode()
  {
    return toString().hashCode();
  }
  
  public boolean isDescription()
  {
    return false;
  }
}
