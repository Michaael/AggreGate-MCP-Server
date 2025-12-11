package com.tibbo.aggregate.client.macro.model;

public class PlainTextDescription extends AbstractStepDescription
{
  private String text;
  
  public PlainTextDescription(String text)
  {
    this.text = text;
  }
  
  public PlainTextDescription(String text, boolean finishing)
  {
    super(finishing);
    this.text = text;
  }
  
  public String getText()
  {
    return text;
  }
  
  public void setText(String text)
  {
    this.text = text;
  }
}
