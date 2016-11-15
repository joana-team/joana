/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree;

import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

import edu.kit.joana.deprecated.jsdg.sdg.IntermediatePDG;
import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IModRef;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamModel;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamSet;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ParameterNode.Type;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.dataflow.ModRefHeapParams;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp.ObjTreeUnfoldingCriterion;
import edu.kit.joana.deprecated.jsdg.util.Util;
import edu.kit.joana.util.maps.MultiMap;

/**
 * Parameter model for the object tree
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ObjTreeParamModel implements IParamModel {

	private final IntermediatePDG pdg;

	public IModRef getModRef(OrdinalSetMapping<AbstractPDGNode> domain) {
		IModRef modRef = new ModRefHeapParams(domain, pdg);

		return modRef;
	}

	/**
	 * Static fields accessed (read/write) by the underlying method (pdg).
	 * Note that static formal nodes are always root nodes.
	 * Mapping from IField -> Form-In Node
	 */
	private final Map<ParameterField, FormInOutNode> staticFormIns;
	private final Map<ParameterField, FormInOutNode> staticFormOuts;

	/**
	 * Includes all formal-in root nodes. These are all static formal-ins from
	 * staticFormIns and all method parameters (including the this pointer)
	 *
	 * They can be seen as escape-in gateways
	 */
	private final Set<FormInOutNode> rootFormIn;
	/**
	 * Includes all formal-out root nodes. These are all static formal-outs from
	 * staticFormOuts and all method parameters (including the this pointer)
	 * which are pointing-to object fields that have been written to.
	 * Additionally this set includes the formal-out node for exceptional returns
	 * and the exit node.
	 *
	 * They can be seen as escape-out gateways
	 */
	private final Set<FormInOutNode> rootFormOut;


	/**
	 * Maps the points-to set elements InstanceKey to a set of may-aliasing
	 * actual-out nodes.
	 *
	 * InstanceKey -> Set of ActualInOutNode
	 */
	private final MultiMap<InstanceKey, ActualInOutNode> ikey2actout;

	/**
	 * Maps the points-to set elements InstanceKey to a set of may-aliasing
	 * formal-in nodes.
	 *
	 * InstanceKey -> Set of FormalInOutNode
	 */
	private final MultiMap<InstanceKey, FormInOutNode> ikey2formin;

	/**
	 * Maps the points-to set elements InstanceKey to a set of may-aliasing
	 * actual-in nodes.
	 *
	 * InstanceKey -> Set of ActualInOutNode
	 */
	private final MultiMap<InstanceKey, ActualInOutNode> ikey2actin;

	/**
	 * Maps the points-to set elements InstanceKey to a set of may-aliasing
	 * formal-out nodes.
	 *
	 * InstanceKey -> Set of FormalInOutNode
	 */
	private final MultiMap<InstanceKey, FormInOutNode> ikey2formout;


	/**
	 * act-in/out nodes for method calls
	 */
	private final MultiMap<CallNode, ActualInOutNode> call2rootActIn;
	private final MultiMap<CallNode, ActualInOutNode> call2rootActOut;


	private final ObjTreeUnfoldingCriterion unfold;

	public ObjTreeParamModel(IntermediatePDG pdg, ObjTreeUnfoldingCriterion unfold) {
		this.pdg = pdg;
		this.unfold = unfold;

		this.staticFormIns = HashMapFactory.make();
		this.staticFormOuts = HashMapFactory.make();
		this.rootFormIn = HashSetFactory.make();
		this.rootFormOut = HashSetFactory.make();

		this.ikey2actout = new MultiMap<InstanceKey, ActualInOutNode>();
		this.ikey2formin = new MultiMap<InstanceKey, FormInOutNode>();
		this.ikey2actin = new MultiMap<InstanceKey, ActualInOutNode>();
		this.ikey2formout = new MultiMap<InstanceKey, FormInOutNode>();

		this.call2rootActIn = new MultiMap<CallNode, ActualInOutNode>();
		this.call2rootActOut = new MultiMap<CallNode, ActualInOutNode>();
	}

	private final boolean containsNode(AbstractPDGNode node) {
		return pdg.containsNode(node);
	}

	private final int getId() {
		return pdg.getId();
	}

	public void addRootFormOut(ExitNode exit) {
		assert (exit != null);
		assert (containsNode(exit));
		assert (exit.getPdgId() == getId());
		assert (!exit.isStatic());
		assert (exit.getPointsTo() != null || exit.isPrimitive());
		assert (exit.isRoot());

		if (!exit.isPrimitive()) {
			addFormOutToMap(exit);
		}

		rootFormOut.add(exit);
	}

	private static boolean assertNodeNotExists(Set<FormInOutNode> rootFormOut, FormInOutNode fOut, IntermediatePDG pdg) {
		// verify that no form out node with the same label already exists
		boolean verify = true;
		for (FormInOutNode node : rootFormOut) {
			verify &= !node.getLabel().equals(fOut.getLabel());
		}
		assert (verify) : "Form-Out node with label " + fOut.getLabel() + " already exists in pdg " + pdg;

		return true;
	}

	public void addRootFormOut(FormInOutNode fOut) {
		assert (fOut != null);
		assert (containsNode(fOut));
		assert (fOut.getPdgId() == getId());
		assert (!fOut.isStatic());
		assert (fOut.isRoot());
		assert (!fOut.isExit());
		assert (fOut.getPointsTo() != null);
		assert assertNodeNotExists(rootFormOut, fOut, pdg);

		if (!fOut.isPrimitive()) {
			addFormOutToMap(fOut);
		}

		rootFormOut.add(fOut);
	}

	public Set<FormInOutNode> getRootFormOuts() {
		return rootFormOut;
	}

	/**
	 * used for form-ins from method parameters
	 * @param fIn
	 */
	public void addRootFormIn(FormInOutNode fIn) {
		assert (fIn != null);
		assert (containsNode(fIn));
		assert (fIn.getPdgId() == getId());
		assert (!fIn.isStatic());
		assert (fIn.getPointsTo() != null || fIn.isPrimitive());
		assert (fIn.isRoot());

		rootFormIn.add(fIn);
		if (!fIn.isPrimitive()) {
			addFormInToMap(fIn);
		}
	}

	/**
	 * used for form-ins from access to static/non-static fields
	 * @param fIn
	 * @param pointsTo
	 */
	public void addStaticFormIn(FormInOutNode fIn) {
		ParameterField field = fIn.getField();

		assert (containsNode(fIn));
		assert (fIn.getPdgId() == getId());
		assert (field != null);
		assert (field.isStatic());
		assert (fIn.getPointsTo() != null || fIn.isPrimitive());
		assert (fIn.isRoot());
		assert (staticFormIns.get(field) == null) : "A node matching this static field reading access has already been added."
			+ " Field: " + Util.fieldName(field) + " <-> " + fIn;

		rootFormIn.add(fIn);
		if (!fIn.isPrimitive()) {
			addFormInToMap(fIn);
		}
		staticFormIns.put(field, fIn);
	}

	/**
	 * Search for all form in nodes that may alias a given points-to set. This
	 * includes not only the root nodes but also the field access nodes.
	 * @param ikey element of the points to set
	 * @return set of all may-aliasing form in nodes
	 */
	public Set<FormInOutNode> getMayAliasingFormIns(OrdinalSet<InstanceKey> pts) {
		Set<FormInOutNode> mayAlias = new HashSet<FormInOutNode>();

		for (InstanceKey ik : pts) {
			Set<FormInOutNode> alias = ikey2formin.get(ik);
			if (alias != null && !alias.isEmpty()) {
				mayAlias.addAll(alias);
			}
		}

		return mayAlias;
	}

	/**
	 * Search for all form in nodes that may alias a given points-to set. This
	 * includes not only the root nodes but also the field access nodes.
	 * @param ikey element of the points to set
	 * @return set of all may-aliasing form in nodes
	 */
	public Set<FormInOutNode> getMayAliasingFormOuts(OrdinalSet<InstanceKey> pts) {
		Set<FormInOutNode> mayAlias = new HashSet<FormInOutNode>();

		for (InstanceKey ik : pts) {
			Set<FormInOutNode> alias = ikey2formout.get(ik);
			if (alias != null && !alias.isEmpty()) {
				mayAlias.addAll(alias);
			}
		}

		return mayAlias;
	}

	/**
	 * Search for all act out nodes that may alias a given points-to set. This
	 * includes not only the root nodes but also the field access nodes.
	 * @param ikey element of the points to set
	 * @return set of all may-aliasing act out nodes
	 */
	public Set<ActualInOutNode> getMayAliasingActOuts(OrdinalSet<InstanceKey> pts) {
		Set<ActualInOutNode> mayAlias = new HashSet<ActualInOutNode>();

		for (InstanceKey ikey : pts) {
			Set<ActualInOutNode> alias = ikey2actout.get(ikey);
			if  (alias != null && !alias.isEmpty()) {
				mayAlias.addAll(alias);
			}
		}

		return mayAlias;
	}

	public void addChildFormInToMap(FormInOutNode node) {
		assert (node != null);
		assert (!node.isRoot());

		addFormInToMap(node);
	}

	/**
	 * Adds a form-in node to the mapping of instancekey to nodes. So
	 * it may be found using getFormInsForInstanceKey
	 * @param node
	 */
	public void addFormInToMap(FormInOutNode node) {
		assert (node != null);
		assert (containsNode(node));
		assert (node.getPdgId() == getId());
		assert (node.isIn());
		assert (!node.isPrimitive());
		assert (node.getPointsTo() != null);

		for (InstanceKey ikey : node.getPointsTo()) {
			ikey2formin.add(ikey, node);
		}
	}

	public void addChildFormOutToMap(FormInOutNode node) {
		assert (node != null);
		assert (!node.isRoot());

		addFormOutToMap(node);
	}

	/**
	 * Adds a form-in node to the mapping of instancekey to nodes. So
	 * it may be found using getFormInsForInstanceKey
	 * @param node
	 */
	public void addFormOutToMap(FormInOutNode node) {
		assert (node != null);
		assert (containsNode(node));
		assert (node.getPdgId() == getId());
		assert (node.isOut());
		assert (!node.isPrimitive());
		assert (node.getPointsTo() != null);

		for (InstanceKey ikey : node.getPointsTo()) {
			ikey2formout.add(ikey, node);
		}
	}

	/**
	 * Get all root form-in nodes - static and non-static
	 * @return set of form in nodes
	 */
	public Set<FormInOutNode> getRootFormIns() {
		return rootFormIn;
	}

	/**
	 * Adds an actual in node of a method call
	 * @param call node of the method call
	 * @param pnode actual in node to add to the call
	 */
	public void addRootActIn(CallNode call, ActualInOutNode pnode) {
		assert (call != null);
		assert (pnode != null);
		assert (containsNode(call));
		assert (containsNode(pnode));
		assert (pnode.getPdgId() == getId());
		assert (pdg.getAllCalls().contains(call));
		assert (pnode.isIn());
		assert (pnode.getPointsTo() != null || pnode.isPrimitive());
		assert (pnode.isRoot());

		call2rootActIn.add(call, pnode);

		if (!pnode.isPrimitive()) {
			addActInToMap(pnode);
		}
	}

	/**
	 * Get all actual in nodes belonging to a method call
	 * @param call node of the method call
	 * @return set of actual in nodes
	 */
	public Set<ActualInOutNode> getRootActIns(CallNode call) {
		assert (call != null);
		assert (containsNode(call));
		assert (pdg.getAllCalls().contains(call));

		return call2rootActIn.get(call);
	}

	/**
	 * Adds an actual out node of a method call. This method also adds the
	 * actual out node (which must have a points-to set associated) to the
	 * mapping of instancekeys to acto out nodes. So the act out node can be
	 * found later on using getActOutsForInstanceKey.
	 * @param call node of the call instruction
	 * @param pnode actual out node belonging to the call
	 */
	public void addRootActOut(CallNode call, ActualInOutNode pnode) {
		assert (call != null);
		assert (pnode != null);
		assert (containsNode(call));
		assert (containsNode(pnode));
		assert (pnode.getPdgId() == getId());
		assert (pdg.getAllCalls().contains(call));
		assert (pnode.isOut());
		//static nodes even have an effect outside the called method when they are primitive
		assert (pnode.getPointsTo() != null || (pnode.isPrimitive() && pnode.getType() == Type.RETURN)
				|| (pnode.isPrimitive() && pnode.isStatic()));
		assert (pnode.isRoot());

		call2rootActOut.add(call, pnode);

		if (!pnode.isPrimitive()) {
			addActOutToMap(pnode);
		}
	}


	public void addChildActOutToMap(ActualInOutNode pnode) {
		assert (pnode != null);
		assert (!pnode.isRoot());

		addActOutToMap(pnode);
	}

	public void addActOutToMap(ActualInOutNode pnode) {
		assert (pnode != null);
		assert (containsNode(pnode));
		assert (pnode.getPdgId() == getId());
		assert (pnode.isOut());
		assert (!pnode.isPrimitive());
		assert (pnode.getPointsTo() != null);

		for (InstanceKey ikey : pnode.getPointsTo()) {
			ikey2actout.add(ikey, pnode);
		}
	}

	public void addChildActInToMap(ActualInOutNode pnode) {
		assert (pnode != null);
		assert (!pnode.isRoot());

		addActInToMap(pnode);
	}

	public void addActInToMap(ActualInOutNode pnode) {
		assert (pnode != null);
		assert (containsNode(pnode));
		assert (pnode.getPdgId() == getId());
		assert (pnode.isIn());
		assert (!pnode.isPrimitive());
		assert (pnode.getPointsTo() != null);

		for (InstanceKey ikey : pnode.getPointsTo()) {
			ikey2actin.add(ikey, pnode);
		}
	}

	/**
	 * Gets all actual out nodes belonging to a method call
	 * @param call ssa instruction of the method call
	 * @return set off all actual out nodes belonging to the method call
	 */
	public Set<ActualInOutNode> getRootActOuts(CallNode call) {
		assert (call != null);
		assert (containsNode(call));
		assert (pdg.getAllCalls().contains(call));

		return call2rootActOut.get(call);
	}

	/**
	 * Get all actual out nodes that may-alias a specified instancekey.
	 * @param ikey instancekey, element of the wala points-to sets
	 * @return set of all may-aliasing actual out nodes
	 */
	public Set<ActualInOutNode> getActOutsForInstanceKey(InstanceKey ikey) {
		return ikey2actout.get(ikey);
	}

	public FormInOutNode getStaticFormIn(ParameterField field) {
		assert (field != null);
		assert (field.isStatic());

		return staticFormIns.get(field);
	}

	private Set<FormInOutNode> staticFormInsCache = null;
	public Set<FormInOutNode> getStaticFormIns() {
		if (staticFormInsCache == null) {
			staticFormInsCache = new HashSet<FormInOutNode>();
		}
		if (staticFormIns.values().size() != staticFormInsCache.size()) {
			staticFormInsCache.clear();
			staticFormInsCache.addAll(staticFormIns.values());
		}

		return staticFormInsCache;
	}

	public FormInOutNode getStaticFormOut(ParameterField field) {
		assert (field != null);
		assert (field.isStatic());

		return staticFormOuts.get(field);
	}

	private Set<FormInOutNode> staticFormOutsCache = null;
	public Set<FormInOutNode> getStaticFormOuts() {
		if (staticFormOutsCache == null) {
			staticFormOutsCache = new HashSet<FormInOutNode>();
		}
		if (staticFormOuts.values().size() != staticFormOutsCache.size()) {
			staticFormOutsCache.clear();
			staticFormOutsCache.addAll(staticFormOuts.values());
		}

		return staticFormOutsCache;
	}

	public void addStaticFormOut(FormInOutNode fOut) {
		ParameterField field = fOut.getField();

		assert (containsNode(fOut));
		assert (fOut.getPdgId() == getId());
		assert (field != null);
		assert (field.isStatic());
		assert (staticFormOuts.get(field) == null) : "FormInOutNode for field " + Util.fieldName(field)
			+ " is already set. Node already set is: " + staticFormOuts.get(field) + " Node trying to set: " +	fOut;

		if (!fOut.isPrimitive()) {
			addFormOutToMap(fOut);
		}

		staticFormOuts.put(field, fOut);
		rootFormOut.add(fOut);
	}

	public FormInOutNode getFormalInParameter(int paramNum) {
		for (FormInOutNode fIn : rootFormIn) {
			if (fIn.isParameter() && fIn.getParamId() == paramNum) {
				return fIn;
			}
		}

		throw new NoSuchElementException("No formal-in node found for paramter no. " + paramNum + "of " + toString());
	}

	public FormInOutNode getFormalOutParameter(int paramNum) {
		for (FormInOutNode fOut : rootFormOut) {
			if (fOut.isParameter() && fOut.getParamId() == paramNum) {
				return fOut;
			}
		}

		throw new NoSuchElementException("No formal-out node found for paramter  no. " + paramNum + "of " + toString());
	}


	/**
	 * get all formal-out parameter nodes
	 */
	public IParamSet<?> getModParams() {
		ObjTreeParamSet params = new ObjTreeParamSet();

		for (FormInOutNode fOut : getRootFormOuts()) {
			params.add(fOut);

			addChilds(params, fOut);
		}

		return params;
	}


	private static final <T extends ParameterNode<T>> void addChilds(ObjTreeParamSet params, T node) {
		if (node.hasChilds()) {
			for (T child : node.getChilds()) {
				params.add(child);
				addChilds(params, child);
			}
		}
	}

	/**
	 * get all formal-in parameter nodes
	 */
	public IParamSet<?> getRefParams() {
		ObjTreeParamSet params = new ObjTreeParamSet();

		for (FormInOutNode fOut : getRootFormIns()) {
			params.add(fOut);

			addChilds(params, fOut);
		}

		return params;
	}

	public IParamSet<?> getModParams(CallNode call) {
		ObjTreeParamSet params = new ObjTreeParamSet();

		for (ActualInOutNode aOut : getRootActOuts(call)) {
			params.add(aOut);

			addChilds(params, aOut);
		}

		return params;
	}

	public IParamSet<?> getRefParams(CallNode call) {
		ObjTreeParamSet params = new ObjTreeParamSet();

		for (ActualInOutNode aOut : getRootActIns(call)) {
			params.add(aOut);

			addChilds(params, aOut);
		}

		return params;
	}

	public AbstractParameterNode getMatchingActualNode(CallNode call, PDG callee, AbstractParameterNode formalNode) {
		if (!formalNode.isFormal()) {
			throw new IllegalArgumentException("argument has to be a formal node");
		}

		assert (formalNode.getPdgId() == callee.getId());

		AbstractParameterNode match = null;

		if (formalNode.isIn()) {
			Set<ActualInOutNode> actIns = getRootActIns(call);

			match = ObjTreeUtil.searchForMatchingNode(actIns, (FormInOutNode) formalNode);
		} else {
			Set<ActualInOutNode> actOuts = getRootActOuts(call);

			match = ObjTreeUtil.searchForMatchingNode(actOuts, (FormInOutNode) formalNode);
		}

		return match;
	}

	public AbstractParameterNode getMatchingFormalNode(CallNode call, AbstractParameterNode actualNode, PDG callee) {
		if (!actualNode.isActual()) {
			throw new IllegalArgumentException("argument hast to be actual node");
		}

		assert (callee.getParamModel() instanceof ObjTreeParamModel);

		AbstractParameterNode match = null;
		ObjTreeParamModel model = (ObjTreeParamModel) callee.getParamModel();

		if (actualNode.isIn()) {
			Set<FormInOutNode> formIns = model.getRootFormIns();

			match = ObjTreeUtil.searchForMatchingNode(formIns, (ActualInOutNode) actualNode);
		} else {
			Set<FormInOutNode> formOuts = model.getRootFormOuts();

			match = ObjTreeUtil.searchForMatchingNode(formOuts, (ActualInOutNode) actualNode);
		}

		return match;
	}

	/***
	 *  Parameter creation stuff
	 */

	/*
	 * BEGIN Actual-in/out
	 */

	public ActualInOutNode makeActualInOut(FormInOutNode formNode, SSAInvokeInstruction instr, OrdinalSet<InstanceKey> pts) {
		assert (pts != null);
		assert (!formNode.isPrimitive());

		ActualInOutNode node = new ActualInOutNode(getId(), formNode, instr, pts, null);
		node.setLabel(formNode.getLabel());
		pdg.addNode(node);

		return node;
	}

	public ActualInOutNode makeActualInOut(FormInOutNode formNode, SSAInvokeInstruction instr) {
		assert (formNode.isPrimitive());

		ActualInOutNode node = new ActualInOutNode(getId(), formNode, instr, null, null);
		node.setLabel(formNode.getLabel());
		pdg.addNode(node);

		return node;
	}

	public ActualInOutNode makeActualInOut(boolean isIn, SSAInvokeInstruction invk, int ssaVar, int paramId,
			OrdinalSet<InstanceKey> pts) {
		assert (pts != null);
		// parameter type omits explicit this pointer - so have we
		if (invk.isStatic()) {
			assert (!invk.getDeclaredTarget().getParameterType(paramId).isPrimitiveType());
		} else if (paramId > 0) {
			assert (!invk.getDeclaredTarget().getParameterType(paramId - 1).isPrimitiveType());
		}

		ActualInOutNode node;

		if (isIn && pts.isEmpty()) {
			node = new ActualInExclusionNode(getId(), invk, ssaVar, paramId, pts);
		} else {
			assert (!pts.isEmpty());

			node = new ActualInOutNode(getId(), isIn, invk, ssaVar, paramId, pts);
		}

		pdg.addNode(node);

		return node;
	}

	public ActualInOutNode makeActualInPrimitive(SSAInvokeInstruction invk, int ssaVar, int paramId) {
		// parameter type omits explicit this pointer - so have we
		if (invk.isStatic()) {
			assert (invk.getDeclaredTarget().getParameterType(paramId).isPrimitiveType());
		} else if (paramId > 0) {
			assert (invk.getDeclaredTarget().getParameterType(paramId - 1).isPrimitiveType());
		}

		ActualInOutNode node = new ActualInOutNode(getId(), true, invk, ssaVar, paramId, Type.NORMAL);

		pdg.addNode(node);

		return node;
	}

	public ActualInOutNode makeActualInConstant(SSAInvokeInstruction invk, int ssaVar, int paramId,
			OrdinalSet<InstanceKey> pts) {
		assert (pts != null);
		// parameter type omits explicit this pointer - so have we
		if (invk.isStatic()) {
			assert (!invk.getDeclaredTarget().getParameterType(paramId).isPrimitiveType());
		} else if (paramId > 0) {
			assert (!invk.getDeclaredTarget().getParameterType(paramId - 1).isPrimitiveType());
		}

		ActualInOutNode node;

		if (pts.isEmpty()) {
			node = new ActualInExclusionNode(getId(), invk, ssaVar, paramId, pts);
		} else {
			node = new ActualInOutNode(getId(), true, invk, ssaVar, paramId, pts);
		}

		pdg.addNode(node);

		return node;
	}

	public ActualInOutNode makeActualOutException(int ssaVar, SSAInvokeInstruction instr, OrdinalSet<InstanceKey> pts) {
		assert (pts != null);

		ActualInOutNode node = new ActualInOutNode(getId(), false, ssaVar, Type.EXCEPTION, instr, pts);

		pdg.addNode(node);

		return node;
	}

	public ActualInOutNode makeActualOutReturn(int ssaVar, SSAInvokeInstruction instr, OrdinalSet<InstanceKey> pts) {
		assert (pts != null);

		ActualInOutNode node = new ActualInOutNode(getId(), false, ssaVar, Type.RETURN, instr, pts);

		pdg.addNode(node);

		return node;
	}

	public ActualInOutNode makeActualOutReturnPrimitive(int ssaVar, SSAInvokeInstruction instr) {
		ActualInOutNode node = new ActualInOutNode(getId(), false, instr, ssaVar, null, Type.RETURN);

		pdg.addNode(node);

		return node;
	}

	/*
	 * END Actual-in/out
	 */

	/*
	 * BEGIN Form-in/out
	 */


	public FormInOutNode makeFormInOutStatic(boolean isIn, ParameterField field, OrdinalSet<InstanceKey> pts) {
		assert (pts != null);
		assert (!field.isPrimitiveType());
		assert (field.isStatic());

		FormInOutNode node = new FormInOutNode(getId(), isIn, field, pts);

		pdg.addNode(node);

		return node;
	}

	public FormInOutNode makeFormInOutStaticPrimitive(boolean isIn, ParameterField field) {
		assert (field.isPrimitiveType());
		assert (field.isStatic());

		FormInOutNode node = new FormInOutNode(getId(), isIn, field);

		pdg.addNode(node);

		return node;
	}

	public <T extends ParameterNode<T>>	FormInOutNode makeFormInOut(T copyFrom, OrdinalSet<InstanceKey> pts) {
		assert (pts != null);
		assert (!copyFrom.isPrimitive());

		FormInOutNode node = new FormInOutNode(getId(), copyFrom, pts);
		node.setLabel(copyFrom.getLabel());

		pdg.addNode(node);

		return node;
	}

	public <T extends ParameterNode<T>> FormInOutNode makeFormInOut(T copyFrom) {
		assert (copyFrom.isPrimitive());

		FormInOutNode node = new FormInOutNode(getId(), copyFrom, null);
		node.setLabel(copyFrom.getLabel());

		pdg.addNode(node);

		return node;
	}

	public <T extends ParameterNode<T>>	FormInOutNode makeFormInOut(T copyFrom, boolean isIn, OrdinalSet<InstanceKey> pts) {
		assert (pts != null);
		assert (!copyFrom.isPrimitive());

		FormInOutNode node = new FormInOutNode(getId(), copyFrom, isIn, pts);
		node.setLabel(copyFrom.getLabel());

		pdg.addNode(node);

		return node;
	}


	/**
	 * Used to create the root formal in parameter nodes (this, param 0-n)
	 */
	public FormInOutNode makeFormInOut(boolean isIn, int paramNum, int ssaVar, OrdinalSet<InstanceKey> pts) {
		assert (pts != null);

		FormInOutNode node = new FormInOutNode(getId(), isIn, paramNum, ssaVar, pts);

		pdg.addNode(node);

		return node;
	}


	public FormInOutNode makeFormInPrimitive(int paramNum, int ssaVar) {
		FormInOutNode node = new FormInOutNode(getId(), paramNum, ssaVar);

		pdg.addNode(node);

		return node;
	}

	public ExceptionExitNode makeFormOutException() {
		ExceptionExitNode node = new ExceptionExitNode(getId());

		node.setLabel("_exception_");
		pdg.addNode(node);

		return node;
	}

	/*
	 * END Form-in/out
	 */

	public ExitNode makeExit(String methodSig, boolean isPrimitive) {
		ExitNode node = new ExitNode(getId(), isPrimitive);

		node.setLabel(methodSig);
		pdg.addNode(node);

		return node;
	}

	public ExitNode makeExitVoid(String methodSig) {
		ExitNode node = new ExitNode(getId());

		node.setLabel(methodSig);
		pdg.addNode(node);

		return node;
	}

	public AbstractParameterNode makeExceptionalExit() {
		return makeFormOutException();
	}

	public AbstractParameterNode makeExit() {
		TypeReference ret = pdg.getMethod().getReturnType();
		if (ret != null && ret != TypeReference.Void) {
			return makeExit(pdg.getRoot().getLabel(), ret.isPrimitiveType());
		} else {
			return makeExitVoid(pdg.getRoot().getLabel());
		}
	}

	/*
	 * BEGIN Param child stuff
	 */

	public int formInObjFields = 0;
	public int formOutObjFields = 0;

	public <A extends ParameterNode<A>> A makeParameterNodeChild(A parent, ParameterField field,
			OrdinalSet<InstanceKey> pts) throws PDGFormatException {
		A child = makeParameterNodeChild2(parent, field, pts);

		if (child.getPdgId() == getId()) {
			if (child.isOut() && child instanceof ActualInOutNode) {
				addChildActOutToMap((ActualInOutNode) child);
			} else if (child.isIn() && child instanceof FormInOutNode) {
				addChildFormInToMap((FormInOutNode) child);
			} else if (child.isIn() && child instanceof ActualInOutNode) {
				addChildActInToMap((ActualInOutNode) child);
			} else if (child.isOut() && child instanceof FormInOutNode) {
				addChildFormOutToMap((FormInOutNode) child);
			}
		}
		return child;
	}

	private <A extends ParameterNode<A>> A makeParameterNodeChild2(A parent, ParameterField field,
			OrdinalSet<InstanceKey> pts) throws PDGFormatException {
		assert (pts != null);
		assert (!field.isStatic()) : "Static fields have to be root.";
		assert (parent.getPdgId() == getId());

		// In addition to the unfolding criterion we also check iff it is tried
		// to add a node to itself as a child.
		if (parent.getField() == field && Util.setsEqual(parent.getPointsTo(), pts)) {
			throw new PDGFormatException("Trying to add child field node that is identical to parent: " + parent);
		} else if (parent != unfold.findNodeMatchingUnfoldingCriterion(parent, field)) {
			throw new PDGFormatException("Node " + parent + " does not match the unfolding criterion.");
		}

		if (parent instanceof FormInOutNode && parent.isIn()) {
			formInObjFields++;
		} else if (parent instanceof FormInOutNode && parent.isOut()) {
			formOutObjFields++;
		}

		A child = parent.createChild(field, pts);

		child.setLabel(Util.fieldName(field));

		pdg.addNode(child);

		pdg.addParameterChildDependency(parent, child);

		return child;
	}

	public <A extends ParameterNode<A>> A makeParameterNodePrimitiveChild(A parent, ParameterField field, Integer ssaVar) {
		assert (parent.getPdgId() == getId());
		assert (!field.isStatic()) : "Static fields have to be root.";
		assert (field.isPrimitiveType());

		if (parent instanceof FormInOutNode && parent.isIn()) {
			formInObjFields++;
		} else if (parent instanceof FormInOutNode && parent.isOut()) {
			formOutObjFields++;
		}

		A child = parent.createPrimitiveChild(field, ssaVar);

		child.setLabel(Util.fieldName(field));

		pdg.addNode(child);

		pdg.addParameterChildDependency(parent, child);

		return child;
	}

	public AbstractParameterNode makeVoidActualOut(CallNode call, PDG target) {
		ActualInOutNode node = makeActualOutReturnPrimitive(-1, call.getInstruction());

		node.setLabel("void stub return");
		pdg.addParameterChildDependency(call, node);
		addRootActOut(call, node);
		target.addNode(node);
		SDG.addParameterOutDependency(target, target.getExit(), node);

		return node;
	}

}
