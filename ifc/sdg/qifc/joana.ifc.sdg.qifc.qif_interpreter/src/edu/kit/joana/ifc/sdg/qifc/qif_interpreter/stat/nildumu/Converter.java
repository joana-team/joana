package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu;

import com.google.common.collect.BiMap;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IComparisonInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.LoopBody;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.ConversionException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.BBlockOrdering;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Util;
import nildumu.Context;
import nildumu.Lattices;
import nildumu.Parser;
import nildumu.typing.Type;
import swp.lexer.Location;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.kit.joana.ifc.sdg.qifc.qif_interpreter.exec.ExecutionVisitor.OUTPUT_FUNCTION;

/**
 * converts a qif program into a nildumu ast
 */

public class Converter {

	private static int varNameCounter = 0;

	public static final Location DUMMY_LOCATION = new Location(-1, -1);
	private NildumuOptions options;
	private Context context;

	public Map<Integer, Parser.InputVariableDeclarationNode> inputs;
	public static Map<LoopBody, LoopConversionResult> loopMethods;

	public Converter() {
		this.options = NildumuOptions.DEFAULT;
		this.context = new Context(options.secLattice, options.intWidth);
		this.inputs = new HashMap<>();
		loopMethods = new HashMap<>();
	}

	public Parser.ProgramNode convertProgram(Program p) throws ConversionException {
		List<Parser.InputVariableDeclarationNode> inputs = convertSecretInputs(p);
		Parser.ProgramNode programNode = new Parser.ProgramNode(context);
		inputs.forEach(programNode::addGlobalStatement);
		programNode.addGlobalStatements(parseConstantValues(p.getEntryMethod()));
		ConversionVisitor cVis = new ConversionVisitor(p.getEntryMethod(), new HashMap<>());
		List<BBlock> allBlocks = BBlockOrdering
				.topological(p.getEntryMethod().getCFG().getBlocks(), p.getEntryMethod().getCFG().entry());
		List<Parser.StatementNode> stmts = convertStatements(allBlocks, cVis);
		programNode.addGlobalStatements(stmts);
		this.loopMethods.values().forEach(r -> programNode.addMethod(r.method));

		for (Method m : p.getMethods()) {
			if (m.equals(p.getEntryMethod()))
				continue;
			programNode.addMethod(convertMethod(m));
		}

		return programNode;
	}

	/**
	 * turns a loop into a recursive method that has the same overall semantics
	 *
	 * @param l loopBody object
	 * @return pair that contains the generated method and a variableDeclarationNode for the loop results
	 */
	public LoopConversionResult convertLoop(LoopBody l) {
		LoopConversionResult result = new LoopConversionResult();
		result.iMethod = l.getOwner();

		// get names for all the variables used ready
		List<Integer> argsParamVals = l.getAllUses();
		argsParamVals.removeAll(l.getAllDefs());
		BiMap<Integer, Integer> beforeLoop = l.phiToBeforeLoop().inverse();
		argsParamVals.removeIf(i -> l.getOwner().isConstant(i) && !beforeLoop.containsKey(i));
		Map<Integer, Integer> inLoop = l.phiToInsideLoop();

		result.callArgs = argsParamVals.stream().mapToInt(i -> i).toArray();
		result.params = argsParamVals.stream().map(i -> beforeLoop.getOrDefault(i, i)).mapToInt(i -> i).toArray();
		result.recCallArgs = Arrays.stream(result.params).map(i -> inLoop.getOrDefault(i, i)).toArray();
		result.recCallReturnVars = IntStream.range(0, inLoop.size()).mapToObj(i -> varName()).toArray(String[]::new);
		result.phiRes = IntStream.range(0, inLoop.size()).mapToObj(i -> varName()).toArray(String[]::new);

		// parameters for loop method
		Parser.ParametersNode param = parameters(result.params());
		Map<Integer, Parser.ParameterNode> parameterMap = param.parameterNodes.stream()
				.collect(Collectors.toMap(p -> valNum(p.name), p -> p));

		// convert loopBody to ast stmts
		ConversionVisitor cVis = new ConversionVisitor(l.getOwner(), parameterMap);
		List<BBlock> loopBlocks = BBlockOrdering
				.topological(l.getBlocks().stream().filter(b -> !l.getHead().equals(b)).collect(Collectors.toSet()),
						l.getHead().succs().stream().filter(b -> l.hasBlock(b.idx())).findFirst().get());

		Parser.BlockNode body = new Parser.BlockNode(DUMMY_LOCATION, parseConstantValues(l.getOwner()));
		body.addAll(convertStatements(loopBlocks, cVis));

		// convert loop condition
		l.getHead().getWalaBasicBlock().getLastInstruction().visit(cVis);

		// args for recursive call
		Parser.ArgumentsNode args = arguments(result.recCallArgs());

		// loop method return type
		List<Type> elementTypes = Arrays.stream(result.recCallReturnVars)
				.map(i -> edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type.INTEGER.nildumuType())
				.collect(Collectors.toList());
		Type returnType = edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type.nTypes.getOrCreateTupleType(elementTypes);

		// recursive call to loop method
		List<Parser.VariableDeclarationNode> returnVarDecls = IntStream.range(0, result.recCallReturnVars.length)
				.mapToObj(i -> Converter.varDecl(result.recCallReturnVars[i], returnType.getBracketAccessResult(i)))
				.collect(Collectors.toList());
		Parser.MethodInvocationNode recCallExpr = new Parser.MethodInvocationNode(DUMMY_LOCATION, methodName(l), args);
		Parser.MultipleVariableAssignmentNode recCall = new Parser.MultipleVariableAssignmentNode(DUMMY_LOCATION,
				result.recCallReturnVars, new Parser.UnpackOperatorNode(recCallExpr));
		body.add(recCall);

		// get condition for if stmt
		cVis.currentBlock = l.getHead();
		l.getHead().getWalaBasicBlock().getLastInstruction().visit(cVis);
		Parser.ExpressionNode loopCond = cVis.blockToExpr.get(l.getHead().idx());
		if (!l.hasBlock(l.getHead().getTrueTarget())) {
			loopCond = new Parser.UnaryOperatorNode(loopCond, Parser.LexerTerminal.INVERT);
		}

		// wrap loopBody in if-stmt
		Parser.BlockNode completeBody = new Parser.BlockNode(DUMMY_LOCATION, new ArrayList<>());
		completeBody.statementNodes.addAll(returnVarDecls);
		// TODO do we need to invert loop condition ???
		Parser.IfStatementNode if_ = new Parser.IfStatementNode(DUMMY_LOCATION, loopCond, body);
		completeBody.add(if_);

		// add phi-stmts + return stmt
		Integer[] returnOrder = new ArrayList<>(inLoop.keySet()).toArray(new Integer[0]);
		String[] phiArgs = Arrays.stream(returnOrder).map(i -> Converter.varName(i, l.getOwner()))
				.toArray(String[]::new);
		for (int i = 0; i < result.phiRes.length; i++) {
			completeBody.add(new Parser.VariableDeclarationNode(DUMMY_LOCATION, result.phiRes[i],
					edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type.INTEGER.nildumuType(),
					new Parser.PhiNode(DUMMY_LOCATION, Arrays.asList(result.recCallReturnVars[i], phiArgs[i]))));
		}
		completeBody.add(new Parser.ReturnStatementNode(DUMMY_LOCATION,
				Arrays.stream(result.phiRes).map(Converter::variableAccess).collect(Collectors.toList())));

		result.method = new Parser.MethodNode(DUMMY_LOCATION, methodName(l), returnType, param, completeBody,
				new Parser.GlobalVariablesNode(DUMMY_LOCATION, new HashMap<>()));
		Parser.MethodInvocationNode callToLoop = new Parser.MethodInvocationNode(DUMMY_LOCATION, methodName(l),
				arguments(result.callArgs()));

		result.call = new Parser.MultipleVariableAssignmentNode(DUMMY_LOCATION,
				Arrays.stream(returnOrder).map(i -> Converter.varName(i, l.getOwner())).toArray(String[]::new),
				new Parser.UnpackOperatorNode(callToLoop));
		result.returnDefs = new HashMap<>();
		IntStream.range(0, returnOrder.length).forEach(i -> result.returnDefs
				.put(i, varDecl(varName(returnOrder[i], l.getOwner()), returnType.getBracketAccessResult(i))));
		return result;
	}

	public static class LoopConversionResult {
		Method iMethod;
		Parser.MethodNode method;
		Map<Integer, Parser.VariableDeclarationNode> returnDefs;
		Parser.MultipleVariableAssignmentNode call;
		int[] callArgs;
		int[] params;
		int[] recCallArgs;
		String[] recCallReturnVars;
		String[] phiRes;

		List<String> recCallArgs() {
			return Arrays.stream(recCallArgs).mapToObj(i -> Converter.varName(i, iMethod)).collect(Collectors.toList());
		}

		List<String> callArgs() {
			return Arrays.stream(callArgs).mapToObj(i -> Converter.varName(i, iMethod)).collect(Collectors.toList());
		}

		List<String> params() {
			return Arrays.stream(params).mapToObj(i -> Converter.varName(i, iMethod)).collect(Collectors.toList());
		}
	}

	public Parser.MethodNode convertMethod(Method m) {
		Map<Integer, Parser.ParameterNode> params = parseParameters(m);
		Parser.BlockNode body = convertMethodBody(m, params);
		Type returnType = m.getReturnType().nildumuType();
		Parser.GlobalVariablesNode globals = new Parser.GlobalVariablesNode(DUMMY_LOCATION, new HashMap<>());
		return new Parser.MethodNode(DUMMY_LOCATION, methodName(m.identifier()), returnType,
				new Parser.ParametersNode(DUMMY_LOCATION, new ArrayList<>(params.values())), body, globals);
	}

	private List<Parser.StatementNode> parseConstantValues(Method m) {
		return m.getProgramValues().entrySet().stream().filter(p -> p.getValue().isConstant())
				.map(p -> new Parser.VariableDeclarationNode(DUMMY_LOCATION, varName(p.getKey(), m),
						p.getValue().getType().nildumuType(), Parser.literal((int) m.getValue(p.getKey()).getVal())))
				.collect(Collectors.toList());
	}

	private Parser.BlockNode convertMethodBody(Method m, Map<Integer, Parser.ParameterNode> params) {
		ConversionVisitor cVis = new ConversionVisitor(m, params);
		return cVis.convert();
	}

	private List<Parser.StatementNode> convertStatements(List<BBlock> toConvert, ConversionVisitor cVis) {
		Pair<List<Parser.StatementNode>, Map<Integer, Parser.VariableDeclarationNode>> convertedPair = convertStatementsRec(
				toConvert, new ArrayList<>(), new HashMap<>(), cVis);
		return Util.prepend(convertedPair.fst, new ArrayList<>(convertedPair.snd.values()));

	}

	private Pair<List<Parser.StatementNode>, Map<Integer, Parser.VariableDeclarationNode>> convertStatementsRec(
			List<BBlock> toConvert, List<Parser.StatementNode> converted,
			Map<Integer, Parser.VariableDeclarationNode> varDecls, ConversionVisitor cVis) {

		if (toConvert.isEmpty())
			return Pair.make(converted, varDecls);

		BBlock b = toConvert.remove(0);

		if (b.isLoopHeader()) {
			LoopBody loop = b.getCFG().getMethod().getLoops().stream().filter(l -> l.getHead().idx() == b.idx())
					.findFirst().get();
			LoopConversionResult convertedMethod = convertLoop(loop);
			this.loopMethods.put(loop, convertedMethod);
			converted.add(convertedMethod.call);
			varDecls.putAll(convertedMethod.returnDefs);
			toConvert.removeAll(loop.getBlocks());
			return convertStatementsRec(toConvert, converted, varDecls, cVis);
		}

		Pair<List<Parser.StatementNode>, Map<Integer, Parser.VariableDeclarationNode>> blockStmts = cVis.visitBlock(b);
		converted.addAll(blockStmts.fst);
		varDecls.putAll(blockStmts.snd);

		if (b.isCondHeader()) {
			BBlock trueTarget = b.getCFG().getBlock(b.getTrueTarget());
			BBlock falseTarget = b.succs().stream().filter(bb -> bb.idx() != b.getTrueTarget()).findAny().get();
			List<BBlock> trueBranch = computeConditionalBranch(b, trueTarget, trueTarget,
					new ArrayList<>(Arrays.asList(trueTarget)));
			List<BBlock> falseBranch = computeConditionalBranch(b, falseTarget, falseTarget,
					new ArrayList<>(Arrays.asList(falseTarget)));
			toConvert.removeAll(trueBranch);
			toConvert.removeAll(falseBranch);
			Pair<List<Parser.StatementNode>, Map<Integer, Parser.VariableDeclarationNode>> trueStmts = convertStatementsRec(
					trueBranch, new ArrayList<>(), varDecls, cVis);
			Pair<List<Parser.StatementNode>, Map<Integer, Parser.VariableDeclarationNode>> falseStmts = convertStatementsRec(
					falseBranch, new ArrayList<>(), varDecls, cVis);

			Parser.IfStatementNode if_ = new Parser.IfStatementNode(DUMMY_LOCATION, cVis.blockToExpr(b.idx()),
					new Parser.BlockNode(DUMMY_LOCATION, trueStmts.fst),
					new Parser.BlockNode(DUMMY_LOCATION, falseStmts.fst));
			converted.add(if_);
			varDecls.putAll(trueStmts.snd);
			varDecls.putAll(falseStmts.snd);
		}

		if (converted.isEmpty()) {
			converted.add(new Parser.EmptyStatementNode(DUMMY_LOCATION));
		}

		return convertStatementsRec(toConvert, converted, varDecls, cVis);
	}

	private List<Parser.InputVariableDeclarationNode> convertSecretInputs(Program p) {
		List<Parser.InputVariableDeclarationNode> inputs = new ArrayList<>();
		Method topLevel = p.getEntryMethod();

		for (int i = 1; i < topLevel.getParamNum(); i++) {
			int valNum = topLevel.getIr().getParameter(i);
			Parser.InputVariableDeclarationNode node = new Parser.InputVariableDeclarationNode(DUMMY_LOCATION,
					varName(valNum, topLevel), topLevel.getParamType(i).nildumuType(),
					new Parser.IntegerLiteralNode(DUMMY_LOCATION, Lattices.ValueLattice.get()
							.parse("0b" + String.join("", Collections.nCopies(options.intWidth, "u")))), "h");
			this.inputs.put(valNum, node);
			inputs.add(node);
		}
		return inputs;
	}

	/**
	 * returns a list of blocks that belong to a branch in an if-statement
	 * <p>
	 * invariant: all nodes that belong to the branch are dominated by the first block in the branch. We recursively (DFS) add successors, until we find one that is not dominated by {@code firstBlock}
	 *
	 * @param head       block containing the if-condition
	 * @param firstBlock successor of head that belongs to the branch we want to compute
	 * @param branch     list of blocks that we have already identified as being part of the branch. {@code current} is already part of the list!
	 * @return list of all basic blocks that belong to the branch
	 */
	public List<BBlock> computeConditionalBranch(BBlock head, BBlock firstBlock, BBlock current, List<BBlock> branch) {
		for (BBlock succ : current.succs()) {
			if ((succ.isDummy() || head.getCFG().isDominatedBy(succ, firstBlock)) && !branch.contains(succ)) {
				branch.add(succ);
				branch = computeConditionalBranch(head, firstBlock, succ, branch);
			}
		}
		return branch;
	}

	public Map<Integer, Parser.ParameterNode> parseParameters(Method m) {
		Map<Integer, Parser.ParameterNode> params = new HashMap<>();

		// start @ 1, bc 0 is 'this'-reference
		for (int i = 1; i < m.getParamNum(); i++) {
			Parser.ParameterNode p = new Parser.ParameterNode(DUMMY_LOCATION, m.getParamType(i).nildumuType(),
					varName(m.getIr().getParameter(i), m));
			params.put(m.getIr().getParameter(i), p);
		}
		return params;
	}

	public static class LexerTerminal {

		public static Parser.LexerTerminal of(IBinaryOpInstruction.Operator op) throws ConversionException {
			switch (op) {
			case ADD:
				return Parser.LexerTerminal.PLUS;
			case SUB:
				return Parser.LexerTerminal.MINUS;
			case MUL:
				return Parser.LexerTerminal.MULTIPLY;
			case DIV:
				return Parser.LexerTerminal.DIVIDE;
			case REM:
				return Parser.LexerTerminal.MODULO;
			case AND:
				return Parser.LexerTerminal.BAND;
			case OR:
				return Parser.LexerTerminal.BOR;
			case XOR:
				return Parser.LexerTerminal.XOR;
			default:
				throw new ConversionException(op);
			}
		}

		public static Parser.LexerTerminal of(IUnaryOpInstruction.Operator op) throws ConversionException {
			switch (op) {
			case NEG:
				return Parser.LexerTerminal.INVERT;
			default:
				throw new ConversionException(op);
			}
		}

		public static Parser.LexerTerminal of(IComparisonInstruction.Operator operator) throws ConversionException {
			switch (operator) {

			case CMP:
				return Parser.LexerTerminal.EQUALS;
			case CMPL:
				return Parser.LexerTerminal.LOWER;
			case CMPG:
				return Parser.LexerTerminal.GREATER;
			}
			throw new ConversionException(operator);
		}

		public static Parser.LexerTerminal of(IConditionalBranchInstruction.Operator operator)
				throws ConversionException {
			switch (operator) {
			case EQ:
				return Parser.LexerTerminal.EQUALS;
			case NE:
				return Parser.LexerTerminal.UNEQUALS;
			case LT:
				return Parser.LexerTerminal.LOWER;
			case GE:
				return Parser.LexerTerminal.GREATER_EQUALS;
			case GT:
				return Parser.LexerTerminal.GREATER;
			case LE:
				return Parser.LexerTerminal.LOWER_EQUALS;
			}
			throw new ConversionException(operator);
		}
	}

	public static class ConversionVisitor extends SSAInstruction.Visitor {

		private final Method m;
		private List<Parser.StatementNode> stmts;
		private final Map<Integer, Parser.ParameterNode> valToParam;
		private final Map<Integer, Parser.ExpressionNode> blockToExpr;
		private Map<Integer, Parser.VariableDeclarationNode> varDecls;
		private BBlock currentBlock;

		public ConversionVisitor(Method m, Map<Integer, Parser.ParameterNode> parameterToNode) {
			this.m = m;
			this.stmts = new ArrayList<>();
			this.valToParam = parameterToNode;
			this.blockToExpr = new HashMap<>();
		}

		public Parser.BlockNode convert() {
			for (BBlock b : m.getCFG().getBlocks()) {
				if (b.isDummy()) {
					continue;
				}
				this.currentBlock = b;
				this.currentBlock.instructions().forEach(i -> i.visit(this));
			}
			return new Parser.BlockNode(DUMMY_LOCATION, stmts);
		}

		@Override public void visitGoto(SSAGotoInstruction instruction) {
			// do nothing
		}

		@Override public void visitArrayLoad(SSAArrayLoadInstruction instruction) {

		}

		@Override public void visitArrayStore(SSAArrayStoreInstruction instruction) {

		}

		@Override public void visitBinaryOp(SSABinaryOpInstruction instruction) {
			IBinaryOpInstruction.Operator op = (IBinaryOpInstruction.Operator) instruction.getOperator();

			Parser.LexerTerminal terminal = null;
			try {
				terminal = LexerTerminal.of(op);
			} catch (ConversionException e) {
				e.printStackTrace();
			}

			Parser.BinaryOperatorNode binOp = new Parser.BinaryOperatorNode(access(instruction.getUse(0)),
					access(instruction.getUse(1)), terminal);

			this.varDecls.put(instruction.getDef(), Converter.varDecl(varName(instruction.getDef(), m),
					m.getValue(instruction.getDef()).getType().nildumuType()));
			this.stmts.add(assignment(varName(instruction.getDef(), m), binOp));
		}

		// TODO how is unary minus handled?
		@Override public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
			IUnaryOpInstruction.Operator op = (IUnaryOpInstruction.Operator) instruction.getOpcode();
			Parser.UnaryOperatorNode unOp = null;
			try {
				unOp = new Parser.UnaryOperatorNode(access(instruction.getUse(0)), LexerTerminal.of(op));
			} catch (ConversionException e) {
				e.printStackTrace();
			}
			this.varDecls.put(instruction.getDef(), Converter.varDecl(varName(instruction.getDef(), m),
					m.getValue(instruction.getDef()).getType().nildumuType()));
			this.stmts.add(assignment(varName(instruction.getDef(), m), unOp));
		}

		@Override public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {
			try {
				Parser.BinaryOperatorNode expr = new Parser.BinaryOperatorNode(access(instruction.getUse(0)),
						access(instruction.getUse(1)),
						LexerTerminal.of((IConditionalBranchInstruction.Operator) instruction.getOperator()));
				this.blockToExpr.put(currentBlock.idx(), expr);
			} catch (ConversionException e) {
				e.printStackTrace();
			}
		}

		@Override public void visitReturn(SSAReturnInstruction instruction) {
			if (currentBlock.getCFG().getMethod().getProg().getEntryMethod().equals(currentBlock.getCFG().getMethod()))
				return;

			if (instruction.returnsVoid()) {
				this.stmts.add(new Parser.ReturnStatementNode(DUMMY_LOCATION));
			} else {
				this.stmts.add(new Parser.ReturnStatementNode(DUMMY_LOCATION, access(instruction.getResult())));
			}
		}

		@Override public void visitInvoke(SSAInvokeInstruction instruction) {
			if (instruction.getCallSite().getDeclaredTarget().getSignature().equals(OUTPUT_FUNCTION)) {
				int leaked = instruction.getUse(0);
				Parser.OutputVariableDeclarationNode node;
				node = new Parser.OutputVariableDeclarationNode(DUMMY_LOCATION,
						"o_" + varName(instruction.getUse(0), m),
						edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type.INTEGER.nildumuType(), access(leaked), "l");
				this.stmts.add(node);
			} else {
				assert (instruction.hasDef());
				Parser.ArgumentsNode args = Converter.arguments(IntStream.range(1, instruction.getNumberOfParameters())
						.mapToObj(i -> varName(instruction.getUse(i), m)).collect(Collectors.toList()));
				Parser.MethodInvocationNode invocation = new Parser.MethodInvocationNode(DUMMY_LOCATION,
						methodName(instruction.getDeclaredTarget()), args);
				this.varDecls.put(instruction.getDef(), Converter.varDecl(varName(instruction.getDef(), m),
						m.getValue(instruction.getDef()).getType().nildumuType()));
				this.stmts.add(assignment(varName(instruction.getDef(), m), invocation));
			}
		}

		@Override public void visitNew(SSANewInstruction instruction) {

		}

		@Override public void visitArrayLength(SSAArrayLengthInstruction instruction) {

		}

		@Override public void visitPhi(SSAPhiInstruction instruction) {
			// TODO where can i get the correct order of the arguments?
			// for if-stmts: first one comes from "true"-branch? Is it the same for joana?

			BBlock condBlock = m.getCFG().getImmDom(currentBlock);
			BBlock firstPred = currentBlock.preds().get(0);
			int firstArg = (m.getCFG().isDominatedBy(firstPred, m.getCFG().getBlock(condBlock.getTrueTarget()))) ?
					0 :
					1;
			int sndArg = 1 - firstArg;

			Parser.PhiNode phi = new Parser.PhiNode(DUMMY_LOCATION,
					Arrays.asList(varName(instruction.getUse(firstArg), m), varName(instruction.getUse(sndArg), m)));
			this.varDecls.put(instruction.getDef(), Converter.varDecl(varName(instruction.getDef(), m),
					m.getValue(instruction.getDef()).getType().nildumuType()));
			stmts.add(new Parser.VariableAssignmentNode(DUMMY_LOCATION, varName(instruction.getDef(), m), phi));
		}

		public Pair<List<Parser.StatementNode>, Map<Integer, Parser.VariableDeclarationNode>> visitBlock(BBlock b) {
			this.currentBlock = b;
			this.stmts = new ArrayList<>();
			this.varDecls = new HashMap<>();
			b.instructions().forEach(i -> i.visit(this));
			return Pair.make(stmts, varDecls);
		}

		public Parser.ExpressionNode blockToExpr(int idx) {
			return this.blockToExpr.get(idx);
		}

		public Parser.PrimaryExpressionNode access(int valNum) {
			if (this.valToParam.containsKey(valNum)) {
				return accessParam(valNum);
			} else if (m.isConstant(valNum)) {
				return Parser.literal((Integer) m.getValue(valNum).getVal());
			}
			return new Parser.VariableAccessNode(DUMMY_LOCATION, varName(valNum, m));
		}

		private Parser.ParameterAccessNode accessParam(int valNum) {
			return new Parser.ParameterAccessNode(DUMMY_LOCATION, valToParam.get(valNum).name);
		}
	}

	public static String varName(int valNum, Method m) {
		return "v_" + m.identifier() + "_" + valNum;
	}

	public static int valNum(String varName) {
		assert (varName.startsWith("v"));
		String[] nameParts = varName.split("_");
		return Integer.parseInt(nameParts[nameParts.length - 1]);
	}

	public static String varName() {
		return "x" + varNameCounter++;
	}

	public static Parser.ArgumentsNode arguments(List<String> varNames) {
		return new Parser.ArgumentsNode(DUMMY_LOCATION,
				varNames.stream().map(Converter::variableAccess).collect(Collectors.toList()));
	}

	public Parser.ParameterNode parameter(String varName) {
		return new Parser.ParameterNode(DUMMY_LOCATION,
				edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type.INTEGER.nildumuType(), varName);
	}

	public Parser.ParametersNode parameters(List<String> varNames) {
		return new Parser.ParametersNode(DUMMY_LOCATION,
				varNames.stream().map(this::parameter).collect(Collectors.toList()));
	}

	public static Parser.VariableAccessNode variableAccess(String varName) {
		return new Parser.VariableAccessNode(DUMMY_LOCATION, varName);
	}

	public static Parser.VariableAssignmentNode assignment(String var, Parser.ExpressionNode expr) {
		return new Parser.VariableAssignmentNode(DUMMY_LOCATION, var, expr);
	}

	public static Parser.VariableDeclarationNode varDecl(String varName, Type type) {
		return new Parser.VariableDeclarationNode(DUMMY_LOCATION, varName, type);
	}

	public static Parser.VariableDeclarationNode varDecl(String varName, Parser.ExpressionNode value) {
		return new Parser.VariableDeclarationNode(DUMMY_LOCATION, varName,
				edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type.INTEGER.nildumuType(), value);
	}

	public static String methodName(MethodReference mRef) {
		return methodName(mRef.getSignature());
	}

	public static String methodName(String signature) {
		String sig = signature.replaceAll("[\\(\\)$&+.,:;=?@#|]", "");
		return sig;
	}

	public static String methodName(LoopBody l) {
		return "m" + l.getOwner().identifierNoSpecialCharacters() + "_" + l.getHead().idx();
	}
}