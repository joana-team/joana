package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu;

import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import nildumu.*;
import nildumu.mih.MethodInvocationHandler;
import nildumu.typing.Type;

import java.util.*;
import java.util.stream.Collectors;

/**
 * wrapper for Nildumu representation of program (or program parts respectively) + additional information about inputs 7 outputs we might have from previous analyses on the program
 */
public class NildumuProgram {

	private static final String handler = "handler=inlining;maxrec=32;bot=summary";
	public static boolean transformPlus = false;

	public static final int TRANSFORM_PLUS = 0b01;
	public static final int TRANSFORM_LOOPS = 0b010;
	public static final int RECORD_ALTERNATIVES = 0b0100;
	public static final int OPTS = TRANSFORM_LOOPS | (MinCut.usedAlgo.supportsAlternatives ? RECORD_ALTERNATIVES : 0);
	public static final Context.Mode MODE = Context.Mode.EXTENDED;

	public Context context;
	// public Parser.ProgramNode ast;
	public NildumuOptions options;
	public Parser.ProgramNode ast;
	public Map<String, ConvertedLoopMethod> loopMethods;
	public Map<String, ConvertedMethod> methods;

	public NildumuProgram(Parser.ProgramNode p, NildumuOptions options, Map<String, ConvertedLoopMethod> loopMethods,
			Map<String, ConvertedMethod> methods) {
		this.options = options;
		this.context = Processor.process(p.toPrettyString(), MODE, MethodInvocationHandler.parse(handler), OPTS);
		this.ast = p;
		this.loopMethods = loopMethods;
		this.methods = methods;
	}

	public double computeMethodCallCC(Method caller, Method m, int[] args) {
		Parser.ProgramNode methodProgram = fromMethod(caller, m, args);
		Context methodContext = Processor
				.process(methodProgram.toPrettyString(), MODE, MethodInvocationHandler.parse(handler), OPTS);
		Map<Lattices.Sec<?>, MinCut.ComputationResult> leakageResult = methodContext.computeLeakage(MinCut.usedAlgo);
		return leakageResult.get(Lattices.BasicSecLattice.LOW).maxFlow;
	}

	public Parser.ProgramNode fromMethod(Method caller, Method m, int[] args) {
		Converter c = new Converter();
		ConvertedMethod base = methods.get(m.identifierNoSpecialCharacters());

		Map<Integer, String> inputLiterals = new HashMap<>();
		for (int i = 0; i < args.length; i++) {
			inputLiterals.put(m.getIr().getParameter(i + 1),
					Value.BitLatticeValue.toStringLiteral(caller.getValue(args[i])));
		}
		List<Parser.StatementNode> inVars = c
				.convertToSecretInput(Arrays.copyOfRange(m.getIr().getParameterValueNumbers(), 1, m.getParamNum()), m,
						inputLiterals);
		String varname = Converter.varName();
		ReplacementVisitor rv = new ReplacementVisitor(varname, m);
		Parser.BlockNode replacedReturns = new Parser.BlockNode(Converter.DUMMY_LOCATION, base.methodBody);
		replacedReturns.accept(rv);
		Parser.OutputVariableDeclarationNode output = new Parser.OutputVariableDeclarationNode(Converter.DUMMY_LOCATION,
				"o_ " + varname, m.getReturnType().nildumuType(),
				new Parser.VariableAccessNode(Converter.DUMMY_LOCATION, varname), "l");
		Parser.ProgramNode methodProgram = new Parser.ProgramNode(context);
		methodProgram.addGlobalStatements(inVars);
		methodProgram.addGlobalStatement(Converter.varDecl(varname, m.getReturnType().nildumuType()));
		methodProgram.addGlobalStatements(replacedReturns.statementNodes);
		methodProgram.addGlobalStatement(output);
		return methodProgram;
	}

	public static class ConvertedLoopMethod {
		public ArrayList<Parser.StatementNode> loopBody;
		public Method iMethod;
		Parser.MethodNode method;
		Map<Integer, Parser.VariableDeclarationNode> returnDefs;
		Parser.MultipleVariableAssignmentNode call;
		int[] callArgs;
		public int[] params;
		int[] recCallArgs;
		public int[] returnVars;

		List<String> recCallArgs() {
			return Arrays.stream(recCallArgs).mapToObj(i -> Converter.varName(i, iMethod)).collect(Collectors.toList());
		}

		List<String> callArgs() {
			return Arrays.stream(callArgs).mapToObj(i -> Converter.varName(i, iMethod)).collect(Collectors.toList());
		}

		List<Pair<String, Type>> params() {
			return Arrays.stream(params).mapToObj(
					i -> Pair.make(Converter.varName(i, iMethod), iMethod.getValue(i).getType().nildumuType()))
					.collect(Collectors.toList());
		}
	}

	public static class ConvertedMethod {
		public Method original;
		public List<Parser.StatementNode> methodBody;
		public int[] params;

		public ConvertedMethod(Method m, List<Parser.StatementNode> body) {
			this.original = m;
			this.params = Arrays.copyOfRange(m.getIr().getParameterValueNumbers(), 1, m.getParamNum());
			this.methodBody = body;
		}
	}
}