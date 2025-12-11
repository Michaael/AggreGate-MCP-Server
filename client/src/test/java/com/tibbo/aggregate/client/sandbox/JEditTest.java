package com.tibbo.aggregate.client.sandbox;

import javax.swing.*;

import com.tibbo.aggregate.component.texteditor.*;

public class JEditTest
{
  public static void main(String[] args)
  {
    try
    {
      JFrame frame = new JFrame("Editor");
      
      TextEditor text = new TextEditor(getText(), "java");
      
      frame.getContentPane().add(text);
      
      frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      frame.pack();
      frame.setVisible(true);
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }
  
  private static String getText()
  {
    String className = "DefaultScript";
    
    StringBuilder sb = new StringBuilder();
    append(sb, "import com.tibbo.aggregate.common.datatable.*;");
    append(sb, "import com.tibbo.linkserver.script.*;");
    append(sb, "");
    append(sb, "public class " + className + " implements Script");
    append(sb, "{");
    append(sb, "  public Object execute(ScriptExecutionEnvironment environment, Object... parameters) throws ScriptException");
    append(sb, "  {");
    append(sb, "    return null;");
    append(sb, "  }");
    append(sb, "}");
    return sb.toString();
  }
  
  private static void append(StringBuilder sb, String s)
  {
    sb.append(s);
    sb.append("\n");
  }
}
