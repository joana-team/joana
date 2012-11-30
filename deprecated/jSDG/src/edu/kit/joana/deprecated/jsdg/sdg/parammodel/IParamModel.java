/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel;


import com.ibm.wala.util.intset.OrdinalSetMapping;

import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.dataflow.FixStubInitializerDependencies;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;

/**
 *
 * Every PDG has a corresponding parameter model that describes its interface.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public interface IParamModel {

	/**
	 * Returns the formal-out nodes of the pdg
	 * @return
	 */
	public IParamSet<? extends AbstractParameterNode> getModParams();

	/**
	 * Returns the formal-in nodes of the pdg
	 * @return
	 */
	public IParamSet<? extends AbstractParameterNode> getRefParams();

	/**
	 * returns the actual-out nodes of a call instruction
	 * @param call
	 * @return
	 */
	public IParamSet<? extends AbstractParameterNode> getModParams(CallNode call);

	/**
	 * returns the actual-in nodes of a call instruction
	 * @param call
	 * @return
	 */
	public IParamSet<? extends AbstractParameterNode> getRefParams(CallNode call);

	/**
	 * Searches the actual node that is connected to the formal node at the
	 * given call instruction.
	 * Fails if the provided pdg is not a target of the call
	 * @param call
	 * @param callee
	 * @param formalNode
	 * @return
	 */
	public AbstractParameterNode getMatchingActualNode(CallNode call, PDG callee, AbstractParameterNode formalNode);

	/**
	 * Searches the formal node that is connected to the actual node at the
	 * given call instruction.
	 * Fails if the provided pdg is not a target of the call
	 * @param call
	 * @param actualNode
	 * @param callee
	 * @return
	 */
	public AbstractParameterNode getMatchingFormalNode(CallNode call, AbstractParameterNode actualNode, PDG callee);

	/**
	 * Creates a actual out node for a void return statement. This is used by
	 * constructor stubs to model the dependency between constructor parameter
	 * and created object.
	 * @see FixStubInitializerDependencies class
	 * @param call
	 * @return actual out node for the call
	 */
	public AbstractParameterNode makeVoidActualOut(CallNode call, PDG target);

	public AbstractParameterNode makeExit();

	public AbstractParameterNode makeExceptionalExit();

	/**
	 * Return a mod ref analysis that maps each heap accessing node in the
	 * underlying pdg to a set of pdg nodes it may refer to or it may modify.
	 * @param domain
	 * @return
	 */
	public IModRef getModRef(OrdinalSetMapping<AbstractPDGNode> domain);

}
