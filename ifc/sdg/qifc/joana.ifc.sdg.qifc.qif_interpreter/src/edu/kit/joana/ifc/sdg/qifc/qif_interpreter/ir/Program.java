package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.types.MethodReference;
import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.annotations.IFCAnnotation;
import edu.kit.joana.api.sdg.SDGCall;
import edu.kit.joana.api.sdg.SDGFormalParameter;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ProgramPart;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.TempValue;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.Environment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Util;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ui.annotations.Level;
import edu.kit.joana.wala.core.SDGBuilder;
import org.logicng.formulas.Formula;

import java.util.*;

public class Program extends ProgramPart {

	private final Environment env;
	private final SDGProgram sdgProg;
	private final SDG sdg;
	private final String className;
	private final SDGBuilder builder;
	private final CallGraph cg;
	private final IFCAnalysis ana;
	private final Method entryMethod;
	private final Map<String, Method> methods;

	public final Map<Integer, TempValue> temporaries;
	public final List<Formula> ccRestrictions;
	public final List<Formula> dlRestrictions;

	public Program(SDGProgram sdgProg, SDG sdg, String className, SDGBuilder builder, CallGraph cg, IFCAnalysis ana,
			Environment env) {
		this.sdgProg = sdgProg;
		this.sdg = sdg;
		this.className = className;
		this.builder = builder;
		this.cg = cg;
		this.ana = ana;
		this.methods = new HashMap<>();
		this.entryMethod = Method.getEntryMethodFromProgram(this);
		this.env = env;
		this.temporaries = new HashMap<>();
		this.ccRestrictions = new ArrayList<>();
		this.dlRestrictions = new ArrayList<>();
	}

	/**
	 * Only our entrypoint method should be called from main (and the default contructor). So we can use this function
	 * to find our entrypoint
	 * @param m a method
	 * @return true iff the method m is called in the main method
	 */
	public boolean isCalledFromMain(JavaMethodSignature m) {
		Collection<SDGCall> callsToMethod = sdgProg.getCallsToMethod(m);
		return callsToMethod.stream().anyMatch(c -> c.getOwningMethod().getSignature().getMethodName().equals("main"));
	}

	public void addTemporaryValue(TempValue t) {
		this.temporaries.put(t.valNum, t);
	}

	public void addTemporaryValue(Collection<TempValue> collection) {
		collection.forEach(t -> temporaries.put(t.valNum, t));
	}

	public List<TempValue> getTempValues() {
		return new ArrayList<>(this.temporaries.values());
	}

	public String getLevelForParam(SDGMethod method, int param) {
		SDGFormalParameter paramNode = method.getParameters().stream().filter(p -> p.getIndex() == param).findAny()
				.get();
		Optional<IFCAnnotation> annotation = this.ana.getSources().stream()
				.filter(a -> a.getProgramPart().equals(paramNode)).findAny();

		if (annotation.isPresent()) {
			return annotation.get().getLevel1();
		}

		// default for un-annotated parameters
		return Level.HIGH;
	}

	public boolean isRecursive(MethodReference mRef, CGNode calledFrom) {
		Set<CGNode> recNodes = this.cg.getNodes(mRef);
		assert (recNodes.size() == 1);

		CGNode node = recNodes.iterator().next();
		List<CallSiteReference> callSites = Util.asList(node.iterateCallSites());

		return callSites.stream().anyMatch(cs -> cs.getDeclaredTarget().getSignature().equals(mRef.getSignature()));
	}

	public boolean hasMethod(String bcString) {
		return this.methods.containsKey(bcString);
	}

	public Method getMethod(String bcString) {
		return methods.get(bcString);
	}

	// ----------------------------- getters + setters ----------------------------------------------

	public CallGraph getCg() {
		return cg;
	}

	public SDGBuilder getBuilder() {
		return builder;
	}

	public SDGProgram getSdgProg() {
		return sdgProg;
	}

	public Method getEntryMethod() {
		return entryMethod;
	}

	public SDG getSdg() {
		return sdg;
	}

	public String getClassName() {
		return className;
	}

	public void addMethod(Method method) {
		this.methods.put(method.identifier(), method);
	}

	public List<Method> getMethods() {
		return new ArrayList<>(this.methods.values());
	}

	public IFCAnalysis getAna() {
		return ana;
	}

	public Environment getEnv() {
		return env;
	}

	@Override public Method getMethod() {
		return this.getEntryMethod();
	}
}