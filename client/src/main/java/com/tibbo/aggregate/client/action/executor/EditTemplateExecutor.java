package com.tibbo.aggregate.client.action.executor;

import javax.swing.*;
import java.awt.event.*;
import java.util.concurrent.*;

import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.guibuilder.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.client.util.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.action.command.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.common.widget.*;

public abstract class EditTemplateExecutor extends AbstractCommandExecutor
{
  protected AbstractAggreGateIDE abstractAggreGateIDE;
  
  private CountDownLatch sync;
  
  public EditTemplateExecutor(String type)
  {
    super(type);
  }
  
  @Override
  public void cancel()
  {
    new AggreGateSwingWorker()
    {
      @Override
      public void run()
      {
        if (abstractAggreGateIDE != null)
        {
          abstractAggreGateIDE.stop();
        }
      }
    }.execute();
    
    if (sync != null)
    {
      sync.countDown();
    }
  }
  
  @Override
  public GenericActionResponse execute(Operation originator, final GenericActionCommand cmd)
  {
    if (!(originator instanceof InvokeActionOperation))
    {
      throw new IllegalArgumentException("Unsupported originator class: " + originator.getClass() + " required " + InvokeActionOperation.class.getName());
    }
    
    final InvokeActionOperation iop = (InvokeActionOperation) originator;
    
    iop.addInterruptionListener(interruptionListener);
    
    try
    {
      DataRecord rec = cmd.getParameters().rec();
      final String template = rec.getString(EditTemplate.CF_WIDGET);
      final String defaultContext = rec.getString(EditTemplate.CF_DEFAULT_CONTEXT);
      final String widgetContext = rec.getString(EditTemplate.CF_WIDGET_CONTEXT);
      final int editMode = rec.getInt(EditTemplate.CF_EDIT_MODE);
      
      final ContextManager contextManager = iop.getContext().getContextManager();
      final String uniqueScriptPrefix = WidgetUtils.uniqueScriptPrefixFor(iop.getContext());
      
      String result = ActionUtils.RESPONSE_ERROR;
      String newWidget = null;
      
      sync = new CountDownLatch(1);
      
      SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          abstractAggreGateIDE = createAggreGateIDE(cmd.getTitle(), contextManager, iop, widgetContext, defaultContext, template, uniqueScriptPrefix, editMode);
          abstractAggreGateIDE.getMainFrame().addWindowListener(guiBuilderListener);
          
          try
          {
            byte[] layout = Client.getWorkspace().getGuiBuilderLayoutData();
            if (layout != null)
            {
              ClientUtils.applyLayoutData(abstractAggreGateIDE.getMainFrame().getDockingManager(), layout);
            }
          }
          catch (Exception ex1)
          {
            Log.GUIBUILDER.warn("Error loading layout data", ex1);
          }
          
          abstractAggreGateIDE.start();
        }
      });
      
      try
      {
        sync.await();
        if (abstractAggreGateIDE != null && abstractAggreGateIDE.isResultCode())
        {
          newWidget = abstractAggreGateIDE.getResultTemplate();
          result = ActionUtils.RESPONSE_SAVED;
        }
        else
        {
          result = ActionUtils.RESPONSE_CLOSED;
        }
      }
      catch (InterruptedException ex)
      {
        Log.CONTEXT_ACTIONS.error("Error while executing GUI Builder", ex);
        newWidget = abstractAggreGateIDE.getResultTemplate();
        result = ActionUtils.RESPONSE_SAVED;
      }
      
      abstractAggreGateIDE = null;
      
      DataTable resultTable = createResponseData(result, newWidget);
      
      GenericActionResponse actionResponse = new GenericActionResponse(resultTable, false /* No multiple form editing */, cmd.getRequestId());
      
      return actionResponse;
    }
    finally
    {
      iop.removeInterruptionListener(interruptionListener);
    }
  }
  
  public static DataTable createResponseData(String result, String widget)
  {
    // String result is [ActionUtils.RESP_SAVED, ActionUtils.RESP_CLOSED, ActionUtils.RESP_ERROR]
    ActionUtils.checkResponseCode(result);
    
    DataTable resultTable = new SimpleDataTable(EditTemplate.RFT_EDIT_WIDGET);
    DataRecord dr = resultTable.addRecord().addString(result);
    dr.addString(widget);
    return resultTable;
  }
  
  protected void stop()
  {
    try
    {
      byte[] layoutData = ClientUtils.getLayoutData(abstractAggreGateIDE.getMainFrame().getDockingManager());
      Client.getWorkspace().setProcessControlLayoutData(layoutData);
    }
    catch (Exception ex)
    {
      Log.GUIBUILDER.warn("Error saving layout data", ex);
    }
  }
  
  protected abstract AbstractAggreGateIDE createAggreGateIDE(final String title, final ContextManager contextManager, final InvokeActionOperation iop,
      final String widgetContext, final String defaultContext, final String template, final String uniqueScriptPrefix, int editMode);
  
  private final WindowListener guiBuilderListener = new WindowAdapter()
  {
    @Override
    public void windowClosed(WindowEvent e)
    {
      sync.countDown();
    }
  };
  
  private final Runnable interruptionListener = new Runnable()
  {
    @Override
    public void run()
    {
      if (abstractAggreGateIDE != null)
      {
        JOptionPane.showMessageDialog(abstractAggreGateIDE.getMainFrame(), "Widget editing action has been interrupted, saving of widget won't be possible!", Cres.get().getString("error"),
            JOptionPane.ERROR_MESSAGE);
      }
    }
  };
}
