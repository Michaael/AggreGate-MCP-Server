package com.tibbo.aggregate.client.sandbox;

import java.awt.*;

import javax.swing.*;

public class AbsoluteLayoutTest
{
  public static void main(String[] args)
  {
    AbsoluteLayoutTest alt = new AbsoluteLayoutTest();
    // alt.test1();
    alt.test2();
  }
  
  private void test2()
  {
    JFrame frame = new JFrame("frame");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setBounds(300, 300, 200, 200);
    
    JPanel corePanel = new JPanel();
    corePanel.setOpaque(false);
    corePanel.setLayout(null);
    
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.BOTH;
    c.gridheight = 1;
    c.gridwidth = 1;
    c.weightx = 1;
    c.weighty = 1;
    
    JLabel repr = new JLabel("test1");
    repr.setOpaque(true);
    repr.setBackground(Color.red);
    repr.setBounds(0, 0, 35, 20);
    corePanel.add(repr);
    
    JPanel p1 = new JPanel();
    p1.setLayout(new GridBagLayout());
    p1.add(corePanel, c);
    
    frame.getContentPane().add(p1);
    frame.setVisible(true);
  }
  
  @SuppressWarnings("unused")
  private void test1()
  {
    JFrame frame = new JFrame("frame");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setBounds(300, 300, 200, 200);
    
    JPanel pane = new JPanel();
    // panel.setLayout(null);
    // JLabel label1 = new JLabel("Label1");
    // label1.setBackground(Color.red);
    // label1.setOpaque(true);
    // JButton button1 = new JButton("Button1");
    // button1.setEnabled(false);
    // panel.add(label1);
    // panel.add(button1);
    // label1.setBounds(10, 10, label1.getWidth(), label1.getHeight());
    // button1.setBounds(15, 15, button1.getWidth(), button1.getHeight());
    pane.setLayout(null);
    
    JLabel b1 = new JLabel("one");
    b1.setOpaque(true);
    b1.setBackground(Color.red);
    JLabel b2 = new JLabel("two");
    b2.setOpaque(true);
    b2.setBackground(Color.blue);
    JLabel b3 = new JLabel("three");
    b3.setOpaque(true);
    b3.setBackground(Color.green);
    
    pane.add(b3);
    pane.add(b2);
    pane.add(b1);
    
    Insets insets = pane.getInsets();
    Dimension size = b1.getPreferredSize();
    b1.setBounds(25 + insets.left, 5 + insets.top, size.width + 10, size.height + 15);
    size = b2.getPreferredSize();
    b2.setBounds(35 + insets.left, 10 + insets.top, size.width + 20, size.height + 15);
    size = b3.getPreferredSize();
    b3.setBounds(45 + insets.left, 15 + insets.top, size.width + 50, size.height + 20);
    
    frame.add(pane);
    frame.setVisible(true);
  }
}
