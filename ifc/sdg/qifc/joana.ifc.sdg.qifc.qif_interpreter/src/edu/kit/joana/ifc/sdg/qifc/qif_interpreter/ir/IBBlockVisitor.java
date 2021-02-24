package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

public interface IBBlockVisitor {

	void visitStartNode(BBlock node);

	void visitExitNode(BBlock node);

	void visitStandardNode(BBlock node);

	void visitDecisionNode(BBlock node);

	void visitDummyNode(BBlock node);

}
