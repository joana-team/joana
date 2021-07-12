package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo;

import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ProgramPart;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.SATVisitor;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotGraph;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotNode;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;

import java.util.*;

/**
 * Represents a section of a program (= collection of segments) that are analysed together i nte combined analysis
 * The combined analysis calculates the channel capacity for the analysisunit either by using nildumu or using the sat-based analysis
 */
public class AnalysisUnit implements DotNode {

	private static int ID_CNT = 0;
	private static final List<AnalysisUnit> allSegments = new ArrayList<>();

	public int id;
	public boolean usedMC;
	public Method top;
	public Formula additionalCond;
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
		if (usedMC) {
			this.collectiveOutputValues = collectiveOuts();
		} else {
			collectiveOutputValues = new ArrayList<>();
		}
	}

	public AnalysisUnit(List<Segment<? extends ProgramPart>> segments, Method m) {
		assert (segments.stream().allMatch(s -> s.level == segments.get(0).level));
		this.segments = segments;
		this.id = ID_CNT++;
		this.top = m;
		this.additionalCond = LogicUtil.ff.constant(true);
		allSegments.add(this);
	}

	private List<Integer> collectiveOuts() {
		List<Integer> outs = new ArrayList<>();
		for (Segment<? extends ProgramPart> s : segments) {
			s.finalize();
			outs.addAll(s.outputs);
		}

		// remove if value is not used anywhere outside of the segments
		outs.removeIf(i -> blocks.containsAll(top.getValue(i).useBlocks()));

		Segment<? extends ProgramPart> tail = segments.stream().max(Comparator.comparingInt(s -> s.rank)).get();
		BasicBlock segmentEnd = tail.getBlocks().stream().max(Comparator.comparingInt(b -> b.idx())).get();

		// special case: following segment is call or loop
		if (segmentEnd.isLoopHeader()) {
			top.getLoop(segmentEnd).get().phiToBeforeLoop().values().forEach(i -> {
				if (!top.getValue(i).isConstant()) {
					outs.add(i);
				}
			});
			SSAConditionalBranchInstruction cond = (SSAConditionalBranchInstruction) segmentEnd.getWalaBasicBlock()
					.getLastInstruction();
			int bound = cond.getNumberOfUses();
			for (int i = 0; i < bound; i++) {
				if (!top.getValue(cond.getUse(i)).isConstant() && top.getValue(cond.getUse(i)).influencesLeak()
						&& !segmentEnd.ownsValue(cond.getUse(i))) {
					outs.add(cond.getUse(i));
				}
			}
		} else if (!segmentEnd.isDummy() && !segmentEnd.getWalaBasicBlock().isExitBlock() && segmentEnd
				.getWalaBasicBlock().getLastInstruction() instanceof SSAInvokeInstruction
				&& !((SSAInvokeInstruction) segmentEnd.getWalaBasicBlock().getLastInstruction()).getDeclaredTarget()
				.getSignature().equals(SATVisitor.OUTPUT_FUNCTION)) {
			SSAInvokeInstruction call = (SSAInvokeInstruction) segmentEnd.getWalaBasicBlock().getLastInstruction();
			int bound = call.getNumberOfUses();
			for (int i = 1; i < bound; i++) {
				if (!top.getValue(call.getUse(i)).isConstant() && top.getValue(call.getUse(i)).influencesLeak()) {
					outs.add(call.getUse(i));
				}
			}
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