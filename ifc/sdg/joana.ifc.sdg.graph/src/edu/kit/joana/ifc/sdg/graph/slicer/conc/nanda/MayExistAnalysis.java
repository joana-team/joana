/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.ContextGraph.ContextEdge;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.BitMatrix;


public class MayExistAnalysis {
	private final BitMatrix map;
	private final Collection<TopologicalNumber>[] slices;

	public MayExistAnalysis(BitMatrix map, Collection<TopologicalNumber>[] slices) {
		this.map = map;
		this.slices = slices;
	}

	public boolean mayExist(int thread, TopologicalNumber nr, int nrThread) {
		if (thread == 0) {
			return true; // TODO: hack, which is sound as long as no joins exist

		} else if (map.get(thread, nrThread)) {
			return true;

		}

		return slices[thread].contains(nr) || slices[nrThread].contains(nr);
	}


	/* FACTORY */

	public static MayExistAnalysis create(ContextGraphs cg) {
		Analysis a = new Analysis();
		MayExistAnalysis mea = a.analyze(cg);

		return mea;
	}

	private static class Analysis {
		private ContextGraphs cg;
		private ContextGraph g;

		public MayExistAnalysis analyze(ContextGraphs cg) {
			this.cg = cg;
			this.g = cg.getWholeGraph();

			BitMatrix map = new BitMatrix(cg.getNumberOfThreads());
			@SuppressWarnings("unchecked")
			final Collection<TopologicalNumber>[] slices =
					(Collection<TopologicalNumber>[]) new Collection[cg.getNumberOfThreads()];

			HashMap<TopologicalNumber, HashSet<Integer>> forksToThreads = forksToThreads();
			Collection<TopologicalNumber> allForks = cg.getAllForkSites();

			// which threads may happen in parallel to each other due to each fork?
			for (TopologicalNumber fork : allForks) {
//				System.out.println("fork: "+fork);
//				for (ContextEdge e : g.outgoingEdgesOf(fork)) {
//		        	if (e.getKind() == SDGEdge.Kind.FORK) {
//		        		System.out.println("fork edge: "+e);
//		        	}
//		        }

				// these are the strings spanned by the fork
				Collection<TopologicalNumber>[] forked = forkedBy(fork);
				for (Collection<TopologicalNumber> c : forked) {
					c.retainAll(allForks);
//					System.out.println("spanned: "+c);
				}

				// translate them to the started threads
				@SuppressWarnings("unchecked")
				final HashSet<Integer>[] threads = (HashSet<Integer>[]) new HashSet[forked.length];
				for (int i = 0; i < threads.length; i++) {
					HashSet<Integer> ints = new HashSet<Integer>();
					for (TopologicalNumber f : forked[i]) {
						ints.addAll(forksToThreads.get(f));
					}
					threads[i] = ints;
//					System.out.println("spanned threads: "+ints);
				}

				// create the map
				for (HashSet<Integer> t : threads) {
					for (HashSet<Integer> u : threads) {
						if (t == u) continue;

						for (int i : t) {
							for (int j : u) {
								map.set(i, j);
								map.set(j, i);
							}
						}
					}
				}
			}

			// which contexts are reachable by the fork of each thread?
			for (int thread = 0; thread < cg.getNumberOfThreads(); thread++) {
				Collection<TopologicalNumber> forks = cg.getForkSites(thread);
				Collection<TopologicalNumber> behind = behind(forks);
				slices[thread] = behind;
			}

			return new MayExistAnalysis(map, slices);
		}


		private HashMap<TopologicalNumber, HashSet<Integer>> forksToThreads() {
			HashMap<TopologicalNumber, HashSet<Integer>> result =
				new HashMap<TopologicalNumber, HashSet<Integer>>();

			for (int thread = 0; thread < cg.getNumberOfThreads(); thread++) {
				for (TopologicalNumber n : cg.getForkSites(thread)) {
					HashSet<Integer> s = result.get(n);
					if (s == null) {
						s = new HashSet<Integer>();
						result.put(n, s);
					}
					s.add(thread);
				}
			}

			return result;
		}

		private Collection<TopologicalNumber>[] forkedBy(TopologicalNumber fork) {
			LinkedList<ContextEdge> edges = new LinkedList<ContextEdge>();
			for (ContextEdge e : g.outgoingEdgesOf(fork)) {
	        	if (e.getKind() != SDGEdge.Kind.HELP) {
	        		edges.add(e);
	        	}
	        }

			@SuppressWarnings("unchecked")
			final Collection<TopologicalNumber>[] result =
				(Collection<TopologicalNumber>[]) new Collection[edges.size()];

			int i = 0;
	        for (ContextEdge e : edges) {
	        	result[i] = forwardSlice(Collections.singleton(e.getTarget()));
	        	if (e.getKind() == SDGEdge.Kind.FORK) {
	        		result[i].add(fork);
	        	}
	        	i++;
	        }

	        return result;
		}

		private Collection<TopologicalNumber> behind(Collection<TopologicalNumber> forks) {
			HashSet<TopologicalNumber> behind = new HashSet<TopologicalNumber>();

			for (TopologicalNumber fork : forks) {
		        for (ContextEdge e : g.outgoingEdgesOf(fork)) {
		        	if (e.getKind() != SDGEdge.Kind.FORK && e.getKind() != SDGEdge.Kind.HELP) {
		        		behind.add(e.getTarget());
		        	}
		        }
			}

	        return forwardSlice(behind);
		}

		public Collection<TopologicalNumber> forwardSlice(Collection<TopologicalNumber> crit) {
			LinkedList<TopologicalNumber> worklist = new LinkedList<TopologicalNumber>();
			HashSet<TopologicalNumber> marked = new HashSet<TopologicalNumber>();

			worklist.addAll(crit);
			marked.addAll(crit);

			while(!worklist.isEmpty()) {
				TopologicalNumber next = worklist.poll();

				for (ContextEdge e : g.outgoingEdgesOf(next)) {
					if (e.getKind() == SDGEdge.Kind.HELP) continue;

					TopologicalNumber succ = e.getTarget();

					if (marked.add(succ)) {
						worklist.add(succ);
					}
				}
			}

			return marked;
		}
	}
}
