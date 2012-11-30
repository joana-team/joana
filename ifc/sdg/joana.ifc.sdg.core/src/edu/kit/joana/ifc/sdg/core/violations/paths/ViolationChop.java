/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.violations.paths;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.Violation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;


public class ViolationChop {
	private static final boolean DEBUG = false;
    private static ViolationChop instance = new ViolationChop();

    public static ViolationChop getInstance() {
        return instance;
    }


    private ChopWrapper chopper = new ChopWrapper(null);

	public List<Violation> addChop(Collection<Violation> violations, SDG g)
	throws NotInLatticeException {
		long viostart = System.currentTimeMillis();
		if (DEBUG) System.out.println("Started viopathgen at " + viostart + " for " + violations.size() + " violations");

		LinkedList<Violation> ret = new LinkedList<Violation>();

		//merge all violations into return list
		for (Violation sViolation : violations) {
            // Generate ViolationPathes and attach them to violation nodes
            ViolationPathes vps = generateChop(sViolation.getSink(), sViolation.getSource(), g);
			Violation vio = Violation.createViolation(sViolation.getSink(), sViolation.getSource(), vps, sViolation.getSink().getRequired());

			vio.setViolationPathes(vps);
			ret.add(vio);
		}

		long vioend = System.currentTimeMillis();
		if (DEBUG) System.out.println("Ended viopathgen at " + vioend + " duration: " + (vioend - viostart));
		return ret;
	}


	private ViolationPathes generateChop (SecurityNode outNode, SecurityNode violation, SDG g) {
	    chopper.setSDG(g);
		Collection<SecurityNode> set = chopper.chop(outNode, violation);
		LinkedList<SecurityNode> chopList = new LinkedList<SecurityNode>();

		for (SecurityNode n : set) {
			chopList.add(n);
		}

		ViolationPathes vps = new ViolationPathes();
		ViolationPath vp = new ViolationPath(chopList);

		vps.add(vp);

		return vps;
	}
}
