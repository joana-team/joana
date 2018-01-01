/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractThrowInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.NormalNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterFieldFactory;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamComputation;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamModel;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp.ObjTreeUnfoldingCriterion;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.deprecated.jsdg.util.Util;

/**
 * Intraprocedural part of the object tree computation.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
@SuppressWarnings("deprecation")
public abstract class ObjTreeIntraprocParamComputation implements IParamComputation {

	protected final ObjTreeUnfoldingCriterion unfold;

	public ObjTreeIntraprocParamComputation(ObjTreeUnfoldingCriterion unfold) {
		this.unfold = unfold;
	}

	public IParamModel getBasicModel(PDG pdg) {
		ObjTreeParamModel model =  new ObjTreeParamModel(pdg, unfold);

		return model;
	}

	public void computeModRef(PDG pdg, IProgressMonitor monitor) throws PDGFormatException {
		IParamModel model = pdg.getParamModel();
		if (!(model instanceof ObjTreeParamModel)) {
			throw new IllegalArgumentException(
				"Mod/Ref can only be computed for an ObjTreeParamModel: " +
				model.getClass().getName());
		}

		ObjTreeParamModel objTree = (ObjTreeParamModel) model;

		createFormInFromSignature(pdg, objTree);

		if (!pdg.isStub() && !(pdg.getIR() == null)) {
			visitGetSetInvoke(pdg, objTree);

			computeIntraproceduralInterface(pdg, objTree);
		}

		registerReturnAndExceptionalReturn(pdg, objTree);

		Log.info(objTree.getRootFormIns().size() + " root form-in nodes, " +
				objTree.getRootFormOuts().size() + " root form-out nodes");
		Log.info(objTree.getStaticFormIns().size() + " static form-in nodes, " +
				objTree.getStaticFormOuts().size() + " static form-out nodes");
		Log.info(objTree.formInObjFields + " form-in object field nodes, " +
				objTree.formOutObjFields + " form-out obj field nodes");
	}

	private void createFormInFromSignature(PDG pdg, ObjTreeParamModel model) {
		int stopAt = pdg.getMethod().getNumberOfParameters();
		IR ir = pdg.getIR();

		for (int paramNum = 0; paramNum < stopAt; paramNum++) {
			int ssaVar = (ir == null ? -1 : ir.getParameter(paramNum));
//TODO add location in sourcecode information to the parameter nodes.
// this has not been done till now because shrike only delivers line positions
// for statements.
// But when using CAsT we may extract exact parameter positions
			FormInOutNode fInParam;
			if (pdg.getMethod().getParameterType(paramNum).isPrimitiveType()) {
				fInParam = model.makeFormInPrimitive(paramNum, ssaVar);
				fInParam.setLabel("primitive param " + paramNum);
			} else {
				if (ir != null) {
					OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(ssaVar);
					fInParam = model.makeFormInOut(true, paramNum, ssaVar, pts);
				} else {
					OrdinalSet<InstanceKey> pts = OrdinalSet.empty();
					fInParam = model.makeFormInOut(true, paramNum, ssaVar, pts);
				}
				if (!pdg.getMethod().isStatic() && paramNum == 0) {
					fInParam.setLabel("this");
				} else {
					fInParam.setLabel("param " + paramNum);
				}
			}

			model.addRootFormIn(fInParam);
			if (ir != null) {
				pdg.addDefinesVar(fInParam, ssaVar);
			}
			pdg.addParameterChildDependency(pdg.getRoot(), fInParam);
		}
	}

	private void registerReturnAndExceptionalReturn(PDG pdg, ObjTreeParamModel model) {
		// add output dependencies of return statements
		if (!pdg.getExit().isVoid()) {
			createReturnDataDependencies(pdg, model);
		}

		// add throw statements
		if (!pdg.isIgnoreExceptions() && !pdg.getThrows().isEmpty()) {
			createExceptionDataDepencendies(pdg, model);
		}

	}

	private void createReturnDataDependencies(PDG pdg, ObjTreeParamModel model) {
		// hashcode e.g. is a synthetic method with a primitive return type
		// TODO check what to do with synthetic methods that have a
		// non-primitive return type -> we issue a warning for now and skip them
		//
		// There may be Methods that do have an empty return set and are neither
		// primitive nor synthetic. This happens if they throw an exception:
	    // T childValue(T parentValue) {
	    //     throw new UnsupportedOperationException();
	    // }
		ExitNode exit = (ExitNode) pdg.getExit();

		/*
		 * Constructors always have the returntype void, but in their case
		 * the exit node models the returning of the created object. So the
		 * exit node is neiter void nor primitive but the returns are. This
		 * would screw up the code below.
		 */
		if (!pdg.getMethod().isInit()) {
			for (NormalNode ret : pdg.getReturns()) {

				SSAInstruction instr = pdg.getInstructionForNode(ret);
				int def = instr.getUse(0);

				assert (def >= 0);

				pdg.addDataDependency(ret, exit, def);

				if (!exit.isPrimitive()) {
					exit.mergePointsTo(pdg.getPointsToSet(def));
				}
			}

			if (!exit.isVoid()) {
				model.addRootFormOut(exit);
			}
		}

		// the points to information of the exit node is just the merge of
		// all return nodes that point to this exit node (as done above)
		if (exit.getPointsTo() != null || exit.isPrimitive()) {
			model.addRootFormOut(exit);
		} else {
			Log.warn("Non-primitive exit node has not points-to set. " +
					Util.methodName(pdg.getMethod()) + " is " +
					(pdg.getMethod().isSynthetic() ? "synthetic" : "not synthetic"));
		}
	}

	private void createExceptionDataDepencendies(PDG pdg, ObjTreeParamModel model) {
		// compute merged points-to set of all exception returns
		OrdinalSet<InstanceKey> pts = null;
		for (NormalNode node : pdg.getThrows()) {
			SSAInstruction instr = pdg.getInstructionForNode(node);

			assert (instr instanceof SSAAbstractThrowInstruction);

			if (pts == null) {
				if (instr.getDef() < 0) {
					continue;
				}
				pts= pdg.getPointsToSet(instr.getDef());
			} else {
				pts = OrdinalSet.unify(pts,	pdg.getPointsToSet(instr.getDef()));
			}
		}

		if (pts == null) {
			Log.error("No points-to set for exceptions in " + pdg.getMethod() + "found");
			return;
		}

		FormInOutNode exitThrow = (FormInOutNode) pdg.getExceptionalExit();
		exitThrow.mergePointsTo(pts);

		pdg.addParameterChildDependency(pdg.getRoot(), exitThrow);

		for (NormalNode node : pdg.getThrows()) {
			SSAInstruction instr = pdg.getInstructionForNode(node);

			assert (instr instanceof SSAAbstractThrowInstruction);

			int exc = ((SSAAbstractThrowInstruction) instr).getException();

			pdg.addDataDependency(node, exitThrow, exc);
		}

		model.addRootFormOut(exitThrow);
	}


	private void visitGetSetInvoke(PDG pdg, ObjTreeParamModel model) {
		ObjTreeBasicNodeCreationVisitor visitor =
			new ObjTreeBasicNodeCreationVisitor(pdg, model);

		for (Iterator<SSAInstruction> it = pdg.getIR().iterateNormalInstructions(); it.hasNext();) {
			SSAInstruction instr = it.next();
			if (instr != null) {
				instr.visit(visitor);
			}
		}
	}

	/**
	 * Asserts that all formal-in/out root nodes have already been built.
	 * Static reads are built on-the-fly while visiting the ir nodes.
	 * Method parameter nodes are created at the very beginning using the
	 * method signature.
	 */
	private boolean assertAllFormalRootNodesBuilt(PDG pdg, ObjTreeParamModel model) {
		for (int i = 0; i < pdg.getMethod().getNumberOfParameters(); i++) {
			FormInOutNode fIn = model.getFormalInParameter(i);

			assert (fIn != null);
			assert (fIn.isParameter());
		}

		assert (pdg.getExit() != null);

		if (pdg.getMethod().getReturnType() == TypeReference.Void) {
			assert (pdg.getExit().isVoid());
		} else {
			assert (!pdg.getExit().isVoid());
			assert (pdg.getMethod().getReturnType().isPrimitiveType() == pdg.getExit().isPrimitive());
		}

		Predicate<SSAFieldAccessInstruction> filterStatic =
			new Predicate<SSAFieldAccessInstruction>() {
				public boolean test(SSAFieldAccessInstruction o) {
					return o.isStatic();
				}
		};


		FilterIterator<SSAGetInstruction> itGet =
			new FilterIterator<SSAGetInstruction>(pdg.getGets().iterator(), filterStatic);

		while (itGet.hasNext()) {
			SSAGetInstruction put = itGet.next();
			IField ifield = pdg.getHierarchy().resolveField(put.getDeclaredField());
			ParameterField field = ParameterFieldFactory.getFactory().getObjectField(ifield);
			FormInOutNode fIn = model.getStaticFormIn(field);
			assert (fIn != null);
		}

		FilterIterator<SSAPutInstruction> itSet =
			new FilterIterator<SSAPutInstruction>(pdg.getSets().iterator(), filterStatic);

		while (itSet.hasNext()) {
			SSAPutInstruction put = itSet.next();
			IField ifield = pdg.getHierarchy().resolveField(put.getDeclaredField());
			if (ifield != null) {
				ParameterField field = ParameterFieldFactory.getFactory().getObjectField(ifield);
				FormInOutNode fOut = model.getStaticFormOut(field);
				assert (fOut != null);
			}
		}

		return true;
	}

	private static boolean assertPut(SSAPutInstruction put, ObjTreeParamModel model, ParameterField field) {
		if (put.isStatic()) {
			// we assume by precondition that form-ins exist for all
			// static field accesses
			FormInOutNode fOut = model.getStaticFormOut(field);
			assert (fOut != null);

			// when a static field is written all object fields that are referenced
			// from this root node may have changed - so we have to add them
			if (!fOut.isPrimitive()) {
				assert (fOut.getPointsTo() != null);
				// empty field nodes are ok iff they are leaves
				assert (! (fOut.getPointsTo().isEmpty() && fOut.hasChilds()));
			}
		}

		return true;
	}

	private void computeIntraproceduralInterface(PDG pdg, ObjTreeParamModel model) throws PDGFormatException {
		//TODO this method is the bottleneck of the intraprocedural analysis
		assert assertAllFormalRootNodesBuilt(pdg, model);

		boolean changed;
		do {
			changed = false;

			for (SSAPutInstruction put : pdg.getSets()) {
				IField ifield = pdg.getHierarchy().resolveField(put.getDeclaredField());

				// Thread.start referenced a field runnable that can not be resolved
				// so we have unresolved fields here which are null. We just skip them for now.
				if (ifield == null) {
					Log.warn("We have to skip instruction " + put +
							" because the field " + put.getDeclaredField()
							+  " can not be resolved.");

					continue;
				}

				ParameterField field = ParameterFieldFactory.getFactory().getObjectField(ifield);

				assert assertPut(put, model, field);

				if (put.getRef() < 0) {
					Log.warn("Skipping: The points-to set for the object "
							+ "whose field " + Util.fieldName(field)
							+ " has been changed is not availiable.");

						// skip as we cannot analyze anything without points-to
						// results

						continue;
				}

				OrdinalSet<InstanceKey> ikRef = pdg.getPointsToSet(put.getRef());

				if (ikRef == null || ikRef.isEmpty()) {
					Log.warn("Skipping: The points-to set for the object "
						+ "whose field " + Util.fieldName(field)
						+ " has been changed is empty.");

					// skip as we cannot analyze anything without points-to
					// results

					continue;
				}

				changed |= adjustFormalNodesToFieldSetAccess(pdg, model, ikRef, field);
			}

			for (SSAArrayStoreInstruction aput : pdg.getArraySets()) {
				TypeReference tRef = aput.getElementType();
				ParameterField field = ParameterFieldFactory.getFactory().getArrayField(tRef);

				if (aput.getArrayRef() < 0) {
					Log.warn("Skipping: The points-to set for the array "
							+ "whose field " + Util.fieldName(field)
							+ " has been changed is not availiable.");

						// skip as we cannot analyze anything without points-to
						// results

						continue;
				}


				OrdinalSet<InstanceKey> ikRef = pdg.getPointsToSet(aput.getArrayRef());

				if (ikRef == null || ikRef.isEmpty()) {
					Log.warn("Skipping: array access to an object "
						+ "that does not have a points-to set: "
						+ aput.toString(pdg.getIR().getSymbolTable()));

					continue;
				}

				changed |= adjustFormalNodesToFieldSetAccess(pdg, model, ikRef, field);
			}

			for (SSAGetInstruction get : pdg.getGets()) {
				if (!get.isStatic()) {
					IField ifield = pdg.getHierarchy().resolveField(get.getDeclaredField());

					/*
					 * Thread.start referenced a field runnable that can not be
					 * resolved. So we have unresolved fields here which are null.
					 * We just skip them for now.
					 */
					if (ifield == null) {
						Log.warn("We have to skip instruction " + get +
								" because the field " + get.getDeclaredField()
								+  " can not be resolved.");
						continue;
					}

					ParameterField field = ParameterFieldFactory.getFactory().getObjectField(ifield);

					OrdinalSet<InstanceKey> ikRef = pdg.getPointsToSet(get.getRef());

					if (ikRef == null || ikRef.isEmpty()) {
						Log.warn("Cannot create an object field for an object "
							+ "that does not have a points-to set: "
							+ toString() + " IR: "
							+ get.toString(pdg.getIR().getSymbolTable()));

						continue;
					}

					changed |= adjustFormalNodesToFieldGetAccess(pdg, model, ikRef, field);
				}
			}

			for (SSAArrayLoadInstruction aget : pdg.getArrayGets()) {
				TypeReference tRef = aget.getElementType();
				ParameterField afield = ParameterFieldFactory.getFactory().getArrayField(tRef);

				OrdinalSet<InstanceKey> ikRef = pdg.getPointsToSet(aget.getArrayRef());

				if (ikRef == null || ikRef.isEmpty()) {
					Log.warn("Cannot create an object field for an object "
						+ "that does not have a points-to set: "
						+ toString() + " IR: "
						+ aget.toString(pdg.getIR().getSymbolTable()));

					continue;
				}

				changed |= adjustFormalNodesToFieldGetAccess(pdg, model, ikRef, afield);
			}
		} while (changed);

	}

	/**
	 * Adds a field node to all in nodes which are aliased to the given
	 * points-to set.
	 *
	 * @param ikRefs
	 *            The points-to set of the reference of the field access
	 * @param field
	 *            The accessed or written field
	 * @param tree
	 *            MultiTree The field structure of the object which is written
	 *            to the field, null if this reads the field
	 * @return whether a parameter object tree has changed
	 * @throws PDGFormatException
	 */
	private boolean adjustFormalNodesToFieldGetAccess(PDG pdg, ObjTreeParamModel model,
			OrdinalSet<InstanceKey> ikRefs, ParameterField field) throws PDGFormatException {
		boolean changed = false;

		Set<FormInOutNode> formIns = model.getMayAliasingFormIns(ikRefs);
		formIns = unfold.filterNodesWithUnfoldingCriterion(formIns, field);

		/**
		 * We only act iff a form-in node matching the points-to element ik
		 * has been found. Otherwise we do nothing and wait for the next
		 * execution of this method and hopefully a matching form-in node
		 * has been created by then.
		 * This works because the "base" form-ins have already been added
		 * for the parameters and static field accesses.
		 * This way the object trees are added top down to the form-in/out
		 * nodes.
		 *
		 * For local variables it may also be that theres no form-in node
		 * necessary because the variable root is inside the method.
		 */
		if (formIns != null && !formIns.isEmpty()) {
			/**
			 * create a child node matching field <field> for all nodes
			 * that may alias the points-to node ik
			 */
			for (FormInOutNode fIn : formIns) { // almost always true....
				assert (!fIn.isPrimitive()) : "Theres no way a primitive type node should be part of a may-alias set";

				Pair<FormInOutNode, Boolean> result =
					findOrCreateChildForField(pdg, model, fIn, field);

				changed |= result.snd;
			}
		}

		return changed;
	}

	/**
	 * Adds a field node to all out nodes which are aliased to the given
	 * points-to set.
	 * Searches for all in nodes who may-alias the base reference and creates
	 * a form-out node for them.
	 *
	 *
	 * @param ikRefs
	 *            The points-to set of the reference of the field access
	 * @param field
	 *            The accessed or written field
	 * @param tree
	 *            MultiTree The field structure of the object which is written
	 *            to the field, null if this reads the field
	 * @return whether a parameter object tree has changed
	 * @throws PDGFormatException
	 */
	private boolean adjustFormalNodesToFieldSetAccess(PDG pdg, ObjTreeParamModel model,
			OrdinalSet<InstanceKey> ikRefs, ParameterField field) throws PDGFormatException {
		boolean changed = false;

		changed |= copyMayAliasingFormInsToFormOut(pdg, model, ikRefs, field);

		Set<FormInOutNode> formOuts = model.getMayAliasingFormOuts(ikRefs);
		formOuts = unfold.filterNodesWithUnfoldingCriterion(formOuts, field);

		/**
		 * We only act iff a form-in node matching the points-to element ik
		 * has been found. Otherwise we do nothing and wait for the next
		 * execution of this method and hopefully a matching form-in node
		 * has been created by then.
		 * This works because the "base" form-ins have already been added
		 * for the parameters and static field accesses.
		 * This way the object trees are added top down to the form-in/out
		 * nodes.
		 *
		 * For local variables it may also be that there is no form-in node
		 * necessary because the variable root is inside the method.
		 */
		if (formOuts != null && !formOuts.isEmpty()) {
			/**
			 * create a child node matching field <field> for all nodes
			 * that may alias the points-to node ik
			 */
			for (FormInOutNode fOut : formOuts) { // almost always true....
				assert (!fOut.isPrimitive()) : "Theres no way a primitive type node should be part of a may-alias set";

				//TODO perhaps deliver the points-to set of the put operation
				// this should be merged to the existing nodes or assigned to newly
				// created ones
				Pair<FormInOutNode, Boolean> result =
					findOrCreateChildForField(pdg, model, fOut, field);

				changed |= result.snd;
			}
		} else {
			assert assertCreatedNodesCanBeFound(unfold, model, ikRefs, field, changed);
		}

		return changed;
	}

	private static boolean assertCreatedNodesCanBeFound(ObjTreeUnfoldingCriterion unfold, ObjTreeParamModel model,
			OrdinalSet<InstanceKey> ikRefs, ParameterField field, boolean changed) {
		// when no may-aliasing formal outs have been found no may-aliasing
		// formal-out nodes should have been created before
		// -> so changed has to be false
		if (changed) {
			Set<FormInOutNode> formIns = model.getMayAliasingFormIns(ikRefs);
			formIns = unfold.filterNodesWithUnfoldingCriterion(formIns, field);

			Log.debug("May-aliasing formal-ins were: ");
			for (FormInOutNode fIn : formIns) {
				Log.debug(fIn.toString());
			}
			Log.debug("But no formal-out has been found.");
		}

		assert (!changed) : "A may-aliasing formal-out node has been created, but it was not found afterwards.";

		return true;
	}

	/**
	 * Searches all formal-in nodes may-aliasing the given points-to set
	 * and copies them to a matching formal-out node (if none existed before)
	 *
	 * @param ikRefs points-to set
	 * @return true if a new formal out node had to be created
	 * @throws PDGFormatException
	 */
	private boolean copyMayAliasingFormInsToFormOut(PDG pdg, ObjTreeParamModel model,
			OrdinalSet<InstanceKey> ikRefs,	ParameterField field) throws PDGFormatException {
		boolean changed = false;

		Set<FormInOutNode> formIns = model.getMayAliasingFormIns(ikRefs);
		formIns = unfold.filterNodesWithUnfoldingCriterion(formIns, field);

		if (formIns != null && !formIns.isEmpty()) {
			for (FormInOutNode fIn : formIns) {
				changed |= createMatchingFormOut(pdg, model, fIn);
			}
		}

		return changed;
	}

	/**
	 * Adds a child node for <it>field</it> to <it>parent</it> if necessary and
	 * adds them to the proper map for the may-alias search.
	 *
	 * @param parent
	 *            ParameterNode
	 * @param field
	 *            IField
	 * @return A Pair consisting of the child node and a Boolean set to true
	 * 			iff the child had to be created.
	 * @throws PDGFormatException
	 */
	private <T extends ParameterNode<T>> Pair<T, Boolean>
	findOrCreateChildForField(PDG pdg, ObjTreeParamModel model, T parent,
			ParameterField field) throws PDGFormatException {
		boolean changed = false;

		T child = parent.getChildForField(field);

		// when the parent according to the unfodling criterion does not have
		// a child for the field field, we are allowed to create one.
		if (child == null) {
			if (field.isPrimitiveType()) {
				child = model.makeParameterNodePrimitiveChild(parent, field, null);
			} else {
				OrdinalSet<InstanceKey> pts =
					pdg.getPointsToSetForObjectField(parent.getPointsTo(), field);

				/*
				 * Wala may exclude some parts of the program from the points-to
				 * analysis, so the points-to set may be empty.
				 * An object field with empty points-to set does not make sense,
				 * as no data flow can later on be calculated.
				 * So we do not create a node at all and ignore it.
				 *
				 * Second option for not creating a child node is the unfolding
				 * criterion. If parent field and points to set equals the child
				 * to be created. We do not create it, as there is no more
				 * additional information we can get from these nodes.
				 */
				if (parent.getField() == field && Util.setsEqual(pts, parent.getPointsTo())) {
					return Pair.make(null, false);
				}

				child = model.makeParameterNodeChild(parent, field, pts);
			}

			changed = true;
		}

		return Pair.make(child, changed);
	}

	/**
	 * Looks for a form-out node matching the form-in node. If no form-out node
	 * is found a new one is created including the child and parent objecttree
	 * parts.
	 * @param in the form-in node a matching form-out node is searched for
	 * @throws PDGFormatException
	 */
	private boolean createMatchingFormOut(PDG pdg, ObjTreeParamModel model,
			FormInOutNode in) throws PDGFormatException {
		boolean changed = false;

		assert (in != null && in.isIn());

		/**
		 * look for the root element of the object tree and search the matching
		 * root form-out node
		 */
		List<FormInOutNode> rootPath = in.getRootPath();
		FormInOutNode inRoot = rootPath.get(rootPath.size() - 1);
		FormInOutNode outRoot = getCorrespondingFormOut(model.getRootFormOuts(), inRoot);

		/**
		 * build form-out root if no matching form-out has been found
		 */
		if (outRoot == null) {
			outRoot = model.makeFormInOut(inRoot, false, inRoot.getPointsTo());
			outRoot.setLabel(inRoot.getLabel());

			pdg.addParameterChildDependency(pdg.getRoot(), outRoot);
			if (outRoot.isStatic()) {
				model.addStaticFormOut(outRoot);
			} else {
				model.addRootFormOut(outRoot);
			}
			changed = true;
		}

		assert (inRoot.getPointsTo() != null);
		assert (outRoot.getPointsTo() != null);

		/**
		 * create path from form-out root node to the form-out node matching
		 * the root path from the form-in node to copy.
		 */
		FormInOutNode currentOut = outRoot;
		for (int i = rootPath.size() - 2; i >= 0; i--) {
			FormInOutNode inNode = rootPath.get(i);
			ParameterField field = inNode.getField();

			assert(field != null);

			FormInOutNode curOutChild = currentOut.getChildForField(field);

			if (curOutChild == null) {
				if (field.isPrimitiveType()) {
					curOutChild = model.makeParameterNodePrimitiveChild(currentOut, field, null /* no ssa var */);
				} else {
					OrdinalSet<InstanceKey> ptsField = inNode.getPointsTo();

					curOutChild = model.makeParameterNodeChild(currentOut, field, ptsField);
				}

				changed = true;
			}

			outRoot = curOutChild;
		}

		return changed;
	}

	/**
	 * Search the corresponding form-out node of a specified form-in node in
	 * a list of form-out nodes
	 * @param fOuts list of form-out nodes
	 * @param fIn form-in node
	 * @return the corresponding form-out node or null
	 */
	private static FormInOutNode getCorrespondingFormOut(Set<FormInOutNode> fOuts,
			FormInOutNode fIn) {
		if (fOuts != null) {
			for (FormInOutNode fOut : fOuts) {
				if (fIn.isMatchingFormOut(fOut)) {
					return fOut;
				}
			}
		}

		return null;
	}

}
