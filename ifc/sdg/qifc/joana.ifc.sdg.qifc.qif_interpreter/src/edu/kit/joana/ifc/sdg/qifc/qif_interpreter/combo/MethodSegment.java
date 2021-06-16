package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ProgramPart;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.State;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.SATVisitor;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.Environment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.CFGUtil;
import nildumu.Parser;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MethodSegment extends Segment<Method> implements IStaticAnalysisSegment {

	SSAInvokeInstruction call;
	Method caller;

	public MethodSegment(Method m, Segment<? extends ProgramPart> parent, SSAInvokeInstruction instruction,
			Method caller) {
		super(m, parent);
		this.call = instruction;
		this.caller = caller;

		m.addSegment(instruction, this);

		if (this.level < 5) {
			this.children = segment(
					new ArrayList<>(CFGUtil.topological(this.getBlocks(), this.programPart.getCFG().entry())));
		}
	}

	@Override public State computeSATDeps(State state, Method m, SATVisitor sv) {
		return null;
	}

	@Override public boolean owns(BasicBlock block) {
		return this.programPart.getCFG().getBlocks().contains(block);
	}

	@Override public void finalize() {
		this.inputs = programPart.getProgramValues().keySet().stream()
				.filter(e -> programPart.getValue(e).isParameter()).collect(Collectors.toList());
		this.outputs = Arrays.stream(this.programPart.getIr().getInstructions())
				.filter(i -> i instanceof SSAReturnInstruction).map(i -> ((SSAReturnInstruction) i).getResult())
				.collect(Collectors.toList());
	}

	@Override public Set<BasicBlock> getBlocks() {
		return new HashSet<>(this.programPart.getCFG().getBlocks());
	}

	@Override public String getLabel() {
		return this.rank + "\n" + "Method " + this.programPart.identifier() + "\n" + this.dynAnaFeasible;
	}

	@Override public double channelCap(Environment env) {
		Map<Integer, String> args = new HashMap<>();
		IntStream.range(1, this.call.getNumberOfUses()).forEach(i -> args.put(this.programPart.getIr().getParameter(i),
				Value.BitLatticeValue.toStringLiteral(caller.getValue(call.getUse(i)))));

		Parser.ProgramNode p = env.nProgram.fromMethod(this.caller, this.programPart, call, args);
		return env.nProgram.computeCC(p);
	}
}