package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ProgramPart;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.State;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.SATVisitor;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotNode;
import org.logicng.formulas.Formula;

import java.util.*;

public abstract class Segment<T extends ProgramPart> implements DotNode {

	private static int dotnode_id = 0;

	private final int id;
	public T programPart;
	public int level;
	public Segment<? extends ProgramPart> parent;
	public List<Segment<? extends ProgramPart>> children;

	public Map<Integer, Formula[]> inputs;
	public Map<Integer, Formula[][]> arrayInputs;

	public List<Integer> outputs;
	public List<Integer> arrayOutputs;

	public BasicBlock entry;

	int channelCapacity;
	Formula executionCondition;

	public Segment(T t, Segment<? extends ProgramPart> parent) {
		this();
		this.programPart = t;
		this.level = parent.level + 1;
		this.parent = parent;
		this.children = new ArrayList<>();
		this.inputs = new HashMap<>();
		this.arrayInputs = new HashMap<>();
		this.outputs = new ArrayList<>();
		this.arrayOutputs = new ArrayList<>();
	}

	public Segment() {
		this.id = dotnode_id++;
	}

	public State analyse() {
		return null;
	}

	public abstract State computeSATDeps(State state, Method m, SATVisitor sv);

	public abstract boolean owns(BasicBlock block);

	public abstract void finalize();

	/**
	 * segments the provided list of blocks
	 * <p>
	 * Segmentation rules:
	 * - Loop Header + Cond HEader belong to preceding LinearSegment
	 * - Block that contains MethodCall belongs to preceding LinearStatement
	 *
	 * @param unclaimed blocks that have not been assigned to a segment yet
	 * @return A List of segments that are all at the same level
	 */
	public List<Segment<? extends ProgramPart>> segment(List<BasicBlock> unclaimed) {
		List<Segment<? extends ProgramPart>> segments = new ArrayList<>();
		LinearSegment linear = LinearSegment.newEmpty(this);

		BasicBlock curr;
		while (!unclaimed.isEmpty()) {
			curr = unclaimed.remove(0);
			linear.addBlock(curr);
			if (curr.isLoopHeader()) {
				BasicBlock finalCurr = curr;
				LoopSegment loop = new LoopSegment(
						curr.getCFG().getMethod().getLoops().stream().filter(l -> l.getHead().equals(finalCurr))
								.findFirst().get(), this);
				linear = startNewSegment(unclaimed, loop, linear, segments);
			} else if (curr.isCondHeader()) {
				ConditionalSegment cond = new ConditionalSegment(this, null, curr);
				linear = startNewSegment(unclaimed, cond, linear, segments);
			} else if (curr.hasMethodCall()) {
				MethodSegment method = new MethodSegment(curr.getCallee(), this);
				linear = startNewSegment(unclaimed, method, linear, segments);
			}
		}
		segments.add(linear);
		return segments;
	}

	private LinearSegment startNewSegment(List<BasicBlock> unclaimed, Segment<? extends ProgramPart> newSegment,
			LinearSegment oldLinear, List<Segment<? extends ProgramPart>> segments) {
		segments.add(oldLinear);
		segments.add(newSegment);
		unclaimed.removeAll(newSegment.getBlocks());
		return LinearSegment.newEmpty(this);
	}

	public abstract Set<BasicBlock> getBlocks();

	public List<DotNode> getNodesRec(List<DotNode> nodes) {
		nodes.add(this);
		for (Segment<? extends ProgramPart> n : this.children) {
			nodes = n.getNodesRec(nodes);
		}
		return nodes;
	}

	@Override public List<DotNode> getSuccs() {
		return new ArrayList<>(this.children);
	}

	@Override public List<DotNode> getPreds() {
		return Arrays.asList(this.parent);
	}

	@Override public boolean isExceptionEdge(DotNode succ) {
		return false;
	}

	@Override public int getId() {
		return this.id;
	}
}