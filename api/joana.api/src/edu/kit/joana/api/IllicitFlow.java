/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import edu.kit.joana.api.annotations.IFCAnnotation;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.ifc.sdg.core.violations.Violation;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;

public class IllicitFlow {
	
	private static final Logger debug = Log.getLogger(Log.L_API_DEBUG); 
	
	private final Violation vio;
	private final SDGProgramPart source;
	private final SDGProgramPart sink;
	private final String attackerLevel;

	public IllicitFlow(Violation vio, Collection<IFCAnnotation> sources, Collection<IFCAnnotation> sinks) {
		this.vio = vio;
		Set<IFCAnnotation> anns = new HashSet<IFCAnnotation>();
		anns.addAll(sources);
		anns.addAll(sinks);
		this.source = selectProgramPart(vio.getSource(), sources);
		this.sink = selectProgramPart(vio.getSink(), sinks);
		this.attackerLevel = vio.getAttackerLevel();


	}

	public Violation getViolation() {
		return vio;
	}

	private SDGProgramPart selectProgramPart(SDGNode node, Collection<IFCAnnotation> annots) {
		SDGProgramPart ret = null;
		for (IFCAnnotation ann : annots) {
			SDGProgramPart part = ann.getProgramPart();
			if (part.covers(node)) {
				ret = part.getCoveringComponent(node);
				break;
			}
		}

		//assert ret != null : node;
		return ret;

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
		result = prime * result
				+ ((attackerLevel == null) ? 0 : attackerLevel.hashCode());
		result = prime * result + ((sink == null) ? 0 : sink.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
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
		if (!(obj instanceof IllicitFlow)) {
			return false;
		}
		IllicitFlow other = (IllicitFlow) obj;
		if (attackerLevel == null) {
			if (other.attackerLevel != null) {
				return false;
			}
		} else if (!attackerLevel.equals(other.attackerLevel)) {
			return false;
		}
		if (sink == null) {
			if (other.sink != null) {
				return false;
			}
		} else if (!sink.equals(other.sink)) {
			return false;
		}
		if (source == null) {
			if (other.source != null) {
				return false;
			}
		} else if (!source.equals(other.source)) {
			return false;
		}
		return true;
	}

	public String toString() {
		if (debug.isEnabled()) {
			return "Illicit Flow from " + source + "[" + vio.getSource() + "] to " + sink + "[" + vio.getSink() + "], visible for "
					+ attackerLevel;
		} else {
			return "Illicit Flow from " + source + " to " + sink + ", visible for "
			+ attackerLevel;
		}
	}
}

