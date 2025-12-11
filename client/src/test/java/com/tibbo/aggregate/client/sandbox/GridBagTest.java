package com.tibbo.aggregate.client.sandbox;

import java.awt.*;

import javax.swing.*;

public class GridBagTest extends JFrame
{
  public GridBagTest()
  {
    splitPanelTest();
    // initView2();
  }
  
  public static void main(String[] args)
  {
    GridBagTest gridBagTest = new GridBagTest();
    gridBagTest.pack();
    gridBagTest.setVisible(true);
    // gridBagTest.initView1();
    // gridBagTest.pack();
    // gridBagTest.setVisible(true);
  }
  
  @SuppressWarnings("unused")
  private void initView2()
  {
    JPanel pane = new JPanel();
    pane.setLayout(new GridBagLayout());
    
    JSplitPane sp = new JSplitPane();
    // sp.setEnabled(false);
    
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.weightx = 1;
    c.weighty = 1;
    c.fill = GridBagConstraints.BOTH;
    pane.add(sp, c);
    pane.setBackground(Color.RED);
    
    this.getContentPane().add(pane);
  }
  
  public void splitPanelTest()
  {
    JTextArea textArea = new JTextArea();
    JScrollPane scrollPane = new JScrollPane(textArea);
    textArea.setEditable(true);
    textArea.setPreferredSize(new Dimension(100, 70));
    
    GridBagConstraints cs = new GridBagConstraints();
    cs.gridx = 0;
    cs.gridy = 0;
    cs.weightx = 1;
    cs.weighty = 1;
    JLabel label = new JLabel("assa");
    JPanel panel = new JPanel(new GridBagLayout());
    panel.add(label, cs);
    cs.gridy = 1;
    cs.weighty = 1;
    panel.add(scrollPane, cs);
    JButton button = new JButton("button");
    cs.gridy = 2;
    cs.weighty = 0;
    panel.add(button, cs);
    
    this.getContentPane().add(panel);
  }
}
