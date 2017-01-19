/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * Provides an interface to the artifacts still available after running an SDGBuilder and then "purging" it 
 * (e.g.: removing it's PDGs) for memory reasons.  
 * 
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public interface SDGBuildArtifacts {
	public com.ibm.wala.ipa.callgraph.CallGraph getNonPrunedWalaCallGraph();
	public com.ibm.wala.ipa.callgraph.CallGraph getWalaCallGraph();
	public IClassHierarchy getClassHierarchy();
}
