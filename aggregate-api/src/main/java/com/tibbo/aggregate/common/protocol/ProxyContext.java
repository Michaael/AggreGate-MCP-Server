package com.tibbo.aggregate.common.protocol;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Level;

import com.tibbo.aggregate.common.Cres;
import com.tibbo.aggregate.common.Log;
import com.tibbo.aggregate.common.action.ActionDefinition;
import com.tibbo.aggregate.common.context.AbstractContext;
import com.tibbo.aggregate.common.context.ActionConstants;
import com.tibbo.aggregate.common.context.CallerController;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextRuntimeException;
import com.tibbo.aggregate.common.context.ContextStatus;
import com.tibbo.aggregate.common.context.ContextUtils;
import com.tibbo.aggregate.common.context.Contexts;
import com.tibbo.aggregate.common.context.DefaultContextEventListener;
import com.tibbo.aggregate.common.context.DefaultContextVisitor;
import com.tibbo.aggregate.common.context.EventData;
import com.tibbo.aggregate.common.context.EventDefinition;
import com.tibbo.aggregate.common.context.FunctionDefinition;
import com.tibbo.aggregate.common.context.RequestController;
import com.tibbo.aggregate.common.context.UncheckedCallerController;
import com.tibbo.aggregate.common.context.VariableDefinition;
import com.tibbo.aggregate.common.data.Event;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.SimpleDataTable;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.common.datatable.encoding.ClassicEncodingSettings;
import com.tibbo.aggregate.common.datatable.encoding.FormatCache;
import com.tibbo.aggregate.common.device.DisconnectionException;
import com.tibbo.aggregate.common.device.RemoteDeviceErrorException;
import com.tibbo.aggregate.common.event.ContextEventListener;
import com.tibbo.aggregate.common.event.EventHandlingException;
import com.tibbo.aggregate.common.event.FireEventRequestController;
import com.tibbo.aggregate.common.security.Permissions;
import com.tibbo.aggregate.common.server.ServerContextConstants;
import com.tibbo.aggregate.common.util.SyntaxErrorException;
import com.tibbo.aggregate.common.util.Util;

public class ProxyContext<C extends Context> extends AbstractContext<C>
{
  private static final long METADATA_READ_TIMEOUT = 120000;
  
  private static final long LISTENER_OPERATIONS_TIMEOUT = 120000;
  
  public static final long DURABLE_OPERATIONS_TIMEOUT = 600000;
  
  public static final String F_LOCAL_REINITIALIZE = "localReinitialize";
  
  private final AbstractAggreGateDeviceController controller;
  
  private boolean notManageRemoteListeners;
  
  private boolean localInitComplete = false;
  
  private boolean initializingInfo = false;
  private boolean initializedInfo = false;
  private final Object initializingInfoLock = new Object();
  
  private boolean initializingChildren = false;
  private boolean initializedChildren = false;
  private final Object initializingChildrenLock = new Object();
  
  private boolean initializingVariables = false;
  private boolean initializedVariables = false;
  private final Object initializingVariablesLock = new Object();
  
  private boolean initializingFunctions = false;
  private boolean initializedFunctions = false;
  private final Object initializingFunctionsLock = new Object();
  
  private boolean initializingEvents = false;
  private boolean initializedEvents = false;
  private final Object initializingEventsLock = new Object();
  
  private boolean initializingActions = false;
  private boolean initializedActions = false;
  private final Object initializingActionsLock = new Object();
  
  private boolean initializingStatus = false;
  private boolean initializedStatus = false;
  private final Object initializingStatusLock = new Object();
  
  private boolean initializingVisibleChildren = false;
  private final Object initializingVisibleChildrenLock = new Object();
  
  private Collection<String> visibleChildren;
  
  private String localRoot;
  private String peerRoot;
  private String peerPrimaryRoot;
  
  private String remoteRoot;
  private String remotePath;
  
  private boolean mapped;
  
  private boolean container;

  private final Map<String, SoftReference<CachedVariableValue>> variableCache = new HashMap();
  private final ReentrantReadWriteLock variableCacheLock = new ReentrantReadWriteLock();
  
  private static final List<String> AUTO_LISTENED_EVENTS = new LinkedList();
  
  static
  {
    AUTO_LISTENED_EVENTS.add(AbstractContext.E_CHILD_ADDED);
    AUTO_LISTENED_EVENTS.add(AbstractContext.E_CHILD_REMOVED);
    AUTO_LISTENED_EVENTS.add(AbstractContext.E_VARIABLE_ADDED);
    AUTO_LISTENED_EVENTS.add(AbstractContext.E_VARIABLE_REMOVED);
    AUTO_LISTENED_EVENTS.add(AbstractContext.E_FUNCTION_ADDED);
    AUTO_LISTENED_EVENTS.add(AbstractContext.E_FUNCTION_REMOVED);
    AUTO_LISTENED_EVENTS.add(AbstractContext.E_EVENT_ADDED);
    AUTO_LISTENED_EVENTS.add(AbstractContext.E_EVENT_REMOVED);
    AUTO_LISTENED_EVENTS.add(AbstractContext.E_INFO_CHANGED);
    AUTO_LISTENED_EVENTS.add(AbstractContext.E_DESTROYED);
    AUTO_LISTENED_EVENTS.add(AbstractContext.E_ACTION_ADDED);
    AUTO_LISTENED_EVENTS.add(AbstractContext.E_ACTION_REMOVED);
    AUTO_LISTENED_EVENTS.add(AbstractContext.E_ACTION_STATE_CHANGED);
    
    AUTO_LISTENED_EVENTS.add(ServerContextConstants.E_VISIBLE_INFO_CHANGED);
    AUTO_LISTENED_EVENTS.add(ServerContextConstants.E_VISIBLE_CHILD_ADDED);
    AUTO_LISTENED_EVENTS.add(ServerContextConstants.E_VISIBLE_CHILD_REMOVED);
  }
  
  public ProxyContext(String name, AbstractAggreGateDeviceController controller)
  {
    super(name);
    this.controller = controller;
    
    clear();
  }
  
  @Override
  public void setupMyself() throws ContextException
  {
    super.setupMyself();
    
    setFireUpdateEvents(false);
    setPermissionCheckingEnabled(false);
    setChildrenSortingEnabled(false);
    
    addLocalFunctionDefinitions();
    
    localInitComplete = true;
  }
  
  private void addListenersOnInfo()
  {
    addEventListener(E_INFO_CHANGED, new DefaultContextEventListener()
    {
      @Override
      public void handle(Event ev)
      {
        initInfoImpl(ev.getData());
      }
    });
  }
  
  private void addListenersOnActions()
  {
    addEventListener(E_ACTION_ADDED, new DefaultContextEventListener()
    {
      @Override
      public void handle(Event ev)
      {
        ActionDefinition def = actDefFromDataRecord(ev.getData().rec());
        if (getActionDefinition(def.getName()) == null || !getActionDefinition(def.getName()).equals(def))
        {
          addActionDefinition(def);
        }
      }
    });
    
    addEventListener(E_ACTION_REMOVED, new DefaultContextEventListener()
    {
      @Override
      public void handle(Event ev)
      {
        removeActionDefinition(ev.getData().rec().getString(AbstractContext.EF_ACTION_REMOVED_NAME));
      }
    });
    
    addEventListener(E_ACTION_STATE_CHANGED, new DefaultContextEventListener()
    {
      @Override
      public void handle(Event ev)
      {
        ActionDefinition def = actDefFromDataRecord(ev.getData().rec());
        removeActionDefinition(def.getName());
        addActionDefinition(def);
      }
    });
  }
  
  private void addListenersOnEvents()
  {
    addEventListener(E_EVENT_ADDED, new DefaultContextEventListener()
    {
      @Override
      public void handle(Event ev)
      {
        EventDefinition def = evtDefFromDataRecord(ev.getData().rec());
        if (getEventDefinition(def.getName()) == null || !getEventDefinition(def.getName()).equals(def))
        {
          addEventDefinition(def);
        }
      }
    });
    
    addEventListener(E_EVENT_REMOVED, new DefaultContextEventListener()
    {
      @Override
      public void handle(Event ev)
      {
        removeEventDefinition(ev.getData().rec().getString(EF_EVENT_REMOVED_NAME));
      }
    });
  }
  
  private void addListenersOnFunctions()
  {
    addEventListener(E_FUNCTION_ADDED, new DefaultContextEventListener()
    {
      @Override
      public void handle(Event ev)
      {
        FunctionDefinition def = funcDefFromDataRecord(ev.getData().rec());
        if (getFunctionDefinition(def.getName()) == null || !getFunctionDefinition(def.getName()).equals(def))
        {
          addFunctionDefinition(def);
        }
      }
    });
    
    addEventListener(E_FUNCTION_REMOVED, new DefaultContextEventListener()
    {
      @Override
      public void handle(Event ev)
      {
        removeFunctionDefinition(ev.getData().rec().getString(EF_FUNCTION_REMOVED_NAME));
      }
    });
  }
  
  private void addListenersOnVariables()
  {
    addEventListener(E_VARIABLE_ADDED, new DefaultContextEventListener()
    {
      @Override
      public void handle(Event ev)
      {
        VariableDefinition def = varDefFromDataRecord(ev.getData().rec());
        if (getVariableDefinition(def.getName()) == null || !getVariableDefinition(def.getName()).equals(def))
        {
          addVariableDefinition(def);
        }
      }
    });
    
    addEventListener(E_VARIABLE_REMOVED, new DefaultContextEventListener()
    {
      @Override
      public void handle(Event ev)
      {
        removeVariableDefinition(ev.getData().rec().getString(EF_VARIABLE_REMOVED_NAME));
      }
    });
  }
  
  private void addListenersOnChildren()
  {
    addEventListener(E_CHILD_ADDED, new DefaultContextEventListener()
    {
      @Override
      public void handle(Event event) throws EventHandlingException
      {
        String child = event.getData().rec().getString(EF_CHILD_ADDED_CHILD);
        if (getChild(child) == null)
        {
          ProxyContext childProxy = createChildContextProxy(child);
          addChild((C) childProxy);
          if (getContextManager() instanceof RemoteContextManager)
          {
            ((RemoteContextManager) getContextManager()).executeDeferredTasks(childProxy.getPath());
          }
        }
      }
    });
    
    addEventListener(E_CHILD_REMOVED, new DefaultContextEventListener()
    {
      @Override
      public void handle(Event event) throws EventHandlingException
      {
        String child = event.getData().rec().getString(EF_CHILD_REMOVED_CHILD);
        if (child != null)
        {
          removeChild(child);
        }
      }
    });
  }
  
  protected void addLocalFunctionDefinitions()
  {
    addFunctionDefinition(new FunctionDefinition(F_LOCAL_REINITIALIZE, TableFormat.EMPTY_FORMAT, TableFormat.EMPTY_FORMAT));
  }
  
  @Override
  protected TableFormat decodeFormat(String source, CallerController caller)
  {
    if (source == null)
    {
      return null;
    }
    
    StringBuilder idSourceBuilder = new StringBuilder();
    
    int i;
    
    for (i = 0; i < source.length(); i++)
    {
      char c = source.charAt(i);
      if (Character.isDigit(c))
      {
        idSourceBuilder.append(c);
      }
      else
      {
        break;
      }
    }
    
    source = source.substring(i);
    
    String idSource = idSourceBuilder.toString();
    
    Integer formatId = idSource.length() > 0 ? Integer.valueOf(idSource) : null;
    
    TableFormat format = source.length() > 0 ? new TableFormat(source, new ClassicEncodingSettings(false)) : null;
    
    return getFormat(format, formatId);
  }

  private TableFormat getFormat(TableFormat format, Integer formatId)
  {
    if (formatId == null)
    {
      return format;
    }
    else
    {
      if (format == null)
      {
        TableFormat cached = controller.getFormatCache().get(formatId);
        
        if (cached == null)
        {
          throw new IllegalArgumentException("Unknown format ID: " + formatId);
        }
        
        return cached;
      }
      else
      {
        controller.getFormatCache().put(formatId, format);
        return format;
      }
    }
  }
  
  public void clear()
  {
    try
    {
      accept(new DefaultContextVisitor()
      {
        @Override
        public void visit(Context context)
        {
          ProxyContext proxyContext = (ProxyContext) context;

          proxyContext.initializedInfo = false;
          proxyContext.initializingInfo = false;

          proxyContext.initializedChildren = false;
          proxyContext.initializingChildren = false;

          proxyContext.initializedVariables = false;
          proxyContext.initializingVariables = false;

          proxyContext.initializedFunctions = false;
          proxyContext.initializingFunctions = false;

          proxyContext.initializedEvents = false;
          proxyContext.initializingEvents = false;

          proxyContext.initializedActions = false;
          proxyContext.initializingActions = false;

          proxyContext.initializedStatus = false;
          proxyContext.initializingStatus = false;

          proxyContext.initializingVisibleChildren = false;
          proxyContext.visibleChildren = null;
        }
      });
    }
    catch (ContextException ex)
    {
      throw new ContextRuntimeException(ex);
    }
  }

  private void initInfo() throws ContextException
  {
    try
    {
      if (initializedInfo)
      {
        return;
      }
      
      if (controller.getContextManager() != null)
      {
        controller.getContextManager().initialize();
      }
      
      synchronized (initializingInfoLock)
      {
        if (!localInitComplete || initializingInfo)
        {
          return;
        }
        
        try
        {
          initializingInfo = true;
          
          initInfoImpl(getRemoteVariable(INFO_DEFINITION_FORMAT, V_INFO, METADATA_READ_TIMEOUT));
          
          initializedInfo = true;
          
          addListenersOnInfo();
        }
        finally
        {
          initializingInfo = false;
        }
      }
    }
    catch (ContextException ex)
    {
      throw ex;
    }
    catch (Exception ex)
    {
      throw new ContextException(ex);
    }
  }
  
  public void initChildren() throws ContextException
  {
    try
    {
      synchronized (initializingChildrenLock)
      {
        if (initializedChildren)
        {
          return;
        }
        
        if (controller.getContextManager() != null)
        {
          controller.getContextManager().initialize();
        }
        
        if (!localInitComplete || initializingChildren)
        {
          return;
        }
        
        try
        {
          initializingChildren = true;
          
          initChildrenImpl(getRemoteVariable(VFT_CHILDREN, V_CHILDREN, METADATA_READ_TIMEOUT));
          
          initializedChildren = true;
          
          addListenersOnChildren();
        }
        finally
        {
          initializingChildren = false;
        }
      }
    }
    catch (ContextException ex)
    {
      throw ex;
    }
    catch (Exception ex)
    {
      throw new ContextException(ex);
    }
  }
  
  private void initVariables() throws ContextException
  {
    try
    {
      if (initializedVariables)
      {
        return;
      }
      
      if (controller.getContextManager() != null)
      {
        controller.getContextManager().initialize();
      }
      
      synchronized (initializingVariablesLock)
      {
        if (!localInitComplete || initializingVariables)
        {
          return;
        }
        
        try
        {
          initializingVariables = true;
          
          initVariablesImpl(getRemoteVariable(VARIABLE_DEFINITION_FORMAT, V_VARIABLES, METADATA_READ_TIMEOUT));
          
          initializedVariables = true;
          addListenersOnVariables();
        }
        finally
        {
          initializingVariables = false;
        }
      }
    }
    catch (ContextException ex)
    {
      throw ex;
    }
    catch (Exception ex)
    {
      throw new ContextException(ex);
    }
  }
  
  private void initFunctions() throws ContextException
  {
    try
    {
      if (initializedFunctions)
      {
        return;
      }
      
      if (controller.getContextManager() != null)
      {
        controller.getContextManager().initialize();
      }
      
      synchronized (initializingFunctionsLock)
      {
        if (!localInitComplete || initializingFunctions)
        {
          return;
        }
        
        try
        {
          initializingFunctions = true;
          
          initFunctionsImpl(getRemoteVariable(FUNCTION_DEFINITION_FORMAT, V_FUNCTIONS, METADATA_READ_TIMEOUT));
          
          initializedFunctions = true;
          
          addListenersOnFunctions();
        }
        finally
        {
          initializingFunctions = false;
        }
      }
    }
    catch (ContextException ex)
    {
      throw ex;
    }
    catch (Exception ex)
    {
      throw new ContextException(ex);
    }
  }
  
  private void initEvents() throws ContextException
  {
    try
    {
      if (initializedEvents)
      {
        return;
      }
      
      if (controller.getContextManager() != null)
      {
        controller.getContextManager().initialize();
      }
      
      synchronized (initializingEventsLock)
      {
        if (!localInitComplete || initializingEvents)
        {
          return;
        }
        
        try
        {
          initializingEvents = true;
          
          initEventsImpl(getRemoteVariable(EVENT_DEFINITION_FORMAT, V_EVENTS, METADATA_READ_TIMEOUT));
          
          initializedEvents = true;
          
          addListenersOnEvents();
        }
        finally
        {
          initializingEvents = false;
        }
      }
    }
    catch (ContextException ex)
    {
      throw ex;
    }
    catch (Exception ex)
    {
      throw new ContextException(ex);
    }
  }
  
  private void initActions() throws ContextException
  {
    try
    {
      if (initializedActions)
      {
        return;
      }
      
      if (controller.getContextManager() != null)
      {
        controller.getContextManager().initialize();
      }
      
      synchronized (initializingActionsLock)
      {
        if (!localInitComplete || initializingActions)
        {
          return;
        }
        
        try
        {
          initializingActions = true;
          
          initActionsImpl(getRemoteVariable(ACTION_DEF_FORMAT, AbstractContext.V_ACTIONS, METADATA_READ_TIMEOUT));
          
          initializedActions = true;
          
          addListenersOnActions();
        }
        finally
        {
          initializingActions = false;
        }
      }
    }
    catch (ContextException ex)
    {
      throw ex;
    }
    catch (Exception ex)
    {
      throw new ContextException(ex);
    }
  }
  
  protected void initVisibleChildren() throws ContextException
  {
    try
    {
      if (visibleChildren != null)
      {
        return;
      }
      
      if (controller.getContextManager() != null)
      {
        controller.getContextManager().initialize();
      }
      
      synchronized (initializingVisibleChildrenLock)
      {
        if (!localInitComplete || initializingVisibleChildren)
        {
          return;
        }
        
        try
        {
          initializingVisibleChildren = true;
          
          initVisibleChildrenImpl();
        }
        finally
        {
          initializingVisibleChildren = false;
        }
      }
    }
    catch (ContextException ex)
    {
      throw ex;
    }
    catch (Exception ex)
    {
      throw new ContextException(ex);
    }
  }
  
  private void initStatus() throws ContextException
  {
    try
    {
      if (initializedStatus)
      {
        return;
      }
      
      if (controller.getContextManager() != null)
      {
        controller.getContextManager().initialize();
      }
      
      synchronized (initializingStatusLock)
      {
        if (!localInitComplete || initializingStatus)
        {
          return;
        }
        
        try
        {
          initializingStatus = true;
          
          initStatusImpl();
          
          initializedStatus = true;
        }
        finally
        {
          initializingStatus = false;
        }
      }
    }
    catch (ContextException ex)
    {
      throw ex;
    }
    catch (Exception ex)
    {
      throw new ContextException(ex);
    }
  }
  
  private void initInfoImpl(DataTable info)
  {
    setDescription(convertRemoteDescription(info.rec().getString(VF_INFO_DESCRIPTION)));
    setType(info.rec().getString(VF_INFO_TYPE));
    
    if (info.getFormat().hasField(VF_INFO_GROUP))
    {
      setGroup(info.rec().getString(VF_INFO_GROUP));
    }
    
    if (info.getFormat().hasField(VF_INFO_ICON))
    {
      setIconId(info.rec().getString(VF_INFO_ICON));
    }
    
    if (info.getFormat().hasField(VF_INFO_LOCAL_ROOT))
    {
      localRoot = info.rec().getString(VF_INFO_LOCAL_ROOT);
    }
    
    if (info.getFormat().hasField(VF_INFO_PEER_ROOT))
    {
      peerRoot = info.rec().getString(VF_INFO_PEER_ROOT);
    }
    
    if (info.getFormat().hasField(VF_INFO_PEER_PRIMARY_ROOT))
    {
      peerPrimaryRoot = info.rec().getString(VF_INFO_PEER_PRIMARY_ROOT);
    }
    
    if (info.getFormat().hasField(VF_INFO_REMOTE_ROOT))
    {
      remoteRoot = info.rec().getString(VF_INFO_REMOTE_ROOT);
    }
    
    if (info.getFormat().hasField(VF_INFO_REMOTE_PATH))
    {
      remotePath = info.rec().getString(VF_INFO_REMOTE_PATH);
    }
    
    if (info.getFormat().hasField(VF_INFO_MAPPED) && info.rec().getBoolean(VF_INFO_MAPPED) != null)
    {
      mapped = info.rec().getBoolean(VF_INFO_MAPPED);
    }
  }
  
  protected String convertRemoteDescription(String remoteDescription)
  {
    return remoteDescription;
  }
  
  protected void initChildrenImpl(DataTable children)
  {
    removeExistingChildren(children);
    
    for (DataRecord rec : children)
    {
      String cn = rec.getString(VF_CHILDREN_NAME);
      if (getChild(cn) == null)
      {
        addChild((C) createChildContextProxy(cn));
      }
    }
  }
  
  protected void removeExistingChildren(DataTable children)
  {
    for (C child : getChildren(getContextManager().getCallerController()))
    {
      if (children.select(VF_CHILDREN_NAME, child.getName()) == null)
      {
        removeChild(child);
      }
    }
  }

  private void initVisibleChildrenImpl() throws ContextException
  {
    initVariables();
    
    visibleChildren = new LinkedHashSet();
    
    addEventListener(ServerContextConstants.E_VISIBLE_CHILD_ADDED, visibleChildAddedListener);
    
    addEventListener(ServerContextConstants.E_VISIBLE_CHILD_REMOVED, visibleChildRemovedListener);
    
    DataTable visibleChildrenData = getRemoteVariable(getVariableDefinition(ServerContextConstants.V_VISIBLE_CHILDREN));
    
    for (DataRecord rec : visibleChildrenData)
    {
      String localVisiblePath = getLocalVisiblePath(rec.getString(ServerContextConstants.VF_VISIBLE_CHILDREN_PATH));
      if (localVisiblePath != null)
      {
        visibleChildren.add(localVisiblePath);
      }
    }
  }
  
  protected ProxyContext createChildContextProxy(String name)
  {
    ProxyContext proxy = new ProxyContext(name, controller);
    proxy.setNotManageRemoteListeners(isNotManageRemoteListeners());
    return proxy;
  }
  
  private void initVariablesImpl(DataTable variables)
  {
    for (VariableDefinition def : getVariableDefinitions())
    {
      if (variables.select(FIELD_VD_NAME, def.getName()) == null)
      {
        removeVariableDefinition(def.getName());
      }
    }
    
    for (DataRecord rec : variables)
    {
      VariableDefinition def = varDefFromDataRecord(rec);
      VariableDefinition existing = getVariableDefinition(def.getName());
      if (existing == null || !existing.equals(def))
      {
        if (existing != null)
        {
          removeVariableDefinition(existing.getName());
        }
        addVariableDefinition(def);
      }
    }
  }
  
  @Override
  protected Optional<FormatCache> obtainFormatCache()
  {
    // When dealing with remote entities, the format cache must be taken not from the server scope but from current
    // controller because those entities belong to their corresponding mount scopes only.
    return Optional.ofNullable(controller.getFormatCache());
  }

  private void initFunctionsImpl(DataTable functions)
  {
    for (FunctionDefinition def : getFunctionDefinitions())
    {
      if (functions.select(FIELD_FD_NAME, def.getName()) == null)
      {
        removeFunctionDefinition(def.getName());
      }
    }
    
    addLocalFunctionDefinitions();
    
    for (DataRecord rec : functions)
    {
      FunctionDefinition def = funcDefFromDataRecord(rec);
      def.setConcurrent(true); // Concurrency is controlled by the server
      FunctionDefinition existing = getFunctionDefinition(def.getName());
      if (existing == null || !existing.equals(def))
      {
        if (existing != null)
        {
          removeFunctionDefinition(existing.getName());
        }
        addFunctionDefinition(def);
      }
    }
  }
  
  private void initEventsImpl(DataTable events)
  {
    for (EventDefinition def : getEventDefinitions())
    {
      if (events.select(FIELD_ED_NAME, def.getName()) == null)
      {
        removeEventDefinition(def.getName());
      }
    }
    
    for (DataRecord rec : events)
    {
      EventDefinition def = evtDefFromDataRecord(rec);
      EventDefinition existing = getEventDefinition(def.getName());
      if (existing == null || !existing.equals(def))
      {
        if (existing != null)
        {
          removeEventDefinition(existing.getName());
        }
        addEventDefinition(def);
      }
    }
  }
  
  private void initActionsImpl(DataTable actions)
  {
    for (ActionDefinition ad : getActionDefinitions())
    {
      if (actions.select(ActionConstants.FIELD_AD_NAME, ad.getName()) == null)
      {
        removeActionDefinition(ad.getName());
      }
    }
    
    for (DataRecord rec : actions)
    {
      ActionDefinition def = actDefFromDataRecord(rec);
      ActionDefinition existing = getActionDefinition(def.getName());
      if (existing == null || !existing.equals(def))
      {
        if (existing != null)
        {
          removeActionDefinition(existing.getName());
        }
        addActionDefinition(def);
      }
    }
  }
  
  private void initStatusImpl() throws ContextException
  {
    initVariables();
    
    final VariableDefinition statusVariable = getVariableDefinition(ServerContextConstants.V_CONTEXT_STATUS);
    
    if (statusVariable == null)
    {
      return;
    }
    
    enableStatus();
    
    addEventListener(ServerContextConstants.E_CONTEXT_STATUS_CHANGED, contextStatusChangedListener);
    
    DataTable contextStatus = getRemoteVariable(statusVariable);
    
    setStatus(contextStatus.rec().getInt(ServerContextConstants.VF_CONTEXT_STATUS_STATUS), contextStatus.rec().getString(ServerContextConstants.VF_CONTEXT_STATUS_COMMENT));
  }
  
  @Override
  public String getDescription()
  {
    try
    {
      initInfo();
    }
    catch (ContextException ex)
    {
      boolean disconnected = ExceptionUtils.indexOfType(ex, DisconnectionException.class) != -1;
      Log.CONTEXT_VARIABLES.log(disconnected ? Level.DEBUG : Level.WARN, "Error getting description of remote context", ex);
    }
    return super.getDescription();
  }
  
  @Override
  public String getType()
  {
    try
    {
      initInfo();
    }
    catch (ContextException ex)
    {
      throw new ContextRuntimeException("Error getting type of remote context: " + ex.getMessage(), ex);
    }
    return super.getType();
  }
  
  @Override
  public String getLocalRoot(boolean withParent)
  {
    try
    {
      initInfo();
    }
    catch (ContextException ex)
    {
      throw new ContextRuntimeException(ex);
    }
    
    return localRoot;
  }
  
  @Override
  public String getRemoteRoot()
  {
    try
    {
      initInfo();
    }
    catch (ContextException ex)
    {
      throw new ContextRuntimeException(ex);
    }
    
    return remoteRoot;
  }
  
  @Override
  public String getPeerRoot()
  {
    try
    {
      initInfo();
    }
    catch (ContextException ex)
    {
      throw new ContextRuntimeException(ex);
    }
    
    return peerRoot;
  }
  
  @Override
  public boolean isMapped()
  {
    try
    {
      initInfo();
    }
    catch (ContextException ex)
    {
      throw new ContextRuntimeException(ex);
    }
    
    return mapped;
  }
  
  @Override
  public C get(String contextPath, CallerController caller)
  {
    if (contextPath == null)
    {
      return null;
    }
    
    if (ContextUtils.isRelative(contextPath))
    {
      return super.get(contextPath, caller);
    }
    
    String localPath = getLocalPath(contextPath, false);
    
    if (localPath == null)
    {
      return null;
    }
    
    return super.get(localPath, caller);
  }
  
  @Override
  public String getIconId()
  {
    try
    {
      initInfo();
    }
    catch (ContextException ex)
    {
      boolean disconnected = ExceptionUtils.indexOfType(ex, DisconnectionException.class) != -1;
      Log.CONTEXT_VARIABLES.log(disconnected ? Level.DEBUG : Level.WARN, "Error getting icon of remote context", ex);
    }
    return super.getIconId();
  }
  
  @Override
  public C getChild(String name, CallerController callerController)
  {
    if (super.getChild(name, callerController) == null)
    {
      try
      {
        initChildren();
      }
      catch (ContextException ex)
      {
        boolean disconnected = ExceptionUtils.indexOfType(ex, DisconnectionException.class) != -1;
        Log.CONTEXT_CHILDREN.log(disconnected ? Level.DEBUG : Level.WARN, "Error initializing children of remote context", ex);
      }
    }
    return super.getChild(name, callerController);
  }
  
  @Override
  public VariableDefinition getVariableDefinition(String name)
  {
    VariableDefinition sup = super.getVariableDefinition(name);
    if (sup == null && isSetupComplete())
    {
      initVariablesLoggingErrors();
      return super.getVariableDefinition(name);
    }
    else
    {
      return sup;
    }
  }
  
  @Override
  public FunctionDefinition getFunctionDefinition(String name)
  {
    FunctionDefinition sup = super.getFunctionDefinition(name);
    if (sup == null && isSetupComplete())
    {
      initFunctionsLoggingErrors();
      return super.getFunctionDefinition(name);
    }
    else
    {
      return sup;
    }
  }
  
  @Override
  public EventData getEventData(String name)
  {
    EventData sup = super.getEventData(name);
    if (sup == null && isSetupComplete())
    {
      initEventsLoggingErrors();
      return super.getEventData(name);
    }
    else
    {
      return sup;
    }
  }
  
  @Override
  public ActionDefinition getActionDefinition(String name)
  {
    initActionsLoggingErrors();
    return super.getActionDefinition(name);
  }
  
  @Override
  public List<VariableDefinition> getVariableDefinitions(CallerController caller, boolean hidden)
  {
    initVariablesLoggingErrors();
    return super.getVariableDefinitions(caller, hidden);
  }
  
  @Override
  public List<FunctionDefinition> getFunctionDefinitions(CallerController caller, boolean hidden)
  {
    initFunctionsLoggingErrors();
    return super.getFunctionDefinitions(caller, hidden);
  }
  
  @Override
  public List<EventDefinition> getEventDefinitions(CallerController caller, boolean hidden)
  {
    initEventsLoggingErrors();
    return super.getEventDefinitions(caller, hidden);
  }
  
  @Override
  public List<ActionDefinition> getActionDefinitions(CallerController caller, boolean hidden)
  {
    initActionsLoggingErrors();
    return super.getActionDefinitions(caller, hidden);
  }
  
  @Override
  public ContextStatus getStatus()
  {
    initStatusLoggingErrors();
    return super.getStatus();
  }
  
  private void initVariablesLoggingErrors()
  {
    try
    {
      initVariables();
    }
    catch (ContextException ex)
    {
      boolean disconnected = ExceptionUtils.indexOfType(ex, DisconnectionException.class) != -1;
      String message = "Error initializing variables of remote context '" + getPathDescription() + "': " + ex.getMessage();
      Log.CONTEXT_VARIABLES.log(disconnected ? Level.DEBUG : Level.WARN, message, ex);
      throw new ContextRuntimeException(message, ex);
    }
  }
  
  private void initFunctionsLoggingErrors()
  {
    try
    {
      initFunctions();
    }
    catch (ContextException ex)
    {
      boolean disconnected = ExceptionUtils.indexOfType(ex, DisconnectionException.class) != -1;
      String message = "Error initializing functions of remote context '" + getPathDescription() + "': " + ex.getMessage();
      Log.CONTEXT_FUNCTIONS.log(disconnected ? Level.DEBUG : Level.WARN, message, ex);
      throw new ContextRuntimeException(message, ex);
    }
  }
  
  private void initEventsLoggingErrors()
  {
    try
    {
      initEvents();
    }
    catch (ContextException ex)
    {
      boolean disconnected = ExceptionUtils.indexOfType(ex, DisconnectionException.class) != -1;
      String message = "Error initializing events of remote context '" + getPathDescription() + "': " + ex.getMessage();
      Log.CONTEXT_EVENTS.log(disconnected ? Level.DEBUG : Level.WARN, message, ex);
      throw new ContextRuntimeException(message, ex);
    }
  }
  
  private void initActionsLoggingErrors()
  {
    try
    {
      initActions();
    }
    catch (ContextException ex)
    {
      boolean disconnected = ExceptionUtils.indexOfType(ex, DisconnectionException.class) != -1;
      Log.CONTEXT_ACTIONS.log(disconnected ? Level.DEBUG : Level.WARN, "Error initializing actions of remote context '" + getPathDescription() + "': " + ex.getMessage(), ex);
    }
  }
  
  private void initStatusLoggingErrors()
  {
    try
    {
      initStatus();
    }
    catch (ContextException ex)
    {
      boolean disconnected = ExceptionUtils.indexOfType(ex, DisconnectionException.class) != -1;
      Log.CONTEXT.log(disconnected ? Level.DEBUG : Level.WARN, "Error initializing status of remote context '" + getPathDescription() + "': " + ex.getMessage(), ex);
    }
  }
  
  private IncomingAggreGateCommand sendGetVariable(String name, Long timeout) throws DisconnectionException, IOException, ContextException, InterruptedException, RemoteDeviceErrorException
  {
    final OutgoingAggreGateCommand cmd = controller.getCommandBuilder().getVariableOperation(getPeerPath(), name);
    cmd.setTimeout(timeout);
    return controller.sendCommandAndCheckReplyCode(cmd);
  }
  
  DataTable getRemoteVariable(TableFormat format, String name, Long timeout)
      throws DisconnectionException, IOException, ContextException, InterruptedException, RemoteDeviceErrorException, SyntaxErrorException
  {
    String encodedReply = sendGetVariable(name, timeout).getEncodedDataTableFromReply();
    try
    {
      return controller.decodeRemoteDataTable(format, encodedReply);
    }
    catch (Exception ex)
    {
      throw new ContextException("Error parsing encoded data table '" + encodedReply + "': " + ex.getMessage(), ex);
    }
  }
  
  public AbstractAggreGateDeviceController getController()
  {
    return controller;
  }
  
  @Override
  protected void setupVariables() throws ContextException
  {
    initVariables();
    super.setupVariables();
  }
  
  @Override
  protected DataTable getVariableImpl(VariableDefinition def, CallerController caller, RequestController request) throws ContextException
  {
    return getRemoteVariable(def);
  }
  
  public DataTable getRemoteVariable(VariableDefinition def) throws ContextException
  {
    try
    {
      boolean cleanup = false;
      
      if (def.getRemoteCacheTime() != null)
      {
        variableCacheLock.readLock().lock();
        try
        {
          SoftReference<CachedVariableValue> ref = variableCache.get(def.getName());
          if (ref != null)
          {
            CachedVariableValue cachedValue = ref.get();
            if (cachedValue != null)
            {
              if (System.currentTimeMillis() - cachedValue.getTimestamp().getTime() < def.getRemoteCacheTime())
              {
                return cachedValue.getValue();
              }
              else
              {
                cleanup = true;
              }
            }
            else
            {
              cleanup = true;
            }
          }
        }
        finally
        {
          variableCacheLock.readLock().unlock();
        }
        
        if (cleanup)
        {
          variableCacheLock.writeLock().lock();
          try
          {
            variableCache.remove(def.getName());
          }
          finally
          {
            variableCacheLock.writeLock().unlock();
          }
        }
      }
      
      return getRemoteVariableImpl(def);
    }
    catch (Exception ex)
    {
      Log.CONTEXT_VARIABLES.debug("Error getting variable '" + def.getName() + "' from context '" + getPathDescription() + "'", ex);
      throw new ContextException(ex.getMessage(), ex);
    }
  }

  protected DataTable getRemoteVariableImpl(VariableDefinition def)
      throws ContextException, RemoteDeviceErrorException, InterruptedException, DisconnectionException, IOException, SyntaxErrorException
  {
    IncomingAggreGateCommand ans = sendGetVariable(def.getName(), null);

    DataTable value = controller.decodeRemoteDataTable(def.getFormat(), ans.getEncodedDataTableFromReply());

    if (def.getRemoteCacheTime() != null)
    {
      cacheVariableValue(def.getName(), value);
    }

    return value;
  }
  
  @Override
  protected boolean setVariableImpl(VariableDefinition def, CallerController caller, RequestController request, DataTable value) throws ContextException
  {
    try
    {
      final String encoded = value.encode(controller.createClassicEncodingSettings(true));
      OutgoingAggreGateCommand operation = controller.getCommandBuilder().setVariableOperation(getPeerPath(), def.getName(), encoded, request != null ? request.getQueue() : null);
      controller.sendCommandAndCheckReplyCode(operation);
      return true;
    }
    catch (Exception ex)
    {
      Log.CONTEXT_VARIABLES.debug("Error setting variable '" + def.getName() + "' of context '" + getPathDescription() + "'", ex);
      throw new ContextException(ex.getMessage(), ex);
    }
  }
  
  @Override
  protected void setupFunctions() throws ContextException
  {
    initFunctions();
    super.setupFunctions();
  }
  
  @Override
  protected DataTable callFunctionImpl(FunctionDefinition def, CallerController caller, RequestController request, DataTable parameters) throws ContextException
  {
    if (def.getName().equals(F_LOCAL_REINITIALIZE))
    {
      reinitialize();
      return new SimpleDataTable(def.getOutputFormat(), true);
    }
    
    return callRemoteFunction(def.getName(), def.getOutputFormat(), parameters, request != null ? request.getQueue() : null, request == null || request.isReplyRequired());
  }
  
  protected DataTable callRemoteFunction(String name, TableFormat outputFormat, DataTable parameters, String queueName, boolean isReplyRequired) throws ContextException
  {
    try
    {
      return controller.callRemoteFunction(getPeerPath(), name, outputFormat, parameters, queueName, isReplyRequired);
    }
    catch (Exception ex)
    {
      Log.CONTEXT_FUNCTIONS.debug("Error calling function '" + name + "' of context '" + getPathDescription() + "'", ex);
      throw new ContextException(ex.getMessage(), ex);
    }
  }
  
  @Override
  public boolean addEventListener(String name, ContextEventListener contextEventListener, boolean weak)
  {
    return addEventListener(name, contextEventListener, weak, true);
  }
  
  public boolean addEventListener(String name, ContextEventListener contextEventListener, boolean weak, boolean sendRemoteCommand)
  {
    try
    {
      initEvents();
      
      EventData ed = getEventData(name);
      
      if (ed == null)
      {
        throw new ContextException(Cres.get().getString("conEvtNotAvail") + name);
      }
      
      if (sendRemoteCommand)
      {
        addRemoteListener(ed.getDefinition().getName(), contextEventListener);
      }
      
      return super.addEventListener(name, contextEventListener, weak);
    }
    catch (Exception ex)
    {
      String msg = MessageFormat.format(Cres.get().getString("conErrAddingListener"), name, getPathDescription());
      throw new IllegalStateException(msg + ": " + ex.getMessage(), ex);
    }
  }
  
  @Override
  public boolean removeEventListener(String name, ContextEventListener contextEventListener)
  {
    return removeEventListener(name, contextEventListener, true);
  }
  
  public boolean removeEventListener(String name, ContextEventListener listener, boolean sendRemoteCommand)
  {
    try
    {
      if (!isInitializedEvents())
      {
        return false;
      }
      
      Log.CONTEXT_EVENTS.debug("Removing listener for event '" + name + "' from context '" + getPathDescription() + "'");
      
      boolean res = super.removeEventListener(name, listener);
      
      EventData ed = getEventData(name);
      
      if (sendRemoteCommand && getController().isConnected() && ed != null && !ed.hasListeners())
      {
        ProtocolVersion protocolVersion = getController().getProtocolVersion();
        if (!notManageRemoteListeners && protocolVersion != null && protocolVersion.ordinal() >= ProtocolVersion.V3.ordinal())
        {
          Integer hashCode = listener.getListenerCode();
          String filter = listener.getFilter() != null ? listener.getFilter().getText() : null;
          String fingerprint = listener.getFingerprint();
          OutgoingAggreGateCommand cmd = controller.getCommandBuilder().removeEventListenerOperation(getPeerPath(), name, hashCode, filter, fingerprint);
          cmd.setTimeout(LISTENER_OPERATIONS_TIMEOUT);
          controller.sendCommandAndCheckReplyCode(cmd);
        }
      }
      
      return res;
    }
    catch (DisconnectionException ex)
    {
      Log.CONTEXT_EVENTS.debug("Disconnection detected when removing listener for event '" + name + "' from context '" + getPathDescription() + "'");
      return false;
    }
    catch (Exception ex)
    {
      String msg = MessageFormat.format(Cres.get().getString("conErrRemovingListener"), name, getPathDescription());
      throw new IllegalStateException(msg + ": " + ex.getMessage(), ex);
    }
  }
  
  private void addRemoteListener(String ename, ContextEventListener contextEventListener)
      throws RemoteDeviceErrorException, InterruptedException, SyntaxErrorException, DisconnectionException, IOException, ContextException
  {
    Integer hashCode = contextEventListener.getListenerCode();
    
    if (hashCode == null && AUTO_LISTENED_EVENTS.contains(ename))
    {
      return;
    }
    
    ProtocolVersion protocolVersion = getController().getProtocolVersion();
    if (!notManageRemoteListeners && protocolVersion != null && protocolVersion.ordinal() >= ProtocolVersion.V3.ordinal())
    {
      String filterText = contextEventListener.getFilter() != null ? contextEventListener.getFilter().getText() : null;
      final String fingerprint = contextEventListener.getFingerprint();
      OutgoingAggreGateCommand cmd = controller.getCommandBuilder().addEventListenerOperation(getPeerPath(), ename, hashCode, filterText, fingerprint);
      cmd.setTimeout(LISTENER_OPERATIONS_TIMEOUT);
      controller.sendCommandAndCheckReplyCode(cmd);
    }
  }
  
  @Override
  public List<C> getChildren(CallerController caller)
  {
    try
    {
      initChildren();
    }
    catch (ContextException ex)
    {
      boolean disconnected = ExceptionUtils.indexOfType(ex, DisconnectionException.class) != -1;
      Log.CONTEXT_CHILDREN.log(disconnected ? Level.DEBUG : Level.WARN, "Error initializing children of remote context", ex);
    }
    return super.getChildren(caller);
  }
  
  @Override
  public List<C> getVisibleChildren(CallerController caller)
  {
    try
    {
      initVisibleChildren();
    }
    catch (ContextException ex)
    {
      boolean disconnected = ExceptionUtils.indexOfType(ex, DisconnectionException.class) != -1;
      Log.CONTEXT_CHILDREN.log(disconnected ? Level.DEBUG : Level.WARN, "Error initializing visible children of remote context", ex);
      return new LinkedList();
    }
    
    List<C> res = new LinkedList();
    
    for (String path : visibleChildren)
    {
      C con = (C) getRoot().get(path, caller);
      if (con != null)
      {
        res.add(con);
      }
    }
    
    return res;
  }
  
  public void addVisibleChild(String localVisiblePath)
  {
    visibleChildren.add(localVisiblePath);
  }
  
  public void removeVisibleChild(String localVisiblePath)
  {
    visibleChildren.remove(localVisiblePath);
  }
  
  public boolean hasVisibleChild(String path)
  {
    return visibleChildren != null && visibleChildren.contains(path);
  }
  
  private void restoreEventListeners() throws ContextException
  {
    for (EventDefinition ed : super.getEventDefinitions((CallerController) null)) // Calling method of superclass directly to avoid fetching remote events info
    {
      EventData edata = getEventData(ed.getName());

      edata.doWithListeners(listener -> {
        try
        {
          addRemoteListener(ed.getName(), listener.getListener());
        }
        catch (Exception ex)
        {
          Log.CONTEXT_EVENTS.warn("Error restoring listener for event '" + ed.getName() + "'", ex);
        }
      });
    }
  }
  
  public void reinitialize() throws ContextException
  {
    clear();
    restoreEventListeners();
  }

  @Override
  protected Event fireEvent(EventDefinition ed, DataTable data, int level, Long id, Date creationtime, Integer listener, CallerController caller, FireEventRequestController request,
      Permissions permissions)
  {
    Event event = super.fireEvent(ed, data, level, id, creationtime, listener, caller, request, permissions);
    
    if (ed.getName().equals(AbstractContext.E_UPDATED) && isInitializedVariables())
    {
      String variable = event.getData().rec().getString(AbstractContext.EF_UPDATED_VARIABLE);
      
      DataTable value = event.getData().rec().getDataTable(AbstractContext.EF_UPDATED_VALUE);
      
      VariableDefinition vd = getVariableDefinition(variable);
      
      if (vd != null && vd.getRemoteCacheTime() != null)
      {
        cacheVariableValue(variable, value);
      }
    }
    
    return event;
  }
  
  private void cacheVariableValue(String variable, DataTable value)
  {
    variableCacheLock.writeLock().lock();
    try
    {
      variableCache.put(variable, new SoftReference(new CachedVariableValue(new Date(), value)));
    }
    finally
    {
      variableCacheLock.writeLock().unlock();
    }
  }
  
  protected String getPathDescription()
  {
    return getPath();
  }
  
  @Override
  public boolean isProxy()
  {
    return true;
  }
  
  @Override
  public boolean isDistributed()
  {
    return getPeerRoot() != null;
  }
  
  @Override
  public String getRemotePath()
  {
    try
    {
      initInfo();
    }
    catch (ContextException ex)
    {
      throw new ContextRuntimeException("Error getting type of remote context: " + ex.getMessage(), ex);
    }
    
    return remotePath;
  }
  
  @Override
  public String getLocalPrimaryRoot()
  {
    return peerPrimaryRoot;
  }
  
  @Override
  public String getPeerPath()
  {
    return getPath();
  }
  
  public String getLocalPath(String remoteFullPath, boolean visible)
  {
    String remoteRoot = visible ? getPeerRoot() : getRemoteRoot();
    
    if (remoteRoot == null)
    {
      return remoteFullPath;
    }
    
    String remoteConverted;
    if (remoteRoot.equals(Contexts.CTX_ROOT))
    {
      remoteConverted = remoteFullPath;
    }
    else if (remoteFullPath.equals(remoteRoot))
    {
      remoteConverted = "";
    }
    else
    {
      return getLocalPrimaryPath(remoteFullPath);
    }
    
    String localRoot = getLocalRoot(!visible);
    
    String converted = remoteConverted.length() > 0 ? localRoot.length() > 0 ?
        ContextUtils.createName(localRoot, remoteConverted) : remoteConverted : localRoot;
    return converted;
  }
  
  private String getLocalPrimaryPath(String remoteFullPath)
  {
    String primaryMount = getLocalPrimaryRoot();
    
    if (primaryMount == null)
    {
      return null;
    }
    
    if (Util.equals(Contexts.CTX_ROOT, remoteFullPath))
    {
      return primaryMount;
    }
    else
    {
      return ContextUtils.createName(primaryMount, remoteFullPath);
    }
  }

  protected String getLocalVisiblePath(String peerVisiblePath)
  {
    return peerVisiblePath;
  }
  
  @Override
  public String toString()
  {
    if (isInitializedInfo())
    {
      return super.toString();
    }
    else
    {
      return getPath();
    }
  }

  @Override
  public boolean isInitializedStatus()
  {
    return initializedStatus;
  }
  
  @Override
  public boolean isInitializedInfo()
  {
    return initializedInfo;
  }
  
  @Override
  public boolean isInitializedChildren()
  {
    return initializedChildren;
  }
  
  @Override
  public boolean isInitializedVariables()
  {
    return initializedVariables;
  }
  
  @Override
  public boolean isInitializedFunctions()
  {
    return initializedFunctions;
  }
  
  @Override
  public boolean isInitializedEvents()
  {
    return initializedEvents;
  }

  public boolean isNotManageRemoteListeners()
  {
    return notManageRemoteListeners;
  }
  
  @Override
  public boolean isContainer()
  {
    return container;
  }

  public void setContainer(boolean container)
  {
    this.container = container;
  }

  public void setNotManageRemoteListeners(boolean notManageRemoteListeners)
  {
    this.notManageRemoteListeners = notManageRemoteListeners;
  }
  
  private final ContextEventListener visibleChildAddedListener = new DefaultContextEventListener(new UncheckedCallerController())
  {
    @Override
    public void handle(Event event) throws EventHandlingException
    {
      String path = event.getData().rec().getString(ServerContextConstants.EF_VISIBLE_CHILD_ADDED_PATH);
      if (visibleChildren != null)
      {
        addVisibleChild(path);
      }
    }
  };
  
  private final ContextEventListener visibleChildRemovedListener = new DefaultContextEventListener(new UncheckedCallerController())
  {
    @Override
    public void handle(Event event) throws EventHandlingException
    {
      String path = event.getData().rec().getString(ServerContextConstants.EF_VISIBLE_CHILD_REMOVED_PATH);
      if (visibleChildren != null)
      {
        removeVisibleChild(path);
      }
    }
  };
  
  private final ContextEventListener contextStatusChangedListener = new DefaultContextEventListener(new UncheckedCallerController())
  {
    @Override
    public void handle(Event event) throws EventHandlingException
    {
      final DataRecord statusRec = event.getData().rec();
      
      setStatus(statusRec.getInt(ServerContextConstants.VF_CONTEXT_STATUS_STATUS), statusRec.getString(ServerContextConstants.VF_CONTEXT_STATUS_COMMENT));
    }
  };
}
