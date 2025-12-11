package com.tibbo.linkserver.plugin.context.models;

import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.server.ModelContextConstants;
import com.tibbo.linkserver.templates.TemplatableServerContext;

public class TestRelativeModelContext extends BaseRelativeModelContextTest
{
  @Override
  protected DataTable prepareRelativeModelVariables()
  {
    return prepareModelVariables(VARIABLE1_RELATIVE);
  }
  
  protected DataTable prepareModelVariables(String name)
  {
    DataRecord modelVariable = new DataRecord(ModelContext.VFT_MODEL_VARIABLES);
    modelVariable.setValue(ModelContext.FIELD_VD_NAME, name);
    return modelVariable.wrap();
  }
  
  private void addVariableToAbsoluteModel(String name) throws ContextException
  {
    modelContext.setVariable(ModelContextConstants.V_MODEL_VARIABLES, getCallerController(), prepareModelVariables(name));
  }
  
  private void renameAbsoluteModelVariableTo(String name) throws ContextException
  {
    modelContext.setVariable(ModelContextConstants.V_MODEL_VARIABLES, getCallerController(), prepareModelVariables(name));
  }
  
  private void renameRelativeModelVariableTo(String name) throws ContextException
  {
    relativeModelContext.setVariable(ModelContextConstants.V_MODEL_VARIABLES, getCallerController(), prepareModelVariables(name));
  }
  
  public void testReadVariableFromAbsoluteModelAfterChanges() throws Exception
  {
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE));
    
    changeAbsoluteModelFromExpression();
    
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE));
    
    addVariableToAbsoluteModel(VARIABLE1_ABSOLUTE);
    
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE));
    
    renameAbsoluteModelVariableTo(VARIABLE2_ABSOLUTE);
    
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE));
    
    renameRelativeModelVariableTo(VARIABLE2_RELATIVE);
    
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE));
  }
  
  public void testTemplatesVariableEnabled() throws Exception
  {
    assertNotNull(getVariableFromAbsoluteModel(TemplatableServerContext.V_TEMPLATES));
  }
}