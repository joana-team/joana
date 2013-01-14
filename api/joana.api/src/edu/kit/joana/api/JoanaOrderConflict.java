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
		this.oc = (OrderConflict) c;
		this.secret = program.findCoveringProgramPart(oc.getSource());
		this.nodeSecret = oc.getSource();
		this.conf1 = program.findCoveringProgramPart(oc.getSink());
		this.nodeConf1 = oc.getSink();
		this.conf2 = program.findCoveringProgramPart(oc.getConflicting());
		this.nodeConf2 = oc.getConflicting();
	}
	
	public String toString() {
		return String.format("%s [%s] is leaked through an order conflict between %s [%s] and %s [%s] (order conflict edge is %s <--> )", secret, nodeSecret, conf1, nodeConf1, conf2, nodeConf2, oc.getAttackerLevel(), oc.getConflictEdge().getSource().getId(), oc.getConflictEdge().getTarget().getId());
	}

}
