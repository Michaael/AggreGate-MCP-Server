package com.tibbo.aggregate.common.expression.function;

import java.util.*;

import com.tibbo.aggregate.common.expression.*;
import com.tibbo.aggregate.common.expression.function.date.*;
import com.tibbo.aggregate.common.tests.*;
import org.junit.Test;

public class TestDateTimeFunctions extends CommonsTestCase
{
  
  private Evaluator ev;
  
  public void setUp() throws Exception
  {
    super.setUp();
    ev = CommonsFixture.createTestEvaluator();
  }
  
  public void testParseDateFunction() throws Exception
  {
    Date res = (Date) new ParseDateFunction().execute(ev, null, "2011-11-11 11:11:11", "yyyy-MM-dd HH:mm:ss");
    
    GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
    
    c.setTime(res);
    
    assertEquals(2011, c.get(GregorianCalendar.YEAR));
    assertEquals(10, c.get(GregorianCalendar.MONTH)); // Zero-based!
    assertEquals(11, c.get(GregorianCalendar.DAY_OF_MONTH));
    assertEquals(11, c.get(GregorianCalendar.HOUR));
    assertEquals(11, c.get(GregorianCalendar.MINUTE));
    assertEquals(11, c.get(GregorianCalendar.SECOND));
  }
  
  public void testCalendarFunctionWithTimeZone() throws Exception
  {
    Date res = (Date) new ParseDateFunction().execute(ev, null, "2011-11-11 11:11:11", "yyyy-MM-dd HH:mm:ss", "GMT");
    
    GregorianCalendar c = new GregorianCalendar();
    
    c.setTime(res);
    
    assertEquals(2011, c.get(GregorianCalendar.YEAR));
    assertEquals(TimeZone.getDefault(), c.getTimeZone());
  }
  
  @Test
  public void testDateDiffFunction() throws Exception
  {
    Date firstDate = (Date) ev.evaluate(new Expression("date(2019,7,31,12,0,0,0)"));
    assertNotNull(firstDate);
    Date secondDate = (Date) ev.evaluate(new Expression("date(2019,7,1,12,0,0,0)"));
    assertNotNull(secondDate);
    
    long difference = (long) new DateDiffFunction().execute(ev, null, firstDate, secondDate, "month");
    assertEquals(difference, 0);
    
    firstDate = (Date) ev.evaluate(new Expression("date(2019,7,33,12,0,0,0)"));
    assertNotNull(firstDate);
    difference = (long) new DateDiffFunction().execute(ev, null, firstDate, secondDate, "month");
    assertEquals(difference, 1);
    
    firstDate = (Date) ev.evaluate(new Expression("date(2019,6,1,11,0,0,0)"));
    assertNotNull(firstDate);
    difference = (long) new DateDiffFunction().execute(ev, null, firstDate, secondDate, "month");
    assertEquals(difference, 1);
    
    firstDate = (Date) ev.evaluate(new Expression("date(2020,0,1,12,0,0,0)"));
    secondDate = (Date) ev.evaluate(new Expression("date(2020,11,31,12,0,0,0)"));
    assertNotNull(firstDate);
    assertNotNull(secondDate);
    difference = (long) new DateDiffFunction().execute(ev, null, firstDate, secondDate, "month");
    assertEquals(difference, 11);
    
    difference = (long) new DateDiffFunction().execute(ev, null, firstDate, secondDate, "year");
    assertEquals(difference, 0);
  }
  
  @Test
  public void testDateAddFunction() throws Exception
  {
    Date orig = (Date) ev.evaluate(new Expression("parseDate (\"01.07.2021\", \"dd.MM.yyyy\")"));
    
    Date res = (Date) ev.evaluate(new Expression("dateAdd (parseDate (\"01.07.2021\", \"dd.MM.yyyy\"), -1, \"month\")"));
    
    GregorianCalendar c = new GregorianCalendar();
    
    c.setTime(res);
    
    assertEquals(1, c.get(GregorianCalendar.DAY_OF_MONTH));
  }
}
