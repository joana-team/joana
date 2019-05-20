/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu.interproc;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Method;

/**
 * Site where a function is called. The node values are stored per call-site.
 */
public class CallSite {
	
    public static class NodeBasedCallSite extends CallSite {

    	final SDGNode callSite;
    	
		public NodeBasedCallSite(Method method, SDGNode callSite) {
			super(method);
			this.callSite = callSite;
		}
    	
		@Override
		public int hashCode() {
			return callSite.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			return obj instanceof NodeBasedCallSite && ((NodeBasedCallSite)obj).callSite.equals(callSite);
		}
    }
	
	public final Method method;

	public CallSite(Method method) {
		this.method = method;
	}
}