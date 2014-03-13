/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util.pointsto;

import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSite;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.ReceiverInstanceContext;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * InstanceKeys of the object sensitive points-to analysis
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ObjSensInstanceKeys extends ZeroXInstanceKeys {

	private final ObjSensZeroXCFABuilder.MethodFilter filter;
	
	public ObjSensInstanceKeys(final ExtendedAnalysisOptions options, final IClassHierarchy cha,
			final RTAContextInterpreter contextInterpreter, final int policy) {
		super(options, cha, contextInterpreter, policy);
		
		if (options.filter == null) {
			throw new IllegalArgumentException();
		}
		
		this.filter = options.filter;
	}

	@Override
	public InstanceKey getInstanceKeyForAllocation(final CGNode node, final NewSiteReference allocation) {
		final InstanceKey ik = super.getInstanceKeyForAllocation(node, allocation);

		if (ik instanceof AllocationSiteInNode && filter.restrictToOneLevelObjectSensitivity(node.getMethod())) {
			// unwrap context of allocation node
			final AllocationSiteInNode a = (AllocationSiteInNode) ik;
			if (a.getNode().getContext() instanceof ReceiverInstanceContext) {
				return new AllocationSite(a.getNode().getMethod(), a.getSite(), a.getConcreteType());
			}
		}

		return ik;
	}

}
