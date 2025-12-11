package com.tibbo.aggregate.client.sandbox;

import java.awt.*;

import javax.swing.*;

public class ToolTipTest
{
  public static void main(String[] args)
  {
    JFrame frame = new JFrame("frame");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setBounds(300, 300, 200, 200);
    Container lPane = frame.getLayeredPane();
    JButton btn = new JButton("start");
    btn.setBounds(0, 0, 80, 30);
    btn.setToolTipText("It is tooltip");
    MToolTip tip = new MToolTip(btn.getToolTipText());
    int x = btn.getSize().width - 20;
    int y = btn.getSize().height - 20;
    tip.setBounds(x, y, tip.getPreferredSize().width, tip.getPreferredSize().height);
    lPane.add(tip, JLayeredPane.MODAL_LAYER);
    lPane.add(btn, JLayeredPane.DEFAULT_LAYER);
    frame.setVisible(true);
  }
}

class MToolTip extends JTextArea
{
  public MToolTip(String s)
  {
    super(s);
    setEditable(false);
    setWrapStyleWord(true);
    setLineWrap(true);
    JToolTip tip = new JToolTip();
    tip.setTipText(s);
    setBackground(tip.getBackground());
    setBorder(tip.getBorder());
    setPreferredSize(tip.getPreferredSize());
  }
}
