package com.tibbo.aggregate.client.macro.model;

public class Macro extends AbstractAtomicStep<Step>
{
  private String fileName;
  
  public String toString()
  {
    return "Macro";
  }
  
  public void setFileName(String fileName)
  {
    this.fileName = fileName;
  }
  
  public String getFileName()
  {
    return fileName;
  }
}
