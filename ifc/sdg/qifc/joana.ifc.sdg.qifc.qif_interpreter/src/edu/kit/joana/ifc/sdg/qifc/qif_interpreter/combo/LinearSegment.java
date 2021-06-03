package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ProgramPart;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.State;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.SATVisitor;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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

	public void finalize() {

	}

	@Override public Set<BasicBlock> getBlocks() {
		return new HashSet(this.programPart.blocks);
	}

	public void addBlock(BasicBlock b) {
		this.programPart.blocks.add(b);
		b.setSegment(this);
	}

	@Override public String getLabel() {
		return this.programPart.blocks.stream().map(b -> String.valueOf(b.idx())).reduce("", (s, str) -> s + " " + str);
	}
}