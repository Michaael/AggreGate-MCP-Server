package com.tibbo.aggregate.client.sandbox;

import java.awt.*;

import javax.swing.*;

import edu.uci.ics.jung.algorithms.layout.*;
import edu.uci.ics.jung.graph.*;
import edu.uci.ics.jung.graph.util.*;
import edu.uci.ics.jung.visualization.*;
import edu.uci.ics.jung.visualization.control.*;
import edu.uci.ics.jung.visualization.decorators.*;

public class GraphTest
{
  
  public static void main(String[] args)
  {
    // Note that we can use the same nodes and edges in two different graphs.
    Graph<Integer, String> g2 = new SparseMultigraph<Integer, String>();
    g2.addVertex(1);
    g2.addVertex(2);
    g2.addVertex(3);
    g2.addVertex(4);
    g2.addVertex(5);
    g2.addEdge("Edge-A", 1, 3);
    g2.addEdge("Edge-B", 2, 3, EdgeType.DIRECTED);
    g2.addEdge("Edge-C", 3, 2, EdgeType.DIRECTED);
    g2.addEdge("Edge-P", 2, 3); // A parallel edge
    System.out.println("The graph g2 = " + g2.toString());
    
    final Dimension dimension = new Dimension(200, 200);
    
    Layout layout = new CircleLayout(g2);
    layout.setSize(dimension);
    
    DefaultVisualizationModel model = new DefaultVisualizationModel(layout, new Dimension(100, 100));
    
    VisualizationViewer vv = new VisualizationViewer(model);
    // vv.scaleToLayout(new LayoutScalingControl());
    vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
    vv.setPreferredSize(new Dimension(800, 800));
    
    DefaultModalGraphMouse gm = new DefaultModalGraphMouse(1 / 1.1f, 1.1f);
    gm.setMode(ModalGraphMouse.Mode.TRANSFORMING);
    vv.setGraphMouse(gm);
    
    JFrame jf = new JFrame();
    jf.getContentPane().add(vv);
    jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    jf.pack();
    jf.setVisible(true);
  }
}
