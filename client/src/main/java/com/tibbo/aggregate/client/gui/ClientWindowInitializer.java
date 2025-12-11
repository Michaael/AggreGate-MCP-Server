package com.tibbo.aggregate.client.gui;

import com.tibbo.aggregate.client.*;

public interface ClientWindowInitializer
{
  void initialize(ClientContainer mainContainer);
  
  void terminate();
}
