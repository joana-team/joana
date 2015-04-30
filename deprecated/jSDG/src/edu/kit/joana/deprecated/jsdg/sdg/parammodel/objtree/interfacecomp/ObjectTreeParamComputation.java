/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp;

import static edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp.InterproceduralInterfaceComputation.copyActInToFormIn;
import static edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp.InterproceduralInterfaceComputation.copyActOutToFormOut;
import static edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp.InterproceduralInterfaceComputation.copyFormInToActIn;
import static edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp.InterproceduralInterfaceComputation.copyFormOutToActOut;
import static edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp.InterproceduralInterfaceComputation.copyObjTree;
import static edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp.InterproceduralInterfaceComputation.getCorrespondantDifferInOut;
import static edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp.InterproceduralInterfaceComputation.getCorrespondantSameInOut;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG.Call;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ActualInOutNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.FormInOutNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ObjTreeParamModel;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ParameterNode;
import edu.kit.joana.deprecated.jsdg.util.Log;

/**
 * Generic unoptimized object tree propagation algorithm. Computes fixed point
 * of all object tree interfaces in the SDG.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ObjectTreeParamComputation {

	private final SDG sdg;
	private final ObjTreeUnfoldingCriterion unfold;

	private ObjectTreeParamComputation(SDG sdg, ObjTreeUnfoldingCriterion unfold) {
		this.sdg = sdg;
		this.unfold = unfold;
	}

	public static void compute(SDG sdg, ObjTreeUnfoldingCriterion unfold, IProgressMonitor progress)
	throws PDGFormatException, CancelException {
		ObjectTreeParamComputation treeprop = new ObjectTreeParamComputation(sdg, unfold);
		treeprop.compute(progress);
	}

	private void compute(IProgressMonitor progress) throws PDGFormatException, CancelException {
		propagateStaticFields(sdg, progress);

		Set<PDG> allMethods = sdg.getAllContainedPDGs();
		Set<PDG> intraChange =
			propagateIntraproceduralSideEffects(sdg, allMethods, progress);
		Set<PDG> interChange =
			propagateInterproceduralSideEffects(sdg, allMethods, progress);

		while (!intraChange.isEmpty() || !interChange.isEmpty()) {
			intraChange =
				propagateIntraproceduralSideEffects(sdg, allMethods, progress);
			/*
			 * intraChange contains a set of methods whose formal interface
			 * has been changed. This has to be propagated to all call sites.
			 */

			interChange =
				propagateInterproceduralSideEffects(sdg, allMethods, progress);

			/*
			 * interChange contains a set of methods whose internal actual-in/out
			 * nodes have been changed. They may be candidates for a changing
			 * formal interface.
			 */

			progress.worked(1);
			if (progress.isCanceled()) {
				throw CancelException.make("Operation aborted.");
			}
		}

		progress.done();
	}


	private static class ObjTreeCall {

		public final Call call;
		public final ObjTreeParamModel callerModel;
		public final ObjTreeParamModel calleeModel;

		public ObjTreeCall(Call call) {
			this.call = call;

			assert (call.caller.getParamModel() instanceof ObjTreeParamModel);
			assert (call.callee.getParamModel() instanceof ObjTreeParamModel);

			this.callerModel = (ObjTreeParamModel) call.caller.getParamModel();
			this.calleeModel = (ObjTreeParamModel) call.callee.getParamModel();
		}
	}

	private void propagateStaticFields(SDG sdg, IProgressMonitor progress) throws PDGFormatException, CancelException {
		Log.info("Propagation static fields for " + sdg.getAllCalls().size() + " calls");
		boolean changed;
		do {
			changed = false;
			for (Call call : sdg.getAllCalls()) {
				ObjTreeCall objCall = new ObjTreeCall(call);
				changed |= propagateStaticsIntraprocedural(objCall);
				changed |= propagateStaticsInterprocedural(sdg, objCall);

				if (progress.isCanceled()) {
					throw CancelException.make("Operation aborted.");
				}
			}
			progress.worked(1);
		} while (changed);
		Log.info("Propagation of static fields done.");
	}

	/**
	 * Propagates the static formal-in/out nodes of a called method to the
	 * actual-in/out nodes of its callsites
	 * @param call
	 * @return
	 * @throws PDGFormatException
	 */
	private boolean propagateStaticsIntraprocedural(ObjTreeCall call) throws PDGFormatException {
		boolean changed = false;

		for (FormInOutNode fIn : call.calleeModel.getStaticFormIns()) {
			ActualInOutNode aIn =
				getCorrespondantSameInOut(call.callerModel.getRootActIns(call.call.node), fIn);

			if (aIn == null) {
				changed = true;
				if (fIn.isPrimitive()) {
					aIn = call.callerModel.makeActualInOut(fIn, call.call.node.getInstruction());
				} else {
					aIn = call.callerModel.makeActualInOut(fIn, call.call.node.getInstruction(), fIn.getPointsTo());
				}
				SDG.addParameterInDependency(call.call.caller, aIn, fIn);
				call.call.caller.addParameterChildDependency(call.call.node, aIn);
				call.callerModel.addRootActIn(call.call.node, aIn);
				if (fIn.hasChilds()) {
					copyFormInToActIn(call.call.callee, fIn, call.call.caller, aIn, unfold);
				}
			}
		}

		for (FormInOutNode fOut : call.calleeModel.getStaticFormOuts()) {
			ActualInOutNode aOut =
				getCorrespondantSameInOut(call.callerModel.getRootActOuts(call.call.node), fOut);

			if (aOut == null) {
				changed = true;
				if (fOut.isPrimitive()) {
					aOut = call.callerModel.makeActualInOut(fOut, call.call.node.getInstruction());
				} else {
					aOut = call.callerModel.makeActualInOut(fOut, call.call.node.getInstruction(), fOut.getPointsTo());
				}
				SDG.addParameterOutDependency(call.call.callee, fOut, aOut);
				call.call.caller.addParameterChildDependency(call.call.node, aOut);
				call.callerModel.addRootActOut(call.call.node, aOut);
				if (fOut.hasChilds()) {
					copyFormOutToActOut(call.call.callee, fOut, call.call.caller, aOut, unfold);
				}
			}
		}

		return changed;
	}

	/**
	 * Propagates the static actual-in/out nodes of a call to the formal-in/out nodes
	 * of the caller.
	 * @param sdg
	 * @param call
	 * @return
	 * @throws PDGFormatException
	 */
	private boolean propagateStaticsInterprocedural(SDG sdg, ObjTreeCall call) throws PDGFormatException {
		boolean changed = false;

		for (ActualInOutNode aIn : call.callerModel.getRootActIns(call.call.node)) {
			if (aIn.isStatic()) {
				FormInOutNode fIn =
					getCorrespondantSameInOut(call.callerModel.getStaticFormIns(), aIn);

				if (fIn == null) {
					changed = true;
					if (!aIn.isPrimitive()) {
						fIn = call.callerModel.makeFormInOut(aIn, aIn.getPointsTo());
					} else {
						fIn = call.callerModel.makeFormInOut(aIn);
					}
					call.call.caller.addParameterChildDependency(call.call.caller.getRoot(), fIn);
					call.callerModel.addStaticFormIn(fIn);
					if (aIn.hasChilds()) {
						copyActInToFormIn(call.call.caller, aIn, call.call.caller, fIn, unfold);
					}
				}
			}
		}

		for (ActualInOutNode aOut : call.callerModel.getRootActOuts(call.call.node)) {
			if (aOut.isStatic()) {
				FormInOutNode fOut =
					getCorrespondantSameInOut(call.callerModel.getStaticFormOuts(), aOut);

				if (fOut == null) {
					changed = true;
					if (!aOut.isPrimitive()) {
						fOut = call.callerModel.makeFormInOut(aOut, aOut.getPointsTo());
					} else {
						fOut = call.callerModel.makeFormInOut(aOut);
					}
					call.call.caller.addParameterChildDependency(call.call.caller.getRoot(), fOut);
					call.callerModel.addStaticFormOut(fOut);
					if (aOut.hasChilds()) {
						copyActOutToFormOut(call.call.caller, aOut, call.call.caller, fOut, unfold);
					}
				}
			}
		}

		return changed;
	}

	private static <A extends ParameterNode<A>> void addAllChildNodes(Set<A> roots, Set<A> all) {
		all.addAll(roots);
		for (A root : roots) {
			if (root.hasChilds()) {
				Set<A> childs = root.getChilds();
				all.addAll(childs);
				addAllChildNodes(childs, all);
			}
		}
	}

	private <A extends ParameterNode<A>, B extends ParameterNode<B>>
	boolean copyObjTree2(SDG sdg, A from, B to) throws PDGFormatException {
		PDG fromPdg = sdg.getPdgForId(from.getPdgId());
		PDG toPdg = sdg.getPdgForId(to.getPdgId());

		return copyObjTree(fromPdg, from, toPdg, to, unfold);
	}

	private <T extends ParameterNode<T>,V extends ParameterNode<V>> boolean
	copyObjTreeToNodes(SDG sdg, T copyFrom, Set<V> copyToNodes) throws PDGFormatException {
		boolean changed = false;

		//TODO add newly created object fields to set of nodes (as a speedup)
		for (V copyTo : copyToNodes) {
			if (copyTo != copyFrom) {
				changed |= copyObjTree2(sdg, copyFrom, copyTo);
			}
		}

		return changed;
	}

	private <A extends ParameterNode<A>, B extends ParameterNode<B>> boolean
	copyToAliasingNodes(SDG sdg, Set<A> nodes, B copyFrom) throws PDGFormatException {
		assert (!copyFrom.isPrimitive()) : "Node is primitive, no aliasing nodes can exist: " + copyFrom;
//			Assertions._assert(!copyFrom.isFieldAccess(),
//				"Not a field access: " + copyFrom);
		assert (copyFrom.getPointsTo() != null) : "Points-to set is null and no ssa var: " + copyFrom;

		Set<A> filteredNodes = unfold.filterNodesWithUnfoldingCriterion(nodes, copyFrom.getField());

		return copyObjTreeToNodes(sdg, copyFrom, filteredNodes);
	}

	/**
	 * fIn may be a form-in node in the middle of an objecttree. As it
	 * was written to we copy this node and its root nodes to the matching
	 * form-out node. If these form-out nodes do not exists we create them.
	 * @throws PDGFormatException
	 */
	private FormInOutNode createMatchingFormOut(PDG pdg, FormInOutNode fIn) throws PDGFormatException {
		assert (fIn.isIn());
		assert (pdg.containsNode(fIn));

		List<FormInOutNode> rootPath = fIn.getRootPath();
		FormInOutNode inRoot = rootPath.get(rootPath.size() - 1);
		ObjTreeParamModel model = (ObjTreeParamModel) pdg.getParamModel();

		FormInOutNode outRoot = getCorrespondantDifferInOut(model.getRootFormOuts(), inRoot);
		if (outRoot == null) {
			outRoot = model.makeFormInOut(inRoot, false, inRoot.getPointsTo());
			if (outRoot.isStatic()) {
				model.addStaticFormOut(outRoot);
			} else {
				model.addRootFormOut(outRoot);
			}
			pdg.addParameterChildDependency(pdg.getRoot(), outRoot);
		}


		FormInOutNode step = outRoot;
		for (int i = rootPath.size() - 2; i >= 0; i--) {
			FormInOutNode inNode = rootPath.get(i);

			assert (inNode.getField() != null);

			FormInOutNode outNode =
				step.getChildForField(inNode.getField());

			if (outNode == null) {
				OrdinalSet<InstanceKey> pts =
					pdg.getPointsToSet(step, inNode.getField());

				/*
				 * Wala may exclude some parts of the program from the points-to
				 * analysis, so the points-to set may be empty.
				 * An object field with empty points-to set does not make sense,
				 * as no data flow can later on be calculated.
				 * So we do not create a node at all and ignore it.
				 */
				if (pts.isEmpty()) {
					break;
				}

				outNode = model.makeParameterNodeChild(step, inNode.getField(), pts);
			}

			step = outNode;
		}

		assert (step != null);
		assert (step.isOut());

		return step;
	}

	/**
	 * Creates a matching actual out root node for a formal out root node.
	 * This node includes all child nodes of the formal node.
	 * @throws PDGFormatException
	 */
	private ActualInOutNode createMatchingActOutRoot(PDG caller, PDG callee,
			CallNode call,	FormInOutNode fOutRoot) throws PDGFormatException {
		assert (fOutRoot.isOut());
		assert (fOutRoot.isRoot());
		assert (callee.containsNode(fOutRoot)) : callee + " does not contain node " + fOutRoot;
		assert (callee.getId() == fOutRoot.getPdgId());
		assert (caller.getId() == call.getPdgId());

		ObjTreeParamModel callerModel = (ObjTreeParamModel) caller.getParamModel();
		ActualInOutNode actOutRoot =
			callerModel.makeActualInOut(fOutRoot, call.getInstruction(), fOutRoot.getPointsTo());
		SDG.addParameterOutDependency(callee, fOutRoot, actOutRoot);
		callerModel.addRootActOut(call, actOutRoot);
		caller.addParameterChildDependency(call, actOutRoot);
		if (fOutRoot.hasChilds()) {
			copyFormOutToActOut(callee, fOutRoot, caller, actOutRoot, unfold);
		}

		return actOutRoot;
	}

	/**
	 * Propagates object trees from the actual-in/out nodes of a call inside
	 * method m to the formal-in/out nodes of m.
	 *
	 * @param sdg
	 * @param methods
	 * @return Returns a list of methods whose formal interface has been changed.
	 * @throws PDGFormatException
	 * @throws CancelException
	 */
	private Set<PDG> propagateIntraproceduralSideEffects(SDG sdg, Set<PDG> methods, IProgressMonitor progress)
	throws PDGFormatException, CancelException {
		Log.info("Propagating intraprocedural side effects of " + methods.size() + " methods.");

		Set<PDG> changed = HashSetFactory.make();
		for (PDG caller: methods) {
			Log.info("Propagating inside " + caller + " with " + caller.getAllCalls().size() + " calls.");

			ObjTreeParamModel callerModel = (ObjTreeParamModel) caller.getParamModel();

			for (CallNode call : caller.getAllCalls()) {
				Set<ActualInOutNode> aInsRoot = callerModel.getRootActIns(call);
				Set<ActualInOutNode> aInsAll = new HashSet<ActualInOutNode>();
				addAllChildNodes(aInsRoot, aInsAll);
				for (ActualInOutNode aIn : aInsAll) {
					if (aIn.hasChilds()) {
						Set<FormInOutNode> fInMayAlias =
							callerModel.getMayAliasingFormIns(aIn.getPointsTo());
						if (copyToAliasingNodes(sdg, fInMayAlias, aIn)) {
							changed.add(caller);
						}
					}
				}

				Set<ActualInOutNode> aOutsRoot = callerModel.getRootActOuts(call);
				Set<ActualInOutNode> aOutsAll = new HashSet<ActualInOutNode>();
				addAllChildNodes(aOutsRoot, aOutsAll);

				for (ActualInOutNode aOut: aOutsAll) {
					if (aOut.hasChilds()) {
						Set<FormInOutNode> fInMayAlias =
							callerModel.getMayAliasingFormIns(aOut.getPointsTo());
						for (FormInOutNode fIn : fInMayAlias) {
							try {
								createMatchingFormOut(caller, fIn);
							} catch (PDGFormatException e) {
								// a pdg format exception is thrown when a parameter
								// node is added that violates the unfolding criterion
								// this may happen as points-to sets of form-in and
								// form-out nodes may differ (really?)
								Log.warn(e.getMessage());
							}
						}

						Set<FormInOutNode> fOutMayAlias =
							callerModel.getMayAliasingFormOuts(aOut.getPointsTo());
						if (copyToAliasingNodes(sdg, fOutMayAlias, aOut)) {
							changed.add(caller);
						}
					}
				}

				if (progress.isCanceled()) {
					throw CancelException.make("Operation aborted.");
				}
			}
		}

		return changed;
	}

	private boolean propagateInParams(PDG caller, CallNode call, PDG callee) throws PDGFormatException {
		boolean changed = false;
		ObjTreeParamModel calleeModel = (ObjTreeParamModel) callee.getParamModel();
		ObjTreeParamModel callerModel = (ObjTreeParamModel) caller.getParamModel();

		for (FormInOutNode fInRoot : calleeModel.getRootFormIns()) {
			Set<ActualInOutNode> aInRoots = callerModel.getRootActIns(call);
			ActualInOutNode aInRoot = getCorrespondantSameInOut(aInRoots, fInRoot);

			assert (aInRoot != null) : "All in-root nodes should have been created by now.";

			changed |= copyFormInToActIn(callee, fInRoot, caller, aInRoot, unfold);
		}

		return changed;
	}

	private boolean propagateOutParams(PDG caller, CallNode call, PDG callee) throws PDGFormatException {
		boolean changed = false;
		ObjTreeParamModel calleeModel = (ObjTreeParamModel) callee.getParamModel();
		ObjTreeParamModel callerModel = (ObjTreeParamModel) caller.getParamModel();

		for (FormInOutNode fOutRoot : calleeModel.getRootFormOuts()) {
			Set<ActualInOutNode> aOutRoots = callerModel.getRootActOuts(call);
			ActualInOutNode aOutRoot = getCorrespondantSameInOut(aOutRoots, fOutRoot);
			if (aOutRoot == null) {
				aOutRoot = createMatchingActOutRoot(caller, callee, call, fOutRoot);
			}

			assert (aOutRoot != null) : "All static root nodes should have been created by now.";

			changed |= copyFormOutToActOut(callee, fOutRoot, caller, aOutRoot, unfold);
		}

		return changed;
	}

	private static boolean isStartOfNewThread(PDG caller, PDG callee) {
		String callerSig = caller.getMethod().getSignature();
		String calleeSig = callee.getMethod().getSignature();

		return "java.lang.Thread.start()V".equals(callerSig)
				&& calleeSig.endsWith(".run()V");
	}

	/**
	 * Propagates the form-in/out object trees of a pdg to the actual-in/out
	 * nodes of each call site
	 * @param sdg
	 * @param methods
	 * @param progress
	 * @return
	 * @throws PDGFormatException
	 * @throws CancelException
	 */
	private Set<PDG> propagateInterproceduralSideEffects(SDG sdg, Set<PDG> methods, IProgressMonitor progress)
	throws PDGFormatException, CancelException {
		Log.info("Propagating interprocedural side effects of " + methods.size() + " methods.");

		//TODO this should propagate from the given method to the callsites of the given method -
		//NOT the callsite found >>in<< the method but the callsites referring >>to<< the method
		//THIS IS ONLY RELEVANT FOR THE OPTIMIZED VERSION - the normal object tree propagation
		//works never the less

		Set<PDG> changed = HashSetFactory.make();
		for (PDG caller: methods) {
			if (caller == null) {
				// for some methods (like native ones) no pdg exists, so we skip
				// them
				continue;
			}
			Log.info("Propagating caller " + caller + " with " + caller.getAllCalls().size() + " calls.");

			for (CallNode call : caller.getAllCalls()) {
//				for (CGNode cgTarget : call.getPossibleTargets()) {
					CGNode cgTarget = call.getTarget();
					if (cgTarget == null) {
						continue;
					}

					PDG target = sdg.getPdgForMethodSignature(cgTarget);
					if (target == null) {
						continue;
					}
					
					if (propagateInParams(caller, call, target)) {
						changed.add(caller);
					}
					if (!isStartOfNewThread(caller, target)) {
						if (propagateOutParams(caller, call, target)) {
							changed.add(caller);
						}
					}
//				}
			}

			if (progress.isCanceled()) {
				throw CancelException.make("Operation aborted.");
			}
		}

		return changed;
	}

}
