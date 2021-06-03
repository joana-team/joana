package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;

import java.util.List;

public abstract class ProgramPart {

	public static class LinearProgramPart extends ProgramPart {
		public List<BBlock> blocks;

		public LinearProgramPart(List<BBlock> blocks) {
			this.blocks = blocks;
		}
	}
}