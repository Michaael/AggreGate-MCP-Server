package com.tibbo.aggregate.client;

import javax.swing.*;

import com.tibbo.aggregate.client.gui.*;
import com.tibbo.aggregate.component.*;

class DefaultClientWindowInitializer implements ClientWindowInitializer
{
  private ClientFrame mainFrame;
  
  @Override
  public void initialize(ClientContainer mainContainer)
  {
    mainFrame = new ClientFrame(mainContainer);
    
    if (ComponentHelper.isSteadyStateMode())
    {
      mainFrame.setUndecorated(true);
      mainFrame.setResizable(false);
      mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
      mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }
    
    mainFrame.setVisible(true);
  }
  
  @Override
  public void terminate()
  {
    if (mainFrame != null)
    {
      mainFrame.setVisible(false);
      
      mainFrame.dispose();
    }
  }
  
}