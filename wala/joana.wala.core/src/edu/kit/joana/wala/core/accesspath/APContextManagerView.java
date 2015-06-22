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
import gnu.trove.set.TIntSet;

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

	public static final class NoAlias {
		
		private final int id1;
		private final int id2;
		
		public NoAlias(final int i1, final int i2) {
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
			
			if (o instanceof NoAlias) {
				final NoAlias noa = (NoAlias) o;
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

}
