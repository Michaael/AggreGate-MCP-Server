package com.tibbo.aggregate.client.gui.dialog;

import java.awt.*;
import java.awt.event.*;
import java.text.*;

import javax.swing.*;
import javax.swing.border.*;

import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.component.*;

public class AboutDialog extends AbstractDialog
{
  public AboutDialog()
  {
    super(ComponentHelper.getMainFrame().getFrame(), MessageFormat.format(Pres.get().getString("dlgAboutTitle"), Cres.get().getString("productClient")));
    jbInit();
  }
  
  private void jbInit()
  {
    this.getContentPane().setLayout(borderLayout1);
    jToggleButton1.setAlignmentX((float) 0.5);
    jToggleButton1.setText(Cres.get().getString("close"));
    jToggleButton1.addActionListener(new AboutDialog_jToggleButton1_actionAdapter(this));
    jLabel1.setAlignmentX((float) 0.5);
    jLabel2.setAlignmentX((float) 0.5);
    this.setModal(true);
    
    jPanel1.setLayout(new BoxLayout(jPanel1, BoxLayout.Y_AXIS));
    jPanel1.setBorder(border2);
    jPanel1.add(Box.createVerticalGlue());
    jPanel1.add(jLabel1);
    jPanel1.add(Box.createVerticalStrut(12));
    jPanel1.add(jLabel2);
    jPanel1.add(Box.createVerticalStrut(12));
    jPanel1.add(jToggleButton1);
    jPanel1.add(Box.createVerticalGlue());
    
    this.getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);
    jLabel1.setText(Cres.get().getString("productClient") + " v" + SoftwareVersion.getCurrentVersionAndBuild());
    jLabel2.setText("(c) " + Cres.get().getString("copyright"));
  }
  
  BorderLayout borderLayout1 = new BorderLayout();
  JPanel jPanel1 = new JPanel();
  JLabel jLabel1 = new JLabel();
  JLabel jLabel2 = new JLabel();
  JToggleButton jToggleButton1 = new JToggleButton();
  TitledBorder titledBorder1 = new TitledBorder("");
  Border border2 = BorderFactory.createEmptyBorder(10, 30, 10, 30);
  
  public void jToggleButton1_actionPerformed(ActionEvent e)
  {
    dispose();
  }
  
  @Override
  public void pack()
  {
    super.pack();
    
    int width = getFontMetrics(getFont()).stringWidth(getTitle()) + 100;
    Dimension preferredSize = getPreferredSize();
    if (preferredSize.width < width)
    {
      setPreferredSize(new Dimension(width, preferredSize.height));
      
      super.pack();
    }
  }
}

class AboutDialog_jToggleButton1_actionAdapter implements ActionListener
{
  private final AboutDialog adaptee;
  
  AboutDialog_jToggleButton1_actionAdapter(AboutDialog adaptee)
  {
    this.adaptee = adaptee;
  }
  
  public void actionPerformed(ActionEvent e)
  {
    adaptee.jToggleButton1_actionPerformed(e);
  }
}
