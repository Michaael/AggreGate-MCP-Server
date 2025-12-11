package com.tibbo.aggregate.client.sandbox;

import java.io.*;

import com.tibbo.aggregate.client.gui.dialog.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.util.*;

public class TableDisplay
{
  
  public static void main(String[] args)
  {
    try
    {
      byte[] b = FileUtils.readFile(new File("c:\\data\\work\\tableEncoding\\offlineRawAlarmsNew.tbl"));
      
      DataTable t = new SimpleDataTable(new String(b, StringUtils.UTF8_CHARSET));
      
      DataTableEditorDialog.showData(null, "", t);
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
    
  }
  
}
