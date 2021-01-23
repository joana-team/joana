package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ipa.callgraph.CallGraph;
import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.sdg.SDGCall;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.wala.core.SDGBuilder;

import java.util.Collection;

public class Program {

	private SDGProgram sdgProg;
	private SDG sdg;
	private String className;
	private SDGBuilder builder;
	private CallGraph cg;
	private IFCAnalysis ana;
	private Method entryMethod;

	public Program(SDGProgram sdgProg, SDG sdg, String className, SDGBuilder builder, CallGraph cg, IFCAnalysis ana) {
		this.sdgProg = sdgProg;
		this.sdg = sdg;
		this.className = className;
		this.builder = builder;
		this.cg = cg;
		this.ana = ana;
		this.entryMethod = Method.getEntryMethodFromProgram(this);
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
}
