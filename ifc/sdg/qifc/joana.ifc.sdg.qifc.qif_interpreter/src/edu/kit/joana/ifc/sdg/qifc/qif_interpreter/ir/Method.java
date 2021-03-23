package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.MissingValueException;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.wala.core.PDG;
import org.logicng.formulas.Formula;

import java.util.*;
import java.util.stream.Collectors;

/**
 * data class to cluster together some objects pertaining to a method
 */
public class Method implements ISATAnalysisFragment {

	private static final String CONSTRUCTOR = "<init>";

	private Program prog;
	private PDG pdg;
	private IR ir;
	private SDGMethod sdgMethod;
	private CGNode cg;
	private CFG cfg;
	private final Map<Integer, Value> programValues;
	private Map<Integer, Formula[]> phiDeps;
	private int returnValue;
	private final List<ISATAnalysisFragment> childFragments;

	public static Method getEntryMethodFromProgram(Program p) {
		Method m = new Method();
		m.prog = p;
		m.pdg = findEntrypointPDG(p);
		m.cg = m.pdg.cgNode;
		m.ir = m.cg.getIR();
		m.sdgMethod = findEntryMethod(p).snd;
		m.cfg = CFG.buildCFG(m);
		m.createParamValues();
		m.initConstants();

		return m;
	}

	public Method() {
		this.programValues = new HashMap<>();
		this.phiDeps = new HashMap<>();
		this.childFragments = new ArrayList<>();
	}

	private void createParamValues() {
		int numParam = this.getIr().getNumberOfParameters();
		for (int i = 1; i < numParam; i++) {
			int valNum = this.getIr().getParameter(i);
			Type type = Type.from(this.getPdg().getParamType(i));
			programValues.put(valNum, Value.createByType(valNum, type));
		}
	}

	private void initConstants() {
		for(BBlock current: this.getCFG().getBlocks()) {
			for (SSAInstruction i: current.instructions()) {
				for (int j = 0; j < i.getNumberOfUses(); j++) {
					if (isConstant(i.getUse(j)) && !this.programValues.containsKey(i.getUse(j))) {
						Type type = getConstType(i.getUse(j));
						programValues.put(i.getUse(j), Value.createConstant(i.getUse(j), type, getConstantValue(i.getUse(j), type)));
					}
				}
			}
		}
	}

	private Object getConstantValue(int valNum, Type type) {
		if (type == Type.INTEGER) {
			return getIntConstant(valNum);
		}
		return null;
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

	public Type getConstType(int i) {
		if (ir.getSymbolTable().isIntegerConstant(i)) {
			return Type.INTEGER;
		} else {
			return Type.CUSTOM;
		}
	}

	public boolean isConstant(int valueNum) {
		return ir.getSymbolTable().isConstant(valueNum);
	}

	private int getIntConstant(int valNum) {
		assert (ir.getSymbolTable().isIntegerConstant(valNum));
		return (int) ir.getSymbolTable().getConstantValue(valNum);
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

	public BBlock getBlockStartingAt(int idx) {
		Optional<BBlock> block = this.getCFG().getBlocks().stream().filter(b -> b.getWalaBasicBLock().getFirstInstructionIndex() == idx).findAny();

		if (!block.isPresent()) {
			throw new IllegalStateException("Couldn't find block starting at index " + idx);
		}

		return block.get();
	}

	public void addValue(int valNum, Value val) {
		programValues.put(valNum, val);
	}

	@Override public boolean hasValnum(int valNum) {
		return programValues.containsKey(valNum);
	}

	@Override public void createValnum(int valNum, SSAInstruction i) {
		assert (i.getNumberOfUses() > 0);

		// TODO: find type of result
		// for now use integer for everything

		edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value defVal = Value
				.createByType(valNum, programValues.get(i.getUse(0)).getType());
	}

	@Override public Method getOwner() {
		return this;
	}

	@Override public List<ISATAnalysisFragment> getChildFragments() {
		return this.childFragments;
	}

	@Override public FragmentType getFragmentType() {
		return FragmentType.METHOD;
	}

	@Override public void setDepsForValnum(int valueNum, Formula[] deps) {
		if (!(deps.length == programValues.get(valueNum).getWidth())) {
			throw new IllegalArgumentException("Different bitwidth: Cannot assign dependencies to value.");
		}
		this.programValues.get(valueNum).setDeps(deps);
	}

	@Override public Formula[] getDepsForValnum(int valNum) {
		return programValues.get(valNum).getDeps();
	}

	@Override public Map<Integer, Formula[]> getFragmentDeps() {
		Map<Integer, Formula[]> fragmentDeps = new HashMap<>();
		programValues.keySet().forEach(i -> fragmentDeps.put(i, programValues.get(i).getDeps()));
		return fragmentDeps;
	}

	public Type type(int valNum) {
		return programValues.get(valNum).getType();
	}

	// TODO: needs overhaul if we add different types
	// add type as argument and create value objects accordingly
	public void setValue(int valNum, Object value) throws MissingValueException {
		if (!hasValnum(valNum)) {
			throw new MissingValueException(valNum);
		}
		programValues.get(valNum).setVal(value);
	}

	public boolean isVoid() {
		return (returnValue == -1);
	}

	public Map<Integer, Formula[]> getPhiDeps() {
		return phiDeps;
	}

	public void addVarSubstitutions(Map<Integer, Formula[]> newSub) {
		if (this.phiDeps == null) {
			this.phiDeps = newSub;
		} else {
			this.phiDeps.putAll(newSub);
		}
	}

	// ----------------------- getters and setters ------------------------------------------

	public IR getIr() {
		return ir;
	}

	public CGNode getCg() {
		return cg;
	}

	public PDG getPdg() {
		return pdg;
	}

	public CFG getCFG() {
		return cfg;
	}

	public Map<Integer, Value> getProgramValues() {
		return programValues;
	}

	public  Value getValue(int valNum) {
		return programValues.getOrDefault(valNum, null);
	}

	public void setReturnValue(int valNum) {
		assert(valNum == -1 || programValues.containsKey(valNum));
		this.returnValue = valNum;
	}
}
