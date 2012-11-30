/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.wala;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.ReceiverInstanceContext;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFAContextSelector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;

/**
 * ContextSelector for the object sensitive points-to analysis. Can be customized with a filter that decides for which
 * call targets an object sensitive analysis should be made. Defaults back to a n-level callsite sensitivity, where
 * n can be chosen as well.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ObjSensContextSelector implements ContextSelector {

	private final MethodFilter filter;
	private final ContextSelector nLevel;

	public interface MethodFilter {
		/**
		 * Decides for which methods an object sensitive points-to analysis should be engaged. A one level call string
		 * sensitivity is used as a fallback. Note that static methods have no reciever, so they cannot be
		 * analyzed with object sensitivity. Therefore no static method will be passed to this interface.
		 * @param m The method that may be analyzed in an object sensitive context.
		 * @return <tt>true</tt> if calls to the method should be analyzed with object sensitivity, <tt>false</tt>
		 * if a one level call string sensitivity should be used.
		 */
		public boolean engageObjectSensitivity(IMethod m);

		public int getFallbackCallsiteSensitivity();
	}

	public ObjSensContextSelector(final ContextSelector defaultSelector, final MethodFilter filter) {
		if (filter == null) {
			this.filter =  new MethodFilter() {
				public boolean engageObjectSensitivity(final IMethod m) {
					// use context sensitivity for all methods as default
					return true;
				}

				public int getFallbackCallsiteSensitivity() {
					// use 1-level callsite sensitivity as default fallback
					return 1;
				}
			};
		} else {
			this.filter = filter;
		}

		final int level = this.filter.getFallbackCallsiteSensitivity();
		if (level < 0) {
			throw new IllegalStateException("Callsite sensitivity has to be >=0, but " + level + " has been provided.");
		}

		this.nLevel = new nCFAContextSelector(level, defaultSelector);
	}

	public Context getCalleeTarget(CGNode caller, CallSiteReference site,
			IMethod callee, InstanceKey[] actualParameters) {
		final InstanceKey receiver = (actualParameters != null && actualParameters.length > 0 ? actualParameters[0] : null);
		if (mayUnderstand(caller, site, callee, receiver)) {
			if (useOneLevelCallString(callee)) {
				return nLevel.getCalleeTarget(caller, site, callee, actualParameters);
			} else {
				return new ReceiverInstanceContext(receiver);
			}
		} else {
			return null;
		}
	}

	private boolean useOneLevelCallString(IMethod callee) {
		return callee.isStatic() || !filter.engageObjectSensitivity(callee);
	}

	private boolean mayUnderstand(CGNode caller, CallSiteReference site,
			IMethod callee, InstanceKey receiver) {
		if (callee.getDeclaringClass().getReference().equals(TypeReference.JavaLangObject)) {
			// ramp down context: assuming methods on java.lang.Object don't cause pollution
			// important for containers that invoke reflection
			return false;
		}

		return true;
	}

	private static final IntSet thisParameter = IntSetUtil.make(new int[] { 0 });

	@Override
	public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
		if (site.isDispatch() && site.getDeclaredTarget().getNumberOfParameters() > 0) {
			return thisParameter;
		} else {
			return EmptyIntSet.instance;
		}
	}


}
