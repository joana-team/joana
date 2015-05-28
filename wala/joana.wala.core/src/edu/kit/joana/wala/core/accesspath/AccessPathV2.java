/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


import org.jgrapht.DirectedGraph;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.wala.core.DependenceGraph;
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.accesspath.APIntraProcV2.MergeInfo;
import edu.kit.joana.wala.summary.SummaryComputation;
import edu.kit.joana.wala.summary.WorkPackage;
import edu.kit.joana.wala.summary.WorkPackage.EntryPoint;
import edu.kit.joana.wala.util.WriteGraphToDot;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class AccessPathV2 {

	private final SDGBuilder sdg;

	/**
	 * Returns a subset of all data heap dependencies. Those that are dependent on aliasing.
	 * @param method entry method for the interprocedural computation.
	 * @throws CancelException
	 */
	public static APResult compute(final SDGBuilder sdg, final PDG start) throws CancelException {
		if (sdg.cfg.fieldPropagation != FieldPropagation.OBJ_TREE
				&& sdg.cfg.fieldPropagation != FieldPropagation.OBJ_TREE_NO_FIELD_MERGE
				&& sdg.cfg.fieldPropagation != FieldPropagation.OBJ_TREE_AP) {
			throw new IllegalStateException("Access path can only be computed when object tree is turned on. "
					+ "Currently used parameter propagation: " + sdg.cfg.fieldPropagation);
		}

		final AccessPathV2 ap = new AccessPathV2(sdg);

		return ap.run(start);
	}
	
	private AccessPathV2(final SDGBuilder sdg) {
		this.sdg = sdg;
	}

	private Set<PDG> findReachable(final PDG start) {
		final Set<PDG> reachable = new HashSet<PDG>();

		final CallGraph cg = sdg.getWalaCallGraph();
		final LinkedList<PDG> todo = new LinkedList<PDG>();
		todo.add(start);

		while (!todo.isEmpty()) {
			final PDG pdg = todo.removeFirst();
			reachable.add(pdg);

			for (Iterator<CGNode> succs = cg.getSuccNodes(pdg.cgNode); succs.hasNext();) {
				final CGNode succ = succs.next();
				final PDG next = sdg.getPDGforMethod(succ);

				if (next != null && !reachable.contains(next)) {
					todo.add(next);
				}
			}
		}

		return Collections.unmodifiableSet(reachable);
	}

	private APResult run(final PDG start) throws CancelException {
		if (start == null) {
			return new APResult();
		}

		final Set<PDG> reachable = findReachable(start);
		final Map<PDG, APIntraProcV2> pdg2ap = new HashMap<PDG, APIntraProcV2>();
		for (PDG pdg : reachable) {
			final APIntraProcV2 aip = APIntraProcV2.compute(pdg, sdg.cfg);
			pdg2ap.put(pdg, aip);
		}

		boolean changed = true;
		while (changed) {
			changed = false;

			// propagate only from callee to callsite
			for (final PDG pdg : reachable) {
				for (final PDGNode call : pdg.getCalls()) {
					for (final PDG callee : sdg.getPossibleTargets(call)) {
						changed |= propagateCalleeToSite(callee, call, pdg, pdg2ap);
					}
				}
			}
		}

		final APResult result = new APResult();
		for (final PDG pdg : reachable) {
			final APIntraProcV2 ap = pdg2ap.get(pdg);
			final int numOfAliasEdges = ap.findAndMarkAliasEdges();
			ap.addAliasConditionToActualIns();
			ap.addPotentialAliasInfoToFormalIns();
			ap.computeReachingMerges(NullProgressMonitor.INSTANCE);
			ap.buildAP2NodeMap();
			final MergeInfo mnfo = ap.getMergeInfo();
			mnfo.setNumAliasEdges(numOfAliasEdges);
			result.add(mnfo);
		}

		if (sdg.cfg.debugAccessPath) {
			for (final PDG pdg :reachable) {
				final APIntraProcV2 ap = pdg2ap.get(pdg);
				APUtil.writeAliasEdgesToFile(ap, sdg.cfg.debugAccessPathOutputDir, pdg, "-ap.txt");
				ap.dumpGraph("-apg.dot");
			}
		}
		
		return result;
	}

	private boolean propagateCalleeToSite(final PDG callee, final PDGNode call, final PDG caller,
			final Map<PDG, APIntraProcV2> pdg2ap) {
		boolean changed = false;

		final APIntraProcV2 aipCaller = pdg2ap.get(caller);
		final APIntraProcV2 aipCallee = pdg2ap.get(callee);

		changed |= aipCaller.propagateFrom(aipCallee, call);

		return changed;
	}

	/**
	 * Compute summary edges for information flow without heap aliases and with aliases. Currently SUMMARY_P is used for
	 * "always there dataflow". DATA_HEAP is used for always on information flow (summary edges without aliasing) and
	 * finally SUMMARY is used for the most conservative approximation: summary edges with aliasing.
	 * @param out
	 * @param builder
	 * @param start
	 * @param sdg
	 * @param progress
	 * @throws CancelException
	 */
	public static void computeMinMaxAliasSummaryEdges(PrintStream out, SDGBuilder builder, PDG start, SDG sdg, IProgressMonitor progress) throws CancelException {
		out.print("minalias");
		Set<EntryPoint> entries = new TreeSet<EntryPoint>();
		PDG pdg = builder.getMainPDG();
		TIntSet formIns = new TIntHashSet();
		for (PDGNode p : pdg.params) {
			formIns.add(p.getId());
		}
		TIntSet formOuts = new TIntHashSet();
		formOuts.add(pdg.exception.getId());
		formOuts.add(pdg.exit.getId());
		EntryPoint ep = new EntryPoint(pdg.entry.getId(), formIns, formOuts);
		entries.add(ep);
		WorkPackage pack = WorkPackage.create(sdg, entries, sdg.getName());
		SummaryComputation.computeNoAliasDataDep(pack, progress);
		out.print(".");

		out.print("maxalias");
		WorkPackage pack2 = WorkPackage.create(sdg, entries, sdg.getName());
		SummaryComputation.computeFullAliasDataDep(pack2, progress);
		out.print(".");
	}

	public static class AliasEdge {
		public final PDGEdge edge;
		public final TIntSet fromAlias;
		public final TIntSet toAlias;

		public AliasEdge(PDGEdge edge) {
			this.edge = edge;
			this.fromAlias = new TIntHashSet();
			this.toAlias = new TIntHashSet();
		}

		public int hashCode() {
			return edge.hashCode();
		}

		public boolean equals(Object obj) {
			if (obj instanceof AliasEdge) {
				AliasEdge other = (AliasEdge) obj;
				return this.edge.equals(other.edge);
			}

			return false;
		}

		public String toString() {
			return edge.from.getLabel() + "-(" + fromAlias + " alias " + toAlias + ")->" + edge.to.getLabel();
		}
	}

	private static String nfo(PDGNode n) {
		return n.getId() + "|" + n.getKind() + "(" + n.getLabel() + ")";
	}

	@SuppressWarnings("unused")
	private <V,E> void debugWriteGraphToDisk(final DirectedGraph<V, E> g, final String fileName) {
		assert sdg.cfg.debugAccessPath;
		assert sdg.cfg.debugAccessPathOutputDir != null;

		try {
			WriteGraphToDot.write(g, sdg.cfg.debugAccessPathOutputDir + File.separator + fileName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	private static void debugPrintAliasEdges(final List<AliasEdge> alias) {
		for (AliasEdge ae : alias) {
			System.out.print(nfo(ae.edge.from) + " -alias-> " + nfo(ae.edge.to) + "\t\treason: { ");
			for (TIntIterator it = ae.fromAlias.iterator(); it.hasNext();) {
				int i = it.next();
				System.out.printf("%d ", i);
			}
			System.out.print("} aliasing { ");
			for (TIntIterator it = ae.toAlias.iterator(); it.hasNext();) {
				int i = it.next();
				System.out.printf("%d ", i);
			}
			System.out.println("}");
		}
	}

	@SuppressWarnings("unused")
	private static void debugPrintHeapEdges(final DependenceGraph hdg) {
		for (PDGEdge e : hdg.edgeSet()) {
			if (e.kind == PDGEdge.Kind.DATA_HEAP) {
				System.out.println(nfo(e.from) + " -heap-> " + nfo(e.to));
			}
		}
	}

	@SuppressWarnings("unused")
	private static void debugPrintEquivClasses(final List<Set<PDGNode>> equivClasses) {
		System.out.println("found " + equivClasses.size() + " equiv classes.");
		for (Set<PDGNode> equiv : equivClasses) {
			System.out.print("{ ");
			for (PDGNode n : equiv) {
				System.out.print(n.getId() + " ");
			}
			System.out.println("}");
		}
	}
}
