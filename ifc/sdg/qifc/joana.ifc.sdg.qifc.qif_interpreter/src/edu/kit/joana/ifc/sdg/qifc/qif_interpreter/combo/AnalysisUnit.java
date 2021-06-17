package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ProgramPart;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotGraph;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotNode;

import java.util.*;
import java.util.stream.IntStream;

public class AnalysisUnit implements DotNode {

	private static int ID_CNT = 0;
	private static final List<AnalysisUnit> allSegments = new ArrayList<>();

	public int id;
	public boolean usedMC;
	public Method top;
	public List<Segment<? extends ProgramPart>> segments;
	public Set<BasicBlock> blocks;
	public List<Integer> collectiveOutputValues;
	public double cc;

	public AnalysisUnit(Method m) {
		this(new ArrayList<>(), m);
	}

	public void addSegment(Segment<? extends ProgramPart> segment) {
		this.segments.add(segment);
	}

	public void finish(boolean usedMC) {
		this.usedMC = usedMC;
		this.blocks = new HashSet<>();
		this.segments.forEach(s -> blocks.addAll(s.getBlocks()));
		this.collectiveOutputValues = collectiveOuts();
	}

	public AnalysisUnit(List<Segment<? extends ProgramPart>> segments, Method m) {
		assert (segments.stream().allMatch(s -> s.level == segments.get(0).level));
		this.segments = segments;
		this.id = ID_CNT++;
		this.top = m;
		allSegments.add(this);
	}

	private List<Integer> collectiveOuts() {
		Method m = segments.get(0).programPart.getMethod();
		List<Integer> outs = new ArrayList<>();
		for (Segment<? extends ProgramPart> s : segments) {
			outs.addAll(s.outputs);
		}

		// remove if value is not used anywhere outside of the segments
		outs.removeIf(i -> blocks.containsAll(m.getValue(i).useBlocks()));

		Segment<? extends ProgramPart> tail = segments.stream().max(Comparator.comparingInt(s -> s.rank)).get();
		BasicBlock segmentEnd = tail.getBlocks().stream().max(Comparator.comparingInt(b -> b.idx())).get();

		// special case: following segment is call or loop
		if (segmentEnd.isLoopHeader()) {
			collectiveOutputValues.addAll(m.getLoop(segmentEnd).get().phiToBeforeLoop().values());
		} else if (!segmentEnd.isDummy() && segmentEnd.getWalaBasicBlock()
				.getLastInstruction() instanceof SSAInvokeInstruction) {
			SSAInvokeInstruction call = (SSAInvokeInstruction) segmentEnd.getWalaBasicBlock().getLastInstruction();
			IntStream.range(0, call.getNumberOfUses()).forEach(i -> collectiveOutputValues.add(call.getUse(i)));
		}

		return outs;
	}

	@Override public String getLabel() {
		StringBuilder sb = new StringBuilder(id + "\n");

		blocks.forEach(b -> sb.append(b.idx()).append(" "));
		sb.append("\nOutput Values: ");
		collectiveOutputValues.forEach(i -> sb.append(i).append(" "));

		String analysisType = (usedMC) ? "Model Counting: " : "Nildumu: ";
		sb.append("\n").append(analysisType).append(cc);
		return sb.toString();
	}

	@Override public List<DotNode> getSuccs() {
		return Collections.emptyList();
	}

	@Override public List<DotNode> getPreds() {
		return Collections.emptyList();
	}

	@Override public boolean isExceptionEdge(DotNode succ) {
		return false;
	}

	@Override public int getId() {
		return id;
	}

	public static CombinedGraph asGraph() {
		return new CombinedGraph();
	}

	public static class CombinedGraph implements DotGraph {

		protected CombinedGraph() {
		}

		@Override public DotNode getRoot() {
			return allSegments.get(0);
		}

		@Override public Set<DotNode> getNodes() {
			return new HashSet<>(allSegments);
		}

		@Override public String getName() {
			return "combined";
		}
	}
}