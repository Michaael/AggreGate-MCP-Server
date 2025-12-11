package com.tibbo.aggregate.client.macro.ui;

import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.tree.*;

import org.apache.log4j.*;

import com.tibbo.aggregate.client.macro.*;
import com.tibbo.aggregate.client.macro.model.*;
import com.tibbo.aggregate.client.macro.persistence.*;
import com.tibbo.aggregate.client.util.*;
import com.tibbo.aggregate.common.resource.*;
import com.tibbo.aggregate.common.util.*;

public class RecorderController
{
  protected final static String AC_SAVE = "save";
  
  private RecorderPanel panel;
  private MacroTreeModel treeModel;
  private RecordingListener listener;
  private EventMacroRecorder recorder;
  
  private boolean inTreeSelectionListener;
  
  public RecorderController(RecorderPanel panel)
  {
    if (panel == null)
    {
      throw new IllegalArgumentException("panel is null");
    }
    
    this.panel = panel;
  }
  
  public void start()
  {
    panel.recordButton.setAction(new StartRecord());
    panel.stopButton.setAction(new StopRecord());
    panel.saveButton.setAction(new FileMenu());
    
    panel.recordButton.setEnabled(true);
    panel.stopButton.setEnabled(false);
    panel.saveButton.setEnabled(true);
    
    panel.jTextArea1.getDocument().addDocumentListener(new DescriptionListener(Target.DESC));
    panel.jTextArea2.getDocument().addDocumentListener(new DescriptionListener(Target.DESC2));
    panel.jTextField1.getDocument().addDocumentListener(new DescriptionListener(Target.TITLE));
    
    panel.jTextArea1.setEnabled(false);
    panel.jTextArea2.setEnabled(false);
    panel.jTextField1.setEnabled(false);
    
    // init Tree and buttons
    rec();
    stop();
    
    panel.macroTree.addTreeSelectionListener(new TreeSelectionListener()
    {
      public void valueChanged(TreeSelectionEvent e)
      {
        inTreeSelectionListener = true;
        
        try
        {
          panel.jTextArea1.setEnabled(panel.macroTree.getSelectionCount() > 0);
          panel.jTextArea2.setEnabled(panel.macroTree.getSelectionCount() > 0);
          panel.jTextField1.setEnabled(panel.macroTree.getSelectionCount() > 0);
          
          TreePath sel = panel.macroTree.getSelectionPath();
          if (sel != null)
          {
            Object o = sel.getLastPathComponent();
            if (!(o instanceof Step))
            {
              return;
            }
            Step step = (Step) o;
            setText(panel.jTextField1, step.getTitle());
            
            HtmlStepDescription desc = (HtmlStepDescription) step.getDescription(HtmlStepDescription.class, false);
            if (desc != null)
            {
              setText(panel.jTextArea1, desc == null ? null : desc.getBody());
            }
            else
            {
              PlainTextDescription pdesc = (PlainTextDescription) step.getDescription(PlainTextDescription.class, false);
              setText(panel.jTextArea1, pdesc == null ? null : pdesc.getText());
            }
            
            HtmlStepDescription desc2 = (HtmlStepDescription) step.getDescription(HtmlStepDescription.class, true);
            if (desc2 != null)
            {
              setText(panel.jTextArea2, desc2 == null ? null : desc2.getBody());
            }
            else
            {
              PlainTextDescription pdesc2 = (PlainTextDescription) step.getDescription(PlainTextDescription.class, true);
              setText(panel.jTextArea2, pdesc2 == null ? null : pdesc2.getText());
            }
          }
          else
          {
            panel.jTextArea1.setText(null);
            panel.jTextArea2.setText(null);
            panel.jTextField1.setText("Select Step");
          }
        }
        finally
        {
          inTreeSelectionListener = false;
        }
      }
      
      private void setText(JTextComponent c, String text) throws RuntimeException
      {
        Document doc = c.getDocument();
        try
        {
          doc.remove(0, doc.getLength());
          doc.insertString(0, text, null);
        }
        catch (BadLocationException ex)
        {
          throw new RuntimeException(ex);
        }
      }
    });
    
    KeyStroke saveKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK);
    panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(saveKeyStroke, AC_SAVE);
    panel.getActionMap().put(AC_SAVE, new FileMenuSave(true));
    
    // Edit macro from the Guide window
    MacroUIFacade uiFacade = MacroUIFacade.getDefault();
    
    PlayerController playerController = uiFacade.getCurrentPlayerController();
    if (playerController != null)
    {
      EventMacroPlayer player = playerController.getPlayer();
      if (player != null)
      {
        Macro macro = player.getMacro();
        if (macro != null)
        {
          loadMacro(macro);
        }
      }
    }
  }
  
  class StartRecord extends AbstractAction
  {
    {
      super.putValue(Action.SMALL_ICON, ResourceManager.getImageIcon(Icons.MACRO_REC));
      super.putValue(Action.SHORT_DESCRIPTION, "Record");
    }
    
    public void actionPerformed(ActionEvent e)
    {
      rec();
    }
    
  }
  
  private void rec()
  {
    panel.recordButton.setEnabled(false);
    panel.stopButton.setEnabled(true);
    panel.saveButton.setEnabled(false);
    
    recorder = new EventMacroRecorder();
    recorder.beginMacro(new Macro());
    treeModel = recorder.getTreeModel();
    panel.macroTree.setModel(treeModel);
    listener = new RecordingListener(recorder);
    
    PlatformEventMulticaster.addListener(listener);
  }
  
  class StopRecord extends AbstractAction
  {
    {
      super.putValue(Action.SMALL_ICON, ResourceManager.getImageIcon(Icons.MACRO_STOP));
      super.putValue(Action.SHORT_DESCRIPTION, "Stop");
    }
    
    public void actionPerformed(ActionEvent e)
    {
      stop();
    }
  }
  
  public void stop()
  {
    panel.recordButton.setEnabled(true);
    panel.stopButton.setEnabled(false);
    panel.saveButton.setEnabled(true);
    
    if (recorder != null)
    {
      recorder.endMacro();
    }
    
    PlatformEventMulticaster.removeListener(listener);
  }
  
  protected void loadMacro(Macro macro)
  {
    PlatformEventMulticaster.removeListener(listener);
    
    recorder = new EventMacroRecorder();
    macro = recorder.beginMacro(macro);
    treeModel = recorder.getTreeModel();
    panel.macroTree.setModel(treeModel);
  }
  
  class FileMenu extends AbstractAction
  {
    {
      super.putValue(Action.SMALL_ICON, ResourceManager.getImageIcon(Icons.MACRO_FILES));
      super.putValue(Action.SHORT_DESCRIPTION, "File Operations");
    }
    
    public void actionPerformed(ActionEvent e)
    {
      JPopupMenu menu = new JPopupMenu("File Operations");
      
      JMenuItem saveMacro = new JMenuItem(new FileMenuSave());
      menu.add(saveMacro);
      JMenuItem openMacro = new JMenuItem(new FileMenuOpen());
      menu.add(openMacro);
      
      menu.show(panel.saveButton, 0, 0);
    }
  }
  
  class FileMenuSave extends AbstractAction
  {
    private boolean silent;
    
    public FileMenuSave()
    {
      this(false);
    }
    
    public FileMenuSave(boolean silent)
    {
      super.putValue(Action.NAME, "Save Macro as...");
      super.setEnabled(recorder.getMacro() != null);
      this.silent = silent;
    }
    
    public void actionPerformed(ActionEvent e)
    {
      Macro macro = recorder.getMacro();
      if (macro == null)
      {
        return;
      }
      
      MacroStorage storage = MacroStorageManager.getDefaultStorage();
      
      String fileName = macro.getFileName();
      if (silent && fileName != null && fileName.length() > 0)
      {
        fileName = macro.getFileName();
      }
      else
      {
        fileName = JOptionPane.showInputDialog(panel, "Macro File Name", macro.getFileName());
      }
      
      if (fileName == null)
      {
        return;
      }
      
      if (!fileName.toLowerCase(Locale.ENGLISH).endsWith(".xml"))
      {
        fileName += ".xml";
      }
      if (macro.getTitle() == null)
      {
        macro.setTitle(fileName);
      }
      storage.saveMacro(macro, fileName);
    }
  }
  
  class FileMenuOpen extends AbstractAction
  {
    {
      super.putValue(Action.NAME, "Open Macro...");
    }
    
    public void actionPerformed(ActionEvent e)
    {
      MacroStorage storage = MacroStorageManager.getDefaultStorage();
      
      String fileName = JOptionPane.showInputDialog(panel, "Macro File Name");
      
      if (fileName == null)
      {
        return;
      }
      
      if (!fileName.toLowerCase(Locale.ENGLISH).endsWith(".xml"))
      {
        fileName += ".xml";
      }
      
      try
      {
        Macro macro = storage.loadMacro(fileName);
        
        macro.setFileName(fileName);
        
        loadMacro(macro);
      }
      catch (FileNotFoundException ex)
      {
        ClientUtils.showError(Level.INFO, ex);
      }
    }
  }
  
  public enum Target
  {
    TITLE, DESC, DESC2
  }
  
  class DescriptionListener implements DocumentListener
  {
    private Target target;
    
    public DescriptionListener(Target target)
    {
      this.target = target;
    }
    
    public void insertUpdate(DocumentEvent e)
    {
      addStepDescription(e.getDocument());
    }
    
    public void removeUpdate(DocumentEvent e)
    {
      addStepDescription(e.getDocument());
    }
    
    public void changedUpdate(DocumentEvent e)
    {
      addStepDescription(e.getDocument());
    }
    
    private void addStepDescription(Document doc)
    {
      if (inTreeSelectionListener)
      {
        return;
      }
      
      TreePath selection = panel.macroTree.getSelectionPath();
      
      if (selection == null)
      {
        return;
      }
      
      Object o = selection.getLastPathComponent();
      
      if (!(o instanceof Step))
      {
        return;
      }
      
      Step step = (Step) o;
      
      String text = null;
      try
      {
        text = doc.getText(0, doc.getLength());
      }
      catch (BadLocationException ex)
      {
        throw new IllegalStateException(ex);
      }
      
      if (target != Target.TITLE)
      {
        step.removeDescription(PlainTextDescription.class, target == Target.DESC2);
        step.removeDescription(HtmlStepDescription.class, target == Target.DESC2);
      }
      
      if (text != null && text.length() >= 0)
      {
        if (target == Target.TITLE)
        {
          step.setTitle(text);
        }
        else
        {
          step.addDescription(new HtmlStepDescription(text, target == Target.DESC2));
        }
      }
      
      MacroUIFacade uiFacade = MacroUIFacade.getDefault();
      
      PlayerController playerController = uiFacade.getCurrentPlayerController();
      if (playerController != null)
      {
        EventMacroPlayer player = playerController.getPlayer();
        if (player != null)
        {
          Macro macro = player.getMacro();
          if (macro == RecorderController.this.recorder.getMacro())
          {
            player.refreshDescriptions();
          }
        }
      }
    }
  }
}
