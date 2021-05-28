package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.ssa.SSAInstruction;
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

	public Map<Integer, Boolean> findNeededDefs(Integer leakedVal, Method m) {
		Map<Integer, Boolean> neededDefs = m.getProgramValues().keySet().stream()
				.collect(Collectors.toMap(i -> i, i -> false));

		SDG sdg = removeUnneededEdges(m.getProg().getSdg(), m);

		if (!m.getValue(leakedVal).isConstant() && !m.isParam(leakedVal)) {
			Optional<BBlock> owner = m.getCFG().getBlocks().stream().filter(b -> b.ownsValue(leakedVal)).findFirst();
			assert (owner.isPresent());

			SSAInstruction leakedValDef = owner.get().instructions().stream()
					.filter(i -> i.hasDef() && i.getDef() == leakedVal).findFirst().get();
			SDGNode criterion = m.of(m.of(leakedValDef));

			SummarySlicerBackward ssb = new SummarySlicerBackward(sdg);
			List<PDGNode> slice = ssb.slice(criterion).stream().map(m::of).filter(Objects::nonNull)
					.collect(Collectors.toList());

			// printSlice(criterion, slice);

			for (PDGNode node : slice) {
				SSAInstruction instruction = m.instruction(node);
				if (instruction != null && instruction.hasDef()) {
					neededDefs.put(instruction.getDef(), true);
				}
			}
		}
		return neededDefs;
	}

	private void printSlice(SDGNode criterion, Collection<PDGNode> slice) {
		System.out.println("Slice for: " + criterion.toString());
		slice.forEach(n -> System.out.println(n.toString()));
	}

	private SDG removeUnneededEdges(SDG sdg, Method m) {
		for (Map.Entry<Integer, Value> e : m.getProgramValues().entrySet()) {
			if (e.getValue().isEffectivelyConstant()) {
				SDGNode def = m.of(m.of(m.getDef(e.getKey())));

				if (def == null)
					continue; // "real" constants don't have their own node

				sdg.removeAllEdges(sdg.getIncomingEdgesOfKind(def, SDGEdge.Kind.DATA_DEP));
			}
		}
		return sdg;
	}
}