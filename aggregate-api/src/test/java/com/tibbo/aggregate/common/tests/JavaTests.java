package com.tibbo.aggregate.common.tests;

import org.junit.*;

public class JavaTests
{
  @Test(expected = NullPointerException.class)
  public void testNullToInt()
  {
    class C
    {
      public int m(int x)
      {
        return x + 1;
      }
    }
    C c = new C();
    Integer i = null;
    c.m(i);
  }
}
