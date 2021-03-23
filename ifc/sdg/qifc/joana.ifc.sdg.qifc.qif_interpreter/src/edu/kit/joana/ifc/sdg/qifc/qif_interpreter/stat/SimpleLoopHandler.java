package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.ISATAnalysisFragment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.LoopBody;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.OutOfScopeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

public class SimpleLoopHandler {

	public static LoopBody analyze(Method m, BBlock head, SATVisitor sv) throws OutOfScopeException {

		assert(head.isLoopHeader());
		LoopBody loop = new LoopBody(m, head);

		Queue<BBlock> toVisit = new ArrayDeque<>();
		toVisit.add(head);

		while (!toVisit.isEmpty()) {
			BBlock b = toVisit.poll();

			sv.visitBlock(m, b, -1);

			for (BBlock succ: b.succs()) {
				if (loop.getBlocks().contains(succ)) {
					toVisit.add(succ);
				}
			}
		}
		extractDeps(m, head, loop);
		return loop;
	}

	private static void extractDeps(Method m, BBlock head, LoopBody loop) {
		Iterator<ISSABasicBlock> orderedPredsIter = head.getCFG().getWalaCFG().getPredNodes(head.getWalaBasicBLock());

		// find position i of the phi-use that we need
		int loopOutValNum = 0;
		while (orderedPredsIter.hasNext()) {
			int blockNum = orderedPredsIter.next().getNumber();
			if (loop.getBlocks().stream().anyMatch(b -> b.idx() == blockNum)) {
				break;
			}
			loopOutValNum++;
		}

		// copy deps to loop
		for (SSAInstruction i: head.instructions()) {
			if (i instanceof SSAPhiInstruction) {
				loop.addInDeps(i.getDef(), m.getDepsForValue(i.getDef()));
				loop.addOutDeps(i.getDef(), m.getDepsForValue(loopOutValNum));

				// create new vars to use for post-loop analysis
				Formula[] newVars = LogicUtil.createVars(i.getDef(), m.getDepsForValue(i.getDef()).length, "x");
				m.setDepsForvalue(i.getDef(), newVars);
			}
		}


	}
}
