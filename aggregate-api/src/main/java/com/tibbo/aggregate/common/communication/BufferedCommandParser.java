package com.tibbo.aggregate.common.communication;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import com.tibbo.aggregate.common.Cres;
import com.tibbo.aggregate.common.Log;
import com.tibbo.aggregate.common.device.DisconnectionException;
import com.tibbo.aggregate.common.util.SyntaxErrorException;
import com.tibbo.aggregate.common.util.WatchdogHolder;

public abstract class BufferedCommandParser<C extends Command> extends AbstractCommandParser<C>
{
  private static final int BUFFER_SIZE = 1024;
  
  protected final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
  protected ReadableByteChannel channel;
  
  public BufferedCommandParser(ReadableByteChannel channel)
  {
    super();
    this.channel = channel;
    buffer.flip();
  }
  
  protected boolean commandCompleted()
  {
    while (buffer.hasRemaining())
    {
      try
      {
        byte cur = buffer.get();
        
        if (commandEnded(cur))
        {
          return true;
        }
      }
      catch (BufferUnderflowException ex)
      {
        Log.COMMANDS.debug("Buffer underflow error in " + toString(), ex);
        return false;
      }
    }
    
    return false;
  }
  
  @Override
  public C readCommand() throws IOException, DisconnectionException, SyntaxErrorException
  {
    int read;
    
    if (commandCompleted())
    {
      return buildCommand();
    }
    
    while (true)
    {
      buffer.clear();

      WatchdogHolder.getInstance().awaitForEnoughMemory();

      read = getChannel().read(buffer);
      
      buffer.flip();
      
      if (read > 0)
      {
        CommandParserListener listener = getListener();
        if (listener != null)
        {
          listener.newDataReceived();
        }
      }
      else
      {
        break;
      }
      
      if (commandCompleted())
      {
        return buildCommand();
      }
    }
    
    if (read == -1)
    {
      getChannel().close();
      throw new DisconnectionException(Cres.get().getString("disconnected"));
    }
    
    return buildCommand();
  }
  
  protected abstract C buildCommand() throws SyntaxErrorException;
  
  protected abstract boolean commandEnded(byte cur);
  
  public ReadableByteChannel getChannel()
  {
    return channel;
  }
}
