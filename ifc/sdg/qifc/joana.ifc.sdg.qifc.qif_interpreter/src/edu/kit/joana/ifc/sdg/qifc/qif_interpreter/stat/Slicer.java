package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.ssa.SSAInstruction;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.wala.core.PDGNode;

import java.util.*;
import java.util.stream.Collectors;

public class Slicer {

	private SummarySlicerBackward ssb;

	public Slicer(Program p) {
		this.ssb = new SummarySlicerBackward(p.getSdg());
	}

	public Map<Integer, Boolean> findSlice(Integer leakedVal, Method m) {

		SDGNode criterion = null;
		Map<Integer, Boolean> neededDefs = m.getProgramValues().keySet().stream()
				.collect(Collectors.toMap(i -> i, i -> false));
		Optional<BBlock> owner = m.getCFG().getBlocks().stream().filter(b -> b.ownsValue(leakedVal)).findFirst();

		if (!owner.isPresent()) {
			// leakedValue is either a parameter of the method or a constant
			if (m.isConstant(leakedVal)) {
				// no leak at all
			} else if (m.isParam(leakedVal)) {

			}

		} else {
			SSAInstruction leakedValDef = owner.get().instructions().stream()
					.filter(i -> i.hasDef() && i.getDef() == leakedVal).findFirst().get();
			PDGNode pdgNode = m.getPdg().getNode(leakedValDef);
			criterion = m.getProg().getSdg().getNode(pdgNode.getId());
			Collection<SDGNode> slice = this.ssb.slice(criterion);

			// TODO: temporary
			// filter out all nodes that don't belong to the methods pdg

			List<PDGNode> sliceNodes = slice.stream().map(n -> m.getPdg().getNodeWithId(n.getId()))
					.filter(Objects::nonNull).collect(Collectors.toList());

			for (PDGNode node : sliceNodes) {
				//System.out.println(node.getLabel());
				SSAInstruction instruction = m.getPdg().getInstruction(node);
				if (instruction != null && instruction.hasDef()) {
					neededDefs.put(instruction.getDef(), true);
				}
			}

			//printSlice(criterion, slice);

		}
		return neededDefs;
	}

	private void printSlice(SDGNode criterion, Collection<SDGNode> slice) {
		System.out.println("Slice for: " + criterion.toString());
		slice.forEach(n -> System.out.println(n.toString()));
	}
}