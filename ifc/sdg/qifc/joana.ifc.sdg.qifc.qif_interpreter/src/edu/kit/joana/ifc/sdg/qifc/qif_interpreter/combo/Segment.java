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
	public int rank;
	public T programPart;
	public int level;
	public Segment<? extends ProgramPart> parent;
	public List<Segment<? extends ProgramPart>> children;

	public Map<Integer, Formula[]> inputs;
	public Map<Integer, Formula[][]> arrayInputs;

	public List<Integer> outputs;
	public List<Integer> arrayOutputs;

	public BasicBlock entry;

	boolean dynAnaFeasible;
	boolean hasStatAnaChild;
	boolean collapsed;

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
		this.dynAnaFeasible = this.level < 3;
		this.collapsed = false;
		this.rank = 0;
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
		int numChildren = 0;
		while (!unclaimed.isEmpty()) {
			curr = unclaimed.remove(0);
			linear.addBlock(curr);
			if (curr.isLoopHeader()) {
				BasicBlock finalCurr = curr;
				linear.rank = numChildren++;
				LoopSegment loop = new LoopSegment(
						curr.getCFG().getMethod().getLoops().stream().filter(l -> l.getHead().equals(finalCurr))
								.findFirst().get(), this);
				loop.rank = numChildren++;
				linear = startNewSegment(unclaimed, loop, linear, segments);
			} else if (curr.isCondHeader()) {
				linear.rank = numChildren++;
				ConditionalSegment cond = new ConditionalSegment(this, null, curr);
				cond.rank = numChildren++;
				linear = startNewSegment(unclaimed, cond, linear, segments);
			} else if (curr.hasMethodCall()) {
				linear.rank = numChildren++;
				MethodSegment method = new MethodSegment(curr.getCallee(), this);
				method.rank = numChildren++;
				linear = startNewSegment(unclaimed, method, linear, segments);
			}
		}
		if (!linear.children.isEmpty()) {
			linear.rank = numChildren;
			segments.add(linear);
		}
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

	public Set<DotNode> getNodesRec(Set<DotNode> nodes) {
		nodes.add(this);
		for (Segment<? extends ProgramPart> n : this.children) {
			nodes = n.getNodesRec(nodes);
		}
		return nodes;
	}

	/**
	 * @param children a list of segments that should be wrapped into a single segment, cannot be empty
	 * @param parent   the parent of the returned segment
	 * @return either a containerSegment that contains all the segments in {@code children} or, if {@code children} contains only a single segment, the segment itself
	 */
	public static Segment<? extends ProgramPart> createWithChildren(List<Segment<? extends ProgramPart>> children,
			Segment<? extends ProgramPart> parent) {
		if (children.size() == 1) {
			return children.get(0);
		}
		Set<BasicBlock> blocks = new HashSet<>();
		for (Segment<? extends ProgramPart> child : children) {
			blocks.addAll(child.getBlocks());
		}
		ContainerSegment seg = new ContainerSegment(new ProgramPart.Container(blocks), parent, false);
		seg.children = children;

		return seg;

	}

	public void collapse() {
		List<Segment<? extends ProgramPart>> collapsed = new ArrayList<>();
		List<Segment<? extends ProgramPart>> canCollapse = new ArrayList<>();
		for (Segment<? extends ProgramPart> child : this.children) {
			child.collapse();
			if (!child.hasStatAnaChild()) {
				canCollapse.add(child);
			} else {
				if (!canCollapse.isEmpty()) {
					collapsed.add(createWithChildren(canCollapse, this));
				}

				canCollapse = new ArrayList<>();
				collapsed.add(child);
			}
		}
		if (!canCollapse.isEmpty()) {
			collapsed.add(createWithChildren(canCollapse, this));
		}

		if (collapsed.size() == 1) {
			this.children = collapsed.get(0).children;
		} else {
			this.children = collapsed;
		}
		this.collapsed = true;
	}

	public boolean hasStatAnaChild() {
		return !this.dynAnaFeasible || this.children.stream().anyMatch(Segment::hasStatAnaChild);
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