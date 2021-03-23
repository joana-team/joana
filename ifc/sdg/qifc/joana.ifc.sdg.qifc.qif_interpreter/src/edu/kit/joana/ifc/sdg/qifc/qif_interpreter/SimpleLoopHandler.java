package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.LoopBody;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.OutOfScopeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.SATVisitor;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class SimpleLoopHandler {

	private final Map<Integer, Formula[]> pre;
	private final Map<Integer, Formula[]> in;
	private final Map<Integer, Formula[]> post;
	private final Map<Formula[], Formula[]> correspondence;

	private final Method m;
	private final BBlock head;

	private final SATVisitor standard;

	public SimpleLoopHandler(Method m, BBlock head) {
		post = new HashMap<>();
		in = new HashMap<>();
		pre = new HashMap<>();
		standard = new SATVisitor();
		this.m = m;
		this.head = head;
		correspondence = new HashMap<>();
	}

	private void loopHeader() throws OutOfScopeException {
		standard.visitBlock(m, head);
		m.getProgramValues().keySet().forEach(i -> pre.put(i, m.getValue(i).getDeps()));

		// sanity check
		for (SSAInstruction i: head.instructions()) {
			assert !(i instanceof SSAPhiInstruction) || (Arrays.stream(pre.get(i.getDef()))
					.allMatch(Formula::isAtomicFormula));
		}
	}

	// TODO: swap
	private void postLoopPrep() {
		for (SSAInstruction i: head.instructions()) {
			Formula[] newVars = new Formula[pre.get(i.getDef()).length];
			IntStream.range(0, newVars.length).forEach(j -> newVars[j] = LogicUtil.ff.variable("x" + i.getDef()+"::"+j));
			post.put(i.getDef(), newVars);
			m.setDepsForValnum(i.getDef(), newVars);
		}
	}

	private void inLoop() {
		SATVisitor sv = new SATVisitor();
		LoopBody body = new LoopBody(m, this);
	}

}
