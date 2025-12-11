package com.tibbo.aggregate.client.action.executor;

import java.awt.*;
import java.net.*;

import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.action.command.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.component.*;

public class BrowseExecutor extends AbstractCommandExecutor
{
  public BrowseExecutor()
  {
    super(ActionUtils.CMD_BROWSE);
  }
  
  public GenericActionResponse execute(Operation originator, GenericActionCommand cmd)
  {
    String uriText = cmd.getParameters().rec().getString(Browse.CF_BROWSE_URI);
    
    uriText = substitute(uriText);
    
    try
    {
      if (ComponentHelper.isSteadyStateMode())
      {
        Toolkit.getDefaultToolkit().beep();
        return null;
      }
      
      BrowseHelper.browse(new URI(uriText), ComponentHelper.getConfig().getHomeDirectory());
    }
    catch (Exception ex)
    {
      throw new IllegalStateException("Error opening url '" + uriText + "'", ex);
    }
    return null;
  }
  
  private String substitute(String uriText)
  {
    uriText = uriText.replace(Constants.ROOT_FOLDER_PATH_PATTERN, Cres.get().getString("urlProduct").replace("\\", "/"));
    
    return uriText;
  }
}
