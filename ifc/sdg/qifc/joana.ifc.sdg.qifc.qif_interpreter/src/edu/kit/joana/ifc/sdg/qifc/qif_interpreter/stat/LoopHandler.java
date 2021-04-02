package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.LoopBody;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.OutOfScopeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LoopHandler {

	// temporary
	private static final int loopUnrollingMax = 4;

	public static LoopBody analyze(LoopBody loop, SATVisitor sv) {
		BBlock head = loop.getHead();
		Method m = loop.getOwner();

		assert (head.isLoopHeader());

		List<Integer> visited = new ArrayList<>();
		Queue<BBlock> toVisit = new ArrayDeque<>();
		toVisit.add(head.succs().stream().filter(loop.getBlocks()::contains).findFirst().get());
		visited.add(head.idx());

		while (!toVisit.isEmpty()) {
			BBlock b = toVisit.poll();
			visited.add(b.idx());

			try {
				sv.visitBlock(m, b, -1);
			} catch (OutOfScopeException e) {
				e.printStackTrace();
			}

			if (b.isLoopHeader() && !b.equals(head)) {
				LoopBody l = new LoopBody(m, b);
				m.addLoop(l);
				LoopHandler.analyze(l, sv);
				toVisit.addAll(
						b.succs().stream().filter(succ -> !l.getBlocks().contains(succ)).collect(Collectors.toList()));
			} else {
				for (BBlock succ : b.succs()) {
					if (loop.getBlocks().contains(succ) && !succ.equals(head) && (succ.isLoopHeader() || succ.preds()
							.stream().mapToInt(BBlock::idx).allMatch(visited::contains))) {
						toVisit.add(succ);
					}
				}
			}
		}

		extractDeps(m, head, loop);

		Map<Integer, Formula[]> previousRun = new HashMap<>();

		for (int i : m.getProgramValues().keySet()) {
			Formula[] before = new Formula[m.getDepsForValue(i).length];
			IntStream.range(0, before.length).forEach(k -> before[k] = (loop.getIn().containsKey(i)) ?
					loop.getBeforeLoop(i)[k] :
					m.getDepsForValue(i)[k]);
			previousRun.put(i, before);
		}

		List<Pair<Map<Integer, Formula[]>, Formula>> runs = new ArrayList<>();
		runs.add(Pair.make(previousRun,
				loop.getStayInLoop().substitute(loop.generateInitialValueSubstitution().toLogicNGSubstitution())));

		for (int i = 1; i < loopUnrollingMax; i++) {
			runs.add(loop.simulateRun(runs.get(i - 1).fst));
		}

		Pair<Map<Integer, Formula[]>, Formula> last = runs.get(loopUnrollingMax - 1);
		last.fst.keySet().forEach(i -> m.setDepsForvalue(i, last.fst.get(i)));

		for (int i = loopUnrollingMax - 2; i >= 0; i--) {
			Pair<Map<Integer, Formula[]>, Formula> run = runs.get(i);
			// TODO this is where my initial values get replaced
			loop.getIn().keySet().forEach(
					j -> m.setDepsForvalue(j, LogicUtil.ternaryOp(run.snd, run.fst.get(j), m.getDepsForValue(j))));
		}
		return loop;
	}

	private static void extractDeps(Method m, BBlock head, LoopBody loop) {
		Iterator<ISSABasicBlock> orderedPredsIter = head.getCFG().getWalaCFG()
				.getPredNodes(head.getWalaBasicBlock());

		// find position i of the phi-use that we need
		int argNum = 0;
		while (orderedPredsIter.hasNext()) {
			int blockNum = orderedPredsIter.next().getNumber();
			if (loop.getBlocks().stream().anyMatch(b -> b.idx() == blockNum)) {
				break;
			}
			argNum++;
		}

		// copy deps to loop
		for (SSAInstruction i: head.instructions()) {
			if (i instanceof SSAPhiInstruction) {
				loop.addInDeps(i.getDef(), m.getVarsForValue(i.getDef()));
				loop.addOutDeps(i.getDef(), i.getUse(argNum));
				loop.addBeforeLoopDeps(i.getDef(), m.getDepsForValue(i.getUse(1 - argNum)));
			}
		}
		loop.generateInitialValueSubstitution();
	}
}
