/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.wala;

import java.util.Iterator;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.ipa.cfg.EdgeFilter;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;

/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
@SuppressWarnings("deprecation")
public class FilteredEdgeManager<I, T extends IBasicBlock<I>> implements NumberedEdgeManager<T> {
	private final ControlFlowGraph<I, T> cfg;

	private final NumberedNodeManager<T> currentCFGNodes;

	private final EdgeFilter<T> filter;

	public FilteredEdgeManager(ControlFlowGraph<I, T> cfg,
			NumberedNodeManager<T> currentCFGNodes, EdgeFilter<T> filter) {
		this.cfg = cfg;
		this.filter = filter;
		this.currentCFGNodes = currentCFGNodes;
	}

	public static class ExceptionEdgePruner<I, T extends IBasicBlock<I>> implements EdgeFilter<T> {
		private final ControlFlowGraph<I, T> cfg;

		public ExceptionEdgePruner(ControlFlowGraph<I, T> cfg) {
			this.cfg = cfg;
		}

		public boolean hasNormalEdge(T src, T dst) {
			return cfg.getNormalSuccessors(src).contains(dst);
		}

		public boolean hasExceptionalEdge(T src, T dst) {
			return false;
		}
	};

	public static class NoEdgePruner<I, T extends IBasicBlock<I>> implements
			EdgeFilter<T> {
		private final ControlFlowGraph<I, T> cfg;

		public NoEdgePruner(ControlFlowGraph<I, T> cfg) {
			this.cfg = cfg;
		}

		public boolean hasNormalEdge(T src, T dst) {
			return cfg.getNormalSuccessors(src).contains(dst);
		}

		public boolean hasExceptionalEdge(T src, T dst) {
			return cfg.getExceptionalSuccessors(src).contains(dst);
		}
	};

	public Iterator<T> getExceptionalSuccessors(final T N) {
		return new FilterIterator<T>(
				cfg.getExceptionalSuccessors(N).iterator(), new Predicate<T>() {
					public boolean test(T o) {
						return currentCFGNodes.containsNode(o)
								&& filter.hasExceptionalEdge(N, o);
					}
				});
	}

	public Iterator<T> getNormalSuccessors(final T N) {
		return new FilterIterator<T>(cfg.getNormalSuccessors(N).iterator(),
				new Predicate<T>() {
					public boolean test(T o) {
						return currentCFGNodes.containsNode(o)
								&& filter.hasNormalEdge(N, o);
					}
				});
	}

	public Iterator<T> getExceptionalPredecessors(final T N) {
		return new FilterIterator<T>(cfg.getExceptionalPredecessors(N)
				.iterator(), new Predicate<T>() {
			public boolean test(T o) {
				return currentCFGNodes.containsNode(o)
						&& filter.hasExceptionalEdge(o, N);
			}
		});
	}

	public Iterator<T> getNormalPredecessors(final T N) {
		return new FilterIterator<T>(cfg.getNormalPredecessors(N).iterator(),
				new Predicate<T>() {
					public boolean test(T o) {
						return currentCFGNodes.containsNode(o)
								&& filter.hasNormalEdge(o, N);
					}
				});
	}

	public Iterator<T> getSuccNodes(final T N) {
		return new FilterIterator<T>(cfg.getSuccNodes(N), new Predicate<T>() {
			public boolean test(T o) {
				return currentCFGNodes.containsNode(o)
						&& (filter.hasNormalEdge(N, o) || filter
								.hasExceptionalEdge(N, o));
			}
		});
	}

	public int getSuccNodeCount(T N) {
		return Iterator2Collection.toList(getSuccNodes(N)).size();
	}

	public IntSet getSuccNodeNumbers(T N) {
		MutableIntSet bits = IntSetUtil.make();
		for (Iterator<T> EE = getSuccNodes(N); EE.hasNext();) {
			bits.add(EE.next().getNumber());
		}

		return bits;
	}

	public Iterator<T> getPredNodes(final T N) {
		return new FilterIterator<T>(cfg.getPredNodes(N), new Predicate<T>() {
			public boolean test(T o) {
				return currentCFGNodes.containsNode(o)
						&& (filter.hasNormalEdge(o, N) || filter
								.hasExceptionalEdge(o, N));
			}
		});
	}

	public int getPredNodeCount(T N) {
		return Iterator2Collection.toList(getPredNodes(N)).size();
	}

	public IntSet getPredNodeNumbers(T N) {
		MutableIntSet bits = IntSetUtil.make();
		for (Iterator<T> EE = getPredNodes(N); EE.hasNext();) {
			bits.add(EE.next().getNumber());
		}

		return bits;
	}

	public boolean hasEdge(T src, T dst) {
		for (Iterator<T> EE = getSuccNodes(src); EE.hasNext();) {
			if (EE.next().equals(dst)) {
				return true;
			}
		}

		return false;
	}

	public void addEdge(T src, T dst) {
		throw new UnsupportedOperationException();
	}

	public void removeEdge(T src, T dst) {
		throw new UnsupportedOperationException();
	}

	public void removeAllIncidentEdges(T node) {
		throw new UnsupportedOperationException();
	}

	public void removeIncomingEdges(T node) {
		throw new UnsupportedOperationException();
	}

	public void removeOutgoingEdges(T node) {
		throw new UnsupportedOperationException();
	}
}
