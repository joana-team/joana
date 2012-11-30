/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.wala;

import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * CallGraph builder for the object sensitive points-to analysis
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ObjSensZeroXCFABuilder extends ZeroXCFABuilder {

	public ObjSensZeroXCFABuilder(IClassHierarchy cha, AnalysisOptions options,
			AnalysisCache cache, ContextSelector appContextSelector,
			SSAContextInterpreter appContextInterpreter, int instancePolicy) {
		super(cha, options, cache, appContextSelector, appContextInterpreter, instancePolicy);
	}

	protected ZeroXInstanceKeys makeInstanceKeys(IClassHierarchy cha,
			AnalysisOptions options, SSAContextInterpreter contextInterpreter,
			int instancePolicy) {
		ObjSensInstanceKeys zik = new ObjSensInstanceKeys(options, cha,	contextInterpreter, instancePolicy);
		return zik;
	}

	  /**
	   * @param options
	   *            options that govern call graph construction
	   * @param cha
	   *            governing class hierarchy
	   * @param cl
	   *            classloader that can find WALA resources
	   * @param scope
	   *            representation of the analysis scope
	   * @param xmlFiles
	   *            set of Strings that are names of XML files holding bypass logic specifications.
	   * @return a 0-1-Opt-CFA Call Graph Builder.
	   * @throws IllegalArgumentException
	   *             if options is null
	   * @throws IllegalArgumentException
	   *             if xmlFiles == null
	   */
	  public static SSAPropagationCallGraphBuilder make(AnalysisOptions options, AnalysisCache cache, IClassHierarchy cha,
	      ClassLoader cl, AnalysisScope scope, String[] xmlFiles, byte instancePolicy) throws IllegalArgumentException {

	    if (xmlFiles == null) {
	      throw new IllegalArgumentException("xmlFiles == null");
	    }
	    if (options == null) {
	      throw new IllegalArgumentException("options is null");
	    }
	    Util.addDefaultSelectors(options, cha);
	    for (int i = 0; i < xmlFiles.length; i++) {
	      Util.addBypassLogic(options, scope, cl, xmlFiles[i], cha);
	    }

	    return new ObjSensZeroXCFABuilder(cha, options, cache, null, null, instancePolicy);
	  }

	  public static ZeroXCFABuilder make(IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache,
	      ContextSelector appContextSelector, SSAContextInterpreter appContextInterpreter,
	      int instancePolicy) throws IllegalArgumentException {
	    if (options == null) {
	      throw new IllegalArgumentException("options == null");
	    }
	    return new ObjSensZeroXCFABuilder(cha, options, cache, appContextSelector, appContextInterpreter, instancePolicy);
	  }

}
