package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui;

import java.util.List;

public interface DotGraph {

	DotNode getRoot();

	List<DotNode> getNodes();

	String getName();

}
