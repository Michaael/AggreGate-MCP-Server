package com.tibbo.aggregate.client.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.client.util.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.component.*;

public class ClientFrame extends JFrame
{
  public ClientFrame(ClientContainer mainContainer)
  {
    mainContainer.setFrame(this);
    
    mainContainer.init();
    
    if (!ComponentHelper.isSteadyStateMode())
    {
      setJMenuBar(mainContainer.getMenuBar());
    }
    
    setTitle(mainContainer.getDefaultTitle());
    
    this.addWindowListener(new WindowAdapter()
    {
      @Override
      public void windowClosing(WindowEvent e)
      {
        if (ComponentHelper.isSteadyStateMode())
        {
          return;
        }
        
        if (ClientUtils.confirm(Pres.get().getString("mfConfirmExit")))
        {
          ComponentHelper.stop();
        }
      }
    });
    
    setIconImages(Util.getIconImages(OtherIcons.CLIENT, Arrays.asList(16, 24, 32, 48, 64, 128)));
    
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    setSize(screenSize);
    setLocation(0, 0);
    
    if (!OSDetector.isMac())
    {
      // This flag makes Client window collapsed to a small title bar brick at the bottom of the screen
      setExtendedState(MAXIMIZED_BOTH);
    }
    
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    
    getContentPane().setLayout(new BorderLayout());
    
    getContentPane().add(mainContainer, java.awt.BorderLayout.CENTER);
  }
}
