package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo.ConditionalSegment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo.ContainerSegment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo.LinearSegment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo.ProgramSegment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.CFGUtil;

import java.util.ArrayList;
import java.util.List;

public class State {

	public ProgramSegment pSeg;
	public LinearSegment currentSegment;

	// global lists of basic blocks we have already visited / still need to visit -- not restricted to locks that belong to the current segment
	public List<Integer> visited;
	public List<BasicBlock> toVisit;

	public BasicBlock reentry;

	private State(Program p) {
		this.pSeg = new ProgramSegment(p);
		currentSegment = LinearSegment.newEmpty(this.pSeg);

		visited = new ArrayList<>();
		toVisit = CFGUtil.topological(p.getEntryMethod().getCFG().getBlocks(), p.getEntryMethod().getCFG().entry());
		reentry = p.getEntryMethod().getCFG().entry();
	}

	public static State init(Program p) {
		return new State(p);
	}

	public State startNewConditionalSegment(BasicBlock condHeader) {
		ConditionalSegment condStmt = new ConditionalSegment(currentSegment.parent, condHeader.getCondExpr(),
				condHeader);
		ContainerSegment ifBranch = condStmt.getIfBranch();
		this.currentSegment = LinearSegment.newEmpty(ifBranch);
		return this;
	}

}