/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.pruned.ApplicationLoaderPolicy;
import com.ibm.wala.ipa.callgraph.pruned.PruningPolicy;

import edu.kit.joana.wala.core.interference.ThreadInformationProvider;

/**
 * A pruning policy which behaves like {@link com.ibm.wala.ipa.callgraph.pruned.ApplicationLoaderPolicy} but does not prune
 * away thread entries.
 * @author Martin Mohr &lt;martin.mohr@kit.edu&gt;
 */
public final class ThreadAwareApplicationLoaderPolicy implements PruningPolicy {
	public static final ThreadAwareApplicationLoaderPolicy INSTANCE = new ThreadAwareApplicationLoaderPolicy();

	private ThreadAwareApplicationLoaderPolicy() {
		
	}
	/* (non-Javadoc)
	 * @see com.ibm.wala.ipa.callgraph.pruned.PruningPolicy#check(com.ibm.wala.ipa.callgraph.CGNode)
	 */
	@Override
	public boolean check(CGNode n) {
		return ApplicationLoaderPolicy.INSTANCE.check(n) || ThreadInformationProvider.overwritesThreadRun(n.getClassHierarchy(), n);
	}

}
