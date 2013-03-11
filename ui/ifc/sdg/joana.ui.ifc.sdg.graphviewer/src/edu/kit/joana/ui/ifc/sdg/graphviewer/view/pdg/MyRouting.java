/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view.pdg;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.jgraph.graph.EdgeView;
import org.jgraph.graph.PortView;


@SuppressWarnings({"rawtypes", "unchecked"})
public class MyRouting extends org.jgraph.graph.DefaultEdge.DefaultRouting {
	private static final long serialVersionUID = -7437400798977013596L;
	public int selfReferenceSize = 20;

	public void route(EdgeView edge, java.util.List points) {
		int n = points.size();
		Point2D from = edge.getPoint(0);
		if (edge.getSource() instanceof PortView)
			from = ((PortView) edge.getSource()).getLocation(null);
		else if (edge.getSource() != null) {
			Rectangle2D b = edge.getSource().getBounds();
			from =
				edge.getAttributes().createPoint(b.getCenterX(), b.getCenterY());
		}
		Point2D to = edge.getPoint(n - 1);
		if (edge.getTarget() instanceof PortView)
			to = ((PortView) edge.getTarget()).getLocation(null);
		else if (edge.getTarget() != null) {
			Rectangle2D b = edge.getTarget().getBounds();
			to = edge.getAttributes().createPoint(b.getCenterX(), b.getCenterY());
		}
		if (from != null && to != null) {
			Point2D[] routed;
			// Handle self references
			if (edge.getSource() == edge.getTarget()
				&& edge.getSource() != null) {
				Rectangle2D bounds =
					edge.getSource().getParentView().getBounds();
//				double height = selfReferenceSize;
//				double width = bounds.getWidth() / 3;
//				routed = new Point2D[2];
//				routed[0] =
//					edge.getAttributes().createPoint(
//						bounds.getX() + width,
//						bounds.getY() + bounds.getHeight() + height);
//				routed[1] =
//					edge.getAttributes().createPoint(
//						bounds.getX() + 2 * width,
//						bounds.getY() + bounds.getHeight() + height);
				double distance = selfReferenceSize;
				routed = new Point2D[4];
				routed[0] =
					edge.getAttributes().createPoint(
							bounds.getX()+ bounds.getWidth() - distance,
							bounds.getMaxY() + distance);
				routed[1] =
					edge.getAttributes().createPoint(
							bounds.getX()+ bounds.getWidth() + distance*2,
							bounds.getMaxY() + distance);
				routed[2] =
					edge.getAttributes().createPoint(
							bounds.getX()+ bounds.getWidth() + distance*2,
							bounds.getMinY() - distance);
				routed[3] =
					edge.getAttributes().createPoint(
							bounds.getX()+ bounds.getWidth() - distance,
							bounds.getMinY() - distance);
			} else {
//				double dx = Math.abs(from.getX() - to.getX());
//				double dy = Math.abs(from.getY() - to.getY());
//				double x2 = from.getX() + ((to.getX() - from.getX()) / 2);
//				double y2 = from.getY() + ((to.getY() - from.getY()) / 2);
//				routed = new Point2D[2];
				routed = new Point2D[0];
//				if (dx > dy) {
//					routed[0] = edge.getAttributes().createPoint(x2, from.getY());
//					//new Point(to.x, from.y)
//					routed[1] = edge.getAttributes().createPoint(x2, to.getY());
//				} else {
//					routed[0] = edge.getAttributes().createPoint(from.getX(), y2);
//					// new Point(from.x, to.y)
//					routed[1] = edge.getAttributes().createPoint(to.getX(), y2);
//				}
			}
			// Set/Add Points
			for (int i = 0; i < routed.length; i++)
				if (points.size() > i + 2)
					points.set(i + 1, routed[i]);
				else
					points.add(i + 1, routed[i]);
		}
	}

}
