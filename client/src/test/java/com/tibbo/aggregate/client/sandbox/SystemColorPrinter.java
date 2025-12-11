package com.tibbo.aggregate.client.sandbox;

import java.awt.*;

public class SystemColorPrinter
{
  
  public static void main(String[] args)
  {
    System.out.println(new Color(SystemColor.control.getRGB()));
    System.out.println(new Color(SystemColor.info.getRGB()));
    System.out.println(SystemColor.control.darker());
  }
  
}
