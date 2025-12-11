package com.tibbo.aggregate.client.macro.persistence;

import java.io.*;

import com.thoughtworks.xstream.*;
import com.tibbo.aggregate.client.macro.model.*;

public abstract class MacroXmlCodec
{
  private static final XStream xstream = new XStream();
  
  static
  {
    xstream.alias("macro", Macro.class);
    xstream.alias("operation", InvokeOperationOperation.class);
    xstream.alias("html", HtmlStepDescription.class);
    xstream.alias("text", PlainTextDescription.class);
    xstream.alias("showDescriptions", ShowDescriptionOperation.class);
    xstream.alias("editTable", EditTableOperation.class);
    xstream.alias("editTableCell", EditTableCellOperation.class);
    xstream.alias("editProperties", EditPropertiesOperation.class);
  }
  
  public static String encode(Macro macro)
  {
    return xstream.toXML(macro);
  }
  
  public static Macro decode(Reader macroXml)
  {
    Macro macro = (Macro) xstream.fromXML(macroXml, new Macro());
    try
    {
      macroXml.close();
    }
    catch (IOException ex)
    {
      throw new RuntimeException(ex);
    }
    return macro;
  }
}
