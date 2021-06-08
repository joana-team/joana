package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.Environment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.CFGUtil;

import java.util.ArrayList;
import java.util.List;

public class State {

	// global lists of basic blocks we have already visited / still need to visit -- not restricted to locks that belong to the current segment
	public List<Integer> visited;
	public List<BasicBlock> toVisit;

	public BasicBlock current;
	public Environment env;

	private State(Environment env, Method m) {
		this.env = env;
		visited = new ArrayList<>();
		toVisit = CFGUtil.topological(m.getCFG().getBlocks(), m.getCFG().entry());
		current = m.getCFG().entry();
	}

	public static State init(Environment env, Method m) {
		return new State(env, m);
	}

}