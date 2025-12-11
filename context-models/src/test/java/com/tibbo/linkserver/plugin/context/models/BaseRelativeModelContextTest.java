package com.tibbo.linkserver.plugin.context.models;

import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.DataTableConversion;
import com.tibbo.aggregate.common.expression.EvaluationException;
import com.tibbo.aggregate.common.expression.Evaluator;
import com.tibbo.aggregate.common.expression.Expression;
import com.tibbo.aggregate.common.server.ModelContextConstants;
import com.tibbo.aggregate.common.util.SyntaxErrorException;
import com.tibbo.linkserver.context.EditableChildrenContext;

public abstract class BaseRelativeModelContextTest extends BaseModelContextTest
{
  protected static final String VARIABLE1_RELATIVE = "v1Relative";
  protected static final String VARIABLE2_RELATIVE = "v2Relative";
  protected static final String VARIABLE1_ABSOLUTE = "v1Absolute";
  protected static final String VARIABLE2_ABSOLUTE = "v2Absolute";
  protected static final String MODEL_ABSOLUTE = "mAbsolute";
  protected static final String MODEL_RELATIVE = "mRelative";
  
  protected ModelContext relativeModelContext;
  
  @Override
  protected void setUp() throws Exception
  {
    super.setUp(); // Create Absolute model
    
    setUpRelativeModel();
  }
  
  protected void setUpRelativeModel() throws ContextException
  {
    relativeModelContext = createModel(MODEL_RELATIVE, MODEL_RELATIVE, "{.:#type} == 'model' && {.:#name} == '" + MODEL_ABSOLUTE + "'", prepareRelativeModelVariables());
  }
  
  protected ModelContext createModel(String name, String description, String validityExpression, DataTable variables) throws ContextException
  {
    Model modelRelative = new Model(name, description);
    modelRelative.setType(Model.TYPE_RELATIVE);
    modelRelative.setValidityExpression(validityExpression);
    modelRelative.setMaximumBindingQueueLength(10_000);
    
    updateValidityListeners(modelRelative);
    
    modelsContext.callFunction(EditableChildrenContext.F_CREATE, getCallerController(), DataTableConversion.beanToTable(modelRelative, Model.FORMAT, true));
    
    ModelContext modelContext = (ModelContext) modelsContext.getChild(modelRelative.getName(), getCallerController());
    
    modelContext.install(getContextManager().getRoot());
    
    modelContext.setVariable(ModelContextConstants.V_MODEL_VARIABLES, getCallerController(), variables);
    
    return modelContext;
  }
  
  protected void updateValidityListeners(Model modelRelative) {
  
  }
  
  protected abstract DataTable prepareRelativeModelVariables();
  
  @Override
  protected Model prepareModel()
  {
    Model modelAbs = new Model(MODEL_ABSOLUTE, MODEL_ABSOLUTE);
    modelAbs.setType(Model.TYPE_ABSOLUTE);
    
    return modelAbs;
  }
  
  protected DataTable getVariableFromAbsoluteModel(String name)
  {
    return getVariableFromAbsoluteModel(modelContext, name);
  }
  
  protected DataTable getVariableFromAbsoluteModel(ModelContext modelContext, String name)
  {
    try
    {
      return modelContext.getVariable(name, getCallerController());
    }
    catch (ContextException ex)
    {
      return null;
    }
  }
  
  protected void changeAbsoluteModelFromExpression() throws SyntaxErrorException, EvaluationException
  {
    String exp = "setVariable(\"users." + userName + ".models." + MODEL_ABSOLUTE + "\",\"modelVariables\", getVariable(\"users." + userName + ".models." + MODEL_ABSOLUTE + "\", \""
        + ModelContextConstants.V_MODEL_VARIABLES + "\"))";
    Evaluator ev = new Evaluator(getContextManager(), getCallerController());
    ev.evaluate(new Expression(exp));
  }
}