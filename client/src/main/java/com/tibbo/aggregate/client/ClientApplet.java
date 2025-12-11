package com.tibbo.aggregate.client;

import java.awt.*;
import java.net.*;

import javax.swing.*;

import com.tibbo.aggregate.client.gui.*;
import com.tibbo.aggregate.common.protocol.*;
import com.tibbo.aggregate.common.resource.*;
import com.tibbo.aggregate.component.*;

public class ClientApplet extends JApplet
{
  
  @Override
  public void init()
  {
    // Copying applet parameter into system property before ResourceManager initialization
    final String language = getParameter("user.language");
    if (language != null)
    {
      System.setProperty("user.language", language);
    }
    
    String address = getParameter("server.address");
    if (address == null)
    {
      address = RemoteServer.DEFAULT_ADDRESS;
    }
    
    String port = getParameter("server.port");
    if (port == null)
    {
      port = String.valueOf(RemoteServer.DEFAULT_PORT);
    }
    
    try
    {
      ClientHelper.init(new String[] { "-r", "-address", address, "-port", port }, Client.getParameters(), new URI(
          getCodeBase().toExternalForm() + ResourceManager.CLIENT_CUSTOMIZATION_RESOURCES_FILENAME));
      
      ComponentHelper.setApplet(true);
    }
    catch (Exception ex)
    {
      throw new IllegalStateException(ex.getMessage(), ex);
    }
  }
  
  @Override
  public void start()
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        Client.startAsync(this, new AppletClientWindowInitializer());
      }
    });
  }
  
  @Override
  public void stop()
  {
    Client.shutdown();
  }
  
  private class AppletClientWindowInitializer implements ClientWindowInitializer
  {
    @Override
    public void initialize(ClientContainer mainContainer)
    {
      setLayout(new BorderLayout());
      
      add(mainContainer, BorderLayout.CENTER);
      
      mainContainer.init();
      
      Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, ClientApplet.this);
      
      if (frame != null)
      {
        mainContainer.setFrame(frame);
      }
      
      setJMenuBar(mainContainer.getMenuBar());
    }
    
    @Override
    public void terminate()
    {
    }
  }
  
}
