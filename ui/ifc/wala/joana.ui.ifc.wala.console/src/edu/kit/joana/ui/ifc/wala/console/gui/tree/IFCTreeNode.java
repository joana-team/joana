/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.gui.tree;

import java.util.Collection;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.kit.joana.api.annotations.IFCAnnotation;
import edu.kit.joana.api.annotations.AnnotationType;
import edu.kit.joana.api.sdg.SDGProgramPart;




public abstract class IFCTreeNode extends DefaultMutableTreeNode implements Comparable<IFCTreeNode> {

	public enum Kind {
		ROOT, PACKAGE, CLASS, ATTRIBUTE, METHOD, PARAMETER, INSTRUCTION, EXIT, NONE;
	}

	protected static final int IFC_STR_POS = 60;
	private static final long serialVersionUID = -1152984674006297724L;
	private final boolean annotatable;
	private IFCAnnotation ifcAnnotation = null;
	private final Kind kind;


	public IFCTreeNode(Object userObject, boolean allowsChildren, boolean annotatable, Kind kind) {
		setUserObject(userObject);
		setAllowsChildren(allowsChildren);
		this.annotatable = annotatable;
		this.kind = kind;
	}

	public Kind getKind() {
		return kind;
	}

	public abstract String toStringPrefix();

	/**
	 * Returns whether this node can be annotated. Standard implementation
	 * returns false. Override this method, if you add an annotatable node type.
	 *
	 * @return false
	 */
	public boolean isAnnotateable() {
		return annotatable;
	}

	public boolean isSource() {
		if (isAnnotated()) {
			return getIFCAnnotation().getType() == AnnotationType.SOURCE;
		} else {
			return false;
		}
	}

	public boolean isAnnotated() {
		return (getIFCAnnotation() != null);
	}

	public final int countSource() {
		int count = (isSource() ? 1 : 0);

		for (int i = 0; i < getChildCount(); i++) {
			IFCTreeNode child = (IFCTreeNode) getChildAt(i);
			count += child.countSource();
		}

		return count;
	}

	public boolean isSink() {
		if (isAnnotated()) {
			return getIFCAnnotation().getType() == AnnotationType.SINK;
		} else {
			return false;
		}
	}

	public final int countSink() {
		int count = (isSink() ? 1 : 0);

		for (int i = 0; i < getChildCount(); i++) {
			IFCTreeNode child = (IFCTreeNode) getChildAt(i);
			count += child.countSink();
		}

		return count;
	}

	public boolean isDeclass() {
		if (isAnnotated()) {
			return getIFCAnnotation().getType() == AnnotationType.DECLASS;
		} else {
			return false;
		}
	}

	public final int countDeclass() {
		int count = (isDeclass() ? 1 : 0);

		for (int i = 0; i < getChildCount(); i++) {
			IFCTreeNode child = (IFCTreeNode) getChildAt(i);
			count += child.countDeclass();
		}

		return count;
	}

	private String annotationToString() {
		if (isAnnotated()) {
			switch (getIFCAnnotation().getType()) {
			case SOURCE:
				return toStringPrefix() + " (" + getIFCAnnotation().getLevel1() + " source)";
			case SINK:
				return toStringPrefix() + " (" + getIFCAnnotation().getLevel1() + " sink)";
			case DECLASS:
				return toStringPrefix() + " (declassifies " + getIFCAnnotation().getLevel1() + " to " + getIFCAnnotation().getLevel2() + ")";
			default:
				throw new IllegalStateException();
			}
		} else {
			return toStringPrefix();
		}
	}

	public String toString() {
		final StringBuffer sbuf = new StringBuffer(annotationToString());
		final int source = countSource();
		final int sink = countSink();
		final int declass = countDeclass();

		if (getAllowsChildren() && (source > 0 || sink > 0 || declass > 0)) {
			for (int i = sbuf.length(); i < IFC_STR_POS; i++) {
				sbuf.append(' ');
			}

			sbuf.append('[');

			if (source > 0) {
				sbuf.append("sources: " + source + " ");
			}

			if (sink > 0) {
				if (source > 0) {
					sbuf.append(", ");
				}
				sbuf.append("sinks: " + sink + " ");
			}

			if (declass > 0) {
				if (source > 0 || sink > 0) {
					sbuf.append(", ");
				}
				sbuf.append("declass: " + declass + " ");
			}

			sbuf.deleteCharAt(sbuf.length() - 1);

			sbuf.append("]");
		}

		return sbuf.toString();
	}



//	public final String toString() {
//		final StringBuffer sbuf = new StringBuffer(toStringPrefix());
//
//		for (int i = sbuf.length(); i < IFC_STR_POS; i++) {
//			sbuf.append(' ');
//		}
//
//		if (ifcAnnotation != null) {
//			sbuf.append('[');
//
//			if (isSource) {
//				sbuf.append("source: ");
//			} else if (isSink) {
//				sbuf.append("sink: ");
//			} else if (isDeclass) {
//				sbuf.append("declass: ");
//			}
//
//			sbuf.append(ifcAnnotation);
//			sbuf.append(']');
//		}
//
//		return sbuf.toString();
//	}


	public void setIFCAnnotation(IFCAnnotation ann) {
		ifcAnnotation = ann;
	}

	public IFCAnnotation getIFCAnnotation() {
		return ifcAnnotation;
	}

	protected abstract boolean matchesPart(SDGProgramPart part);

	public boolean annotate(Collection<IFCAnnotation> sources, Collection<IFCAnnotation> sinks, Collection<IFCAnnotation> declasses) {

		for (IFCAnnotation anno : sources) {
			if (matchesPart(anno.getProgramPart())) {
				setIFCAnnotation(anno);
				return true;
			}
		}

		for (IFCAnnotation anno : sinks) {
			if (matchesPart(anno.getProgramPart())) {
				setIFCAnnotation(anno);
				return true;
			}
		}

		for (IFCAnnotation anno : declasses) {
			if (matchesPart(anno.getProgramPart())) {
				setIFCAnnotation(anno);
				return true;
			}
		}

		setIFCAnnotation(null);
		return false;
	}

	@Override
	public int compareTo(IFCTreeNode o) {
		return toStringPrefix().compareTo(o.toStringPrefix());
	}
}
