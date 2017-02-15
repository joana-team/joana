/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.annotations;

import edu.kit.joana.ifc.sdg.core.SecurityNode;

public class NodeAnnotationInfo {
	private final SecurityNode node;
	private final IFCAnnotation annotation;
	private final String which;

	public static final String PROV = "provided";
	public static final String REQ = "required";
	public static final String BOTH = "both";

	public NodeAnnotationInfo(SecurityNode node, IFCAnnotation annotation, String which) {
		this.node = node;
		this.annotation = annotation;
		this.which = which;
	}

	/**
	 * @return the node
	 */
	public SecurityNode getNode() {
		return node;
	}

	/**
	 * @return the annotation
	 */
	public IFCAnnotation getAnnotation() {
		return annotation;
	}

	/**
	 * @return the which
	 */
	public String getWhich() {
		return which;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(node);
		sb.append(": ");
		sb.append(annotation);
		sb.append("(" + which + ")");
		return sb.toString();
	}
	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((annotation == null) ? 0 : annotation.hashCode());
		result = prime * result + ((node == null) ? 0 : node.hashCode());
		result = prime * result + ((which == null) ? 0 : which.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof NodeAnnotationInfo)) {
			return false;
		}
		NodeAnnotationInfo other = (NodeAnnotationInfo) obj;
		if (annotation == null) {
			if (other.annotation != null) {
				return false;
			}
		} else if (!annotation.equals(other.annotation)) {
			return false;
		}
		if (node == null) {
			if (other.node != null) {
				return false;
			}
		} else if (!node.equals(other.node)) {
			return false;
		}
		if (which == null) {
			if (other.which != null) {
				return false;
			}
		} else if (!which.equals(other.which)) {
			return false;
		}
		return true;
	}
}