/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath;

import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.wala.core.accesspath.AP.RootNode;
import edu.kit.joana.wala.core.accesspath.APIntraProcV2.MergeOp;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.set.TIntSet;

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
	private Set<NoAlias> noAlias = new HashSet<>();

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
	
	public void setInitialNoAlias(final Set<NoAlias> noAlias) {
		this.noAlias = noAlias;
		this.baseContext.setInitialNoAlias(noAlias);
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
	
	public static final class CallContext {
		int callId;
		int calleeId;
		TIntSet actualIns;
		TIntSet act2formal;
	}
	
	public static final class NoAlias {
		
		// the lexically smaller string is always stored in ap1
		public final String ap1;
		public final String ap2;
		
		public NoAlias(final AP ap1, final AP ap2) {
			// add '.' so fields with same prefix are not accidentally matched. e.g. ("p1.f2" starts with "p1.f")
			final String s1 = ap1.toString() + ".";
			final String s2 = ap2.toString() + ".";
			if (s1.compareTo(s2) <= 0) {
				this.ap1 = s1;
				this.ap2 = s2;
			} else {
				this.ap1 = s2;
				this.ap2 = s1;
			}
		}
		
		public boolean captures(final AP p1, final AP p2) {
			final String s1 = p1.toString() + ".";
			final String s2 = p2.toString() + ".";
			
			if (s1.compareTo(s2) <= 0) {
				return s1.startsWith(ap1) && s2.startsWith(ap2);
			} else {
				return s1.startsWith(ap2) && s2.startsWith(ap1);
			}
		}
		
		public int hashCode() {
			return ap1.hashCode() + ap2.hashCode();
		}
		
		public boolean equals(final Object o) {
			if (o == this) {
				return true;
			}
			
			if (o instanceof NoAlias) {
				final NoAlias na = (NoAlias) o;
				return na.ap1.equals(ap1) && na.ap2.equals(ap2);
			}
			
			return false;
		}
	}
	
}
