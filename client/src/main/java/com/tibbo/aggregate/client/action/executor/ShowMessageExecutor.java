package com.tibbo.aggregate.client.action.executor;

import javax.swing.*;

import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.gui.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.action.command.*;
import com.tibbo.aggregate.common.event.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.component.*;

public class ShowMessageExecutor extends AbstractCommandExecutor
{
  private static final int MAX_LINE_LENGTH = 150;
  
  public ShowMessageExecutor()
  {
    super(ActionUtils.CMD_SHOW_MESSAGE);
  }
  
  public GenericActionResponse execute(Operation originator, GenericActionCommand cmd)
  {
    String title = cmd.getTitle();
    String message = cmd.getParameters().rec().getString(ShowMessage.CF_MESSAGE);
    int level = cmd.getParameters().rec().getFormat().hasField(ShowMessage.CF_LEVEL) ? cmd.getParameters().rec().getInt(ShowMessage.CF_LEVEL) : EventLevel.INFO;
    
    int jopLevel;
    switch (level)
    {
      case EventLevel.NONE:
        jopLevel = JOptionPane.PLAIN_MESSAGE;
        break;
      
      case EventLevel.NOTICE:
      case EventLevel.INFO:
        jopLevel = JOptionPane.INFORMATION_MESSAGE;
        break;
      
      case EventLevel.WARNING:
        jopLevel = JOptionPane.WARNING_MESSAGE;
        break;
      
      case EventLevel.ERROR:
      case EventLevel.FATAL:
      default:
        jopLevel = JOptionPane.ERROR_MESSAGE;
    }
    
    String finalMessage = StringUtils.wrapText(message, MAX_LINE_LENGTH, "\n");
    
    if (cmd.isBatchEntry())
    {
      OptionPane.showMessageDialog(ComponentHelper.getMainFrame(), finalMessage, title, jopLevel);
    }
    else
    {
      JOptionPane.showMessageDialog(ComponentHelper.getMainFrame(), finalMessage, title, jopLevel);
    }
    
    return new GenericActionResponse(null, false, cmd.getRequestId());
  }
}
