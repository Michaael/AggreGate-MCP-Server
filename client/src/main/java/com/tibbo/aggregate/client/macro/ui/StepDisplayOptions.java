package com.tibbo.aggregate.client.macro.ui;

public class StepDisplayOptions
{
  private boolean finishing;
  private boolean performed;
  
  public StepDisplayOptions()
  {
    super();
  }
  
  public StepDisplayOptions(boolean finishing)
  {
    this.finishing = finishing;
  }
  
  public void setFinishing(boolean finishing)
  {
    this.finishing = finishing;
  }
  
  public void setPerformed(boolean performed)
  {
    this.performed = performed;
  }
  
  public boolean isFinishing()
  {
    return finishing;
  }
  
  public boolean isPerformed()
  {
    return performed;
  }
}
