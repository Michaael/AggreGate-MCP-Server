package com.tibbo.aggregate.common.tests;

import java.util.*;

import junit.framework.*;

public class CommonsTestCase extends TestCase
{
  private CommonsFixture commonsFixture = new CommonsFixture();
  
  @Override
  protected void setUp() throws Exception
  {
    super.setUp();
    
    commonsFixture.setUp();
  }
  
  @Override
  protected void tearDown() throws Exception
  {
    commonsFixture.tearDown();
    commonsFixture = null;
    
    super.tearDown();
  }
  
  public CommonsFixture getCommonsFixture()
  {
    return commonsFixture;
  }

  public static Calendar getCalendar() {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar;
  }
}
