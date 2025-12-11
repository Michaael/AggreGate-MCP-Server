package com.tibbo.aggregate.client.action.executor;

import java.io.*;

import javax.swing.*;

import com.tibbo.aggregate.client.gui.frame.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.swing.*;

import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.action.command.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.resource.*;
import com.tibbo.aggregate.common.server.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.component.*;

public class ShowReportExecutor extends AbstractCommandExecutor
{
  private static final String FRAME_PREFIX = "report";
  
  public ShowReportExecutor()
  {
    super(ActionUtils.CMD_SHOW_REPORT);
  }
  
  @Override
  public GenericActionResponse execute(Operation originator, final GenericActionCommand cmd)
  {
    DataTable params = cmd.getParameters();
    
    DataRecord rec = cmd.getParameters().rec();
    
    byte[] reportData = null;
    if (params.getRecordCount() > 0)
    {
      reportData = rec.getData(ShowReport.CF_REPORT_DATA).getData();
    }
    
    DataTable locationData = rec.getDataTable(ShowReport.CF_LOCATION);
    final WindowLocation location = locationData != null ? new WindowLocation(locationData.rec()) : null;
    
    DataTable dashboardData = rec.hasField(ShowReport.CF_DASHBOARD) ? rec.getDataTable(ShowReport.CF_DASHBOARD) : null;
    final DashboardProperties dashboard = dashboardData != null ? new DashboardProperties(dashboardData.rec()) : null;
    
    DataTable dhInfoData = rec.hasField(ShowReport.CF_DASHBOARDS_HIERARCHY_INFO) ? rec.getDataTable(ShowReport.CF_DASHBOARDS_HIERARCHY_INFO) : null;
    final DashboardsHierarchyInfo dhInfo = dhInfoData != null ? new DashboardsHierarchyInfo(dhInfoData.rec()) : null;
    
    final String key = rec.getFormat().hasField(ShowReport.CF_KEY) ? rec.getString(ShowReport.CF_KEY) : null;
    
    final JRViewer viewer;
    try
    {
      JasperReportsContext jasperReportsContext = DefaultJasperReportsContext.getInstance();
      jasperReportsContext.setProperty("net.sf.jasperreports.default.font.name", ReportContextConstants.JASPER_REPORTS_FONTS_PACK_FAMILY);
      
      viewer = new JRViewer(new ByteArrayInputStream(reportData), false);
    }
    catch (JRException ex)
    {
      throw new IllegalStateException(ex.getMessage(), ex);
    }
    
    SwingUtilities.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        String frameKey = (key != null && !key.isEmpty()) ? key : ExecutionHelper.createFrameKey(location, FRAME_PREFIX + Util.descriptionToName(cmd.getTitle()));
        InternalFrame frame = InternalFrame.create(frameKey, cmd.getTitle(), new DefaultDmfComponent(viewer), true, false, location, dashboard, Docs.CL_REPORT_VIEWER, null, dhInfo);
        frame.setFrameIcon(ResourceManager.getImageIcon(Icons.ST_REPORT));
      }
    });
    
    return new GenericActionResponse(null, false, cmd.getRequestId());
  }
}
