package com.tibbo.aggregate.common.util;

/*
 * General purpose class containing common <code>Object</code> manipulation
 * methods.
 */
import java.awt.*;
import java.lang.reflect.*;
import java.util.*;

import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.datatable.converter.*;

public class CloneUtils
{
  public static Object genericClone(Object object)
  {
    if (object == null)
    {
      return null;
    }
    
    if (Object.class == object.getClass() || object instanceof String || object instanceof Number || object instanceof Boolean || object instanceof Character || object instanceof Throwable)
    {
      return object;
    }
    
    if (object instanceof PublicCloneable)
    {
      return ((PublicCloneable) object).clone();
    }
    
    if (object instanceof Date)
    {
      return ((Date) object).clone();
    }
    
    if (object instanceof Color)
    {
      Color c = (Color) object;
      return new Color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }
    
    if (object instanceof ArrayList)
    {
      return ((ArrayList) object).clone();
    }
    
    if (object instanceof LinkedList)
    {
      return ((LinkedList) object).clone();
    }
    
    if (object instanceof HashMap)
    {
      return ((HashMap) object).clone();
    }
    
    if (object instanceof Hashtable)
    {
      return ((Hashtable) object).clone();
    }
    
    FormatConverter converter = DataTableConversion.getFormatConverter(object.getClass());
    if (converter != null)
    {
      return converter.clone(object, true);
    }
    
    if (!(object instanceof Cloneable))
    {
      throw new IllegalStateException("Object is not cloneable: " + object + " (" + object.getClass().getName() + ")");
    }
    
    try
    {
      if (Log.CORE.isDebugEnabled())
      {
        Log.CORE.debug("Using slow reflection cloning for: " + object.getClass().getName(), new Exception());
      }
      
      Method method = object.getClass().getMethod("clone", (Class[]) null);
      method.setAccessible(true);
      return method.invoke(object, (Object[]) null);
    }
    catch (Exception e)
    {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }
  
  /**
   * Try to create a deep clone of the provides object. This handles arrays, collections and maps. If the class in not a supported standard JDK collection type the <code>genericClone</code> will be
   * used instead.
   * 
   * @param object
   *          The object to be copied.
   */
  public static Object deepClone(Object object)
  {
    if (null == object)
    {
      return null;
    }
    
    String classname = object.getClass().getName();
    
    // check if it's an array
    if ('[' == classname.charAt(0))
    {
      // handle 1 dimensional primitive arrays
      if (classname.charAt(1) != '[' && classname.charAt(1) != 'L')
      {
        switch (classname.charAt(1))
        {
          case 'B':
            return ((byte[]) object).clone();
          case 'Z':
            return ((boolean[]) object).clone();
          case 'C':
            return ((char[]) object).clone();
          case 'S':
            return ((short[]) object).clone();
          case 'I':
            return ((int[]) object).clone();
          case 'J':
            return ((long[]) object).clone();
          case 'F':
            return ((float[]) object).clone();
          case 'D':
            return ((double[]) object).clone();
          default:
            throw new IllegalStateException("Unknown primitive array class: " + classname);
        }
      }
      
      // get the base type and the dimension count of the array
      int dimension_count = 1;
      while (classname.charAt(dimension_count) == '[')
      {
        dimension_count += 1;
      }
      Class baseClass = null;
      if (classname.charAt(dimension_count) != 'L')
      {
        baseClass = getBaseClass(object);
      }
      else
      {
        try
        {
          baseClass = Class.forName(classname.substring(dimension_count + 1, classname.length() - 1));
        }
        catch (ClassNotFoundException e)
        {
          throw new IllegalStateException(e.getMessage(), e);
        }
      }
      
      // instantiate the array but make all but the first dimension 0.
      int[] dimensions = new int[dimension_count];
      dimensions[0] = Array.getLength(object);
      for (int i = 1; i < dimension_count; i += 1)
      {
        dimensions[i] = 0;
      }
      Object copy = Array.newInstance(baseClass, dimensions);
      
      // now fill in the next level down by recursion.
      for (int i = 0; i < dimensions[0]; i += 1)
      {
        Array.set(copy, i, deepClone(Array.get(object, i)));
      }
      
      return copy;
    }
    // handle cloneable collections
    else if (object instanceof Collection && object instanceof Cloneable)
    {
      Collection collection = (Collection) object;
      
      // instantiate the new collection and clear it
      Collection copy = (Collection) CloneUtils.genericClone(object);
      copy.clear();
      
      // clone all the values in the collection individually
      Iterator collection_it = collection.iterator();
      while (collection_it.hasNext())
      {
        copy.add(deepClone(collection_it.next()));
      }
      
      return copy;
    }
    // handle cloneable maps
    else if (object instanceof Map && object instanceof Cloneable)
    {
      Map map = (Map) object;
      
      // instantiate the new map and clear it
      Map copy = (Map) CloneUtils.genericClone(object);
      copy.clear();
      
      // now clone all the keys and values of the entries
      Iterator collection_it = map.entrySet().iterator();
      Map.Entry entry = null;
      while (collection_it.hasNext())
      {
        entry = (Map.Entry) collection_it.next();
        copy.put(deepClone(entry.getKey()), deepClone(entry.getValue()));
      }
      
      return copy;
    }
    // use the generic clone method
    else
    {
      Object copy = CloneUtils.genericClone(object);
      if (null == copy)
      {
        throw new IllegalStateException("Clone not supported: " + object.getClass().getName());
      }
      return copy;
    }
  }
  
  /**
   * This routine returns the base class of an object. This is just the class of the object for non-arrays.
   * 
   * @param object
   *          The object whose base class you want to retrieve.
   */
  private static Class getBaseClass(Object object)
  {
    if (object == null)
    {
      return Void.TYPE;
    }
    
    String className = object.getClass().getName();
    
    // skip forward over the array dimensions
    int dims = 0;
    while (className.charAt(dims) == '[')
    {
      dims += 1;
    }
    
    // if there were no array dimensions, just return the class of the
    // provided object
    if (dims == 0)
    {
      return object.getClass();
    }
    
    switch (className.charAt(dims))
    {
    // handle the boxed primitives
      case 'Z':
        return Boolean.TYPE;
      case 'B':
        return Byte.TYPE;
      case 'S':
        return Short.TYPE;
      case 'C':
        return Character.TYPE;
      case 'I':
        return Integer.TYPE;
      case 'J':
        return Long.TYPE;
      case 'F':
        return Float.TYPE;
      case 'D':
        return Double.TYPE;
        // look up the class of another reference type
      case 'L':
        try
        {
          return Class.forName(className.substring(dims + 1, className.length() - 1));
        }
        catch (ClassNotFoundException e)
        {
          return null;
        }
      default:
        return null;
    }
  }
}
