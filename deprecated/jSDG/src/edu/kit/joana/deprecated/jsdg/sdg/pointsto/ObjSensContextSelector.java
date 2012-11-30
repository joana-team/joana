/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.pointsto;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.ReceiverInstanceContext;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallerSiteContext;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;

/**
 * ContextSelector for the object sensitive points-to analysis.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ObjSensContextSelector implements ContextSelector {

	public Context getCalleeTarget(CGNode caller, CallSiteReference site,
			IMethod callee, InstanceKey[] actualParameters) {
		final InstanceKey receiver = (actualParameters != null && actualParameters.length > 0 ? actualParameters[0] : null);
		if (mayUnderstand(caller, site, callee, receiver)) {
			if (useOneLevelCallString(callee)) {
				return new CallerSiteContext(caller, site);
			} else {
				return new ReceiverInstanceContext(receiver);
			}
		} else {
			return null;
		}
	}

	private boolean useOneLevelCallString(IMethod callee) {
		return callee.isStatic();
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
