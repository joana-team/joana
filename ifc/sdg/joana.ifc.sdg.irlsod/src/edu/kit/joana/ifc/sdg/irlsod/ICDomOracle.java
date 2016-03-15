package edu.kit.joana.ifc.sdg.irlsod;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;

/**
 * Let G = (N,E) be a threaded interprocedural control-flow graph. A cdom oracle
 * is a symmetric function N \times N \to N s.t. cdom(n,m) context-sensitively
 * dominates both n and m and does not happen in parallel to any of the two. A
 * node x context-sensitively dominates a node y iff x is contained on every
 * realizable path from start to y
 * <p>
 * Note that a classical dominator is also a context-sensitive dominator, so it
 * is perfectly okay if for example an implementation of this interface computes
 * lowest common ancestors on a variant of the classical dominator tree, which
 * ensures x dom y ==> not (MHP(x,y)).
 * <p>
 *
 * @author Martin Mohr&lt;martin.mohr@kit.edu&gt;
 */
public interface ICDomOracle {
	VirtualNode cdom(SDGNode n1, int threadN1, SDGNode n2, int threadN2);
}