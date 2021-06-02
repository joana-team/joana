package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu;

import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.LoopBody;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import nildumu.Context;
import nildumu.MinCut;
import nildumu.Parser;
import nildumu.Processor;
import nildumu.mih.MethodInvocationHandler;
import nildumu.typing.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
	public Map<LoopBody, LoopMethod> loopMethods;

	public NildumuProgram(Parser.ProgramNode p, NildumuOptions options, Map<LoopBody, LoopMethod> loopMethods) {
		this.options = options;
		this.context = Processor.process(p.toPrettyString(), MODE, MethodInvocationHandler.parse(handler), OPTS);
		//Map<Lattices.Sec<?>, MinCut.ComputationResult> leakageResult = context.computeLeakage(MinCut.usedAlgo);
		//System.out.println("Leakage: " + leakageResult.get(Lattices.BasicSecLattice.LOW).maxFlow);
		this.ast = p;
		this.loopMethods = loopMethods;
	}

	public static class LoopMethod {
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
}