/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath.nodes;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.util.graph.INodeWithNumber;

import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.accesspath.AP;
import edu.kit.joana.wala.core.accesspath.AP.FieldNode;
import edu.kit.joana.wala.core.accesspath.AP.RootNode;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public abstract class APNode implements INodeWithNumber {

	public enum Type { NORM, NEW, FIELD_GET, FIELD_SET, CALL, ENTRY, PARAM_IN, PARAM_OUT, RETURN, EXC };
	public static final int UNKNOWN_IINDEX = -1; // used by entry node

	public final int iindex;
	private int number;
	public final APNode.Type type;
	public boolean changed = false;
	public final PDGNode node;
	private final Set<AP> paths = new HashSet<AP>();

	public APNode(final int iindex, final APNode.Type type, final PDGNode node) {
		this.iindex = iindex;
		this.type = type;
		this.node = node;
	}

	public abstract AP propagate(AP ap);

	/**
	 * Propagate all accesspaths of this node to the given node. Replace roots of accesspaths with the accesspaths
	 * matching the proper root node of the given node.
	 */
	public boolean propagateTo(final Map<RootNode, APParamNode> map, final APParamNode to) {
		boolean change = false;

		for (final AP ap : paths) {
			final AP.RootNode root = ap.getRoot();

			if (map.containsKey(root)) {
				// replace with all ap paths of apnode.
				final List<FieldNode> formOutPath = ap.getFieldPath();

				if (formOutPath.size() > 0) {
					final APNode aIn = map.get(root);
					final Iterator<AP> it = aIn.getOutgoingPaths();
					while (it.hasNext()) {
						final AP apAin = it.next();

						for (final FieldNode n : formOutPath) {
							final AP exp = apAin.expand(n);
							change |= to.addPath(exp);
						}
					}
				}
			} else {
				assert ap.getRoot().getType() != AP.NodeType.PARAM;
				assert ap.getRoot().getType() != AP.NodeType.RETURN;
				change |= to.addPath(ap);
			}
		}

		return change;
	}

	public boolean addPath(AP ap) {
		if (!paths.contains(ap)) {
			paths.add(ap);
			changed = true;
			return true;
		}

		return false;
	}

	public Iterator<AP> getOutgoingPaths() {
		return new Iterator<AP>() {

			private final Iterator<AP> it = new HashSet<AP>(paths).iterator();

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public AP next() {
				final AP origAP = it.next();
				final AP ap = propagate(origAP);

				return ap;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public final int getGraphNodeId() {
		return number;
	}

	public final void setGraphNodeId(final int number) {
		this.number = number;
	}

	@Override
	public final int hashCode() {
		return (node.hashCode() * 51 + iindex) * 23 + type.hashCode();
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof APNode) {
			final APNode other = (APNode) obj;

			return iindex == other.iindex && type == other.type && node.equals(other.node);
		}

		return false;
	}

	public final String toString() {
		final StringBuilder sb = new StringBuilder(type + "(" + iindex + "_" + node.getId() + ")");

		if (!paths.isEmpty()) {
			for (final AP p : paths) {
				sb.append("[");
				sb.append(p.toString());
				sb.append("]");
			}
		}

		return sb.toString();
	}

	public boolean sharesPathWith(final APNode n2) {
		if (paths.size() > n2.paths.size()) {
			for (final AP n2ap : n2.paths) {
				if (paths.contains(n2ap)) {
					return true;
				}
			}
		} else {
			for (final AP ap : paths) {
				if (n2.paths.contains(ap)) {
					return true;
				}
			}
		}

		return false;
	}
}
