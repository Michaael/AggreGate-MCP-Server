package com.tibbo.linkserver.plugin.context.models.rules;

import com.tibbo.aggregate.common.*;

public class RuleException extends AggreGateException
{
  public RuleException(String message)
  {
    super(message);
  }
  
  public RuleException(String message, String details)
  {
    super(message, details);
  }
  
  public RuleException(Throwable cause)
  {
    super(cause);
  }
  
  public RuleException(String message, Throwable cause)
  {
    super(message, cause);
  }
  
  public RuleException(String message, Throwable cause, String details)
  {
    super(message, cause, details);
  }
}
