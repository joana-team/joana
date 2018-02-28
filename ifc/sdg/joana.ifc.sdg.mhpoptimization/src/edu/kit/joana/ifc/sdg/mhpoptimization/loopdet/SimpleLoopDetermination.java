/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.mhpoptimization.loopdet;

import java.util.HashSet;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.FoldedCFG;
import edu.kit.joana.ifc.sdg.mhpoptimization.ThreadAllocationAnalysis;

/**
 * This implementation of {@link LoopDetermination} is the old analysis which relies on the folded control-flow graph and is very conservative. For example, it does consider a fork
 * which is executed only once and definitely happens after a loop as dynamic.<p/>
 * This analysis has been moved out of {@link ThreadAllocationAnalysis}, so that it can be replaced by a more precise analysis with the option of going back, if there are bugs.
 * @author  Dennis Giffhorn, Martin Mohr
 */
public class SimpleLoopDetermination<C extends Context<C>> implements LoopDetermination<C> {
	
	private final FoldedCFG folded;
	private final DynamicContextManager conMan;
	
	public SimpleLoopDetermination(FoldedCFG folded, DynamicContextManager conMan) {
		this.folded = folded;
		this.conMan = conMan;
	}
	
	
	public boolean isInALoop(C thread) {
    	LinkedList<C> w = new LinkedList<>();
		HashSet<C> visited = new HashSet<>();
		w.add(thread);
		visited.add(thread);

		while(!w.isEmpty()) {
			C next = w.poll();
			SDGNode node = folded.map(next.getNode());

			if (node.getId() < 0) { // loop: return true
				return true;
			}

			for (SDGEdge e : folded.incomingEdgesOf(node)) {
				SDGNode source = e.getSource();

				if (e.getKind() == SDGEdge.Kind.CONTROL_FLOW) {
			        C newContext = next.level(source);

					if (visited.add(newContext)) {
						w.add(newContext);
					}

				} else if (e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.FORK) {
					// the call site calling the procedure to descend into
			        SDGNode mapped = conMan.map(source);
			        C newContext = null;

			        // if the corresponding call site is recursive,
			        // clone context and set 'source' as new node
			        // else ascend to the calling procedure
			        if (conMan.isFolded(source) && next.top() == mapped) {
			        	newContext = next.level(source);

			        } else {
			        	newContext = next.ascend(source, new SDGNodeTuple(source, null));
			        }

			        if (visited.add(newContext)) {
			        	w.add(newContext);
			        }
				}
			}
		}

		return false;
	}
}
