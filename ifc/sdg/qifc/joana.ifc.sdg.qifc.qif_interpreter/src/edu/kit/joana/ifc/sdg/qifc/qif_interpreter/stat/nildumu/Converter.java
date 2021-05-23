package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu;

import com.google.common.collect.BiMap;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IComparisonInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.ssa.*;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.LoopBody;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.ConversionException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.BBlockOrdering;
import nildumu.Context;
import nildumu.Lattices;
import nildumu.Operator;
import nildumu.Parser;
import nildumu.typing.Type;
import nildumu.typing.Types;
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
	public Map<LoopBody, Parser.MethodNode> loopMethods;

	public Converter() {
		this.options = NildumuOptions.DEFAULT;
		this.context = new Context(options.secLattice, options.intWidth);
		this.inputs = new HashMap<>();
		this.loopMethods = new HashMap<>();
	}

	public static String varName(int valNum) {
		return "v" + valNum;
	}

	public static int valNum(String varName) {
		assert (varName.startsWith("v"));
		return Integer.parseInt(varName.substring(1));
	}

	public static String varName() {
		return "x" + varNameCounter++;
	}

	public Parser.ArgumentsNode arguments(List<String> varNames) {
		return new Parser.ArgumentsNode(DUMMY_LOCATION,
				varNames.stream().map(s -> variableAccess(s)).collect(Collectors.toList()));
	}

	public Parser.ParameterNode parameter(String varName) {
		return new Parser.ParameterNode(DUMMY_LOCATION, NildumuType.types.INT, varName);
	}

	public Parser.ParametersNode parameters(List<String> varNames) {
		return new Parser.ParametersNode(DUMMY_LOCATION,
				varNames.stream().map(this::parameter).collect(Collectors.toList()));
	}

	public Parser.VariableAccessNode variableAccess(String varName) {
		return new Parser.VariableAccessNode(DUMMY_LOCATION, varName);
	}

	public Parser.VariableDeclarationNode varDecl(String varName) {
		return new Parser.VariableDeclarationNode(DUMMY_LOCATION, varName, NildumuType.types.INT);
	}

	public static String methodName(LoopBody l) {
		return "m" + l.getHead().idx();
	}

	public Parser.ProgramNode convertProgram(Program p) throws ConversionException {
		List<Parser.InputVariableDeclarationNode> inputs = convertSecretInputs(p);
		Map<Integer, Parser.ExpressionNode> consts = parseConstantValues(p.getEntryMethod());
		Parser.ProgramNode programNode = new Parser.ProgramNode(context);
		inputs.forEach(programNode::addGlobalStatement);
		consts.entrySet().stream()
				.map(entry -> new Parser.VariableDeclarationNode(DUMMY_LOCATION, varName(entry.getKey()),
						NildumuType.of(edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type.INTEGER), entry.getValue()))
				.forEach(programNode::addGlobalStatement);

		ConversionVisitor cVis = new ConversionVisitor(p.getEntryMethod(), new HashMap<>(), consts);
		List<BBlock> allBlocks = new ArrayList<>();
		allBlocks.addAll(p.getEntryMethod().getCFG().getBlocks());
		List<Parser.StatementNode> stmts = convertStatements(allBlocks, new ArrayList<>(), cVis);
		programNode.addGlobalStatements(stmts);
		this.loopMethods.values().forEach(programNode::addMethod);

		return programNode;
	}

	/**
	 * turns a loop into a recursive method that has the same overall semantics
	 *
	 * @param l loopBody object
	 * @return pair that contains the generated method and a variableDeclarationNode for the loop results
	 */
	public Pair<Parser.MethodNode, List<Parser.StatementNode>> convertLoop(LoopBody l) {

		// get names for all the variables used ready
		List<Integer> argsParamVals = l.getAllUses();
		argsParamVals.removeAll(l.getAllDefs());
		BiMap<Integer, Integer> beforeLoop = l.phiToBeforeLoop().inverse();
		Map<Integer, Integer> inLoop = l.phiToInsideLoop();

		String[] loopMethodArgs = argsParamVals.stream().map(Converter::varName).toArray(String[]::new);
		String[] loopMethodParam = argsParamVals.stream().map(i -> beforeLoop.getOrDefault(i, i))
				.map(Converter::varName).toArray(String[]::new);
		String[] recCallArgs = Arrays.stream(loopMethodParam).map(i -> inLoop.getOrDefault(valNum(i), valNum(i)))
				.map(Converter::varName).toArray(String[]::new);
		String[] recCallRes = IntStream.range(0, inLoop.size()).mapToObj(i -> varName()).toArray(String[]::new);
		String[] phiRes = IntStream.range(0, inLoop.size()).mapToObj(i -> varName()).toArray(String[]::new);

		// parameters for loop method
		Parser.ParametersNode param = parameters(Arrays.asList(loopMethodParam));
		Map<Integer, Parser.ParameterNode> parameterMap = param.parameterNodes.stream()
				.collect(Collectors.toMap(p -> valNum(p.name), p -> p));

		// convert loopBody to ast stmts
		ConversionVisitor cVis = new ConversionVisitor(l.getOwner(), parameterMap, new HashMap<>());
		List<BBlock> loopBlocks = BBlockOrdering
				.topological(l.getBlocks().stream().filter(b -> !b.isLoopHeader()).collect(Collectors.toSet()),
						l.getHead().succs().stream().filter(b -> l.hasBlock(b.idx())).findFirst().get());
		Parser.BlockNode body = new Parser.BlockNode(DUMMY_LOCATION,
				convertStatements(loopBlocks, new ArrayList<>(), cVis));

		// convert loop condition
		l.getHead().getWalaBasicBlock().getLastInstruction().visit(cVis);

		// args for recursive call
		Parser.ArgumentsNode args = arguments(Arrays.asList(recCallArgs));

		// loop method return type
		List<Type> elementTypes = Arrays.stream(recCallRes).map(i -> NildumuType.types.INT)
				.collect(Collectors.toList());
		Type returnType = NildumuType.types.getOrCreateTupleType(elementTypes);

		// recursive call to loop method
		List<Parser.VariableDeclarationNode> returnVarDecls = Arrays.stream(recCallRes).map(this::varDecl)
				.collect(Collectors.toList());
		Parser.MethodInvocationNode recCallExpr = new Parser.MethodInvocationNode(DUMMY_LOCATION, methodName(l), args);
		Parser.MultipleVariableAssignmentNode recCall = new Parser.MultipleVariableAssignmentNode(DUMMY_LOCATION,
				recCallRes, new Parser.UnpackOperatorNode(recCallExpr));
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
		String[] phiArgs = Arrays.stream(returnOrder).map(Converter::varName).toArray(String[]::new);
		for (int i = 0; i < phiRes.length; i++) {
			completeBody.add(new Parser.VariableDeclarationNode(DUMMY_LOCATION, phiRes[i], NildumuType.types.INT,
					new Parser.PhiNode(DUMMY_LOCATION, Arrays.asList(recCallRes[i], phiArgs[i]))));
		}
		completeBody.add(new Parser.ReturnStatementNode(DUMMY_LOCATION,
				Arrays.stream(phiRes).map(this::variableAccess).collect(Collectors.toList())));

		Parser.MethodNode loopMethod = new Parser.MethodNode(DUMMY_LOCATION, methodName(l), returnType, param,
				completeBody, new Parser.GlobalVariablesNode(DUMMY_LOCATION, new HashMap<>()));
		Parser.MethodInvocationNode callToLoop = new Parser.MethodInvocationNode(DUMMY_LOCATION, methodName(l),
				arguments(Arrays.asList(loopMethodArgs)));

		List<Parser.StatementNode> defsAndCall = Arrays.stream(returnOrder).map(i -> varDecl(varName(i)))
				.collect(Collectors.toList());
		defsAndCall.add(new Parser.MultipleVariableAssignmentNode(DUMMY_LOCATION,
				Arrays.stream(returnOrder).map(Converter::varName).toArray(String[]::new),
				new Parser.UnpackOperatorNode(callToLoop)));

		return Pair.make(loopMethod, defsAndCall);
	}

	public Parser.MethodNode convertMethod(Method m) throws ConversionException {
		Map<Integer, Parser.ParameterNode> params = parseParameters(m);
		Map<Integer, Parser.ExpressionNode> consts = parseConstantValues(m);
		Parser.BlockNode body = convertMethodBody(m, params, consts);
		Type returnType = NildumuType.of(m.getReturnType());
		Parser.GlobalVariablesNode globals = new Parser.GlobalVariablesNode(DUMMY_LOCATION, new HashMap<>());
		return new Parser.MethodNode(DUMMY_LOCATION, m.getCFG().getName(), returnType,
				new Parser.ParametersNode(DUMMY_LOCATION,
						params.values().stream().map(params::get).map(n -> (Parser.ParameterNode) n)
								.collect(Collectors.toList())), body, globals);
	}

	private Map<Integer, Parser.ExpressionNode> parseConstantValues(Method m) {
		Map<Integer, Parser.ExpressionNode> consts = new HashMap<>();
		m.getProgramValues().entrySet().stream().filter(p -> p.getValue().isConstant())
				.forEach(p -> consts.put(p.getKey(), Parser.literal((int) m.getValue(p.getKey()).getVal())));
		return consts;
	}

	private Parser.BlockNode convertMethodBody(Method m, Map<Integer, Parser.ParameterNode> params,
			Map<Integer, Parser.ExpressionNode> consts) {
		ConversionVisitor cVis = new ConversionVisitor(m, params, consts);
		return cVis.convert();
	}

	private List<Parser.StatementNode> convertStatements(List<BBlock> toConvert, List<Parser.StatementNode> converted,
			ConversionVisitor cVis) {

		// System.out.println(Arrays.toString(toConvert.stream().map(BBlock::idx).toArray()));

		if (toConvert.isEmpty()) {
			return converted;
		}

		BBlock b = toConvert.remove(0);

		if (b.isLoopHeader()) {
			LoopBody loop = b.getCFG().getMethod().getLoops().stream().filter(l -> l.getHead().idx() == b.idx())
					.findFirst().get();
			Pair<Parser.MethodNode, List<Parser.StatementNode>> convertedMethod = convertLoop(loop);
			this.loopMethods.put(loop, convertedMethod.fst);
			converted.addAll(convertedMethod.snd);
			toConvert.removeAll(loop.getBlocks());
			return convertStatements(toConvert, converted, cVis);
		}

		converted.addAll(cVis.visitBlock(b));

		if (b.isCondHeader()) {
			BBlock trueTarget = b.getCFG().getBlock(b.getTrueTarget());
			BBlock falseTarget = b.succs().stream().filter(bb -> bb.idx() != b.getTrueTarget()).findAny().get();
			List<BBlock> trueBranch = computeConditionalBranch(b, trueTarget, trueTarget,
					new ArrayList<>(Arrays.asList(trueTarget)));
			List<BBlock> falseBranch = computeConditionalBranch(b, falseTarget, falseTarget,
					new ArrayList<>(Arrays.asList(falseTarget)));
			toConvert.removeAll(trueBranch);
			toConvert.removeAll(falseBranch);
			List<Parser.StatementNode> trueStmts = convertStatements(trueBranch, new ArrayList<>(), cVis);
			List<Parser.StatementNode> falseStmts = convertStatements(falseBranch, new ArrayList<>(), cVis);

			Parser.IfStatementNode if_ = new Parser.IfStatementNode(DUMMY_LOCATION, cVis.blockToExpr(b.idx()),
					new Parser.BlockNode(DUMMY_LOCATION, trueStmts), new Parser.BlockNode(DUMMY_LOCATION, falseStmts));
			converted.add(if_);
		}

		if (converted.isEmpty()) {
			converted.add(new Parser.EmptyStatementNode(DUMMY_LOCATION));
		}

		return convertStatements(toConvert, converted, cVis);
	}

	private List<Parser.InputVariableDeclarationNode> convertSecretInputs(Program p) throws ConversionException {
		List<Parser.InputVariableDeclarationNode> inputs = new ArrayList<>();
		Method topLevel = p.getEntryMethod();

		for (int i = 1; i < topLevel.getParamNum(); i++) {
			int valNum = topLevel.getIr().getParameter(i);
			Parser.InputVariableDeclarationNode node = new Parser.InputVariableDeclarationNode(DUMMY_LOCATION,
					varName(valNum), NildumuType.of(topLevel.getParamType(i)),
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

	public Map<Integer, Parser.ParameterNode> parseParameters(Method m) throws ConversionException {
		Map<Integer, Parser.ParameterNode> params = new HashMap<>();

		// start @ 1, bc 0 is 'this'-reference
		for (int i = 1; i < m.getParamNum(); i++) {
			Parser.ParameterNode p = new Parser.ParameterNode(DUMMY_LOCATION, NildumuType.of(m.getParamType(i)),
					varName(m.getIr().getParameter(i)));
			params.put(m.getIr().getParameter(i), p);
		}
		return params;
	}

	public static class NildumuType {

		private static final Types types = new Types();

		public static Type of(edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type type) {
			if (type == edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type.INTEGER)
				return types.INT;
			return null;
		}
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

	public static class NildumuOperator {

		public static Operator of(IBinaryOpInstruction.Operator op) throws ConversionException {
			switch (op) {
			case ADD:
				return Operator.ADD;
			case MUL:
				return Operator.MULTIPLY;
			case DIV:
				return Operator.DIVIDE;
			case REM:
				return Operator.MODULO;
			case AND:
				return Operator.AND;
			case OR:
				return Operator.OR;
			case XOR:
				return Operator.XOR;
			default:
				throw new ConversionException(op);
			}
		}
	}

	public static class ConversionVisitor implements SSAInstruction.IVisitor {

		private final Method m;
		private List<Parser.StatementNode> stmts;
		private final Map<Integer, Parser.ParameterNode> valToParam;
		private final Map<Integer, Parser.ExpressionNode> valToExpr;
		private final Map<Integer, Parser.ExpressionNode> blockToExpr;
		private BBlock currentBlock;

		public ConversionVisitor(Method m, Map<Integer, Parser.ParameterNode> parameterToNode,
				Map<Integer, Parser.ExpressionNode> consts) {
			this.m = m;
			this.stmts = new ArrayList<>();
			this.valToParam = parameterToNode;
			this.valToExpr = consts;
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
			this.valToExpr.put(instruction.getDef(), binOp);

			// TODO should we use the same Types() obj every time?
			// TODO compute result type from operation, instead of statically using INT
			this.stmts.add(new Parser.VariableDeclarationNode(DUMMY_LOCATION, varName(instruction.getDef()),
					NildumuType.of(edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type.INTEGER), binOp));
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
			this.valToExpr.put(instruction.getDef(), unOp);
			this.stmts.add(new Parser.VariableDeclarationNode(DUMMY_LOCATION, varName(instruction.getDef()),
					NildumuType.of(edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type.INTEGER), unOp));
		}

		@Override public void visitConversion(SSAConversionInstruction instruction) {

		}

		@Override public void visitComparison(SSAComparisonInstruction instruction) {
			try {
				Parser.BinaryOperatorNode comp = new Parser.BinaryOperatorNode(access(instruction.getUse(0)),
						access(instruction.getUse(1)), LexerTerminal.of(instruction.getOperator()));
				stmts.add(new Parser.VariableDeclarationNode(DUMMY_LOCATION, varName(instruction.getDef()),
						NildumuType.of(edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type.INTEGER), comp));
			} catch (ConversionException e) {
				e.printStackTrace();
			}
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

		@Override public void visitSwitch(SSASwitchInstruction instruction) {

		}

		@Override public void visitReturn(SSAReturnInstruction instruction) {

		}

		@Override public void visitGet(SSAGetInstruction instruction) {

		}

		@Override public void visitPut(SSAPutInstruction instruction) {

		}

		@Override public void visitInvoke(SSAInvokeInstruction instruction) {

			if (instruction.getCallSite().getDeclaredTarget().getSignature().equals(OUTPUT_FUNCTION)) {
				int leaked = instruction.getUse(0);
				Parser.OutputVariableDeclarationNode node = null;
				node = new Parser.OutputVariableDeclarationNode(DUMMY_LOCATION, "o_" + varName(instruction.getUse(0)),
						NildumuType.of(edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type.INTEGER), access(leaked),
						"l");
				this.stmts.add(node);
			}
		}

		@Override public void visitNew(SSANewInstruction instruction) {

		}

		@Override public void visitArrayLength(SSAArrayLengthInstruction instruction) {

		}

		@Override public void visitThrow(SSAThrowInstruction instruction) {

		}

		@Override public void visitMonitor(SSAMonitorInstruction instruction) {

		}

		@Override public void visitCheckCast(SSACheckCastInstruction instruction) {

		}

		@Override public void visitInstanceof(SSAInstanceofInstruction instruction) {

		}

		@Override public void visitPhi(SSAPhiInstruction instruction) {
			// TODO where can i get the correct order of the arguments?
			// for if-stmts: first one comes from "true"-branch? Is it the same for joana?

			// TODO: handle loop-phis separately

			BBlock condBlock = m.getCFG().getImmDom(currentBlock);
			BBlock firstPred = currentBlock.preds().get(0);
			int firstArg = (m.getCFG().isDominatedBy(firstPred, m.getCFG().getBlock(condBlock.getTrueTarget()))) ?
					0 :
					1;
			int sndArg = 1 - firstArg;

			Parser.PhiNode phi = new Parser.PhiNode(DUMMY_LOCATION,
					Arrays.asList(varName(instruction.getUse(firstArg)), varName(instruction.getUse(sndArg))));
			stmts.add(new Parser.VariableDeclarationNode(DUMMY_LOCATION, varName(instruction.getDef()),
					NildumuType.of(edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type.INTEGER), phi));
		}

		@Override public void visitPi(SSAPiInstruction instruction) {

		}

		@Override public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {

		}

		@Override public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {

		}

		public List<Parser.StatementNode> visitBlock(BBlock b) {
			this.currentBlock = b;
			this.stmts = new ArrayList<>();
			b.instructions().forEach(i -> i.visit(this));
			return stmts;
		}

		public Parser.ExpressionNode blockToExpr(int idx) {
			return this.blockToExpr.get(idx);
		}

		public Parser.PrimaryExpressionNode access(int valNum) {
			if (this.valToParam.containsKey(valNum)) {
				return accessParam(valNum);
			} else if (m.isConstant(valNum)) {
				return (Parser.PrimaryExpressionNode) valToExpr.get(valNum);
			}
			return new Parser.VariableAccessNode(DUMMY_LOCATION, varName(valNum));
		}

		private Parser.ParameterAccessNode accessParam(int valNum) {
			return new Parser.ParameterAccessNode(DUMMY_LOCATION, valToParam.get(valNum).name);
		}
	}
}