package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.LoopHandler;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Substitution;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.util.*;
import java.util.stream.IntStream;

public class LoopBody {

	private final int level;
	private final Map<Integer, Formula[]> in;
	private final Map<Integer, Formula[]> beforeLoop;
	private final Map<Integer, Formula[]> out;
	private final Map<Integer, Integer> mapping;
	private Map<Integer, Pair<Map<Integer, Formula[]>, Formula>> runs;
	private final Method owner;
	private final BBlock head;
	private final Set<BBlock> blocks;
	private final List<BBlock> breaks;
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

	private List<BBlock> findBreaks() {
		List<BBlock> breaks = new ArrayList<>();
		for (BBlock b : this.blocks) {
			if (b.isCondHeader() && b.succs().stream()
					.anyMatch(succ -> owner.getCFG().getLevel(succ) < owner.getCFG().getLevel(head))) {
				breaks.add(b);
			}
		}

		// sort the possible break locations inside the loop according to the block indices
		// earliest possible break should be in front
		//
		// a blocks idx is always greater than that of its predecessors
		// (exceptions iis the back edge to a loop head, but we are not looking at loop heads here, so it should be ok)
		Comparator<BBlock> comp = new Comparator<BBlock>() {
			@Override public int compare(BBlock o1, BBlock o2) {
				assert(!o1.isDummy() && !o2.isDummy());
				return o1.idx() - o2.idx();
			}
		};
		breaks.sort(comp);

		return breaks;
	}

	/**
	 * Compute dependencies of a value that is defined inside the loop
	 * Make sure to simulate the loop before calling this method!
	 *
	 * Implicitly assumes that the loop body is executed at least once (otherwise this value wouldn't exist)
	 *
	 * @param valNum number of a value that is defined inside the loop
	 * @return Formula, that describes the computed value depending on the # of loop iterations
	 */
	public Formula[] extractInLoopValue(int valNum) {
		Formula[] base = this.owner.getDepsForValue(valNum);
		assert (this.runs.size() > 0);

		Formula[] res = LoopHandler.substituteAll(this, base, runs.get(runs.size() - 1).fst);

		for (int i = runs.size() - 2; i > 0; i--) {
			Formula jmpOut = runs.get(i).snd;
			Formula[] iterationVal = LoopHandler.substituteAll(this, base, runs.get(i).fst);
			res = LogicUtil.ternaryOp(jmpOut, iterationVal, res);
		}

		return base;
	}

	public Set<BBlock> getBlocks() {
		return blocks;
	}

	public boolean hasBlock(int idx) {
		return this.blocks.stream().anyMatch(b -> b.idx() == idx);
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

	/**
	 * Simulates the execution of the loop
	 *
	 * @param inputs Map that contains all method values (not only those defined in the loop) at the start of the iteration
	 * @return Pair, whith its first component being a map containg the method's values after the loop execution
	 * and the second component being the condition that must be fulfilled, to exit the loop after the simulated iteration
	 */
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

	public List<BBlock> getBreaks() {
		return this.breaks;
	}

	public Map<Integer, Formula[]> getIn() {
		return in;
	}

	public Formula getJumpOut() {
		return this.jumpOut;
	}

	public Method getOwner() {
		return this.owner;
	}

	public void addRuns(Map<Integer, Pair<Map<Integer, Formula[]>, Formula>> runs) {
		this.runs = runs;
	}

	public Map<Integer, Pair<Map<Integer, Formula[]>, Formula>> getRuns() {
		return this.runs;
	}
}
