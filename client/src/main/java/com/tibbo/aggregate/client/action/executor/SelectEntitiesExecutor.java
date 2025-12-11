package com.tibbo.aggregate.client.action.executor;

import java.util.*;

import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.gui.dialog.*;
import com.tibbo.aggregate.client.gui.dialog.EntitySelectorDialog.SelectionResult;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.action.command.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.expression.*;
import com.tibbo.aggregate.component.*;

public class SelectEntitiesExecutor extends AbstractCommandExecutor
{
  public SelectEntitiesExecutor()
  {
    super(ActionUtils.CMD_SELECT_ENTITIES);
  }
  
  public GenericActionResponse execute(Operation originator, GenericActionCommand cmd)
  {
    if (!(originator instanceof InvokeActionOperation))
    {
      throw new IllegalArgumentException("Unsupported originator class: " + originator.getClass() + " required " + InvokeActionOperation.class.getName());
    }
    
    InvokeActionOperation iop = (InvokeActionOperation) originator;
    
    String title = cmd.getTitle();
    DataRecord params = cmd.getParameters().rec();
    
    java.util.List<String> contextTypes = null;
    DataTable typesTable = params.getDataTable(SelectEntities.CF_TYPES);
    
    if (typesTable != null)
    {
      contextTypes = new LinkedList();
      for (DataRecord rec : typesTable)
      {
        contextTypes.add(rec.getString(SelectEntities.CF_TYPES_TYPE));
      }
    }
    
    String rootContext = params.getString(SelectEntities.CF_ROOT);
    String defaultContext = params.getString(SelectEntities.CF_DEFAULT);
    String expandedContext = params.getString(SelectEntities.CF_EXPANDED);
    boolean showChildren = params.getBoolean(SelectEntities.CF_SHOW_CHILDREN);
    boolean allowMasks = params.getBoolean(SelectEntities.CF_ALLOW_MASKS);
    boolean showVars = params.getBoolean(SelectEntities.CF_SHOW_VARS);
    boolean showFuncs = params.getBoolean(SelectEntities.CF_SHOW_FUNCS);
    boolean showEvents = params.getBoolean(SelectEntities.CF_SHOW_EVENTS);
    boolean showFields = params.getBoolean(SelectEntities.CF_SHOW_FIELDS);
    boolean singleSelection = params.getBoolean(SelectEntities.CF_SINGLE_SELECTION);
    boolean useCheckboxes = params.getBoolean(SelectEntities.CF_USE_CHECKBOXES);

    ContextManager cm = iop.getContext().getContextManager();
    
    Context root = iop.getContext().get(rootContext);
    Context def = iop.getContext().get(defaultContext);
    Context expanded = iop.getContext().get(expandedContext);
    
    if (root == null)
    {
      root = iop.getContext().get(iop.getContext().getPeerRoot());
    }
    
    if (root == null)
    {
      throw new IllegalStateException("Unable to get root context: " + rootContext);
    }
    
    SelectionResult res = EntitySelectorDialog.show(ComponentHelper.getMainFrame().getFrame(), title, cmd.isBatchEntry() && cmd.getRequestId() != null, cm, contextTypes, root, def, expanded,
        showChildren, allowMasks, showVars, showFuncs, showEvents, showFields, singleSelection, useCheckboxes);
    
    DataTable resultTable = new SimpleDataTable(SelectEntities.RFT_SELECT_ENTITIES);
    
    Reference[] selection = res.getSelection();
    
    if (selection != null)
    {
      for (int i = 0; i < selection.length; i++)
      {
        // Distributed: converting local directly connected server's paths to remote paths
        Context con = iop.getContext().getRoot().get(selection[i].getContext());
        
        if (con == null)
        {
          throw new IllegalStateException(Cres.get().getString("conNotAvail") + rootContext);
        }
        
        selection[i].setContext(con.getRemotePath());
        
        resultTable.addRecord().addString(selection[i].getImage());
      }
    }
    
    GenericActionResponse ar = new GenericActionResponse(resultTable, false, cmd.getRequestId());
    ar.setRemember(res.getSelectedOption() == OkCancelDialog.ALL_OPTION);
    return ar;
  }
}
