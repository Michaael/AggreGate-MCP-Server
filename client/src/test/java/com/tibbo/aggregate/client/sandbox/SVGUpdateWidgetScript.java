package com.tibbo.aggregate.client.sandbox;

import org.apache.batik.bridge.*;
import org.apache.batik.swing.*;
import org.w3c.dom.*;
import org.w3c.dom.svg.*;

import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.widget.*;
import com.tibbo.aggregate.common.widget.runtime.renderers.*;

public class SVGUpdateWidgetScript implements WidgetScript
{
  public Object execute(WidgetScriptExecutionEnvironment environment, Object... parameters)
  {
    final float level = Float.parseFloat(parameters[0].toString());
    
    final VectorDrawingSwingRenderer r = (VectorDrawingSwingRenderer) environment.getEngine().getViewer().getComponentRenderer("reactor");
    final JSVGCanvas c = r.getRender();
    final UpdateManager um = c.getUpdateManager();
    
    um.getUpdateRunnableQueue().invokeLater(new Runnable()
    {
      public void run()
      {
        Log.WIDGETS.debug("Reactor level changed to " + level);
        
        SVGDocument d = c.getSVGDocument();
        Element e = d.getElementById("level_mask_src");
        float svgLevel = 630 - (630 - 67) * level / 100;
        e.setAttributeNS(null, "y", String.valueOf(svgLevel));
        c.setDocument(d);
      }
    });
    
    return null;
  }
}
