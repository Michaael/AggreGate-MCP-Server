package com.tibbo.aggregate.client.device;

import com.tibbo.aggregate.common.datatable.*;

public class AlertInstanceData
{
  private String source;
  private int level;
  private String description;
  private String cause;
  private String message;
  private String trigger;
  private DataTable data;
  private String alertContextName;
  private Long alertEventId;
  private byte[] soundData;
  private boolean notifyOwner;
  private String notificationNecessityExpression;
  private Long lifetime;
  private boolean ackRequired;
  private DataTable actions;
  
  public AlertInstanceData()
  {
  }
  
  public String getSource()
  {
    return source;
  }
  
  public void setSource(String source)
  {
    this.source = source;
  }
  
  public int getLevel()
  {
    return level;
  }
  
  public void setLevel(int level)
  {
    this.level = level;
  }
  
  public String getDescription()
  {
    return description;
  }
  
  public void setDescription(String description)
  {
    this.description = description;
  }
  
  public String getCause()
  {
    return cause;
  }
  
  public void setCause(String cause)
  {
    this.cause = cause;
  }
  
  public String getMessage()
  {
    return message;
  }
  
  public void setMessage(String message)
  {
    this.message = message;
  }
  
  public String getTrigger()
  {
    return trigger;
  }
  
  public void setTrigger(String trigger)
  {
    this.trigger = trigger;
  }
  
  public DataTable getData()
  {
    return data;
  }
  
  public void setData(DataTable data)
  {
    this.data = data;
  }
  
  public String getAlertContextName()
  {
    return alertContextName;
  }
  
  public void setAlertContextName(String alertContextName)
  {
    this.alertContextName = alertContextName;
  }
  
  public Long getAlertEventId()
  {
    return alertEventId;
  }
  
  public void setAlertEventId(Long alertEventId)
  {
    this.alertEventId = alertEventId;
  }
  
  public byte[] getSoundData()
  {
    return soundData;
  }
  
  public void setSoundData(byte[] soundData)
  {
    this.soundData = soundData;
  }
  
  public boolean isNotifyOwner()
  {
    return notifyOwner;
  }
  
  public void setNotifyOwner(boolean notifyOwner)
  {
    this.notifyOwner = notifyOwner;
  }
  
  public String getNotificationNecessityExpression()
  {
    return notificationNecessityExpression;
  }
  
  public void setNotificationNecessityExpression(String notificationNecessityExpression)
  {
    this.notificationNecessityExpression = notificationNecessityExpression;
  }
  
  public Long getLifetime()
  {
    return lifetime;
  }
  
  public void setLifetime(Long lifetime)
  {
    this.lifetime = lifetime;
  }
  
  public boolean isAckRequired()
  {
    return ackRequired;
  }
  
  public void setAckRequired(boolean ackRequired)
  {
    this.ackRequired = ackRequired;
  }
  
  public DataTable getActions()
  {
    return actions;
  }
  
  public void setActions(DataTable actions)
  {
    this.actions = actions;
  }
}