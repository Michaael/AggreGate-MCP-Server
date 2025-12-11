package com.tibbo.aggregate.client.workspace;

import java.beans.*;
import java.io.*;
import java.security.spec.*;
import java.text.*;
import java.util.*;
import javax.swing.*;

import com.jidesoft.editor.*;
import com.jidesoft.editor.action.*;
import com.jidesoft.shortcut.*;
import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.client.util.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.component.*;
import org.apache.log4j.*;

public class WorkspaceManager
{
  public final static String WORKSPACE_FILE_NAME = "workspace.dat";
  public final static String WORKSPACES_SUBDIR = "workspaces/";
  
  private static final String OLD_EXTENSION = ".old";
  private static final String TMP_EXTENSION = ".tmp";
  
  private final ByteArrayPersistenceDelegate byteArrayPersister = new ByteArrayPersistenceDelegate();
  
  public WorkspaceManager()
  {
    super();
  }
  
  private static String getWorkspacesDir()
  {
    return ComponentHelper.getConfig().getDataDirectory() + WorkspaceManager.WORKSPACES_SUBDIR;
  }
  
  private static String getWorkspaceDir(String username)
  {
    return getWorkspacesDir() + username + "/";
  }
  
  public synchronized Workspace loadWorkspace(String name, String password) throws AggreGateException, InvalidKeySpecException
  {
    Workspace workspace = null;
    
    String dir = getWorkspaceDir(name);
    String workspaceFile = dir + WorkspaceManager.WORKSPACE_FILE_NAME;
    
    try
    {
      FileInputStream fis = new FileInputStream(workspaceFile);
      
      InputStream decryptedStream = new DesEncrypter(password).decryptedStream(new BufferedInputStream(fis));
      
      XMLDecoder decoder = new XMLDecoder(decryptedStream);
      
      decoder.setExceptionListener(new ExceptionListener()
      {
        @Override
        public void exceptionThrown(Exception ex)
        {
          Log.CORE.warn("Error while loading workspace", ex);
        }
      });
      
      workspace = (Workspace) decoder.readObject();
      decoder.close();
    }
    catch (FileNotFoundException ex1)
    {
      if (ComponentHelper.isCreateWorkspace()
          || (!ComponentHelper.isSimpleMode() && JOptionPane.showConfirmDialog(null, MessageFormat.format(Pres.get().getString("wsNotExistCreate"), name), Pres.get().getString("wsNotExist"),
              JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION))
      {
        try
        {
          saveWorkspace(new Workspace(), name, password);
        }
        catch (IOException ex)
        {
          throw new AggreGateException(ex.getMessage(), ex);
        }
      }
      else
      {
        throw new AggreGateException(Pres.get().getString("wsNotCreated"));
      }
    }
    catch (InvalidKeySpecException ex1) // "Password is not ASCII" and similar errors
    {
      ClientUtils.showError(Level.ERROR, null, Pres.get().getString("mErrSavingWorkspace"), ex1);
      return null;
    }
    catch (NoSuchElementException ex1) // The only known indication of "invalid password" situation
    {
      return null;
    }
    catch (Exception ex)
    {
      throw new AggreGateException(Pres.get().getString("wsErrLoadingWorkspace") + ex.getMessage(), ex);
    }
    
    if (workspace == null)
    {
      workspace = new Workspace();
    }
    
    byte[] layout = workspace.getLayoutData(null);
    
    if (layout != null)
    {
      ComponentHelper.getMainFrame().getDockingManager().resetToDefault();
      ClientUtils.applyLayoutData(ComponentHelper.getMainFrame().getDockingManager(), layout);
    }
    
    ComponentHelper.getMainFrame().getFrame().setTitle(ComponentHelper.getMainFrame().getDefaultTitle() + " - " + name);
    
    byte[] shortcuts = workspace.getShortcuts();
    
    if (shortcuts != null)
    {
      try
      {
        DefaultInputHandler inputHandler = (DefaultInputHandler) DefaultSettings.getDefaults().getInputHandler();
        
        ShortcutPersistenceUtils.load(inputHandler.getShortcutSchemaManager(), new ByteArrayInputStream(shortcuts));
      }
      catch (Exception ex)
      {
        Log.CLIENTS.error("Error loading code editor shortcuts", ex);
      }
    }
    
    return workspace;
  }
  
  public synchronized void saveWorkspace(Workspace workspace, String name, String password) throws IOException, InvalidKeySpecException
  {
    String dir = getWorkspaceDir(name);
    String workspaceFile = dir + WorkspaceManager.WORKSPACE_FILE_NAME;
    String workspaceTempFile = workspaceFile + TMP_EXTENSION;
    String workspaceOldFile = workspaceFile + OLD_EXTENSION;
    
    final File file = new File(workspaceFile);
    final File tempFile = new File(workspaceTempFile);
    final File oldFile = new File(workspaceOldFile);
    
    File fDir = new File(dir);
    if (!fDir.exists())
    {
      if (!new File(dir).mkdirs())
      {
        throw new IOException("Can't create directory '" + dir + "'");
      }
    }
    
    workspace.setFirstUse(false);
    
    if (ComponentHelper.getMainFrame() != null)
    {
      workspace.setLayoutData(null, ClientUtils.getLayoutData(ComponentHelper.getMainFrame().getDockingManager()));
    }
    
    try
    {
      DefaultInputHandler inputHandler = (DefaultInputHandler) DefaultSettings.getDefaults().getInputHandler();
      
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      
      ShortcutPersistenceUtils.save(inputHandler.getShortcutSchemaManager(), stream);
      
      workspace.setShortcuts(stream.toByteArray());
    }
    catch (Exception ex)
    {
      Log.CLIENTS.error("Error saving code editor shortcuts", ex);
    }
    
    Log.CORE.debug("Writing workspace to output stream");
    
    FileOutputStream fos = new FileOutputStream(tempFile, false);
    
    OutputStream encryptedStream = new DesEncrypter(password).encryptedStream(new BufferedOutputStream(fos));
    
    XMLEncoder encoder = new WorkspaceEncoder(encryptedStream);
    
    encoder.setPersistenceDelegate(List.class, encoder.getPersistenceDelegate(AbstractList.class));
    
    encoder.setExceptionListener(new ExceptionListener()
    {
      @Override
      public void exceptionThrown(Exception ex)
      {
        Log.CORE.warn("Error while saving workspace", ex);
      }
    });
    
    encoder.writeObject(workspace);
    encoder.close();
    
    fos.close();
    
    oldFile.delete();
    
    if (file.exists())
    {
      org.apache.commons.io.FileUtils.moveFile(file, oldFile);
    }
    
    org.apache.commons.io.FileUtils.moveFile(tempFile, file);
    
    if (!oldFile.delete())
    {
      Log.CLIENTS.warn("Error deleting old workspace file");
    }
  }
  
  public static void saveWorkspace()
  {
    if (Client.getParameters().isRemote() || Client.getParameters().isKerberos())
    {
      return;
    }
    
    try
    {
      ComponentHelper.getMainFrame().saveFrameData();
      Client.getWorkspaceManager().saveWorkspace(Client.getWorkspace(), ComponentHelper.getUsername(), ComponentHelper.getPassword());
    }
    catch (Exception ex)
    {
      ClientUtils.showError(Level.ERROR, null, Pres.get().getString("mErrSavingWorkspace"), ex);
    }
  }
  
  public List<String> getWorkspaceNames()
  {
    List<String> res = new LinkedList<>();
    
    File workspaces = new File(getWorkspacesDir());
    if (workspaces.exists())
    {
      File[] dirs = workspaces.listFiles();
      for (File file : dirs) {
        if (file.isDirectory()) {
          res.add(file.getName());
        }
      }
    }
    
    return res;
  }
  
  private final class WorkspaceEncoder extends XMLEncoder
  {
    private WorkspaceEncoder(OutputStream out)
    {
      super(out);
    }
    
    @Override
    public PersistenceDelegate getPersistenceDelegate(Class<?> type)
    {
      if (type == byte[].class)
        return byteArrayPersister;
      else
        return super.getPersistenceDelegate(type);
    }
  }
  
}
