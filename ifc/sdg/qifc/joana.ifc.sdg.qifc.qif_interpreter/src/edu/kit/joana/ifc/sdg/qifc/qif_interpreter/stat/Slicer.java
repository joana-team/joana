package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.ssa.SSAInstruction;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.wala.core.PDGNode;

import java.util.Map;
import java.util.Optional;

public class Slicer {

	private SummarySlicerBackward ssb;

	public Map<Integer, Boolean> findSlice(Integer leakedVal, Method m) {

		SDGNode criterion = null;

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
			System.out.println(pdgNode.toString());
			pdgNode.getBytecodeIndex();
			criterion = m.getProg().getSdg().getNode(pdgNode.getId());
			System.out.println(criterion.toString());
			System.out.println(criterion.operation.toString());

		}
		return null;
	}
}