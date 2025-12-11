package com.tibbo.aggregate.client.sandbox;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

import org.apache.batik.anim.dom.*;
import org.apache.batik.bridge.*;
import org.apache.batik.swing.*;
import org.apache.batik.util.*;
import org.w3c.dom.*;
import org.w3c.dom.Element;
import org.w3c.dom.svg.*;

import com.tibbo.aggregate.common.util.*;

public class SvgManipulator
{
  
  public static void main(String[] args)
  {
    try
    {
      String file = "d:\\data\\temp2\\regulator.svg";
      
      String parser = XMLResourceDescriptor.getXMLParserClassName();
      SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
      Document doc = f.createDocument("SVG", new FileInputStream(new File(file)));
      
      final JSVGCanvas canvas = new JSVGCanvas();
      
      canvas.setDocumentState(JSVGCanvas.ALWAYS_DYNAMIC);
      
      canvas.setDocument(doc);
      
      JFrame frame = new JFrame("SVG");
      
      frame.getContentPane().add(canvas, BorderLayout.CENTER);
      
      frame.addWindowListener(new WindowAdapter()
      {
        public void windowClosing(WindowEvent e)
        {
          System.exit(0);
        }
      });
      
      frame.setSize(800, 800);
      frame.setVisible(true);
      
      final Holder<Integer> holder = new Holder(0);
      
      while (true)
      {
        Thread.currentThread();
        Thread.sleep(100);
        
        UpdateManager um = canvas.getUpdateManager();
        
        if (um != null)
        {
          um.getUpdateRunnableQueue().invokeLater(new Runnable()
          {
            public void run()
            {
              transform(canvas, "transformedRegulatorMaskGrp");
              transform(canvas, "regulatorMaskGrp");
            }
            
            private void transform(final JSVGCanvas canvas, String element)
            {
              String attribute = "transform";
              
              SVGDocument d = canvas.getSVGDocument();
              Element e = d.getElementById(element);
              
              String value = e.getAttribute(attribute);
              
              System.out.println("Current: " + value);
              
              canvas.setDocument(d);
              
              e.setAttributeNS(null, attribute, "rotate(" + holder.get() + " 61.542 61.792)");
            }
          });
          
          holder.put(holder.get() + 1);
        }
      }
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }
}
