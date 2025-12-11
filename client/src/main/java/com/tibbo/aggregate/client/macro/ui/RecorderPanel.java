package com.tibbo.aggregate.client.macro.ui;

import java.awt.*;

import javax.swing.*;

import com.tibbo.aggregate.common.resource.*;
import com.tibbo.aggregate.common.util.*;

public class RecorderPanel extends JPanel
{
  BorderLayout borderLayout1 = new BorderLayout();
  JSplitPane jSplitPane1 = new JSplitPane();
  JPanel jPanel1 = new JPanel();
  JLabel jLabel1 = new JLabel();
  JScrollPane jScrollPane2 = new JScrollPane();
  JTextArea jTextArea1 = new JTextArea();
  JToolBar jToolBar1 = new JToolBar();
  JButton recordButton = new JButton();
  JPanel jPanel3 = new JPanel();
  JScrollPane jScrollPane1 = new JScrollPane();
  JTree macroTree = new JTree();
  JButton stopButton = new JButton();
  JButton saveButton = new JButton();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  JLabel jLabel3 = new JLabel();
  JTextField jTextField1 = new JTextField();
  JSplitPane jSplitPane2 = new JSplitPane();
  JPanel jPanel5 = new JPanel();
  BorderLayout borderLayout2 = new BorderLayout();
  JPanel jPanel6 = new JPanel();
  BorderLayout borderLayout4 = new BorderLayout();
  JLabel jLabel4 = new JLabel();
  JScrollPane jScrollPane3 = new JScrollPane();
  JTextArea jTextArea2 = new JTextArea();
  BorderLayout borderLayout5 = new BorderLayout();
  
  public RecorderPanel()
  {
    jbInit();
  }
  
  private void jbInit()
  {
    this.setLayout(borderLayout1);
    jLabel1.setText("Description");
    jPanel1.setLayout(gridBagLayout1);
    jToolBar1.setFloatable(false);
    jSplitPane1.setContinuousLayout(true);
    jSplitPane1.setResizeWeight(0.8);
    jLabel3.setText("Step Title");
    jPanel5.setLayout(borderLayout2);
    jSplitPane2.setOrientation(JSplitPane.VERTICAL_SPLIT);
    jSplitPane2.setContinuousLayout(true);
    jSplitPane2.setOneTouchExpandable(true);
    jSplitPane2.setResizeWeight(0.5);
    jPanel6.setLayout(borderLayout4);
    jLabel4.setText("Finishing Description");
    this.add(jSplitPane1, java.awt.BorderLayout.CENTER);
    jSplitPane1.add(jPanel1, JSplitPane.RIGHT);
    jSplitPane1.add(jPanel3, JSplitPane.LEFT);
    jPanel3.setLayout(borderLayout5);
    jPanel3.add(jScrollPane1, null);
    jScrollPane1.getViewport().add(macroTree);
    this.add(jToolBar1, java.awt.BorderLayout.SOUTH);
    jToolBar1.add(recordButton);
    jToolBar1.add(stopButton);
    jToolBar1.add(saveButton);
    jPanel1.add(jLabel3, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel1.add(jTextField1, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    jScrollPane2.getViewport().add(jTextArea1);
    jPanel1.add(jSplitPane2, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    jPanel5.add(jScrollPane2, java.awt.BorderLayout.CENTER);
    jPanel5.add(jLabel1, java.awt.BorderLayout.NORTH);
    jSplitPane2.add(jPanel5, JSplitPane.TOP);
    jSplitPane2.add(jPanel6, JSplitPane.BOTTOM);
    jPanel6.add(jLabel4, java.awt.BorderLayout.NORTH);
    jPanel6.add(jScrollPane3, java.awt.BorderLayout.CENTER);
    jScrollPane3.getViewport().add(jTextArea2);
    jTextArea1.setLineWrap(true);
    jTextArea1.setWrapStyleWord(true);
    
    recordButton.setIcon(ResourceManager.getImageIcon(Icons.MACRO_REC));
    recordButton.setToolTipText("Record");
    stopButton.setToolTipText("Stop");
    stopButton.setIcon(ResourceManager.getImageIcon(Icons.MACRO_STOP));
    saveButton.setToolTipText("Save Macro as...");
    saveButton.setIcon(ResourceManager.getImageIcon(Icons.MACRO_FILES));
  }
}
