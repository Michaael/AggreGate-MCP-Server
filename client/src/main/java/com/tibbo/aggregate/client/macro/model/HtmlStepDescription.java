package com.tibbo.aggregate.client.macro.model;

public class HtmlStepDescription extends AbstractStepDescription
{
  private String body;
  
  public HtmlStepDescription(String body, boolean finishing)
  {
    super(finishing);
    this.body = body;
  }
  
  public String getBody()
  {
    
    return body;
  }
  
  public void setBody(String body)
  {
    
    this.body = body;
  }
}
