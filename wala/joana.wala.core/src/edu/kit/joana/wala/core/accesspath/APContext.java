/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.accesspath.APIntraProcV2.MergeOp;
import edu.kit.joana.wala.util.NotImplementedException;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class APContext {
	
	private final PDG pdg;
	
	private final List<MergeOp> merges = new LinkedList<MergeOp>();
	private final Set<AP> paths;
	private final Set<MergeOp> origMerges;
	private final Map<PDGNode, Set<AP>> n2ap;
	private final Map<PDGNode, OrdinalSet<MergeOp>> n2reach;
	private final Set<EQClass> eqClasses = new HashSet<>();

	private APContext(final PDG pdg, final Set<AP> paths, final Set<MergeOp> origMerges,
			final Map<PDGNode, Set<AP>> n2ap, final Map<PDGNode, OrdinalSet<MergeOp>> n2reach) {
		this.pdg = pdg;
		this.paths = paths;
		this.origMerges = origMerges;
		this.n2ap = n2ap;
		this.n2reach = n2reach;
	}
	
	public static APContext create(final PDG pdg, final Set<AP> paths, final Set<MergeOp> origMerges,
			final Map<PDGNode, Set<AP>> n2ap, final Map<PDGNode, OrdinalSet<MergeOp>> n2reach) {
		final APContext ctx = new APContext(pdg, paths, origMerges, n2ap, n2reach);
		ctx.buildEQclasses();
		
		return ctx;
	}
	
	private int eqID = 0;
	
	private int getNextID() {
		return eqID++;
	}
	
	private static class EQClass {
		private final int id;
		private Set<AP> paths;
		
		private EQClass(final int id, final Set<AP> paths) {
			this.id = id;
			this.paths = paths;
		}
		
		public boolean needsMerge(final EQClass other) {
			if (other == this) {
				return false;
			}
			
			for (final AP ap : paths) {
				if (other.paths.contains(ap)) {
					return true;
				}
			}
			
			return false;
		}
		
		public static EQClass merge(final EQClass c1, final EQClass c2, final int newID) {
			final Set<AP> paths = new HashSet<>();
			paths.addAll(c1.paths);
			paths.addAll(c2.paths);
			return new EQClass(newID, paths);
		}
	}
	
	private void addToEqClasses(final MergeOp op) {
		final int id = getNextID();
		final Set<AP> paths = new HashSet<>();
		paths.addAll(op.from);
		paths.addAll(op.to);
		final EQClass cls = new EQClass(id, paths);
		addToEqClasses(cls);
	}

	private void addToEqClasses(final EQClass cls) {
		EQClass toMerge = null;
		for (final EQClass eqc : eqClasses) {
			if (eqc.needsMerge(cls)) {
				toMerge = eqc;
				break;
			}
		}
		
		if (toMerge != null) {
			eqClasses.remove(toMerge);
			final EQClass mergedEQ = EQClass.merge(toMerge, cls, cls.id);
			eqClasses.add(mergedEQ);
		} else {
			eqClasses.add(cls);
		}
	}

	private void buildEQclasses() {
		for (final MergeOp op : origMerges) {
			addToEqClasses(op);
		}
		
		for (final MergeOp op : merges) {
			addToEqClasses(op);
		}
	}
	
	public boolean mayBeActive(final PDGEdge e) {
		if (e.kind != PDGEdge.Kind.DATA_ALIAS) {
			throw new IllegalArgumentException();
		}
		
		return mayBeAliased(e.from, e.to);
	}

	public boolean mayBeAliased(final PDGNode n1, final PDGNode n2) {
		final OrdinalSet<MergeOp> r1 = n2reach.get(n1);
		final OrdinalSet<MergeOp> r2 = n2reach.get(n2);
		
		final OrdinalSet<MergeOp> unified = OrdinalSet.unify(r1, r2);
		
		// get matching context, then check
		
		throw new NotImplementedException();
	}

	public void addMerge(final MergeOp mo) {
		merges.add(mo);
		addToEqClasses(mo);
	}
	
	private String equivalenceClassAP(final AP ap) {
		final StringBuilder sb = new StringBuilder();

		final TIntList matches = new TIntArrayList();
		
		for (final MergeOp mop : origMerges) {
			if (mop.matches(ap)) {
				matches.add(mop.id);
			}
		}
		
		for (final MergeOp mop : merges) {
			if (mop.matches(ap)) {
				matches.add(mop.id);
			}
		}

		if (matches.isEmpty()) {
			return ap.toString();
		} else {
			// wrong build mop -> single id map first!! need to merge equivalence classes in case merge sets differ
			final int[] ids = matches.toArray();
			Arrays.sort(ids);
			for (final int id : ids) {
				sb.append("_" + id);
			}

			return sb.toString();
		}
	}
	
	public boolean isAliased(final AP a1, final AP a2) {
		//TODO lookup merged sub paths
		if (a1.equals(a2)) {
			return true;
		}
		
		final String eqAP1 = equivalenceClassAP(a1);
		final String eqAP2 = equivalenceClassAP(a2);
		
		return eqAP1.equals(eqAP2);
	}
	
}
