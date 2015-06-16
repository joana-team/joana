/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.util.intset.MutableMapping;

import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.accesspath.APIntraProcV2.MergeOp;
import edu.kit.joana.wala.core.accesspath.APIntraprocContextManager.CallContext;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;

/**
 * Contains utils to transform and replace access paths.
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public final class APReplacer {
	
	private final APIntraprocContextManager ctx;
	private Set<AP> newallaps;
	private Map<AP, Set<AP>> replaceWith;
	
	private Set<ReplaceAP> current = new HashSet<>();
	private boolean changed = false;

	public APReplacer(final APIntraprocContextManager ctx) {
		this.ctx = ctx;
	}
	
	public boolean isChanged() {
		return changed;
	}
	
	public final class ReplaceAP {
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
	
	
	public void executeReplaceAPs() {
		newallaps = new HashSet<>();
		replaceWith = new HashMap<>();
		
		for (final ReplaceAP rap : current) {
			//System.out.println(rap);
			Set<AP> set = replaceWith.get(rap.orig);
			if (set == null) {
				set = new HashSet<>();
				replaceWith.put(rap.orig, set);
			}
			set.add(rap.replacement);
			newallaps.add(rap.replacement);
		}
		
		// extend mapping
		for (final AP orig : ctx.getAllPaths()) {
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
		ctx.triggerReplaceMapping(this);
		
		// replace orig merges
		ctx.triggerReplaceMerges(this);

		// replace all aps
		ctx.triggerReplaceAllAPs(this);
		
		// cleanup
		newallaps = null;
		replaceWith = null;
		current.clear();
		changed = false;
	}

	public void replaceAllAPs(final Set<AP> paths) {
		paths.clear();
		paths.addAll(newallaps);
	}
	
	public void replaceMerges(final Set<MergeOp> origMerges, final MutableMapping<MergeOp> mergeMap) {
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
	
	public void replaceMapping(final TIntObjectMap<Set<AP>> n2ap) {
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
	}
	
	public void buildReplaceAPsForCall(final CallContext call, final APIntraprocContextManager caller) {
		if (call.calleeId != ctx.pdgId) {
			throw new IllegalArgumentException("this callsite '" + call + "' does not call the current method ("
					+ ctx.pdgId + ")");
		}
		
		call.actualIns.forEach(new TIntProcedure() {
			@Override
			public boolean execute(final int id) {
				final int fin = call.act2formal.get(id);
				final Set<AP> aInAPs = caller.getAccessPaths(id);
				buildReplaceAPOps(fin, aInAPs);
				return true;
			}
		});
	}
	
	private void buildReplaceAPOps(final int fin, final Set<AP> aInAPs) {
		// replace mapping of fin to APs and rewrite all merge-ops that contain original aps with new ones.
		final Set<AP> oldAPs = ctx.getAccessPaths(fin);
		for (final AP old : oldAPs) {
			for (final AP newap : aInAPs) {
				if (!old.equals(newap)) {
					addReplaceAP(old, newap);
				}
			}
		}
	}

	private void addReplaceAP(final AP orig, final AP replacement) {
		final ReplaceAP rap = new ReplaceAP(orig, replacement);
		
		if (!current.contains(rap)) {
			current.add(rap);
			changed = true;
		}
	}
	
	private Set<AP> transcode(final AP orig) {
		final Set<AP> tr = new HashSet<>();

		for (final ReplaceAP rap : current) {
			// execute replace
			if (rap.orig.equals(orig)) {
				tr.add(rap.replacement);
			} else if (rap.orig.isSubPathOf(orig)) {
				final List<ParameterField> subp = orig.getSubPathFrom(rap.orig);
				final AP extap = rap.replacement.append(subp);
				tr.add(extap);
			}
		}
		
		if (tr.isEmpty()) {
			tr.add(orig);
		}
		
		return tr;
	}

	@SuppressWarnings("unused")
	private Set<AP> transcode(final Set<AP> orig) {
		if (current.isEmpty()) {
			return orig;
		}
		
		final Set<AP> tr = new HashSet<>();
		
		for (final AP origap : orig) {
			final Set<AP> trAP = transcode(origap);
			tr.addAll(trAP);
		}
		
		return tr;
	}

}
