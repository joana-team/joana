/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api;

import java.util.Collection;

import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class IllicitFlow extends JoanaViolation {
	
	private static final Logger debug = Log.getLogger(Log.L_API_DEBUG); 
	
	private final ClassifiedViolation vio;
	private final SDGProgramPart source;
	private final SDGProgramPart sink;
	private final String attackerLevel;

	public IllicitFlow(ClassifiedViolation vio, Collection<SDGProgramPart> pparts) {
		this.vio = vio;
		if (vio.getSource() != null) {
			this.source = selectProgramPart(vio.getSource(), pparts);
		} else {
			this.source = null; // TODO: fix this! IllicitFlow should not be used to also represent probabilistic conflicts!
		}
		this.sink = selectProgramPart(vio.getSink(), pparts);
		this.attackerLevel = vio.getAttackerLevel();


	}
	
	public SDGProgramPart getSource() {
		return source;
	}
	
	public SDGProgramPart getSink() {
		return sink;
	}

	public ClassifiedViolation getViolation() {
		return vio;
	}

	private SDGProgramPart selectProgramPart(SDGNode node, Collection<SDGProgramPart> pparts) {
		SDGProgramPart ret = null;
		for (SDGProgramPart ann : pparts) {
			if (ann.covers(node)) {
				ret = ann.getCoveringComponent(node);
				break;
			}
		}

		//assert ret != null : node;
		if (ret == null) {
			debug.outln("node " + node + " of kind " + node.getKind() + " has no covering program part!");
		}
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

	public static TObjectIntMap<IllicitFlow> groupByPParts(Collection<IllicitFlow> iflows) {
		TObjectIntMap<IllicitFlow> ret = new TObjectIntHashMap<IllicitFlow>();
		for (IllicitFlow ill : iflows) {
			if (ret.containsKey(ill)) {
				int noiFlows = ret.get(ill);
				ret.put(ill, noiFlows + 1);
			} else {
				ret.put(ill, 1);
			}
		}
		
		return ret;
	}
}

