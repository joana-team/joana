/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.wala.core.accesspath.APIntraProcV2.MergeOp;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * Manages and prepares the various context configurations of a single method/pdg.
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class APIntraprocContextManager implements APContextManagerView {
	
	public final String pdgName;
	public final int pdgId;
	// set of all access paths relevant to this method context
	private final Set<AP> paths;
	private Set<MergeOp> initialAlias; // merges/alias context at method entry
	private final Set<MergeOp> origMerges; // all merges that occur naturally during execution
	// map of pdg node id to its access paths
	private final TIntObjectMap<Set<AP>> n2ap;
	// map that records for each node id which origMerges may affect it
	private final TIntObjectMap<OrdinalSet<MergeOp>> n2reach;
	private APContext baseContext;
	private final Set<CallContext> calls = new HashSet<>(); 
	private final MutableMapping<MergeOp> mergeMap;
	private final Set<MergeOp> maxMerges;
	private final Set<NoMerge> noAlias = new HashSet<>();

	private APIntraprocContextManager(final String pdgName, final int pdgId, final Set<AP> paths, final Set<MergeOp> origMerges,
			final TIntObjectMap<Set<AP>> n2ap, final Set<MergeOp> maxMerges, final TIntObjectMap<OrdinalSet<MergeOp>> n2reach,
			final MutableMapping<MergeOp> mergeMap) {
		this.pdgName = pdgName;
		this.pdgId = pdgId;
		this.paths = paths;
		this.origMerges = origMerges;
		this.n2ap = n2ap;
		this.maxMerges = maxMerges;
		this.n2reach = n2reach;
		this.mergeMap = mergeMap;
		this.baseContext = new APContext(pdgId, n2ap);
	}
	
	public static APIntraprocContextManager create(final String pdgName, final int pdgId, final Set<AP> paths,
			final Set<MergeOp> origMerges, final TIntObjectMap<Set<AP>> n2ap, final Set<MergeOp> maxMerges,
			final TIntObjectMap<OrdinalSet<MergeOp>> n2reach, final MutableMapping<MergeOp> mergeMap) {
		final APIntraprocContextManager manager = new APIntraprocContextManager(pdgName, pdgId, paths, origMerges, n2ap, maxMerges, n2reach, mergeMap);

		return manager;
	}
	
	private static class NoMerge {
		
		public final String ap1str;
		public final String ap2str;
		public final AP ap1;
		public final AP ap2;
		
		public NoMerge(final AP ap1, final AP ap2) {
			final String a1 = flatten(ap1);
			final String a2 = flatten(ap2);
			if (a1.compareTo(a2) < 0) {
				this.ap1str = a1;
				this.ap2str = a2;
				this.ap1 = ap1;
				this.ap2 = ap2;
			} else {
				this.ap1str = a2;
				this.ap2str = a1;
				this.ap1 = ap2;
				this.ap2 = ap1;
			}
		}
		
		public static String flatten(final AP ap) {
			return ap.toString() + ".";
		}
		
		public static boolean prefixMatch(final String s1, final String s2) {
			return s1.startsWith(s2) || s2.startsWith(s1);
		}
		
		public boolean violates(final MergeOp mop) {
			boolean vio11 = false;
			boolean vio12 = false;
			
			for (final AP a1 : mop.from) {
				final String a1flat = flatten(a1);
				if (prefixMatch(a1flat, ap1str)) {
					vio11 = true;
				} else if (prefixMatch(a1flat, ap2str)) {
					vio12 = true;
				}
				
				if (vio11 && vio12) break;
			}
			
			if (!(vio11 || vio12)) {
				return false;
			}
			
			for (final AP a2 : mop.to) {
				final String a2flat = flatten(a2);
				if (vio12 && prefixMatch(a2flat, ap1str)) {
					return true;
				} else if (vio11 && prefixMatch(a2flat, ap2str)) {
					return true;
				}
			}

			return false;
		}
		
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			
			if (o instanceof NoMerge) {
				final NoMerge nom = (NoMerge) o;
				return ap1str.equals(nom.ap1str) && ap2str.equals(nom.ap2str);
			}
			
			return false;
		}
		
		public int hashCode() {
			return ap1str.hashCode() + 4711 * ap2str.hashCode();
		}
	}
	
	/**
	 * Compute the maximal set of initial merge operations that do not violate the previously set no-alias options.
	 */
	public Set<MergeOp> computeMaxInitialContext() {
		final Set<MergeOp> ok = new HashSet<>();
		
		for (final MergeOp mop : maxMerges) {
			final NoMerge nm = findViolatingNoAlias(mop);
			
			if (nm != null) {
				final Set<MergeOp> splitted = splitUp(nm, mop);
				ok.addAll(splitted);
			} else {
				ok.add(mop);
			}
		}

		return ok;
	}
	
	private Set<MergeOp> splitUp(final NoMerge nm, final MergeOp mop) {
		final Set<MergeOp> split = new HashSet<>();

		final Set<AP> okFrom = new HashSet<>();
		final Set<AP> okTo = new HashSet<>();
		final Set<AP> vioFrom = new HashSet<>();
		final Set<AP> vioTo = new HashSet<>();
		
		for (final AP ap : mop.from) {
			final String apStr = NoMerge.flatten(ap);
			if (NoMerge.prefixMatch(apStr, nm.ap1str)) {
				vioFrom.add(ap);
			} else if (NoMerge.prefixMatch(apStr, nm.ap2str)) {
				vioFrom.add(ap);
			} else {
				okFrom.add(ap);
			}
		}
		
		for (final AP ap : mop.to) {
			final String apStr = NoMerge.flatten(ap);
			if (NoMerge.prefixMatch(apStr, nm.ap1str)) {
				vioTo.add(ap);
			} else if (NoMerge.prefixMatch(apStr, nm.ap2str)) {
				vioTo.add(ap);
			} else {
				okTo.add(ap);
			}
		}

		if (!okTo.isEmpty() && !okFrom.isEmpty()) {
			final MergeOp op1ok = new MergeOp(okFrom, okTo);
			split.add(op1ok);
		}
		
		return split;
	}
	
	private NoMerge findViolatingNoAlias(final MergeOp mop) {
		for (final NoMerge nm : noAlias) {
			if (nm.violates(mop)) {
				return nm;
			}
		}
		
		return null;
	}
	
	public boolean addNoAlias(final NoAlias noa) {
		boolean changed = false;
		
		final int id1 = noa.getId1();
		final Set<AP> ap1 = n2ap.get(id1);
		final int id2 = noa.getId2();
		final Set<AP> ap2 = n2ap.get(id2);
		for (final AP a1 : ap1) {
			for (final AP a2 : ap2) {
				final NoMerge nm = new NoMerge(a1, a2);
				changed |= noAlias.add(nm);
			}
		}
		
		return changed;
	}
	
	public String toString() {
		return "context manager of " + pdgName + " (" + pdgId + ")";
	}
	
	public void addCallContext(final CallContext ctx) {
		calls.add(ctx);
	}
	
	public Set<CallContext> getCallContexts() {
		return Collections.unmodifiableSet(calls);
	}
	
	public TIntSet getCalledMethods() {
		final TIntSet called = new TIntHashSet();
		
		for (final CallContext call : calls) {
			called.add(call.calleeId);
		}
		
		return called;
	}
	
	public APContext getMatchingContext(final CallContext call) {
		final OrdinalSet<MergeOp> rcall = n2reach.get(call.callSite);

		final APContext callCtx = baseContext.clone();
		for (final MergeOp mo : rcall) {
			callCtx.addMerge(mo);
		}
		
		return callCtx;
	}
	
	public APContext computeContextForAllCallsTo(final APIntraprocContextManager callee) {
		final APContext ctx = callee.baseContext.clone();
		
		for (final CallContext call : calls) {
			if (call.calleeId == callee.pdgId) {
				// add this context to end result
				final APContext callerCtx = getMatchingContext(call);
				mergeAdditionalCallContext(callerCtx, call, ctx);
			}
		}
		
		return ctx;
	}
	
	/**
	 * Translate the APContext of a callsite of this method to a matching method local context.
	 * @param caller The context at the callsite
	 * @param call Description of the call with mapping between actual and formal parameters
	 * @return An APContext with access paths relative to this methods parameters.
	 */
	public APContext translateToLocalContext(final APContext caller, final CallContext call) {
		final APContext ctx = baseContext.clone();
		
		mergeAdditionalCallContext(caller, call, ctx);
		
		return ctx;
	}
	
	public void mergeAdditionalCallContext(final APContext caller, final CallContext call, final APContext toMerge) {
		final int[] actIns = call.actualIns.toArray();
		
		for (int i = 0; i < actIns.length; i++) {
			final int a1 = actIns[i];
			final int f1 = call.act2formal.get(a1);
			
			for (int j = i+1; j < actIns.length; j++) {
				final int a2 = actIns[j];
				
				if (caller.mayBeAliased(a1, a2)) {
					// add alias to matching formal-ins
					final int f2 = call.act2formal.get(a2);
					toMerge.addMerge(f1, f2);
				}
			}
		}
	}
	
	public void setInitialAlias(final Set<MergeOp> initialAliasing) {
		this.initialAlias = initialAliasing;
		this.baseContext = new APContext(pdgId, n2ap);
		for (final MergeOp mop : initialAliasing) {
			this.baseContext.addMerge(mop);
		}
	}
	
	public Set<MergeOp> getInitialAlias() {
		return Collections.unmodifiableSet(initialAlias);
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
	
	public TIntSet getAllMappedNodes() {
		return n2ap.keySet();
	}
	
	public Set<MergeOp> getOrigMerges() {
		return Collections.unmodifiableSet(origMerges);
	}
	
	public Set<AP> getAllPaths() {
		return Collections.unmodifiableSet(paths);
	}
	
	public OrdinalSet<MergeOp> getReachingMerges(final int id) {
		return n2reach.get(id);
	}
	
	public static final class CallContext {
		final int callId;
		final int calleeId;
		final int callSite;
		final TIntSet actualIns = new TIntHashSet();
		final TIntIntMap act2formal = new TIntIntHashMap();
		
		public CallContext(final int callId, final int calleeId, final int callSite) {
			this.callId = callId;
			this.calleeId = calleeId;
			this.callSite = callSite;
		}
		
		public boolean equals(final Object o) {
			if (o == this) {
				return true;
			}
			
			if (o instanceof CallContext) {
				final CallContext other = (CallContext) o;
				return callId == other.callId && calleeId == other.calleeId && callSite == other.callSite;
			}
			
			return false;
		}
		
		public String toString() {
			return "call-ctx " + callId + " to " + calleeId + " at " + callSite; 
		}
		
		public int hashCode() {
			return callId + 7 * calleeId + 23 * callSite;
		}
	}
	
	public int getPdgId() {
		return pdgId;
	}

	public final void triggerReplaceMapping(final APReplacer rep) {
		rep.replaceMapping(n2ap);
	}
	
	public final void triggerReplaceMerges(final APReplacer rep) {
		rep.replaceMerges(origMerges, mergeMap);
	}
	
	public final void triggerReplaceAllAPs(final APReplacer rep) {
		rep.replaceAllAPs(paths);
	}
}
