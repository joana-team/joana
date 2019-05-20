/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;

import edu.kit.joana.api.sdg.SDGFormalParameter;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.qifc.nildumu.ui.CodeUI;

/**
 * Combines a method with its entry and some helper methods
 */
public class Method {
	public final Program program;
	public final SDGMethod method;
	public final SDGNode entry;
	private final Supplier<BasicBlockGraph> bbgSupplier;
	public final IR ir;
	
	/**
	 * Dominators and loop headers for the cfg
	 */
	private BasicBlockGraph bbg;
	
	public Method(Program program, SDGMethod method, SDGNode entry, Supplier<BasicBlockGraph> bbgSupplier) {
		this.program = program;
		this.method = method;
		this.entry = entry;
		this.bbgSupplier = bbgSupplier;
		this.ir = program.getProcIR(entry);
	}
	
	@Override
	public String toString() {
		return toBCString();
	}
	
	public boolean isOutputMethod() {
		return Program.classForType(method.getSignature().getDeclaringType()).get().equals(CodeUI.class);
	}

	public String toBCString() {
		return method.getSignature().toBCString();
	}
	
	public boolean hasReturnValue() {
		return method.getSignature().getReturnType() != null && !method.getSignature().getReturnType().toHRString().equals("void");
	}
	
	public List<SDGFormalParameter> getParameters(){
		return method.getParameters().stream().sorted(Comparator.comparingInt(SDGFormalParameter::getIndex)).collect(Collectors.toList());
	}
	
	public int getLoopDepth(SDGNode node) {
		return getDoms().loopDepth(program.getBlock(node));
	}
	
	public BasicBlockGraph getDoms(){
		if (bbg == null) {
			bbg = bbgSupplier.get();
			this.bbg.registerDotGraph("cfg", method.getSignature().toStringHRShort(), 
					b -> {
						List<String> strs = new ArrayList<>();
						Stream.of(b.getFirstInstructionIndex(), b.getLastInstructionIndex())
							.filter(i -> i > 0).map(i -> ir.getInstructions()[i])
							.filter(Objects::nonNull)
							.forEach(instr -> strs.add(instr.toString()));
						strs.add(b.getNumber() + "");
						return strs.stream().collect(Collectors.joining("|"));
					});
			DotRegistry.get().storeFiles();
		}
		return bbg;
	}
	
	public int getLoopDepth(ISSABasicBlock block) {
		return getDoms().loopDepth(block);
	}
	
	public boolean isPartOfLoop(SDGNode node) {
		return getLoopDepth(node) > 0;
	}
}