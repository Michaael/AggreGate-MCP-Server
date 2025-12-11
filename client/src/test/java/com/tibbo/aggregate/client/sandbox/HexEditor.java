package com.tibbo.aggregate.client.sandbox;

import javax.swing.*;

import at.HexLib.library.*;

public class HexEditor extends JFrame
{
  public HexEditor()
  {
    HexLib hex = new HexLib("feki otgfhbj fhvjdfsil ghdslgnbefsjk gbefsjil nfs".getBytes());
    
    this.getContentPane().add(hex);
  }
  
  public static void main(String[] args)
  {
    HexEditor editor = new HexEditor();
    editor.pack();
    editor.setVisible(true);
  }
  
}
