package com.tibbo.aggregate.client.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.tibbo.aggregate.client.*;

public class OptionPane
{
  static MessagePanel messagePanel = new MessagePanel();
  static JDialog messageDialog;
  
  public static void showMessageDialog(Component parentComponent, String message, String title, int messageType) throws HeadlessException
  {
    if (messageDialog == null)
    {
      messagePanel.clearMessages();
      JOptionPane jOptionPane = new JOptionPane(messagePanel, messageType);
      JDialog jDialog = jOptionPane.createDialog(parentComponent, Pres.get().getString("actionMessagesDialog"));
      jDialog.setModal(false);
      jDialog.setVisible(true);
      jDialog.setResizable(true);
      jDialog.addComponentListener(new ComponentAdapter()
      {
        public void componentHidden(ComponentEvent e)
        {
          messagePanel.clearMessages();
          messageDialog = null;
        }
      });
      messageDialog = jDialog;
    }
    messagePanel.addMessage((title != null ? title + ": " : "") + (message != null ? message : ""));
    messageDialog.setVisible(true);
    messageDialog.pack();
  }
  
  public static ConfirmationResult showConfirmDialog(Component parentComponent, String message, String title, int optionType, int messageType, boolean showToAll) throws HeadlessException
  {
    ConfirmPanel confirmPanel = new ConfirmPanel(message);
    confirmPanel.setShowApplyToAll(showToAll);
    int option = JOptionPane.showConfirmDialog(parentComponent, confirmPanel, title, optionType, messageType);
    return new ConfirmationResult(option, confirmPanel.isApplyToAll());
  }
  
  public static class ConfirmationResult
  {
    int option;
    boolean applyToAll;
    
    public ConfirmationResult(int option, boolean applyToAll)
    {
      this.option = option;
      this.applyToAll = applyToAll;
    }
    
    public int getOption()
    {
      return option;
    }
    
    public boolean isApplyToAll()
    {
      return applyToAll;
    }
  }
}
