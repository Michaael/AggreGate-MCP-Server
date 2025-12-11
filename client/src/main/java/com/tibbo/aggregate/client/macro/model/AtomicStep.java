package com.tibbo.aggregate.client.macro.model;

import java.util.*;

/**
 * 
 * <p>
 * A generic step that may consist of one or more basic steps that should be performed atomically. Atomically may have different meaning in case of different operations. For example in case of editing
 * a table we need first to obtain the table then perform specified operations and finally save changes.
 * </p>
 */
public interface AtomicStep<T extends Step> extends Step
{
  /**
   * Adds a step to the end of the step sequence of this AtomicStep Since java parameterization is only build-time the implementor should check the class of T step and throw IllegalArgumentException
   * on mismatch.
   */
  void addStep(T step) throws IllegalArgumentException;
  
  /**
   * Removes a step from the step sequence
   */
  void removeStep(T step);
  
  /**
   * Lists all contained steps
   */
  List<T> listSteps();
  
  /**
   * Returns count of children
   */
  int getStepCount();
}
