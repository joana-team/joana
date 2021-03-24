package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Substitution;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.util.*;
import java.util.stream.IntStream;

public class LoopBody {

	private final Map<Integer, Formula[]> in;
	private final Map<Integer, Formula[]> beforeLoop;
	private final Map<Integer, Formula[]> out;
	private final Map<Integer, Integer> mapping;
	private final Map<Integer, Map<Integer, Formula[]>> runs;
	private final Method owner;
	private final BBlock head;
	private final Set<BBlock> blocks;
	private Substitution initialVals;

	public LoopBody(Method owner, BBlock head) {
		this.owner = owner;
		this.in = new HashMap<>();
		this.out = new HashMap<>();
		this.runs = new HashMap<>();
		this.mapping = new HashMap<>();
		this.beforeLoop = new HashMap<>();
		this.head = head;
		this.blocks = this.owner.getCFG().getBasicBlocksInLoop(head);
	}

	public Set<BBlock> getBlocks() {
		return blocks;
	}

	public BBlock getHead() {
		return head;
	}

	public void addInDeps(int i, Formula[] deps) {
		this.in.put(i, deps);
	}

	public void addOutDeps(int in, int out) {
		this.mapping.put(in, out);
		this.out.put(in, owner.getDepsForValue(out));
	}

	public void addBeforeLoopDeps(int in, Formula[] deps) {
		this.beforeLoop.put(in, deps);
	}

	public void print() {
		StringBuilder sb = new StringBuilder("in:").append(System.lineSeparator());
		for (int i : in.keySet()) {
			sb = sb.append(i).append(" ").append(Arrays.toString(in.get(i))).append(System.lineSeparator());
		}
		sb = sb.append("out:").append(System.lineSeparator());
		for (int i : out.keySet()) {
			sb = sb.append(i).append(" ").append(Arrays.toString(out.get(i))).append(System.lineSeparator());
		}
		System.out.println(sb.toString());
	}

	public Map<Integer, Formula[]> getRun(int n) {
		if (!runs.containsKey(n)) {
			computeRunsUpTo(n);
		}
		return runs.get(n);
	}

	private void computeRunsUpTo(int n) {
		int currMax = (runs.keySet().isEmpty()) ? 0 : Collections.max(runs.keySet());
		while (currMax <= n) {
			if (currMax == 0) {
				runs.put(0, this.beforeLoop);
			} else {
				Map<Integer, Formula[]> runBefore = this.runs.get(currMax - 1);
				Substitution sub = new Substitution();
				for (Integer i : this.in.keySet()) {
					for (int j = 0; j < this.in.get(i).length; j++) {
						sub.addMapping((Variable) this.in.get(i)[j], runBefore.get(i)[j]);
					}
				}
				Map<Integer, Formula[]> run = new HashMap<>();
				for (Integer i : this.in.keySet()) {
					Formula[] res = new Formula[this.in.get(i).length];
					for (int j = 0; j < this.in.get(i).length; j++) {
						res[j] = this.out.get(i)[j].substitute(sub.toLogicNGSubstitution());
					}
					run.put(i, res);
				}
				runs.put(currMax, run);
			}
			currMax++;
		}
	}

	private Substitution loopSub() {
		Substitution sub = new Substitution();

		for (int i: this.in.keySet()) {
			assert(this.out.containsKey(i));
			Formula[] in = this.in.get(i);
			Formula[] out = this.out.get(i);
			IntStream.range(0, in.length).forEach(j -> sub.addMapping((Variable)in[j], out[j]));
		}

		return sub;
	}

	public void printRun(int n) {
		System.out.println(n);
		Map<Integer, Formula[]> run = runs.get(n);
		if (run != null) {
			for (int i : in.keySet()) {
				System.out.println(i + " " + Arrays.toString(run.get(i)));
			}
		}
	}

	public boolean producesValNum(int valNum) {
		return this.in.containsKey(valNum);
	}

	public Formula getImplicitFlowForIter(int iter) {
		this.computeRunsUpTo(iter);

		if (iter == 0) {
			Optional<BBlock> outSideLoopPred = head.preds().stream()
					.filter(b -> !(owner.getCFG().isDominatedBy(b, head))).findFirst();
			assert (outSideLoopPred.isPresent());
			return outSideLoopPred.get().generateImplicitFlowFormula();
		} else {
			Formula beforeLoop = head.generateImplicitFlowFormula();
			Formula loopCond = head.getCondExpr();
			boolean jumpInLoopIfCond = blocks.stream().filter(b -> head.succs().contains(b)).findFirst().get()
					.getImplicitFlows().stream().filter(p -> p.fst == head.idx()).findFirst().get().snd;
			Map<Integer, Formula[]> lastIter = this.runs.get(iter - 1);

			Substitution sub = new Substitution();

			for (int i : lastIter.keySet()) {
				for (int j = 0; j < lastIter.get(i).length; j++) {
					assert (this.in.containsKey(i));
					if (loopCond.containsVariable((Variable) this.in.get(i)[j])) {
						sub.addMapping((Variable) this.in.get(i)[j], lastIter.get(i)[j]);
					}
				}
			}
			Formula implicitFlow = (jumpInLoopIfCond) ?
					loopCond.substitute(sub.toLogicNGSubstitution()) :
					LogicUtil.ff.not(loopCond.substitute(sub.toLogicNGSubstitution()));
			return LogicUtil.ff.and(beforeLoop, implicitFlow);
		}
	}

	public void generateInitialValueSubstitution() {
		this.initialVals = new Substitution();

		for (int i : this.in.keySet()) {
			for (int j = 0; j < this.in.get(i).length; j++) {
				initialVals.addMapping((Variable) this.in.get(i)[j], this.beforeLoop.get(i)[j]);
			}
		}
	}
}
