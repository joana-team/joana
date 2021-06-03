package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ProgramPart;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.LoopHandler;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.BBlockOrdering;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGNode;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * data class to cluster together some objects pertaining to a method
 */
public class Method extends ProgramPart {

	private static final String OUTPUT_FUNCTION = "edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out.print(I)V";
	private static final String CONSTRUCTOR = "<init>";
	private static final String ENTRYPOINT_ANNOTATION_NAME = "Ledu/kit/joana/ifc/sdg/qifc/nildumu/ui/EntryPoint";

	private Program prog;
	private PDG pdg;
	private IR ir;
	private SDGMethod sdgMethod;
	private CGNode cg;
	private CFG cfg;
	private final Map<Integer, Value> programValues;
	private List<LoopBody> loops;
	private final Map<Integer, List<Pair<Formula[], Formula>>> phiValPossibilities;
	private int returnValue;
	private IReturnValue<?> rv;
	private int recursionDepth;
	private boolean depsAnalyzed;

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
		m.initReturnValue();
		m.initLoops();
		m.initValues();
		p.addMethod(m);
		m.initCallees();
		return m;
	}

	public Method() {
		this.programValues = new HashMap<>();
		this.phiValPossibilities = new HashMap<>();
		this.recursionDepth = -1;
		this.depsAnalyzed = false;
	}

	public Method(MethodReference ref, Program p) {
		this();
		this.prog = p;
		this.cg = p.getCg().getNodes(ref).iterator().next();
		this.ir = this.cg.getIR();
		this.pdg = p.getBuilder().getPDGforMethod(cg);
		this.sdgMethod = p.getSdgProg().getAllMethods().stream()
				.filter(m -> m.getSignature().toBCString().equals(ref.getSignature())).findFirst().get();
		this.cfg = CFG.buildCFG(this);
		this.createParamValues();
		this.initConstants();
		this.initReturnValue();
		this.initLoops();
		this.initValues();
		p.addMethod(this);
		this.initCallees();
	}

	private void initCallees() {
		this.cfg.getBlocks().forEach(b -> b.instructions().forEach(i -> {
			if (i instanceof SSAInvokeInstruction && !((SSAInvokeInstruction) i).getDeclaredTarget().getSignature()
					.equals(OUTPUT_FUNCTION) && !prog
					.hasMethod(((SSAInvokeInstruction) i).getDeclaredTarget().getSignature())) {
				new Method(((SSAInvokeInstruction) i).getDeclaredTarget(), prog);
			}
		}));
	}

	private void initLoops() {
		this.loops = this.cfg.getBlocks().stream().filter(BBlock::isLoopHeader)
				.map(b -> LoopHandler.buildSkeleton(this, b)).collect(Collectors.toList());
	}

	private void initReturnValue() {
		Type returnType = this.getReturnType();
		switch (returnType) {

		case INTEGER:
			this.rv = new ReturnValue(this);
			break;
		case ARRAY:
			this.rv = new ArrayReturnValue(this);
			break;
		case CUSTOM:
			break;
		}
	}

	private void createParamValues() {
		int numParam = this.getIr().getNumberOfParameters();
		for (int i = 1; i < numParam; i++) {
			Type type = getParamType(i);
			int valNum = this.getIr().getParameter(i);
			if (type == Type.ARRAY) {
				Type elementType = Type.from(this.pdg.getParamType(i).getArrayElementType());
				try {
					Array<? extends Value> array = Array.newArray(elementType, valNum, true);
					programValues.put(valNum, array);
				} catch (UnexpectedTypeException e) {
					e.printStackTrace();
				}
			} else {
				programValues.put(valNum, Value.createPrimitiveByType(valNum, type));
			}
			programValues.get(valNum).setParameter(true);
		}
	}

	private void initConstants() {
		for(BBlock current: this.getCFG().getBlocks()) {
			for (SSAInstruction i: current.instructions()) {

				if (i instanceof SSAArrayLoadInstruction) {
					checkForConstant(((SSAArrayLoadInstruction) i).getIndex());
				} else if (i instanceof SSAArrayStoreInstruction) {
					checkForConstant(((SSAArrayStoreInstruction) i).getIndex());
					checkForConstant(((SSAArrayStoreInstruction) i).getValue());
				} else {
					for (int j = 0; j < i.getNumberOfUses(); j++) {
						checkForConstant(i.getUse(j));
					}
				}
			}
		}
	}

	private void checkForConstant(int valNum) {
		if (isConstant(valNum) && !this.programValues.containsKey(valNum)) {
			Type type = getConstType(valNum);
			programValues.put(valNum, Value.createConstant(valNum, type, getConstantValue(valNum, type)));
		}
	}

	private void initValues() {
		for (BBlock b : BBlockOrdering.topological(cfg.getBlocks(), cfg.entry())) {
			b.instructions().forEach(i -> {
				if (i.hasDef() && (i instanceof SSANewInstruction || (i instanceof SSAInvokeInstruction
						&& ((SSAInvokeInstruction) i).getDeclaredResultType().isArrayType()))) {
					initArrayDef(i);
				} else if (i.hasDef()) {
					initPrimitiveDef(i);
				} else if (i instanceof SSAInvokeInstruction && ((SSAInvokeInstruction) i).getDeclaredTarget()
						.getSignature().equals(OUTPUT_FUNCTION)) {
					this.getProgramValues().get(i.getUse(0)).leak();
				}
			});
		}
	}

	private void initPrimitiveDef(SSAInstruction i) {
		assert (i.hasDef());

		Type resType;
		if (i instanceof SSAArrayLoadInstruction) {
			assert (programValues.containsKey(((SSAArrayLoadInstruction) i).getArrayRef()));
			resType = ((Array) programValues.get(((SSAArrayLoadInstruction) i).getArrayRef())).elementType();
		} else if (i instanceof SSAInvokeInstruction) {
			resType = Type.from(((SSAInvokeInstruction) i).getDeclaredResultType());
		} else {
			OptionalInt use = IntStream.range(0, i.getNumberOfUses())
					.filter(j -> this.programValues.containsKey(i.getUse(j))).findFirst();
			assert (use.isPresent());
			resType = programValues.get(i.getUse(use.getAsInt())).getType();
		}
		programValues.put(i.getDef(), Value.createPrimitiveByType(i.getDef(), resType));
	}

	private void initArrayDef(SSAInstruction i) {
		Value res = null;
		if (i instanceof SSANewInstruction) {
			assert (((SSANewInstruction) i).getConcreteType().isArrayType());
			try {
				res = Array.newArray((SSANewInstruction) i, this, false);
			} catch (UnexpectedTypeException e) {
				e.printStackTrace();
			}
		} else {
			TypeReference typeRef = ((SSAInvokeInstruction) i).getDeclaredResultType();
			assert (typeRef.isArrayType());
			Type arrayType = Type.from(typeRef.getArrayElementType());
			try {
				res = Array.newArray(arrayType, i.getDef(), false);
			} catch (UnexpectedTypeException e) {
				e.printStackTrace();
			}
		}
		programValues.put(i.getDef(), res);
	}

	public SSAInstruction getDef(int valNum) {
		for (BBlock b : this.cfg.getBlocks()) {
			if (b.ownsValue(valNum)) {
				return b.instructions().stream().filter(i -> i.hasDef() && i.getDef() == valNum).findAny().get();
			}
		}
		return null;
	}

	public boolean isCallRecursive(SSAInvokeInstruction i) {
		return i.getDeclaredTarget().getSignature().equals(this.identifier());
	}

	private Object getConstantValue(int valNum, Type type) {
		if (type == Type.INTEGER) {
			return getIntConstant(valNum);
		}
		return null;
	}

	public List<Integer> getLeakedValues() {
		return this.programValues.entrySet().stream().filter(e -> e.getValue().isLeaked()).map(Map.Entry::getKey)
				.collect(Collectors.toList());
	}

	/**
	 * Returns the type of the i-th parameter of the function
	 *
	 * @param i the index of the parameter. This is **not** the value number of the parameter in the SSA representation!
	 * @return matchign type, if none is found it is assumed that the type is userdefined and CUSTOM is returned
	 */
	public Type getParamType(int i) {

		if (this.pdg.getParamType(i).isArrayType()) {
			return Type.ARRAY;
		}

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
		Optional<BBlock> block = this.getCFG().getBlocks().stream()
				.filter(b -> b.getWalaBasicBlock().getFirstInstructionIndex() == idx).findAny();

		if (!block.isPresent()) {
			throw new IllegalStateException("Couldn't find block starting at index " + idx);
		}

		return block.get();
	}

	public void addValue(int valNum, Value val) {
		programValues.put(valNum, val);
	}

	public boolean hasValue(int valNum) {
		return programValues.containsKey(valNum);
	}

	public void setDepsForvalue(int valueNum, Formula[] deps) {
		if (!(deps.length == programValues.get(valueNum).getWidth())) {
			throw new IllegalArgumentException("Different bitwidth: Cannot assign dependencies to value.");
		}
		this.programValues.get(valueNum).setDeps(deps);
	}

	public Formula[] getDepsForValue(int valNum) {
		return programValues.get(valNum).getDeps();
	}

	public Type type(int valNum) {
		return programValues.get(valNum).getType();
	}

	// TODO: needs overhaul if we add different types
	// add type as argument and create value objects accordingly
	public void setValue(int valNum, Object value) {
		programValues.get(valNum).setVal(value, this.recursionDepth);
	}

	public boolean isVoid() {
		return pdg.isVoid();
	}

	public void addLoop(LoopBody loop) {
		this.loops.add(loop);
	}

	public boolean isComputedInLoop(int valNum) {
		return this.loops.stream().anyMatch(l -> l.producesValNum(valNum));
	}

	public void addVarsToValue(int valNum, Variable[] vars) {
		this.programValues.get(valNum).addVars(vars);
	}

	public Variable[] getVarsForValue(int valNum) {
		return this.programValues.get(valNum).getVars();
	}

	public void resetValues() {
		this.programValues.values().forEach(v -> v.resetValueToDepth(this.recursionDepth));
	}

	public String identifier() {
		return this.sdgMethod.getSignature().toBCString();
	}

	public String identifierNoSpecialCharacters() {
		return identifier().replaceAll("[-+.\\(\\)\\[\\]^:,]", "_");
	}

	public int getParamNum() {
		return this.ir.getNumberOfParameters();
	}

	public Object getReturnValueForCall(SSAInvokeInstruction callSite, Method caller) {
		return this.rv.getReturnValueForCallSite(callSite, caller);
	}

	public Array<? extends Value> getArray(int valNum) {
		assert (this.programValues.get(valNum) instanceof Array);
		return (Array<? extends Value>) this.programValues.get(valNum);
	}

	public boolean isArrayType(int valNum) {
		return this.programValues.get(valNum).getType() == Type.ARRAY;
	}

	public boolean isParam(int valNum) {
		return Arrays.stream(this.ir.getParameterValueNumbers()).anyMatch(i -> i == valNum);
	}

	public SDGNode of(PDGNode pdgNode) {
		return (pdgNode == null) ? null : this.getProg().getSdg().getNode(pdgNode.getId());
	}

	public PDGNode of(SDGNode sdgNode) {
		return (sdgNode == null) ? null : this.pdg.getNodeWithId(sdgNode.getId());
	}

	public PDGNode of(SSAInstruction i) {
		return (i == null) ? null : this.pdg.getNode(i);
	}

	public SSAInstruction instruction(PDGNode pdgNode) {
		return (pdgNode == null) ? null : this.pdg.getInstruction(pdgNode);
	}

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

	public List<LoopBody> getLoops() {
		return this.loops;
	}

	public Map<Integer, List<Pair<Formula[], Formula>>> getPhiValPossibilities() {
		return phiValPossibilities;
	}

	public Program getProg() {
		return prog;
	}

	public SDGMethod getSdgMethod() {
		return sdgMethod;
	}

	public int getReturnValue() {
		return this.returnValue;
	}

	public void increaseRecursionDepth() {
		this.recursionDepth++;
	}

	public int getRecursionDepth() {
		return recursionDepth;
	}

	public void decreaseRecursionDepth() {
		this.recursionDepth--;
	}

	public Type getReturnType() {
		return Type.from(cg.getMethod().getReturnType());
	}

	public Type getReturnElementType() {
		assert (this.getReturnType().equals(Type.ARRAY));
		return Type.from(cg.getMethod().getReturnType().getArrayElementType());
	}

	public IReturnValue getReturn() {
		return this.rv;
	}

	public void finishedAnalysis() {
		this.depsAnalyzed = true;
	}

	public boolean isDepsAnalyzed() {
		return this.depsAnalyzed;
	}

	public List<SSAInstruction> instructions() {
		return Arrays.stream(this.getCFG().getWalaCFG().getInstructions()).filter(Objects::nonNull)
				.collect(Collectors.toList());
	}
}