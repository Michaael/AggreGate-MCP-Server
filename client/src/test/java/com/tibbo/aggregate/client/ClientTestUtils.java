package com.tibbo.aggregate.client;

import java.util.*;

import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.tests.*;
import com.tibbo.aggregate.component.*;
import com.tibbo.aggregate.component.systemtree.*;
import com.tibbo.aggregate.component.systemtree.context.*;

public abstract class ClientTestUtils
{
  /**
   * Wait while the System Tree context being available. After a default timeout the method will return.
   * 
   * @throws Exception
   * @throws AssertionError
   */
  public static SystemTreeContext waitForSystemTreeContext(final String contextPath) throws AssertionError, Exception
  {
    ClientContainer mf = ComponentHelper.getMainFrame();
    if (mf == null)
    {
      throw new IllegalStateException("ComponentHelper.getMainFrame() returned null.");
    }
    
    SystemTree st = mf.getSystemTree();
    if (st == null)
    {
      throw new IllegalStateException("MainFrame.getSystemTree() returned null.");
    }
    
    final ContextManager<SystemTreeContext> cm = st.getContextManager();
    if (cm == null)
    {
      throw new IllegalStateException("SystemTree.getContextManager() returned null.");
    }
    
    Condition cond = new Condition()
    {
      public boolean check()
      {
        return cm.get(contextPath) != null && cm.get(contextPath).isSetupComplete();
      }
    };
    
    if (!AggreGateTestingUtils.wait(10000, 10, cond))
    {
      throw new AssertionError("Context not available: " + contextPath);
    }
    
    return cm.get(contextPath);
  }
  
  public static SystemTreeDeviceContext getContext(SystemTreeDeviceRootContext root, String path) throws Exception
  {
    SystemTreeDeviceContext<SystemTreeContext> curCtx = root;
    
    StringTokenizer st = new StringTokenizer(path, ContextUtils.CONTEXT_NAME_SEPARATOR);
    while (st.hasMoreTokens())
    {
      SystemTreeDeviceContext nextCtx = null;
      
      if (curCtx.isDynamic())
      {
        curCtx.load();
        curCtx.loadChildren();
      }
      
      if (!AggreGateTestingUtils.wait(5000, 10, new ChildrenLoadedCondition(curCtx)))
      {
        throw new Exception("Children of system tree context were not loaded: " + curCtx.getPath());
      }
      
      String name = st.nextToken();
      for (Context child : curCtx.getChildren())
      {
        if (!(child instanceof SystemTreeDeviceContext))
        {
          continue;
        }
        SystemTreeDeviceContext dmfCtx = (SystemTreeDeviceContext) child;
        
        if (name.equals(dmfCtx.getRemoteContext().getName()))
        {
          if (nextCtx != null)
          {
            throw new IllegalStateException("More then one contexts match the path");
          }
          
          nextCtx = dmfCtx;
        }
      }
      
      curCtx = nextCtx;
      
      if (curCtx == null)
      {
        return null;
      }
    }
    
    return curCtx;
  }
  
  // Type 1: lower case letters only
  // Type 2: lower case letters and numbers
  // Type 3: letters and numbers
  public static String createUniqueID(int length, int type)
  {
    StringBuffer chal = new StringBuffer();
    Random r = new Random();
    for (int i = 0; i < length; i++)
    {
      int rng = r.nextInt(type);
      switch (rng)
      {
        case 0:
          chal.append((char) ('a' + r.nextInt(26)));
          break;
        case 1:
          chal.append((char) ('0' + r.nextInt(10)));
          break;
        case 2:
          chal.append((char) ('A' + r.nextInt(26)));
          break;
      }
    }
    
    return chal.toString();
  }
  
  private static class ChildrenLoadedCondition implements Condition
  {
    private final SystemTreeContext checked;
    
    ChildrenLoadedCondition(SystemTreeContext checked)
    {
      this.checked = checked;
    }
    
    public boolean check()
    {
      return checked.isChildrenLoaded();
    }
  };
}
