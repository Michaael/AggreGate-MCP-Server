package com.tibbo.aggregate.common.datatable.validator;

import java.text.*;
import java.util.regex.*;

import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;

public class RegexValidator extends AbstractFieldValidator
{
  private static final String SEPARATOR = "^^";
  private static final String SEPARATOR_REGEX = "\\^\\^";
  
  private String regex;
  private String message;
  
  public RegexValidator(String source)
  {
    String[] parts = source.split(SEPARATOR_REGEX);
    
    regex = parts[0];
    
    if (parts.length > 1)
    {
      message = parts[1];
    }
  }
  
  public RegexValidator(String regex, String message)
  {
    this.regex = regex;
    this.message = message;
  }
  
  public boolean shouldEncode()
  {
    return true;
  }
  
  public String encode()
  {
    return regex + (message != null ? SEPARATOR + message : "");
  }
  
  public Character getType()
  {
    return FieldFormat.VALIDATOR_REGEX;
  }
  
  public Object validate(Context context, ContextManager contextManager, CallerController caller, Object value) throws ValidationException
  {
    try
    {
      if (value != null && !value.toString().matches(regex))
      {
        throw new ValidationException(message != null ? message : MessageFormat.format(Cres.get().getString("dtValueDoesNotMatchPattern"), value, regex));
      }
    }
    catch (PatternSyntaxException ex)
    {
      throw new ValidationException(ex.getMessage(), ex);
    }
    
    return value;
  }
  
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((message == null) ? 0 : message.hashCode());
    result = prime * result + ((regex == null) ? 0 : regex.hashCode());
    return result;
  }
  
  public boolean equals(Object obj)
  {
    if (this == obj)
    {
      return true;
    }
    if (!super.equals(obj))
    {
      return false;
    }
    if (getClass() != obj.getClass())
    {
      return false;
    }
    RegexValidator other = (RegexValidator) obj;
    if (message == null)
    {
      if (other.message != null)
      {
        return false;
      }
    }
    else if (!message.equals(other.message))
    {
      return false;
    }
    if (regex == null)
    {
      if (other.regex != null)
      {
        return false;
      }
    }
    else if (!regex.equals(other.regex))
    {
      return false;
    }
    return true;
  }
}
