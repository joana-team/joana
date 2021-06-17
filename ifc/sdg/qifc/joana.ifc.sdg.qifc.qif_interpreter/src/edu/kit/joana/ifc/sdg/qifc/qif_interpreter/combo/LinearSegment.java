package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ProgramPart;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.State;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.SATVisitor;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LinearSegment extends Segment<ProgramPart.LinearProgramPart> {

	public LinearSegment(ProgramPart.LinearProgramPart pp, Segment<? extends ProgramPart> parent) {
		super(pp, parent);
	}

	public static LinearSegment newEmpty(Segment<? extends ProgramPart> parent) {
		return new LinearSegment(new ProgramPart.LinearProgramPart(new ArrayList<>()), parent);
	}

	@Override public State computeSATDeps(State state, Method m, SATVisitor sv) {
		return null;
	}

	@Override public boolean owns(BasicBlock block) {
		return this.programPart.blocks.contains(block);
	}

	@Override public void finalize() {
		this.entry = this.programPart.blocks.get(0);
		Set<Integer> inputCandidates = new HashSet<>();
		Method m = this.entry.getCFG().getMethod();
		this.getBlocks().forEach(b -> inputCandidates.addAll(b.getPrimitiveBlockUses()));
		this.inputs = inputCandidates.stream().filter(i -> !m.getValue(i).isConstant()).collect(Collectors.toList());

		Set<Integer> outputCandidates = new HashSet<>();
		this.getBlocks().forEach(b -> outputCandidates.addAll(b.getPrimitiveBlockDefs()));
		if (this.getBlocks().stream().anyMatch(b -> !b.isDummy() && b.getWalaBasicBlock().isEntryBlock())) {
			outputCandidates.addAll(m.getProgramValues().keySet().stream().filter(i -> m.getValue(i).isParameter())
					.collect(Collectors.toList()));
		}
		List<Integer> list = new ArrayList<>();
		for (Integer i : outputCandidates) {
			if (m.getValue(i).influencesLeak() && !(m.getDef(i) instanceof SSAInvokeInstruction)) {
				if (!(m.getDef(i) instanceof SSAPhiInstruction && BasicBlock
						.getBBlockForInstruction(m.getDef(i), m.getCFG()).isLoopHeader())) {
					list.add(i);
				}
			}
		}
		this.outputs = list;

		this.inputs.removeAll(this.outputs);

	}

	@Override public Set<BasicBlock> getBlocks() {
		return new HashSet(this.programPart.blocks);
	}

	public void addBlock(BasicBlock b) {
		this.programPart.blocks.add(b);
		b.setSegment(this);
	}

	@Override public String getLabel() {
		return this.rank + "\n" + this.programPart.blocks.stream().map(b -> String.valueOf(b.idx()))
				.reduce("", (s, str) -> s + " " + str);
	}
}