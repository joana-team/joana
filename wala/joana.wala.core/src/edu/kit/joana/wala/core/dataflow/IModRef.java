/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.dataflow;

import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

import edu.kit.joana.wala.core.PDGNode;

/**
 * Generic interface for Mod-Ref computation of single pdg nodes.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public interface IModRef {

	/**
	 * Computes mod-ref bitsets. Has to be called before getMod() and getRef()
	 *
	 * @param monitor
	 * @throws CancelException
	 */
	public void compute(IProgressMonitor monitor) throws CancelException;

	/**
	 * Computes a set of all pdg nodes that refer to a value which may have been
	 * modified by the pdg node provided
	 * @param node pdg node
	 * @return set of nodes which may have been modified by node
	 */
	public BitVectorVariable getMod(PDGNode node);

	/**
	 * Computes a set of pdg nodes that may provide a value which is referenced
	 * by the pdg node node.
	 * @param node pdg node
	 * @return set of pdg nodes the node node may be referencing
	 */
	public BitVectorVariable getRef(PDGNode node);

}
