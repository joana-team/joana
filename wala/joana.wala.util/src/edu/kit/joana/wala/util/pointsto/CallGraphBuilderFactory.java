/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util.pointsto;

import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * Factory for call graph builders. Use this, if you want to customize the call graph builder used in
 * SDG construction.
 * @author Martin Mohr
 */
public interface CallGraphBuilderFactory {

	/**
	 * @param options
	 * @param cache
	 * @param cha
	 * @param scope
	 * @param additionalContextSelector
	 * @param additionalContextInterpreter
	 * @return
	 */
	CallGraphBuilder createCallGraphBuilder(ExtendedAnalysisOptions options, AnalysisCache cache, IClassHierarchy cha,
			AnalysisScope scope, ContextSelector additionalContextSelector,
			SSAContextInterpreter additionalContextInterpreter);

}
