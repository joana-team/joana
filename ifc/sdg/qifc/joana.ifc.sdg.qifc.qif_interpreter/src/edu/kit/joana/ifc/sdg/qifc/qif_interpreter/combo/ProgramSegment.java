package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.State;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.SATVisitor;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotGraph;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotNode;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.CFGUtil;

import java.util.*;

public class ProgramSegment extends Segment<Program> implements DotGraph {

	public ProgramSegment(Program p) {
		super();
		this.level = 0;
		this.parent = null;
		this.programPart = p;
		this.children = new ArrayList<>();
		this.outputs = new ArrayList<>();
		this.arrayOutputs = new ArrayList<>();
		this.arrayInputs = Collections.emptyMap();

		this.children = segment(
				CFGUtil.topological(p.getEntryMethod().getCFG().getBlocks(), p.getEntryMethod().getCFG().entry()));
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
		return "Program";
	}

	@Override public DotNode getRoot() {
		return this;
	}

	@Override public List<DotNode> getNodes() {
		return this.getNodesRec(new ArrayList<>(Collections.singletonList(this)));
	}

	@Override public String getName() {
		return programPart.getClassName() + "_Segmentation";
	}

}