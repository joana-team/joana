package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.LoopBody;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Logger;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Util;
import nildumu.*;
import nildumu.mih.MethodInvocationHandler;
import nildumu.typing.Type;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * wrapper for Nildumu representation of program (or program parts respectively) + additional information about inputs 7 outputs we might have from previous analyses on the program
 */
public class NildumuProgram {

	public static final String handler = "handler=inlining;maxrec=32;bot=summary";
	public static boolean transformPlus = false;

	public static final int TRANSFORM_PLUS = 0b01;
	public static final int TRANSFORM_LOOPS = 0b010;
	public static final int RECORD_ALTERNATIVES = 0b0100;
	public static final int OPTS = TRANSFORM_LOOPS;
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

	public double computeCC(Parser.ProgramNode p) {
		Logger.log(Level.INFO, "Nildumu invocation for: \n" + p.toPrettyString());
		Context methodContext = Processor
				.process(p.toPrettyString(), MODE, MethodInvocationHandler.parse(handler), OPTS);
		Map<Lattices.Sec<?>, LeakageAlgorithm.ComputationResult> leakageResult = methodContext
				.computeLeakage(LeakageAlgorithm.Algo.OPENWBO_GLUCOSE);
		return leakageResult.get(Lattices.BasicSecLattice.LOW).maxFlow;
	}

	public double computeCC(LoopBody l, Map<Integer, String> inputs) {
		Parser.ProgramNode methodProgram = fromLoop(l, l.getOwner(), inputs);
		Context methodContext = Processor
				.process(methodProgram.toPrettyString(), MODE, MethodInvocationHandler.parse(handler), OPTS);
		Map<Lattices.Sec<?>, LeakageAlgorithm.ComputationResult> leakageResult = methodContext
				.computeLeakage(LeakageAlgorithm.Algo.OPENWBO_GLUCOSE);
		return leakageResult.get(Lattices.BasicSecLattice.LOW).maxFlow;
	}

	public Parser.ProgramNode fromMethod(Method caller, Method m, SSAInvokeInstruction i, Map<Integer, String> inputs) {
		Converter c = new Converter();
		ConvertedMethod base = methods.get(m.identifierNoSpecialCharacters());

		int[] args = Arrays.copyOfRange(m.getIr().getParameterValueNumbers(), 1, m.getParamNum());
		List<Parser.StatementNode> stmts = c.convertToSecretInput(args, m, inputs);

		stmts.add(Converter.varDecl(Converter.varName(i.getDef(), caller), m.getReturnType().nildumuType()));
		stmts.add(Converter.assignment(Converter.varName(i.getDef(), caller),
				new Parser.MethodInvocationNode(Converter.DUMMY_LOCATION, base.complete, Converter.arguments(
						Arrays.stream(args).mapToObj(j -> Converter.varName(j, caller))
								.collect(Collectors.toList())))));

		stmts.addAll(c.convertToPublicOutput(new String[] { Converter.varName(i.getDef(), caller) },
				new edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type[] { m.getReturnType() }));

		Parser.ProgramNode p = new Parser.ProgramNode(context);
		p.addGlobalStatements(stmts);
		this.addMethodsAndLoopMethods(p, new ArrayList<>());
		return p;
	}

	public Parser.ProgramNode fromLoop(LoopBody l, Method m, Map<Integer, String> inputLiterals) {
		Converter c = new Converter();
		ConvertedLoopMethod loopMethod = loopMethods.get(Converter.methodName(l));

		List<Parser.StatementNode> stmts = c
				.convertToSecretInput(Util.removeDuplicates(loopMethod.callArgs), m, inputLiterals);
		String[] outputVars = Arrays.stream(loopMethod.returnVars).mapToObj(i -> Converter.varName())
				.toArray(String[]::new);
		IntStream.range(0, outputVars.length).forEach(i -> stmts
				.add(Converter.varDecl(outputVars[i], m.getValue(loopMethod.recCallArgs[i]).getType().nildumuType())));

		Parser.MethodNode singleRun = loopMethod.method;
		stmts.add(new Parser.MultipleVariableAssignmentNode(Converter.DUMMY_LOCATION, outputVars,
				new Parser.UnpackOperatorNode(new Parser.MethodInvocationNode(Converter.DUMMY_LOCATION, singleRun.name,
						Converter.arguments(Arrays.stream(loopMethod.callArgs).mapToObj(i -> Converter.varName(i, m))
								.collect(Collectors.toList()))))));

		edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type[] returnTypes = Arrays.stream(loopMethod.recCallArgs)
				.mapToObj(i -> m.getValue(i).getType())
				.toArray(edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type[]::new);

		stmts.addAll(c.convertToPublicOutput(outputVars, returnTypes));

		Parser.ProgramNode p = new Parser.ProgramNode(context);
		p.addGlobalStatements(stmts);
		p.addMethod(singleRun);
		this.addMethodsAndLoopMethods(p, Collections.singletonList(singleRun.name));
		return p;
	}

	void addMethodsAndLoopMethods(Parser.ProgramNode program, List<String> exclude) {
		for (ConvertedMethod m : this.methods.values()) {
			if (!exclude.contains(m.complete.name)) {
				program.addMethod(m.complete);
			}

		}
		for (ConvertedLoopMethod m : this.loopMethods.values()) {
			if (!exclude.contains(m.method.name)) {
				program.addMethod(m.method);
			}
		}
	}

	public static class ConvertedLoopMethod {
		LoopBody loop;
		public ArrayList<Parser.StatementNode> loopBody;
		public Method iMethod;
		Parser.MethodNode method;
		Map<Integer, Parser.VariableDeclarationNode> returnDefs;
		Parser.MultipleVariableAssignmentNode call;
		public int[] callArgs;
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

		Parser.MethodNode singleRun() {
			Parser.BlockNode body = new Parser.BlockNode(Converter.DUMMY_LOCATION, loopBody);
			List<Type> returnTypes = Arrays.stream(recCallArgs)
					.mapToObj(i -> iMethod.getValue(i).getType().nildumuType()).collect(Collectors.toList());
			Parser.ReturnStatementNode ret = new Parser.ReturnStatementNode(Converter.DUMMY_LOCATION,
					Arrays.stream(recCallArgs).mapToObj(i -> Converter.variableAccess(Converter.varName(i, iMethod)))
							.collect(Collectors.toList()));
			body.add(ret);
			return new Parser.MethodNode(Converter.DUMMY_LOCATION, Converter.methodName(loop),
					edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type.nTypes.getOrCreateTupleType(returnTypes),
					method.parameters, body, new Parser.GlobalVariablesNode(Converter.DUMMY_LOCATION, new HashMap<>()));
		}
	}

	public static class ConvertedMethod {
		public Parser.MethodNode complete;
		public Method original;
		public List<Parser.StatementNode> methodBody;
		public int[] params;

		public ConvertedMethod(Method m, List<Parser.StatementNode> body, Parser.MethodNode complete) {
			this.original = m;
			this.params = Arrays.copyOfRange(m.getIr().getParameterValueNumbers(), 1, m.getParamNum());
			this.methodBody = body;
			this.complete = complete;
		}
	}
}