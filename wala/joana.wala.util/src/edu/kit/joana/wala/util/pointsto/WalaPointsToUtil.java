/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util.pointsto;


import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DefaultSSAInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.FallbackContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.rta.BasicRTABuilder;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.intset.OrdinalSet;


public final class WalaPointsToUtil {

	private WalaPointsToUtil() {}
	
	/**
	 * Unifies two points-to sets even if they refer to null - OrdinalSet.unify does not allow this.
	 * Unify (a, null) -> a, (null, b) -> b, (null,null) -> null, (a,b) -> OrdinalSet.unify(a,b)
	 * @param a Set a.
	 * @param b Set b.
	 * @return Unification of a and b.
	 */
	public static final <T> OrdinalSet<T> unify(final OrdinalSet<T> a, final OrdinalSet<T> b) {
		if (a != null && b != null) {
			return OrdinalSet.unify(a, b);
		} else if (a == null) {
			return b;
		} else {
			return a;
		}
	}

	public static CallGraphBuilder makeRTA(final AnalysisOptions options, final AnalysisCache cache,
			final IClassHierarchy cha, final AnalysisScope scope) {

		Util.addDefaultSelectors(options, cha);
		Util.addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha);

		return new BasicRTABuilder(cha, options, cache, null, null);
	}

	public static CallGraphBuilder makeContextFreeType(final AnalysisOptions options, final AnalysisCache cache,
			final IClassHierarchy cha, final AnalysisScope scope) {
		
	    if (options == null) {
	      throw new IllegalArgumentException("options is null");
	    }

	    Util.addDefaultSelectors(options, cha);
	    Util.addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha);

	    return ZeroXCFABuilder.make(cha, options, cache, null, null, ZeroXInstanceKeys.NONE);
	}

	public static CallGraphBuilder makeContextSensSite(final AnalysisOptions options, final AnalysisCache cache,
			final IClassHierarchy cha, final AnalysisScope scope) {

	    if (options == null) {
	      throw new IllegalArgumentException("options is null");
	    }
	    
	    Util.addDefaultSelectors(options, cha);
	    Util.addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha);

	    return ZeroXCFABuilder.make(cha, options, cache, null, new FallbackContextInterpreter(
	    		new DefaultSSAInterpreter(options, cache)),
	    		ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.CONSTANT_SPECIFIC | ZeroXInstanceKeys.SMUSH_MANY
	    			| ZeroXInstanceKeys.SMUSH_THROWABLES);
	}

	public static CallGraphBuilder makeObjectSens(final ExtendedAnalysisOptions options, final AnalysisCache cache,
			final IClassHierarchy cha, final AnalysisScope scope) {

	    if (options == null) {
	      throw new IllegalArgumentException("options is null");
	    }
	    
	    Util.addDefaultSelectors(options, cha);
	    Util.addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha);
	    final ContextSelector defaultSelector = new DefaultContextSelector(options, cha);

		return ObjSensZeroXCFABuilder.make(cha, options, cache, defaultSelector,
				new FallbackContextInterpreter(new DefaultSSAInterpreter(options, cache)),
				ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.CONSTANT_SPECIFIC | ZeroXInstanceKeys.SMUSH_MANY
	    			| ZeroXInstanceKeys.SMUSH_THROWABLES);
	}

	public static CallGraphBuilder makeNCallStackSens(final int n, final AnalysisOptions options,
			final AnalysisCache cache, final IClassHierarchy cha, final AnalysisScope scope) {
	    if (options == null) {
	      throw new IllegalArgumentException("options is null");
	    }
	    Util.addDefaultSelectors(options, cha);
	    Util.addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha);
	    final SSAPropagationCallGraphBuilder result =
	    	new nCFABuilder(n, cha, options, cache, null, null);
	    // nCFABuilder uses type-based heap abstraction by default, but we want allocation sites
	    result.setInstanceKeys(new ZeroXInstanceKeys(options, cha, result.getContextInterpreter(),
	        ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.SMUSH_MANY | ZeroXInstanceKeys.CONSTANT_SPECIFIC
	      | ZeroXInstanceKeys.SMUSH_THROWABLES));
	    
	    return result;
	}
}
