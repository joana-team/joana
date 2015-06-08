/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath;

import edu.kit.joana.wala.core.accesspath.APIntraProcV2.MergeInfo;
import edu.kit.joana.wala.util.NotImplementedException;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * Result of the accesspath and merge info computation. Contains info for each PDG in the SDG.
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class APResult {
	
	private final TIntObjectMap<APContextManager> pdgId2ctx = new TIntObjectHashMap<>();
	private int numOfAliasEdges = 0;
	private final int rootPdgId;
	
	public APResult(final int rootPdgId) {
		this.rootPdgId = rootPdgId;
	}
	
	void add(final MergeInfo mnfo) {
		final APContextManager ctx = mnfo.extractContext();
		pdgId2ctx.put(mnfo.pdg.getId(), ctx);
		numOfAliasEdges += mnfo.getNumAliasEdges();
	}
	
	public void replaceAPsForwardFromRoot() {
		throw new NotImplementedException();
	}
	
	public APContextManager get(final int pdgId) {
		return pdgId2ctx.get(pdgId);
	}

	public int getNumOfAliasEdges() {
		return numOfAliasEdges;
	}
}
