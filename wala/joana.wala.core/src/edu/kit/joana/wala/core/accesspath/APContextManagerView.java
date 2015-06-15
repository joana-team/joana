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
import edu.kit.joana.wala.core.accesspath.APIntraprocContextManager.CallContext;
import edu.kit.joana.wala.core.accesspath.APIntraProcV2.MergeOp;
import gnu.trove.set.TIntSet;

/**
 * Interface for the intra and interprocedural implementations of the context manager.
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public interface APContextManagerView {

	public Set<AP> getAllPaths();
	public OrdinalSet<MergeOp> getReachingMerges(final int id);
	public TIntSet getAllMappedNodes();
	public Set<AP> getAccessPaths(final int pdgNodeId);
	public Set<CallContext> getCallContexts();
	public int getPdgId();
	public Set<MergeOp> getOrigMerges();
	public APContext getMatchingContext(final SDGEdge e);
	public APContext getMatchingContext(final int n1Id, final int n2Id);

}
