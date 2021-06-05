package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo.LinearSegment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo.ProgramSegment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo.Segment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.Environment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.CFGUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class State {

	public ProgramSegment pSeg;
	public Stack<Pair<Integer, Segment<? extends ProgramPart>>> currentSegment;

	// global lists of basic blocks we have already visited / still need to visit -- not restricted to locks that belong to the current segment
	public List<Integer> visited;
	public List<BasicBlock> toVisit;

	public BasicBlock next;

	private State(Environment env) {
		Program p = env.iProgram;
		visited = new ArrayList<>();
		toVisit = CFGUtil.topological(p.getEntryMethod().getCFG().getBlocks(), p.getEntryMethod().getCFG().entry());
		next = p.getEntryMethod().getCFG().entry();
		this.currentSegment = new Stack<>();
		this.currentSegment.push(Pair.make(0, env.segments));
		advance();
	}

	public static State init(Environment env) {
		return new State(env);
	}

	// updates the currentSegment stack to match {@code next}
	public void advance() {
		if (this.currentSegment.peek().snd.owns(next))
			return;

		Pair<Integer, Segment<? extends ProgramPart>> lastSeen = currentSegment.peek();

		while (!currentSegment.peek().snd.owns(next)) {
			lastSeen = currentSegment.pop();
		}
		currentSegment.push(Pair.make(lastSeen.fst + 1, currentSegment.peek().snd.children.get(lastSeen.fst)));

		while (!(currentSegment.peek().snd instanceof LinearSegment)) {
			currentSegment.push(Pair.make(0, currentSegment.peek().snd.children.get(0)));
		}
	}

	public Segment<? extends ProgramPart> currentSegment() {
		return this.currentSegment.peek().snd;
	}

}