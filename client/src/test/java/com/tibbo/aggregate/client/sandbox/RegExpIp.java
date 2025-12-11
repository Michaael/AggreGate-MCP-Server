package com.tibbo.aggregate.client.sandbox;

import java.awt.*;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.text.*;

public class RegExpIp
{
  public static void main(String[] args)
  {
    
    try
    {
      MaskFormatter formatter = new MaskFormatter("###.###.###.###");
      formatter.setPlaceholderCharacter(' ');
      
      final JFrame frame = new JFrame("Test IP");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.getContentPane().setLayout(new BorderLayout());
      JTextField ipField = new JFormattedTextField(formatter);
      JButton button = new JButton("Button");
      
      ipField.setInputVerifier(new InputVerifier()
      {
        Pattern pat = Pattern.compile("\\b(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
            + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b");
        
        public boolean shouldYieldFocus(JComponent input)
        {
          boolean inputOK = verify(input);
          if (inputOK)
          {
            return true;
          }
          else
          {
            Toolkit.getDefaultToolkit().beep();
            return false;
          }
        }
        
        public boolean verify(JComponent input)
        {
          JTextField field = (JTextField) input;
          Matcher m = pat.matcher(field.getText());
          return m.matches();
        }
      });
      frame.add(ipField, BorderLayout.NORTH);
      frame.add(button, BorderLayout.SOUTH);
      frame.pack();
      SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          frame.setVisible(true);
          frame.setBounds(100, 100, 100, 100);
        }
      });
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }
}