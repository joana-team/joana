package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;

import java.util.List;
import java.util.Set;

public abstract class ProgramPart {

	public static class LinearProgramPart extends ProgramPart {
		public List<BasicBlock> blocks;

		public LinearProgramPart(List<BasicBlock> blocks) {
			this.blocks = blocks;
		}
	}

	public static class Container extends ProgramPart {
		public Set<BasicBlock> blocks;

		public Container(Set<BasicBlock> blocks) {
			this.blocks = blocks;
		}
	}
}