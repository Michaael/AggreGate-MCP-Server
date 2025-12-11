package com.tibbo.aggregate.client.macro;

import com.tibbo.aggregate.common.*;

public class MacroHelper
{
  private static String START = "$R{";
  private static String END = "}";
  
  public static String processResourceReferences(String source)
  {
    StringBuilder res = new StringBuilder();
    
    int index = 0;
    while (true)
    {
      int newIndex = source.indexOf(START, index);
      
      res.append(source.substring(index, newIndex != -1 ? newIndex : source.length()));
      
      if (newIndex == -1)
      {
        break;
      }
      
      index = newIndex;
      
      newIndex = source.indexOf(END, index);
      
      if (newIndex == -1)
      {
        break; // Corrupted input!
      }
      
      String resourceName = source.substring(index + START.length(), newIndex - END.length() + 1);
      
      String resourceValue = Cres.get().getString(resourceName);// $NON-NLS$
      
      res.append(resourceValue);
      
      index = newIndex + 1;
    }
    
    return res.toString();
  }
}
