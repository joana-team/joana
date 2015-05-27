/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath;

import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.accesspath.APIntraProcV2.MergeInfo;
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
	
	void add(final MergeInfo mnfo) {
		final APContextManager ctx = mnfo.extractContext();
		pdgId2ctx.put(mnfo.pdg.getId(), ctx);
		numOfAliasEdges += mnfo.getNumAliasEdges();
	}
	
	public APContextManager get(final PDG pdg) {
		return pdgId2ctx.get(pdg.getId());
	}

	public int getNumOfAliasEdges() {
		return numOfAliasEdges;
	}
}
