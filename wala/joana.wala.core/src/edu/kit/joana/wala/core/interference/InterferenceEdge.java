/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.interference;

import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.PDGEdge.Kind;

public class InterferenceEdge {

	private final PDG pdgWrite;
	private final PDGNode from;
	private final PDGNode to;
	private final PDGEdge.Kind kind;

	private InterferenceEdge(PDG pdgWrite, PDGNode from, PDGNode to, PDGEdge.Kind kind) {
		assert kind == Kind.INTERFERENCE || kind == Kind.INTERFERENCE_WRITE;
		this.pdgWrite = pdgWrite;
		this.from = from;
		this.to = to;
		this.kind = kind;
	}

	public PDG getPDGWrite() {
		return pdgWrite;
	}

	public PDGNode getFrom() {
		return from;
	}

	public PDGNode getTo() {
		return to;
	}

	public void addToPDG() {
		PDGNode node1 = getFrom();
		PDG pdgWrite = getPDGWrite();
		PDGNode node2 = getTo();

		if (!pdgWrite.containsVertex(node1)) {
			pdgWrite.addVertex(node1);
		}

		if (!pdgWrite.containsVertex(node2)) {
			pdgWrite.addVertex(node2);
		}
		pdgWrite.addEdge(node1, node2, kind);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + ((kind == null) ? 0 : kind.hashCode());
		result = prime * result + ((pdgWrite == null) ? 0 : pdgWrite.hashCode());
		result = prime * result + ((to == null) ? 0 : to.hashCode());
		return result;
	}

	/* (non-Javadoc)
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
		if (!(obj instanceof InterferenceEdge)) {
			return false;
		}
		InterferenceEdge other = (InterferenceEdge) obj;
		if (from == null) {
			if (other.from != null) {
				return false;
			}
		} else if (!from.equals(other.from)) {
			return false;
		}
		if (kind != other.kind) {
			return false;
		}
		if (pdgWrite == null) {
			if (other.pdgWrite != null) {
				return false;
			}
		} else if (!pdgWrite.equals(other.pdgWrite)) {
			return false;
		}
		if (to == null) {
			if (other.to != null) {
				return false;
			}
		} else if (!to.equals(other.to)) {
			return false;
		}
		return true;
	}

	public static InterferenceEdge createReadWriteInterferenceEdge(PDG pdgWrite2, PDGNode ewrite, PDGNode eread) {
		return new InterferenceEdge(pdgWrite2, ewrite, eread, PDGEdge.Kind.INTERFERENCE);
	}

	public static InterferenceEdge createWriteWriteInterferenceEdge(PDG pdgWrite1, PDGNode ewrite1, PDGNode ewrite2) {
		return new InterferenceEdge(pdgWrite1, ewrite1, ewrite2, PDGEdge.Kind.INTERFERENCE_WRITE);
	}
}
