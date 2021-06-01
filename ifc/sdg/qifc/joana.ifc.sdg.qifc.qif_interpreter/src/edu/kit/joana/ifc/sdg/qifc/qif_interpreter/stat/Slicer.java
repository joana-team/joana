package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.wala.core.PDGNode;

import java.util.*;
import java.util.stream.Collectors;

public class Slicer {

	public Slicer() {
	}

	public Map<Integer, Boolean> neededDefs;
	public Map<Integer, Boolean> neededCF;

	public void findRelevantSlice(Integer leakedVal, Method m) {
		neededDefs = m.getProgramValues().keySet().stream().collect(Collectors.toMap(i -> i, i -> false));
		neededCF = m.getCFG().getBlocks().stream().filter(BBlock::splitsControlFlow).map(BBlock::idx)
				.collect(Collectors.toMap(i -> i, i -> false));

		SDG sdg = removeUnneededEdges(m.getProg().getSdg(), m);

		if (!m.getValue(leakedVal).isConstant() && !m.isParam(leakedVal)) {
			Optional<BBlock> owner = m.getCFG().getBlocks().stream().filter(b -> b.ownsValue(leakedVal)).findFirst();
			assert (owner.isPresent());

			SSAInstruction leakedValDef = owner.get().instructions().stream()
					.filter(i -> i.hasDef() && i.getDef() == leakedVal).findFirst().get();

			SDGNode criterion = m.of(m.of(leakedValDef));

			/* if the leaked value is defined through a function call, we have to compute the slice of the actual node in the sdg, not the call node */
			if (leakedValDef instanceof SSAInvokeInstruction) {
				Method callee = m.getProg()
						.getMethod(((SSAInvokeInstruction) leakedValDef).getDeclaredTarget().getSignature());

				Set<SDGNode> formalOuts = sdg.getFormalOutsOfProcedure(callee.of(callee.getPdg().entry));
				SDGNode formalOut = formalOuts.stream().filter(node -> node.kind == SDGNode.Kind.EXIT).findFirst()
						.get();
				criterion = sdg.getActualOut(criterion, formalOut);
			}

			edu.kit.joana.ifc.sdg.graph.slicer.Slicer ssb = new SummarySlicerBackward(sdg);

			Collection<SDGNode> sdgSlice = ssb.slice(criterion);
			List<PDGNode> slice = sdgSlice.stream().map(m::of).filter(Objects::nonNull).collect(Collectors.toList());

			// printSlice(criterion, sdgSliceF);
			// printSlice(sliceF);

			for (PDGNode node : slice) {
				SSAInstruction instruction = m.instruction(node);
				if (instruction != null && instruction.hasDef()) {
					neededDefs.put(instruction.getDef(), true);
				} else if (instruction instanceof SSAConditionalBranchInstruction) {
					BBlock b = BBlock.getBBlockForInstruction(instruction, m.getCFG());
					neededCF.put(b.idx(), true);
				}
			}
		}
	}

	private void printSlice(SDGNode criterion, Collection<SDGNode> slice) {
		System.out.println("Slice for: " + criterion.toString());
		slice.forEach(n -> System.out.println(n.toString()));
	}

	private void printSlice(Collection<PDGNode> slice) {
		System.out.println("Filtered to PDG Nodes");
		slice.forEach(n -> System.out.println(n.toString()));
	}

	private SDG removeUnneededEdges(SDG sdg, Method m) {
		for (Map.Entry<Integer, Value> e : m.getProgramValues().entrySet()) {
			if (e.getValue().isEffectivelyConstant(m)) {
				SDGNode def = m.of(m.of(m.getDef(e.getKey())));

				if (def == null)
					continue; // "real" constants don't have their own node

				sdg.removeAllEdges(sdg.getIncomingEdgesOfKind(def, SDGEdge.Kind.DATA_DEP));
				sdg.removeAllEdges(sdg.getIncomingEdgesOfKind(def, SDGEdge.Kind.CONTROL_DEP_COND));
			}
		}
		return sdg;
	}
}