package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui;

import java.util.Set;

public interface DotGraph {

	DotNode getRoot();

	Set<DotNode> getNodes();

	String getName();

}