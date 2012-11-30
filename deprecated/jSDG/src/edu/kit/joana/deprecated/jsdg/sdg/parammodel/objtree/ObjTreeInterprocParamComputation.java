/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree;

import static edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp.InterproceduralInterfaceComputation.copyFormInToActIn;
import static edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp.InterproceduralInterfaceComputation.copyFormOutToActOut;
import static edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp.InterproceduralInterfaceComputation.getCorrespondantSameInOut;

import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;

import edu.kit.joana.deprecated.jsdg.Messages;
import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamComputation;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamModel;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp.ObjTreeUnfoldingCriterion;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp.ObjectTreeParamComputation;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp.SimpleDataDependencyParamComputation;
import edu.kit.joana.deprecated.jsdg.util.Log;

/**
 * Interprocedural part of the object tree propagation.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ObjTreeInterprocParamComputation extends ObjTreeIntraprocParamComputation implements IParamComputation {

	public ObjTreeInterprocParamComputation(ObjTreeUnfoldingCriterion unfold) {
		super(unfold);
	}

	/**
	 * Connects the actual-in root nodes of a call to the formal-in of the
	 * called pdg. Does the same with formal-out -> actual-out nodes using
	 * parameter-in/out edges.
	 * @throws PDGFormatException
	 */
	public void connectCallParamNodes(PDG caller, CallNode callNode, PDG callee) throws PDGFormatException {
		IParamModel cModel = caller.getParamModel();
		if (!(cModel instanceof ObjTreeParamModel)) {
			throw new IllegalArgumentException(caller + " does not have an object tree param model.");
		}
		ObjTreeParamModel callerModel = (ObjTreeParamModel) cModel;

		IParamModel ceModel = callee.getParamModel();
		if (!(cModel instanceof ObjTreeParamModel)) {
			throw new IllegalArgumentException(callee + " does not have an object tree param model.");
		}
		ObjTreeParamModel calleeModel = (ObjTreeParamModel) ceModel;

		for (ActualInOutNode actIn : callerModel.getRootActIns(callNode)) {
			FormInOutNode fIn;
			if (actIn.getParamId() != null) {
				try {
					fIn = calleeModel.getFormalInParameter(actIn.getParamId());
				} catch (NoSuchElementException e) {
					Log.warn("No formal-in parameter no. " + actIn.getParamId() + " for " + callee);
					Log.warn(e);
					continue;
				}
			} else if (actIn.isStatic()) {
				assert (actIn.getField() != null);

				fIn = calleeModel.getStaticFormIn(actIn.getField());

				assert (fIn != null) : "No formal-in parameter for static field " + actIn.getField() + " in " + callee;
			} else {
				throw new PDGFormatException("Found an actual-in root node that is not " +
					"a parameter or a static field: " + actIn + " in " + caller);
			}

			SDG.addParameterInDependency(caller, actIn, fIn);
		}

		for (ActualInOutNode actOut : callerModel.getRootActOuts(callNode)) {
			FormInOutNode fOut;
			if (actOut.getParamId() != null) {
				fOut = calleeModel.getFormalOutParameter(actOut.getParamId());

				assert (fOut != null) : "No formal-out parameter no. " + actOut.getParamId() + " for " + callee;
			} else if (actOut.isStatic()) {
				assert (actOut.getField() != null);

				fOut = calleeModel.getStaticFormIn(actOut.getField());

				assert (fOut != null) : "No formal-out parameter for static field " + actOut.getField() + " in " + callee;
			} else if (actOut.getType() == ParameterNode.Type.EXCEPTION) {
				fOut = (FormInOutNode) callee.getExceptionalExit();
				if (fOut == null) {
					if (callee.getMethod().isNative()) {
						Log.debug("Ignoring non-existent exception of native method.");
					} else {
						Log.info("No formal-out parameter for exceptional exit in " + callee);
					}
					continue;
				}
			} else if (actOut.getType() == ParameterNode.Type.RETURN) {
				fOut = (FormInOutNode) callee.getExit();
				if (fOut == null) {
					if (callee.getMethod().isNative()) {
						Log.debug("Ignoring non-existent exit node of native method.");
					} else {
						Log.warn("No formal-out parameter for method exit in " + callee);
					}
					continue;
				}
			} else {
				throw new PDGFormatException("Found an actual-out root node that is not " +
					"a parameter or a static field: " + actOut + " in " + caller);
			}

			SDG.addParameterOutDependency(callee, fOut, actOut);
		}
	}


	public void computeTransitiveModRef(SDG sdg, IProgressMonitor progress) throws PDGFormatException, CancelException, WalaException {
		progress.subTask(Messages.getString("SDG.SubTask_Act_Nodes")); //$NON-NLS-1$
		buildIntraproceduralActualNodes(sdg, progress);
		if (progress.isCanceled()) {
			throw CancelException.make("Operation aborted.");
		}

		progress.subTask(Messages.getString("SDG.SubTask_Object-Trees")); //$NON-NLS-1$
		propagateObjectTrees(sdg, progress);
		if (progress.isCanceled()) {
			throw CancelException.make("Operation aborted.");
		}

		progress.subTask(Messages.getString("SDG.SubTask_Connect_Statics")); //$NON-NLS-1$
		connectStatics(sdg, progress);
		if (progress.isCanceled()) {
			throw CancelException.make("Operation aborted.");
		}
	}

	/**
	 * Computes data dependences of static initializers
	 */
	private void connectStatics(SDG sdg, IProgressMonitor progress) {
		// connect form-in of statics
		Set<PDG> initPdgs = new HashSet<PDG>(sdg.getStaticInitializers());

		PDG mainPdg = sdg.getMainPDG();

		assert (mainPdg != null);

		initPdgs.add(mainPdg);

		for (PDG pdg : initPdgs) {
			IParamModel pModel = pdg.getParamModel();
			if (!(pModel instanceof ObjTreeParamModel)) {
				throw new IllegalStateException(
					"Static initializer as no obj tree parameter model: " +
					pModel.getClass().getCanonicalName());
			}
			ObjTreeParamModel objModel = (ObjTreeParamModel) pModel;

			connectStaticFormIns(pdg, objModel, sdg.getStaticInitializers());
			progress.worked(1);
		}

		progress.done();
	}

	/**
	 * Connects all static fields used by this method to its static initializer
	 * We are doing this because the static initializers are not called directly
	 * by the main method. But never the less their execution is triggered
	 * before main and influences the data accessed by the main method. So
	 * each heap location that is read by main, is connected to all possibly
	 * matching out nodes of all static initializers.
	 * @param clinits set of all static initializer pdgs
	 */
	private void connectStaticFormIns(PDG pdg, ObjTreeParamModel model, Set<PDG> clinits) {

		for (FormInOutNode fIn : model.getStaticFormIns()) {
			assert (fIn.getField() != null);
			assert (fIn.getField().isStatic());

			for (PDG pdgInit : clinits) {
				if (pdgInit != pdg) {
					IParamModel pModel = pdgInit.getParamModel();
					if (!(pModel instanceof ObjTreeParamModel)) {
						throw new IllegalStateException(
							"Static initializer as no obj tree parameter model: " +
							pModel.getClass().getCanonicalName());
					}
					ObjTreeParamModel objModel = (ObjTreeParamModel) pModel;

					connectStaticInitializerOutsToFormIns(pdgInit, objModel, fIn, pdg);
				}
			}
		}
	}

	/**
	 * Connect the matching form-out of the static initializer pdg to the
	 * form-in fIn of a static field used in this method.
	 * As this is done with all static initializers the brute force way it is
	 * not guaranteed (and not unusual) that a matching form-out is not found
	 * in the initializer. When no matching node is found nothing will happen.
	 * @param pdg pdg of a static initializer != this
	 * @param fIn formal-in node of a static field belonging to this pdg
	 */
	private void connectStaticInitializerOutsToFormIns(PDG pdg, ObjTreeParamModel model,
			FormInOutNode fIn, PDG toPdg) {
		// we may connect formal-outs to formal-ins as pdg is a static initializer
		// which is simply called once without a callsite - so there are no
		// existing actual nodes for static initializers - they are identical
		// with the formal nodes
		assert (pdg.getMethod().isClinit());
		assert (fIn.isStatic());
		assert (fIn.isRoot());

		connectTo(pdg, model.getRootFormOuts(), fIn, toPdg);
	}

	/**
	 * Looks in a set of nodes for nodes whose field matches the field of fIn
	 * and connects those matching nodes with an parameter in dependency.
	 * This is only used to connect static fields initialized by static
	 * initializers
	 * @param nodes set of parameter nodes
	 * @param fIn formal-in node to search the matching nodes for
	 */
	private void connectTo(PDG pdgFrom, Set<FormInOutNode> nodes, FormInOutNode fIn, PDG pdgTo) {
		for (FormInOutNode from : nodes) {
			if (from.getField() != null && from.getField().equals(fIn.getField())) {
				if (!pdgTo.containsNode(from)) {
					pdgTo.addNode(from);
				}

				SDG.addParameterInDependency(pdgFrom, from, fIn);
			}
		}
	}


	/**
	 * Propagates the previously computed object trees along the methods of the
	 * call graph.
	 * @throws PDGFormatException
	 * @throws CancelException
	 * @throws WalaException
	 */
	private void propagateObjectTrees(SDG sdg, IProgressMonitor progress) throws PDGFormatException, CancelException, WalaException {
		Log.info("About to propagate object-trees");

		if (sdg.isSimpleDataDependency()) {
			SimpleDataDependencyParamComputation.compute(sdg, progress);
		} else {
			ObjectTreeParamComputation.compute(sdg, unfold, progress);
		}

		Log.info("Propagation of object-trees done.");
	}



	private void buildIntraproceduralActualNodes(SDG sdg, IProgressMonitor progress) throws PDGFormatException, CancelException {
		Collection<PDG> pdgs = sdg.getAllContainedPDGs();
		Log.info("Intraproc propagation for " + pdgs.size() + " PDGs");
		for (PDG pdg : pdgs) {
			Log.info("Processing intraproc nodes in " + pdg.getAllCalls().size() + " calls from " + pdg);

			IParamModel model = pdg.getParamModel();
			if (!(model instanceof ObjTreeParamModel)) {
				throw new IllegalStateException("expected obj-tree param model: " + model.getClass().getCanonicalName());
			}
			ObjTreeParamModel objModel = (ObjTreeParamModel) model;

			for (CallNode call : pdg.getAllCalls()) {
//				for (CGNode cgTarget : call.getPossibleTargets()) {
					CGNode cgTarget = call.getTarget();
					if (cgTarget == null) {
						continue;
					}

					PDG target = sdg.getPdgForMethodSignature(cgTarget);
					IParamModel tModel = target.getParamModel();
					if (!(tModel instanceof ObjTreeParamModel)) {
						throw new IllegalStateException("expected obj-tree param model: " + tModel.getClass().getCanonicalName());
					}
					ObjTreeParamModel tObjModel = (ObjTreeParamModel) tModel;
					buildIntraprocActualNodes(pdg, objModel, call, target, tObjModel);
//				}
				if (progress.isCanceled()) {
					throw CancelException.make("Operation aborted.");
				}
			}
			progress.worked(1);
		}

		Log.info("Intraproc propagation done.");
		progress.done();
	}

	/**
	 * Copies the formal-in and -out trees of a called method to the actual-in
	 * and -out nodes of a callsite.
	 * This is called when all intraprocedural formal-nodes for all pdgs have been
	 * computed.
	 * @param call
	 * @param callee
	 * @throws PDGFormatException
	 */
	public void buildIntraprocActualNodes(PDG pdg, ObjTreeParamModel pdgModel,
			CallNode call, PDG callee, ObjTreeParamModel calleeModel) throws PDGFormatException {
		for (FormInOutNode fIn : calleeModel.getRootFormIns()) {
			ActualInOutNode aIn = getCorrespondantSameInOut(pdgModel.getRootActIns(call), fIn);
			if (aIn == null) {
				if (fIn.isPrimitive()) {
					aIn = pdgModel.makeActualInOut(fIn, call.getInstruction());
				} else {
					aIn = pdgModel.makeActualInOut(fIn, call.getInstruction(), fIn.getPointsTo());
				}
				SDG.addParameterInDependency(pdg, aIn, fIn);
				pdgModel.addRootActIn(call, aIn);
				pdg.addExpressionControlDependency(call, aIn);
			}
			copyFormInToActIn(callee, fIn, pdg, aIn, unfold);
		}

		for (FormInOutNode fOut : calleeModel.getRootFormOuts()) {
			ActualInOutNode aOut = getCorrespondantSameInOut(pdgModel.getRootActOuts(call), fOut);
			if (aOut == null) {
				if (fOut.isPrimitive()) {
					aOut = pdgModel.makeActualInOut(fOut, call.getInstruction());
				} else {
					aOut = pdgModel.makeActualInOut(fOut, call.getInstruction(), fOut.getPointsTo());
				}
				SDG.addParameterOutDependency(callee, fOut, aOut);
				pdgModel.addRootActOut(call, aOut);
				pdg.addExpressionControlDependency(call, aOut);
			}
			copyFormOutToActOut(callee, fOut, pdg, aOut, unfold);
		}
	}


}
