package com.tibbo.aggregate.common.protocol;

import java.io.*;
import java.util.*;

import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.datatable.encoding.*;
import com.tibbo.aggregate.common.util.*;

public class OutgoingAggreGateCommand extends AggreGateCommand
{
  private static final byte[] CLIENT_COMMAND_SEPARATOR = AggreGateCommand.CLIENT_COMMAND_SEPARATOR.getBytes();
  
  protected int paramCount = 0;
  
  protected String id;
  
  protected boolean async;
  
  public OutgoingAggreGateCommand()
  {
    super();
  }
  
  @Override
  public String header()
  {
    return Character.toString((char) AggreGateCommand.START_CHAR);
  }
  
  @Override
  public String footer()
  {
    return Character.toString((char) AggreGateCommand.END_CHAR);
  }
  
  public OutgoingAggreGateCommand addParam(String param)
  {
    if (paramCount != 0)
    {
      write(CLIENT_COMMAND_SEPARATOR, 0, CLIENT_COMMAND_SEPARATOR.length);
    }
    
    if (paramCount == AggreGateCommand.INDEX_ID)
    {
      this.id = param;
    }
    
    try
    {
      if (param.length() > TransferEncodingHelper.LARGE_DATA_SIZE)
      {
        writeLargeData(param);
      }
      else
      {
        byte[] paramBytes = param.getBytes(StringUtils.UTF8_CHARSET);
        if (size() + paramBytes.length > buf.length)
          buf = Arrays.copyOf(buf, buf.length + paramBytes.length);
        write(paramBytes);
      }
    }
    catch (Exception ex)
    {
      Log.COMMANDS.warn(ex.getMessage(), ex);
    }
    
    paramCount++;
    return this;
  }
  
  private void writeLargeData(String param) throws IOException
  {
    Integer estimatedSize = estimateSize(param);
    buf = Arrays.copyOf(buf, buf.length + estimatedSize);
    for (int i = 0; i < param.length(); i += TransferEncodingHelper.MB)
    {
      int end = i + (TransferEncodingHelper.MB);
      if (end > param.length())
        end = param.length();
      String subString = param.substring(i, end);
      write(subString.getBytes(StringUtils.UTF8_CHARSET));
    }
  }
  
  private Integer estimateSize(String param)
  {
    Integer bufferLength = 0;
    for (int i = 0; i < param.length(); i += TransferEncodingHelper.MB)
    {
      int end = i + (TransferEncodingHelper.MB);
      if (end > param.length())
        end = param.length();
      String subString = param.substring(i, end);
      bufferLength += subString.getBytes(StringUtils.UTF8_CHARSET).length;
      
    }
    return bufferLength;
  }
  
  @Override
  public String getId()
  {
    return id;
  }
  
  public void setAsync(boolean async)
  {
    this.async = async;
  }
  
  @Override
  public boolean isAsync()
  {
    return async;
  }
  
  public void constructReply(String id, String code)
  {
    if (paramCount > 0)
    {
      throw new IllegalStateException("Can't construct reply - parameters already added to command");
    }
    
    addParam(String.valueOf(AggreGateCommand.COMMAND_CODE_REPLY));
    addParam(id);
    addParam(code);
  }
  
  public void constructReply(String id, String code, String message)
  {
    constructReply(id, code);
    addParam(TransferEncodingHelper.encode(message));
  }
  
  public void constructReply(String id, String code, String message, String details)
  {
    constructReply(id, code, message);
    addParam(TransferEncodingHelper.encode(details));
  }
  
  public void constructEvent(String context, String name, int level, String encodedDataTable, Long eventId, Date creationtime, Integer listener)
  {
    this.id = new String();
    
    setAsync(true);
    
    addParam(String.valueOf(AggreGateCommand.COMMAND_CODE_MESSAGE));
    addParam(this.id);
    addParam(String.valueOf(AggreGateCommand.MESSAGE_CODE_EVENT));
    addParam(context);
    addParam(name);
    addParam(String.valueOf(level));
    addParam(eventId != null ? eventId.toString() : "");
    addParam(listener != null ? listener.toString() : "");
    addParam(encodedDataTable);
    addParam(creationtime != null ? String.valueOf(creationtime.getTime()) : "");
  }
}
