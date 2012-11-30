/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import java.util.Set;

import com.ibm.wala.escape.IMethodEscapeAnalysis;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSite;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.WalaException;

import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;

public class MethodEscapeAnalysis extends EscapeAnalysis {

	private static final Logger debug = Log.getLogger(Log.L_WALA_CORE_DEBUG);
	
	private final IMethodEscapeAnalysis escape;

	public MethodEscapeAnalysis(IMethodEscapeAnalysis escape) {
		this.escape = escape;
	}


	public boolean mayEscape(CGNode m, Iterable<InstanceKey> pts, Set<CGNode> eSet) {
		boolean mayEscape = false;
		for (InstanceKey ik : pts) {
			if (ik instanceof AllocationSite) {
				AllocationSite ak = (AllocationSite)ik;
				try {
					for (CGNode e : eSet) {
						mayEscape |= escape.mayEscape(ak.getMethod().getReference(), ak.getSite().getProgramCounter(), e.getMethod().getReference());
					}
				} catch (WalaException e) {
					debug.outln("Escape analysis 1 failed.", e);
					return true;
				}
			} else if (ik instanceof AllocationSiteInNode) {
				AllocationSiteInNode aki = (AllocationSiteInNode)ik;
				CGNode allocNode = aki.getNode();
				int allocPC = aki.getSite().getProgramCounter();
				for (CGNode st : eSet) {
					try {
						mayEscape |= escape.mayEscape(allocNode.getMethod().getReference(), allocPC, st.getMethod().getReference());
					} catch (WalaException e) {
						debug.outln("Escape analysis 2 failed.", e);
						return true;
					}
				}
			} else {
				return true;
			}
		}
		return mayEscape;
	}
}

