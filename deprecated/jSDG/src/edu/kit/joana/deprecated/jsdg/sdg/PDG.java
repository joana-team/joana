/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg;

import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.intset.MutableSparseIntSet;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.deprecated.jsdg.exceptions.ExceptionPrunedCFGAnalysis;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamComputation;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamModel;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.pointsto.IPointerAnalysis;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.deprecated.jsdg.wala.objecttree.IKey2Origin;
import edu.kit.joana.deprecated.jsdg.wala.objecttree.IKey2Origin.PointerKeyOrigin;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class PDG extends IntermediatePDG {

	private final IParamComputation pcomp;
	private final IParamModel pmodel;
	private final boolean isStub;

	public static PDG create(IProgressMonitor monitor, CGNode method, int id, IPointerAnalysis pta, IKey2Origin k2o,
			CallGraph cg, boolean ignoreExceptions,	ExceptionPrunedCFGAnalysis<SSAInstruction, IExplodedBasicBlock> epa, IParamComputation pcomp)
	throws PDGFormatException, CancelException {
		PDG pdg = new PDG(method, id, pta, k2o, cg, ignoreExceptions, epa, pcomp, false);

		pdg.build(monitor);

		return pdg;
	}

	public static PDG createStub(IProgressMonitor monitor, CGNode method, int id, IPointerAnalysis pta,
			IKey2Origin k2o, CallGraph cg, boolean ignoreExceptions,
			ExceptionPrunedCFGAnalysis<SSAInstruction, IExplodedBasicBlock> epa, IParamComputation pcomp)
	throws PDGFormatException, CancelException {
		PDG pdg = new PDG(method, id, pta, k2o, cg, ignoreExceptions, epa, pcomp, true);

		pdg.build(monitor);

		return pdg;
	}

	private final IKey2Origin k2o;

	private PDG(CGNode method, int id, IPointerAnalysis pta, IKey2Origin k2o, CallGraph cg, boolean ignoreExceptions,
			ExceptionPrunedCFGAnalysis<SSAInstruction, IExplodedBasicBlock> epa, IParamComputation pcomp, boolean isStub) {
		super(method, id, pta, cg, ignoreExceptions, epa);
		this.k2o = k2o;
		this.pcomp = pcomp;
		this.pmodel = pcomp.getBasicModel(this);
		this.isStub = isStub;
	}

	private void build(IProgressMonitor monitor) throws PDGFormatException, CancelException {
		if (isStub || (getIR() == null && getMethod().isNative())) {
			Log.info("Nodes could not be created because no IR exists.");
			createNodesForStub();
			buildIntraproceduralInterface(monitor);
		} else {
			ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> cfg = buildCFG(monitor);
			createNodesFromIR(cfg);
			Log.info(getNumberOfNodes() + " nodes created.");
			createControlDependencies(cfg, monitor);
			Log.info(numControlDep + " control dependencies.");
			buildIntraproceduralInterface(monitor);
			createMissingDataDependencies(cfg);
			Log.info(numDataDep + " data dependencies.");
		}
	}

	public boolean isStub() {
		return isStub;
	}

	public IParamModel getParamModel() {
		return pmodel;
	}

	/**
	* Call this method to build the parameter trees for parameter objects
	* that are passed intraprocedurally.
	*
	* Precodition is that all root nodes of the method parameters have already
	* been created. And for all static field accesses a root formal-in node
	* has been created.
	 * @throws PDGFormatException
	*/
	private void buildIntraproceduralInterface(IProgressMonitor monitor) throws PDGFormatException {
		pcomp.computeModRef(this, monitor);
	}


	/**
	 * Compute the points-to set of an object field using the points-to set
	 * of the reference object. E.g. compute points-to(x.y) from points-to(x)
	 * and field name y.
	 * @param roots possible allocation sites of the reference object
	 * @param field object field
	 * @return set of allocation sites the object field may point to
	 */
	public OrdinalSet<InstanceKey> getPointsToSetForObjectField(OrdinalSet<InstanceKey> roots, ParameterField field) {
		OrdinalSet<InstanceKey> fieldAllocs = null;

		assert (field != null);
		assert (!field.isPrimitiveType());

		fieldAllocs = searchForFieldAllocs(roots, field);

		return fieldAllocs;
	}

	public static long searchFieldAllocs = 0;

	/**
	 * Search all possible allocationsites of an object field.
	 * @param roots location sites of the root object
	 * @param field the object field
	 * @return set of allocation sites (points-to set)
	 */
	private OrdinalSet<InstanceKey> searchForFieldAllocs(OrdinalSet<InstanceKey> roots, ParameterField field) {
		searchFieldAllocs++;

		MutableSparseIntSet fieldAllocs = MutableSparseIntSet.makeEmpty();

		for (InstanceKey ik : roots) {
			Iterator<? extends Object> it = pta.getHeapGraph().getSuccNodes(ik);
			while (it.hasNext()) {
				Object obj = it.next();
				if (obj instanceof PointerKey) {
					Set<PointerKeyOrigin> origins =	k2o.getOrigin((PointerKey) obj);

					assert (origins != null) : "No origin found for PointerKey " + obj;

					if (origins == null) {
						Log.warn("No origin found for " + obj.getClass().getSimpleName() + " " + obj);
						continue;
					}


					if (isFieldOrigin(origins, field)) {
						OrdinalSet<InstanceKey> pts = pta.getPointsToSet((PointerKey) obj);

						assert (pts.getMapping().equals(roots.getMapping())) : "Mappings of two points-to sets do not "
								+ "match. The merged result will be wrong!";

						if (!pts.isEmpty()) {
							fieldAllocs.addAll(pts.getBackingSet());
						} else {
							// if the points-to set is empty we assume that this
							// field was never referenced - this depends on the
							// fact that a conservative points-to analysis has
							// to be applied
							Log.debug("Empty points-to set for pointerkey: " + obj);
						}
					}
				}
			}
		}

		OrdinalSet<InstanceKey> result = new OrdinalSet<InstanceKey>(fieldAllocs, roots.getMapping());

		return result;
	}

	/**
	 * Checks if the specified field is part of the pointerkey origins
	 * @param origins set of pointerkey origins
	 * @param field field variable
	 * @return true iff field is part of the pointerkey origin set
	 */
	private boolean isFieldOrigin(Set<PointerKeyOrigin> origins, ParameterField field) {
		for (PointerKeyOrigin pko : origins) {
			switch (pko.getType()) {
			case INSTANCE_FIELD:
			case STATIC_FIELD:
			case ARRAY_FIELD:
				if (pko.getField().equals(field)) {
					return true;
				}
				break;
			default: // nothing to do here
			}
		}

		return false;
	}

	protected OrdinalSet<InstanceKey> getPointsToSet(int ssaBase, ParameterField field) {
		return getPointsToSetForObjectField(getPointsToSet(ssaBase), field);
	}

	public OrdinalSet<InstanceKey> getPointsToSet(ParameterNode<?> base, ParameterField field) {
		return getPointsToSetForObjectField(base.getPointsTo(), field);
	}

}
