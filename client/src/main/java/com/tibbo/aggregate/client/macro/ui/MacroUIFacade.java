package com.tibbo.aggregate.client.macro.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

import com.jidesoft.docking.event.*;
import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.client.gui.frame.*;
import com.tibbo.aggregate.client.macro.model.*;
import com.tibbo.aggregate.client.macro.persistence.*;
import com.tibbo.aggregate.common.resource.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.component.*;

public class MacroUIFacade
{
  public static String MACRO_DIR = "macro/";
  
  private static MacroUIFacade defaultFacade = new MacroUIFacade();
  private InternalFrame currentPlayerDialog;
  private PlayerController currentPlayerController;
  
  public static MacroUIFacade getDefault()
  {
    return defaultFacade;
  }
  
  public Action getStartRecorderAction()
  {
    return new AbstractAction()
    {
      {
        super.putValue(Action.NAME, "Start Macro Recorder");
      }
      
      @Override
      public void actionPerformed(ActionEvent e)
      {
        RecorderPanel panel = new RecorderPanel();
        final RecorderController controller = new RecorderController(panel);
        controller.start();
        
        DmfComponent component = new DmfComponent();
        component.setLayout(new BorderLayout());
        component.add(panel, BorderLayout.CENTER);
        
        final JDialog jd = new JDialog(ComponentHelper.getMainFrame().getFrame());
        jd.setPreferredSize(new Dimension(900, 500));
        jd.setTitle("Macro Recorder");
        jd.add(component);
        jd.pack();
        jd.setVisible(true);
      }
    };
  }
  
  public Action getStartGuideAction(final Workspace workspace)
  {
    return new AbstractAction()
    {
      {
        super.putValue(Action.NAME, Pres.get().getString("macroShowPlayer"));
        super.putValue(Action.SMALL_ICON, ResourceManager.getImageIcon(Icons.MACRO_GUIDE));
      }
      
      @Override
      public void actionPerformed(ActionEvent e)
      {
        startMacroPlayer(workspace, null);
      }
    };
  }
  
  public boolean macrosAvailable()
  {
    final File macroDir = new File(ComponentHelper.getConfig().getHomeDirectory() + MACRO_DIR);
    return macroDir.exists() && macroDir.isDirectory();
  }
  
  public PlayerController startMacroPlayer(final Workspace workspace, Macro macro)
  {
    if (currentPlayerDialog != null)
    {
      currentPlayerController.stopPlayback();
      currentPlayerDialog.dispose();
    }
    
    final PlayerPanel panel = new PlayerPanel();
    final PlayerController controller = new PlayerController(panel);
    controller.start(macro);
    
    SwingUtilities.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        DmfComponent component = new DmfComponent();
        component.setLayout(new BorderLayout());
        component.add(panel, BorderLayout.CENTER);
        
        WindowLocation wl = new WindowLocation(WindowLocation.STATE_DOCKED, WindowLocation.SIDE_RIGHT, 0, InternalFrame.SIZE_GUIDE);
        
        final InternalFrame jd = InternalFrame.create(InternalFrame.FRAME_KEY_GUIDE, Pres.get().getString("macroTutorial"), component, true, false, wl, null, Docs.CL_INTERACTIVE_GUIDE, null);
        
        jd.setTitle(Pres.get().getString("macroTutorial"));
        jd.add(component);
        
        currentPlayerDialog = jd;
        
        jd.addDockableFrameListener(new DockableFrameAdapter()
        {
          @Override
          public void dockableFrameRemoved(DockableFrameEvent e)
          {
            controller.stopPlayback();
            if (workspace != null && workspace.isFirstUse())
            {
              JTextPane textPane = new JTextPane();
              textPane.setEditable(false);
              textPane.setOpaque(false);
              String oncloseHtml = PlayerController.loadFile(MacroStorageManager.getDefaultStorage(), PlayerController.ON_CLOSE_FILE_NAME);
              if (oncloseHtml != null)
              {
                textPane.setContentType("text/html");
                
                try
                {
                  String head = "<head><base href=\"" + new File("./" + MACRO_DIR + "img/").toURI().toURL() + "\" /></head>";
                  
                  textPane.read(new StringReader("<html>" + head + "<body>" + oncloseHtml + "</body></html>"), null);
                }
                catch (IOException ex)
                {
                  throw new RuntimeException(ex);
                }
              }
              else
              {
                textPane.setText(Pres.get().getString("macroPlayerOnClose"));
              }
              
              JOptionPane.showMessageDialog(ComponentHelper.getMainFrame(), textPane, Pres.get().getString("macroTutorial"), JOptionPane.PLAIN_MESSAGE);
            }
          }
        });
      }
    });
    
    currentPlayerController = controller;
    
    return controller;
  }
  
  public InternalFrame getCurrentPlayerDialog()
  {
    return currentPlayerDialog;
  }
  
  public PlayerController getCurrentPlayerController()
  {
    return currentPlayerController;
  }
}
