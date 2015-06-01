/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath;

import java.util.Set;

import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.wala.core.accesspath.APIntraProcV2.MergeOp;
import gnu.trove.map.TIntObjectMap;

/**
 * Manages and prepares the various context configurations of a single method/pdg.
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class APContextManager {
	
	private final int pdgId;
	// set of all access paths relevant to this method context
	private final Set<AP> paths;
	private Set<MergeOp> initialAlias; // merges/alias context at method entry
	private final Set<MergeOp> origMerges; // all merges that occur naturally during execution
	// map of pdg node id to its access paths
	private final TIntObjectMap<Set<AP>> n2ap;
	// map that records for each node id which origMerges may affect it
	private final TIntObjectMap<OrdinalSet<MergeOp>> n2reach;
	private APContext baseContext;

	private APContextManager(final int pdgId, final Set<AP> paths, final Set<MergeOp> origMerges,
			final TIntObjectMap<Set<AP>> n2ap, final TIntObjectMap<OrdinalSet<MergeOp>> n2reach) {
		this.pdgId = pdgId;
		this.paths = paths;
		this.origMerges = origMerges;
		this.n2ap = n2ap;
		this.n2reach = n2reach;
		this.baseContext = new APContext(pdgId, n2ap);
	}
	
	public static APContextManager create(final int pdgId, final Set<AP> paths, final Set<MergeOp> origMerges,
			final TIntObjectMap<Set<AP>> n2ap, final TIntObjectMap<OrdinalSet<MergeOp>> n2reach) {
		final APContextManager manager = new APContextManager(pdgId, paths, origMerges, n2ap, n2reach);

		return manager;
	}
	
	public void setInitialAlias(final Set<MergeOp> initialAliasing) {
		this.initialAlias = initialAliasing;
		this.baseContext = new APContext(pdgId, n2ap);
		for (final MergeOp mop : initialAliasing) {
			this.baseContext.addMerge(mop);
		}
	}
	
	public APContext getMatchingContext(final SDGEdge e) {
		return getMatchingContext(e.getSource().getId(), e.getTarget().getId());
	}
	
	public APContext getMatchingContext(final int n1Id, final int n2Id) {
		final OrdinalSet<MergeOp> r1 = n2reach.get(n1Id);
		final OrdinalSet<MergeOp> r2 = n2reach.get(n2Id);
		
		final OrdinalSet<MergeOp> unified = OrdinalSet.unify(r1, r2);

		if (unified.isEmpty()) {
			return baseContext;
		} else {
			final APContext ctx = baseContext.clone();
			for (final MergeOp mop : unified) {
				ctx.addMerge(mop);
			}
			
			return ctx;
		}
	}
	
	public Set<AP> getAccessPaths(final int pdgNodeId) {
		return n2ap.get(pdgNodeId);
	}
	
}
