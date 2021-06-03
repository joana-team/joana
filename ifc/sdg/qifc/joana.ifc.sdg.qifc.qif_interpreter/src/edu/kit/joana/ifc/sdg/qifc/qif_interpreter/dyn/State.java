package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo.LinearSegment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo.ProgramSegment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class State {

	public ProgramSegment pSeg;
	public LinearSegment currentSegment;

	public List<Integer> visited;
	public Queue<BBlock> toVisit;

	public BBlock reentry;

	private State(Program p) {
		this.pSeg = new ProgramSegment(p);
		currentSegment = LinearSegment.empty(this.pSeg);

		visited = new ArrayList<>();
		toVisit = new ArrayDeque<>();
		reentry = p.getEntryMethod().getCFG().entry();
	}

	public static State init(Program p) {
		return new State(p);
	}

}