/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp;

import java.util.BitSet;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ActualInExclusionNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ActualInOutNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.FormInOutNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ObjTreeParamModel;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ParameterNode;
import edu.kit.joana.deprecated.jsdg.util.Log;

/**
 * Base class for interprocedural propagation of the object trees. Used by
 * all implementations.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public abstract class InterproceduralInterfaceComputation {

	protected final SDG sdg;

	InterproceduralInterfaceComputation(SDG sdg) {
		this.sdg = sdg;
	}

	public final static class InterProcStatus extends BitSet {

		private static final long serialVersionUID = -1682330033613919300L;

		private static int NUMBER_OF_STATUS_BITS = 2;
		private static int NORMAL_PARAMS_INDEX = 0;
		private static int RETVAL_OR_EXCEPTION_INDEX = 1;
		public InterProcStatus() {
			super(NUMBER_OF_STATUS_BITS);
		}

		public void setNormalParamsChanged() {
			set(NORMAL_PARAMS_INDEX);
		}

		public boolean isNormalParamsChanged() {
			return get(NORMAL_PARAMS_INDEX);
		}

		public void setRetvalOrExceptionChanged() {
			set(RETVAL_OR_EXCEPTION_INDEX);
		}

		public boolean isRetvalOrExceptionChanged() {
			return get(RETVAL_OR_EXCEPTION_INDEX);
		}

		public void setBothChanged() {
			setNormalParamsChanged();
			setRetvalOrExceptionChanged();
		}

		public boolean isBothChanged() {
			return isNormalParamsChanged() && isRetvalOrExceptionChanged();
		}

		public boolean isChanged() {
			return isNormalParamsChanged() || isRetvalOrExceptionChanged();
		}

	}

	/**
	 * Connect form-in/out and call edges for all calls to <tt>callee</tt>
	 * in the caller pdg.
	 * @param sdg the system dependency graph containing all pdgs
	 * @param callerPdg pdg of the caller
	 * @param calleePdg  PDG of the callee
	 * @return NO_CHANGE if nothing has been changed
	 * @throws PDGFormatException
	 */
	public final InterProcStatus calcInterProc(PDG callerPdg, ObjTreeParamModel callerModel,
			PDG calleePdg, ObjTreeParamModel calleeModel) throws PDGFormatException {
		InterProcStatus status = new InterProcStatus();

		// compute interprocedural objecttree dependencies for all calls to
		// calleePdg within the callerPdg
		// if any of those calls have changed the current objecttree structure
		// the overall status has to be set as beeing changed.
		// (therefore the disjunction)
		Set<CallNode> calls = callerPdg.getCallsTo(calleePdg);
		for (CallNode call : calls) {
			status.or(calcInterProc(callerPdg, callerModel, call, calleePdg, calleeModel));
		}

		return status;
	}

	/**
	 * Connects the act-in/out nodes of the CallNode call (contained in the
	 * caller PDG) to the matching form-in/out nodes of the PDG calleePdg.
	 * Checks the objecttree of caller and callee for a single call. Iff they
	 * do not match new nodes are created and the status is set to "changed".
	 * @throws PDGFormatException
	 */
	protected abstract InterProcStatus calcInterProc(PDG callerPdg,
		ObjTreeParamModel callerModel, CallNode call, PDG calleePdg,
		ObjTreeParamModel calleeModel) throws PDGFormatException;

	public static boolean isStartOfANewThread(PDG caller, PDG callee) {
		String callerSig = caller.getMethod().getSignature();
		String calleeSig = callee.getMethod().getSignature();

		return "java.lang.Thread.start()V".equals(callerSig)
				&& calleeSig.endsWith(".run()V");
	}

	/**
	 * Searches for an act-in node corresponding to a form-in node in a set of
	 * act-in nodes
	 * @param actIns Set of act-in nodes
	 * @param fIn Form-in node a corresponding node is searched for
	 * @return Act-in node contained in actIns and corresponding to fIn
	 */
	public static ActualInOutNode getCorrespondantSameInOut(Set<ActualInOutNode> actIns,
			FormInOutNode fIn) {
		assert (fIn.isRoot());

		return getCorrespondingNode(actIns, fIn, true);
	}

	/**
	 * Searches for nodes corresponding to a given node in a set of nodes.
	 * Typically we search for a corresponding out node using an in node and
	 * vice versa.
	 * @param <T> May be FormInOutNode or ActInOutNode
	 * @param formNodes Set of from in/out nodes we the corresponding node is
	 * 	searched in
	 * @param node A corresponding node is searched for this node
	 * @return A node contained in formNodes that corresponds to node
	 */
	public static <T extends ParameterNode<T>>
	FormInOutNode getCorrespondantDifferInOut(Set<FormInOutNode> formNodes, T node) {
		assert (node.isRoot());

		return getCorrespondingNode(formNodes, node, false);
	}

	public static <T extends ParameterNode<T>>
	FormInOutNode getCorrespondantSameInOut(Set<FormInOutNode> formNodes, T node) {
		assert (node.isRoot());

		return getCorrespondingNode(formNodes, node, true);
	}

	/**
	 * Searches a corresponding node in a set of nodes
	 * @param <T> FormInOutNode or ActInOutNode
	 * @param nodes Set of nodes to search in
	 * @param ref Node to search a corresponding node for
	 * @param sameInOut Is true if ref.isIn() and corresponding node.isIn()
	 * should be equal, false if they shouldn't be equal.
	 * @return Node contained in set nodes that corresponds to node ref
	 */
	private static <T extends ParameterNode<?>>
	T getCorrespondingNode(Set<T> nodes, ParameterNode<?> ref, boolean sameInOut) {
		T result = null;

		if (nodes != null) {
			for (T node : nodes) {
				assert (node.isRoot());

				if (node.isCorrespondingNode(ref, sameInOut)) {
					result = node;
					break;
				}
			}
		}

		return result;
	}

	public static boolean copyFormInToActIn(PDG fromPdg, FormInOutNode from,
			PDG toPdg, ActualInOutNode to, ObjTreeUnfoldingCriterion unfold) throws PDGFormatException {
		return copyObjTree(fromPdg, from, toPdg, to, unfold, depParamFormToActIn);
	}

	public static boolean copyFormOutToActOut(PDG fromPdg, FormInOutNode from,
			PDG toPdg, ActualInOutNode to, ObjTreeUnfoldingCriterion unfold) throws PDGFormatException {
		return copyObjTree(fromPdg, from, toPdg, to, unfold, depParamFormToActOut);
	}

	public static boolean copyActInToFormIn(PDG fromPdg, ActualInOutNode from,
			PDG toPdg, FormInOutNode to, ObjTreeUnfoldingCriterion unfold) throws PDGFormatException {
		return copyObjTree(fromPdg, from, toPdg, to, unfold, depParamActToFormIn);
	}

	public static boolean copyActOutToFormOut(PDG fromPdg, ActualInOutNode from,
			PDG toPdg, FormInOutNode to, ObjTreeUnfoldingCriterion unfold) throws PDGFormatException {
		return copyObjTree(fromPdg, from, toPdg, to, unfold, depParamActToFormOut);
	}

	public static <A extends ParameterNode<A>, B extends ParameterNode<B>>
	boolean copyObjTree(PDG fromPdg, A from, PDG toPdg, B to, ObjTreeUnfoldingCriterion unfold) throws PDGFormatException {
		return copyObjTree(fromPdg, from, toPdg, to, unfold, null);
	}

	private static <A extends ParameterNode<A>, B extends ParameterNode<B>>
	boolean copyObjTree(PDG fromPdg, A from, PDG toPdg, B to,
			ObjTreeUnfoldingCriterion unfold, DependencyCreator dep) throws PDGFormatException {
		boolean change = false;

		/**
		 * Nothing to copy if from has no childs or to node was excluded
		 * from points-to analysis
		 */
		if (!from.hasChilds() || to instanceof ActualInExclusionNode ||
				(to.getPointsTo() != null && to.getPointsTo().isEmpty())) {
			return change;
		}

		assert (fromPdg.getId() == from.getPdgId());
		assert (toPdg.getId() == to.getPdgId());
		assert (!(from.isPrimitive() && from.hasChilds())) : "Primitive from node has childs: " + from;
		assert (!(to.isPrimitive() && to.hasChilds())) : "Primitive to node has childs: " + to;
		assert (!(from.isPrimitive() && !to.isPrimitive())) : "Trying to copy from primitive node (" + from
			+ ") to non-primitive node (" + to + ")";
		assert (!(!from.isPrimitive() && to.isPrimitive())) : "Trying to copy from non-primitive node (" + from
			+ ") to primitive node (" + to + ")";
		assert (toPdg.containsNode(to)) : toPdg.toString() + " doesn't contain to-node (" + to + ")";
		assert (fromPdg.containsNode(from)) : fromPdg.toString() + " doesn't contain from-node (" + from + ")";
		assert (to.getPointsTo() != null) : "Points-to set of target parent node " + to + " is null.";
//			Assertions._assert(!to.getPointsTo().isEmpty(),
//				"Points-to set of target parent node " + to + " is empty.");

		for (A fromChild : from.getChilds()) {
			ParameterField field = fromChild.getField();

			//XXX Thread.start referenced a field runnable that can not be resolved
			// so we have unresolved fields here which are null. We just skip them for now.
			if (field == null) {
				continue;
			}

			B toChild = to.getChildForField(field);
			if (toChild == null) {
				// create matching child

				ObjTreeParamModel toModel = (ObjTreeParamModel) toPdg.getParamModel();

				if (fromChild.isPrimitive()) {
					toChild = toModel.makeParameterNodePrimitiveChild(to, field, null);
				} else {
					assert (fromChild.getPointsTo() != null) : "Points-to set of non-primitive node is null.";
						//TODO empty field nodes are ok iff they are leafs
//						Assertions._assert(!fromChild.getPointsTo().isEmpty(),
//							"Points-to set of non-primitive node is empty: " +
//							fromChild);

					OrdinalSet<InstanceKey> fieldAllocs =
						toPdg.getPointsToSetForObjectField(to.getPointsTo(), field);

					/* There are 2 reasons why the points-to set may be empty:
					 *
					 * 1. Wala may exclude some parts of the program from the points-to
					 * analysis, so the points-to set may be empty.
					 * An object field with empty points-to set does not make sense,
					 * as no data flow can later on be calculated.
					 * This is ok for leaf nodes.
					 *
					 * 2. As the from-node is not a must alias of the to-node it may
					 * happen that the points-to sets are not the same. Iff the
					 * child field of the from-node  is only reachable from a
					 * points-to base element that is not in the points-to set
					 * of the to-node, no reachable field allocations can be
					 * found for the to-node. Therefore the field needs not to
					 * be copied as it will never be accessed from the to-node.
					 */
					if (fieldAllocs.isEmpty() && to.getPointsTo().isEmpty()) {
						Log.warn("Empty points-to set in parent node while " +
							"copying from field " + fromChild + " to " + to);
						return false;
					}

					// this is the simplest form of the unfolding criterion (a special case)
					// skip when we try to add a child to node "to" that is equal to node "to" itself
					if (unfold.fieldNodesAreEqual(to, field, fieldAllocs)) {
						return false;
					} else if (!unfold.nodeMatchesUnfoldingCriterion(to, field)) {
						return false;
					}

					toChild = toModel.makeParameterNodeChild(to, field, fieldAllocs);
				}

				change = true;
			}

			if (toChild.getPointsTo() != null && !toChild.getPointsTo().isEmpty()) {
				change |= copyObjTree(fromPdg, fromChild, toPdg, toChild, unfold, dep);
			}

			if (dep != null) {
				dep.addDependency(fromPdg, fromChild, toPdg, toChild);
			}
		}

		return change;
	}

	<A extends ParameterNode<A>, B extends ParameterNode<B>>
	boolean copyObjTree(A from, B to, ObjTreeUnfoldingCriterion unfold, DependencyCreator dep) throws PDGFormatException {
		PDG fromPdg = sdg.getPdgForId(from.getPdgId());
		PDG toPdg = sdg.getPdgForId(to.getPdgId());

		return copyObjTree(fromPdg, from, toPdg, to, unfold, dep);
	}

	<A extends ParameterNode<A>, B extends ParameterNode<B>>
	boolean copyObjTree(A from, B to) throws PDGFormatException {
		PDG fromPdg = sdg.getPdgForId(from.getPdgId());
		PDG toPdg = sdg.getPdgForId(to.getPdgId());

		return copyObjTree(fromPdg, from, toPdg, to, null);
	}

	protected static final DependencyCreator depParamFormToActIn = new DependencyCreator() {

		@Override
		public void addDependency(PDG pdgFrom, AbstractPDGNode formIn, PDG pdgTo,
				AbstractPDGNode actIn) {
			assert (actIn instanceof ActualInOutNode) : actIn + " not instanceof ActualInOutNode";
            assert (formIn instanceof FormInOutNode) : formIn + " not instanceof FormInOutNode";
		    assert (((ActualInOutNode)actIn).isIn());
            assert (((FormInOutNode)formIn).isIn());

            // add dependency in the opposite direction as we have param in dependency
			SDG.addParameterInDependency(pdgTo, (ActualInOutNode) actIn, (FormInOutNode) formIn);
		}

	};

	protected static final DependencyCreator depParamActToFormOut = new DependencyCreator() {

		@Override
		public void addDependency(PDG pdgFrom, AbstractPDGNode actOut, PDG pdgTo,
				AbstractPDGNode formOut) {
			assert (actOut instanceof ActualInOutNode) : actOut + " not instanceof ActualInOutNode";
            assert (formOut instanceof FormInOutNode) : formOut + " not instanceof FormInOutNode";
		    assert (((ActualInOutNode)actOut).isOut());
            assert (((FormInOutNode)formOut).isOut());

			// add dependency in the opposite direction as we have param in dependency
			SDG.addParameterOutDependency(pdgTo, (FormInOutNode) formOut, (ActualInOutNode) actOut);
		}

	};

	protected static final DependencyCreator depParamActToFormIn = new DependencyCreator() {

		@Override
		public void addDependency(PDG pdgFrom, AbstractPDGNode actIn, PDG pdgTo,
				AbstractPDGNode formIn) {
			assert (actIn instanceof ActualInOutNode);
            assert (formIn instanceof FormInOutNode);
		    assert (((ActualInOutNode) actIn).isIn());
            assert (((FormInOutNode) formIn).isIn());

			// add dependency in the opposite direction as we have param in dependency
			SDG.addParameterInDependency(pdgFrom, (ActualInOutNode) actIn, (FormInOutNode) formIn);
		}

	};

	protected static final DependencyCreator depParamFormToActOut = new DependencyCreator() {

		@Override
		public void addDependency(PDG pdgFrom, AbstractPDGNode formOut,PDG pdgTo,
				AbstractPDGNode actOut) {
            assert (actOut instanceof ActualInOutNode);
            assert (formOut instanceof FormInOutNode);
            assert (((ActualInOutNode) actOut).isOut());
            assert (((FormInOutNode) formOut).isOut());

            SDG.addParameterOutDependency(pdgFrom, (FormInOutNode) formOut, (ActualInOutNode) actOut);
		}

	};

	private static abstract class DependencyCreator {
		public abstract void addDependency(PDG pdgFrom, AbstractPDGNode from, PDG pdgTo, AbstractPDGNode to);
	}


}
