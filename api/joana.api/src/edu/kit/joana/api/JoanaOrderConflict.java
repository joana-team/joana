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
import edu.kit.joana.ifc.sdg.core.violations.OrderConflict;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * @author Martin Mohr
 */
public class JoanaOrderConflict extends JoanaConflict {

	private final OrderConflict oc;
	private final SDGProgramPart secret;
	private final SDGNode nodeSecret;
	private final SDGProgramPart conf1;
	private final SDGNode nodeConf1;
	private final SDGProgramPart conf2;
	private final SDGNode nodeConf2;
	
	/**
	 * @param program
	 * @param c
	 */
	public JoanaOrderConflict(SDGProgram program, Conflict c) {
		if (!(c instanceof OrderConflict)) {
			throw new IllegalArgumentException("This class is to be used with OrderConflicts only!");
		}
		this.oc = (OrderConflict) c;
		this.secret = program.findCoveringProgramPart(oc.getSource());
		this.nodeSecret = oc.getSource();
		this.conf1 = program.findCoveringProgramPart(oc.getSink());
		this.nodeConf1 = oc.getSink();
		this.conf2 = program.findCoveringProgramPart(oc.getConflicting());
		this.nodeConf2 = oc.getConflicting();
	}
	
	public String toString() {
		return String.format("%s [%s] is leaked through an order conflict between %s [%s] and %s [%s], visible for %s (order conflict edge is %s <--> %s)", secret, nodeSecret, conf1, nodeConf1, conf2, nodeConf2, oc.getAttackerLevel(), oc.getConflictEdge().getSource().getId(), oc.getConflictEdge().getTarget().getId());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((conf1 == null) ? 0 : conf1.hashCode());
		result = prime * result + ((conf2 == null) ? 0 : conf2.hashCode());
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
		JoanaOrderConflict other = (JoanaOrderConflict) obj;
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
		if (secret == null) {
			if (other.secret != null)
				return false;
		} else if (!secret.equals(other.secret))
			return false;
		return true;
	}

}
