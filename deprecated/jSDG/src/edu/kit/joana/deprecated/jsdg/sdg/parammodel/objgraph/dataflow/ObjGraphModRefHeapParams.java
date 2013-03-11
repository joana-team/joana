/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.dataflow;

import java.util.Map;


import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CatchNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ConstantPhiValueNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.EntryNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ExpressionNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.FieldAccessNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.HeapAccessCompound;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.IPDGNodeVisitor;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.NormalNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.PhiValueNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.PredicateNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.SyncNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IModRef;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ActInHeapNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ActOutHeapNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.FormInHeapNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.FormOutHeapNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ObjGraphParameter;

/**
 * Compute mod-ref information for each heap accessing node.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ObjGraphModRefHeapParams implements IModRef {

	private final PDG pdg;
	private final OrdinalSetMapping<AbstractPDGNode> domain;

	private final BitVectorVariable emptyBitVector = new BitVectorVariable();

	/**
	 * mapping object fields and static fields to the pdg nodes that may
	 * reference those fields (field-get/set and form-in/out, act-in/out nodes)
	 */
	private Map<ParameterField, BitVector> mayRefField;

	/**
	 * mapping points-to set elements of base references to nodes that may
	 * use this as reference (object field access nodes)
	 */
	private Map<InstanceKey, BitVector> mayRefBase;

	private Map<ParameterField, BitVector> mayModField;
	private Map<InstanceKey, BitVector> mayModBase;


	private final PDGModVisitor modVisitor;
	private final PDGRefVisitor refVisitor;

	private final boolean isFieldSensitive;

	public ObjGraphModRefHeapParams(PDG pdg, OrdinalSetMapping<AbstractPDGNode> domain,
			boolean fieldSensitive) {
		this.pdg = pdg;
		this.domain = domain;
		this.isFieldSensitive = fieldSensitive;
		this.modVisitor = new PDGModVisitor();
		this.refVisitor = new PDGRefVisitor();
	}

	public void computeModRef(IProgressMonitor monitor) {
		mayRefField = HashMapFactory.make();
		mayModField = HashMapFactory.make();
		mayRefBase = HashMapFactory.make();
		mayModBase = HashMapFactory.make();

		ModRefVisitor modRef = new ModRefVisitor();
		for (AbstractPDGNode node : domain) {
			node.accept(modRef);
		}

		monitor.worked(1);
	}

	/**
	 * Visit every node in the pdg and memorize nodes that may be referenced
	 * or modified by the current node.
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	private class ModRefVisitor implements IPDGNodeVisitor {

		public void visitExpression(ExpressionNode node) {
			if (node.isArrayAccess()) {
				ParameterField field = node.getField();
				SSAArrayReferenceInstruction aRef = getArrayReference(node);

				if (node.isGet()) {
					handleArrayGet(node, field, aRef);
				} else {
					handleArraySet(node, field, aRef);
				}
			} else if (node.isFieldAccess()) {
				ParameterField field = node.getField();
				SSAFieldAccessInstruction fAcc = getFieldAccess(node);

				if (node.isGet()) {
					if (field.isStatic()) {
						handleStaticFieldGet(node, field, fAcc);
					} else {
						handleFieldGet(node, field, fAcc);
					}
				} else {
					if (field.isStatic()) {
						handleStaticFieldSet(node, field, fAcc);
					} else {
						handleFieldSet(node, field, fAcc);
					}
				}
			}
		}

		/**
		 * An operation writing to an object field references the location
		 * of the value the field is set to and modifies the location where
		 * the field pointer is stored in. This location is identified
		 * by the base points-to set and the field name.
		 * @param node
		 */
		private final void handleFieldSet(ExpressionNode node,
				ParameterField field, SSAFieldAccessInstruction fAcc) {
			int baseRefVar = fAcc.getRef();
			OrdinalSet<InstanceKey> baseRef = pdg.getPointsToSet(baseRefVar);

			// add to mod (baseRef x field)
			addNodeToBitVector(mayModField, field, node);
			addNodeToBitVector(mayModBase, baseRef, node);
		}

		/**
		 * An operation writing to a static field references the location
		 * of the value the field is set to and modifies the location where
		 * the static field is stored in. This location is identified
		 * by the field name.
		 * @param node
		 */
		private final void handleStaticFieldSet(ExpressionNode node,
				ParameterField field, SSAFieldAccessInstruction fAcc) {
			// add to mod (field)
			addNodeToBitVector(mayModField, field, node);
		}

		/**
		 * An operation reading an object field references the location
		 * of the field value and the location where the field pointer is
		 * stored in. This location is identified by the base points-to set
		 * and the field name.
		 * @param node
		 */
		private final void handleFieldGet(ExpressionNode node,
				ParameterField field, SSAFieldAccessInstruction fAcc) {
			int baseRefVar = fAcc.getRef();
			OrdinalSet<InstanceKey> baseRef = pdg.getPointsToSet(baseRefVar);

			// add to ref (baseRef x field)
			addNodeToBitVector(mayRefField, field, node.getFieldValue());
			addNodeToBitVector(mayRefBase, baseRef, node.getFieldValue());
		}

		/**
		 * An operation reading a static field references the location
		 * of the field value and the location where the static field is
		 * stored in. This location is identified by the field name.
		 * @param node
		 */
		private final void handleStaticFieldGet(ExpressionNode node,
				ParameterField field, SSAFieldAccessInstruction fAcc) {
			// add to ref (field)
			addNodeToBitVector(mayRefField, field, node.getFieldValue());
		}

		/**
		 * An operation writing to an array field references the location
		 * of the value the array field is set to and modifies the location
		 * where the array field pointer is stored in. This location is
		 * identified by the base points-to set and the array field.
		 * @param node
		 */
		private final void handleArraySet(ExpressionNode node,
				ParameterField field, SSAArrayReferenceInstruction aRef) {
			int baseRefVar = aRef.getArrayRef();
			OrdinalSet<InstanceKey> baseRef = pdg.getPointsToSet(baseRefVar);

			// add to mod (base x array field)
			addNodeToBitVector(mayModBase, baseRef, node);
			addNodeToBitVector(mayModField, field, node);
		}

		/**
		 * An operation reading an array field references the location
		 * of the array field value and the location where the array field
		 * pointer is stored in. This location is identified by the base
		 * points-to set and the array field.
		 * @param node
		 */
		private final void handleArrayGet(ExpressionNode node,
				ParameterField field, SSAArrayReferenceInstruction aRef) {
			int baseRefVar = aRef.getArrayRef();
			OrdinalSet<InstanceKey> baseRef = pdg.getPointsToSet(baseRefVar);

			// add to ref (base x array field)
			addNodeToBitVector(mayRefBase, baseRef, node.getFieldValue());
			addNodeToBitVector(mayRefField, field, node.getFieldValue());
		}

		public void visitParameter(AbstractParameterNode node) {
			if (node.isOnHeap()) {
				if (node.isActual()) {
					if (node.isOut()) {
						handleActOut((ActOutHeapNode) node);
					} else {
						handleActIn((ActInHeapNode) node);
					}
				} else {
					if (node.isOut()) {
						handleFormOut((FormOutHeapNode) node);
					} else {
						handleFormIn((FormInHeapNode) node);
					}
				}
			}
		}

		private final void handleFormIn(FormInHeapNode node) {
			ParameterField field = node.getBaseField();
			if (node.isStatic()) {
				addNodeToBitVector(mayModField, field, node);
			} else {
				OrdinalSet<InstanceKey> basePts = node.getBasePointsTo();
				addNodeToBitVector(mayModField, field, node);
				addNodeToBitVector(mayModBase, basePts, node);
			}
		}

		private final void handleFormOut(FormOutHeapNode node) {
			ParameterField field = node.getBaseField();
			if (node.isStatic()) {
				addNodeToBitVector(mayRefField, field, node);
			} else {
				OrdinalSet<InstanceKey> basePts = node.getBasePointsTo();
				addNodeToBitVector(mayRefField, field, node);
				addNodeToBitVector(mayRefBase, basePts, node);
			}
		}

		private void handleActIn(ActInHeapNode node) {
			ParameterField field = node.getBaseField();
			if (node.isStatic()) {
				addNodeToBitVector(mayRefField, field, node);
			} else {
				OrdinalSet<InstanceKey> basePts = node.getBasePointsTo();
				addNodeToBitVector(mayRefField, field, node);
				addNodeToBitVector(mayRefBase, basePts, node);
			}
		}

		private void handleActOut(ActOutHeapNode node) {
			ParameterField field = node.getBaseField();
			if (node.isStatic()) {
				addNodeToBitVector(mayModField, field, node);
			} else {
				OrdinalSet<InstanceKey> basePts = node.getBasePointsTo();
				addNodeToBitVector(mayModField, field, node);
				addNodeToBitVector(mayModBase, basePts, node);
			}
		}

		public void visitCall(CallNode node) {}
		public void visitCatch(CatchNode node) {}
		public void visitConstPhiValue(ConstantPhiValueNode node) {}
		public void visitEntry(EntryNode node) {}
		public void visitNormal(NormalNode node) {
			/* heap access compounds are added by the matching expression nodes */
		}
		public void visitPredicate(PredicateNode node) {}
		public void visitSync(SyncNode node) {}
		public void visitPhiValue(PhiValueNode node) {}

	}

	/**
	 * Computes a set of all pdg nodes that refer to a value which may have been
	 * modified by the pdg node provided
	 * @param node pdg node
	 * @return set of nodes which may have been modified by node
	 */
	public BitVectorVariable getMod(AbstractPDGNode node) {
		BitVectorVariable vmod = null;
		// check if node is relevant for our analysis
		if (domain.getMappedIndex(node) != -1) {
			node.accept(modVisitor);
			vmod = modVisitor.getLastMod();
		} else {
			vmod = emptyBitVector;
		}

		return vmod;
	}

	/**
	 * Computes a set of pdg nodes that may provide a value which is referenced
	 * by the pdg node node.
	 * @param node pdg node
	 * @return set of pdg nodes the node node may be referencing
	 */
	public BitVectorVariable getRef(AbstractPDGNode node) {
		BitVectorVariable vref = null;
		// check if node is relevant for our analysis
		if (domain.getMappedIndex(node) != -1) {
			node.accept(refVisitor);
			vref = refVisitor.getLastRef();
		} else {
			vref = emptyBitVector;
		}

		return vref;
	}

	private final class PDGModVisitor implements IPDGNodeVisitor {

		private BitVectorVariable lastMod = emptyBitVector;

		public BitVectorVariable getLastMod() {
			return lastMod;
		}

		public void visitNormal(NormalNode node) {
			lastMod = emptyBitVector;
		}

		/**
		 * Compute a set of nodes reading a value that may have been modified
		 * by this expression node
		 */
		public void visitExpression(ExpressionNode expr) {
			BitVectorVariable modNodes = emptyBitVector;

			if (expr.isSet()) {
				if (expr.isFieldAccess()) {
					SSAFieldAccessInstruction facc = getFieldAccess(expr);

					if (facc.isStatic()) {
						modNodes = handleStaticFieldSet(expr);
					} else {
						modNodes = handleFieldSet(expr, facc);
					}
				} else if (expr.isArrayAccess()) {
					SSAArrayReferenceInstruction aref = getArrayReference(expr);

					modNodes = handleArraySet(expr, aref);
				}
			}

			lastMod = modNodes;
		}

		private final BitVectorVariable handleStaticFieldSet(ExpressionNode expr) {
			// get node accessing the static field
			ParameterField field = expr.getField();

			BitVectorVariable possibleMod = getRef(field, expr);

			return possibleMod;
		}

		private final BitVectorVariable handleFieldSet(ExpressionNode expr, SSAFieldAccessInstruction facc) {
			// get nodes whose basepointers may/must alias and who
			// access the same field
			ParameterField field = expr.getField();
			int baseRef = facc.getRef();
			OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(baseRef);

			BitVectorVariable possibleMod = getRef(field, pts, expr);

			return possibleMod;
		}

		private final BitVectorVariable handleArraySet(ExpressionNode expr, SSAArrayReferenceInstruction aref) {
			ParameterField field = expr.getField();
			int baseRef = aref.getArrayRef();
			OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(baseRef);

			BitVectorVariable possibleMod = getRef(field, pts, expr);

			return possibleMod;
		}

		public void visitParameter(AbstractParameterNode node) {
			BitVectorVariable result = emptyBitVector;

			if (node.isExit() || node.isException()) {
				lastMod = emptyBitVector;
				return;
			}

			if (node.isOnHeap()) {
				if (node.isFormal() && node.isIn()) {
					result = handleFormIn((FormInHeapNode) node);
				} else if (node.isActual() && node.isOut()) {
					result = handleActOut((ActOutHeapNode) node);
				}
			}

			lastMod = result;
		}

		private final BitVectorVariable handleFormIn(FormInHeapNode fIn) {
			BitVectorVariable bv = emptyBitVector;

			ParameterField field = fIn.getBaseField();

			if (fIn.isStatic()) {
				bv = getRef(field, fIn);
			} else {
				OrdinalSet<InstanceKey> baseRef = fIn.getBasePointsTo();
				bv = getRef(field, baseRef, fIn);
			}

			return bv;
		}


		/**
		 * Compute a set of all pdg node that may have been modified by
		 * this actual-in/out parameter node. This are all nodes referenceing
		 * the value this node is modifying
		 */
		private final BitVectorVariable handleActOut(ActOutHeapNode aOut) {
			BitVectorVariable bv = emptyBitVector;

			ParameterField field = aOut.getBaseField();

			if (aOut.isStatic()) {
				bv = getRef(field, aOut);
			} else {
				OrdinalSet<InstanceKey> baseRef = aOut.getBasePointsTo();
				bv = getRef(field, baseRef, aOut);
			}

			return bv;
		}

		/**
		 * get all pdg nodes who may reference the static field field
		 * @param field a static field
		 * @param pnode pdg node of the access operation, this node will be excluded from the result set
		 * @return set of ints (use domain to map back to pdg nodes)
		 */
		private final BitVectorVariable getRef(ParameterField field, AbstractPDGNode pnode) {
			assert (field.isStatic());

			BitVector bv = mayRefField.get(field);

			if (bv != null) {
				removeNodeFromSet(pnode, bv);

				if (!bv.isZero()) {
					BitVectorVariable bvv = new BitVectorVariable();
					bvv.addAll(bv);

					return bvv;
				}
			}

			return emptyBitVector;
		}

		/**
		 * get all pdg nodes who may reference the object field field
		 * @param field non-static object field
		 * @param basePts points-to set of the base reference object
		 * @param pnode pdg node of the access operation, this node will be excluded from the result set
		 * @return set of ints (use domain to map back to pdg nodes)
		 */
		private final BitVectorVariable getRef(ParameterField field, OrdinalSet<InstanceKey> basePts, AbstractPDGNode pnode) {
			assert (!field.isStatic());

			BitVector bvField = mayRefField.get(field);
			BitVector bvAllBases = new BitVector();
			for (InstanceKey ik : basePts) {
				BitVector bvIk = mayRefBase.get(ik);
				if (bvIk != null) {
					bvAllBases.or(bvIk);
				}
			}

			BitVector bvFieldAndBases;
			if (!isFieldSensitive && bvAllBases != null) {
				bvFieldAndBases = bvAllBases;
				removeNodeFromSet(pnode, bvFieldAndBases);
			} else if (bvField == null || bvAllBases == null) {
				bvFieldAndBases = null;
			} else {
				bvFieldAndBases = BitVector.and(bvField, bvAllBases);
				removeNodeFromSet(pnode, bvFieldAndBases);
			}


			if (bvFieldAndBases != null && !bvFieldAndBases.isZero()) {
				BitVectorVariable bvv = new BitVectorVariable();
				bvv.addAll(bvFieldAndBases);

				return bvv;
			} else {
				return emptyBitVector;
			}
		}

		public void visitCall(CallNode node) {
			lastMod = emptyBitVector;
		}

		public void visitCatch(CatchNode node) {
			lastMod = emptyBitVector;
		}

		public void visitEntry(EntryNode node) {
			lastMod = emptyBitVector;
		}

		public void visitPredicate(PredicateNode node) {
			lastMod = emptyBitVector;
		}

		public void visitSync(SyncNode node) {
			lastMod = emptyBitVector;
		}

		public void visitConstPhiValue(ConstantPhiValueNode node) {
			lastMod = emptyBitVector;
		}

		public void visitPhiValue(PhiValueNode node) {
			lastMod = emptyBitVector;
		}

	}


	private final class PDGRefVisitor implements IPDGNodeVisitor {

		private BitVectorVariable lastRef = emptyBitVector;

		public BitVectorVariable getLastRef() {
			return lastRef;
		}

		public void visitNormal(NormalNode nnode) {
			BitVectorVariable ref = emptyBitVector;

			if (nnode.isHeapCompound()) {
				HeapAccessCompound hacc = (HeapAccessCompound) nnode;
				if (hacc.getType() == HeapAccessCompound.Type.FIELD) {
					FieldAccessNode facc = hacc.getAccess();

					if (facc.isGet()) {
						if (facc.isArrayAccess()) {
							ParameterField field = facc.getField();
							SSAArrayReferenceInstruction aRef = getArrayReference(facc);
							ref = handleArrayGet(nnode, field, aRef);
						} else if (facc.isFieldAccess()) {
							ParameterField field = facc.getField();
							SSAFieldAccessInstruction fAcc = getFieldAccess(facc);

							if (field.isStatic()) {
								ref = handleStaticFieldGet(nnode, field, fAcc);
							} else {
								ref = handleFieldGet(nnode, field, fAcc);
							}
						}
					}
				}
			}

			lastRef = ref;
		}

		/**
		 * Compute a set of nodes that this node may be referencing. This are
		 * all nodes that may be modifying the value that this node is reading.
		 */
		public void visitExpression(ExpressionNode node) {
			BitVectorVariable ref = emptyBitVector;

			// add flow if not

			lastRef = ref;
		}

		private final <V> BitVectorVariable getMergedBitVector(AbstractPDGNode node, Map<V, BitVector> map, Iterable<V> set) {
			if (set != null) {
				BitVector bv = new BitVector();
				for (V elem : set) {
					BitVector elemBv = map.get(elem);
					if (elemBv != null) {
						bv.or(elemBv);
					}
				}

				removeNodeFromSet(node, bv);

				if (bv.isZero()) {
					return emptyBitVector;
				} else {
					BitVectorVariable result = new BitVectorVariable();
					result.addAll(bv);

					return result;
				}
			} else {
				return emptyBitVector;
			}
		}

		@SuppressWarnings("unused")
		private final BitVectorVariable handleFieldSet(ExpressionNode node,
				ParameterField field, SSAFieldAccessInstruction fAcc) {
			// when an object field is set, the base object is referenced...
			// search for dependencies concerning base ref
			int baseRefVar = fAcc.getRef();
			OrdinalSet<InstanceKey> baseRefPts = pdg.getPointsToSet(baseRefVar);
			BitVectorVariable result = getMergedBitVector(node, mayModBase, baseRefPts);

			return result;
		}

		@SuppressWarnings("unused")
		private final BitVectorVariable handleStaticFieldSet(ExpressionNode node,
				ParameterField field, SSAFieldAccessInstruction fAcc) {
			// nothing is being referenced when a static field is set
			// the value the static field is set to is stored in an ssa variable
			// so this data dependency has already been treated
			return emptyBitVector;
		}

		/**
		 * when no points-to set for base ref is found we stay conservative
		 * and look for all nodes modifying the field
		 */
		private final BitVectorVariable getBothModMatched(AbstractPDGNode node,
				ParameterField field, OrdinalSet<InstanceKey> pts) {
			BitVector bothMatched;
			if (pts != null && !pts.isEmpty()) {
				bothMatched = getMod(field, pts, node);
			} else {
				bothMatched = getMod(field, node);
			}

			if (bothMatched != null && !bothMatched.isZero()) {
				BitVectorVariable result = new BitVectorVariable();
				result.addAll(bothMatched);

				return result;
			} else {
				return emptyBitVector;
			}
		}

		private final BitVectorVariable handleFieldGet(AbstractPDGNode node,
				ParameterField field, SSAFieldAccessInstruction fAcc) {
			// search for nodes defining base ref + facc.field
			int baseRefVar = fAcc.getRef();
			OrdinalSet<InstanceKey> baseRefPts = pdg.getPointsToSet(baseRefVar);
			BitVectorVariable result = getBothModMatched(node, field, baseRefPts);

			return result;
		}

		private final BitVectorVariable handleStaticFieldGet(AbstractPDGNode node,
				ParameterField field, SSAFieldAccessInstruction fAcc) {
			// search for nodes defining facc.field  -> these should be in mayModField and NOT in may ref!
			BitVector mayMod = getMod(field, node);

			if (mayMod != null && !mayMod.isZero()) {
				BitVectorVariable result = new BitVectorVariable();
				result.addAll(mayMod);

				return result;
			} else {
				return emptyBitVector;
			}
		}

		@SuppressWarnings("unused")
		private final BitVectorVariable handleArraySet(ExpressionNode node,
				ParameterField field, SSAArrayReferenceInstruction aRef) {
			// search for dependencies concerning base ref
			int baseRefVar = aRef.getArrayRef();
			OrdinalSet<InstanceKey> baseRefPts = pdg.getPointsToSet(baseRefVar);
			BitVectorVariable result = getMergedBitVector(node, mayModBase, baseRefPts);

			return result;
		}

		private final BitVectorVariable handleArrayGet(AbstractPDGNode node,
				ParameterField field, SSAArrayReferenceInstruction aRef) {
			// search for nodes defining base ref + facc.field
			int baseRefVar = aRef.getArrayRef();
			OrdinalSet<InstanceKey> baseRefPts = pdg.getPointsToSet(baseRefVar);
			BitVectorVariable result = getBothModMatched(node, field, baseRefPts);

			return result;
		}

		/**
		 * Computes a set of pdg nodes that may have modified the value
		 * that is read by this node
		 */
		public void visitParameter(AbstractParameterNode node) {
			BitVectorVariable ref = emptyBitVector;

			if (node.isExit() || node.isException()) {
				lastRef = emptyBitVector;
				return;
			}

			if (node.isOnHeap()) {
				if (node.isFormal() && node.isOut()) {
					ref = handleFormOut((FormOutHeapNode) node);
				} else if (node.isActual() && node.isIn()) {
					ref = handleActIn((ActInHeapNode) node);
				}
			}

			lastRef = ref;
		}

		private final BitVectorVariable handleActIn(ActInHeapNode aIn) {
			BitVectorVariable result = getUncheckedMod(aIn);

			return result;
		}

		private final BitVectorVariable handleFormOut(FormOutHeapNode fOut) {
			BitVectorVariable result = getUncheckedMod(fOut);

			return result;
		}

		/**
		 * This methods answers the question which nodes may have modified
		 * the value referenced by the provided parameter node.
		 * @param node parameter node
		 * @return set of nodes modifying the value the parameter node may be referencing
		 */
		private BitVectorVariable getUncheckedMod(ObjGraphParameter node) {
			ParameterField field = node.getBaseField();

			BitVector modSet = null;
			if (node.isStatic()) {
				modSet = getMod(field, node);
			} else {
				OrdinalSet<InstanceKey> basePts = node.getBasePointsTo();
				modSet = getMod(field, basePts, node);
			}

			if (modSet != null && !modSet.isZero()) {
				BitVectorVariable result = new BitVectorVariable();
				result.addAll(modSet);

				return result;
			} else {
				return emptyBitVector;
			}
		}

		/**
		 * get all pdg nodes who may modify the static field field
		 * @param field a static field
		 * @param pnode pdg node of the access operation, this node will be excluded from the result set
		 * @return set of ints (use domain to map back to pdg nodes)
		 */
		private BitVector getMod(ParameterField field, AbstractPDGNode pnode) {
			assert (field.isStatic() || field.isArray() || true);
			// assertion is out of order because:
			// due to incomplete points-to information it may be that an object field is being changed without
			// a valid base pointer set

			BitVector bv = mayModField.get(field);

			if (bv != null) {
				removeNodeFromSet(pnode, bv);
			}

			return bv;
		}

		/**
		 * get all pdg nodes who may modify the object field field
		 * @param field non-static object field
		 * @param basePts points-to set of the base reference object
		 * @param pnode pdg node of the access operation, this node will be excluded from the result set
		 * @return set of ints (use domain to map back to pdg nodes)
		 */
		private BitVector getMod(ParameterField field, OrdinalSet<InstanceKey> basePts, AbstractPDGNode pnode) {
			assert (!field.isStatic());
			assert (basePts != null);

			BitVector bvField = mayModField.get(field);
			BitVector bvAllBases = new BitVector();
			for (InstanceKey ik : basePts) {
				BitVector bvIk = mayModBase.get(ik);
				if (bvIk != null) {
					bvAllBases.or(bvIk);
				}
			}

			BitVector bvFieldAndBases;
			if (bvField == null || bvAllBases == null) {
				bvFieldAndBases = null;
			} else {
				bvFieldAndBases = BitVector.and(bvField, bvAllBases);
				removeNodeFromSet(pnode, bvFieldAndBases);
			}

			return bvFieldAndBases;
		}

		public void visitCall(CallNode node) {
			lastRef = emptyBitVector;
		}

		public void visitEntry(EntryNode node) {
			lastRef = emptyBitVector;
		}

		public void visitCatch(CatchNode node) {
			lastRef = emptyBitVector;
		}

		public void visitPredicate(PredicateNode node) {
			lastRef = emptyBitVector;
		}

		public void visitSync(SyncNode node) {
			lastRef = emptyBitVector;
		}

		public void visitConstPhiValue(ConstantPhiValueNode node) {
			lastRef = emptyBitVector;
		}

		public void visitPhiValue(PhiValueNode node) {
			lastRef = emptyBitVector;
		}


	}

	private final SSAArrayReferenceInstruction getArrayReference(ExpressionNode node) {
		assert (node.isGet() || node.isSet());
		assert (node.isArrayAccess());
		assert (node.getPdgId() == pdg.getId());

		SSAInstruction instr = pdg.getInstructionForNode(node);

		assert (instr != null);
		assert (instr instanceof SSAArrayReferenceInstruction);

		return (SSAArrayReferenceInstruction) instr;
	}

	private final SSAFieldAccessInstruction getFieldAccess(ExpressionNode node) {
		assert (node.isGet() || node.isSet());
		assert (node.isFieldAccess());
		assert (node.getPdgId() == pdg.getId());

		SSAInstruction instr = pdg.getInstructionForNode(node);

		assert (instr != null);
		assert (instr instanceof SSAFieldAccessInstruction);

		return (SSAFieldAccessInstruction) instr;
	}

	private final <V> void addNodeToBitVector(Map<V, BitVector> map, Iterable<V> keys, AbstractPDGNode node) {
		for (V key : keys) {
			addNodeToBitVector(map, key, node);
		}
	}

	private static final <V> BitVector getBV(Map<V, BitVector> map, V key) {
		BitVector bv = map.get(key);
		if (bv == null) {
			bv = new BitVector();
			map.put(key, bv);
		}

		return bv;
	}

	private final <V> void addNodeToBitVector(Map<V, BitVector> map, V key, AbstractPDGNode node) {
		BitVector bv = getBV(map, key);

		int nodeId = domain.getMappedIndex(node);

		assert (nodeId >= 0);

		bv.set(nodeId);
	}

	private final void removeNodeFromSet(AbstractPDGNode node, BitVector set) {
		int thisNode = domain.getMappedIndex(node);

		assert (thisNode != -1);
		assert (set != null);

		set.clear(thisNode);
	}

}
