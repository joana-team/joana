package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import edu.kit.joana.api.sdg.SDGCall;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Program {

	private static final String CONSTRUCTOR = "<init>";

	SDGProgram sdg;
	String className;
	JavaMethodSignature entryMethod;

	public Program(SDGProgram sdg, String className) {
		this.sdg = sdg;
		this.className = className;
		this.entryMethod  = findEntryMethod();
	}

	/**
	 * Finds method that is the entrypoint for the analysis.
	 * By convention this should be the only method called from the main method of the class (apart from the default constructor).
	 * If this is not the case w/ the input program this will not work!
	 *
	 * @return JavaMethodSignature of the method we wish to analyse
	 * @throws IllegalStateException if no entrypoint method for the analysis is found
	 */
	private JavaMethodSignature findEntryMethod() throws IllegalStateException {

		List<JavaMethodSignature> signatures = new ArrayList<>();
		sdg.getAllMethods().forEach(m -> signatures.add(m.getSignature()));
		List<JavaMethodSignature> signaturesNoConstructors = signatures.stream().filter(s -> !s.getMethodName().equals(CONSTRUCTOR)).collect(Collectors.toList());

		for (JavaMethodSignature m: signaturesNoConstructors) {
			if (isCalledInMain(m)) {
				return m;
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
	private boolean isCalledInMain(JavaMethodSignature m) {
		Collection<SDGCall> callsToMethod = sdg.getCallsToMethod(m);
		return callsToMethod.stream().anyMatch(c -> c.getOwningMethod().getSignature().getMethodName().equals("main"));
	}
}
