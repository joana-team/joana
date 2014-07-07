/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;


/**
 * Common interface for classes which take the call graph and pointer analysis result produced by WALA
 * and do something useful with it
 * @author Martin Mohr
 */
public interface CGConsumer {
	void consume(com.ibm.wala.ipa.callgraph.CallGraph cg, PointerAnalysis<? extends InstanceKey> pts);
}
