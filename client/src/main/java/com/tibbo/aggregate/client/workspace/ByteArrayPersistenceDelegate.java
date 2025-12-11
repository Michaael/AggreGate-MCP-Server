package com.tibbo.aggregate.client.workspace;

import java.beans.*;

public class ByteArrayPersistenceDelegate extends PersistenceDelegate
{
  @Override
  protected Expression instantiate(Object oldInstance, Encoder out)
  {
    byte[] e = (byte[]) oldInstance;
    return new Expression(e, ByteArrayPersistenceDelegate.class, "decode", new Object[] { ByteArrayPersistenceDelegate.encode(e) });
  }
  
  public static byte[] decode(String encoded)
  {
    return org.apache.commons.codec.binary.Base64.decodeBase64(encoded);
  }
  
  public static String encode(byte[] data)
  {
    return org.apache.commons.codec.binary.Base64.encodeBase64String(data);
  }
}