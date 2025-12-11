package com.tibbo.aggregate.common.protocol;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;

import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.encoding.*;
import com.tibbo.aggregate.common.util.*;
import org.json.simple.*;
import org.json.simple.parser.*;

public class IncomingAggreGateCommand extends AggreGateCommand
{
  private static final String EMPTY_ID = "";
  
  protected List<StringWrapper> parameters = null;
  
  private boolean jsonBody = false;
  
  public IncomingAggreGateCommand(byte[] data) throws SyntaxErrorException
  {
    super();
    write(data, 0, data.length);
    parse();
  }
  
  public IncomingAggreGateCommand(String str) throws SyntaxErrorException
  {
    super();
    write(str.getBytes(), 0, str.getBytes().length);
    parse();
  }
  
  public IncomingAggreGateCommand(ByteArrayOutputStream s) throws SyntaxErrorException
  {
    super();
    write(s.toByteArray(), 0, s.size());
    parse();
  }
  
  protected void parse() throws SyntaxErrorException
  {
    if (isContentEmpty())
    {
      throw new SyntaxErrorException("Zero length command received");
    }
    
    if (buf.length > 0 && buf[0] == JSON_START_ARRAY_SEPARATOR)
    {
      jsonBody = true;
      parseJsonCommand();
    }
    else
    {
      if (buf.length < TransferEncodingHelper.LARGE_DATA_SIZE)
      {
        parameters = StringWrapper.split(getContent(), AggreGateCommand.CLIENT_COMMAND_SEPARATOR.charAt(0));
      }
      else
      {
        parseBigCommand();
      }
    }
  }
  
  private void parseJsonCommand() throws SyntaxErrorException
  {
    parameters = new LinkedList();
    JSONParser json = new JSONParser();
    try
    {
      Object result = json.parse(getContent());
      if (result instanceof JSONArray)
      {
        for (Object o : ((JSONArray) result))
          parameters.add(StringWrapper.valueOf(o.toString()));
      }
      else
      {
        throw new SyntaxErrorException("Unknown JSON object received");
      }
    }
    catch (ParseException ex)
    {
      Log.COMMANDS.error(ex.getMessage(), ex);
    }
  }
  
  private void parseBigCommand()
  {
    ByteBuffer b = ByteBuffer.wrap(buf);
    parameters = new ArrayList<>();
    
    int position = 0;
    Integer end = null;
    
    while (b.hasRemaining())
    {
      char currentChar = (char) b.get();
      if (currentChar == AggreGateCommand.CLIENT_COMMAND_SEPARATOR.charAt(0))
      {
        parameters.add(StringWrapper.valueOf(new String(buf, position, b.position() - position - 1, StringUtils.UTF8_CHARSET)));
        position = b.position();
      }
      else
      {
        if (currentChar == Character.MIN_VALUE && end == null)
        {
          end = b.position() - 1;
        }
        
        if (currentChar != Character.MIN_VALUE && end != null)
        {
          end = null;
        }
      }
      
      if (!b.hasRemaining())
      {
        if (end == null)
          end = buf.length;
  
        parseDataUpdated(position);
      }
    }
  }
  
  private void parseDataUpdated(Integer position)
  {
    try
    {
      String parameter = new String(buf, position, buf.length - position, StandardCharsets.UTF_8);
      parameters.add(StringWrapper.valueOf(parameter));
    }
    catch (OutOfMemoryError | Exception ex)
    {
      Log.COMMANDS.warn(ex.getMessage(), ex);
    }
  }
  
  public int getNumberOfParameters()
  {
    return parameters != null ? parameters.size() : 0;
  }
  
  public boolean hasParameter(int number)
  {
    return number < getNumberOfParameters();
  }
  
  public StringWrapper getParameter(int number)
  {
    if (number > getNumberOfParameters())
    {
      throw new IllegalArgumentException("Trying to access parameter #" + number + " of command that has only " + getNumberOfParameters() + " parameters");
    }
    else
    {
      return parameters.get(number);
    }
  }
  
  public boolean isReply()
  {
    final StringWrapper param = getParameter(0);
    if (param.length() > 1)
    {
      throw new IllegalStateException("Invalid command type: " + param);
    }
    
    return (param.charAt(0) == AggreGateCommand.COMMAND_CODE_REPLY);
  }
  
  public boolean isMessage()
  {
    final StringWrapper param = getParameter(0);
    if (param.length() > 1)
    {
      throw new IllegalStateException("Invalid command type: " + param);
    }
    
    return (param.charAt(0) == AggreGateCommand.COMMAND_CODE_MESSAGE);
  }
  
  public String getReplyCode()
  {
    if (!isReply())
    {
      throw new UnsupportedOperationException("Command is not a reply");
    }
    
    return getParameter(AggreGateCommand.INDEX_REPLY_CODE).getString();
  }
  
  public StringWrapper getMessageCode()
  {
    if (!isMessage())
    {
      throw new UnsupportedOperationException("Command is not a message");
    }
    
    return getParameter(AggreGateCommand.INDEX_MESSAGE_CODE);
  }
  
  public String getEncodedDataTable(int index) throws ContextException, SyntaxErrorException
  {
    return getParameter(index).getString();
  }
  
  public String getEncodedDataTableFromReply() throws ContextException, SyntaxErrorException
  {
    if (!isReply())
    {
      throw new UnsupportedOperationException("Command is not a reply");
    }
    
    return getEncodedDataTable(AggreGateCommand.INDEX_DATA_TABLE_IN_REPLY);
  }
  
  public String getEncodedDataTableFromOperationMessage() throws ContextException, SyntaxErrorException
  {
    if (!isMessage())
    {
      throw new UnsupportedOperationException("Command is not a message");
    }
    
    return getEncodedDataTable(AggreGateCommand.INDEX_OPERATION_MESSAGE_DATA_TABLE);
  }
  
  public String getEncodedDataTableFromEventMessage() throws ContextException, SyntaxErrorException
  {
    if (!isMessage())
    {
      throw new UnsupportedOperationException("Command is not a message");
    }
    
    return getEncodedDataTable(AggreGateCommand.INDEX_EVENT_DATA_TABLE);
  }
  
  public String getQueueName()
  {
    if (!isMessage() || getMessageCode().charAt(0) != AggreGateCommand.MESSAGE_CODE_OPERATION)
    {
      return null;
    }
    
    char opcode = getParameter(AggreGateCommand.INDEX_OPERATION_CODE).charAt(0);
    if (opcode != AggreGateCommand.COMMAND_OPERATION_SET_VAR && opcode != AggreGateCommand.COMMAND_OPERATION_CALL_FUNCTION)
    {
      return null;
    }
    
    if (hasParameter(AggreGateCommand.INDEX_OPERATION_MESSAGE_QUEUE_NAME))
    {
      return TransferEncodingHelper.decode(getParameter(AggreGateCommand.INDEX_OPERATION_MESSAGE_QUEUE_NAME));
    }
    
    return null;
  }
  
  public String getFlags()
  {
    if (!isMessage() || getMessageCode().charAt(0) != AggreGateCommand.MESSAGE_CODE_OPERATION)
    {
      return null;
    }
    
    char opcode = getParameter(AggreGateCommand.INDEX_OPERATION_CODE).charAt(0);
    if (opcode != AggreGateCommand.COMMAND_OPERATION_CALL_FUNCTION)
    {
      return null;
    }
    
    if (hasParameter(AggreGateCommand.INDEX_OPERATION_MESSAGE_FLAGS))
    {
      return TransferEncodingHelper.decode(getParameter(AggreGateCommand.INDEX_OPERATION_MESSAGE_FLAGS));
    }
    
    return null;
  }
  
  @Override
  public String getId()
  {
    return getNumberOfParameters() > AggreGateCommand.INDEX_ID ? getParameter(AggreGateCommand.INDEX_ID).getString() : EMPTY_ID;
  }
  
  @Override
  public boolean isAsync()
  {
    return isMessage();
  }
  
  public boolean isJsonBody()
  {
    return jsonBody;
  }
}
