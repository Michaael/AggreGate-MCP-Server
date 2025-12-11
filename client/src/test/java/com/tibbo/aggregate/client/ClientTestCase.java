package com.tibbo.aggregate.client;

import junit.framework.*;

public abstract class ClientTestCase extends TestCase
{
  private ClientFixture clientFixture;
  
  public void setUp() throws Exception
  {
    super.setUp();
    
    clientFixture = new ClientFixture();
    
    clientFixture.setUp();
  }
  
  public void tearDown() throws Exception
  {
    clientFixture.tearDown();
    
    super.tearDown();
  }
}
