package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;

import java.util.List;
import java.util.Set;

public abstract class ProgramPart {

	public abstract Method getMethod();

	public static class LinearProgramPart extends ProgramPart {
		public List<BasicBlock> blocks;

		public LinearProgramPart(List<BasicBlock> blocks) {
			this.blocks = blocks;
		}

		@Override public Method getMethod() {
			return blocks.get(0).getCFG().getMethod();
		}
	}

	public static class Container extends ProgramPart {
		public Set<BasicBlock> blocks;

		public Container(Set<BasicBlock> blocks) {
			this.blocks = blocks;
		}

		@Override public Method getMethod() {
			return blocks.stream().findFirst().get().getCFG().getMethod();
		}
	}
}