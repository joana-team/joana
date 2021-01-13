package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.api.sdg.SDGCall;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.SDGBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class Program {

	private static final String CONSTRUCTOR = "<init>";

	private SDGProgram sdgProg;
	private SDG sdg;
	private String className;
	private Pair<JavaMethodSignature, SDGMethod> entryMethod;
	private SDGBuilder builder;
	private CallGraph cg;

	public Program(SDGProgram sdgProg, SDG sdg, String className, SDGBuilder builder, CallGraph cg) {
		this.sdgProg = sdgProg;
		this.sdg = sdg;
		this.className = className;
		this.entryMethod  = findEntryMethod();
		this.builder = builder;
		this.cg = cg;
	}

	/**
	 * Finds method that is the entrypoint for the analysis.
	 * By convention this should be the only method called from the main method of the class (apart from the default constructor).
	 * If this is not the case w/ the input program this will not work!
	 *
	 * @return JavaMethodSignature of the method we wish to analyse
	 * @throws IllegalStateException if no entrypoint method for the analysis is found
	 */
	private Pair<JavaMethodSignature, SDGMethod> findEntryMethod() throws IllegalStateException {

		List<Pair<JavaMethodSignature, SDGMethod>> signatures = new ArrayList<>();
		sdgProg.getAllMethods().forEach(m -> signatures.add(Pair.make(m.getSignature(),m )));
		List<Pair<JavaMethodSignature, SDGMethod>> signaturesNoConstructors = signatures.stream().filter(s -> !s.fst.getMethodName().equals(CONSTRUCTOR)).collect(Collectors.toList());

		for (Pair<JavaMethodSignature, SDGMethod> p: signaturesNoConstructors) {
			if (isCalledFromMain(p.fst)) {
				return p;
			}
		}
		throw new IllegalStateException("No entrypoint method found");
	}

	/**
	 * Only our entrypoint method should be called from main (and the default contructor). So we can use this function
	 * to find our entrypoint
	 * @param m a method
	 * @return true iff the method m is called in the main method
	 */
	private boolean isCalledFromMain(JavaMethodSignature m) {
		Collection<SDGCall> callsToMethod = sdgProg.getCallsToMethod(m);
		return callsToMethod.stream().anyMatch(c -> c.getOwningMethod().getSignature().getMethodName().equals("main"));
	}

	public PDG findEntrypointPDG() {
		CGNode main = cg.getEntrypointNodes().stream().findFirst().get();
		Iterator<CGNode> iter = cg.getSuccNodes(main);

		CGNode actualEntry = null;

		while(iter.hasNext()) {
			CGNode node = iter.next();
			if (!node.getMethod().getSignature().contains("<init>")) {
				actualEntry = node;
			}
		}
		return builder.getPDGforMethod(actualEntry);
	}

	public Pair<JavaMethodSignature, SDGMethod> getEntryMethod() {
		return entryMethod;
	}
}
