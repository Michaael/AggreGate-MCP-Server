package com.tibbo.linkserver.plugin.context.models.rules;

import com.tibbo.aggregate.common.Cres;
import com.tibbo.aggregate.common.datatable.AggreGateBean;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.DataTableBindingProvider;
import com.tibbo.aggregate.common.datatable.FieldFormat;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.common.datatable.field.StringFieldFormat;
import com.tibbo.aggregate.common.expression.DefaultReferenceResolver;
import com.tibbo.linkserver.plugin.context.models.Lres;
import com.tibbo.linkserver.plugin.context.models.Model;

public class Rule extends AggreGateBean
{
  public static final String TARGET_RESULT = "";
  
  public static final String FIELD_TARGET = "target";
  public static final String FIELD_EXPRESSION = "expression";
  public static final String FIELD_CONDITION = "condition";
  public static final String FIELD_COMMENT = "comment";
  
  public static final TableFormat FORMAT = new TableFormat();
  
  static
  {
    FORMAT.setReorderable(true);
    
    FieldFormat targF = FieldFormat.create(FIELD_TARGET, FieldFormat.STRING_FIELD, Cres.get().getString("target"));
    targF.setExtendableSelectionValues(true);
    targF.addSelectionValue("", Lres.get().getString("ruleSetResult"));
    targF.setHelp(Lres.get().getString("ruleSetRulesTargetHelp"));
    FORMAT.addField(targF);
    
    FieldFormat exprF = FieldFormat.create(FIELD_EXPRESSION, FieldFormat.STRING_FIELD, Cres.get().getString("expression"));
    exprF.setEditor(StringFieldFormat.EDITOR_EXPRESSION);
    exprF.setHelp(Lres.get().getString("ruleSetRulesExpressionHelp"));
    FORMAT.addField(exprF);
    
    FieldFormat condition = FieldFormat.create(FIELD_CONDITION, FieldFormat.STRING_FIELD, Cres.get().getString("condition"));
    condition.setEditor(StringFieldFormat.EDITOR_EXPRESSION);
    condition.setHelp(Lres.get().getString("ruleSetRulesConditionHelp"));
    FORMAT.addField(condition);
    
    FieldFormat comment = FieldFormat.create(FIELD_COMMENT, FieldFormat.STRING_FIELD, Cres.get().getString("comment"));
    comment.setEditor(StringFieldFormat.EDITOR_TEXT_AREA);
    comment.setHelp(Lres.get().getString("ruleSetRulesCommentHelp"));
    FORMAT.addField(comment);
    
    String ref = FIELD_EXPRESSION + "#" + DataTableBindingProvider.PROPERTY_OPTIONS;
    FORMAT.addBinding(ref, Model.DEFAULT_CONTEXT_EXPRESSION_OPTIONS);
    
    ref = FIELD_CONDITION + "#" + DataTableBindingProvider.PROPERTY_OPTIONS;
    FORMAT.addBinding(ref, Model.DEFAULT_CONTEXT_EXPRESSION_OPTIONS);
    
    FORMAT.setNamingExpression("'" + Cres.get().getString("rules") + ": ' + {#" + DefaultReferenceResolver.RECORDS + "}");
  }
  
  private String target;
  private String expression;
  private String condition;
  private String comment;
  
  public Rule()
  {
    super(FORMAT);
  }
  
  public Rule(String target, String expression, String condition)
  {
    this();
    this.target = target;
    this.expression = expression;
    this.condition = condition;
  }
  
  public Rule(String target, String expression, String condition, String comment)
  {
    this(target, expression, condition);
    this.comment = comment;
  }
  
  /**
   * This constructor will create a rule targeted to rule set result.
   */
  public Rule(String expression, String condition)
  {
    this();
    this.expression = expression;
    this.condition = condition;
  }
  
  /**
   * This constructor will create a rule targeted to rule set result.
   */
  public Rule(String expression)
  {
    this();
    this.expression = expression;
  }
  
  public Rule(DataRecord data)
  {
    super(FORMAT, data);
  }
  
  public String getTarget()
  {
    return target;
  }
  
  public void setTarget(String target)
  {
    this.target = target;
  }
  
  public String getExpression()
  {
    return expression;
  }
  
  public void setExpression(String expression)
  {
    this.expression = expression;
  }
  
  public String getCondition()
  {
    return condition;
  }
  
  public void setCondition(String condition)
  {
    this.condition = condition;
  }
  
  public String getComment()
  {
    return comment;
  }
  
  public void setComment(String comment)
  {
    this.comment = comment;
  }

  @Override
  public String toString()
  {
    final String format = "%s [%s: '%s', %s: '%s', %s: '%s', %s: '%s']";

    return String.format(format, Lres.get().getString("rule"),
            Cres.get().getString("target"), target,
            Cres.get().getString("expression"), expression,
            Cres.get().getString("condition"), condition,
            Cres.get().getString("comment"), comment);
  }
}
