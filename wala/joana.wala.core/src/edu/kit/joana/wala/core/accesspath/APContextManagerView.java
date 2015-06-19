/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
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

}
