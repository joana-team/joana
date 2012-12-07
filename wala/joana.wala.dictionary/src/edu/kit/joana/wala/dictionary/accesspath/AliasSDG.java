/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.dictionary.accesspath;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.wala.summary.GraphUtil;
import edu.kit.joana.wala.summary.SummaryComputation;
import edu.kit.joana.wala.summary.WorkPackage;
import edu.kit.joana.wala.summary.GraphUtil.SummaryProperties;
import edu.kit.joana.wala.summary.WorkPackage.EntryPoint;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;

public class AliasSDG {

	private final SDG sdg;
	private WorkPackage workPack;
	private final Alias mayAlias = new Alias();
	private final Alias noAlias = new Alias();
	private final List<SDGEdge> currentlyRemoved = new LinkedList<SDGEdge>();

	//done: rewrite to contain pairs, because this is wrong: !(a,a) && !(b,b) => !(a,b)
	public static class Alias implements Cloneable {

		private static final class P {
			private final int id1; // id1 <= id2
			private final int id2;

			private P(final int num1, final int num2) {
				if (num1 <= num2) {
					this.id1 = num1;
					this.id2 = num2;
				} else {
					this.id1 = num2;
					this.id2 = num1;
				}
			}

			public boolean equals(final Object o) {
				if (this == o) {
					return true;
				}

				if (o instanceof P) {
					final P other = (P) o;
					return id1 == other.id1 && id2 == other.id2;
				}

				return false;
			}

			public int hashCode() {
				return id1 + 23 * id2;
			}

			public String toString() {
				return "(" + id1 + "," + id2 + ")";
			}
		}

		private final Set<P> set = new HashSet<P>();

		public Alias clone() {
			final Alias clone = new Alias();
			clone.set.addAll(set);

			return clone;
		}

		public int size() {
			return set.size();
		}

		public boolean add(final int a, final int b) {
			final P p = new P(a, b);

			return set.add(p);
		}

		public void reset() {
			set.clear();
		}

		public boolean isSet(final int a, final int b) {
			final P p = new P(a, b);

			return set.contains(p);
		}

		public boolean addAll(final Alias otherAlias) {
			return set.addAll(otherAlias.set);
		}

		public boolean containsAll(final AliasCondition ac) {
			for (final IntIterator it1 = ac.id1.intIterator(); it1.hasNext();) {
				final int i1 = it1.next();
				for (final IntIterator it2 = ac.id2.intIterator(); it2.hasNext();) {
					final int i2 = it2.next();
					if (!isSet(i1, i2)) {
						return false;
					}
				}
			}

			return true;
		}

		public boolean containsAny(final AliasCondition ac) {
			for (final IntIterator it1 = ac.id1.intIterator(); it1.hasNext();) {
				final int i1 = it1.next();
				for (final IntIterator it2 = ac.id2.intIterator(); it2.hasNext();) {
					final int i2 = it2.next();
					if (isSet(i1, i2)) {
						return true;
					}
				}
			}

			return false;
		}

		/**
		 * performs a logical and to the aliases stored in this class, using the aliases given in the
		 * parameter otherAlias.
		 * All aliases that are not contained in otherAlias are removed from this Alias configuration.
		 */
		public void and(final Alias otherAlias) {
			final List<P> toRemove = new LinkedList<P>();

			for (final P p : set) {
				if (!otherAlias.set.contains(p)) {
					toRemove.add(p);
				}
			}

			set.removeAll(toRemove);
		}

		public String toString() {
			return "Alias: " + set.toString();
		}

	}

	private AliasSDG(final SDG sdg, final WorkPackage workPack) {
		this.sdg = sdg;
		this.workPack = workPack;
	}

	public static AliasSDG create(final SDG sdg) {
		final WorkPackage wp = createWorkPack(sdg);

		return new AliasSDG(sdg, wp);
	}

	public static AliasSDG readFrom(final String file) throws IOException {
		final SDG sdg = SDG.readFrom(file);
		final WorkPackage wp = createWorkPack(sdg);

		return new AliasSDG(sdg, wp);
	}

	public static AliasSDG readFrom(final Reader reader) throws IOException {
		final SDG sdg = SDG.readFrom(reader);
		final WorkPackage wp = createWorkPack(sdg);

		return new AliasSDG(sdg, wp);
	}

	public SDG getSDG() {
		return sdg;
	}

	public void setNoAlias(final Alias alias) {
		this.noAlias.reset();
		this.noAlias.addAll(alias);
	}

	public String getFileName() {
		return sdg.getFileName();
	}

	public Alias getNoAlias() {
		return noAlias.clone();
	}

	/**
	 * Adjusts the internal SDG to the current alias setting. Returns true iff some edges have changed:
	 * added heap dependencies, removed alias dependencies.
	 * @return true iff some edges have changed: added heap dependencies, removed alias dependencies.
	 * @throws CancelException
	 */
	public boolean adjustSDG(final IProgressMonitor progress) throws CancelException {
		final List<SDGEdge> toDisable = new LinkedList<SDGEdge>();
		final Logger debug = Log.getLogger(Log.L_MOJO_DEBUG);
		
		
		if (noAlias.size() > 0) {
			if (debug.isEnabled()) {
				debug.out("propagating no-aliases... ");
				final int sizeBefore = noAlias.size();

				if (propagateAliasesFromCallSites()) {
					debug.out("[" + sizeBefore + " => " + noAlias.size() + "] ");
				} else {
					debug.out("[no change] ");
				}

				debug.outln("done.");
			} else {
				propagateAliasesFromCallSites();
			}
		}

		int aliasEdges = 0;

		for (final SDGEdge e : sdg.edgeSet()) {
			if (e.getKind() == SDGEdge.Kind.DATA_ALIAS) {
				MonitorUtil.throwExceptionIfCanceled(progress);
				aliasEdges++;
				final AliasCondition ac = parseAliasEdge(e);
				if (noAlias.containsAll(ac)) {
					toDisable.add(e);
				}
			}
		}

		debug.outln("disabling " + toDisable.size() + " of " + aliasEdges + " alias edges.");

		for (final SDGEdge alias : toDisable) {
			sdg.removeEdge(alias);
			currentlyRemoved.add(alias);
		}

		return !toDisable.isEmpty();
	}

	private boolean propagateAliasesFromCallSites() {
		boolean totalChange = false;
		// propagate alias from call-sites to formal-in of callees
		boolean change = true;
		while (change) {
			change = false;

			for (final SDGNode entry : sdg.vertexSet()) {
				if (entry.kind == SDGNode.Kind.ENTRY) {
					// summarize no-aliases from all callsites (or clone for specific alias sets)
					Alias mergedAliases = null;
					for (final SDGNode call : sdg.getCallers(entry)) {
						final Alias callAlias = propagateAliasesFromCall(call, entry);
						if (mergedAliases == null) {
							mergedAliases = callAlias;
						} else {
							// only set no-aliases that are valid in all calling contexts
							mergedAliases.and(callAlias);
						}
					}

					if (mergedAliases != null) {
						change |= noAlias.addAll(mergedAliases);
						totalChange |= change;
					}
				}
			}
		}

		return totalChange;
	}

	private Alias propagateAliasesFromCall(final SDGNode call, final SDGNode entry) {
		final Alias alias = new Alias();

		// compute no-aliases for current call (containing formal-in nodes of entry)
		final Set<SDGNode> formIns = sdg.getFormalInsOfProcedure(entry);
		final SDGNode[] actIns = new SDGNode[formIns.size()];
		final int[] formInId = new int[actIns.length];
		int cur = 0;
		for (final SDGNode fIn : formIns) {
			final SDGNode actIn = sdg.getActualIn(call, fIn);
			assert actIn != null;
			actIns[cur] = actIn;
			formInId[cur] = fIn.getId();
			cur++;
		}

		for (int i = 0; i < actIns.length; i++) {
			final SDGNode a1 = actIns[i];
			for (int i2 = i; i2 < actIns.length; i2++) {
				final SDGNode a2 = actIns[i2];

				if (isNotAliased(a1, a2)) {
					final int f1id = formInId[i];
					final int f2id = formInId[i2];

					alias.add(f1id, f2id);
				}
			}
		}

		return alias;
	}

	private boolean isNotAliased(final SDGNode n1, final SDGNode n2) {
		final TIntSet n1a = n1.getAliasDataSources();
		final TIntSet n2a = n2.getAliasDataSources();

		/*
		 *
		 */
		if (n1a != null && n2a != null) {
			if (n1 == n2) {
				if (n1a.contains(n2.getId())) {
					return false;
				} else {
					final TIntIterator it = n1a.iterator();
					while (it.hasNext()) {
						final int id = it.next();
						if (sdg.getNode(id).kind == SDGNode.Kind.FORMAL_IN) {
							// other kinds (new-sites) are not aliased by default
							if (!noAlias.isSet(id, id)) {
								return false;
							}
						}
					}
				}
			} else {
				for (final TIntIterator it1 = n1a.iterator(); it1.hasNext();) {
					final int id1 = it1.next();

					for (final TIntIterator it2 = n2a.iterator(); it2.hasNext();) {
						final int id2 = it2.next();

						if (!noAlias.isSet(id1, id2)) {
							return false;
						}
					}
				}
			}

			return true;
		}

		return false;
	}

	/**
	 * Resets all adjustments that were made to the alias sdg. This adds previously disabled edges and
	 * removes all non- and may-aliasing states.
	 */
	public void reset() {
		noAlias.reset();
		mayAlias.reset();
		workPack.reset();

		final List<SDGEdge> toDelete = new LinkedList<SDGEdge>();

		for (final SDGEdge e : sdg.edgeSet()) {
			if (e.getKind() == SDGEdge.Kind.SUMMARY_DATA) {
				toDelete.add(e);
			}
		}

		sdg.removeAllEdges(toDelete);

		if (!currentlyRemoved.isEmpty()) {
			sdg.addAllEdges(currentlyRemoved);
			currentlyRemoved.clear();
		}
	}

	/**
	 * Creates a summary computation workpackage that holds for all alias configurations
	 * @param sdg The SDG we compute the work package for.
	 * @return A summary computation work package for the given SDG.
	 */
	private static WorkPackage createWorkPack(final SDG sdg) {
		final EntryPoint ep = GraphUtil.extractEntryPoint(sdg, sdg.getRoot());
		final Set<WorkPackage.EntryPoint> entries =	Collections.singleton(ep);
		final SummaryProperties sumProp = GraphUtil.createSummaryProperties(sdg);
		final WorkPackage wp = WorkPackage.create(sdg, entries, sdg.getName() + "-alias-sdg", null,
				sumProp.fullyConnectedIds, sumProp.out2in);

		return wp;
	}

	/**
	 * Run once precomputeSummary beforehand. Do before each subsequent invocation.
	 */
	public int recomputeSummary(final IProgressMonitor progress) throws CancelException {
		if (workPack.isFinished()) {
			workPack.reset();
		}

		final int newEdges = SummaryComputation.computeAdjustedAliasDep(workPack, progress);

		return newEdges;
	}

	/**
	 * Do this once before running recompute summary (as long as there are not already precomputed no-alias
	 * dep summary edges in the loaded sdg)
	 */
	public int precomputeSummary(final IProgressMonitor progress) throws CancelException {
		if (workPack.isFinished()) {
			workPack.reset();
		}

		final int newEdges = SummaryComputation.computeNoAliasDataDep(workPack, progress);

		workPack = createWorkPack(sdg);

		return newEdges;
	}

	public int precomputeAllAliasSummary(final IProgressMonitor progress) throws CancelException {
		if (workPack.isFinished()) {
			workPack.reset();
		}

		final int newEdges = SummaryComputation.computeFullAliasDataDep(workPack, progress);

		return newEdges;
	}

	private static class AliasCondition {
		public final IntSet id1;
		public final IntSet id2;

		public AliasCondition(final IntSet id1, final IntSet id2) {
			this.id1 = id1;
			this.id2 = id2;
		}
	}

	private static AliasCondition parseAliasEdge(final SDGEdge e) throws NumberFormatException {
		final String str = e.getLabel();
		assert str != null;
		assert str.charAt(0) == '[';

		final String firstPart = str.substring(1, str.indexOf(']'));
		final String secondPart = str.substring(str.lastIndexOf('[') + 1, str.length() - 1);

		//System.out.print(str + ": '" + firstPart + "' - '" + secondPart + "'");

		final MutableIntSet id1 = new BitVectorIntSet();
		for (final String part : firstPart.split(",")) {
			final int i = Integer.parseInt(part);
			id1.add(i);
		}

		final MutableIntSet id2 = new BitVectorIntSet();
		for (final String part : secondPart.split(",")) {
			final int i = Integer.parseInt(part);
			id2.add(i);
		}

		// System.out.println(" -> " + id1 + " - " + id2);

		return new AliasCondition(id1, id2);
	}

	public boolean setMayAlias(final int nodeId1, final int nodeId2) {
		return mayAlias.add(nodeId1, nodeId2);
	}

	public boolean isMayAlias(final int nodeId1, final int nodeId2) {
		return mayAlias.isSet(nodeId1, nodeId2);
	}

	public boolean setNoAlias(final int nodeId1, final int nodeId2) {
		return noAlias.add(nodeId1, nodeId2);
	}

	public boolean isNoAlias(final int nodeId1, final int nodeId2) {
		return noAlias.isSet(nodeId1, nodeId2);
	}

	public int countEdges(final Kind kind) {
		int count = 0;

		for (final SDGEdge e : sdg.edgeSet()) {
			if (e.getKind() == kind) {
				count++;
			}
		}

		return count;
	}

}
