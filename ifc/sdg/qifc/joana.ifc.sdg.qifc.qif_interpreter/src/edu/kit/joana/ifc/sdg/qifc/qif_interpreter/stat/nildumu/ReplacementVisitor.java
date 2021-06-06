package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import nildumu.Parser;

import java.util.List;

public class ReplacementVisitor implements Parser.StatementVisitor<Parser.StatementNode> {

	String var;
	Method m;

	public ReplacementVisitor(String var, Method m) {
		this.var = var;
		this.m = m;
	}

	@Override public Parser.StatementNode visit(Parser.StatementNode statementNode) {
		return statementNode;
	}

	@Override public Parser.StatementNode visit(Parser.VariableAssignmentNode assignment) {
		return assignment;
	}

	@Override public Parser.StatementNode visit(Parser.MultipleVariableAssignmentNode assignment) {
		return assignment;
	}

	@Override public Parser.StatementNode visit(Parser.VariableDeclarationNode variableDeclaration) {
		return variableDeclaration;
	}

	@Override public Parser.StatementNode visit(Parser.ArrayAssignmentNode variableDeclaration) {
		return variableDeclaration;
	}

	@Override public Parser.StatementNode visit(Parser.OutputVariableDeclarationNode outputDecl) {
		return outputDecl;
	}

	@Override public Parser.StatementNode visit(Parser.AppendOnlyVariableDeclarationNode appendDecl) {
		return appendDecl;
	}

	@Override public Parser.StatementNode visit(Parser.InputVariableDeclarationNode inputDecl) {
		return inputDecl;
	}

	@Override public Parser.StatementNode visit(Parser.TmpInputVariableDeclarationNode inputDecl) {
		return inputDecl;
	}

	@Override public Parser.StatementNode visit(Parser.BlockNode block) {
		return new Parser.BlockNode(block.location, visitChildren(block));
	}

	@Override public Parser.StatementNode visit(Parser.ConditionalStatementNode condStatement) {
		return condStatement;
	}

	@Override public Parser.StatementNode visit(Parser.IfStatementNode ifStatement) {
		return new Parser.IfStatementNode(ifStatement.location, ifStatement.conditionalExpression,
				visit(ifStatement.ifBlock), (ifStatement.elseBlock == null) ?
				new Parser.EmptyStatementNode(ifStatement.location) :
				visit(ifStatement.elseBlock));
	}

	@Override public Parser.StatementNode visit(Parser.IfStatementEndNode ifEndStatement) {
		return ifEndStatement;
	}

	@Override public Parser.StatementNode visit(Parser.WhileStatementNode whileStatement) {
		return new Parser.WhileStatementNode(whileStatement.location, whileStatement.getPreCondVarAss(),
				whileStatement.conditionalExpression, visit(whileStatement.body));
	}

	@Override public Parser.StatementNode visit(Parser.WhileStatementEndNode whileEndStatement) {
		return whileEndStatement;
	}

	@Override public Parser.StatementNode visit(Parser.LoopInterruptionNode loopInterruptionNode) {
		return loopInterruptionNode;
	}

	@Override public Parser.StatementNode visit(Parser.ExpressionStatementNode expressionStatement) {
		return expressionStatement;
	}

	@Override public Parser.StatementNode visit(Parser.ReturnStatementNode returnStatement) {
		return new Parser.VariableAssignmentNode(Converter.DUMMY_LOCATION, this.var, returnStatement.expression);
	}

	@Override public List<Parser.StatementNode> visitChildren(Parser.MJNode node) {
		return Parser.StatementVisitor.super.visitChildren(node);
	}

	@Override public void visitChildrenDiscardReturn(Parser.MJNode node) {
		Parser.StatementVisitor.super.visitChildrenDiscardReturn(node);
	}
}