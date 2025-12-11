package com.tibbo.aggregate.client.action.executor;

import java.awt.*;

import com.jidesoft.diff.*;
import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.gui.dialog.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.action.command.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.component.*;

public class ShowDiffExecutor extends AbstractCommandExecutor
{
  public ShowDiffExecutor()
  {
    super(ActionUtils.CMD_SHOW_DIFF);
  }

  public GenericActionResponse execute(Operation originator, GenericActionCommand cmd)
  {
    DataRecord rec = cmd.getParameters().rec();

    CodeEditorDiffPane diffPane = new CodeEditorDiffPane();

    diffPane.setFromTitle(rec.getString(ShowDiff.CF_FIRST_FILE_TITLE));
    diffPane.setToTitle(rec.getString(ShowDiff.CF_SECOND_FILE_TITLE));

    diffPane.setFromText(rec.getString(ShowDiff.CF_FIRST_FILE));
    diffPane.setToText(rec.getString(ShowDiff.CF_SECOND_FILE));
    diffPane.diff();

    // UI params
    OkCancelDialog dialog = new OkCancelDialog(ComponentHelper.getMainFrame().getFrame(), cmd.getTitle(), true, false);

    dialog.setMainComponent(diffPane);
    dialog.setPreferredSize(new Dimension(750, 500));
    dialog.setMinimumSize(new Dimension(530, 300));
    dialog.run();

    return new GenericActionResponse(null, false, cmd.getRequestId());
  }
}
