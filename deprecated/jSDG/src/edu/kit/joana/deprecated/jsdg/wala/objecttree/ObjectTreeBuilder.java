/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.wala.objecttree;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.propagation.AbstractFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.ArrayContentsKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.slicer.HeapExclusions;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.deprecated.jsdg.sdg.nodes.ArrayField;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ObjectField;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterFieldFactory;
import edu.kit.joana.deprecated.jsdg.util.Debug;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.deprecated.jsdg.util.Util;
import edu.kit.joana.wala.util.MultiTree;

/**
 * Encapsulates the creation of (sub)objecttrees from a given points-to set.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ObjectTreeBuilder {

	private final HeapGraph heap;
	private final HeapExclusions excl;
	private final PointerAnalysis pta;

	/**
	 * Stores all pointerkey objects that have been visited so far during
	 * the calculation. Every pointerkey has only to be visited once.
	 */
	private Set<Integer> visitedPks;

	/**
	 * Stores all instancekey objects that have been visited so far during
	 * the calculation. Every instancekey has only to be visited once.
	 */
	private Set<Integer> visitedIks;

	public ObjectTreeBuilder(PointerAnalysis pta) {
		this.heap = pta.getHeapGraph();
		this.pta = pta;
		AnalysisScope scope = pta.getClassHierarchy().getScope();
		this.excl = new HeapExclusions(scope.getExclusions());
	}

	/**
	 * Returns a set of all possible referenced object fields from an provided
	 * object field.
	 * @param ptkBase PointerKey of the base object that hold a reference to field
	 * @param field The field whose subobject tree has to be calculated
	 * @return Set of all possible referenced object fields from and including
	 * object field field.
	 */
	public synchronized MultiTree<FieldRef> getSubobjectTree(
			PointerKey ptkBase, ParameterField field) {
		MultiTree<FieldRef> tree = null;

		if (Debug.Var.PRINT_SUBOBJECT_TREE_INFO.isSet()) {
			System.err.println("\nSearching subobject tree for " + ptkBase + " and field " + field);
			System.err.println(ptkBase.getClass().getCanonicalName());
		}

		Set<PointerKey> ptkField = getFieldPointerKeys(ptkBase, field);
		OrdinalSet<InstanceKey> pts = null;
		/*
		 * TODO The points-to set of all possible base references are merged. This
		 * is source of some imprecision
		 */
		for (PointerKey fieldPk : ptkField) {
			OrdinalSet<InstanceKey> ptsField = pta.getPointsToSet(fieldPk);
			if (pts == null) {
				pts = ptsField;
			} else {
				pts = OrdinalSet.unify(pts, ptsField);
			}
		}

		if (pts != null && !pts.isEmpty()) {
			FieldRef rootField = new FieldRef(field, pts);

			// add root node to the visited pk and ik sets
			{
				int ikHash = createHashIK(ptkField, rootField);
		        visitedIks = new HashSet<Integer>();
				visitedIks.add(ikHash);

				int pkHash = createHashPK(pts, rootField);
				visitedPks = new HashSet<Integer>();
				visitedPks.add(pkHash);
			}

			Node<FieldRef> root = buildInternalTree(rootField);
			tree = convert(root);

			assert (tree.value() != null);
			assert (tree.value().getField() == field);
		} else {
			// this is legal as not every field need to have a points-to set
			// assigned
			Log.debug("No subobjecttree for field " + field + " could be " +
				"created as no points-to information has been found.");
		}

		return tree;
	}

	private Set<PointerKey> getFieldPointerKeys(PointerKey pkParent, ParameterField field) {
		if (field.isArray()) {
			return getArrayFieldPointerKeys(pkParent, (ArrayField) field);
		} else {
			return getObjectFieldPointerKeys(pkParent, (ObjectField) field);
		}
	}

	private Set<PointerKey> getObjectFieldPointerKeys(PointerKey pkParent, ObjectField ofield) {
		Set<PointerKey> result = new HashSet<PointerKey>();

		IField field = ofield.getField();
		OrdinalSet<InstanceKey> pts = pta.getPointsToSet(pkParent);
		for (InstanceKey ikParent : pts) {
			for (Iterator<?> it = heap.getSuccNodes(ikParent); it.hasNext();) {
				Object obj = it.next();
				if (obj instanceof InstanceFieldKey) {
					InstanceFieldKey fieldKey = (InstanceFieldKey) obj;

					if (field.equals(fieldKey.getField())) {
						result.add(fieldKey);
					}
				}
			}
		}

		if (result.isEmpty() && !excl.excludes(pkParent)) {
			Log.warn("Found no PointerKey for the field " + Util.fieldName(ofield));
		}

		return result;
	}

	private Set<PointerKey> getArrayFieldPointerKeys(PointerKey pkParent, ArrayField afield) {
		Set<PointerKey> result = new HashSet<PointerKey>();

		OrdinalSet<InstanceKey> pts = pta.getPointsToSet(pkParent);
		for (InstanceKey ikParent : pts) {
			for (Iterator<?> it = heap.getSuccNodes(ikParent); it.hasNext();) {
				Object obj = it.next();
				if (obj instanceof ArrayContentsKey) {
					ArrayContentsKey aKey = (ArrayContentsKey) obj;

					//TODO conservative - we simply add every reachable array access without checking anything else
					result.add(aKey);
				}
			}
		}


		if (result.isEmpty() && !excl.excludes(pkParent)) {
			Log.warn("Found no PointerKey for the arrayfield of " + pkParent);
		}

		return result;
	}

	/**
	 * Builds an objecttree of all object fields that may be refenrenced by
	 * a specified pointerkey and the parent object fields that may point to
	 * them. E.g. for A.x.y.z and a pointerkey pk pointing to the same location
	 * as field y, the calculated subobject tree is A.x.y
	 * @param pKey pointerkey
	 * @return tree describing the possible field access to this pointer key
	 */
	public synchronized MultiTree<FieldRef> getSubobjectTree(PointerKey pKey) {
		Node<FieldRef> root = buildInternalTree(pKey);

		if (Debug.Var.PRINT_SUBOBJECT_TREE_INFO.isSet()) {
			System.err.println("\nSearching subobject tree for " + pKey);
			System.err.println(pKey.getClass().getCanonicalName());
		}

		// convert the intermediate representation to a MultiTree<IField> object
		return convert(root);
	}

	private Node<FieldRef> buildInternalTree(FieldRef rootField) {
        assert (visitedIks != null);
        assert (visitedIks.size() == 1) : "Only IK root node should have been already added. Size: " + visitedIks.size();
        assert (visitedPks != null);
        assert (visitedPks.size() == 1) : "Only PK root node should have been already added. Size: " + visitedPks.size();

        Node<FieldRef> root = new Node<FieldRef>(rootField);

        for (InstanceKey ik : rootField.getPointsToSet()) {
            buildTree(ik, root);
        }

        return root;
	}

	private Node<FieldRef> buildInternalTree(PointerKey pKey) {
        visitedPks = new HashSet<Integer>();
        visitedIks = new HashSet<Integer>();

        Node<FieldRef> root = new Node<FieldRef>(null);

        buildTree(pKey, root);

        return root;
	}

	public final static class FieldRef {
		private final ParameterField field;
		private final OrdinalSet<InstanceKey> pointsToSet;

		public FieldRef(ParameterField field) {
			assert (field != null);
			assert field.isPrimitiveType() : "A non-primitive field should have a points-to set associated";

			this.field = field;
			this.pointsToSet = null;
		}

		public FieldRef(ParameterField field, OrdinalSet<InstanceKey> pts) {
			assert (field != null);
			assert (pts != null);
			assert (!field.isPrimitiveType()) :	"A primitive field should not have a points-to set associated";

			this.field = field;
			this.pointsToSet = pts;
		}

		public ParameterField getField() {
			return field;
		}

		public OrdinalSet<InstanceKey> getPointsToSet() {
			return pointsToSet;
		}

		public boolean isPrimitive() {
			return pointsToSet == null;
		}

		public boolean isLeafField() {
			return pointsToSet == null || pointsToSet.isEmpty();
		}

		public String toString() {
			return (isPrimitive() ? "primitive " : pointsToSet + " ") + Util.fieldName(field);
		}
	}

	/**
	 * Intermediate representation of the generated multitree. Better suited
	 * to the algorithm of the creation process (bottom up creation).
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 * @param <C> type of the value entry the node may hold
	 */
	private static class Node<C> {

		private final C val;
		private final Set<Node<C>> parents;
		private final Set<Node<C>> children;

		private Node(C val) {
			this.val = val;
			this.parents = new HashSet<Node<C>>();
			this.children = new HashSet<Node<C>>();
		}

		public void addParent(C val) {
			addParent(new Node<C>(val));
		}

		public void addParent(Node<C> node) {
			assert (!(node.getVal() == null && !parents.isEmpty())) : "Adding a top node to a non-empty parents set.";

			parents.add(node);
		}

		public void removeParent(Node<C> node) {
			parents.remove(node);
		}

		public boolean hasParent(Node<C> node) {
			return children.contains(node);
		}

		public int hashCode() {
			return (val == null ? 0 : val.hashCode());
		}

		public boolean equals(Object obj) {
			if (obj != null && obj instanceof Node) {
				return hashCode() == obj.hashCode();
			} else {
				return false;
			}

		}

		public boolean hasParent(C val) {
			return parents.contains(val);
		}

		public Node<C> getParent(C val) {
			for (Node<C> node : parents) {
				if (val.equals(node.getVal())) {
					return node;
				}
			}

			return null;
		}

		public Set<Node<C>> getParents() {
			return parents;
		}

		public void addChild(Node<C> node) {
			assert (!(node.getVal() == null && !children.isEmpty())) : "Adding a bottom node to a non-empty child set.";

			children.add(node);
		}

		public void removeChild(Node<C> node) {
			children.remove(node);
		}

		public boolean hasChild(Node<C> node) {
			return children.contains(node);
		}

		public boolean hasChildren() {
			return !children.isEmpty();
		}

		public boolean hasBottomChild() {
			if (children.size() == 1) {
				return children.iterator().next().getVal() == null;
			}

			return false;
		}

		public Node<C> getChild(C val) {
			for (Node<C> node : children) {
				if (val.equals(node.getVal())) {
					return node;
				}
			}

			return null;
		}

		public Set<Node<C>> getChildren() {
			return children;
		}

		public C getVal() {
			return val;
		}

		public boolean isRoot() {
			return parents.isEmpty();
		}

		public String toString() {
			StringBuffer str = new StringBuffer(val != null ? val.toString() : "null");

			if (children != null) {
				for (Node<C> child : children) {
					str.append("(");
					str.append(child.toString());
					str.append(")");
				}
			}

			return str.toString();
		}
	}

	/**
	 * Converts the intermediate objecttree representation to a MultiTree object
	 *
	 * @param root
	 *            root of the intermediate objecttree
	 * @return multitree containting same information as the intermediate
	 *         objecttree
	 */
	private MultiTree<FieldRef> convert(Node<FieldRef> root) {
		MultiTree<FieldRef> mTree = new MultiTree<FieldRef>(root.getVal());

		for (Node<FieldRef> node : root.getChildren()) {
			if (node.getVal() != null) {
				MultiTree<FieldRef> curTree = convert(node);
				mTree.addChild(curTree);
			}
		}

		return mTree;
	}

	/**
	 * Multiple InstanceKeys may refere to the same object field and have the
	 * same points-to set of pointerkeys. Albeit they are different Objects,
	 * they should be treated as identical during creation of the object tree.
	 * As our unfolding criterion clearly says that no field node should be
	 * inserted iff a node is belonging to the same field and having identical
	 * points-to sets.
	 */
	public static int createHashIK(Iterable<PointerKey> pks, FieldRef field) {
		int hash = (field == null ? 0 : field.field.hashCode());

		for (PointerKey pk : pks) {
			hash += pk.hashCode();
		}

		return hash;
	}

	/**
	 * Multiple PointerKeys may refere to the same object field and have the
	 * same points-to set of pointerkeys. Albeit they are different Objects,
	 * they should be treated as identical during creation of the object tree.
	 * As our unfolding criterion clearly says that no field node should be
	 * inserted iff a node is belonging to the same field and having identical
	 * points-to sets.
	 */
	public static int createHashPK(Iterable<InstanceKey> iks, FieldRef field) {
		int hash = (field == null ? 0 : field.field.hashCode());

		for (InstanceKey ik : iks) {
			hash += ik.hashCode();
		}

		return hash;
	}

	/**
	 * Searches for all object fields that may be referenced from the given
	 * instancekey.
	 * @param iKey element of the points-to set
	 * @param tree current position in the object tree
	 */
	private void buildTree(InstanceKey iKey, Node<FieldRef> tree) {
		Set<PointerKey> ikPointsTo = getPointsTo(iKey);

		int ikHash = createHashIK(ikPointsTo, tree.getVal());
		if (visitedIks.contains(ikHash)) {
			// skip already processed instance keys
			return;
		} else {
			visitedIks.add(ikHash);
		}

		/**
		 * steps through all locations that may point to the instancekey ikey
		 */
		for (PointerKey pk : ikPointsTo) {
			FieldRef objField = getFieldOfPointer(pk);
			if (objField == null) {
				continue;
			}

			int pkHash = createHashPK(pta.getPointsToSet(pk), objField);
			if (pkHash != 0 && !visitedPks.contains(pkHash)) {
				Node<FieldRef> current = tree.getChild(objField);
				if (current == null) {
					current = new Node<FieldRef>(objField);

					if (!tree.isRoot() && tree.getVal().isLeafField()) {
						throw new IllegalStateException("Can not add a child to a node that should be a leaf: " + tree.toString());
					}

					tree.addChild(current);
					current.addParent(tree);
				}

				buildTree(pk, current);
			}
		}
	}

	private Set<PointerKey> getPointsTo(InstanceKey ik) {
		Set<PointerKey> result = new HashSet<PointerKey>();

		for (Iterator<?> it = heap.getSuccNodes(ik); it.hasNext();) {
			Object obj = it.next();
			if (obj instanceof PointerKey) {
				result.add((PointerKey) obj);
			} else {
				Log.warn("Object pointed to by instancekey " + ik + " is not" +
					" a pointerkey: " + obj);
			}
		}

		return result;
	}

	/**
	 * Searches for all instancekeys that may point to the specified pointerkey
	 * and adds all objectfields that point to those instancekeys as new parents
	 * to the objecttree tree.
	 * @param pk pointerkey
	 * @param tree current position in the objecttree
	 * @param root root node of the objecttree
	 */
	private void buildTree(PointerKey pk, Node<FieldRef> tree) {
		OrdinalSet<InstanceKey> pts = pta.getPointsToSet(pk);
		int pkHash = createHashPK(pts, tree.getVal());
		visitedPks.add(pkHash);

		for (InstanceKey ik : pts) {
			buildTree(ik, tree);
		}
	}

	/**
	 * Computes all object fields that may be aliased to the specified
	 * pointerkey
	 * @param pk pointerkey
	 * @return set of may aliased object fields
	 */
	private FieldRef getFieldOfPointer(PointerKey pk) {
		FieldRef objField = null;

		if (pk instanceof AbstractFieldPointerKey) {
			if (pk instanceof InstanceFieldKey) {
				InstanceFieldKey fieldPk = (InstanceFieldKey) pk;
				ParameterField field = ParameterFieldFactory.getFactory().getObjectField(fieldPk.getField());

				if (field.isPrimitiveType()) {
					objField = new FieldRef(field);
				} else {
					OrdinalSet<InstanceKey> pts = pta.getPointsToSet(pk);
					if (pts != null) {
						// empty points-to sets are allowed as long as these nodes are leaf nodes
						objField = new FieldRef(field, pts);
					}
				}
			} else if (pk instanceof ArrayContentsKey) {
				ArrayContentsKey arrayContPk = (ArrayContentsKey) pk;
				InstanceKey ik = arrayContPk.getInstanceKey();
				IClass klass = ik.getConcreteType();
				if (klass != null) {
					TypeReference tRef = klass.getReference();
					ParameterField field = ParameterFieldFactory.getFactory().getArrayField(tRef);

					OrdinalSet<InstanceKey> pts = pta.getPointsToSet(pk);
					if (pts != null) {
						// empty points-to sets are allowed as long as these nodes are leaf nodes
						objField = new FieldRef(field, pts);
					}
				} else {
					Log.error("Array field has no contrete type: " + ik);
					throw new IllegalStateException();
				}
			}
		}

		return objField;
	}

}
