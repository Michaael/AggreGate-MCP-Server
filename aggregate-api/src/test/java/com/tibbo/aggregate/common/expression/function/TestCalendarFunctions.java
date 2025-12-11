package com.tibbo.aggregate.common.expression.function;

import java.text.*;
import java.util.*;

import com.tibbo.aggregate.common.data.*;
import com.tibbo.aggregate.common.expression.*;
import com.tibbo.aggregate.common.expression.function.date.*;
import com.tibbo.aggregate.common.tests.*;
import com.tibbo.aggregate.common.util.*;

public class TestCalendarFunctions extends CommonsTestCase
{
  private static final int year = 2014;
  private static final int month = Calendar.OCTOBER;
  private static final int day = 18;
  private static final int hour = 14;
  private static final int minutes = 55;
  private static final int seconds = 0;
  private static String ts = "GMT+4:00";
  private static TimeZone tz = TimeZone.getTimeZone(ts);
  private static GregorianCalendar gc = new GregorianCalendar(tz);
  static
  {
    gc.set(year, month, day, hour, minutes, seconds);
  }
  
  private Evaluator ev;
  
  public void setUp() throws Exception
  {
    super.setUp();
    ev = CommonsFixture.createTestEvaluator();
  }
  
  public void testDateCreateFunction() throws Exception
  {
    Object res = new DateCreateFunction().execute(ev, null, year, month, day, hour, minutes, seconds, ts);
    Date dres = (Date) res;
    
    GregorianCalendar calendar = new GregorianCalendar(tz);
    calendar.setTime(dres);
    assertEquals(hour, calendar.get(Calendar.HOUR_OF_DAY));
  }
  
  public void testDataCreateFunctionWithLongParameter() throws Exception
  {
    Long timeZone = 1L;
    
    Object res = new DateCreateFunction().execute(ev, null, year, month, day, hour, minutes, seconds, timeZone, timeZone);
    Date dres = (Date) res;
    
    GregorianCalendar calendar = new GregorianCalendar(tz);
    calendar.setTime(dres);
    assertEquals(18, calendar.get(Calendar.HOUR_OF_DAY));
  }
  
  public void testDateAddFunction() throws Exception
  {
    Object res = new DateAddFunction().execute(ev, null, gc.getTime(), 2, TimeHelper.NAME_HOUR, ts);
    Date dres = (Date) res;
    
    GregorianCalendar calendar = new GregorianCalendar(tz);
    calendar.setTime(dres);
    assertEquals(hour + 2, calendar.get(Calendar.HOUR_OF_DAY));
  }
  
  public void testDateFunction() throws Exception
  {
    Object y = new DateFieldFunction("year", Calendar.YEAR, "").execute(ev, null, gc.getTime(), ts);
    Object h = new DateFieldFunction("hourOfDay", Calendar.HOUR_OF_DAY, "").execute(ev, null, gc.getTime(), ts);
    
    assertEquals(y, year);
    assertEquals(h, hour);
  }
  
  public void testDateDiffFunction() throws Exception
  {
    long firstDate = System.currentTimeMillis();
    long secondDate = firstDate + TimeHelper.DAY_IN_MS;
    long result = TimeHelper.DAY_IN_MS;
    
    long diff = (long) new DateDiffFunction().execute(null, null, firstDate, secondDate, TimeHelper.MILLISECOND);
    
    assertEquals(result, diff);
  }
  
  public void testFormatDateFunction() throws Exception
  {
    long date = System.currentTimeMillis();
    String simpleDateFormatPattern = "yyyy.MM.dd HH:mm:ss";
    String timeZone = TimeZones.DEFAULT_TIME_ZONE_ID;
    
    SimpleDateFormat sdf = new SimpleDateFormat(simpleDateFormatPattern, Locale.ENGLISH);
    sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
    String expected = sdf.format(new Date());
    
    String result = (String) new FormatDateFunction().execute(null, null, date, simpleDateFormatPattern, timeZone);
    
    assertEquals(expected, result);
  }
  
  public void testFormatDateFunctionWithEmptyParams() throws Exception
  {
    String result = (String) new FormatDateFunction().execute(null, null, 0, "", "");
    
    assertEquals("", result);
  }
  
  public void testTimeFunction() throws Exception
  {
    Date date = new Date();
    long expected = date.getTime();
    
    long result = (long) new TimeFunction().execute(null, null, date);
    
    assertEquals(expected, result);
  }
  
  public void testPrintPeriodFunction() throws Exception
  {
    long period = 1000;
    String expected = "1 Seconds";
    String result = (String) new PrintPeriodFunction().execute(null, null, period);
    
    assertEquals(expected, result);
  }
  
  public void testPrintPeriodFunctionWithTimeUnits() throws Exception
  {
    long period = 1000;
    String expected = "1 Seconds";
    String result = (String) new PrintPeriodFunction().execute(null, null, period, "1", "4");
    
    assertEquals(expected, result);
  }
}
