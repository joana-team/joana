/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.accesspath.APIntraProcV2.MergeOp;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

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
	private final Set<CallContext> calls = new HashSet<>(); 
	private Set<NoAlias> noAlias = new HashSet<>();
	private final MutableMapping<MergeOp> mergeMap;

	private APContextManager(final int pdgId, final Set<AP> paths, final Set<MergeOp> origMerges,
			final TIntObjectMap<Set<AP>> n2ap, final TIntObjectMap<OrdinalSet<MergeOp>> n2reach,
			final MutableMapping<MergeOp> mergeMap) {
		this.pdgId = pdgId;
		this.paths = paths;
		this.origMerges = origMerges;
		this.n2ap = n2ap;
		this.n2reach = n2reach;
		this.mergeMap = mergeMap;
		this.baseContext = new APContext(pdgId, n2ap);
	}
	
	public static APContextManager create(final int pdgId, final Set<AP> paths, final Set<MergeOp> origMerges,
			final TIntObjectMap<Set<AP>> n2ap, final TIntObjectMap<OrdinalSet<MergeOp>> n2reach,
			final MutableMapping<MergeOp> mergeMap) {
		final APContextManager manager = new APContextManager(pdgId, paths, origMerges, n2ap, n2reach, mergeMap);

		return manager;
	}
	
	public void addCallContext(final CallContext ctx) {
		calls.add(ctx);
	}
	
	public Set<CallContext> getCallContexts() {
		return Collections.unmodifiableSet(calls);
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

	public int getPdgId() {
		return pdgId;
	}

	public void replaceAPsForCall(final CallContext ctx, final APContextManager callee) {
		final Set<ReplaceAP> toReplace = new HashSet<ReplaceAP>();

		ctx.actualIns.forEach(new TIntProcedure() {
			@Override
			public boolean execute(final int id) {
				final int fin = ctx.act2formal.get(id);
				final Set<AP> aInAPs = n2ap.get(id);
				callee.buildReplaceAPOps(fin, aInAPs, toReplace);
				return true;
			}
		});
		
		callee.executeReplaceAPs(toReplace);
	}
	
	private void executeReplaceAPs(final Set<ReplaceAP> cmds) {
		final Set<AP> newallaps = new HashSet<>();
		final Map<AP, Set<AP>> replaceWith = new HashMap<>();
		for (final ReplaceAP rap : cmds) {
			System.out.println(rap);
			Set<AP> set = replaceWith.get(rap.orig);
			if (set == null) {
				set = new HashSet<>();
				replaceWith.put(rap.orig, set);
			}
			set.add(rap.replacement);
			newallaps.add(rap.replacement);
		}
		
		//extend mapping
		for (final AP orig : paths) {
			for (final AP newap : replaceWith.keySet()) {
				if (!newap.equals(orig) && newap.isSubPathOf(orig)) {
					// add to mapping
					final List<ParameterField> subp = orig.getSubPathFrom(newap);
					final Set<AP> newrepl = replaceWith.get(newap);
					Set<AP> extrepl = replaceWith.get(orig);
					if (extrepl == null) {
						extrepl = new HashSet<>();
						replaceWith.put(orig, extrepl);
					}
					for (final AP newrepap : newrepl) {
						final AP extap = newrepap.append(subp);
						newallaps.add(extap);
						extrepl.add(extap);
					}
				}
			}
		}
		
		// execute replacement
		final TIntSet keys = n2ap.keySet();
		for (final TIntIterator it = keys.iterator(); it.hasNext(); ){
			final int cur = it.next();
			final Set<AP> old = n2ap.get(cur);
			final Set<AP> newaps = new HashSet<>();
			for (final AP o : old) {
				final Set<AP> repl = replaceWith.get(o);
				if (repl != null) {
					newaps.addAll(repl);
				} else {
					newaps.add(o);
				}
			}
			n2ap.put(cur, newaps);
		}
		
		// replace orig merges
		final Set<MergeOp> newMerges = new HashSet<>();
		for (final MergeOp mop : origMerges) {
			final Set<AP> from = new HashSet<AP>();
			for (final AP ap : mop.from) {
				final Set<AP> repl = replaceWith.get(ap);
				if (repl != null) {
					from.addAll(repl);
				} else {
					from.add(ap);
				}
			}
			final Set<AP> to = new HashSet<AP>();
			for (final AP ap : mop.to) {
				final Set<AP> repl = replaceWith.get(ap);
				if (repl != null) {
					to.addAll(repl);
				} else {
					to.add(ap);
				}
			}
			
			final MergeOp newOP = new MergeOp(from, to);
			newOP.id = mop.id;
			newMerges.add(newOP);
			mergeMap.replace(mop, newOP);
		}
		
		origMerges.clear();
		origMerges.addAll(newMerges);
	}

	protected void buildReplaceAPOps(final int fin, final Set<AP> aInAPs, final Set<ReplaceAP> toReplace) {
		// replace mapping of fin to APs and rewrite all merge-ops that contain original aps with new ones.
		final Set<AP> oldAPs = n2ap.get(fin);
		for (final AP old : oldAPs) {
			for (final AP newap : aInAPs) {
				if (!old.equals(newap)) {
					final ReplaceAP replace = new ReplaceAP(old, newap);
					toReplace.add(replace);
				}
			}
		}
	}
	
	public class ReplaceAP {
		public final AP orig;
		public final AP replacement;
		
		public ReplaceAP(final AP orig, final AP replacement) {
			this.orig = orig;
			this.replacement = replacement;
		}
		
		public int hashCode() {
			return orig.hashCode() + 2711 * replacement.hashCode();
		}
		
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			
			if (o instanceof ReplaceAP) {
				final ReplaceAP other = (ReplaceAP) o;
				
				return orig.equals(other.orig) && replacement.equals(other.replacement);
			}
			
			return false;
		}
		
		public String toString() {
			return "replace " + orig + " with " + replacement;
		}
	}
	
}
