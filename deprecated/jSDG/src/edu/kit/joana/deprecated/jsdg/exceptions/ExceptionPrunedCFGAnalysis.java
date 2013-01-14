/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.exceptions;

import java.util.HashMap;
import java.util.Map;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.strings.StringStuff;

/**
 * This abstract class is used as interface for analyses that remove impossible
 * control flow from a CFG. This is done by detecting exceptions that may always
 * (or never) appear.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public abstract class ExceptionPrunedCFGAnalysis<I, T extends IBasicBlock<I>> {

    protected final CallGraph cg;
    protected final PointerAnalysis pta;
    protected final AnalysisCache cache;

    // Debug stuff
	public static final Map<TypeReference, Integer> COUNT_EXCEPTIONS = new HashMap<TypeReference, Integer>();
	public static final Map<CGNode, Double> PERCENT = new HashMap<CGNode, Double>();
	public static final Map<CGNode, Double> PERCENT_PIE = new HashMap<CGNode, Double>();

	/**
	 * Creates a new instance of the exception prune analysis for a given program.
	 * @param cg Callgraph of the program.
	 * @param pta Points-to analysis of the program.
	 * @param cache Analysis cache of the program.
	 */
	public ExceptionPrunedCFGAnalysis(CallGraph cg, PointerAnalysis pta, AnalysisCache cache) {
		this.cg = cg;
		this.pta = pta;
		this.cache = cache;
	}

	/**
	 * Returns the result of the analysis: A control flow graph where impossible
	 * control flow has been removed. The way how and which impossible flow is detected
	 * may vary between different implementations of this class.
	 *
	 * @param method Callgraph node of the method.
	 * @param progress A progress monitor that is used to display the progress of the analysis.
	 * It can also be used to detect a cancel request from the user. The common behavior is
	 * to cancel the method if progress.isCanceled() is true by throwing a CancelException.
	 * @return The improved CFG without edges that were detected as impossible flow.
	 * @throws UnsoundGraphException Thrown if the original CFG contains inconsistencies.
	 * @throws CancelException Thrown if the user requested cancellation through the progress monitor.
	 */
	/*
	 * You may want to use the com.ibm.wala.util.MonitorUtil with the progress monitor.
	 */
	public abstract ControlFlowGraph<I,T> getPruned(CGNode method, IProgressMonitor progress)
	throws UnsoundGraphException, CancelException;

	/**
	 * Returns the control flow graph that is used as starting point of this analysis. This
	 * should be the original CFG without any deleted edges.
	 *
	 * @param method Callgraph node of the method.
	 * @return The original CFG of the given method.
	 * @throws UnsoundGraphException Thrown if the original CFG contains inconsistencies.
	 */
	public abstract ControlFlowGraph<I,T> getOriginal(CGNode method) throws UnsoundGraphException;

	/**
	 * PrettyWalaNames method that searches for a method matching a given signature.
	 * @param cha Class hierarchy where the method is searched in.
	 * @param methodSig The bytecode signature of the method. E.g. "java/lang/String.indexOf(I)I"
	 * @return The method matching the signature or null if no matching method has been found.
	 */
	public static IMethod resolveMethod(ClassHierarchy cha, String methodSig) {
        // Methode in der Klassenhierarchie suchen
        MethodReference mr = StringStuff.makeMethodReference(Language.JAVA, methodSig);
        IMethod m = cha.resolveMethod(mr);

        return m;
	}
}
