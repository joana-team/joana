package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;

public interface IBBlockVisitor {

	void visitStartNode(BBlock node);

	void visitStandardNode(BBlock node);

	void visitDecisionNode(BBlock node);

	void visitDummyNode(BBlock node);

}
