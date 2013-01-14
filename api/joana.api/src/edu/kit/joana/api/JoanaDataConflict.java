/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api;

import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.ifc.sdg.core.violations.Conflict;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * @author Martin Mohr
 */
public class JoanaDataConflict extends JoanaConflict {

	private final Conflict c;
	private final SDGProgramPart secret;
	private final SDGNode nodeSecret;
	private final SDGProgramPart leaking;
	private final SDGNode nodeLeaking;
	private final SDGProgramPart conf1;
	private final SDGNode nodeConf1;
	private final SDGProgramPart conf2;
	private final SDGNode nodeConf2;

	public JoanaDataConflict(SDGProgram p, Conflict c) {
		this.c = c;
		this.secret = p.findCoveringProgramPart(c.getSource());
		this.nodeSecret = c.getSource();
		this.leaking = p.findCoveringProgramPart(c.getSink());
		this.nodeLeaking = c.getSink();
		SDGEdge confEdge = c.getConflictEdge();
		this.conf1 = p.findCoveringProgramPart(confEdge.getSource());
		this.nodeConf1 = confEdge.getSource();
		this.conf2 = p.findCoveringProgramPart(confEdge.getTarget());
		this.nodeConf2 = confEdge.getTarget();
	}
	
	public String toString() {
		return String.format("%s [%d] influences a data conflict between %s [%s] and %s [%s] which may influence %s [%s], visible for %s (data conflict edge is %s)", secret, nodeSecret.getId(), conf1, nodeConf1.getId(), conf2, nodeConf2.getId(), leaking, nodeLeaking, c.getAttackerLevel(), c.getConflictEdge());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((conf1 == null) ? 0 : conf1.hashCode());
		result = prime * result + ((conf2 == null) ? 0 : conf2.hashCode());
		result = prime * result + ((leaking == null) ? 0 : leaking.hashCode());
		result = prime * result + ((secret == null) ? 0 : secret.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JoanaDataConflict other = (JoanaDataConflict) obj;
		if (conf1 == null) {
			if (other.conf1 != null)
				return false;
		} else if (!conf1.equals(other.conf1))
			return false;
		if (conf2 == null) {
			if (other.conf2 != null)
				return false;
		} else if (!conf2.equals(other.conf2))
			return false;
		if (leaking == null) {
			if (other.leaking != null)
				return false;
		} else if (!leaking.equals(other.leaking))
			return false;
		if (secret == null) {
			if (other.secret != null)
				return false;
		} else if (!secret.equals(other.secret))
			return false;
		return true;
	}
}
