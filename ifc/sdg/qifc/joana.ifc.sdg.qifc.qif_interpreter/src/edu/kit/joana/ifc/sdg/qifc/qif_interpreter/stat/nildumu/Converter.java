package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu;

import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IComparisonInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.ConversionException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.CFGUtil;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Util;
import edu.kit.joana.util.Triple;
import nildumu.Context;
import nildumu.Lattices;
import nildumu.Parser;
import nildumu.typing.Type;
import org.apache.commons.lang3.ArrayUtils;
import swp.lexer.Location;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.CFGUtil.computeConditionalBranch;

/**
 * converts a qif program into a nildumu ast
 */

public class Converter {

	private static int varNameCounter = 0;

	public static final Location DUMMY_LOCATION = new Location(-1, -1);
	private NildumuOptions options;
	private Context context;

	public Map<Integer, Parser.InputVariableDeclarationNode> inputs;
	public static Map<LoopBody, NildumuProgram.LoopMethod> loopMethods;
	public static Map<SSAArrayStoreInstruction, Integer> arrayVarIndices;

	public Converter() {
		this.options = NildumuOptions.DEFAULT;
		this.context = new Context(options.secLattice, options.intWidth);
		this.inputs = new HashMap<>();
		arrayVarIndices = new HashMap<>();
		loopMethods = new HashMap<>();
	}

	public Converter(Context context) {
		this.options = NildumuOptions.DEFAULT;
		this.context = context;
		this.inputs = new HashMap<>();
		arrayVarIndices = new HashMap<>();
		loopMethods = new HashMap<>();
	}

	public Parser.ProgramNode convertProgram(Program p) throws ConversionException {
		List<Parser.StatementNode> inputs = convertSecretInputs(p);
		Parser.ProgramNode programNode = new Parser.ProgramNode(context);
		inputs.forEach(programNode::addGlobalStatement);
		programNode.addGlobalStatements(parseConstantValues(p.getEntryMethod()));
		ConversionVisitor cVis = new ConversionVisitor(this, p.getEntryMethod(), new HashMap<>());
		List<BasicBlock> allBlocks = CFGUtil
				.topological(p.getEntryMethod().getCFG().getBlocks(), p.getEntryMethod().getCFG().entry());
		List<Parser.StatementNode> stmts = convertStatements(allBlocks, cVis);
		programNode.addGlobalStatements(stmts);
		loopMethods.values().forEach(r -> programNode.addMethod(r.method));

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
	public NildumuProgram.LoopMethod convertLoop(LoopBody l) {
		NildumuProgram.LoopMethod result = new NildumuProgram.LoopMethod();
		result.iMethod = l.getOwner();

		// get names for all the variables used ready
		List<Integer> usedValsForParams = l.getAllUses();
		usedValsForParams.removeAll(l.getAllDefs());
		usedValsForParams.removeIf(i -> l.getOwner().isConstant(i));

		List<Triple<Integer, Integer, Integer>> phiMap = l.phiMapping();

		result.callArgs = new int[phiMap.size() + usedValsForParams.size()];
		IntStream.range(0, phiMap.size()).forEach(i -> result.callArgs[i] = phiMap.get(i).getMiddle());
		IntStream.range(0, usedValsForParams.size())
				.forEach(i -> result.callArgs[phiMap.size() + i] = usedValsForParams.get(i));

		result.params = result.callArgs.clone();
		IntStream.range(0, phiMap.size()).forEach(i -> result.params[i] = phiMap.get(i).getLeft());

		result.recCallArgs = result.callArgs.clone();
		IntStream.range(0, phiMap.size()).forEach(i -> result.callArgs[i] = phiMap.get(i).getRight());

		result.returnVars = ArrayUtils.addAll(phiMap.stream().mapToInt(Triple::getLeft).toArray(),
				l.getAllWrittenToArrays().stream().mapToInt(i -> i).toArray());

		// parameters for loop method
		Parser.ParametersNode param = parameters(result.params());
		Map<Integer, Parser.ParameterNode> parameterMap = param.parameterNodes.stream()
				.collect(Collectors.toMap(p -> valNum(p.name), p -> p));

		// convert loopBody to ast stmts
		ConversionVisitor cVis = new LoopConversionVisitor(this, l.getOwner(), parameterMap, l, result);
		List<BasicBlock> loopBlocks = CFGUtil
				.topological(l.getBlocks().stream().filter(b -> !l.getHead().equals(b)).collect(Collectors.toSet()),
						l.getHead().succs().stream().filter(b -> l.hasBlock(b.idx())).findFirst().get());

		Parser.BlockNode body = new Parser.BlockNode(
				new Location(l.getHead().idx(), l.getHead().getWalaBasicBlock().getFirstInstructionIndex()),
				parseConstantValues(l.getOwner()));
		body.addAll(convertStatements(loopBlocks, cVis));
		result.loopBody = new ArrayList<>(body.statementNodes);

		// convert loop condition
		l.getHead().getWalaBasicBlock().getLastInstruction().visit(cVis);

		// args for recursive call
		Parser.ArgumentsNode args = arguments(result.recCallArgs());

		// loop method return type
		List<Type> elementTypes = IntStream.range(0, phiMap.size())
				.mapToObj(i -> edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type.INTEGER.nildumuType())
				.collect(Collectors.toList());
		l.getAllWrittenToArrays()
				.forEach(a -> elementTypes.add(edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type.ARRAY.nildumuType()));
		Type returnType = edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type.nTypes.getOrCreateTupleType(elementTypes);

		// recursive call to loop method
		Parser.MethodInvocationNode recCallExpr = new Parser.MethodInvocationNode(DUMMY_LOCATION, methodName(l), args);
		Parser.MultipleVariableAssignmentNode recCall = new Parser.MultipleVariableAssignmentNode(DUMMY_LOCATION,
				Arrays.stream(result.returnVars).mapToObj(s -> varName(s, l.getOwner())).toArray(String[]::new),
				new Parser.UnpackOperatorNode(recCallExpr));
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
		// TODO do we need to invert loop condition ???
		Parser.IfStatementNode if_ = new Parser.IfStatementNode(DUMMY_LOCATION, loopCond, body);
		completeBody.add(if_);

		// add phi-stmts + return stmt
		result.method = new Parser.MethodNode(DUMMY_LOCATION, methodName(l), returnType, param, completeBody,
				new Parser.GlobalVariablesNode(DUMMY_LOCATION, new HashMap<>()));
		Parser.MethodInvocationNode callToLoop = new Parser.MethodInvocationNode(DUMMY_LOCATION, methodName(l),
				arguments(result.callArgs()));
		completeBody.add(new Parser.ReturnStatementNode(DUMMY_LOCATION,
				Arrays.stream(result.returnVars).mapToObj(i -> varName(i, l.getOwner())).map(Converter::variableAccess)
						.collect(Collectors.toList())));

		result.call = new Parser.MultipleVariableAssignmentNode(DUMMY_LOCATION,
				Arrays.stream(result.returnVars).mapToObj(i -> Converter.varName(i, l.getOwner()))
						.toArray(String[]::new), new Parser.UnpackOperatorNode(callToLoop));
		result.returnDefs = new HashMap<>();
		IntStream.range(0, phiMap.size()).forEach(i -> result.returnDefs.put(result.returnVars[i],
				varDecl(varName(result.returnVars[i], l.getOwner()), returnType.getBracketAccessResult(i))));
		return result;
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
		ConversionVisitor cVis = new ConversionVisitor(this, m, params);
		return new Parser.BlockNode(DUMMY_LOCATION,
				convertStatements(CFGUtil.topological(m.getCFG().getBlocks(), m.getCFG().entry()), cVis));
	}

	public List<Parser.StatementNode> convertStatements(List<BasicBlock> toConvert, ConversionVisitor cVis) {
		Pair<List<Parser.StatementNode>, Map<Integer, Parser.VariableDeclarationNode>> convertedPair = convertStatementsRec(
				toConvert, new ArrayList<>(), new HashMap<>(), cVis);
		return Util.prepend(convertedPair.fst, new ArrayList<>(convertedPair.snd.values()));

	}

	private Pair<List<Parser.StatementNode>, Map<Integer, Parser.VariableDeclarationNode>> convertStatementsRec(
			List<BasicBlock> toConvert, List<Parser.StatementNode> converted,
			Map<Integer, Parser.VariableDeclarationNode> varDecls, ConversionVisitor cVis) {

		if (toConvert.isEmpty())
			return Pair.make(converted, varDecls);

		BasicBlock b = toConvert.remove(0);

		if (b.isLoopHeader()) {
			LoopBody loop = b.getCFG().getMethod().getLoops().stream().filter(l -> l.getHead().idx() == b.idx())
					.findFirst().get();
			NildumuProgram.LoopMethod convertedMethod = convertLoop(loop);
			loopMethods.put(loop, convertedMethod);
			converted.add(convertedMethod.call);
			varDecls.putAll(convertedMethod.returnDefs);
			toConvert.removeAll(loop.getBlocks());
			for (BasicBlock bb : loop.getBreaks()) {
				toConvert.removeAll(loop.breakToPostLoop(bb));
			}
			return convertStatementsRec(toConvert, converted, varDecls, cVis);
		}

		Pair<List<Parser.StatementNode>, Map<Integer, Parser.VariableDeclarationNode>> blockStmts = cVis.visitBlock(b);
		converted.addAll(blockStmts.fst);
		varDecls.putAll(blockStmts.snd);

		if (b.isCondHeader() && !b.isBreak()) {
			BasicBlock trueTarget = b.getCFG().getBlock(b.getTrueTarget());
			BasicBlock falseTarget = b.succs().stream().filter(bb -> bb.idx() != b.getTrueTarget()).findAny().get();
			List<BasicBlock> trueBranch = computeConditionalBranch(b, trueTarget);
			List<BasicBlock> falseBranch = computeConditionalBranch(b, falseTarget);
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

	private List<Parser.StatementNode> convertSecretInputs(Program p) {
		int[] paramValNums = Arrays.copyOfRange(p.getEntryMethod().getIr().getParameterValueNumbers(), 1,
				p.getEntryMethod().getIr().getNumberOfParameters());
		List<Parser.StatementNode> inputs = convertToSecretInput(paramValNums, p.getEntryMethod());
		for (int i = 0; i < paramValNums.length; i++) {
			this.inputs.put(paramValNums[i], (Parser.InputVariableDeclarationNode) inputs.get(i));
		}
		return inputs;
	}

	public List<Parser.StatementNode> convertToSecretInput(int[] valNums, Method m) {
		List<Parser.StatementNode> inputs = new ArrayList<>();
		for (int valNum : valNums) {
			if (m.getValue(valNum).getType().equals(edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type.INTEGER)) {
				Parser.InputVariableDeclarationNode decl = new Parser.InputVariableDeclarationNode(DUMMY_LOCATION,
						varName(valNum, m), m.getValue(valNum).getType().nildumuType(),
						new Parser.IntegerLiteralNode(DUMMY_LOCATION,
								Lattices.ValueLattice.get().parse(unknownLiteral(options.intWidth))), "h");
				inputs.add(decl);

			} else {
				assert (m.getValue(valNum).isArrayType());
				Array<? extends Value> arr = (Array<? extends Value>) m.getValue(valNum);
				inputs.add(varDecl(varName(valNum, m), arr.getType().nildumuType()));

				for (int i = 0; i < arr.length(); i++) {
					String varname = varName();
					Parser.InputVariableDeclarationNode decl = new Parser.InputVariableDeclarationNode(DUMMY_LOCATION,
							varname, arr.elementType().nildumuType(), new Parser.IntegerLiteralNode(DUMMY_LOCATION,
							Lattices.ValueLattice.get().parse(unknownLiteral(options.intWidth))), "h");
					inputs.add(decl);
					inputs.add(new Parser.ArrayAssignmentNode(DUMMY_LOCATION, varName(valNum, m),
							new Parser.IntegerLiteralNode(DUMMY_LOCATION, Lattices.ValueLattice.get().parse(i)),
							variableAccess(varname)));
				}
			}
		}
		return inputs;
	}

	private String unknownLiteral(int length) {
		return "0b" + String.join("", Collections.nCopies(length, "u"));
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

	public List<Parser.StatementNode> convertToPublicOutput(int[] returnVars, Method m) {
		List<Parser.StatementNode> outputs = new ArrayList<>();
		for (int valNum : returnVars) {
			if (m.getValue(valNum).getType().equals(edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type.INTEGER)) {
				Parser.OutputVariableDeclarationNode out = new Parser.OutputVariableDeclarationNode(DUMMY_LOCATION,
						varName(), m.getValue(valNum).getType().nildumuType(), variableAccess(varName(valNum, m)), "l");
				outputs.add(out);
			} else {
				assert (m.getValue(valNum).isArrayType());
				Array<? extends Value> arr = (Array<? extends Value>) m.getValue(valNum);
				for (int i = 0; i < arr.length(); i++) {
					String varname = varName();
					outputs.add(new Parser.OutputVariableDeclarationNode(DUMMY_LOCATION, varname,
							arr.elementType().nildumuType(),
							new Parser.BracketedAccessOperatorNode(variableAccess(varName(arr.getValNum(), m)),
									new Parser.IntegerLiteralNode(DUMMY_LOCATION,
											Lattices.ValueLattice.get().parse(i))), "l"));
				}
			}
		}
		return outputs;
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

	public static String varName(int valNum, Method m) {
		String methodId = m.identifier().replaceAll("[\\.\\(\\)]", "_");
		String loopId = "";
		if (m.isComputedInLoop(valNum)) {
			LoopBody loop = m.getLoops().stream().filter(l -> l.producesValNum(valNum)).findFirst().get();
			loopId = "_" + methodName(loop);
		}
		return "v_" + /*methodId + loopId + */ "_" + valNum;
	}

	public static int valNum(String varName) {
		assert (varName.startsWith("v"));
		String[] nameParts = varName.split("_");
		return Integer.parseInt(nameParts[nameParts.length - 1]);
	}

	public static String varName() {
		return "x" + varNameCounter++;
	}

	public static List<String> arrayVarName(int valNum, Method method, int idx) {
		int arrayLength = edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type.ARRAY.bitwidth();
		String base = "__bl_" + varName(valNum, method) + "_";
		String idxStr = (idx == 0) ? "" : String.valueOf(idx);
		return IntStream.range(0, arrayLength).mapToObj(i -> base + i + idxStr).collect(Collectors.toList());
	}

	public static Parser.ArgumentsNode arguments(List<String> varNames) {
		return new Parser.ArgumentsNode(DUMMY_LOCATION,
				varNames.stream().map(Converter::variableAccess).collect(Collectors.toList()));
	}

	public Parser.ParameterNode parameter(String varName, Type type) {
		return new Parser.ParameterNode(DUMMY_LOCATION, type, varName);
	}

	public Parser.ParametersNode parameters(List<Pair<String, Type>> varNames) {
		return new Parser.ParametersNode(DUMMY_LOCATION,
				varNames.stream().map(p -> parameter(p.fst, p.snd)).collect(Collectors.toList()));
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
		String sig = signature.replaceAll("[\\(\\)\\[\\]$&+.,:;=?@#|]", "_");
		return sig;
	}

	public static String methodName(LoopBody l) {
		return "m" + l.getOwner().identifierNoSpecialCharacters() + "_" + l.getHead().idx();
	}
}