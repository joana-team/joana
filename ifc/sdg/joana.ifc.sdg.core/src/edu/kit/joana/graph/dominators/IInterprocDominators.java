package edu.kit.joana.graph.dominators;

import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;

public interface IInterprocDominators {

	public abstract void run();

	public abstract Set<VirtualNode> getDominators(VirtualNode n);

	public abstract Set<VirtualNode> getStrictDominators(VirtualNode n);

	public abstract Set<VirtualNode> getImmediateDominators(VirtualNode n);

	public abstract Set<VirtualNode> getDominated(VirtualNode n);

}