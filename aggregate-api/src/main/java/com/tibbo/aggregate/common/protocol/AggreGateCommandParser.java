package com.tibbo.aggregate.common.protocol;

import java.io.*;
import java.nio.channels.*;
import java.util.zip.*;

import com.tibbo.aggregate.common.communication.*;
import com.tibbo.aggregate.common.util.*;

public class AggreGateCommandParser extends SimpleCommandParser<IncomingAggreGateCommand>
{
  private static final byte TYPE_COMPRESSED = 1;
  
  public final byte[] decompressor_buffer = new byte[1024];
  private Inflater decompressor = new Inflater();
  
  protected CommandData commandData;
  private ProtocolVersion version = ProtocolVersion.V2;
  
  public AggreGateCommandParser(ReadableByteChannel channel)
  {
    this(channel, AggreGateCommand.START_CHAR, AggreGateCommand.END_CHAR);
  }
  
  public AggreGateCommandParser(ReadableByteChannel channel, byte startChar, byte endChar)
  {
    super(channel, startChar, endChar);
    commandData = new CommandData(startChar);
  }
  
  @Override
  protected IncomingAggreGateCommand buildCommand() throws SyntaxErrorException
  {
    if (ProtocolVersion.V2.equals(version))
      return super.buildCommand();
    
    IncomingAggreGateCommand commandFrom = buildCommandFrom(commandData);
    reset();
    return commandFrom;
  }
  
  @Override
  protected boolean commandEnded(byte cur)
  {
    if (ProtocolVersion.V2.equals(version))
      return super.commandEnded(cur);
    
    commandData.addNextByte(cur);
    return commandData.isCompleted();
  }
  
  @Override
  public void reset()
  {
    super.reset();
    if (commandData != null)
      commandData.reset();
  }
  
  @Override
  protected IncomingAggreGateCommand createCommandFromBufferContent() throws SyntaxErrorException
  {
    return new IncomingAggreGateCommand(clearData());
  }
  
  public void setVersion(ProtocolVersion version)
  {
    this.version = version;
  }
  
  protected IncomingAggreGateCommand buildCommandFrom(CommandData commandData) throws SyntaxErrorException
  {
    if (!commandData.isCompleted())
    {
      return null;
    }
    
    CommandData.Body body = commandData.getBody();
    
    byte[] data = body.getContents();
    
    boolean usesCompression = Util.equals(body.getType(), TYPE_COMPRESSED);
    
    if (usesCompression)
    {
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      decompressor.setInput(data);
      try
      {
        // decompressor.finished() - is buggy and always return false, so using getRemaining() here
        while (decompressor.getRemaining() != 0)
        {
          int bytesWritten = decompressor.inflate(decompressor_buffer);
          stream.write(decompressor_buffer, 0, bytesWritten);
        }
      }
      catch (DataFormatException e)
      {
        throw new SyntaxErrorException("Error decompressing command.");
      }
      
      decompressor.reset();
      
      data = stream.toByteArray();
    }
    
    return new IncomingAggreGateCommand(data);
  }
  
}
