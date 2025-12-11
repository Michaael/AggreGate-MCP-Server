package com.tibbo.aggregate.client.sandbox;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;


public class MenuTest extends JFrame
{
  
  public static void main(String[] args) throws Exception
  {
    JFrame frame = new JFrame("Title");
    JMenuBar menuBar = new JMenuBar();
    JMenu fileMenu = new JMenu("File");
    JMenuItem disabledMenuItem = new JMenuItem("Disabled");
    disabledMenuItem.setEnabled(false);
    fileMenu.add(disabledMenuItem);
    menuBar.add(fileMenu);
    frame.setJMenuBar(menuBar);
    frame.setSize(400, 300);
    frame.setVisible(true);
  }
}
