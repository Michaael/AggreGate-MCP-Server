package com.tibbo.aggregate.client.macro.persistence;

public abstract class MacroStorageManager
{
  private static MacroStorage defaultStorage = new FileMacroStorage();
  
  public static MacroStorage getDefaultStorage()
  {
    return defaultStorage;
  }
}
