/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.controlflow;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cfg.EdgeFilter;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.AbstractNumberedGraph;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NodeManager;
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.graph.traverse.DFS;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.IntSet;

import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.deprecated.jsdg.wala.FilteredEdgeManager;

/**
 * while(true) loops may have no controlflow path to the exit node. Dennis wants them
 * all to have a path. So we set each non-termination node as direct predecessor of the exit node.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class IgnoreNonTerminationCFG<I, T extends IBasicBlock<I>>
	implements ControlFlowGraph<I, T> {

	private final ControlFlowGraph<I, T> parent;

	private final boolean ignoreExceptions;
	private final Set<T> nonTerminating;

	public IgnoreNonTerminationCFG(ControlFlowGraph<I, T> parent, boolean ignoreExceptions) {
		this.parent = parent;
		this.ignoreExceptions = ignoreExceptions;
		this.nonTerminating = findNonTerminating();
	}

	private Set<T> findNonTerminating() {
		Set<T> noExit = HashSetFactory.make();

		Graph<T> temp = parent;

		if (ignoreExceptions) {
			final EdgeFilter<T> filter = new FilteredEdgeManager.ExceptionEdgePruner<I, T>(parent);

			temp = new AbstractNumberedGraph<T>() {
				private final NumberedEdgeManager<T> edges = new FilteredEdgeManager<I, T>(
						parent, parent, filter);

				@Override
				protected NumberedNodeManager<T> getNodeManager() {
					return parent;
				}

				@Override
				protected NumberedEdgeManager<T> getEdgeManager() {
					return edges;
				}
			};
		}

		for (T b : parent) {
			if (!b.isEntryBlock() && !b.isExitBlock()) {
				Set<T> reachable = DFS.getReachableNodes(temp, Collections
						.singleton(parent.entry()));

				if (!reachable.contains(parent.exit())) {
					Log.warn("Added cfg edge to exit for non-terminating basic block: " + b);
					noExit.add(b);
				}
			}
		}

		return noExit;
	}

	protected NodeManager<T> getNodeManager() {
		return parent;
	}

	protected EdgeManager<T> getEdgeManager() {
		return this;
	}

	public T entry() {
		return parent.entry();
	}

	public T exit() {
		return parent.exit();
	}

	public BitVector getCatchBlocks() {
		return parent.getCatchBlocks();
	}

	public T getBlockForInstruction(int index) {
		return parent.getBlockForInstruction(index);
	}

	public I[] getInstructions() {
		return parent.getInstructions();
	}

	public int getProgramCounter(int index) {
		return parent.getProgramCounter(index);
	}

	public IMethod getMethod() {
		return parent.getMethod();
	}

	public List<T> getExceptionalSuccessors(T b) {
		return parent.getExceptionalSuccessors(b);
	}

	public Iterator<T> getSuccNodes(T b) throws IllegalArgumentException {
		final Iterator<T> it = parent.getSuccNodes(b);

		if (b.isEntryBlock()) {
			return new Iterator<T>() {
					boolean exit = false;

			      public boolean hasNext() {
			          return !exit || it.hasNext();
			        }

			        public T next() {
			        	if (!exit) {
			        		exit = true;
			        		return parent.exit();
			        	}

			        	return it.next();
			        }

			        public void remove() {
			          Assertions.UNREACHABLE();
			        }
			};
		}

		return it;
	}

	public int getPredNodeCount(T node) {
		if (node == parent.exit()) {
			if (predsOfExit == null) {
				computePredsOfExit();
			}

			return predsOfExit.size();
		} else {
			return parent.getPredNodeCount(node);
		}
	}

	public int getSuccNodeCount(T node) {
		int succ = parent.getSuccNodeCount(node);

		if (node == parent.entry() && !parent.hasEdge(node, parent.exit())) {
			succ++;
		} else if (nonTerminating.contains(node) && !parent.hasEdge(node, parent.exit())) {
			succ++;
		}

		return succ;
	}

	private Collection<T> predsOfExit = null;

	private void computePredsOfExit() {
		Collection<T> col = HashSetFactory.make();
		col.add(parent.entry());

		Iterator<? extends T> it = parent.getPredNodes(parent.exit());
		while (it.hasNext()) {
			col.add(it.next());
		}

		for (T nt : nonTerminating) {
			col.add(nt);
		}

		predsOfExit = Collections.unmodifiableCollection(col);
	}

	public Iterator<T> getPredNodes(T b) throws IllegalArgumentException {
		if (b.isExitBlock()) {
			if (predsOfExit == null) {
				computePredsOfExit();
			}

			return predsOfExit.iterator();
		} else {
			return parent.getPredNodes(b);
		}
	}

	public Collection<T> getNormalSuccessors(T b) {
		Collection<T> norm = parent.getNormalSuccessors(b);

		// add edge to exit node for non-terminating stuff
		if (b == entry() || nonTerminating.contains(b)) {
			Collection<T> added = HashSetFactory.make(norm);
			added.add(parent.exit());
			norm = added;
		}

		return norm;
	}

	public Collection<T> getExceptionalPredecessors(T b) {
		return parent.getExceptionalPredecessors(b);
	}

	public Collection<T> getNormalPredecessors(T b) {
		Collection<T> preds = parent.getNormalPredecessors(b);

		if (b == exit()) {
			Collection<T> added = HashSetFactory.make(preds);
			added.add(parent.entry());
			preds = added;
		}

		// add non-terminating instructions as direct predecessors to exit
		if (b.isExitBlock()) {
			Collection<T> added = HashSetFactory.make(preds);
			added.addAll(nonTerminating);
			preds = added;
		}

		return preds;
	}

	public boolean hasEdge(T src, T dst) {
		return parent.hasEdge(src, dst)
			|| (src == parent.entry() && dst == parent.exit())
			|| (nonTerminating.contains(src) && dst == parent.exit());
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.Graph#removeNodeAndEdges(java.lang.Object)
	 */
	@Override
	public void removeNodeAndEdges(T n) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();////
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NodeManager#addNode(java.lang.Object)
	 */
	@Override
	public void addNode(T n) {
		throw new UnsupportedOperationException();////
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NodeManager#containsNode(java.lang.Object)
	 */
	@Override
	public boolean containsNode(T n) {
		return parent.containsNode(n);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NodeManager#getNumberOfNodes()
	 */
	@Override
	public int getNumberOfNodes() {
		return parent.getNumberOfNodes();
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NodeManager#iterator()
	 */
	@Override
	public Iterator<T> iterator() {
		return parent.iterator();
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NodeManager#removeNode(java.lang.Object)
	 */
	@Override
	public void removeNode(T n) {
		throw new UnsupportedOperationException();//
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#addEdge(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void addEdge(T src, T dst) {
		throw new UnsupportedOperationException();//
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#removeAllIncidentEdges(java.lang.Object)
	 */
	@Override
	public void removeAllIncidentEdges(T node)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();//
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#removeEdge(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void removeEdge(T src, T dst) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();//
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#removeIncomingEdges(java.lang.Object)
	 */
	@Override
	public void removeIncomingEdges(T node)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();//
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#removeOutgoingEdges(java.lang.Object)
	 */
	@Override
	public void removeOutgoingEdges(T node)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();//
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NumberedNodeManager#getMaxNumber()
	 */
	@Override
	public int getMaxNumber() {
		return parent.getMaxNumber();
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NumberedNodeManager#getNode(int)
	 */
	@Override
	public T getNode(int number) {
		return parent.getNode(number);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NumberedNodeManager#getNumber(java.lang.Object)
	 */
	@Override
	public int getNumber(T N) {
		return parent.getNumber(N);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NumberedNodeManager#iterateNodes(com.ibm.wala.util.intset.IntSet)
	 */
	@Override
	public Iterator<T> iterateNodes(IntSet s) {
		throw new UnsupportedOperationException();//return null;
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NumberedEdgeManager#getPredNodeNumbers(java.lang.Object)
	 */
	@Override
	public IntSet getPredNodeNumbers(T node) {
		throw new UnsupportedOperationException();//return null;
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NumberedEdgeManager#getSuccNodeNumbers(java.lang.Object)
	 */
	@Override
	public IntSet getSuccNodeNumbers(T node) {
		throw new UnsupportedOperationException();//return null;
	}

}
