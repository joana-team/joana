/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.util.Pair;

class GraphStats implements Iterable<Pair<String, MethodStats>> {

	private final SortedMap<String, List<MethodStats>> stats = new TreeMap<String, List<MethodStats>>();

	private GraphStats() {
	}

	private void addMethodStatsFor(String mSig, MethodStats mStats) {
		List<MethodStats> lsMethodStats;
		if (!stats.containsKey(mSig)) {
			lsMethodStats = new LinkedList<MethodStats>();
			stats.put(mSig, lsMethodStats);
		} else {
			lsMethodStats = stats.get(mSig);
		}
		lsMethodStats.add(mStats);
	}

	private boolean containsMethodStatsFor(String mSig) {
		return stats.containsKey(mSig);
	}

	private List<MethodStats> getMethodStatsFor(String mSig) {
		return stats.get(mSig);
	}

	private Set<String> getMethodSignatures() {
		return stats.keySet();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Pair<String, MethodStats>> iterator() {
		final Iterator<Entry<String, List<MethodStats>>> statsIter = stats.entrySet().iterator();
		return new Iterator<Pair<String, MethodStats>>() {

			Entry<String, List<MethodStats>> curEntry = null;
			Iterator<MethodStats> curIter = null;

			@Override
			public boolean hasNext() {
				if (curEntry == null) {
					assert curIter == null;
					return statsIter.hasNext();
				} else {
					assert curIter != null;
					return curIter.hasNext() || statsIter.hasNext();
				}
			}

			@Override
			public Pair<String, MethodStats> next() {
				if (curEntry == null) {
					assert curIter == null;
					curEntry = statsIter.next();
					curIter = curEntry.getValue().iterator();
					// inv: all lists occurring as values are non-empty
					assert curIter.hasNext();
					return Pair.pair(curEntry.getKey(), curIter.next());
				} else {
					assert curIter != null;
					if (curIter.hasNext()) {
						return Pair.pair(curEntry.getKey(), curIter.next());
					} else {
						curEntry = statsIter.next();
						curIter = curEntry.getValue().iterator();
						// inv: all lists occurring as values are non-empty
						assert curIter.hasNext();
						return Pair.pair(curEntry.getKey(), curIter.next());
					}
				}

			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("This operation is not supported!");
			}

		};
	}

	public static GraphStats computeFrom(SDG sdg) {
		GraphStats ret = new GraphStats();
		for (SDGNode node : sdg.vertexSet()) {
			if (node.getKind() == SDGNode.Kind.ENTRY && !ret.containsMethodStatsFor(node.getBytecodeMethod())) {
				ret.addMethodStatsFor(node.getBytecodeMethod(), MethodStats.computeFrom(node, sdg));
			}
		}
		return ret;
	}

	public static GraphStats union(GraphStats gs1, GraphStats gs2) {
		Set<String> mSigs = new HashSet<String>();
		mSigs.addAll(gs1.getMethodSignatures());
		mSigs.retainAll(gs2.getMethodSignatures());
		if (!mSigs.isEmpty()) {
			throw new IllegalArgumentException("Cannot join graph stats with common methods!");
		} else {
			GraphStats ret = new GraphStats();
			for (Pair<String, MethodStats> e : gs1) {
				ret.addMethodStatsFor(e.getFirst(), e.getSecond());
			}

			for (Pair<String, MethodStats> e : gs2) {
				ret.addMethodStatsFor(e.getFirst(), e.getSecond());
			}
			return ret;
		}
	}

	public static GraphStats difference(GraphStats gs1, GraphStats gs2) {
		GraphStats ret = new GraphStats();
		for (Pair<String, MethodStats> e : gs1) {
			if (gs2.containsMethodStatsFor(e.getFirst())) {
				List<MethodStats> lsMDiff = MethodStats.subtract(e.getSecond(), gs2.getMethodStatsFor(e.getFirst()));
				for (MethodStats mDiff : lsMDiff) {
					if (!mDiff.isZero()) {
						ret.addMethodStatsFor(e.getFirst(), mDiff);
					}
				}
			} else {
				ret.addMethodStatsFor(e.getFirst(), e.getSecond());
			}
		}
		return ret;
	}

	public static GraphStats symmetricDifference(GraphStats gs1, GraphStats gs2) {
		return union(difference(gs1, gs2), difference(gs2, gs1));
	}
}