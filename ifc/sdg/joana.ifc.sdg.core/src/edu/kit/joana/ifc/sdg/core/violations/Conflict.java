/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.violations;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.paths.ViolationPath;
import edu.kit.joana.ifc.sdg.core.violations.paths.ViolationPathes;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;

public class Conflict extends Violation {

	private SDGEdge confEdge;
	
    public Conflict(SecurityNode sink, SecurityNode source, String attacker) {
        setSink(sink);
        setSource(source);
        attackerLevel = attacker;

        // Generate ViolationPathes and attach them to violation nodes
        ViolationPath race = new ViolationPath();
        race.add(source);
        race.add(sink);
        ViolationPathes vps = new ViolationPathes();
        vps.add(race);
        addChop(new Chop("Standard"));
        setViolationPathes(vps);
    }

    public void addConflict(SDGEdge edge) {
        ViolationPath race = new ViolationPath();
        race.add((SecurityNode)edge.getSource());
        race.add((SecurityNode)edge.getTarget());
        getChop("Standard").getViolationPathes().add(race);
    }
    
    public void setConflictEdge(SDGEdge confEdge) {
    	this.confEdge = confEdge;
    }
    
    public SDGEdge getConflictEdge() {
    	return confEdge;
    }

    public String toString() {
        return "Probabilistic Data Channel from Node "+getSource()+" to Node "+getSink()+", visible for "+attackerLevel;
    }
}
