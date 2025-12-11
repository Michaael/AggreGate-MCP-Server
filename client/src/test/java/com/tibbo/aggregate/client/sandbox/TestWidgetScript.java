package com.tibbo.aggregate.client.sandbox;

import org.apache.batik.swing.*;
import org.w3c.dom.*;
import org.w3c.dom.svg.*;

import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.widget.*;
import com.tibbo.aggregate.common.widget.runtime.renderers.*;

public class TestWidgetScript implements WidgetScript
{
  public Object execute(WidgetScriptExecutionEnvironment environment, Object... parameters)
  {
    Log.CORE.warn("Starting script LEVEL");
    VectorDrawingSwingRenderer r = (VectorDrawingSwingRenderer) environment.getEngine().getViewer().getComponentRenderer("reactor");
    JSVGCanvas c = r.getRender();
    if (c == null)
    {
      return null;
    }
    SVGDocument d = c.getSVGDocument();
    Element e = d.getElementById("level_mask_src");
    e.setAttribute("y", parameters.toString());
    return null;
  }
}
