package com.tibbo.linkserver.plugin.context.models;

import java.util.*;

import com.tibbo.aggregate.common.resource.*;
import com.tibbo.aggregate.common.util.*;

public class Lres
{
  private static ResourceBundle BUNDLE = ResourceAccessor.fetch(Lres.class, ResourceManager.getLocale(), Lres.class.getClassLoader());
  
  public static ResourceBundle get()
  {
    return BUNDLE;
  }
  
  public void reinit(Locale locale)
  {
    BUNDLE = ResourceAccessor.fetch(getClass(), locale);
  }
}