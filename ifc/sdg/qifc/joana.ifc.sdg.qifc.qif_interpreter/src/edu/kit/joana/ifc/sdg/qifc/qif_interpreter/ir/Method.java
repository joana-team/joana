package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.Util;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.wala.core.PDG;

import java.util.*;
import java.util.stream.Collectors;

/**
 *  data class to cluster together some objects pertaining to a method
 */
public class Method {

	private static final String CONSTRUCTOR = "<init>";

	private Program prog;
	private PDG pdg;
	private IR ir;
	private SDGMethod sdgMethod;
	private CGNode cg;
	private CFG cfg;

	public static Method getEntryMethodFromProgram(Program p) {
		Method m = new Method();
		m.prog = p;
		m.pdg = findEntrypointPDG(p);
		m.cg = m.pdg.cgNode;
		m.ir = m.cg.getIR();
		m.sdgMethod = findEntryMethod(p).snd;
		m.cfg = CFG.buildCFG(m);

		return m;
	}

	/**
	 * Returns the type of the i-th parameter of the function
	 * @param i the index of the parameter. This is **not** the value number of the parameter in the SSA representation!
	 * @return matchign type, if none is found it is assumed that the type is userdefined and CUSTOM is returned
	 */
	public Type getParamType(int i) {
		String name = pdg.getParamType(i).getName().toString();

		switch (name) {
		case "I":
			return Type.INTEGER;
		default:
			return Type.CUSTOM;
		}
	}

	public boolean isConstant(int valueNum) {
		return ir.getSymbolTable().isConstant(valueNum);
	}

	public int getIntConstant(int valNum) {
		assert (ir.getSymbolTable().isIntegerConstant(valNum));
		return (int) ir.getSymbolTable().getConstantValue(valNum);
	}

	// TODO: how do we find out what type a local variable is ????
	// is the information already stored somewhere or do we have to keep track of it ourselves ???
	public Type getType(int valueNum) {
		if (ir.getSymbolTable().isConstant(valueNum)) {
			if (ir.getSymbolTable().isIntegerConstant(valueNum)) {
				return Type.INTEGER;
			}
		}
		return null;
	}

	public static PDG findEntrypointPDG(Program p) {
		CGNode main = p.getCg().getEntrypointNodes().stream().findFirst().get();
		Iterator<CGNode> iter = p.getCg().getSuccNodes(main);

		CGNode actualEntry = null;

		while(iter.hasNext()) {
			CGNode node = iter.next();
			if (!node.getMethod().getSignature().contains("<init>")) {
				actualEntry = node;
			}
		}
		return p.getBuilder().getPDGforMethod(actualEntry);
	}

	/**
	 * Finds method that is the entrypoint for the analysis.
	 * By convention this should be the only method called from the main method of the class (apart from the default constructor).
	 * If this is not the case w/ the input program this will not work!
	 *
	 * @return JavaMethodSignature of the method we wish to analyse
	 * @throws IllegalStateException if no entrypoint method for the analysis is found
	 */
	private static Pair<JavaMethodSignature, SDGMethod> findEntryMethod(Program p) throws IllegalStateException {
		List<Pair<JavaMethodSignature, SDGMethod>> signatures = new ArrayList<>();
		p.getSdgProg().getAllMethods().forEach(m -> signatures.add(Pair.make(m.getSignature(),m )));
		List<Pair<JavaMethodSignature, SDGMethod>> signaturesNoConstructors = signatures.stream().filter(s -> !s.fst.getMethodName().equals(CONSTRUCTOR)).collect(
				Collectors.toList());

		for (Pair<JavaMethodSignature, SDGMethod> pair: signaturesNoConstructors) {
			if (p.isCalledFromMain(pair.fst)) {
				return pair;
			}
		}
		throw new IllegalStateException("No entrypoint method found");
	}

	/**
	 * Doesn't work for Phi's !!!
	 * @param idx
	 * @return
	 */
	public SSAInstruction getInstructionByIdx(int idx) {
		Optional<SSAInstruction> instruction;

		Set<BBlock> blocks = this.getCFG().getBlocks();
		for (BBlock bb: blocks) {
			instruction = bb.getWalaBasicBLock().getAllInstructions().stream().filter(i -> i.iindex == idx).findAny();
			if (instruction.isPresent()) {
				return instruction.get();
			}

		}

		throw new IllegalStateException("Error: Missing instruction. Looking for iindex " + idx);
	}

	public BBlock getBlockStartingAt(int idx) {
		Optional<BBlock> block = this.getCFG().getBlocks().stream().filter(b -> b.getWalaBasicBLock().getFirstInstructionIndex() == idx).findAny();

		if (!block.isPresent()) {
			throw new IllegalStateException("Couldn't find block starting at index " + idx);
		}

		return block.get();
	}

	// ----------------------- getters and setters ------------------------------------------

	public IR getIr() {
		return ir;
	}

	public CGNode getCg() {
		return cg;
	}

	public SDGMethod getSdgMethod() {
		return sdgMethod;
	}

	public PDG getPdg() {
		return pdg;
	}

	public Program getProg() {
		return prog;
	}

	public CFG getCFG() {
		return cfg;
	}
}
