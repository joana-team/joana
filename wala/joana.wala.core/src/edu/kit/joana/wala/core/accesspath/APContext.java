/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.accesspath.APIntraProcV2.MergeInfo;
import edu.kit.joana.wala.core.accesspath.APIntraProcV2.MergeOp;
import edu.kit.joana.wala.util.NotImplementedException;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class APContext {
	
	private final PDG pdg;
	
	private final List<MergeOp> merges = new LinkedList<MergeOp>();
	private final Set<AP> paths = new HashSet<AP>();
	
	
	public APContext(final PDG pdg) {
		this.pdg = pdg;
	}
	
	public void read(final MergeInfo nfo) {
		throw new NotImplementedException();
	}
	
	public boolean mayBeActive(final PDGEdge e) {
		if (e.kind != PDGEdge.Kind.DATA_ALIAS) {
			throw new IllegalArgumentException();
		}
		
		return mayBeAliased(e.from, e.to);
	}

	public boolean mayBeAliased(final PDGNode n1, final PDGNode n2) {
		throw new NotImplementedException();
	}

	public void addMerge(final MergeOp mo) {
	}
	
	public boolean isAliased(final AP a1, final AP a2) {
		//TODO lookup merged sub paths
		if (a1.equals(a2)) {
			return true;
		}
		
		return false;
	}
	
}
