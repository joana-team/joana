/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath;

import java.util.LinkedList;
import java.util.List;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;

import edu.kit.joana.wala.core.ParameterField;

/**
 * Models a single access path from a root node to an end field node. The path consists of node objects that can be
 * shared among multiple AP paths to save memory.
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class AP {

	public enum NodeType { FIELD, PARAM, NEW, RETURN };

	private final RootNode root;
	private final Node end;

	public AP(final RootNode root) {
		this(root, root);
	}

	private AP(final RootNode root, final Node end) {
		this.root = root;
		this.end = end;
	}

	public RootNode getRoot() {
		return root;
	}

	public AP append(final ParameterField f) {
		final FieldNode fn = new FieldNode(end, f);

		return new AP(root, fn);
	}

	public Node getEnd() { 
		return end;
	}
	
	public AP getParentPath() {
		final Node parent = end.getParent();
		
		return (parent == null ? null : new AP(root, parent));
	}
	
	public AP expand(final FieldNode fn) {
		final AP sub = getSubPathTo(fn.field);

		if (sub != null) {
			return sub;
		}

		return append(fn.field);
	}
	
	public AP expand(final List<FieldNode> fn) {
		AP cur = this;
		
		for (final FieldNode f : fn) {
			cur = cur.expand(f);
		}
		
		return cur;
	}

	public List<Node> getPath() {
		final LinkedList<Node> path = new LinkedList<Node>();

		for (Node cur = end; cur != null; cur = cur.getParent()) {
			path.addFirst(cur);
		}

		return path;
	}

	public List<FieldNode> getFieldPath() {
		final LinkedList<FieldNode> path = new LinkedList<FieldNode>();

		for (Node cur = end; cur != null; cur = cur.getParent()) {
			if (cur instanceof FieldNode) {
				path.addFirst((FieldNode) cur);
			}
		}

		return path;
	}

	/**
	 * Searches for the first node of this access path that matches the given parameter field. Starting from
	 * the end of the path and going to root. If a matching node n is found a subpath from root to n is returned.
	 * If no matching node is found, nothing is returned.
	 * @param field
	 * @return subpath to the given field or null
	 */
	public AP getSubPathTo(final ParameterField field) {
		Node cur = end;

		if (end.matches(field)) {
			return this;
		}

		while (cur != null) {
			if (cur.matches(field)) {
				return new AP(root, cur);
			}

			cur = cur.getParent();
		}

		return null;
	}

	public List<Node> getSubPathTo(final Node end) {
		assert contains(end);

		final LinkedList<Node> path = new LinkedList<Node>();

		for (Node cur = end; cur != null; cur = cur.getParent()) {
			path.addFirst(cur);
		}

		return path;
	}

	public boolean contains(final Node n) {
		for (Node cur = end; cur != null; cur = cur.getParent()) {
			if (cur.equals(n)) {
				return true;
			}
		}

		return false;
	}

	public int hashCode() {
		return (root.hashCode() << 3) + end.hashCode();
	}

	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof AP) {
			final AP other = (AP) obj;

			return root.equals(other.root) && end.equals(other.end);
		}

		return false;
	}

	public String toString() {
		final StringBuilder sb = new StringBuilder();

		for (final Node n : getPath()) {
			sb.append(n.toString() + ".");
		}

		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}

		return sb.toString();
	}

	public static abstract class Node {

		public abstract NodeType getType();
		public abstract Node getParent();
		public abstract int hashCode();
		public abstract boolean equals(Object obj);
		public abstract boolean matches(ParameterField f);
		public abstract String toString();

	}

	public static abstract class RootNode extends Node {

		public final int pdgNodeId;

		public RootNode(final int pdgNodeId) {
			this.pdgNodeId = pdgNodeId;
		}

		public final Node getParent() {
			return null;
		}

		public boolean matches(final ParameterField f) {
			return false;
		}
	}

	public static abstract class ParamNode extends RootNode {

		public ParamNode(final int pdgNodeId) {
			super(pdgNodeId);
		}

		@Override
		public NodeType getType() {
			return NodeType.PARAM;
		}

	}

	public static final class MethodParamNode extends ParamNode {

		private final int cgNodeId;
		private final int paramNum;

		public MethodParamNode(final CGNode method, final int paramNum, final int pdgNodeId) {
			super(pdgNodeId);
			this.cgNodeId = method.getGraphNodeId();
			this.paramNum = paramNum;
		}

		@Override
		public int hashCode() {
			return (cgNodeId << 5) | paramNum;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}

			if (obj instanceof MethodParamNode) {
				final MethodParamNode other = (MethodParamNode) obj;

				return cgNodeId == other.cgNodeId && paramNum == other.paramNum;
			}

			return false;
		}

		@Override
		public String toString() {
			return "<p" + paramNum + "@" + cgNodeId + ">";
		}

	}

	public static final class StaticParamNode extends ParamNode {

		public final IField field;

		public StaticParamNode(final IField field, final int pdgNodeId) {
			super(pdgNodeId);

			if (field == null || !field.isStatic() || field.getFieldTypeReference().isPrimitiveType()) {
				throw new IllegalArgumentException();
			}

			this.field = field;
		}

		@Override
		public int hashCode() {
			return field.hashCode();
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}

			if (obj instanceof StaticParamNode) {
				final StaticParamNode other = (StaticParamNode) obj;

				return field.equals(other.field);
			}

			return false;
		}

		public boolean matches(final ParameterField f) {
			return f.isStatic() && field.equals(f.getField());
		}

		@Override
		public String toString() {
			return "<" + field.getName() + ">";
		}

	}

	public static final class NewNode extends RootNode {

		private final int cgNodeId;
		private final NewSiteReference nsr;

		public NewNode(final CGNode n, final NewSiteReference nsr, final int pdgNodeId) {
			super(pdgNodeId);
			this.cgNodeId = n.getGraphNodeId();
			this.nsr = nsr;
		}

		@Override
		public NodeType getType() {
			return NodeType.NEW;
		}

		@Override
		public int hashCode() {
			return (cgNodeId << 4) ^ nsr.hashCode();
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}

			if (obj instanceof NewNode) {
				final NewNode other = (NewNode) obj;

				return cgNodeId == other.cgNodeId && nsr.equals(other.nsr);
			}

			return false;
		}

		@Override
		public String toString() {
			return "<new@" + cgNodeId + ":" + nsr.getProgramCounter() + ">";
		}

	}

	public static final class ReturnNode extends RootNode {

		private final int cgNodeId;
		private final CallSiteReference csr;

		public ReturnNode(final CGNode method, final CallSiteReference csr, final int pdgNodeId) {
			super(pdgNodeId);
			this.cgNodeId = method.getGraphNodeId();
			this.csr = csr;
		}

		@Override
		public NodeType getType() {
			return NodeType.RETURN;
		}

		@Override
		public int hashCode() {
			return (cgNodeId << 4) ^ csr.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}

			if (obj instanceof ReturnNode) {
				final ReturnNode other = (ReturnNode) obj;

				return cgNodeId == other.cgNodeId && csr.equals(other.csr);
			}
			return false;
		}

		@Override
		public String toString() {
			return "<ret@" + cgNodeId + ">";
		}

	}

	public static final class FieldNode extends Node {

		private final Node parent;
		private final ParameterField field;

		public FieldNode(final Node parent, final ParameterField field) {
			if (parent == null) {
				throw new IllegalArgumentException();
			} else if (field == null) {
				throw new IllegalArgumentException();
			}

			this.parent = parent;
			this.field = field;
		}

		@Override
		public NodeType getType() {
			return NodeType.FIELD;
		}

		@Override
		public int hashCode() {
			return field.hashCode() << 2;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}

			if (obj instanceof FieldNode) {
				final FieldNode other = (FieldNode) obj;

				return field.equals(other.field) && parent.equals(other.parent);
			}

			return false;
		}

		@Override
		public Node getParent() {
			return parent;
		}

		@Override
		public boolean matches(final ParameterField f) {
			return field.equals(f);
		}

		@Override
		public String toString() {
			return field.toString();
		}

	}

}
