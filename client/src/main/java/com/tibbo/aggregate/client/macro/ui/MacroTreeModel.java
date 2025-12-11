package com.tibbo.aggregate.client.macro.ui;

import java.util.*;

import javax.swing.event.*;
import javax.swing.tree.*;

import com.tibbo.aggregate.client.macro.model.*;

public class MacroTreeModel implements TreeModel
{
  private Macro macro;
  protected EventListenerList listenerList = new EventListenerList();
  
  public MacroTreeModel(Macro macro)
  {
    if (macro == null)
    {
      throw new IllegalArgumentException("macro is null");
    }
    else
    {
      this.macro = macro;
    }
  }
  
  public Object getRoot()
  {
    return macro;
  }
  
  public Object getChild(Object parent, int index)
  {
    if (!(parent instanceof AtomicStep))
    {
      return null;
    }
    
    AtomicStep atomicStep = (AtomicStep) parent;
    List<Step> steps = atomicStep.listSteps();
    
    if (steps.size() <= index)
    {
      return null;
    }
    
    Step step = null;
    Iterator<Step> i = steps.iterator();
    for (int j = 0; j <= index; j++)
    {
      step = i.next();
    }
    
    return step;
  }
  
  public int getChildCount(Object parent)
  {
    if (!(parent instanceof AtomicStep))
    {
      return 0;
    }
    
    AtomicStep atomicStep = (AtomicStep) parent;
    List<Step> steps = atomicStep.listSteps();
    
    return steps.size();
  }
  
  public boolean isLeaf(Object node)
  {
    return !(node instanceof AtomicStep);
  }
  
  public int getIndexOfChild(Object parent, Object child)
  {
    if (!(parent instanceof AtomicStep))
    {
      return 0;
    }
    
    AtomicStep atomicStep = (AtomicStep) parent;
    List<Step> steps = atomicStep.listSteps();
    
    List<Step> stepList = new LinkedList(steps);
    
    return stepList.indexOf(child);
  }
  
  public void valueForPathChanged(TreePath path, Object newValue)
  {
  }
  
  public void addTreeModelListener(TreeModelListener l)
  {
    listenerList.add(TreeModelListener.class, l);
  }
  
  public void removeTreeModelListener(TreeModelListener l)
  {
    listenerList.remove(TreeModelListener.class, l);
  }
  
  public TreeModelListener[] getTreeModelListeners()
  {
    return (TreeModelListener[]) listenerList.getListeners(TreeModelListener.class);
  }
  
  public void fireTreeNodesChanged(Object source, Object[] path, int[] childIndices, Object[] children)
  {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    TreeModelEvent e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2)
    {
      if (listeners[i] == TreeModelListener.class)
      {
        // Lazily create the event:
        if (e == null)
        {
          e = new TreeModelEvent(source, path, childIndices, children);
        }
        ((TreeModelListener) listeners[i + 1]).treeNodesChanged(e);
      }
    }
  }
  
  public void fireTreeNodesInserted(Object source, Object[] path, int[] childIndices, Object[] children)
  {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    TreeModelEvent e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2)
    {
      if (listeners[i] == TreeModelListener.class)
      {
        // Lazily create the event:
        if (e == null)
        {
          e = new TreeModelEvent(source, path, childIndices, children);
        }
        ((TreeModelListener) listeners[i + 1]).treeNodesInserted(e);
      }
    }
  }
  
  public void fireTreeNodesRemoved(Object source, Object[] path, int[] childIndices, Object[] children)
  {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    TreeModelEvent e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2)
    {
      if (listeners[i] == TreeModelListener.class)
      {
        // Lazily create the event:
        if (e == null)
        {
          e = new TreeModelEvent(source, path, childIndices, children);
        }
        ((TreeModelListener) listeners[i + 1]).treeNodesRemoved(e);
      }
    }
  }
  
  public void fireTreeStructureChanged(Object source, Object[] path, int[] childIndices, Object[] children)
  {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    TreeModelEvent e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2)
    {
      if (listeners[i] == TreeModelListener.class)
      {
        // Lazily create the event:
        if (e == null)
        {
          e = new TreeModelEvent(source, path, childIndices, children);
        }
        ((TreeModelListener) listeners[i + 1]).treeStructureChanged(e);
      }
    }
  }
  
  public void fireTreeStructureChanged(Object source, TreePath path)
  {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    TreeModelEvent e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2)
    {
      if (listeners[i] == TreeModelListener.class)
      {
        // Lazily create the event:
        if (e == null)
        {
          e = new TreeModelEvent(source, path);
        }
        ((TreeModelListener) listeners[i + 1]).treeStructureChanged(e);
      }
    }
  }
}
