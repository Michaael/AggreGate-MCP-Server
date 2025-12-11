package com.tibbo.aggregate.client;

import java.util.*;

import com.tibbo.aggregate.client.device.*;
import com.tibbo.aggregate.client.gui.frame.*;
import com.tibbo.aggregate.component.*;

public class Workspace implements FrameDataStorage
{
  private boolean firstUse = true;
  private Integer lastNewVersionReported = null;
  private DeviceList deviceList = new DeviceList();
  private Map<String, Object> frameData = new Hashtable<String, Object>();
  
  private Map<String, byte[]> layoutData = new HashMap<String, byte[]>();
  private byte[] guiBuilderLayoutData;
  private byte[] processControlLayoutData;
  private byte[] shortcuts;
  
  private String commonDirectory;
  private String dataTableDirectory;
  private String propertiesDirectory;
  private String dataDirectory;
  
  private boolean preferModalDialogs = false;
  private boolean autoRun = true;
  
  public Workspace()
  {
    super();
  }
  
  public DeviceList getDeviceList()
  {
    return deviceList;
  }
  
  public Map<String, Object> getFrameData()
  {
    return frameData;
  }
  
  public boolean isFirstUse()
  {
    return firstUse;
  }
  
  public Integer getLastNewVersionReported()
  {
    return lastNewVersionReported;
  }
  
  public void setLastNewVersionReported(Integer lastNewVersionReported)
  {
    this.lastNewVersionReported = lastNewVersionReported;
  }
  
  public void setDeviceList(DeviceList deviceList)
  {
    this.deviceList = deviceList;
  }
  
  public void setFirstUse(boolean firstUse)
  {
    this.firstUse = firstUse;
  }
  
  public Map<String, byte[]> getLayoutData()
  {
    return layoutData;
  }
  
  @Override
  public byte[] getLayoutData(String key)
  {
    return layoutData.get(key);
  }
  
  @Override
  public void setLayoutData(String key, byte[] data)
  {
    layoutData.put(key, data);
  }
  
  public void setLayoutData(Map<String, byte[]> layoutData)
  {
    this.layoutData = layoutData;
  }
  
  public byte[] getGuiBuilderLayoutData()
  {
    return guiBuilderLayoutData;
  }
  
  public void setGuiBuilderLayoutData(byte[] guiBuilderLayoutData)
  {
    this.guiBuilderLayoutData = guiBuilderLayoutData;
  }
  
  public byte[] getProcessControlLayoutData()
  {
    return processControlLayoutData;
  }
  
  public void setProcessControlLayoutData(byte[] processControlLayoutData)
  {
    this.processControlLayoutData = processControlLayoutData;
  }
  
  public byte[] getShortcuts()
  {
    return shortcuts;
  }
  
  public void setShortcuts(byte[] shortcuts)
  {
    this.shortcuts = shortcuts;
  }
  
  @Override
  public Object getFrameData(String key)
  {
    return frameData.get(key);
  }
  
  @Override
  public void setFrameData(String key, Object data)
  {
    frameData.put(key, data);
  }
  
  public void setFrameData(Map<String, Object> frameData)
  {
    this.frameData = frameData;
  }
  
  public String getCommonDirectory()
  {
    return commonDirectory;
  }
  
  public void setCommonDirectory(String commonDirectory)
  {
    this.commonDirectory = commonDirectory;
  }
  
  public String getDataTableDirectory()
  {
    return dataTableDirectory;
  }
  
  public void setDataTableDirectory(String dataTableDirectory)
  {
    this.dataTableDirectory = dataTableDirectory;
  }
  
  public String getPropertiesDirectory()
  {
    return propertiesDirectory;
  }
  
  public void setPropertiesDirectory(String propertiesDirectory)
  {
    this.propertiesDirectory = propertiesDirectory;
  }
  
  public String getDataDirectory()
  {
    return dataDirectory;
  }
  
  public void setDataDirectory(String dataDirectory)
  {
    this.dataDirectory = dataDirectory;
  }
  
  public boolean isAutoRun()
  {
    return autoRun;
  }
  
  public void setAutoRun(boolean autoRun)
  {
    this.autoRun = autoRun;
  }
  
  public boolean isPreferModalDialogs()
  {
    return preferModalDialogs;
  }
  
  public void setPreferModalDialogs(boolean preferModalDialogs)
  {
    this.preferModalDialogs = preferModalDialogs;
    ComponentHelper.setPreferModalDialogs(preferModalDialogs);
  }
}
