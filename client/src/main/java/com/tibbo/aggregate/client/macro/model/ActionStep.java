package com.tibbo.aggregate.client.macro.model;

/**
 * <p>
 * UI steps supported by the Action framework. Currently they are:
 * <ul>
 * <li>Edit data</li>
 * <li>Edit properties</li>
 * <li>Confirm operation</li>
 * <li>etc...</li>
 * </ul>
 * See Actions framework for more careful information.
 * </p>
 */
public interface ActionStep extends Step
{
  public String getActionRequestId();
  
  public void setActionRequestId(String actionRequestId);
}
