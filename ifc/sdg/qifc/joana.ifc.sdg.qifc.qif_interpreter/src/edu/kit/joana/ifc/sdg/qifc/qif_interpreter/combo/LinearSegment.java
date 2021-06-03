package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ProgramPart;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;

import java.util.ArrayList;

public class LinearSegment extends Segment<ProgramPart.LinearProgramPart> {

	public LinearSegment(ProgramPart.LinearProgramPart pp, Segment<? extends ProgramPart> parent) {
		super(pp, parent);
	}

	public static LinearSegment empty(Segment<? extends ProgramPart> parent) {
		return new LinearSegment(new ProgramPart.LinearProgramPart(new ArrayList<>()), parent);
	}

	public void finalize() {

	}

	public void addBlock(BBlock b) {
		this.programPart.blocks.add(b);
	}
}