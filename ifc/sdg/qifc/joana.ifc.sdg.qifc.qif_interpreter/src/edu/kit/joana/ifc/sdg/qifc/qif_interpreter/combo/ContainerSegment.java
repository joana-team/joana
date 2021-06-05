package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ProgramPart;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.State;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.SATVisitor;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ContainerSegment extends Segment<ProgramPart.Container> {

	public ContainerSegment(ProgramPart.Container c, Segment<? extends ProgramPart> parent, boolean segmentChildren) {
		super(c, parent);
		if (segmentChildren) {
			this.children = segment(new ArrayList<>(this.getBlocks()));
		}

	}

	public ContainerSegment(ProgramPart.Container c, Segment<? extends ProgramPart> parent) {
		this(c, parent, true);

	}

	@Override public State computeSATDeps(State state, Method m, SATVisitor sv) {
		for (Segment<? extends ProgramPart> c : this.children) {
			state = c.computeSATDeps(state, m, sv);
		}
		return state;
	}

	@Override public boolean owns(BasicBlock block) {
		return this.programPart.blocks.contains(block);
	}

	@Override public void finalize() {

	}

	@Override public Set<BasicBlock> getBlocks() {
		return new HashSet<>(this.programPart.blocks);
	}

	@Override public String getLabel() {
		return this.rank + "\n" + "Container";
	}
}