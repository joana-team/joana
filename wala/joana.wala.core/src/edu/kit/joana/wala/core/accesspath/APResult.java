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
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class APResult {
	
	private final TIntObjectMap<MergeInfo> pdgId2info = new TIntObjectHashMap<MergeInfo>();
	private int numOfAliasEdges = 0;

	public void add(final MergeInfo mnfo) {
		pdgId2info.put(mnfo.pdg.getId(), mnfo);
		numOfAliasEdges += mnfo.getNumAliasEdges();
	}
	
	public MergeInfo get(final PDG pdg) {
		return pdgId2info.get(pdg.getId());
	}
	
	public int getNumOfAliasEdges() {
		return numOfAliasEdges;
	}
}
