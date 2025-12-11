package com.tibbo.aggregate.client.macro.persistence;

import java.io.*;

import com.tibbo.aggregate.client.macro.model.*;

public interface MacroStorage
{
  Macro[] listMacros();
  
  Macro loadMacro(String fileName) throws FileNotFoundException;
  
  void saveMacro(Macro macro, String fileName);
  
  void deleteMacro(String fileName);
  
  InputStream loadResource(String fileName) throws FileNotFoundException;
}
