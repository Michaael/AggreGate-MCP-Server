package com.tibbo.aggregate.client.macro.ui;

import com.tibbo.aggregate.client.macro.model.*;

public class StepContextHolder
{
  public Step step;
  public ActionCommandContext ctx;

  public StepContextHolder()
  {
  }

  public StepContextHolder(Step step, ActionCommandContext ctx)
  {
    this.step = step;
    this.ctx = ctx;
  }

  public int hashCode()
  {
    return (step != null ? step.hashCode() : 0) << 10 + (ctx != null ? ctx.hashCode() : 0);
  }

  public boolean equals(Object o)
  {
    if (!(o instanceof StepContextHolder))
    {
      return false;
    }

    StepContextHolder s = (StepContextHolder)o;

    return step == s.step && (ctx == null ? s.ctx == null : ctx.equals(s.ctx));
  }
}
