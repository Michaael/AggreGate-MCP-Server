package com.tibbo.linkserver.plugin.context.models;

import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.server.*;
import com.tibbo.linkserver.context.*;
import com.tibbo.linkserver.tests.*;
import com.tibbo.linkserver.user.*;

public abstract class BaseModelContextTest extends ServerTestCase
{
  protected UserContext userContext;
  protected ModelsContext modelsContext;
  protected ModelContext modelContext;
  protected static final String userName = "testUser";
  
  public BaseModelContextTest()
  {
    super(SetupType.MINIMAL);
  }
  
  @Override
  protected void setUp() throws Exception
  {
    super.setUp();
    
    getOrCreateContextManager();
   
    userContext = createUser();
    
    modelContext = createModelContext(prepareModel());
    
    modelContext.install(getContextManager().getRoot());
  }
  
  @Override
  protected void tearDown() throws Exception
  {
    tearDownImplementation();
  
    super.tearDown();
  }
  
  protected void tearDownImplementation() throws ContextException {
    modelContext.getParent().callFunction(EditableChildrenContext.F_DELETE, getCallerController(), modelContext.getName());
    
    UserManager.remove(userContext.getParent(), userName, getCallerController());
  }
  
  protected UserContext createUser() throws AggreGateException
  {
    UsersContext<ServerContext, ServerContextManager> usersContext = new UsersContext();
    
    addToRoot(usersContext);
    
    return UserManager.create(getContextManager(), userName, userName);
  }
  
  protected ModelContext createModelContext(Model model) throws AggreGateException
  {
    if(modelsContext == null)
    {
      modelsContext = new ModelsContext(userContext);
    
      userContext.addChild(modelsContext);
    }
    
    modelsContext.callFunction(EditableChildrenContext.F_CREATE, getCallerController(), DataTableConversion.beanToTable(model, Model.FORMAT, true));
    
    return (ModelContext) modelsContext.getChild(model.getName(), getCallerController());
  }
  
  protected abstract Model prepareModel();
}
