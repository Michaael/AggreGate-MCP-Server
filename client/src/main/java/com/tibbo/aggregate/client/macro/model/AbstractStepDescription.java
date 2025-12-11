package com.tibbo.aggregate.client.macro.model;

public abstract class AbstractStepDescription implements StepDescription
{
  private boolean onFinish;
  
  public AbstractStepDescription()
  {
  }
  
  public AbstractStepDescription(boolean onFinish)
  {
    this.onFinish = onFinish;
  }
  
  public boolean isOnFinish()
  {
    return onFinish;
  }
  
  public void setOnFinish(boolean onFinish)
  {
    this.onFinish = onFinish;
  }
}
