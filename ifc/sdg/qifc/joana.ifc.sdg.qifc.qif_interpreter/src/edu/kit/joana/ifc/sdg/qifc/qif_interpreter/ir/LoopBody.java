package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Substitution;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.util.*;
import java.util.stream.IntStream;

public class LoopBody {

	private final Map<Integer, Formula[]> in;
	private final Map<Integer, Formula[]> out;
	private final Map<Integer, Map<Integer, Formula[]>> runs;
	private final Method owner;
	private final BBlock head;
	private final Set<BBlock> blocks;

	public LoopBody(Method owner, BBlock head) {
		this.owner = owner;
		this.in = new HashMap<>();
		this.out = new HashMap<>();
		this.runs = new HashMap<>();
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

	public void addOutDeps(int i, Formula[] deps) {
		this.out.put(i, deps);
	}

	public void print() {
		StringBuilder sb = new StringBuilder("in:").append(System.lineSeparator());
		for (int i: in.keySet()) {
			sb = sb.append(i).append(" ").append(Arrays.toString(in.get(i))).append(System.lineSeparator());
		}
		sb = sb.append("out:").append(System.lineSeparator());
		for (int i: out.keySet()) {
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
				this.runs.put(0, this.in);
			} else if (currMax == 1) {
				this.runs.put(1, this.out);
			} else {
				Map<Integer, Formula[]> base = runs.get(currMax - 1);
				Map<Integer,Formula[]> added = new HashMap<>();
				for (int i: base.keySet()) {
					// TODO: cope here if it doesnt work
					Formula[] addedRun = new Formula[base.get(i).length];
					IntStream.range(0, base.get(i).length).forEach(j -> addedRun[j] = base.get(i)[j].substitute(loopSub().toLogicNGSubstitution()));
					added.put(i, addedRun);
				}
				runs.put(currMax, added);
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
			for (int i: in.keySet()) {
				System.out.println(i + " " + Arrays.toString(run.get(i)));
			}
		}
	}
}
