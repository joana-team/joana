package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Substitution;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LoopBody {

	private final int level;
	private final Map<Integer, Formula[]> in;
	private final Map<Integer, Formula[]> beforeLoop;
	private final Map<Integer, Formula[]> out;
	private final Map<Integer, Integer> mapping;
	private final Map<Integer, Map<Integer, Formula[]>> runs;
	private final Method owner;
	private final BBlock head;
	private final Set<BBlock> blocks;
	private final Set<BBlock> breaks;
	private final Formula jumpOut;

	public LoopBody(Method owner, BBlock head) {
		this.level = owner.getCFG().getLevel(head);
		this.owner = owner;
		this.in = new HashMap<>();
		this.out = new HashMap<>();
		this.runs = new HashMap<>();
		this.mapping = new HashMap<>();
		this.beforeLoop = new HashMap<>();
		this.head = head;
		this.blocks = this.owner.getCFG().getBasicBlocksInLoop(head);
		this.breaks = findBreaks();

		BBlock insideLoopSuccessor = head.succs().stream().filter(blocks::contains).findFirst().get();
		Boolean evalTo = insideLoopSuccessor.getImplicitFlows().stream().filter(p -> p.fst == head.idx()).findFirst()
				.get().snd;
		assert (evalTo != null);
		this.jumpOut = (evalTo) ? LogicUtil.ff.not(head.getCondExpr()) : head.getCondExpr();
	}

	private Set<BBlock> findBreaks() {
		Set<BBlock> breaks = new HashSet<>();
		for (BBlock b : this.blocks) {
			if (b.isCondHeader() && b.succs().stream()
					.anyMatch(succ -> owner.getCFG().getLevel(succ) < owner.getCFG().getLevel(head))) {
				breaks.add(b);
			}
		}
		return breaks;
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
		System.out.println(sb);
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

	public boolean producesValNum(int valNum) {
		return this.in.containsKey(valNum);
	}

	public Substitution generateInitialValueSubstitution() {
		Substitution s = new Substitution();

		for (int i : this.in.keySet()) {
			for (int j = 0; j < this.in.get(i).length; j++) {
				s.addMapping((Variable) this.in.get(i)[j], this.beforeLoop.get(i)[j]);
			}
		}
		return s;
	}

	public Map<Integer, Formula[]> getIn() {
		return in;
	}

	public Formula substituteWithIterationOutputs(int run, Formula f) {
		Substitution s = new Substitution();
		for (int i : this.in.keySet()) {
			for (int j = 0; j < this.in.get(i).length; j++) {
				s.addMapping((Variable) this.in.get(i)[j], this.runs.get(run).get(i)[j]);
			}
		}
		return f.substitute(s.toLogicNGSubstitution());
	}

	public Formula getJumpOut() {
		return this.jumpOut;
	}

	public Method getOwner() {
		return this.owner;
	}

	public Set<LoopBody> containedLoops() {
		Set<BBlock> headers = this.blocks.stream()
				.filter(b -> b.isLoopHeader() && owner.getCFG().getLevel(b) == this.level + 1)
				.collect(Collectors.toSet());
		return owner.getLoops().stream().filter(l -> headers.contains(l.head)).collect(Collectors.toSet());
	}

	public Pair<Map<Integer, Formula[]>, Formula> simulateRun(Map<Integer, Formula[]> inputs) {
		assert (inputs.keySet().containsAll(this.in.keySet()));
		Map<Integer, Formula[]> result = new HashMap<>();

		Substitution s = new Substitution();
		out.keySet().forEach(i -> s.addMapping(this.in.get(i), inputs.get(i)));

		for (int i : inputs.keySet()) {
			if (out.containsKey(i)) {
				Formula[] res = new Formula[inputs.get(i).length];
				IntStream.range(0, out.get(i).length)
						.forEach(k -> res[k] = out.get(i)[k].substitute(s.toLogicNGSubstitution()));
				result.put(i, res);
			} else {
				result.put(i, inputs.get(i));
			}
		}
		Substitution afterSub = new Substitution();
		this.in.keySet().forEach(i -> afterSub.addMapping(this.in.get(i), result.get(i)));
		Formula exitLoop = this.jumpOut.substitute(afterSub.toLogicNGSubstitution());
		return Pair.make(result, exitLoop);
	}

	public Formula[] getBeforeLoop(int i) {
		return this.beforeLoop.get(i);
	}

	public Set<BBlock> getBreaks() {
		return this.breaks;
	}

	/**
	 * returns true if the value with the provided value number is defined inside this loop, false otherwise
	 */
	public boolean ownsValue(int valNum) {
		return blocks.stream().anyMatch(b -> b.ownsValue(valNum));
	}
}
