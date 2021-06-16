package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ProgramPart;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.State;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.SATVisitor;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.LoopBody;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.Environment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.CFGUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LoopSegment extends Segment<LoopBody> implements IStaticAnalysisSegment {

	public LoopSegment(LoopBody loop, Segment<? extends ProgramPart> parent) {
		super(loop, parent);
		List<BasicBlock> toSegment = CFGUtil.topological(this.getBlocks(), this.programPart.getHead());
		toSegment.remove(this.programPart.getHead());
		this.children = segment(toSegment);
		this.inputs = new ArrayList<>(loop.getIn().keySet());
		loop.setSegment(this);
	}

	@Override public State computeSATDeps(State state, Method m, SATVisitor sv) {
		return null;
	}

	@Override public boolean owns(BasicBlock block) {
		return this.programPart.hasBlock(block.idx());
	}

	@Override public void finalize() {
		this.inputs = new ArrayList<>(this.programPart.getIn().keySet());
		this.outputs = new ArrayList<>(this.programPart.getResultMapping().values());
	}

	@Override public Set<BasicBlock> getBlocks() {
		return new HashSet<>(programPart.getBlocks());
	}

	public State dynamic(State state) {

		return state;
	}

	@Override public String getLabel() {
		return this.rank + "\n" + "Loop " + this.programPart.getHead().idx() + "\n" + this.dynAnaFeasible;
	}

	@Override public double channelCap(Environment env) {
		return 0;
	}
}