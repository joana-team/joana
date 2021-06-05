package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.State;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.SATVisitor;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotGraph;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotNode;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.CFGUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ProgramSegment extends Segment<Program> implements DotGraph {

	public static ProgramSegment create(Program p) {
		ProgramSegment programPart = skeleton(p);
		programPart.children = programPart.segment(
				CFGUtil.topological(p.getEntryMethod().getCFG().getBlocks(), p.getEntryMethod().getCFG().entry()));
		return programPart;
	}

	private ProgramSegment() {
		super();
	}

	public static ProgramSegment skeleton(Program p) {
		ProgramSegment seg = new ProgramSegment();
		seg.level = 0;
		seg.parent = null;
		seg.programPart = p;
		seg.children = new ArrayList<>();
		seg.outputs = new ArrayList<>();
		seg.arrayOutputs = new ArrayList<>();
		seg.arrayInputs = Collections.emptyMap();
		return seg;
	}

	@Override public State computeSATDeps(State state, Method m, SATVisitor sv) {
		return null;
	}

	// all basic blocks belong to the program
	@Override public boolean owns(BasicBlock block) {
		return true;
	}

	@Override public void finalize() {
	}

	@Override public Set<BasicBlock> getBlocks() {
		Set<BasicBlock> allBlocks = new HashSet<>();
		this.programPart.getMethods().forEach(m -> allBlocks.addAll(m.getCFG().getBlocks()));
		return allBlocks;
	}

	@Override public String getLabel() {
		return this.rank + "\n" + "Program\n" + this.dynAnaFeasible;
	}

	@Override public DotNode getRoot() {
		return this;
	}

	@Override public Set<DotNode> getNodes() {
		return this.getNodesRec(new HashSet<>(Collections.singletonList(this)));
	}

	@Override public String getName() {
		String label = programPart.getClassName() + "_seg";
		if (this.collapsed) {
			label += "_collapsed";
		}
		return label;
	}

}