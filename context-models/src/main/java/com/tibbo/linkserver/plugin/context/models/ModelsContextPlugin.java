package com.tibbo.linkserver.plugin.context.models;

import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.plugin.*;
import com.tibbo.aggregate.common.server.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.linkserver.*;
import com.tibbo.linkserver.context.*;
import com.tibbo.linkserver.security.*;
import com.tibbo.linkserver.user.*;

public class ModelsContextPlugin extends AbstractContextPlugin implements ContextPlugin
{
  public static final String ID = "com.tibbo.linkserver.plugin.context.models";
  
  public ModelsContextPlugin()
  {
    super(Lres.get().getString("models"));
  }
  
  @Override
  public void initialize() throws PluginException
  {
    super.initialize();
    
    Server.getSecurityDirector().addDefaultPermission(new DefaultPermission(ContextUtils.modelsContextPath(ContextUtils.USERNAME_PATTERN), Lres.get().getString("models"), true, true));
  }
  
  @Override
  public void install(ServerContext context) throws ContextException, PluginException
  {
    if (context instanceof UserContext)
    {
      UserContext uc = (UserContext) context;
      
      uc.registerContainerBuilder(new DefaultResourceContainerBuilder(ContextUtils.modelsContextPath(ContextUtils.USERNAME_PATTERN))
      {
        @Override
        public void buildImpl(UserContext context) throws ContextException
        {
          context.addChild(new ModelsContext(context));
          context.addVisibleChild(ContextUtils.modelsContextPath(context.getName()));
        }
        
        @Override
        public void dismantleImpl(UserContext context) throws ContextException
        {
          context.removeVisibleChild(Contexts.CTX_MODELS, false);
          context.removeChild(Contexts.CTX_MODELS);
        }
      });
    }
    
    super.install(context);
  }
  
  @Override
  public void install(ContextManager cm) throws ContextException, PluginException
  {
    ServerContextUtils.createAggregationContexts(cm.getRoot(), Contexts.CTX_MODELS, Lres.get().getString("models"), Icons.ST_MODELS, Docs.LS_MODELS,
        Lres.get().getString("modelGroups"), true);
    
    RootContext root = (RootContext) cm.getRoot();
    
    root.addVisibleChild(Contexts.CTX_MODELS);
    
    super.install(cm);
  }
}
