/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.eval.util;

import java.util.HashMap;
import java.util.Map;

import edu.kit.joana.api.sdg.ConstructionNotifier;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.ifc.sdg.graph.SDG;

/**
 * Collects information from the sdg computation phase
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class EvalTimingStats implements ConstructionNotifier {

	public class TaskInfo {
		
		public TaskInfo(final String name) {
			this.name = name;
		}
		
		public final String name;
		public long startTime;
		public long finishTime;
		public int numberUnpruned;
		public int numberPruned;
		public long startStripCD;
		public long finishStripCD;
		public int numberOfNodes;
		public int numberOfEdges;
		
		public String toString() {
			return name + ": " + numberPruned + " methods (" + numberUnpruned + " unpruned) total of " + numberOfNodes
				+ " nodes and " + numberOfEdges + " edges. Computation time was " + (finishTime - startTime) + " ms";
		}
	}
	
	private TaskInfo current = new TaskInfo("standard");
	private Map<String, TaskInfo> name2info = new HashMap<String, TaskInfo>();
	
	public void setCurrentTask(final String name) {
		current = new TaskInfo(name);
	}
	
	public void saveCurrent() {
		name2info.put(current.name, current);
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.ConstructionNotifier#sdgStarted()
	 */
	@Override
	public void sdgStarted() {
		current.startTime = System.currentTimeMillis();
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.ConstructionNotifier#sdgFinished()
	 */
	@Override
	public void sdgFinished() {
		current.finishTime = System.currentTimeMillis();
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.ConstructionNotifier#numberOfCGNodes(int, int)
	 */
	@Override
	public void numberOfCGNodes(final int numberUnpruned, final int numberPruned) {
		current.numberUnpruned = numberUnpruned;
		current.numberPruned = numberPruned;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.ConstructionNotifier#stripControlDepsStarted()
	 */
	@Override
	public void stripControlDepsStarted() {
		current.startStripCD = System.currentTimeMillis();
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.ConstructionNotifier#stripControlDepsFinished()
	 */
	@Override
	public void stripControlDepsFinished() {
		current.finishStripCD = System.currentTimeMillis();
	}

	public void readAdditionalStats(final SDG sdg, final SDGConfig cfg) {
		current.numberOfNodes = sdg.vertexSet().size();
		current.numberOfEdges = sdg.edgeSet().size();
	}

	public TaskInfo getCurrent() {
		return current;
	}
}
