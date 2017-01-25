/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph;

import java.util.Map;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.EntryNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamModel;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.dataflow.ObjGraphModRefHeapParams;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ActInLocalNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ActNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ActOutExceptionNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ActOutLocalNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ExceptionExitNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ExitNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.FormInDummyNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.FormInHeapNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.FormInLocalNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.FormInNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.FormNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.FormOutHeapNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.FormOutLocalNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.FormOutNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ObjGraphHeapParameterFactory;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ObjGraphParameter;
import edu.kit.joana.deprecated.jsdg.wala.BytecodeLocation;

/**
 * Main class of the object graph parameter model.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ObjGraphParamModel implements IParamModel {

	private final PDG pdg;
	private final ObjGraphHeapParameterFactory heap;

	private final ObjGraphParamSet modParams;
	private final ObjGraphParamSet refParams;

	private final Map<CallNode, ObjGraphParamSet> call2mod;
	private final Map<CallNode, ObjGraphParamSet> call2ref;

	private final Map<CallNode, ActInLocalNode[]> call2params;
	private final Map<CallNode, ActOutLocalNode> call2exit;
	private final Map<CallNode, ActOutExceptionNode> call2exception;

	private FormInLocalNode params[];
	private ExitNode exit;
	private ExceptionExitNode exceptionExit;

	private Set<ActOutExceptionNode> exceptionExits;

	private final Map<PDG, Map<CallNode, Map<ActNode, FormNode>>> actual2formal;
	private final Map<PDG, Map<CallNode, Map<FormNode, ActNode>>> formal2actual;

	private final boolean isFieldSensitive;

	public ObjGraphParamModel(PDG pdg, ObjGraphHeapParameterFactory heap, boolean fieldSensitive) {
		this.pdg = pdg;
		this.heap = heap;
		this.isFieldSensitive = fieldSensitive;

		modParams = new ObjGraphParamSet();
		refParams = new ObjGraphParamSet();
		call2mod = HashMapFactory.make();
		call2ref = HashMapFactory.make();
		actual2formal = HashMapFactory.make();
		formal2actual = HashMapFactory.make();
		call2params = HashMapFactory.make();
		call2exception = HashMapFactory.make();
		call2exit = HashMapFactory.make();
		if (!pdg.isIgnoreExceptions()) {
			exceptionExits = HashSetFactory.make();
		}
	}

	public ExceptionExitNode makeExceptionalExit() {
		ExceptionExitNode excExit = null;

		excExit = new ExceptionExitNode(pdg.getId(), TypeReference.JavaLangException, null, null);

		excExit.setLabel("_exception_");
		pdg.addNode(excExit);

		setParameterException(excExit);

		modParams.add(excExit);

		return excExit;
	}

	public ExitNode makeExit() {
		ExitNode exit = null;

		EntryNode entry = pdg.getRoot();
		String label = (entry.getLabel() == null ? "?" : entry.getLabel());
		TypeReference ret = pdg.getMethod().getReturnType();
		if (ret != null && ret != TypeReference.Void) {
			exit = new ExitNode(pdg.getId(), ret.isPrimitiveType(), ret, null, null, false);
			exit.setLabel(label);
		} else {
			exit = new ExitNode(pdg.getId(), true, ret, null, null, true);
			exit.setLabel("<void> " + label);
		}

		pdg.addNode(exit);

		setParameterReturn(exit);

		modParams.add(exit);

		return exit;
	}

	protected void setParameterIn(FormInLocalNode[] fInParams) {
		params = fInParams;
	}

	protected void setParameterReturn(ExitNode fOut) {
		exit = fOut;
	}

	protected void setParameterException(ExceptionExitNode fOut) {
		exceptionExit = fOut;
	}

	protected void setParameterIn(CallNode call, ActInLocalNode[] aInParams) {
		call2params.put(call, aInParams);
	}

	protected ActInLocalNode[] getParameterIn(CallNode call) {
		return call2params.get(call);
	}

	protected void setParameterReturn(CallNode call, ActOutLocalNode aOut) {
		call2exit.put(call, aOut);
	}

	protected ActOutLocalNode getParameterReturn(CallNode call) {
		return call2exit.get(call);
	}

	protected void setParameterException(CallNode call, ActOutExceptionNode aOut) {
		exceptionExits.add(aOut);
		call2exception.put(call, aOut);
	}

	protected ActOutLocalNode getParameterException(CallNode call) {
		return call2exception.get(call);
	}

	protected void addParameterInOutRelation(CallNode call, PDG callee, FormNode fNode, ActNode aNode) {
		Map<CallNode, Map<ActNode, FormNode>> a2c = actual2formal.get(callee);
		if (a2c == null) {
			a2c = HashMapFactory.make();
			actual2formal.put(callee, a2c);
		}

		Map <ActNode, FormNode> a2f = a2c.get(call);
		if (a2f == null) {
			a2f = HashMapFactory.make();
			a2c.put(call, a2f);
		}

		a2f.put(aNode, fNode);


		Map<CallNode, Map<FormNode, ActNode>> f2c = formal2actual.get(callee);
		if (f2c == null) {
			f2c = HashMapFactory.make();
			formal2actual.put(callee, f2c);
		}

		Map<FormNode, ActNode> f2a = f2c.get(call);
		if (f2a == null) {
			f2a = HashMapFactory.make();
			f2c.put(call, f2a);
		}

		f2a.put(fNode, aNode);
	}

	protected void addMod(FormOutNode node) {
		modParams.add(node);
	}

	protected void addRef(FormInNode node) {
		refParams.add(node);
	}

	protected void addMod(CallNode call, ActNode node) {
		ObjGraphParamSet mod = call2mod.get(call);
		if (mod == null) {
			mod = new ObjGraphParamSet();
			call2mod.put(call, mod);
		}

		mod.add(node);
	}

	protected void addRef(CallNode call, ActNode node) {
		ObjGraphParamSet ref = call2ref.get(call);
		if (ref == null) {
			ref = new ObjGraphParamSet();
			call2ref.put(call, ref);
		}

		ref.add(node);
	}

	public ActNode getMatchingActualNode(CallNode call,
			PDG callee, AbstractParameterNode formalNode) {
		Map<CallNode, Map<FormNode, ActNode>> map = formal2actual.get(callee);

		if (map != null) {
			Map<FormNode, ActNode> f2a = map.get(call);

			return (f2a != null ? f2a.get(formalNode) : null);
		} else {
			return null;
		}
	}

	public FormNode getMatchingFormalNode(CallNode call,
			AbstractParameterNode actualNode, PDG callee) {
		Map<CallNode, Map<ActNode, FormNode>> map = actual2formal.get(callee);

		if (map != null) {
			Map<ActNode, FormNode> a2f = map.get(call);

			return (a2f != null ? a2f.get(actualNode) : null);
		} else {
			return null;
		}
	}

	public ObjGraphParamSet getModParams() {
		return modParams;
	}

	public ObjGraphParamSet getModParams(CallNode call) {
		ObjGraphParamSet result = call2mod.get(call);

		return (result != null ? result : ObjGraphParamSet.emptySet);
	}

	public ObjGraphModRefHeapParams getModRef(OrdinalSetMapping<AbstractPDGNode> domain) {
		return new ObjGraphModRefHeapParams(pdg, domain, isFieldSensitive);
	}

	public ObjGraphParamSet getRefParams() {
		return refParams;
	}

	public ObjGraphParamSet getRefParams(CallNode call) {
		ObjGraphParamSet result = call2ref.get(call);

		return (result != null ? result : ObjGraphParamSet.emptySet);
	}

	public ExitNode getExit() {
		return exit;
	}

	public ExceptionExitNode getExceptionExit() {
		return exceptionExit;
	}

	public Iterable<ActOutExceptionNode> getExceptionExits() {
		return exceptionExits;
	}

	public FormInLocalNode[] getParameters() {
		return params;
	}

	public FormNode makeMatchingFormNode(ObjGraphParameter wp) {
		FormNode result = null;
		boolean isRootNode = false;

		if (wp.isIn()) {
			if (wp.isOnHeap()) {
				if (wp.isStatic()) {
					result = heap.makeFormInStatic(wp.getBaseField(), pdg.getId(),
							wp.isPrimitive(), wp.getType(), wp.getPointerKeys(), wp.getPointsTo());
					isRootNode = true;
				} else {
					result = heap.makeFormInHeap(wp.getBasePointsTo(), wp.getBaseField(), pdg.getId(),
							wp.isPrimitive(), wp.getType(), wp.getPointerKeys(), wp.getPointsTo());
				}
			} else {
				result = new FormInLocalNode(pdg.getId(), wp.isPrimitive(), wp.getType(),
						wp.getPointerKeys(), wp.getPointsTo(), wp.getParameterNumber(), wp.getDisplayParameterNumber());
				isRootNode = true;
			}
		} else {
			if (wp.isOnHeap()) {
				if (wp.isStatic()) {
					result = heap.makeFormOutStatic(wp.getBaseField(), pdg.getId(),
							wp.isPrimitive(), wp.getType(), wp.getPointerKeys(), wp.getPointsTo());
					isRootNode = true;
				} else {
					result = heap.makeFormOutHeap(wp.getBasePointsTo(), wp.getBaseField(), pdg.getId(),
							wp.isPrimitive(), wp.getType(), wp.getPointerKeys(), wp.getPointsTo());
				}
			} else {
				result = new FormOutLocalNode(pdg.getId(), wp.isPrimitive(), wp.getType(),
						wp.getPointerKeys(), wp.getPointsTo(), wp.getParameterNumber(), wp.getDisplayParameterNumber());
				isRootNode = true;
			}
		}

		result.setLabel(wp.getLabel());

		pdg.addNode(result);
		if (isRootNode) {
			pdg.addParameterStructureDependency(pdg.getRoot(), result);
		}

		return result;
	}

	public ActNode makeMatchingActNode(CallNode call, ObjGraphParameter wp) {
		ActNode result = null;
		boolean isRootNode = false;

		if (wp.isIn()) {
			if (wp.isOnHeap()) {
				if (wp.isStatic()) {
					result = heap.makeActInStatic(call, wp.getBaseField(), pdg.getId(),
							wp.isPrimitive(), wp.getType(), wp.getPointerKeys(), wp.getPointsTo());
					isRootNode = true;
				} else {
					result = heap.makeActInHeap(call, wp.getBasePointsTo(), wp.getBaseField(), pdg.getId(),
							wp.isPrimitive(), wp.getType(), wp.getPointerKeys(), wp.getPointsTo());
				}
			} else {
				result = new ActInLocalNode(pdg.getId(), wp.isPrimitive(), wp.getType(),
						wp.getPointerKeys(), wp.getPointsTo(), call.getUniqueId(), wp.getParameterNumber(),
						wp.getDisplayParameterNumber());
				isRootNode = true;
			}
		} else {
			if (wp.isOnHeap()) {
				if (wp.isStatic()) {
					result = heap.makeActOutStatic(call, wp.getBaseField(), pdg.getId(),
							wp.isPrimitive(), wp.getType(), wp.getPointerKeys(), wp.getPointsTo());
					isRootNode = true;
				} else {
					result = heap.makeActOutHeap(call, wp.getBasePointsTo(), wp.getBaseField(), pdg.getId(),
							wp.isPrimitive(), wp.getType(), wp.getPointerKeys(), wp.getPointsTo());
				}
			} else if (wp.isException()) {
				ActOutExceptionNode aOutExc = new ActOutExceptionNode(pdg.getId(), wp.getType(),
						wp.getPointerKeys(), wp.getPointsTo(), call.getUniqueId());

				setParameterException(call, aOutExc);

				result = aOutExc;
				isRootNode = true;
			} else {
				result = new ActOutLocalNode(pdg.getId(), wp.isPrimitive(), wp.getType(),
						wp.getPointerKeys(), wp.getPointsTo(), call.getUniqueId(), wp.getParameterNumber(),
						wp.getDisplayParameterNumber());
				isRootNode = true;
			}
		}

		result.setLabel(wp.getLabel());

		pdg.addNode(result);
		if (isRootNode) {
			pdg.addParameterStructureDependency(call, result);
		}

		return result;
	}

	public FormInDummyNode makeFormInDummy(boolean isPrimitive, TypeReference type) {
		FormInDummyNode node = new FormInDummyNode(pdg.getId(), isPrimitive, type);
		pdg.addNode(node);
		pdg.addParameterStructureDependency(pdg.getRoot(), node);

		return node;
	}

	public FormInLocalNode makeFormInLocal(boolean isPrimitive, TypeReference type, Set<PointerKey> pKeys,
			OrdinalSet<InstanceKey> pts, final int paramNum, final int displayParamNum) {
		FormInLocalNode node =
				new FormInLocalNode(pdg.getId(), isPrimitive, type, pKeys, pts, paramNum, displayParamNum);
		pdg.addNode(node);
		pdg.addParameterStructureDependency(pdg.getRoot(), node);

		return node;
	}

	public FormInHeapNode makeFormInHeap(OrdinalSet<InstanceKey> basePts, ParameterField baseField,
			boolean isPrimitive, TypeReference type, Set<PointerKey> pKey, OrdinalSet<InstanceKey> pts) {
		FormInHeapNode node = heap.makeFormInHeap(basePts, baseField, pdg.getId(), isPrimitive, type, pKey, pts);
		pdg.addNode(node);

		return node;
	}

	public FormInHeapNode makeFormInHeap(ParameterField baseField,
			boolean isPrimitive, TypeReference type, Set<PointerKey> pKey, OrdinalSet<InstanceKey> pts) {
		FormInHeapNode node = heap.makeFormInStatic(baseField, pdg.getId(), isPrimitive, type, pKey, pts);
		pdg.addNode(node);
		pdg.addParameterStructureDependency(pdg.getRoot(), node);

		return node;
	}

	public FormOutHeapNode makeFormOutHeap(OrdinalSet<InstanceKey> basePts, ParameterField baseField,
			boolean isPrimitive, TypeReference type, Set<PointerKey> pKey, OrdinalSet<InstanceKey> pts) {
		FormOutHeapNode node = heap.makeFormOutHeap(basePts, baseField, pdg.getId(), isPrimitive, type, pKey, pts);
		pdg.addNode(node);

		return node;
	}

	public FormOutHeapNode makeFormOutHeap(ParameterField baseField,
			boolean isPrimitive, TypeReference type, Set<PointerKey> pKey, OrdinalSet<InstanceKey> pts) {
		FormOutHeapNode node = heap.makeFormOutStatic(baseField, pdg.getId(), isPrimitive, type, pKey, pts);
		pdg.addNode(node);
		pdg.addParameterStructureDependency(pdg.getRoot(), node);

		return node;
	}

	public ActInLocalNode makeActInLocal(boolean isPrimitive, TypeReference type, Set<PointerKey> pKeys,
			OrdinalSet<InstanceKey> pts, CallNode call, final int paramNum, final int displayParamNum) {
		ActInLocalNode node = new ActInLocalNode(pdg.getId(), isPrimitive, type, pKeys, pts, call.getUniqueId(),
				paramNum, displayParamNum);
		pdg.addNode(node);
		pdg.addParameterStructureDependency(call, node);

		return node;
	}

	public ActOutLocalNode makeActOutLocal(boolean isPrimitive, TypeReference type, Set<PointerKey> pKeys,
			OrdinalSet<InstanceKey> pts, CallNode call, final int paramNum, final int displayParamNum) {
		ActOutLocalNode node = new ActOutLocalNode(pdg.getId(), isPrimitive, type, pKeys, pts, call.getUniqueId(),
				paramNum, displayParamNum);
		pdg.addNode(node);
		pdg.addParameterStructureDependency(call, node);

		return node;
	}

	public ActOutExceptionNode makeActOutException(Set<PointerKey> pKeys, TypeReference type, OrdinalSet<InstanceKey> pts, CallNode call) {
		ActOutExceptionNode node = new ActOutExceptionNode(pdg.getId(), type, pKeys, pts, call.getUniqueId());
		pdg.addNode(node);
		pdg.addParameterStructureDependency(call, node);

		return node;
	}

	public AbstractParameterNode makeVoidActualOut(CallNode call, PDG target) {
		assert (target.getMethod() != null);
		assert (target.getMethod().isInit());
		assert (target.isStub());

		ActOutLocalNode aOut = makeActOutLocal(true, TypeReference.Void, null, null, call,
				BytecodeLocation.UNDEFINED_POS_IN_BYTECODE, BytecodeLocation.UNDEFINED_POS_IN_BYTECODE);
		aOut.setLabel("void stub return");
		addMod(call, aOut);
		setParameterReturn(call, aOut);
		target.addNode(aOut);
		addParameterInOutRelation(call, target, (FormOutNode) target.getExit(), aOut);
		pdg.addParameterChildDependency(call, aOut);
		SDG.addParameterOutDependency(target, target.getExit(), aOut);
		pdg.addParameterStructureDependency(call, aOut);

		return aOut;
	}

}
