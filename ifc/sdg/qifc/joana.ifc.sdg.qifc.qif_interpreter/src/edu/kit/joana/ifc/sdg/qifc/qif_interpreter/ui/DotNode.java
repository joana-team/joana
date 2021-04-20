package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui;

import java.util.List;

public interface DotNode {

	String getLabel();

	List<DotNode> getSuccs();

	List<DotNode> getPreds();

	boolean isExceptionEdge(DotNode succ);

	int getId();
}
