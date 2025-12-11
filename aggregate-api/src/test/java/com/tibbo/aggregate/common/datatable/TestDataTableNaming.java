package com.tibbo.aggregate.common.datatable;

import com.tibbo.aggregate.common.tests.*;

public class TestDataTableNaming extends CommonsTestCase
{
  private DataTable dataTable = null;
  
  protected void setUp() throws Exception
  {
    super.setUp();
    final TableFormat rf = new TableFormat();
    rf.addField("<f1><S>");
    rf.addField("<f2><S>");
    rf.addField("<b><B>");
    rf.setNamingExpression("{f1} + ' ' + ({b}?{f1}:{f2})");
    dataTable = new SimpleDataTable(rf);
  }
  
  protected void tearDown() throws Exception
  {
    dataTable = null;
    super.tearDown();
  }
  
  public void testGetDescription()
  {
    dataTable.addRecord().addString("field1").addString("field2");
    
    String expectedReturn = "field1 field2";
    String actualReturn = dataTable.getDescription();
    assertEquals("return value", expectedReturn, actualReturn);
    
    dataTable.rec().setValue("b", true);
    expectedReturn = "field1 field1";
    actualReturn = dataTable.getDescription();
    assertEquals("return value", expectedReturn, actualReturn);
  }
  
}
