/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.dataflow;

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

import edu.kit.joana.deprecated.jsdg.sdg.IntermediatePDG;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CatchNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ConstantPhiValueNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.EntryNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ExpressionNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.IPDGNodeVisitor;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.NormalNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.PhiValueNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.PredicateNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.SyncNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IModRef;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ActualInOutNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.FormInOutNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ParameterNode;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ModRefHeapParams implements IModRef {

	private final OrdinalSetMapping<AbstractPDGNode> domain;
	private final IntermediatePDG pdg;
	private final BitVectorVariable emptyBitVector = new BitVectorVariable();

	/*
	 * mapping object fields and static fields to the pdg nodes that may
	 * reference those fields (field-get/set and form-in/out, act-in/out nodes)
	 */
	private Map<ParameterField, BitVector> mayRefField;

	/*
	 * mapping points-to set elements of base references to nodes that may
	 * use this as reference (object field access nodes)
	 */
	private Map<InstanceKey, BitVector> mayRefBase;

	private Map<ParameterField, BitVector> mayModField;
	private Map<InstanceKey, BitVector> mayModBase;


	private final PDGModVisitor modVisitor = new PDGModVisitor();
	private final PDGRefVisitor refVisitor = new PDGRefVisitor();

	public ModRefHeapParams(OrdinalSetMapping<AbstractPDGNode> domain, IntermediatePDG pdg) {
		this.domain = domain;
		this.pdg = pdg;
	}

	public void computeModRef(IProgressMonitor monitor) {
		mayRefBase = HashMapFactory.make();//new MultiMap<InstanceKey, AbstractPDGNode, Set<AbstractPDGNode>>();
		mayRefField = HashMapFactory.make();//new MultiMap<IField, AbstractPDGNode, Set<AbstractPDGNode>>();
		mayModBase = HashMapFactory.make();
		mayModField = HashMapFactory.make();

		IPDGNodeVisitor modRefVisitor = new ModRefVisitor();
		for (AbstractPDGNode node : domain) {
			node.accept(modRefVisitor);
		}

		monitor.worked(1);
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

	private final <V> void addNodeToBitVector(Map<V, BitVector> map, V key, AbstractPDGNode node) {
		BitVector bv = map.get(key);
		if (bv == null) {
			bv = new BitVector();
			map.put(key, bv);
		}

		int nodeId = domain.getMappedIndex(node);

		assert (nodeId >= 0);

		bv.set(nodeId);
	}

	private final void removeThisNodeFromSet(AbstractPDGNode node, BitVector set) {
		int thisNode = domain.getMappedIndex(node);

		assert (thisNode != -1);
		assert (set != null);

		set.clear(thisNode);
	}



	private class ModRefVisitor implements IPDGNodeVisitor {

		public void visitExpression(ExpressionNode node) {
			/*
			 * build may reference information - this should have been done
			 * earlier
			 */
			if (node.isArrayAccess()) {
				SSAArrayReferenceInstruction aref = getArrayReference(node);
				ParameterField field = node.getField();
				OrdinalSet<InstanceKey> baseRef = pdg.getPointsToSet(aref.getArrayRef());

				if (node.isGet()) {
					if (baseRef != null) {
						// add ik -> node references
						for (InstanceKey ik : baseRef) {
							addNodeToBitVector(mayRefBase, ik, node);
						}
					}

					if (field != null) {
						addNodeToBitVector(mayRefField, field, node);
					}
				} else if (node.isSet()) {
					if (baseRef != null) {
						// add ik -> node references
						for (InstanceKey ik : baseRef) {
							addNodeToBitVector(mayRefBase, ik, node);
							addNodeToBitVector(mayModBase, ik, node);
						}
					}

					if (field != null) {
						addNodeToBitVector(mayModField, field, node);
					}
				} else {
					throw new IllegalStateException("Array access must be a get or set operation.");
				}
			} else if (node.isFieldAccess()) {

				SSAFieldAccessInstruction facc = getFieldAccess(node);
				ParameterField field = node.getField();

				if (node.isGet() && facc.isStatic()) {
					if (field != null) {
						// add node to list of referencing static field
						addNodeToBitVector(mayRefField, field, node);
					}
				} else if (node.isGet() && !facc.isStatic()) {
					OrdinalSet<InstanceKey> baseRef = pdg.getPointsToSet(facc.getRef());
					if (baseRef != null) {
						// add ik -> node references
						for (InstanceKey ik : baseRef) {
							addNodeToBitVector(mayRefBase, ik, node);
						}
					}

					if (field != null) {
						addNodeToBitVector(mayRefField, field, node);
					}
				} else if (node.isSet() && !facc.isStatic()) {
					OrdinalSet<InstanceKey> baseRef = pdg.getPointsToSet(facc.getRef());
					if (baseRef != null) {
						// add ik -> node references
						for (InstanceKey ik : baseRef) {
							addNodeToBitVector(mayRefBase, ik, node);
							addNodeToBitVector(mayModBase, ik, node);
						}
					}

					if (field != null) {
						addNodeToBitVector(mayModField, field, node);
					}
				} else if (node.isSet() && facc.isStatic()) {
					if (field != null) {
						addNodeToBitVector(mayModField, field, node);
					}
				}
			}
		}

		public void visitParameter(AbstractParameterNode node) {
			if (node.isExit() || node.isException()) {
				return;
			}

			if (node.isActual()) {
				/**
				 * All actual in/out nodes are accepted except non-static root
				 * nodes. These nodes have already been treated by the ssa
				 * variable dataflow.
				 */

				ActualInOutNode param = (ActualInOutNode) node;

				if (param.isIn() && param.isStatic()) {
					addNodeToBitVector(mayRefField, param.getField(), param);
				} else if (param.isIn() && !param.isStatic()) {
					for (InstanceKey ik : param.getParent().getPointsTo()) {
						addNodeToBitVector(mayRefBase, ik, param);
					}

					addNodeToBitVector(mayRefField, param.getField(), param);
				} if (param.isOut() && param.isStatic()) {
					addNodeToBitVector(mayModField, param.getField(), param);
				} else if (param.isOut() && !param.isStatic()) {
					for (InstanceKey ik : param.getParent().getPointsTo()) {
						addNodeToBitVector(mayModBase, ik, param);
					}

					addNodeToBitVector(mayModField, param.getField(), param);
				}
			} else if (node.isFormal()) {
				/**
				 * No form-in/out nodes from other pdgs are accepted.
				 * Non-static root nodes are also not accepted as their dataflow
				 * has already been treated.
				 */

				FormInOutNode param = (FormInOutNode) node;

				if (param.isOut() && param.isStatic()) {
					addNodeToBitVector(mayRefField, param.getField(), param);
				} else if (param.isOut() && !param.isStatic()) {
					for (InstanceKey ik : param.getParent().getPointsTo()) {
						addNodeToBitVector(mayRefBase, ik, param);
					}
					addNodeToBitVector(mayRefField, param.getField(), param);
				} else if (param.isIn() && param.isStatic()) {
					addNodeToBitVector(mayModField, param.getField(), param);
				} else if (param.isIn() && !param.isStatic()) {
					for (InstanceKey ik : param.getParent().getPointsTo()) {
						addNodeToBitVector(mayModBase, ik, param);
					}
					addNodeToBitVector(mayModField, param.getField(), param);
				}
			}
		}

		public void visitCall(CallNode node) {}
		public void visitCatch(CatchNode node) {}
		public void visitConstPhiValue(ConstantPhiValueNode node) {}
		public void visitEntry(EntryNode node) {}
		public void visitNormal(NormalNode node) {}
		public void visitPredicate(PredicateNode node) {}
		public void visitSync(SyncNode node) {}
		public void visitPhiValue(PhiValueNode node) {}

	}


	private final class PDGModVisitor implements IPDGNodeVisitor {
		private BitVectorVariable lastMod = emptyBitVector;

		public BitVectorVariable getLastMod() {
			return lastMod;
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

		/**
		 * Compute a set of nodes reading a value that may have been modified
		 * by this expression node
		 */
		public void visitExpression(ExpressionNode expr) {
			BitVectorVariable vmod = emptyBitVector;

			if (expr.isSet()) {
				BitVector modNodes = null;

				if (expr.isFieldAccess()) {
					SSAFieldAccessInstruction facc = getFieldAccess(expr);
					if (facc.isStatic()) {
						// get node accessing the static field
						ParameterField field = expr.getField();
						modNodes = getRef(field, expr);
					} else {
						// get nodes whose basepointers may/must alias and who
						// access the same field
						ParameterField field = expr.getField();
						int baseRef = facc.getRef();
						OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(baseRef);

						modNodes = getRef(field, pts, expr);
					}

				} else if (expr.isSet() && expr.isArrayAccess()) {
					SSAArrayReferenceInstruction aref = getArrayReference(expr);

					ParameterField field = expr.getField();
					int baseRef = aref.getArrayRef();
					OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(baseRef);

					modNodes = getRef(field, pts, expr);
				}

				if (modNodes != null && !modNodes.isZero()) {
					vmod = new BitVectorVariable();
					vmod.addAll(modNodes);
				}
			}

			lastMod = vmod;
		}

		public void visitParameter(AbstractParameterNode node) {
			BitVectorVariable result = emptyBitVector;

			if (node.isExit() || node.isException()) {
				lastMod = emptyBitVector;
				return;
			}

			if (node.isFormal() && node.isIn()) {
				FormInOutNode param = (FormInOutNode) node;

				ParameterField field = param.getField();

				BitVector modNodes = null;
				if (param.isStatic()) {
					modNodes = getRef(field, param);
				} else {
					assert (!param.isRoot());
					assert (param.getParent().getPointsTo() != null);

					ParameterNode<?> parent = param.getParent();
					OrdinalSet<InstanceKey> baseRef = parent.getPointsTo();
					modNodes = getRef(field, baseRef, param);
				}

				if (modNodes != null && !modNodes.isZero()) {
					result = new BitVectorVariable();
					result.addAll(modNodes);
				}
			} else if (node.isActual() && node.isOut()) {
				/**
				 * Compute a set of all pdg node that may have been modified by
				 * this actual-in/out parameter node. This are all nodes referenceing
				 * the value this node is modifying
				 */

				ActualInOutNode param = (ActualInOutNode) node;
				ParameterField field = param.getField();

				BitVector modNodes = null;
				if (param.isStatic()) {
					modNodes = getRef(field, param);
				} else {
					assert (!param.isRoot());
					assert (param.getParent().getPointsTo() != null);

					ParameterNode<?> parent = param.getParent();
					OrdinalSet<InstanceKey> baseRef = parent.getPointsTo();
					modNodes = getRef(field, baseRef, param);
				}

				if (modNodes != null && !modNodes.isZero()) {
					result = new BitVectorVariable();
					result.addAll(modNodes);
				}
			}

			lastMod = result;
		}

		public void visitNormal(NormalNode node) {
			lastMod = emptyBitVector;
		}

		public void visitPredicate(PredicateNode node) {
			lastMod = emptyBitVector;
		}

		public void visitSync(SyncNode node) {
			lastMod = emptyBitVector;
		}

		/**
		 * get all pdg nodes who may reference the static field field
		 * @param field a static field
		 * @param pnode pdg node of the access operation, this node will be excluded from the result set
		 * @return set of ints (use domain to map back to pdg nodes)
		 */
		private BitVector getRef(ParameterField field, AbstractPDGNode pnode) {
			assert (field.isStatic());

			BitVector bv = mayRefField.get(field);

			if (bv != null) {
				removeThisNodeFromSet(pnode, bv);
			}

			return bv;
		}

		/**
		 * get all pdg nodes who may reference the object field field
		 * @param field non-static object field
		 * @param basePts points-to set of the base reference object
		 * @param pnode pdg node of the access operation, this node will be excluded from the result set
		 * @return set of ints (use domain to map back to pdg nodes)
		 */
		private BitVector getRef(ParameterField field, OrdinalSet<InstanceKey> basePts, AbstractPDGNode pnode) {
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
			if (bvField == null || bvAllBases == null) {
				bvFieldAndBases = null;
			} else {
				bvFieldAndBases = BitVector.and(bvField, bvAllBases);
				removeThisNodeFromSet(pnode, bvFieldAndBases);
			}

			return bvFieldAndBases;
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

		public void visitCall(CallNode node) {
			lastRef = emptyBitVector;
		}

		public void visitEntry(EntryNode node) {
			lastRef = emptyBitVector;
		}

		/**
		 * Compute a set of nodes that this node may be referencing. This are
		 * all nodes that may be modifying the value that this node is reading.
		 */
		public void visitExpression(ExpressionNode node) {
			BitVectorVariable ref = new BitVectorVariable();

			if (node.isFieldAccess()) {
				SSAFieldAccessInstruction facc = getFieldAccess(node);
				ParameterField field = node.getField();

				if (node.isGet() && facc.isStatic()) {
					// search for nodes defining facc.field  -> these should be in mayModField and NOT in may ref!
					if (field != null) {
						BitVector mayMod = getMod(field, node);
						if (mayMod != null) {
							ref.addAll(mayMod);
						}
					}
				}

				if (!facc.isStatic()) {
					if (node.isGet()){
						// search for nodes defining base ref + facc.field
						OrdinalSet<InstanceKey> iks = pdg.getPointsToSet(facc.getRef());

						BitVector bothMatched;
						if (iks != null && !iks.isEmpty()) {
							bothMatched = getMod(field, iks, node);
						} else {
							bothMatched = getMod(field, node);
						}

						if (bothMatched != null) {
							ref.addAll(bothMatched);
						}
					} else if (node.isSet()) {
						// search for dependencies concerning base ref
						BitVector baseRef = new BitVector();
						OrdinalSet<InstanceKey> iks = pdg.getPointsToSet(facc.getRef());
						if (iks != null) {
							for (InstanceKey ik : iks) {
								BitVector mayMod = mayModBase.get(ik);
								if (mayMod != null) {
									baseRef.or(mayMod);
								}
							}
						}

						removeThisNodeFromSet(node, baseRef);
						ref.addAll(baseRef);
					}
				}
			} else if (node.isArrayAccess()) {
				SSAArrayReferenceInstruction aref = getArrayReference(node);
				ParameterField field = node.getField();

				if (node.isGet()){
					// search for nodes defining base ref + facc.field
					OrdinalSet<InstanceKey> iks = pdg.getPointsToSet(aref.getArrayRef());

					BitVector bothMatched;
					if (iks != null && !iks.isEmpty()) {
						bothMatched = getMod(field, iks, node);
					} else {
						bothMatched = getMod(field, node);
					}

					if (bothMatched != null) {
						ref.addAll(bothMatched);
					}
				} else if (node.isSet()) {
					// search for dependencies concerning base ref
					BitVector baseRef = new BitVector();
					OrdinalSet<InstanceKey> iks = pdg.getPointsToSet(aref.getArrayRef());
					if (iks != null) {
						for (InstanceKey ik : iks) {
							BitVector mayMod = mayModBase.get(ik);
							if (mayMod != null) {
								baseRef.or(mayMod);
							}
						}
					}

					removeThisNodeFromSet(node, baseRef);
					ref.addAll(baseRef);
				}
			}

			if (ref.getValue() == null || ref.getValue().isEmpty()) {
				// we do not need to create multiple empty sets
				lastRef = emptyBitVector;
			} else {
				lastRef = ref;
			}
		}

		/**
		 * Computes a set of pdg nodes that may have modified the value
		 * that is read by this node
		 */
		public void visitParameter(AbstractParameterNode node) {
			if (node.isExit() || node.isException()) {
				lastRef = emptyBitVector;
				return;
			}

			BitVectorVariable ref = emptyBitVector;

			if (node.isFormal() && node.isOut()) {
				FormInOutNode param = (FormInOutNode) node;
				ref = getUncheckedMod(param);
			} else if (node.isActual() && node.isIn()) {
				ActualInOutNode param = (ActualInOutNode) node;
				ref = getUncheckedMod(param);
			}

			lastRef = ref;
		}


		public void visitNormal(NormalNode node) {
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

		/**
		 * This methods answers the question of which nodes may be referencing
		 * the provided parameter node. It does not return a set of nodes
		 * the provided node may reference
		 * @param node parameter node
		 * @return set of nodes may-referencing the parameter node provided
		 *
		private BitVectorVariable getUncheckedRef(ParameterNode<?> node) {
			BitVectorVariable ref = new BitVectorVariable();

			BitVector baseRef = null;
			BitVector fieldRef = mayRefField.get(node.getField());

			if (!node.isStatic()) {
				OrdinalSet<InstanceKey> ptsBase = node.getParent().getPointsTo();
				if (ptsBase != null) {
					baseRef = new BitVector();
					for (InstanceKey ik : ptsBase) {
						BitVector mayRef = mayRefBase.get(ik);
						if (mayRef != null) {
							baseRef.or(mayRef);
						}
					}
				}
			}

			if (baseRef != null) {
				BitVector matchesBoth;
				if (fieldRef != null) {
					matchesBoth = BitVector.and(fieldRef, baseRef);
				} else {
					matchesBoth = baseRef;
				}
				ref.addAll(matchesBoth);
			} else if (fieldRef != null) {
				ref.addAll(fieldRef);
			}

			int thisNode = domain.getMappedIndex(node);
			if (Assertions.verifyAssertions) {
				Assertions._assert(thisNode != -1);
			}

			ref.clear(thisNode);


			return ref;
		}*/

		/**
		 * This methods answers the question of which nodes may have modified
		 * the value referenced by the provided parameter node.
		 * @param node parameter node
		 * @return set of nodes modifying the value the parameter node may be referencing
		 */
		private BitVectorVariable getUncheckedMod(ParameterNode<?> node) {
			BitVectorVariable ref = new BitVectorVariable();

			BitVector modSet = null;
			if (node.isStatic()) {
				modSet = getMod(node.getField(), node);
			} else {
				modSet = getMod(node.getField(), node.getParent().getPointsTo(), node);
			}

			if (modSet != null) {
				ref.addAll(modSet);
			}

			return ref;
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
				removeThisNodeFromSet(pnode, bv);
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
			assert (!basePts.isEmpty());

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
				removeThisNodeFromSet(pnode, bvFieldAndBases);
			}

			return bvFieldAndBases;
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

}
