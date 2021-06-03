package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

public interface IBBlockVisitor {

	void visitStartNode(BasicBlock node);

	void visitExitNode(BasicBlock node);

	void visitStandardNode(BasicBlock node);

	void visitDecisionNode(BasicBlock node);

	void visitDummyNode(BasicBlock node);

}