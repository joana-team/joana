/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on May 14, 2004
 *
 */
package edu.kit.joana.ifc.sdg.graph;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.traverse.BreadthFirstIterator;

/**
 * @author hammer
 *
 */
public class SDGStructureIterator<D> extends BreadthFirstIterator<SDGNode, SDGEdge> {
	Queue<SDGNode> m_queue = new LinkedList<SDGNode>();
	/**
	 * @param arg0
	 */
	public SDGStructureIterator(DirectedPseudograph<SDGNode, SDGEdge> arg0) {
		super(arg0, findRoot(arg0));
	}

	private static SDGNode findRoot(DirectedPseudograph<SDGNode, SDGEdge> g) {
    SDGNode element = null;
    for (Iterator<SDGNode> iter = g.vertexSet().iterator(); iter.hasNext(); ) {
    	element = iter.next();
    	if (element.getId() == 1)
    		break;
    }
    return element;
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public SDGStructureIterator(DirectedPseudograph<SDGNode, SDGEdge> arg0, SDGNode arg1) {
		super(arg0, arg1);
	}

  /**
   * @see org.jgrapht.traverse.CrossComponentIterator#encounterVertex(java.lang.Object,
   *      org.jgrapht.Edge)
   */
  protected void encounterVertex( SDGNode vertex, SDGEdge edge ) {
  		if (edge == null || // start vertex
  				(edge.getKind() == SDGEdge.Kind.CONTROL_DEP_UNCOND ||
  						edge.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR ||
							edge.getKind() == SDGEdge.Kind.HELP ||
							edge.getKind() == SDGEdge.Kind.CALL)) {
        putSeenData( vertex, null ); // mark the vertex as visited
  			m_queue.add( vertex );
  		}
  }

  /**
   * @see org.jgrapht.traverse.CrossComponentIterator#isConnectedComponentExhausted()
   */
  protected boolean isConnectedComponentExhausted(  ) {
      return m_queue.isEmpty(  );
  }

  /**
   * @see org.jgrapht.traverse.CrossComponentIterator#encounterVertexAgain(java.lang.Object,
   *      org.jgrapht.Edge)
   */
  protected void encounterVertexAgain( SDGNode vertex, SDGEdge edge ) {}

  /**
   * @see org.jgrapht.traverse.CrossComponentIterator#provideNextVertex()
   */
  protected SDGNode provideNextVertex(  ) {
      return m_queue.remove(  );
  }
}
