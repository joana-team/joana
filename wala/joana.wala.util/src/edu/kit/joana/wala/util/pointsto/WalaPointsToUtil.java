/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util.pointsto;

import com.ibm.wala.analysis.reflection.ReflectionContextInterpreter;
import com.ibm.wala.analysis.reflection.ReflectionContextSelector;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.ipa.callgraph.impl.UnionContextSelector;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DefaultSSAInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DelegatingSSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.FallbackContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.rta.BasicRTABuilder;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.intset.OrdinalSet;
// Available in a special variant of WALA

import joana.contrib.lib.Contrib;

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
	
	public static final <T> boolean isSubsetOf(final OrdinalSet<T> a, final OrdinalSet<T> b) {
		if (a.getMapping()    != b.getMapping()) throw new IllegalArgumentException();
		return a.getBackingSet().isSubset(b.getBackingSet());
	}
	
	public static final SSAContextInterpreter newDelegatingSSAContextInterpreterStack(SSAContextInterpreter... cis) {
		SSAContextInterpreter result = null;
		for (SSAContextInterpreter ci : cis) {
			if (result == null) {
				result = ci;
			} else if (ci != null) {
				result = new DelegatingSSAContextInterpreter(result, ci);
			}
		}
		return result;
	}
	
	public static final ContextSelector newUnionContextSelectorStack(ContextSelector... css) {
		ContextSelector result = null;
		for (ContextSelector cs : css) {
			if (result == null) {
				result = cs;
			} else if (cs != null) {
				result = new UnionContextSelector(result, cs);
			}
		}
		return result;
	}
	
	public static final ContextSelector newDelegatingContextSelectorStack(ContextSelector... css) {
		ContextSelector result = null;
		for (ContextSelector cs : css) {
			if (result == null) {
				result = cs;
			} else if (cs != null) {
				result = new DelegatingContextSelector(result, cs);
			}
		}
		return result;
	}

    /**
     *  Asks parent-selector to resolve target; ask child only if not found by parent.
     */
    private static class DelegatingMethodTargetSelector implements MethodTargetSelector {
        private final MethodTargetSelector parent;
        private final MethodTargetSelector child;
        private final AnalysisScope scope;

        public DelegatingMethodTargetSelector(final MethodTargetSelector parent, final MethodTargetSelector child, 
                final AnalysisScope scope) {
            this.parent = parent;
            this.child = child;
            this.scope = scope;
        }
        @Override
        public IMethod getCalleeTarget (CGNode caller, CallSiteReference site, IClass receiver) {
            IMethod target = parent.getCalleeTarget(caller, site, receiver);
            if (target != null) {
                return target;
            } 
            target = child.getCalleeTarget(caller, site, receiver);
            if (target != null) {
                return child.getCalleeTarget(caller, site, receiver);
            }

            // Assert it's not explicitly excluded (is this necessary?)
            final String clsName = site.getDeclaredTarget().getDeclaringClass().getName().toString().substring(1);
            if (!(scope.getExclusions().contains(clsName))) {
                // This should not be!
                IClassHierarchy cha = caller.getClassHierarchy();
                
                // If this recurses we are doomed!
                if (cha.getRootClass().getReference().equals(site.getDeclaredTarget().getDeclaringClass())) {
                    //System.out.println("ROOT");
                    target = cha.getRootClass().getMethod(site.getDeclaredTarget().getSelector());
                    return target;
                }

                target = cha.resolveMethod(site.getDeclaredTarget()); 
                if (target != null) {
                    return target;
                }

                // This happens quite a view times depending on the stubs used:
                // System.out.println("ERROR: The Method to " + site + " could not be retreived!");
                return null;
            }
            return null;
        }
        @Override
        public String toString() {
            return "<Delegate to " + this.parent + " and " + this.child + ">";
        }
    }

    public static CallGraphBuilder<InstanceKey> makeRTA(final AnalysisOptions options, final IAnalysisCacheView cache,
			final IClassHierarchy cha, final AnalysisScope scope) {

		Util.addDefaultSelectors(options, cha);
		Util.addDefaultBypassLogic(options, scope, Contrib.class.getClassLoader(), cha);

		return new BasicRTABuilder(cha, options, cache, null, null);
	}

	public static CallGraphBuilder<InstanceKey> makeContextFreeType(final AnalysisOptions options, final IAnalysisCacheView cache,
		      final IClassHierarchy cha, final AnalysisScope scope, final ContextSelector contextSelector,
		      final SSAContextInterpreter contextInterpreter) {
	    if (options == null) {
	      throw new IllegalArgumentException("options is null");
	    }

        { // Set the MethodTargetSelector
            MethodTargetSelector oldMethodTargetSelector = options.getMethodTargetSelector();
	        Util.addDefaultSelectors(options, cha);
	        Util.addDefaultBypassLogic(options, scope, Contrib.class.getClassLoader(), cha);

            if (oldMethodTargetSelector != null) {
                options.setSelector(new DelegatingMethodTargetSelector(oldMethodTargetSelector, options.getMethodTargetSelector(), scope));
            }
        }
        
        final int instancePolicy = ZeroXInstanceKeys.NONE;

	    return ZeroXCFABuilder.make(cha, options, cache, contextSelector, contextInterpreter, instancePolicy);
	}

	public static CallGraphBuilder<InstanceKey> makeContextSensSite(final AnalysisOptions options, final IAnalysisCacheView cache,
		      final IClassHierarchy cha, final AnalysisScope scope, final ContextSelector contextSelector,
		      final SSAContextInterpreter additionalContextInterpreter) {

	    if (options == null) {
	      throw new IllegalArgumentException("options is null");
	    }
       
        { // Set the MethodTargetSelector
            MethodTargetSelector oldMethodTargetSelector = options.getMethodTargetSelector();
            Util.addDefaultSelectors(options, cha);
            Util.addDefaultBypassLogic(options, scope, Contrib.class.getClassLoader(), cha);

            if (oldMethodTargetSelector != null) {
                options.setSelector(new DelegatingMethodTargetSelector(oldMethodTargetSelector, options.getMethodTargetSelector(), scope));
            }
        }

        final SSAContextInterpreter contextInterpreter = newDelegatingSSAContextInterpreterStack(
                ReflectionContextInterpreter.createReflectionContextInterpreter(cha, options, cache),
                additionalContextInterpreter,
                new FallbackContextInterpreter(new DefaultSSAInterpreter(options, cache))
        );
        
        final int instancePolicy =  ZeroXInstanceKeys.ALLOCATIONS |
                                    ZeroXInstanceKeys.CONSTANT_SPECIFIC |
                                    ZeroXInstanceKeys.SMUSH_MANY |
                                    ZeroXInstanceKeys.SMUSH_THROWABLES;

	    return ZeroXCFABuilder.make(cha, options, cache, contextSelector, contextInterpreter, instancePolicy);
	}

	public static CallGraphBuilder<InstanceKey> makeObjectSens(final ExtendedAnalysisOptions options, final IAnalysisCacheView cache,
		      final IClassHierarchy cha, final AnalysisScope scope, final ContextSelector additionalContextSelector,
		      final SSAContextInterpreter additionalContextInterpreter) {

	    if (options == null) {
	      throw new IllegalArgumentException("options is null");
	    }

        { // Set the MethodTargetSelector
            final MethodTargetSelector oldMethodTargetSelector = options.getMethodTargetSelector();
            Util.addDefaultSelectors(options, cha);
            Util.addDefaultBypassLogic(options, scope, Contrib.class.getClassLoader(), cha);

            if (oldMethodTargetSelector != null) {
                options.setSelector(new DelegatingMethodTargetSelector(oldMethodTargetSelector, options.getMethodTargetSelector(), scope));
            }
        }


        
        final SSAContextInterpreter contextInterpreter = newDelegatingSSAContextInterpreterStack(
                ReflectionContextInterpreter.createReflectionContextInterpreter(cha, options, cache),
                additionalContextInterpreter,
                new FallbackContextInterpreter(new DefaultSSAInterpreter(options, cache))
        );

        final ContextSelector contextSelector = newDelegatingContextSelectorStack(
                ReflectionContextSelector.createReflectionContextSelector(options),
                new ObjSensContextSelector(
                        newUnionContextSelectorStack(
                                additionalContextSelector,
                                new DefaultContextSelector(options, cha)
                        ),
                        options.filter
                )
        );
        
        final int instancePolicy =  ZeroXInstanceKeys.ALLOCATIONS |
                                    ZeroXInstanceKeys.CONSTANT_SPECIFIC |
                                    ZeroXInstanceKeys.SMUSH_MANY |
                                    ZeroXInstanceKeys.SMUSH_THROWABLES;

        return new ObjSensZeroXCFABuilder(cha, options, cache, contextSelector, contextInterpreter, instancePolicy);
	}

    /**
     *  @todo TODO  Does not respect SDGBuilderConfig.additinalContextSelector and additionalContextInterpreter
     */
	public static CallGraphBuilder<InstanceKey> makeNCallStackSens(final int n, final AnalysisOptions options,
			final IAnalysisCacheView cache, final IClassHierarchy cha, final AnalysisScope scope,
			final ContextSelector additionalContextSelector,
			final SSAContextInterpreter additionalContextInterpreter) {

	    if (options == null) {
	      throw new IllegalArgumentException("options is null");
	    }

        { // Set the MethodTargetSelector
            MethodTargetSelector oldMethodTargetSelector = options.getMethodTargetSelector();
            Util.addDefaultSelectors(options, cha);
            Util.addDefaultBypassLogic(options, scope, Contrib.class.getClassLoader(), cha);

            if (oldMethodTargetSelector != null) {
                options.setSelector(new DelegatingMethodTargetSelector(oldMethodTargetSelector, options.getMethodTargetSelector(), scope));
            }
        }

	    final SSAPropagationCallGraphBuilder result =
	    	new nCFABuilder(n, cha, options, cache, null, null);
	   
        { // Set the ContextSelector
            if (additionalContextSelector != null) {
                final ContextSelector nCFA = result.getContextSelector();
                result.setContextSelector(new DelegatingContextSelector(additionalContextSelector, nCFA));
            }
        }

        { // Set the ContextInterpreter
            if (additionalContextInterpreter != null) {
                final SSAContextInterpreter nCFA = result.getCFAContextInterpreter();
                result.setContextInterpreter(new FallbackContextInterpreter(new DelegatingSSAContextInterpreter(
                                nCFA, additionalContextInterpreter)));
            }
        }
        
        // nCFABuilder uses type-based heap abstraction by default, but we want allocation sites
	    result.setInstanceKeys(new ZeroXInstanceKeys(options, cha, result.getContextInterpreter(),
	        ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.SMUSH_MANY | ZeroXInstanceKeys.CONSTANT_SPECIFIC
	      | ZeroXInstanceKeys.SMUSH_THROWABLES));
	    
	    return result;
	}
    //
    // Old methods follow for compatibility reasons
    //
	public static CallGraphBuilder<InstanceKey> makeNCallStackSens(final int n, final AnalysisOptions options,
			final AnalysisCache cache, final IClassHierarchy cha, final AnalysisScope scope) {
        return makeNCallStackSens(n, options, cache, cha, scope, null, null);
    }

	public static CallGraphBuilder<InstanceKey> makeContextFreeType(final AnalysisOptions options, final AnalysisCache cache,
		      final IClassHierarchy cha, final  AnalysisScope scope) {
        return makeContextFreeType(options, cache, cha, scope, null, null);
    }
	public static CallGraphBuilder<InstanceKey> makeContextSensSite(final AnalysisOptions options, final AnalysisCache cache,
		      final IClassHierarchy cha, final AnalysisScope scope) {
        return makeContextSensSite(options, cache, cha, scope, null, null);
    }
	public static CallGraphBuilder<InstanceKey> makeObjectSens(final ExtendedAnalysisOptions options, final AnalysisCache cache,
		      final IClassHierarchy cha, final AnalysisScope scope) {
        return makeObjectSens(options, cache, cha, scope, null, null);
    }

}
