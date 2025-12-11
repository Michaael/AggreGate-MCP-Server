package com.tibbo.aggregate.client.macro.model;

import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.util.*;

public class EditPropertiesOperation extends AbstractAtomicStep<EditTableCellOperation> implements ActionStep
{
  private String remoteContext;
  private String propertiesGroup;
  private String[] properties;
  private String actionRequestId;
  
  public EditPropertiesOperation()
  {
  }
  
  public EditPropertiesOperation(String actionRequestId, String remoteContext, String propertiesGroup)
  {
    this.actionRequestId = actionRequestId;
    this.remoteContext = remoteContext;
    this.propertiesGroup = propertiesGroup;
  }
  
  public EditPropertiesOperation(String actionRequestId, String remoteContext, String[] properties)
  {
    this.actionRequestId = actionRequestId;
    this.remoteContext = remoteContext;
    this.properties = properties;
  }
  
  public String getRemoteContext()
  {
    return remoteContext;
  }
  
  public String getPropertiesGroup()
  {
    return propertiesGroup;
  }
  
  public String[] getProperties()
  {
    return properties;
  }
  
  public String getActionRequestId()
  {
    return actionRequestId;
  }
  
  public void setRemoteContext(String remoteContext)
  {
    this.remoteContext = remoteContext;
  }
  
  public void setPropertiesGroup(String propertiesGroup)
  {
    this.propertiesGroup = propertiesGroup;
  }
  
  public void setProperties(String[] properties)
  {
    this.properties = properties;
  }
  
  public void setActionRequestId(String actionRequestId)
  {
    this.actionRequestId = actionRequestId;
  }
  
  public boolean stepEquals(Object o)
  {
    if (!getClass().isInstance(o))
    {
      return false;
    }
    
    EditPropertiesOperation op = (EditPropertiesOperation) o;
    
    if (remoteContext == null ? op.remoteContext != null : !ContextUtils.masksIntersect(remoteContext, op.remoteContext, false, false))
    {
      return false;
    }
    
    if (properties == null)
    {
      if (propertiesGroup == null ? op.propertiesGroup != null : !propertiesGroup.equals(op.propertiesGroup))
      {
        return false;
      }
    }
    else
    {
      if (op.properties == null || properties.length != op.properties.length)
      {
        return false;
      }
      
      for (int i = 0; i < properties.length; i++)
      {
        if (!ContextUtils.masksIntersect(properties[i], op.properties[i], false, false))
        {
          return false;
        }
      }
    }
    
    return true;
  }
  
  public String toString()
  {
    return "Edit properties of '" + remoteContext + "' " + (propertiesGroup != null ? "in group '" + propertiesGroup + "'" : " properties list " + StringUtils.print(properties));
  }
}
