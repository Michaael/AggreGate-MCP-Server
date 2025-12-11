package com.tibbo.aggregate.client.action.executor;

import java.io.*;

import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.macro.model.*;
import com.tibbo.aggregate.client.macro.persistence.*;
import com.tibbo.aggregate.client.macro.ui.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.action.command.*;
import com.tibbo.aggregate.common.datatable.*;

public class ShowGuideExecutor extends AbstractCommandExecutor
{
  private static final String MACRO_FILE_EXTENSION = ".xml";
  
  public ShowGuideExecutor()
  {
    super(ActionUtils.CMD_SHOW_GUIDE);
  }
  
  public GenericActionResponse execute(Operation originator, GenericActionCommand cmd)
  {
    DataRecord rec = cmd.getParameters().rec();
    String invokerContext = rec.getString(ShowGuide.CF_INVOKER_CONTEXT);
    String macroName = rec.getString(ShowGuide.CF_MACRO_NAME);
    
    Macro macro;
    try
    {
      if (!macroName.endsWith(MACRO_FILE_EXTENSION))
      {
        macroName += MACRO_FILE_EXTENSION;
      }
      macro = MacroStorageManager.getDefaultStorage().loadMacro(macroName);
    }
    catch (FileNotFoundException ex)
    {
      throw new IllegalArgumentException("No macro found with name " + macroName);
    }
    
    PlayerController playerController = MacroUIFacade.getDefault().startMacroPlayer(null, macro);
    playerController.getPlayer().setVariable(EventMacroPlayer.VAR_INVOKER_CONTEXT, invokerContext);
    
    return new GenericActionResponse(null, false, cmd.getRequestId());
  }
  
}
