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

public class OrderConflict extends ClassifiedConflict {
    private SecurityNode conflicting;

    public OrderConflict(SecurityNode sink, SecurityNode source, SecurityNode conflicting, String attacker) {
        super(sink, source, attacker);
        this.conflicting = conflicting;

        // Generate ViolationPathes and attach them to violation nodes
        ViolationPath race = new ViolationPath();
        race.add(source);
        race.add(conflicting);
        race.add(sink);
        ViolationPathes vps = new ViolationPathes();
        vps.add(race);
        setViolationPathes(vps);
    }

    public SecurityNode getConflicting() {
    	return conflicting;
    }

    public String toString() {
        return "Probabilistic Order Channel between Nodes "+getSink()+
        		" and "+conflicting+", leaking Node "+getSource()+
        		", visible for "+attackerLevel;
    }

    public int hashCode() {
        return (sink.hashCode() + conflicting.hashCode()) * 31 + source.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof OrderConflict) {
            OrderConflict c = (OrderConflict) o;

            if (c.source == source
            		&& ((c.sink == sink && c.conflicting == conflicting)
            				|| (c.sink == conflicting && c.conflicting == sink))) { // symmetrie

                return true;
            }
        }

        return false;
    }
}
