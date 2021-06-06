package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import nildumu.Parser;

import java.util.List;

public class ReplacementVisitor implements Parser.StatementVisitor<Object> {

	String var;
	Method m;

	public ReplacementVisitor(String var, Method m) {
		this.var = var;
		this.m = m;
	}

	@Override public Object visit(Parser.StatementNode statementNode) {
		return null;
	}

	@Override public Object visit(Parser.VariableAssignmentNode assignment) {
		return Parser.StatementVisitor.super.visit(assignment);
	}

	@Override public Object visit(Parser.MultipleVariableAssignmentNode assignment) {
		return Parser.StatementVisitor.super.visit(assignment);
	}

	@Override public Object visit(Parser.VariableDeclarationNode variableDeclaration) {
		return Parser.StatementVisitor.super.visit(variableDeclaration);
	}

	@Override public Object visit(Parser.ArrayAssignmentNode variableDeclaration) {
		return Parser.StatementVisitor.super.visit(variableDeclaration);
	}

	@Override public Object visit(Parser.OutputVariableDeclarationNode outputDecl) {
		return Parser.StatementVisitor.super.visit(outputDecl);
	}

	@Override public Object visit(Parser.AppendOnlyVariableDeclarationNode appendDecl) {
		return Parser.StatementVisitor.super.visit(appendDecl);
	}

	@Override public Object visit(Parser.InputVariableDeclarationNode inputDecl) {
		return Parser.StatementVisitor.super.visit(inputDecl);
	}

	@Override public Object visit(Parser.TmpInputVariableDeclarationNode inputDecl) {
		return Parser.StatementVisitor.super.visit(inputDecl);
	}

	@Override public Object visit(Parser.BlockNode block) {
		block.statementNodes.replaceAll(n -> {
			if (n instanceof Parser.ReturnStatementNode) {
				return new Parser.VariableAssignmentNode(Converter.DUMMY_LOCATION, this.var,
						((Parser.ReturnStatementNode) n).expression);
			} else {
				return n;
			}
		});
		return null;
	}

	@Override public Object visit(Parser.ConditionalStatementNode condStatement) {
		visitChildren(condStatement);
		return null;
	}

	@Override public Object visit(Parser.IfStatementNode ifStatement) {
		ifStatement.ifBlock.accept(this);
		ifStatement.elseBlock.accept(this);
		return null;
	}

	@Override public Object visit(Parser.IfStatementEndNode ifEndStatement) {
		return null;
	}

	@Override public Object visit(Parser.WhileStatementNode whileStatement) {
		whileStatement.body.accept(this);
		return null;
	}

	@Override public Object visit(Parser.WhileStatementEndNode whileEndStatement) {
		return Parser.StatementVisitor.super.visit(whileEndStatement);
	}

	@Override public Object visit(Parser.LoopInterruptionNode loopInterruptionNode) {
		return Parser.StatementVisitor.super.visit(loopInterruptionNode);
	}

	@Override public Object visit(Parser.ExpressionStatementNode expressionStatement) {
		return Parser.StatementVisitor.super.visit(expressionStatement);
	}

	@Override public Object visit(Parser.ReturnStatementNode returnStatement) {
		return Parser.StatementVisitor.super.visit(returnStatement);
	}

	@Override public List<Object> visitChildren(Parser.MJNode node) {
		return Parser.StatementVisitor.super.visitChildren(node);
	}

	@Override public void visitChildrenDiscardReturn(Parser.MJNode node) {
		Parser.StatementVisitor.super.visitChildrenDiscardReturn(node);
	}
}