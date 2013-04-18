/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.ContextSlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;

/**
 * A directed slicer is a slicer which exposes the direction (i.e. forward or backward) of propagation.
 * @author Martin Mohr
 */
public abstract class DirectedSlicer implements Slicer {

	public enum Direction {
		FORWARD, BACKWARD;
	}
	
	public abstract Direction getDirection();
	
	
	public static DirectedSlicer decorateWithDirection(Slicer slice, Direction dir) {
		return new DirectedSlicerDecorator(slice, dir);
	}
	
	/**
	 * This class decorates a given slicer s with a direction d. It implements the Slicer interface by delegating
	 * to s and returns d as direction, even if s itself is a DirectedSlicer. It does not check whether s is compatible
	 * with d. It is not prevented for example, that a {@link ContextSlicerBackward} is combined with {@link DirectedSlicer.Direction#FORWARD}.
	 * Note, however, that other classes may rely on this kind of compatibility.
	 * @author Martin Mohr
	 */
	private static class DirectedSlicerDecorator extends DirectedSlicer {
		
		private Slicer slicer;
		private DirectedSlicer.Direction dir;
		
		public DirectedSlicerDecorator(Slicer slicer, DirectedSlicer.Direction dir) {
			this.slicer = slicer;
			this.dir = dir;
		}
		
		/* (non-Javadoc)
		 * @see edu.kit.joana.ifc.sdg.graph.slicer.Slicer#setGraph(edu.kit.joana.ifc.sdg.graph.SDG)
		 */
		@Override
		public void setGraph(SDG graph) {
			slicer.setGraph(graph);
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.ifc.sdg.graph.slicer.Slicer#slice(java.util.Collection)
		 */
		@Override
		public Collection<SDGNode> slice(Collection<SDGNode> criteria) {
			return slicer.slice(criteria);
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.ifc.sdg.graph.slicer.Slicer#slice(edu.kit.joana.ifc.sdg.graph.SDGNode)
		 */
		@Override
		public Collection<SDGNode> slice(SDGNode criterion) {
			return slicer.slice(criterion);
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.ifc.sdg.core.DirectedSlicer#getDirection()
		 */
		@Override
		public Direction getDirection() {
			return dir;
		}

	}
}




