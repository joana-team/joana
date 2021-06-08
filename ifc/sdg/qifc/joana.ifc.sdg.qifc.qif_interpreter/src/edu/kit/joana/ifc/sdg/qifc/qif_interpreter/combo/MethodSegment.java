package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ProgramPart;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.State;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.SATVisitor;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.CFGUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class MethodSegment extends Segment<Method> {

	public MethodSegment(Method m, Segment<? extends ProgramPart> parent, SSAInvokeInstruction instruction) {
		super(m, parent);

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
				.filter(e -> programPart.getValue(e).isParameter())
				.collect(Collectors.toMap(i -> i, i -> programPart.getDepsForValue(i)));
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
}