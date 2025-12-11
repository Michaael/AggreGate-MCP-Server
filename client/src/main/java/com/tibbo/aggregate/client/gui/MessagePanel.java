package com.tibbo.aggregate.client.gui;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.common.resource.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.resource.*;

public class MessagePanel extends JPanel
{
  private JScrollPane jScrollPane1 = new JScrollPane();
  private DefaultListModel defaultListModel1 = new DefaultListModel();
  private JPopupMenu jPopupMenu1 = new JPopupMenu();
  
  private Action copy;
  private Action copyAll;
  
  private JList jList1 = new JList()
  {
    public String getToolTipText(MouseEvent evt)
    {
      int index = locationToIndex(evt.getPoint());
      Object item = getModel().getElementAt(index);
      return item == null ? null : item.toString();
    }
  };
  
  public MessagePanel()
  {
    try
    {
      jbInit();
    }
    catch (Exception exception)
    {
      exception.printStackTrace();
    }
  }
  
  public void addMessage(String message)
  {
    defaultListModel1.addElement(message);
  }
  
  public void clearMessages()
  {
    defaultListModel1.clear();
  }
  
  private void jbInit() throws Exception
  {
    this.setLayout(new BorderLayout());
    jScrollPane1.setOpaque(false);
    jList1.setToolTipText(Pres.get().getString("actionClipboardOperations"));
    jList1.setModel(defaultListModel1);
    this.add(jScrollPane1, java.awt.BorderLayout.CENTER);
    jScrollPane1.setViewportView(jList1);
    initMenu();
  }
  
  private void initMenu()
  {
    copy = new AbstractAction(Pres.get().getString("actionCopyMessage"), ResourceManager.getImageIcon(Icons.CM_COPY))
    {
      public void actionPerformed(ActionEvent e)
      {
        int selection[] = jList1.getSelectedIndices();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < selection.length; i++)
        {
          Object value = defaultListModel1.get(selection[i]);
          sb.append(value);
          if (i < selection.length - 1)
          {
            sb.append("\n");
          }
        }
        StringSelection stsel = new StringSelection(sb.toString());
        Clipboard system = Toolkit.getDefaultToolkit().getSystemClipboard();
        system.setContents(stsel, stsel);
      }
    };
    
    copyAll = new AbstractAction(Pres.get().getString("actionCopyMessages"), null)
    {
      public void actionPerformed(ActionEvent e)
      {
        StringBuffer sb = new StringBuffer();
        for (Enumeration en = defaultListModel1.elements(); en.hasMoreElements();)
        {
          Object item = (Object) en.nextElement();
          sb.append(item);
          if (en.hasMoreElements())
          {
            sb.append("\n");
          }
        }
        StringSelection stsel = new StringSelection(sb.toString());
        Clipboard system = Toolkit.getDefaultToolkit().getSystemClipboard();
        system.setContents(stsel, stsel);
      }
    };
    
    jPopupMenu1.add(copy);
    jPopupMenu1.add(copyAll);
    
    jList1.addMouseListener(new MouseAdapter()
    {
      public void mouseClicked(MouseEvent e)
      {
        showMenu(e);
      }
      
      public void mousePressed(MouseEvent e)
      {
        showMenu(e);
      }
      
      public void mouseReleased(MouseEvent e)
      {
        showMenu(e);
      }
      
      private void showMenu(MouseEvent e)
      {
        if (e.isPopupTrigger())
        {
          int selection[] = jList1.getSelectedIndices();
          if (selection == null || selection.length == 0)
          {
            int index = jList1.locationToIndex(e.getPoint());
            jList1.setSelectedIndex(index);
            copy.setEnabled(index >= 0);
          }
          jPopupMenu1.show(e.getComponent(), e.getX(), e.getY());
        }
      }
    });
  }
  
  public static void main(String... args) throws Exception
  {
    ResourceManager.initialize(null, Resource.class);
    JFrame frame = new JFrame("Messages");
    MessagePanel messagePanel = new MessagePanel();
    frame.add(messagePanel);
    frame.pack();
    frame.setLocationByPlatform(true);
    // frame.setVisible(true);
    JOptionPane jop = new JOptionPane(messagePanel, JOptionPane.INFORMATION_MESSAGE);
    JDialog jDialog = jop.createDialog("Messages");
    jDialog.setModal(false);
    jDialog.setVisible(true);
    messagePanel.addMessage("xxx");
    Thread.sleep(1000);
    messagePanel.addMessage("Yoqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq");
    Thread.sleep(1000);
    messagePanel.addMessage("test");
    Thread.sleep(1000);
    messagePanel.addMessage("!");
  }
}
