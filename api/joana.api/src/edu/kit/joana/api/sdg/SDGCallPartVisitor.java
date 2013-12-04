package edu.kit.joana.api.sdg;

public interface SDGCallPartVisitor {
	void visitCallInstruction(SDGCall ci);
	void visitActualParameter(SDGActualParameter ap);
	void visitReturnNode(SDGCallReturnNode rn);
	void visitExceptionNode(SDGCallExceptionNode en);
}
