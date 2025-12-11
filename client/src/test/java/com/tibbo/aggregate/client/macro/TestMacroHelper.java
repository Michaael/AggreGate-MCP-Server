package com.tibbo.aggregate.client.macro;

import junit.framework.*;

public class TestMacroHelper extends TestCase
{
  public void testSubstituteResourceReferences()
  {
    String input = "$R{product} xxx $R{product} $R{product} yyy $R{product}";
    String output = "AggreGate xxx AggreGate AggreGate yyy AggreGate";
    
    String processed = MacroHelper.processResourceReferences(input);
    
    assertEquals(output, processed);
  }
}
