package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ProgramPart;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.State;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.LoopBody;

public class LoopSegment extends Segment<LoopBody> {

	public LoopSegment(LoopBody loop, Segment<? extends ProgramPart> parent) {
		super(loop, parent);

		this.inputs = loop.getIn();
	}

	@Override public void finalize() {

	}

	public State dynamic(State state) {

		return state;
	}
}