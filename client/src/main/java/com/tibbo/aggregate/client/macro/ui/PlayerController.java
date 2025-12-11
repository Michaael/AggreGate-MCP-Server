package com.tibbo.aggregate.client.macro.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.*;

import javax.swing.*;
import javax.swing.event.*;

import com.tibbo.aggregate.client.gui.frame.*;
import org.apache.log4j.*;

import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.client.macro.*;
import com.tibbo.aggregate.client.macro.model.*;
import com.tibbo.aggregate.client.macro.persistence.*;
import com.tibbo.aggregate.client.util.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.component.*;

public class PlayerController
{
  public final static String CMD_NEXT = "next";
  public final static String CMD_PREV = "prev";
  public final static String CMD_ESCAPE = "escape";
  
  public final static String HTTP = "http://";
  
  public static final String INDEX_FILE_NAME = "index.html";
  public static final String ON_CLOSE_FILE_NAME = "onclose.html";
  
  private final PlayerPanel panel;
  private final EventMacroPlayer player = new EventMacroPlayer(this);
  private final EventListenerList actionListeners = new EventListenerList();
  
  private final ActionListener actionListener = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      fireActionPerformed(new ActionEvent(this, e.getID(), e.getActionCommand()));
    }
  };
  
  private final HyperlinkListener hyperlinkListener = new HyperlinkListener()
  {
    public void hyperlinkUpdate(HyperlinkEvent e)
    {
      if (e.getEventType() == HyperlinkEvent.EventType.ENTERED)
      {
        panel.getGuideTextPane().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      }
      else if (e.getEventType() == HyperlinkEvent.EventType.EXITED)
      {
        panel.getGuideTextPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      }
      else if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
      {
        String fileName = e.getDescription();
        
        if (fileName == null)
        {
          return;
        }
        if (INDEX_FILE_NAME.equals(fileName))
        {
          if (player.isActive() && !ClientUtils.confirm(Pres.get().getString("macroPlayerJumpIndex")))
          {
            return;
          }
          
          stopPlayback();
          
          player.endMacro();
          init();
          
          panel.getGuideTextPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
        else if (fileName.endsWith(".xml"))
        {
          try
          {
            MacroStorage storage = MacroStorageManager.getDefaultStorage();
            
            Macro macro = storage.loadMacro(fileName);
            
            macro.setFileName(fileName);
            
            if (player.isActive() && !ClientUtils.confirm(Pres.get().getString("macroPlayerJumpMacro")))
            {
              return;
            }
            
            stopPlayback();
            
            player.beginMacro(macro);
            
            panel.getGuideTextPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          }
          catch (FileNotFoundException ex)
          {
            ClientUtils.showError(Level.INFO, ex);
          }
        }
        else if (!fileName.startsWith(HTTP) && fileName.endsWith("." + Constants.DOCS_FILE_EXTENSION))
        {
          String filePath;
          if (fileName.contains(File.pathSeparator) || fileName.contains("/"))
          {
            filePath = new File(ComponentHelper.getConfig().getHomeDirectory() + fileName).getAbsolutePath();
          }
          else
          {
            filePath = ClientUtils.getHelpFilePath(fileName.substring(0, fileName.lastIndexOf('.')));
          }
          
          File helpFile = new File(filePath);
          
          if (!helpFile.exists())
          {
            Log.GUIDE.warn("Help file is not found: " + filePath);
            return;
          }
          
          try
          {
            if (!ComponentHelper.isSteadyStateMode())
            {
              BrowseHelper.browse(helpFile.toURI(), ComponentHelper.getConfig().getHomeDirectory());
            }
            else
            {
              Toolkit.getDefaultToolkit().beep();
            }
          }
          catch (Exception ex)
          {
            ClientUtils.showError(Level.INFO, ex);
          }
        }
        else
        {
          try
          {
            Desktop.getDesktop().browse(new URL(fileName).toURI());
          }
          catch (Exception ex)
          {
            ClientUtils.showError(Level.INFO, ex);
          }
        }
      }
      
    }
  };
  
  public PlayerController(PlayerPanel panel)
  {
    if (panel == null)
    {
      throw new IllegalArgumentException("panel is null");
    }
    
    this.panel = panel;
    
  }
  
  public void start(Macro macro)
  {
    if (macro == null)
    {
      init();
    }
    else
    {
      initButtons();
      player.beginMacro(macro);
    }
  }
  
  public void setTitle(String title)
  {
    panel.getTitleLabel().setText(title);
    panel.getTitleLabel().setToolTipText(title);
  }
  
  public void setDialogTitle(String title)
  {
    InternalFrame currentDialog = MacroUIFacade.getDefault().getCurrentPlayerDialog();
    currentDialog.setTitle(title + " - " + Pres.get().getString("macroTutorial"));
  }
  
  public void setHtml(String html)
  {
    if (html == null)
    {
      panel.getGuideTextPane().setText(null);
    }
    else
    {
      panel.getGuideTextPane().setContentType("text/html");
      
      try
      {
        String styleSheet = "<style type=\"text/css\">" + "ul {" + "margin-left: 5px; " + "padding-left: 5px;" + "}" + "</style>";
        String head = "<head><base href=\"" + new File("./" + MacroUIFacade.MACRO_DIR + "img/").toURI().toURL() + "\" />" + styleSheet + "</head>";
        
        panel.getGuideTextPane().read(new StringReader("<html>" + head + "<body>" + html + "</body></html>"), null);
      }
      catch (IOException ex)
      {
        throw new RuntimeException(ex);
      }
    }
  }
  
  public void setText(String text)
  {
    panel.getGuideTextPane().setText(text);
  }
  
  public void showNavigationMenu(JPopupMenu menu)
  {
    if (menu == null)
    {
      return;
    }
    
    menu.show(panel.getButtonPanel(), 0, 0);
  }
  
  public void setButtonText(String cmdButton, String text)
  {
    if (CMD_NEXT.equals(cmdButton))
    {
      panel.getNextButton().setText(text);
    }
    else if (CMD_PREV.equals(cmdButton))
    {
      panel.getBackButton().setText(text);
    }
  }
  
  public void setButtonTooltipText(String cmdButton, String text)
  {
    if (CMD_NEXT.equals(cmdButton))
    {
      panel.getNextButton().setToolTipText(text);
    }
    else if (CMD_PREV.equals(cmdButton))
    {
      panel.getBackButton().setToolTipText(text);
    }
  }
  
  public void setButtonEnabled(String cmdButton, boolean enabled)
  {
    if (CMD_NEXT.equals(cmdButton))
    {
      panel.getNextButton().setEnabled(enabled);
    }
    else if (CMD_PREV.equals(cmdButton))
    {
      panel.getBackButton().setEnabled(enabled);
    }
  }
  
  public void setButtonVisible(String cmdButton, boolean f)
  {
    if (CMD_NEXT.equals(cmdButton))
    {
      panel.getNextButton().setVisible(f);
    }
    else if (CMD_PREV.equals(cmdButton))
    {
      panel.getBackButton().setVisible(f);
    }
  }
  
  public void setOneButtonEnabled(String cmdButton, boolean enabled)
  {
    panel.getNextButton().setEnabled(CMD_NEXT.equals(cmdButton));
    panel.getBackButton().setEnabled(CMD_PREV.equals(cmdButton));
  }
  
  public void setOneButtonVisible(String cmdButton, boolean enabled)
  {
    panel.getNextButton().setVisible(CMD_NEXT.equals(cmdButton));
    panel.getBackButton().setVisible(CMD_PREV.equals(cmdButton));
  }
  
  public void hideButtons()
  {
    panel.getNextButton().setVisible(false);
    panel.getBackButton().setVisible(false);
  }
  
  public void stopPlayback()
  {
    player.endMacro();
  }
  
  protected void init()
  {
    final MacroStorage storage = MacroStorageManager.getDefaultStorage();
    final StringBuilder s = new StringBuilder();
    
    String indexHtml = loadFile(storage, INDEX_FILE_NAME);
    if (indexHtml != null)
    {
      s.append(indexHtml);
    }
    
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        setTitle(MessageFormat.format(Pres.get().getString("macroPlayerInitTitle"), Cres.get().getString("product")));
        
        if (s.length() == 0)
        {
          s.append(Pres.get().getString("macroPlayerInitHtmlHeader"));
          
          Macro[] macros = storage.listMacros();
          
          s.append("<ul>");
          for (int i = 0; i < macros.length; i++)
          {
            Macro macro = macros[i];
            String desc = MacroHelper.processResourceReferences(macro.getTitle());
            s.append("<li>");
            s.append("<a href=\"" + macro.getFileName() + "\">" + desc + "</a>");
            s.append("</li>");
          }
          s.append("</ul>");
        }
        
        try
        {
          panel.getGuideTextPane().setContentType("text/html");
          String head = "<head><base href=\"" + new File("./" + MacroUIFacade.MACRO_DIR + "img/").toURI().toURL() + "\" /></head>";
          
          panel.getGuideTextPane().read(new StringReader("<html>" + head + "<body>" + s.toString() + "</body></html>"), null);
        }
        catch (IOException ex)
        {
          throw new RuntimeException(ex);
        }
        
        panel.getGuideTextPane().removeHyperlinkListener(hyperlinkListener);
        panel.getGuideTextPane().addHyperlinkListener(hyperlinkListener);
        
        initButtons();
      }
    });
  }
  
  public static String loadFile(MacroStorage storage, String fileName)
  {
    StringBuilder s = new StringBuilder();
    try
    {
      InputStream indexStream = storage.loadResource(fileName);
      InputStreamReader fr = new InputStreamReader(indexStream, "utf-8");
      BufferedReader in = new BufferedReader(fr);
      int c;
      c = in.read();
      while (c != -1)
      {
        s.append((char) c);
        c = in.read();
      }
    }
    catch (FileNotFoundException ex)
    {
      Log.GUIDE.warn("File '" + fileName + "' isn't found in macro resources, using automatic listing", ex);
      return null;
    }
    catch (IOException ex)
    {
      Log.GUIDE.warn("Error loading " + fileName, ex);
      return null;
    }
    
    return MacroHelper.processResourceReferences(s.toString());
  }
  
  private void initButtons()
  {
    panel.getBackButton().setEnabled(false);
    panel.getNextButton().setEnabled(false);
    
    panel.getBackButton().setActionCommand(CMD_PREV);
    panel.getNextButton().setActionCommand(CMD_NEXT);
    
    panel.getBackButton().removeActionListener(actionListener);
    panel.getNextButton().removeActionListener(actionListener);
    panel.getBackButton().addActionListener(actionListener);
    panel.getNextButton().addActionListener(actionListener);
  }
  
  public synchronized void addActionListener(ActionListener l)
  {
    actionListeners.add(ActionListener.class, l);
  }
  
  public synchronized void removeActionListener(ActionListener l)
  {
    actionListeners.remove(ActionListener.class, l);
  }
  
  protected void fireActionPerformed(ActionEvent e)
  {
    if (actionListeners != null)
    {
      ActionListener[] lst = actionListeners.getListeners(ActionListener.class);
      for (int i = 0; i < lst.length; i++)
      {
        lst[i].actionPerformed(e);
      }
    }
  }
  
  public EventMacroPlayer getPlayer()
  {
    return player;
  }
}
