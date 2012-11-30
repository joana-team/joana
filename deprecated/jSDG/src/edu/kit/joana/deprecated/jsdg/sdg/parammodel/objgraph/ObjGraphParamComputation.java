/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.deprecated.jsdg.Messages;
import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG.Call;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.EdgeType;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamModel;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamSet;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParameter;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.interfacecomp.GenReach;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ActOutExceptionNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.FormInNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.FormOutNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ObjGraphParameter;
import edu.kit.joana.deprecated.jsdg.util.Log;

/**
 * Interprocedural part of the object graph parameter computation.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ObjGraphParamComputation extends ObjGraphIntraProcParamComputation {

	private final boolean pruneNonEscapingNodes;

	private SDG sdg;

	public ObjGraphParamComputation(boolean fieldSensitive, boolean pruneNonEscapingNodes) {
		super(fieldSensitive);
		this.pruneNonEscapingNodes = pruneNonEscapingNodes;
	}

	public void computeTransitiveModRef(SDG sdg, IProgressMonitor progress)
			throws PDGFormatException, CancelException, WalaException {
		this.sdg = sdg;
		progress.subTask(Messages.getString("SDG.SubTask_Transitive_Interface")); //$NON-NLS-1$
		Log.info("Computing transitive Mod/Ref");
		CallGraph cg = sdg.getCallGraph();

		Log.info("Inverting call graph");
		Graph<CGNode> cgInverted = GraphInverter.invert(cg);
		Log.info("Building intraproc Mod/Ref sets");
		Map<CGNode, Collection<IParameter>> cgnode2heap = buildCGNode2HeapMap(sdg);
		GenReach<CGNode, IParameter> gr = new GenReach<CGNode, IParameter>(cgInverted, cgnode2heap);
		progress.worked(1);

		if (progress.isCanceled()) {
			throw CancelException.make("Operation aborted.");
		}

		Log.info("Propagating Mod/Refs interprocedural.");
		BitVectorSolver<CGNode> solver = new BitVectorSolver<CGNode>(gr);
		solver.solve(progress);
		Map<CGNode, OrdinalSet<IParameter>> result = HashMapFactory.make();
		for (CGNode cgNode : cg) {
			BitVectorVariable bv = solver.getOut(cgNode);
			result.put(cgNode, new OrdinalSet<IParameter>(bv.getValue(), gr.getLatticeValues()));

			if (progress.isCanceled()) {
				throw CancelException.make("Operation aborted.");
			}
		}

		progress.worked(1);

		progress.done();

		Log.info("Copying result back to pdgs.");

		copyResultBackToPdgs(sdg, result, progress);

		if (!sdg.isIgnoreExceptions()) {
			// quick fix to add data dependencies from throws to exception exit
			for (PDG pdg : sdg.getAllContainedPDGs()) {
				ObjGraphParamModel model = (ObjGraphParamModel) pdg.getParamModel();

				AbstractParameterNode excOut = pdg.getExceptionalExit();

				for (ActOutExceptionNode aOut : model.getExceptionExits()) {
					final AbstractPDGNode catcher = aOut.getCatcher();
					if (catcher != null) {
						pdg.addDataDependency(aOut, catcher);
					} else if (pdg.getSuccNodeCount(aOut) == 0) {
						pdg.addDataDependency(aOut, excOut);
					}
				}
			}
		}

		progress.done();

		Log.info("Connection form-out of statics to form-in of main method.");

		progress.subTask(Messages.getString("SDG.SubTask_Connect_Statics")); //$NON-NLS-1$
		connectStatics(sdg, progress);

		progress.subTask(Messages.getString("SDG.SubTask_Recreate_Interface")); //$NON-NLS-1$
		recreateInterfaceStructure(sdg, progress);

		Log.info("WalaParamComputation done.");

		progress.done();

		this.sdg = null;
	}

	/**
	 * Recreates the parameter field structure of the computed parameter nodes. A node that
	 * represents an object that may be accessed through the field of another parameter node
	 * are connected with an parameter structure edge (PS).
	 * This is don for each formal-node interface as well as for each call site with
	 * actual-nodes.
	 * @param sdg The dependency graph.
	 * @param progress A progress monitor used to display the progress of the computation.
	 */
	private static void recreateInterfaceStructure(final SDG sdg, final IProgressMonitor progress) {

		for (final PDG pdg : sdg.getAllContainedPDGs()) {
			final ObjGraphParamModel model = (ObjGraphParamModel) pdg.getParamModel();

			// fix formal-out nodes
			annotateParameterOutStructure(pdg, model.getModParams(), model.getRefParams());
			if (CHECK_STRUCTURE) { checkParameterStructure(pdg, model.getModParams());}
			// fix formal-in nodes
			annotateParameterStructure(pdg, model.getRefParams());
			if (CHECK_STRUCTURE) { checkParameterStructure(pdg, model.getRefParams()); }

			for (CallNode call : pdg.getAllCalls()) {
				// fix actual-in nodes of calls
				final ObjGraphParamSet callRefs = model.getRefParams(call);
				annotateParameterStructure(pdg, callRefs);
				if (CHECK_STRUCTURE) { checkParameterStructure(pdg, callRefs); }
				// fix actual-out nodes of calls
				final ObjGraphParamSet callMods = model.getModParams(call);
				annotateParameterOutStructure(pdg, callMods, callRefs);
				if (CHECK_STRUCTURE) { checkParameterStructure(pdg, callMods); }
			}
		}
	}

	private static final void checkParameterStructure(final PDG pdg, final ObjGraphParamSet params) {
		Set<ObjGraphParameter> all = new HashSet<ObjGraphParameter>();
		Set<ObjGraphParameter> done = new HashSet<ObjGraphParameter>();
		LinkedList<ObjGraphParameter> work = new LinkedList<ObjGraphParameter>();
		for (ObjGraphParameter param : params) {
			all.add(param);
			if (param.isRoot()) {
				work.add(param);
			}
		}

		final int totalNum = all.size();

		while (!work.isEmpty()) {
			final ObjGraphParameter param = work.removeFirst();
			all.remove(param);
			done.add(param);

			for (Iterator<? extends AbstractPDGNode> it = pdg.getSuccNodes(param, EdgeType.PS); it.hasNext();) {
				final ObjGraphParameter child = (ObjGraphParameter) it.next();
				if (!done.contains(child)) {
					work.add(child);
				}
			}
		}

		final int reached = done.size();
		final int notReached = all.size();
		final int percentReached = (totalNum == 0 ? 100 : (((totalNum - notReached) * 100) / totalNum));
		System.err.println(percentReached + "% reached: " + totalNum + " params total. "
				+ reached + " params reached. " + notReached + " params not reached.");
		for (ObjGraphParameter unreached : all) {
			System.err.println(pdg + ": No path to parameter " + unreached);
		}
	}

	public static final boolean CHECK_STRUCTURE = false;

	private static final void annotateParameterStructure(final PDG pdg, final ObjGraphParamSet params) {
		final IClassHierarchy cha = pdg.getHierarchy();

		for (ObjGraphParameter param : params) {
			for (ObjGraphParameter child : params) {
				if (param != child && child.isOnHeap() && !child.isStatic()) {
					// only nodes representing values on the heap and non-static fields may be
					// reached through an instance field.
					if (param.mayBeParentOf(cha, child)) {
						pdg.addParameterStructureDependency(param, child);
					}
				}
			}
		}
	}

	private static final void annotateParameterOutStructure(final PDG pdg, final ObjGraphParamSet out, final ObjGraphParamSet in) {
		final IClassHierarchy cha = pdg.getHierarchy();

		for (ObjGraphParameter param : out) {
			for (ObjGraphParameter child : out) {
				if (param != child && child.isOnHeap() && !child.isStatic()) {
					// only nodes representing values on the heap and non-static fields may be
					// reached through an instance field.
					if (param.mayBeParentOf(cha, child)) {
						pdg.addParameterStructureDependency(param, child);
					}
				}
			}
		}

		for (ObjGraphParameter param : in) {
			for (ObjGraphParameter child : out) {
				if (param != child && child.isOnHeap() && !child.isStatic()) {
					// only nodes representing values on the heap and non-static fields may be
					// reached through an instance field.
					if (param.mayBeParentOf(cha, child)) {
						pdg.addParameterStructureDependency(param, child);
					}
				}
			}
		}
	}

	private final void copyResultBackToPdgs(SDG sdg, Map<CGNode,
			OrdinalSet<IParameter>> result, IProgressMonitor progress) throws CancelException {
		progress.subTask("Adding Interprocedural Interface Nodes to PDGs");

		// adjust formal nodes
		for (PDG pdg : sdg.getAllContainedPDGs()) {
			CGNode node = pdg.getCallGraphNode();

			if (pruneNonEscapingNodes) {
				OrdinalSet<IParameter> heapParams = result.get(node);
				ObjGraphParamModel pmodel = (ObjGraphParamModel) pdg.getParamModel();
				ObjGraphParamSet escapesIn = getEscapeInGateways(heapParams, pmodel);
				ObjGraphParamSet escapesOut = getEscapeOutGateways(heapParams, pmodel);
				escapesOut.merge(escapesIn);
				ObjGraphParamSet othersIn = getNonEscapeInGateways(heapParams, pmodel);
				othersIn.merge(escapesIn);
				ObjGraphParamSet othersOut = getNonEscapeOutGateways(heapParams, pmodel);
				othersOut.merge(othersIn);
				othersOut.merge(escapesOut);

				for (IParameter param : heapParams) {
					ObjGraphParameter wp = (ObjGraphParameter) param;

					if (hasPathToEscape(wp, escapesIn, escapesOut, othersIn, othersOut)) {
						if (wp.getPdgId() != pdg.getId()) {
							addToInterfaceAsNeeded(pdg, wp);
						}
					} else {
						if (isAlreadyInInterface(wp, pdg)) {
							removeFromInterface(wp, pdg);
						}
					}
				}
			} else {
				OrdinalSet<IParameter> heapParams = result.get(node);
				for (IParameter param : heapParams) {
					ObjGraphParameter wp = (ObjGraphParameter) param;

					if (wp.getPdgId() != pdg.getId()) {
						addToInterfaceAsNeeded(pdg, wp);
					}
				}
			}

			if (progress.isCanceled()) {
				throw CancelException.make("Operation aborted.");
			}

			progress.worked(1);
		}

		// adjust actual nodes
		for (Call call : sdg.getAllCalls()) {
			if (call.node.getTarget() == null) {
				continue;
			}
			//TODO already created act-nodes may belong to form nodes that have been removed
			ObjGraphIntraProcParamComputation.connectHeapParamNodes(call.caller, call.node, call.callee);

			if (progress.isCanceled()) {
				throw CancelException.make("Operation aborted.");
			}
		}

		progress.worked(1);
	}

	private static final ObjGraphParamSet getEscapeInGateways(OrdinalSet<IParameter> heapParams,
			ObjGraphParamModel pmodel) {
		ObjGraphParamSet escapesIn = new ObjGraphParamSet();

		// add propagated static fields
		for (IParameter ip : heapParams) {
			ObjGraphParameter wp = (ObjGraphParameter) ip;
			if (wp.isIn() && wp.isStatic()) {
				escapesIn.add(wp);
			}
		}

		// add local static fields and local parameters
		for (ObjGraphParameter wp : pmodel.getRefParams()) {
			if (wp.isStatic() || !wp.isOnHeap()) {
				escapesIn.add(wp);
			}
		}

		return escapesIn;
	}

	private static final ObjGraphParamSet getEscapeOutGateways(OrdinalSet<IParameter> heapParams,
			ObjGraphParamModel pmodel) {
		ObjGraphParamSet escapesOut = new ObjGraphParamSet();

		// add propagated static fields
		for (IParameter ip : heapParams) {
			ObjGraphParameter wp = (ObjGraphParameter) ip;
			if (wp.isOut() && wp.isStatic()) {
				escapesOut.add(wp);
			}
		}

		// add local static fields and local parameters
		for (ObjGraphParameter wp : pmodel.getModParams()) {
			if (wp.isStatic() || !wp.isOnHeap()) {
				escapesOut.add(wp);
			}
		}

		return escapesOut;
	}

	private static final ObjGraphParamSet getNonEscapeOutGateways(OrdinalSet<IParameter> heapParams,
			ObjGraphParamModel pmodel) {
		ObjGraphParamSet othersOut = new ObjGraphParamSet();

		// add propagated heap fields
		for (IParameter ip : heapParams) {
			ObjGraphParameter wp = (ObjGraphParameter) ip;
			if (wp.isOut() && !wp.isStatic()) {
				othersOut.add(wp);
			}
		}

		// add local heap fields
		for (ObjGraphParameter wp : pmodel.getModParams()) {
			if (!wp.isStatic() && wp.isOnHeap()) {
				othersOut.add(wp);
			}
		}

		return othersOut;
	}

	private static final ObjGraphParamSet getNonEscapeInGateways(OrdinalSet<IParameter> heapParams,
			ObjGraphParamModel pmodel) {
		ObjGraphParamSet othersIn = new ObjGraphParamSet();

		// add propagated heap fields
		for (IParameter ip : heapParams) {
			ObjGraphParameter wp = (ObjGraphParameter) ip;
			if (wp.isIn() && !wp.isStatic()) {
				othersIn.add(wp);
			}
		}

		// add local heap fields
		for (ObjGraphParameter wp : pmodel.getRefParams()) {
			if (!wp.isStatic() && wp.isOnHeap()) {
				othersIn.add(wp);
			}
		}

		return othersIn;
	}

	private void removeFromInterface(ObjGraphParameter wp, PDG pdg) {
		if (wp.getPdgId() != pdg.getId()) {
			throw new IllegalArgumentException("parameter has to belong to the pdg");
		}

		ObjGraphParamModel pmodel = (ObjGraphParamModel) pdg.getParamModel();
		if (wp.isIn()) {
			pmodel.getRefParams().remove(wp);
		} else {
			pmodel.getModParams().remove(wp);
		}
		if (pdg.containsNode(wp)) {
			pdg.removeNodeAndEdges(wp);
		}
		if (wp.getPdgId() == pdg.getId() && sdg.containsNode(wp)) {
			sdg.removeNodeAndEdges(wp);
		}
		/*
		 * Formal-in nodes are also part of the pdgs calling this pdg. So we have
		 * to remove them from the caller pdgs too.
		 *
		 * Actual-out nodes are also part of the called pdg..
		 */
		if (wp.isFormal() && wp.isIn()) {
			CGNode cgNode = pdg.getCallGraphNode();
			CallGraph cg = pdg.getCallGraph();
			for (Iterator<? extends CGNode> it = cg.getPredNodes(cgNode); it.hasNext(); ) {
				CGNode cgCaller = it.next();
				PDG pdgCaller = sdg.getPdgForMethodSignature(cgCaller);
				if (pdgCaller != null) {
					if (pdgCaller.containsNode(wp)) {
						pdgCaller.removeNodeAndEdges(wp);
					}
				}
			}
		} else if (wp.isActual() && wp.isOut()) {
			CGNode cgNode = pdg.getCallGraphNode();
			CallGraph cg = pdg.getCallGraph();
			for (Iterator<? extends CGNode> it = cg.getSuccNodes(cgNode); it.hasNext(); ) {
				CGNode cgCallee = it.next();
				PDG pdgCaller = sdg.getPdgForMethodSignature(cgCallee);
				if (pdgCaller != null) {
					if (pdgCaller.containsNode(wp)) {
						pdgCaller.removeNodeAndEdges(wp);
					}
				}
			}
		}
	}

	private boolean isAlreadyInInterface(ObjGraphParameter wp, PDG pdg) {
		if (wp.isIn()) {
			return pdg.getParamModel().getRefParams().contains(wp);
		} else {
			return pdg.getParamModel().getModParams().contains(wp);
		}
	}

	private final boolean hasPathToEscape(ObjGraphParameter wp, ObjGraphParamSet escapesIn,
			ObjGraphParamSet escapesOut, ObjGraphParamSet othersIn, ObjGraphParamSet othersOut) {
		if (pruneNonEscapingNodes) {
			if (wp.isStatic()) {
				return true;
			}

			if (wp.isIn()) {
				// get escape in gateways and check if wp has path to one node
				// of those gateways
				// wp has roots in escapesIn
				return hasPathToEscape(wp, escapesIn, othersIn);
			} else {
				// get escape out gateways and check if wp has path to one node
				// of the set
				// wp has roots in escapesOut
				return hasPathToEscape(wp, escapesOut, othersOut);
			}
		} else {
			return true;
		}
	}

	private final static boolean mayBePred(ObjGraphParameter pred, ObjGraphParameter wp) {
		if (pred.getPointsTo() != null) {
			boolean mayAlias = pred.getPointsTo().containsAny(wp.getBasePointsTo());
//			if (pred.getBaseField() != null) {
				// TODO check if types match -> they should
			// pred.getType has field wp.getBaseField
//			}

			return mayAlias;
		} else {
			return false;
		}
	}

	private final static ObjGraphParamSet getPossiblePreds(ObjGraphParameter wp, ObjGraphParamSet others) {
		ObjGraphParamSet result = new ObjGraphParamSet();

		for (ObjGraphParameter pred : others) {
			if (mayBePred(pred, wp)) {
				result.add(pred);
			}
		}

		return result;
	}

	private static final boolean hasPathToEscape(ObjGraphParameter wp,
			ObjGraphParamSet escapes, ObjGraphParamSet others, ObjGraphParamSet visited) {
		if (escapes.contains(wp)) {
			return true;
		} else {
			ObjGraphParamSet preds = getPossiblePreds(wp, others);
			preds.removeAll(visited);
			if (escapes.containsAny(preds)) {
				escapes.add(wp);
				return true;
			} else {
				visited.merge(preds);
				for (ObjGraphParameter pred : preds) {
					if (hasPathToEscape(pred, escapes, others, visited)) {
						escapes.add(wp);
						return true;
					}
				}

				return false;
			}
		}
	}

	private static final boolean hasPathToEscape(ObjGraphParameter wp,
			ObjGraphParamSet escapes, ObjGraphParamSet others) {
		if (escapes.contains(wp)) {
			return true;
		} else {
			ObjGraphParamSet visited = new ObjGraphParamSet();
			visited.add(wp);

			return hasPathToEscape(wp, escapes, others, visited);
		}
	}

	private final void addToInterfaceAsNeeded(PDG pdg, ObjGraphParameter wp) {
		ObjGraphParamModel model = (ObjGraphParamModel) pdg.getParamModel();
		if (wp.isIn()) {
			IParamSet<ObjGraphParameter> ref = model.getRefParams();
			if (!ref.contains(wp)) {
				FormInNode fIn = (FormInNode) model.makeMatchingFormNode(wp);
				pdg.addParameterChildDependency(pdg.getRoot(), fIn);
				model.addRef(fIn);
			}
		} else {
			IParamSet<ObjGraphParameter> mod = model.getModParams();
			if (!mod.contains(wp)) {
				FormOutNode fOut = (FormOutNode) model.makeMatchingFormNode(wp);
				pdg.addParameterChildDependency(pdg.getRoot(), fOut);
				model.addMod(fOut);
			}
		}

		if (!pdg.containsNode(wp)) {
			pdg.addNode(wp);
		}
	}

	private static final Map<CGNode, Collection<IParameter>> buildCGNode2HeapMap(SDG sdg) {
		Map<CGNode, Collection<IParameter>> result = HashMapFactory.make();

		for (PDG pdg : sdg.getAllContainedPDGs()) {
			CGNode node = pdg.getCallGraphNode();
			Collection<IParameter> heapParams = HashSetFactory.make();

			IParamSet<?> refs = pdg.getParamModel().getRefParams();
			addHeapParamsToSet(refs, heapParams);

			IParamSet<?> mods = pdg.getParamModel().getModParams();
			addHeapParamsToSet(mods, heapParams);

//			for (CGNode node : cgNodes) {
				result.put(node, heapParams);
//			}
		}

		return result;
	}

	private static final void addHeapParamsToSet(IParamSet<?> params, Collection<IParameter> set) {
		for (AbstractParameterNode node : params) {
			if (node.isOnHeap() && !node.isExit() && !node.isException()) {
				set.add(node);
			}
		}
	}

	/**
	 * Computes data dependences of static initializers
	 * @throws CancelException
	 */
	private void connectStatics(SDG sdg, IProgressMonitor progress) throws CancelException {
		// connect form-in of statics
		Set<PDG> initPdgs = new HashSet<PDG>(sdg.getStaticInitializers());

		PDG mainPdg = sdg.getMainPDG();

		assert (mainPdg != null);

		initPdgs.add(mainPdg);

		for (PDG pdg : initPdgs) {
			IParamModel pModel = pdg.getParamModel();
			if (!(pModel instanceof ObjGraphParamModel)) {
				throw new IllegalStateException(
					"Static initializer as no wala tree parameter model: " +
					pModel.getClass().getCanonicalName());
			}
			ObjGraphParamModel objModel = (ObjGraphParamModel) pModel;

			connectStaticFormIns(pdg, objModel, sdg.getStaticInitializers());
			progress.worked(1);

			if (progress.isCanceled()) {
				throw CancelException.make("Operation aborted.");
			}
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
	private void connectStaticFormIns(PDG pdg, ObjGraphParamModel model, Set<PDG> clinits) {
		for (ObjGraphParameter fIn : model.getRefParams()) {
			if (!fIn.isOnHeap()) {
				continue;
			}

			assert (fIn.isIn());
			assert (fIn.isFormal());

			for (PDG pdgInit : clinits) {
				if (pdgInit != pdg) {
					IParamModel pModel = pdgInit.getParamModel();
					if (!(pModel instanceof ObjGraphParamModel)) {
						throw new IllegalStateException(
							"Static initializer as no wala tree parameter model: " +
							pModel.getClass().getCanonicalName());
					}
					ObjGraphParamModel objModel = (ObjGraphParamModel) pModel;

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
	 *
	 * Looks in a set of nodes for nodes whose field matches the field of fIn
	 * and connects those matching nodes with an parameter in dependency.
	 * This is only used to connect static fields initialized by static
	 * initializers
	 *
	 * @param pdg pdg of a static initializer != this
	 * @param fIn formal-in node of a static field belonging to this pdg
	 */
	private void connectStaticInitializerOutsToFormIns(PDG pdgFrom, ObjGraphParamModel model,
			ObjGraphParameter fIn, PDG pdgTo) {
		// we may connect formal-outs to formal-ins as pdg is a static initializer
		// which is simply called once without a callsite - so there are no
		// existing actual nodes for static initializers - they are identical
		// with the formal nodes
		assert (pdgFrom.getMethod().isClinit());

		for (ObjGraphParameter from : model.getModParams()) {
			if (from.isOnHeap() && from.getBaseField() == fIn.getBaseField() && from.isMayAliasing(fIn)) {
				if (!pdgTo.containsNode(from)) {
					pdgTo.addNode(from);
				}

				SDG.addParameterInDependency(pdgFrom, from, fIn);
			}
		}
	}

}
