package com.tibbo.linkserver.plugin.context.models.rules;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.tibbo.aggregate.common.Cres;
import com.tibbo.aggregate.common.datatable.AggreGateBean;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.FieldFormat;
import com.tibbo.aggregate.common.datatable.SimpleDataTable;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.common.datatable.validator.TableKeyFieldsValidator;
import com.tibbo.aggregate.common.datatable.validator.ValidatorHelper;
import com.tibbo.aggregate.common.structure.Pinpoint;
import com.tibbo.aggregate.common.structure.PinpointAware;
import com.tibbo.linkserver.plugin.context.models.Lres;

public class RuleSet extends AggreGateBean implements PinpointAware
{
  public static final String FIELD_NAME = "name";
  public static final String FIELD_DESCRIPTION = "description";
  public static final String FIELD_TYPE = "type";
  public static final String FIELD_RULES = "rules";
  
  public static final int TYPE_SEQUENTIAL = 0;
  public static final int TYPE_DEPENDENT = 1;
  
  public static final TableFormat FORMAT = new TableFormat();
  
  static
  {
    FORMAT.setReorderable(true);
    
    FieldFormat ff = FieldFormat.create(FIELD_NAME, FieldFormat.STRING_FIELD, Cres.get().getString("name"));
    ff.addValidator(ValidatorHelper.NAME_LENGTH_VALIDATOR);
    ff.addValidator(ValidatorHelper.NAME_SYNTAX_VALIDATOR);
    ff.setHelp(Lres.get().getString("ruleSetNameHelp"));
    ff.setKeyField(true);
    FORMAT.addField(ff);
    
    ff = FieldFormat.create("<" + FIELD_DESCRIPTION + "><S><D=" + Cres.get().getString("description") + ">");
    ff.addValidator(ValidatorHelper.DESCRIPTION_LENGTH_VALIDATOR);
    ff.addValidator(ValidatorHelper.DESCRIPTION_SYNTAX_VALIDATOR);
    ff.setHelp(Lres.get().getString("ruleSetDescriptionHelp"));
    ff.setNullable(true);
    FORMAT.addField(ff);
    
    ff = FieldFormat.create(FIELD_TYPE, FieldFormat.INTEGER_FIELD, Cres.get().getString("type"));
    ff.setDefault(TYPE_SEQUENTIAL);
    ff.addSelectionValue(TYPE_SEQUENTIAL, Lres.get().getString("ruleSetTypeSequential"));
    ff.addSelectionValue(TYPE_DEPENDENT, Lres.get().getString("ruleSetTypeDependent"));
    ff.setHelp(Lres.get().getString("ruleSetTypeHelp"));
    FORMAT.addField(ff);
    
    ff = FieldFormat.create(FIELD_RULES, FieldFormat.DATATABLE_FIELD, Cres.get().getString("rules"));
    ff.setDefault(new SimpleDataTable(Rule.FORMAT));
    ff.setHelp(Lres.get().getString("ruleSetRulesHelp"));
    FORMAT.addField(ff);
    
    FORMAT.addTableValidator(new TableKeyFieldsValidator());
  }
  
  private String name;
  private String description;
  private int type;
  private List<Rule> rules;

  @Nullable
  private transient Pinpoint pinpoint;
  
  public RuleSet()
  {
    super(FORMAT);
  }
  
  public RuleSet(String name, String description, int type)
  {
    this();
    this.name = name;
    this.description = description;
    this.type = type;
  }
  
  public RuleSet(List<Rule> rules)
  {
    this();
    this.rules = rules;
  }
  
  public RuleSet(DataRecord data)
  {
    super(FORMAT, data);
  }
  
  public String getName()
  {
    return name;
  }
  
  public void setName(String name)
  {
    this.name = name;
  }
  
  public String getDescription()
  {
    return description;
  }
  
  public void setDescription(String description)
  {
    this.description = description;
  }
  
  public int getType()
  {
    return type;
  }
  
  public void setType(int type)
  {
    this.type = type;
  }
  
  public List<Rule> getRules()
  {
    return rules;
  }
  
  public void setRules(List<Rule> rules)
  {
    this.rules = rules;
  }
  
  public void addRule(Rule rule)
  {
    rules.add(rule);
  }
  
  @Override
  public String toString()
  {
    final String format = "%s [%s: '%s', %s: '%s', %s: %s, %s: '%s']";
    
    return String.format(format, Lres.get().getString("ruleSet"),
        Cres.get().getString("name"), name,
        Cres.get().getString("description"), description,
        Cres.get().getString("type"), type,
        Cres.get().getString("rules"), rules);
  }

  @Override
  public void assignPinpoint(Pinpoint pinpoint)
  {
    checkState(this.pinpoint == null, "This '%s' already contains pinpoint '%s' but " +
        "somebody attempted to assign another one: '%s'", this, this.pinpoint, pinpoint);
    this.pinpoint = pinpoint;
  }

  @Override
  public Optional<Pinpoint> obtainPinpoint()
  {
    return Optional.ofNullable(pinpoint);
  }
}
