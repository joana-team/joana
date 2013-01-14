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
		return String.format("%s [%d] causes a data conflict between %s [%s] and %s [%s] which may influence %s [%s], visible for %s", secret, nodeSecret.getId(), conf1, nodeConf1.getId(), conf2, nodeConf2.getId(), leaking, nodeLeaking, c.getAttackerLevel());
	}
}
