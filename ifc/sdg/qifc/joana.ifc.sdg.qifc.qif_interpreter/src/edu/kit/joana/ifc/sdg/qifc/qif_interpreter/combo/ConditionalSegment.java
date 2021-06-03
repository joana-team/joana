package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ProgramPart;

public class ConditionalSegment extends Segment<ProgramPart.LinearProgramPart> {

	public ConditionalSegment(ProgramPart.LinearProgramPart pp, Segment<? extends ProgramPart> parent) {
		super(pp, parent);
	}

	@Override public void registerChild(Segment<? extends ProgramPart> newChild) {
		assert (newChild instanceof LinearSegment);
		super.registerChild(newChild);
		assert (this.children.size() <= 2);
	}

	@Override public void finalize() {
	}

	public LinearSegment getifBranch() {
		return (LinearSegment) this.children.get(0);
	}

	public LinearSegment getElseBranch() {
		return (LinearSegment) this.children.get(1);
	}

}