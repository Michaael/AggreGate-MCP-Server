package com.tibbo.linkserver.plugin.context.models;

import java.util.*;

import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.server.*;

public class TestRelativeModelContextGroup extends BaseRelativeModelContextTest
{
  private static final String VARIABLE0_RELATIVE = "v0Relative";
  private static final String VARIABLE0_RELATIVE_GROUPED = "v0RelativeG";
  private static final String VARIABLE1_RELATIVE_GROUPED = "v1RelativeG";
  private static final String VARIABLE2_RELATIVE_GROUPED = "v2RelativeG";
  private static final String VARIABLE0_ABSOLUTE = "v0Absolute";
  private static final String VARIABLE0_ABSOLUTE_GROUPED = "v0AbsoluteG";
  private static final String VARIABLE1_ABSOLUTE_GROUPED = "v1AbsoluteG";
  private static final String VARIABLE2_ABSOLUTE_GROUPED = "v2AbsoluteG";
  private static final String GROUP_NAME = "Group1";
  
  @Override
  protected ModelContext createModelContext(Model model) throws AggreGateException
  {
    ModelContext modelContext = super.createModelContext(model);
    
    DataTable value = new SimpleDataTable(prepareModelVariable(VARIABLE0_ABSOLUTE, null));
    value.addRecord(prepareModelVariable(VARIABLE0_ABSOLUTE_GROUPED, GROUP_NAME));
    
    modelContext.setVariable(ModelContextConstants.V_MODEL_VARIABLES, getCallerController(), value);
    return modelContext;
  }
  
  @Override
  protected DataTable prepareRelativeModelVariables()
  {
    DataTable value = new SimpleDataTable(prepareModelVariable(VARIABLE0_RELATIVE, null));
    value.addRecord(prepareModelVariable(VARIABLE0_RELATIVE_GROUPED, GROUP_NAME));
    return value;
  }
  
  protected DataRecord prepareModelVariable(String name, String group)
  {
    DataRecord modelVariable = new DataRecord(ModelContext.VFT_MODEL_VARIABLES);
    modelVariable.setValue(ModelContext.FIELD_VD_NAME, name);
    modelVariable.setValue(ModelContext.FIELD_VD_GROUP, group);
    return modelVariable;
  }
  
  private void addVariableToModel(ModelContext modelContext, String name, String group) throws ContextException
  {
    DataTable value = modelContext.getVariable(ModelContextConstants.V_MODEL_VARIABLES, getCallerController()).clone();
    value.addRecord(prepareModelVariable(name, group));
    
    modelContext.setVariable(ModelContextConstants.V_MODEL_VARIABLES, getCallerController(), value);
  }
  
  private void renameModelVariableTo(ModelContext modelContext, String oldName, String newName) throws ContextException
  {
    DataTable value = modelContext.getVariable(ModelContextConstants.V_MODEL_VARIABLES, getCallerController()).clone();
    for (DataRecord rec : value)
    {
      if (Objects.equals(oldName, rec.getString(ModelContext.FIELD_VD_NAME)))
      {
        rec.setValue(ModelContext.FIELD_VD_NAME, newName);
        break;
      }
    }
    
    modelContext.setVariable(ModelContextConstants.V_MODEL_VARIABLES, getCallerController(), value);
  }
  
  private void removeVariableFromModel(ModelContext modelContext, String name) throws ContextException
  {
    DataTable value = modelContext.getVariable(ModelContextConstants.V_MODEL_VARIABLES, getCallerController()).clone();
    Iterator<DataRecord> records = value.iterator();
    while (records.hasNext())
      if (Objects.equals(records.next().getValue(ModelContext.FIELD_VD_NAME), name))
      {
        records.remove();
        break;
      }
    
    modelContext.setVariable(ModelContextConstants.V_MODEL_VARIABLES, getCallerController(), value);
  }
  
  public void testReadGroupedVariableFromAbsoluteModelAfterChanges() throws Exception
  {
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE_GROUPED));
    
    changeAbsoluteModelFromExpression();
    
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE_GROUPED));
    
    addVariableToModel(modelContext, VARIABLE1_ABSOLUTE, null);
    
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE_GROUPED));
    
    addVariableToModel(modelContext, VARIABLE1_ABSOLUTE_GROUPED, GROUP_NAME);
    
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE_GROUPED));
    
    renameModelVariableTo(modelContext, VARIABLE1_ABSOLUTE, VARIABLE2_ABSOLUTE);
    
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE_GROUPED));
    
    renameModelVariableTo(modelContext, VARIABLE1_ABSOLUTE_GROUPED, VARIABLE2_ABSOLUTE_GROUPED);
    
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE_GROUPED));
    
    addVariableToModel(relativeModelContext, VARIABLE1_RELATIVE, null);
    
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE_GROUPED));
    
    addVariableToModel(relativeModelContext, VARIABLE1_RELATIVE_GROUPED, GROUP_NAME);
    
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE_GROUPED));
    
    renameModelVariableTo(relativeModelContext, VARIABLE1_RELATIVE, VARIABLE2_RELATIVE);
    
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE_GROUPED));
    
    renameModelVariableTo(relativeModelContext, VARIABLE1_RELATIVE_GROUPED, VARIABLE2_RELATIVE_GROUPED);
    
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE_GROUPED));
    
    removeVariableFromModel(modelContext, VARIABLE2_ABSOLUTE);
    
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE_GROUPED));
    
    removeVariableFromModel(modelContext, VARIABLE2_ABSOLUTE_GROUPED);
    
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE_GROUPED));
    
    removeVariableFromModel(relativeModelContext, VARIABLE2_RELATIVE);
    
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE_GROUPED));
    
    removeVariableFromModel(relativeModelContext, VARIABLE2_RELATIVE_GROUPED);
    
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_ABSOLUTE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_ABSOLUTE_GROUPED));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE));
    assertNotNull(getVariableFromAbsoluteModel(VARIABLE0_RELATIVE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE1_RELATIVE_GROUPED));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE));
    assertNull(getVariableFromAbsoluteModel(VARIABLE2_RELATIVE_GROUPED));
  }
  
  public void testEmptyValueAddSameVariableAfterRemove() throws Exception
  {
    DataTable variableValue = getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE_GROUPED);
    assertNotNull(variableValue);
    assertEquals((Integer) 0, variableValue.getRecordCount());
    
    final String fieldName = "field1";
    final TableFormat tf = new TableFormat(FieldFormat.create(fieldName, FieldFormat.INTEGER_FIELD));
    final int testVariableFieldValue = new Random().nextInt();
    modelContext.setVariable(VARIABLE0_ABSOLUTE_GROUPED, getCallerController(), new SimpleDataTable(tf, (Object) testVariableFieldValue));
    
    variableValue = getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE_GROUPED);

    assertNotNull(variableValue);
    assertEquals((Integer) 1, variableValue.getRecordCount());
    assertEquals(testVariableFieldValue, variableValue.rec().getValue(fieldName));

    removeVariableFromModel(modelContext, VARIABLE0_ABSOLUTE_GROUPED);
    
    assertNull(getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE_GROUPED));
    
    addVariableToModel(modelContext, VARIABLE0_ABSOLUTE_GROUPED, GROUP_NAME);
    
    variableValue = getVariableFromAbsoluteModel(VARIABLE0_ABSOLUTE_GROUPED);
    assertNotNull(variableValue);
    assertEquals((Integer) 0, variableValue.getRecordCount());
  }
}