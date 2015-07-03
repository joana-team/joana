/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * Interface for the intra and interprocedural implementations of the context manager.
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public interface APContextManagerView {

	public TIntSet getAllMappedNodes();
	public TIntSet getCalledMethods();
	public int getPdgId();
	public APContext getMatchingContext(final SDGEdge e);
	public APContext getMatchingContext(final int n1Id, final int n2Id);
	public boolean addNoAlias(final AliasPair noa);
	public boolean addMinAlias(final AliasPair noa);
	public void reset();

	public static final class AliasPair {
		
		// ids of pdg nodes (formal-in nodes) that should (or shoud not) be aliased
		private final int id1;
		private final int id2;
		
		public AliasPair(final int i1, final int i2) {
			if (i1 < i2) {
				id1 = i1;
				id2 = i2;
			} else {
				id1 = i2;
				id2 = i1;
			}
		}
		
		public boolean captures(final SDGNode n1, final SDGNode n2) {
			return captures(n1.getId(), n2.getId());
		}
		
		public boolean captures(final int i1, final int i2) {
			return ((i1 < i2) ? (id1 == i1 && id2 == i2) : (id1 == i2 && id2 == i1)); 
		}
		
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			
			if (o instanceof AliasPair) {
				final AliasPair noa = (AliasPair) o;
				return id1 == noa.id1 && id2 == noa.id2;
			}
			
			return false;
		}
		
		public String toString() {
			return "noalias(" + id1 + "," + id2 + ")";
		}
		
		public int hashCode() {
			return id1 + 47 * id2;
		}

		public int getId1() {
			return id1;
		}

		public int getId2() {
			return id2;
		}
	}

	public static final class CallContext {
		public final int callId;
		public final int calleeId;
		public final int callSite;
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

}
