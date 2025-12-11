package com.tibbo.aggregate.client.gui;

import java.awt.*;

import javax.swing.*;

import com.tibbo.aggregate.client.*;

public class ConfirmPanel extends JPanel
{
  private JLabel messageLabel = new JLabel();
  private JCheckBox setToAllCheckBox = new JCheckBox();
  
  public ConfirmPanel()
  {
    jbInit();
  }
  
  public ConfirmPanel(String message)
  {
    jbInit();
    messageLabel.setText(message);
  }
  
  private void jbInit()
  {
    this.setLayout(new BorderLayout());
    setToAllCheckBox.setOpaque(false);
    setToAllCheckBox.setText(Pres.get().getString("actionApplyToAll"));
    this.add(setToAllCheckBox, java.awt.BorderLayout.SOUTH);
    this.add(messageLabel, java.awt.BorderLayout.CENTER);
  }
  
  public boolean isApplyToAll()
  {
    return setToAllCheckBox.isSelected();
  }
  
  public void setApplyToAll(boolean all)
  {
    setToAllCheckBox.setSelected(all);
  }
  
  public void setMessage(String message)
  {
    messageLabel.setText(message);
  }
  
  public void setShowApplyToAll(boolean showApplyToAll)
  {
    setToAllCheckBox.setVisible(showApplyToAll);
  }
  
  public boolean isShowApplyToAll()
  {
    return setToAllCheckBox.isVisible();
  }
}
