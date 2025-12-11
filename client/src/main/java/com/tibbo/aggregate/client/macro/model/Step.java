package com.tibbo.aggregate.client.macro.model;

import java.util.*;

public interface Step
{
  public static final String CONCURRENCY_OPTIONAL = "optional";
  public static final String CONCURRENCY_PARALLEL = "parallel";
  public static final String CONCURRENCY_PARALLEL_OPTIONAL = "parallel-optional";
  
  /**
   * Adds a description to the step. Step may have several descritpions implemented by different descedents of StepDescription interface. May be useful for plain-text description, html description,
   * etc.
   */
  void addDescription(StepDescription desc);
  
  /**
   * Removes the description from the description list. If no desription exists do nothing.
   */
  void removeDescription(StepDescription desc);
  
  /**
   * Remove description by class
   */
  public void removeDescription(Class descriptionClass, boolean finishing);
  
  /**
   * Lists descriptions for this Step.
   */
  Set<StepDescription> getDescriptions();
  
  void setDescriptions(Set<StepDescription> descs);
  
  /**
   * Get description of appropriate type this Step.
   */
  StepDescription getDescription(Class descriptionClass, boolean finishing);
  
  /**
   * Returns plain-text short step description.
   */
  String getTitle();
  
  /**
   * Set short step description.
   */
  void setTitle(String title);
  
  /**
   * Returns an expression which result should be boolean and shows if the step is active
   */
  String getEnabled();
  
  /**
   * Sets the step enableing expression
   */
  void setEnabled(String expr);
  
  /**
   * Returns the step concurrency, i.e. how it is executed with respect to other steps.
   */
  String getConcurrency();
  
  /**
   * Sets the step concurrency, i.e. how it is executed with respect to other steps: CONCURRENCY_OPTIONAL - one of several steps should be invoked CONCURRENCY_PARALLEL - steps should be executed in
   * parallel
   */
  void setConcurrency(String c);
  
  Map<String, String> getExpressions();
  
  void setExpressions(Map<String, String> expressions);
  
  boolean stepEquals(Object o);
  
  boolean isDescription();
}
