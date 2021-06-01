package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.wala.core.PDGNode;

import java.util.*;
import java.util.stream.Collectors;

public class Slicer {

	public Slicer() {
	}

	public Map<Pair<Method, Integer>, Boolean> neededDefs;
	public Map<Pair<Method, Integer>, Boolean> neededCF;

	public void findRelevantSlice(Integer leakedVal, Method m) {
		initResultDataStructures(m.getProg());

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
			printSlice(criterion, sdgSlice);

			for (Method method : m.getProg().getMethods()) {
				List<PDGNode> slice = sdgSlice.stream().map(method::of).filter(Objects::nonNull)
						.collect(Collectors.toList());
				//System.out.println("PDG: " + method.identifier());
				//printSlice(slice);
				for (PDGNode node : slice) {
					SSAInstruction instruction = method.instruction(node);
					if (instruction != null && instruction.hasDef()) {
						neededDefs.put(Pair.make(method, instruction.getDef()), true);
					} else if (instruction instanceof SSAConditionalBranchInstruction) {
						BBlock b = BBlock.getBBlockForInstruction(instruction, method.getCFG());
						neededCF.put(Pair.make(method, b.idx()), true);
					}
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

	private void initResultDataStructures(Program p) {
		neededDefs = new HashMap<>();
		neededCF = new HashMap<>();

		for (Method m : p.getMethods()) {
			neededDefs.putAll(m.getProgramValues().keySet().stream()
					.collect(Collectors.toMap(i -> Pair.make(m, i), i -> false)));
			neededCF.putAll(m.getCFG().getBlocks().stream().filter(BBlock::splitsControlFlow).map(BBlock::idx)
					.collect(Collectors.toMap(i -> Pair.make(m, i), i -> false)));
		}
	}
}