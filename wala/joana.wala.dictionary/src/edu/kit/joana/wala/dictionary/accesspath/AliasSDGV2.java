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
import edu.kit.joana.wala.core.accesspath.APContextManagerView.NoAlias;
import edu.kit.joana.wala.core.accesspath.APResult;
import edu.kit.joana.wala.summary.GraphUtil;
import edu.kit.joana.wala.summary.GraphUtil.SummaryProperties;
import edu.kit.joana.wala.summary.SummaryComputation;
import edu.kit.joana.wala.summary.WorkPackage;
import edu.kit.joana.wala.summary.WorkPackage.EntryPoint;
import edu.kit.joana.wala.util.NotImplementedException;

/**
 * Combines pre-computed SDG with current alias information and prepares a context-aware SDG. 
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class AliasSDGV2 {

	private final SDG sdg;
	private final APResult ap;
	private WorkPackage workPack;
	private final Set<NoAlias> noAlias = new HashSet<>();
	private final List<SDGEdge> currentlyRemoved = new LinkedList<SDGEdge>();

	private AliasSDGV2(final SDG sdg, final WorkPackage workPack, final APResult ap) {
		this.sdg = sdg;
		this.ap = ap;
		this.workPack = workPack;
	}

	public static AliasSDGV2 create(final SDG sdg, final APResult ap) {
		final WorkPackage wp = createWorkPack(sdg);

		return new AliasSDGV2(sdg, wp, ap);
	}

	public static AliasSDGV2 readFrom(final String file, final APResult ap) throws IOException {
		final SDG sdg = SDG.readFrom(file);
		final WorkPackage wp = createWorkPack(sdg);

		return new AliasSDGV2(sdg, wp, ap);
	}

	public static AliasSDGV2 readFrom(final Reader reader, final APResult ap) throws IOException {
		final SDG sdg = SDG.readFrom(reader);
		final WorkPackage wp = createWorkPack(sdg);

		return new AliasSDGV2(sdg, wp, ap);
	}

	public SDG getSDG() {
		return sdg;
	}

	public String getFileName() {
		return sdg.getFileName();
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
			// TODO rewrite propagation
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
					for (final SDGNode call : sdg.getCallers(entry)) {
						propagateAliasesFromCall(call, entry);
						
					}

				}
			}
		}

		throw new NotImplementedException();
	}

	private void propagateAliasesFromCall(final SDGNode call, final SDGNode entry) {

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
					//...
				}
			}
		}
		
		throw new NotImplementedException();
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
		noAlias.clear();
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

	public boolean setNoAlias(final int nodeId1, final int nodeId2) {
		final SDGNode n1 = sdg.getNode(nodeId1);
		final SDGNode n2 = sdg.getNode(nodeId2);
		assert (n1.getProc() == n2.getProc());
		final APContextManagerView ctx = ap.get(n1.getProc());
		System.err.println("TODO set no alias in context manager");
		final NoAlias noa = new NoAlias(nodeId1, nodeId2);
		noAlias.add(noa);
		
		return true;
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
