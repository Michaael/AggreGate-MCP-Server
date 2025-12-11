package com.tibbo.aggregate.client.macro.persistence;

import java.io.*;
import java.util.*;

import com.tibbo.aggregate.client.macro.model.*;
import com.tibbo.aggregate.common.*;

public class FileMacroStorage implements MacroStorage
{
  public static String MACROS_DIRECTORY = "macro";
  
  private final File targetDirectory = new File(MACROS_DIRECTORY);
  
  public FileMacroStorage()
  {
    targetDirectory.mkdirs();
  }
  
  public void deleteMacro(String fileName)
  {
    File target = new File(targetDirectory, fileName);
    if (!target.exists())
    {
      return;
    }
    target.delete();
  }
  
  public Macro[] listMacros()
  {
    File[] files = targetDirectory.listFiles();
    
    if (files == null)
    {
      return new Macro[0];
    }
    
    List<Macro> macros = new LinkedList();
    
    for (int i = 0; i < files.length; i++)
    {
      File f = files[i];
      
      if (f.isDirectory() || !f.getName().endsWith(".xml"))
      {
        continue;
      }
      
      try
      {
        Macro macro = loadMacro(f.getName());
        macros.add(macro);
      }
      catch (Exception ex)
      {
        Log.GUIDE.error("Error loading macro file: " + f, ex);
      }
    }
    
    return macros.toArray(new Macro[macros.size()]);
  }
  
  public Macro loadMacro(String fileName) throws FileNotFoundException
  {
    File target = new File(targetDirectory, fileName);
    
    Macro macro = MacroXmlCodec.decode(new FileReader(target));
    
    macro.setFileName(fileName);
    
    return macro;
  }
  
  public InputStream loadResource(String fileName) throws FileNotFoundException
  {
    File target = new File(targetDirectory, fileName);
    
    return new FileInputStream(target);
  }
  
  public void saveMacro(Macro macro, String fileName)
  {
    macro.setFileName(fileName);
    
    File target = new File(targetDirectory, fileName);
    
    String macroXml = MacroXmlCodec.encode(macro);
    
    try
    {
      FileWriter writer = new FileWriter(target);
      writer.write(macroXml);
      writer.close();
    }
    catch (IOException ex)
    {
      throw new RuntimeException("Error saving macro", ex);
    }
  }
}
