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
	// private final Map<Integer, Map<Integer, Formula[]>> runs;
	private final Method owner;
	private final BBlock head;
	private final Set<BBlock> blocks;
	private Substitution initialVals;
	private final Map<Integer, Run> iterations;

	public LoopBody(Method owner, BBlock head) {
		this.owner = owner;
		this.in = new HashMap<>();
		this.out = new HashMap<>();
		// this.runs = new HashMap<>();
		this.mapping = new HashMap<>();
		this.beforeLoop = new HashMap<>();
		this.head = head;
		this.blocks = this.owner.getCFG().getBasicBlocksInLoop(head);
		this.iterations = new HashMap<>();
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

	public Run getRun(int n) {
		if (!iterations.containsKey(n)) {
			computeRunsUpTo(n);
		}
		return iterations.get(n);
	}

	/*
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

	 */

	private void computeRunsUpTo(int n) {
		int currMax = (iterations.keySet().isEmpty()) ? 0 : Collections.max(iterations.keySet());
		while (currMax <= n) {
			if (currMax == 0) {
				this.iterations.put(currMax, this.first());
			} else {
				this.iterations.put(currMax, this.next(currMax));
			}
			currMax++;
		}
	}

	/*
	private Map<Integer, Formula[]> computeRun(int n) {
		if (n == 0) {
			return this.beforeLoop;
		} else {
			assert(this.runs.containsKey(n - 1));
			Map<Integer, Formula[]> run = new HashMap<>();
			Map<Integer, Formula[]> old = new HashMap<>();
			for (int i: this.in.keySet()) {
				Formula[] vars = this.in.get(i);
				Variable[] newVars = new Variable[vars.length];
				IntStream.range(0, vars.length).forEach(j -> newVars[j] = LogicUtil.ff.variable("x" + ((Variable)old.get(i)[j]).name()));

			}

			return run;
		}
	}

	 */

	public boolean producesValNum(int valNum) {
		return this.in.containsKey(valNum);
	}

	public void generateInitialValueSubstitution() {
		this.initialVals = new Substitution();

		for (int i : this.in.keySet()) {
			for (int j = 0; j < this.in.get(i).length; j++) {
				initialVals.addMapping((Variable) this.in.get(i)[j], this.beforeLoop.get(i)[j]);
			}
		}
	}

	public Map<Integer, Formula[]> getIn() {
		return in;
	}

	public Formula substituteWithIterationOutputs(int run, Formula f) {
		Formula s = f;
		for (int i : this.in.keySet()) {
			for (int j = 0; j < this.in.get(i).length; j++) {
				s = LogicUtil.ff.and(s,
						LogicUtil.ff.equivalence(this.in.get(i)[j], this.iterations.get(run).getAfter().get(i)[j]));
			}
		}
		return s;
	}

	public Run first() {
		Run first = new Run(0);
		first.after = beforeLoop;
		first.runDeps = new HashMap<>();

		for (int i : beforeLoop.keySet()) {
			first.runDeps.put(i, LogicUtil.ff.constant(true));
		}

		first.vars = in;
		return first;
	}

	public Run next(int n) {
		assert (n > 0 && iterations.containsKey(n - 1));
		Run before = iterations.get(n - 1);
		Run newIter = new Run(n);

		for (int i : before.after.keySet()) {

			// create new variables
			Variable[] newVars = new Variable[before.after.get(i).length];
			IntStream.range(0, before.after.get(i).length)
					.forEach(j -> newVars[j] = LogicUtil.ff.variable("i" + ((Variable) before.vars.get(i)[j]).name()));
			newIter.vars.put(i, newVars);

			// set new Variables equal to output of loop iteration n - 1 in runDeps
			Formula[] runDeps = new Formula[newVars.length];
			IntStream.range(0, newVars.length)
					.forEach(k -> runDeps[k] = LogicUtil.ff.equivalence(newVars[k], before.after.get(i)[k]));
			newIter.runDeps.put(i, Arrays.stream(runDeps).reduce(before.runDeps.get(i), LogicUtil.ff::and));

			// apply newVars to computation in loop
			Substitution sub = new Substitution();
			IntStream.range(0, before.after.get(i).length)
					.forEach(j -> sub.addMapping((Variable) in.get(i)[j], newVars[j]));

			// compute result values if this were the last loop iteration
			Formula[] newOut = new Formula[newVars.length];
			IntStream.range(0, before.after.get(i).length)
					.forEach(j -> newOut[j] = out.get(i)[j].substitute(sub.toLogicNGSubstitution()));
			newIter.after.put(i, newOut);
		}

		return newIter;
	}

	/**
	 * data class to store information about a specific iteration of the loop
	 */
	public class Run {

		private final int iteration; // # of iteration ( 0 = before any loop iteration )
		private Map<Integer, Formula[]> vars; // set of vars created to represent inputs for this loop iteration
		private Map<Integer, Formula[]> after; // result from applying the computations in the loop body to the variables in {@code vars}
		private Map<Integer, Formula> runDeps; // combine iteration w/ iterations before by setting the inputs if this iteration equal to the ouputs of the iteration beforehand

		private Run(int iteration) {
			this.iteration = iteration;
			this.after = new HashMap<>();
			this.runDeps = new HashMap<>();
			this.vars = new HashMap<>();
		}

		public int getIteration() {
			return iteration;
		}

		public Map<Integer, Formula[]> getVars() {
			return vars;
		}

		public Map<Integer, Formula[]> getAfter() {
			return after;
		}

		public Map<Integer, Formula> getRunDeps() {
			return runDeps;
		}
	}
}
