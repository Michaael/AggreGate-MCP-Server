package com.tibbo.aggregate.client.sandbox;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class ToggleButtonTest extends JFrame
{
  public static void main(String[] args)
  {
    ToggleButtonTest tbt = new ToggleButtonTest();
    // tbt.composeToggleButtonPanel()
    // tbt.composeAbsolutePanel();
    // tbt.transferHandlerTest();
    tbt.setVisible(true);
    tbt.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }
  
  public void composeToggleButtonPanel()
  {
    getContentPane().setLayout(new GridBagLayout());
    JTabbedPane pane = new JTabbedPane();
    JPanel tab1 = new JPanel();
    tab1.add(new JLabel("assa"));
    pane.addTab("tab1", tab1);
    getContentPane().add(pane);
  }
  
  public void composeAbsolutePanel()
  {
    JPanel pane = new JPanel();
    pane.setLayout(new OverlayLayout(pane));
    JPanel pan1 = new JPanel();
    JLabel lab1 = new JLabel("label1");
    pan1.add(lab1);
    pan1.setBorder(BorderFactory.createLineBorder(Color.GREEN));
    pan1.setOpaque(false);
    
    JPanel pan2 = new JPanel();
    JLabel lab2 = new JLabel("label2");
    pan2.add(lab2);
    pan2.setOpaque(false);
    pan2.setBorder(BorderFactory.createLineBorder(Color.BLUE));
    
    pane.add(pan1);
    pane.add(pan2);
    pane.setBorder(BorderFactory.createLineBorder(Color.RED));
    getContentPane().add(pane);
  }
  
  public void transferHandlerTest()
  {
    final JLabel label = new JLabel("label");
    label.setOpaque(true);
    label.setForeground(Color.GREEN);
    label.setTransferHandler(new TransferHandler()
    {
      public boolean canImport(TransferSupport support)
      {
        label.setForeground(Color.RED);
        return true;
      }
    });
    JButton button = new JButton("Disable label");
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        label.setEnabled(false);
      }
    });
    
    JPanel panel = new JPanel();
    panel.add(label);
    panel.add(button);
    getContentPane().add(panel);
  }
}
