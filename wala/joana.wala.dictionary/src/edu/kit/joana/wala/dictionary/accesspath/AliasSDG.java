/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.dictionary.accesspath;

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

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.wala.core.accesspath.APContext;
import edu.kit.joana.wala.core.accesspath.APContextManagerView;
import edu.kit.joana.wala.core.accesspath.APContextManagerView.AliasPair;
import edu.kit.joana.wala.core.accesspath.APResult;
import edu.kit.joana.wala.summary.GraphUtil;
import edu.kit.joana.wala.summary.GraphUtil.SummaryProperties;
import edu.kit.joana.wala.summary.SummaryComputation;
import edu.kit.joana.wala.summary.WorkPackage;
import edu.kit.joana.wala.summary.WorkPackage.EntryPoint;
import edu.kit.joana.wala.util.NotImplementedException;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.stack.TIntStack;
import gnu.trove.stack.array.TIntArrayStack;

/**
 * Combines pre-computed SDG with current alias information and prepares a context-aware SDG. 
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class AliasSDG {

	private final SDG sdg;
	private final APResult ap;
	private final Alias mayAlias = new Alias();
	private final Alias noAlias = new Alias();
	private WorkPackage workPack;
	private final List<SDGEdge> currentlyRemoved = new LinkedList<SDGEdge>();

	private AliasSDG(final SDG sdg, final WorkPackage workPack, final APResult ap) {
		this.sdg = sdg;
		this.ap = ap;
		this.workPack = workPack;
	}

	public static AliasSDG create(final SDG sdg, final APResult ap) {
		final WorkPackage wp = createWorkPack(sdg);

		return new AliasSDG(sdg, wp, ap);
	}
	
	public static AliasSDG readFrom(final String file) throws IOException {
		throw new NotImplementedException("need to implement read/write of apresult");
	}

	public static AliasSDG readFrom(final Reader reader) throws IOException {
		throw new NotImplementedException("need to implement read/write of apresult");
	}

	public static AliasSDG readFrom(final String file, final APResult ap) throws IOException {
		final SDG sdg = SDG.readFrom(file);
		final WorkPackage wp = createWorkPack(sdg);

		return new AliasSDG(sdg, wp, ap);
	}

	public static AliasSDG readFrom(final Reader reader, final APResult ap) throws IOException {
		final SDG sdg = SDG.readFrom(reader);
		final WorkPackage wp = createWorkPack(sdg);

		return new AliasSDG(sdg, wp, ap);
	}

	public SDG getSDG() {
		return sdg;
	}

	public String getFileName() {
		return sdg.getFileName();
	}


	/**
	 * Adjusts the internal SDG to the current max alias setting. Returns true iff some edges have changed:
	 * added heap dependencies, removed alias dependencies.
	 * @return number of removed (max) alias dependencies.
	 * @throws CancelException
	 */
	public int adjustMaxSDG(final IProgressMonitor progress) throws CancelException {
		// propagate aliases from starting apcontextmanager
		propagateMaxAliasesFromRoot();
		
		return adjustSDG(progress);
	}
	
	/**
	 * Adjusts the internal SDG to the current min alias setting. Returns true iff some edges have changed:
	 * added heap dependencies, removed alias dependencies.
	 * @return number of removed (max) alias dependencies.
	 * @throws CancelException
	 */
	public int adjustMinSDG(final IProgressMonitor progress) throws CancelException {
		// propagate aliases from starting apcontextmanager
		propagateMinAliasesFromRoot();
		
		return adjustSDG(progress);
	}
	
	/**
	 * Adjusts the internal SDG to the current alias setting. Returns true iff some edges have changed:
	 * added heap dependencies, removed alias dependencies.
	 * @return number of removed (max) alias dependencies.
	 * @throws CancelException
	 */
	private int adjustSDG(final IProgressMonitor progress) throws CancelException {
		final List<SDGEdge> toDisable = new LinkedList<SDGEdge>();
		final Logger debug = Log.getLogger(Log.L_MOJO_DEBUG);
		
		// enable active alias edges and remove inactive ones
		int aliasEdges = 0;

		for (final SDGEdge e : sdg.edgeSet()) {
			if (e.getKind() == SDGEdge.Kind.DATA_ALIAS) {
				MonitorUtil.throwExceptionIfCanceled(progress);
				aliasEdges++;
				if (isNotAliased(e.getSource(), e.getTarget())) {
					toDisable.add(e);
				}
			}
		}

		debug.outln("disabling " + toDisable.size() + " of " + aliasEdges + " alias edges.");

		for (final SDGEdge alias : toDisable) {
			sdg.removeEdge(alias);
			currentlyRemoved.add(alias);
		}

		return toDisable.size();
	}

	private boolean propagateMaxAliasesFromRoot() {
		boolean changed = false;
		
		final APContextManagerView ctxRoot = ap.getRoot();
		changed = ap.propagateMaxInitialContextToCalls(ctxRoot.getPdgId());

		return changed;
	}

	private boolean propagateMinAliasesFromRoot() {
		boolean changed = false;
		
		final APContextManagerView ctxRoot = ap.getRoot();
		changed = ap.propagateMinInitialContextToCalls(ctxRoot.getPdgId());

		return changed;
	}

	private boolean isNotAliased(final SDGNode n1, final SDGNode n2) {
		final APContextManagerView ctxmanag = ap.get(n1.getProc());
		final APContext ctx = ctxmanag.getMatchingContext(n1.getId(), n2.getId());
		
		return !ctx.mayBeAliased(n1, n2);
	}

	/**
	 * Resets all adjustments that were made to the alias sdg. This adds previously disabled edges and
	 * removes all non- and may-aliasing states.
	 */
	public void reset() {
		workPack.reset();
		mayAlias.reset();
		noAlias.reset();
		ap.reset();

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

	public void setNoAlias(final Alias alias) {
		for (final Alias.P p : alias.set) {
			setNoAlias(p.id1, p.id2);
		}
	}
	
	public boolean isNoAlias(final int nodeId1, final int nodeId2) {
		return noAlias.isSet(nodeId1, nodeId2);
	}

	public boolean setNoAlias(final int[] nodeIds) {
		if (nodeIds.length < 2) {
			return false;
		}
		
		boolean changed = false;
		
		final APContextManagerView ctx;
		{
			final SDGNode n = sdg.getNode(nodeIds[0]);
			ctx = ap.get(n.getProc());
		}
		
		for (int i = 0; i < nodeIds.length; i++) {
			final int id1 = nodeIds[i];

			for (int j = i + 1; j < nodeIds.length; j++) {
				final int id2 = nodeIds[j];
				final AliasPair noa = new AliasPair(id1, id2);
				changed |= ctx.addNoAlias(noa);
				noAlias.add(id1, id2);
			}
		}
		
		return changed;
	}
	
	private int[] findReaching(final int nodeId1) {
		final TIntSet reach = new TIntHashSet();
		final LinkedList<Integer> work = new LinkedList<>();
		work.add(nodeId1);
		reach.add(nodeId1);
		
		while (!work.isEmpty()) {
			final int curId = work.removeFirst();
			final SDGNode cur = sdg.getNode(curId);
			
			for (final SDGEdge e : sdg.getOutgoingEdgesOfKind(cur, SDGEdge.Kind.PARAMETER_STRUCTURE)) {
				final int nextId = e.getTarget().getId();
				if (!reach.contains(nextId)) {
					reach.add(nextId);
					work.add(nextId);
				}
			}
		}
		
		return reach.toArray();
	}
	
	public boolean setNoAlias(final int nodeId1, final boolean n1wildcard, final int nodeId2, final boolean n2wildcard) {
		if (!n1wildcard && !n2wildcard) {
			return setNoAlias(nodeId1, nodeId2);
		} else {
			int[] n1arr;
			if (n1wildcard) {
				n1arr =findReaching(nodeId1);
			} else {
				n1arr = new int[] { nodeId1 };
			}

			int[] n2arr;
			if (n2wildcard) {
				n2arr =findReaching(nodeId2);
			} else {
				n2arr = new int[] { nodeId2 };
			}
			
			return setNoAlias(n1arr, n2arr);
		}
	}
	
	public boolean setNoAlias(final int[] n1arr, final int[] n2arr) {
		boolean result = false;
		
		for (final int n1id : n1arr) {
			final SDGNode n1 = sdg.getNode(n1id);
			for (final int n2id : n2arr) {
				final SDGNode n2 = sdg.getNode(n2id);
				if (typesCompatible(n1, n2)) {
					result |= setNoAlias(n1id, n2id);
				}
			}
		}
		
		return result;
	}
	
	private boolean typesCompatible(final SDGNode n1, final SDGNode n2) {
		return ap.typesMayAlias(n1, n2);
	}
	
	public boolean setNoAlias(final int nodeId1, final int nodeId2) {
		final SDGNode n1 = sdg.getNode(nodeId1);
		final SDGNode n2 = sdg.getNode(nodeId2);
		assert (n1.getProc() == n2.getProc());

		final APContextManagerView ctx = ap.get(n1.getProc());
		final AliasPair noa = new AliasPair(nodeId1, nodeId2);
		final boolean changed = ctx.addNoAlias(noa);
		
		noAlias.add(nodeId1, nodeId2);
		
		return changed;
	}
	
	public boolean setMayAlias(final int nodeId1, final int nodeId2) {
		final SDGNode n1 = sdg.getNode(nodeId1);
		final SDGNode n2 = sdg.getNode(nodeId2);
		assert (n1.getProc() == n2.getProc());

		final APContextManagerView ctx = ap.get(n1.getProc());
		final AliasPair noa = new AliasPair(nodeId1, nodeId2);
		final boolean changed = ctx.addMinAlias(noa);
		
		mayAlias.add(nodeId1, nodeId2);
		
		return changed;
	}

	public Alias getMayAlias() {
		return mayAlias.clone();
	}
	
	public Alias getNoAlias() {
		return noAlias.clone();
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

}
