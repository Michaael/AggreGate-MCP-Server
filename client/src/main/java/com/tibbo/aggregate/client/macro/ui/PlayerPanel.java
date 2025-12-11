package com.tibbo.aggregate.client.macro.ui;

import java.awt.*;

import javax.swing.*;

import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.client.util.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.resource.*;
import com.tibbo.aggregate.common.util.*;

public class PlayerPanel extends JPanel
{
  private static final Color GUIDE_BACKGROUND = Color.decode("0xFFFFD1");
  
  private final JLabel titleIcon = new JLabel();
  private final JScrollPane guideScrollPane = new JScrollPane();
  private final JTextPane guideTextPane = new JTextPane();
  private final JPanel buttonPanel = new JPanel();
  private final BorderLayout borderLayout2 = new BorderLayout();
  private final JButton backButton = new JButton();
  private final JButton nextButton = new JButton();
  private final GridBagLayout gridBagLayout1 = new GridBagLayout();
  private final JTextPane titleLabel = new JTextPane();
  
  public PlayerPanel()
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
  
  private void jbInit() throws Exception
  {
    this.setLayout(gridBagLayout1);
    buttonPanel.setLayout(borderLayout2);
    backButton.setText(Cres.get().getString("back"));
    nextButton.setHorizontalTextPosition(SwingConstants.LEADING);
    backButton.setVisible(false);
    nextButton.setText(Cres.get().getString("next"));
    nextButton.setVisible(false);
    buttonPanel.setOpaque(false);
    this.setBackground(SystemColors.INFO);
    titleIcon.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
    titleLabel.setFont(new java.awt.Font("Arial", Font.BOLD, 14));
    titleLabel.setOpaque(false);
    titleLabel.setEditable(false);
    getButtonPanel().add(getBackButton(), java.awt.BorderLayout.WEST);
    getButtonPanel().add(getNextButton(), java.awt.BorderLayout.EAST);
    guideScrollPane.getViewport().add(guideTextPane);
    this.add(titleIcon, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    this.add(titleLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    this.add(guideScrollPane, new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    this.add(buttonPanel, new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    titleLabel.setText(Pres.get().getString("macroTutorial"));
    titleIcon.setIcon(ResourceManager.getImageIcon(Icons.MACRO_COMPASS));
    titleIcon.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    backButton.setIcon(ResourceManager.getImageIcon(Icons.MACRO_PREV));
    nextButton.setIcon(ResourceManager.getImageIcon(Icons.MACRO_NEXT));
    guideTextPane.setEditable(false);
    guideTextPane.setBackground(GUIDE_BACKGROUND);
    guideTextPane.setFont(Font.decode("arial"));
    guideTextPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
  }
  
  @Override
  protected void paintChildren(Graphics g)
  {
    // Creating a new OtherIcons object to fix bug with NullPointerException in javax.swing.text.html.StyleSheet$ListPainter.paint
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setFont(Font.decode("arial"));
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    super.paintChildren(g2);
  }
  
  JTextPane getGuideTextPane()
  {
    return guideTextPane;
  }
  
  JPanel getButtonPanel()
  {
    return buttonPanel;
  }
  
  JButton getBackButton()
  {
    return backButton;
  }
  
  JButton getNextButton()
  {
    return nextButton;
  }
  
  JTextPane getTitleLabel()
  {
    return titleLabel;
  }
  
  public static void main(String... args)
  {
    JFrame f = new JFrame();
    f.add(new PlayerPanel());
    f.pack();
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    f.setVisible(true);
  }
}
