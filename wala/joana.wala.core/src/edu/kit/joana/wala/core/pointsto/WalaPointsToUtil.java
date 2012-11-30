/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.pointsto;

import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DefaultSSAInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.IClassHierarchy;

import edu.kit.joana.wala.flowless.wala.ObjSensContextSelector;
import edu.kit.joana.wala.flowless.wala.ObjSensZeroXCFABuilder;

public final class WalaPointsToUtil {

	private WalaPointsToUtil() {}

	public static CallGraphBuilder makeContextFreeType(AnalysisOptions options, AnalysisCache cache,
		      IClassHierarchy cha, AnalysisScope scope) {
	    if (options == null) {
	      throw new IllegalArgumentException("options is null");
	    }

	    Util.addDefaultSelectors(options, cha);
	    Util.addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha);

	    return ZeroXCFABuilder.make(cha, options, cache, null, null, ZeroXInstanceKeys.NONE);
	}

	public static CallGraphBuilder makeContextSensSite(AnalysisOptions options, AnalysisCache cache,
		      IClassHierarchy cha, AnalysisScope scope) {

	    if (options == null) {
	      throw new IllegalArgumentException("options is null");
	    }
	    Util.addDefaultSelectors(options, cha);
	    Util.addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha);

	    return ZeroXCFABuilder.make(cha, options, cache, null, null,
	    		ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.CONSTANT_SPECIFIC | ZeroXInstanceKeys.SMUSH_MANY
	    		| ZeroXInstanceKeys.SMUSH_THROWABLES);
	}

	public static CallGraphBuilder makeObjectSens(AnalysisOptions options, AnalysisCache cache,
		      IClassHierarchy cha, AnalysisScope scope, ObjSensContextSelector.MethodFilter filter) {

	    if (options == null) {
	      throw new IllegalArgumentException("options is null");
	    }
	    Util.addDefaultSelectors(options, cha);
	    Util.addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha);
	    final ContextSelector defaultSelector = new DefaultContextSelector(options, cha);

		return ObjSensZeroXCFABuilder.make(cha, options, cache, new ObjSensContextSelector(defaultSelector, filter),
				new DefaultSSAInterpreter(options, cache),
				ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.CONSTANT_SPECIFIC | ZeroXInstanceKeys.SMUSH_MANY
	    		| ZeroXInstanceKeys.SMUSH_THROWABLES);
	}

}
