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
import edu.kit.joana.wala.core.accesspath.APIntraprocContextManager.CallContext;
import gnu.trove.set.TIntSet;

/**
 * Interprocedural adjusted access path context for a specific method.
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class APInterprocContextManager implements APContextManagerView {
	
	private final APIntraprocContextManager intra;
	
	public APInterprocContextManager(final APIntraprocContextManager intra) {
		this.intra = intra;
	}

	@Override
	public Set<AP> getAllPaths() {
		return intra.getAllPaths();
	}

	@Override
	public OrdinalSet<MergeOp> getReachingMerges(final int id) {
		return intra.getReachingMerges(id);
	}

	@Override
	public TIntSet getAllMappedNodes() {
		return intra.getAllMappedNodes();
	}

	@Override
	public Set<AP> getAccessPaths(final int pdgNodeId) {
		return intra.getAccessPaths(pdgNodeId);
	}

	@Override
	public Set<CallContext> getCallContexts() {
		return intra.getCallContexts();
	}

	@Override
	public int getPdgId() {
		return intra.getPdgId();
	}

	@Override
	public Set<MergeOp> getOrigMerges() {
		return intra.getOrigMerges();
	}

	@Override
	public APContext getMatchingContext(final SDGEdge e) {
		return intra.getMatchingContext(e);
	}

	@Override
	public APContext getMatchingContext(final int n1Id, final int n2Id) {
		return intra.getMatchingContext(n1Id, n2Id);
	}

}
