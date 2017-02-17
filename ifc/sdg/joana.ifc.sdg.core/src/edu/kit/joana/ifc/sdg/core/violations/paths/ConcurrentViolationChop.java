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
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.chopper.conc.ContextSensitiveThreadChopper;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;


public class ConcurrentViolationChop {
	
	private final Logger debug = Log.getLogger(Log.L_SDG_INTERFERENCE_DEBUG);
	
    private static ConcurrentViolationChop instance = new ConcurrentViolationChop();

    public static ConcurrentViolationChop getInstance() {
        return instance;
    }

    private ContextSensitiveThreadChopper chopper;

	public List<ClassifiedViolation> addChop(Collection<ClassifiedViolation> violations, SDG g)
	throws NotInLatticeException {
	    chopper = new ContextSensitiveThreadChopper(g);

		long viostart = System.currentTimeMillis();
		debug.outln("Started viopathgen at " + viostart + " for " + violations.size() + " violations");


		LinkedList<ClassifiedViolation> ret = new LinkedList<ClassifiedViolation>();

		//merge all violations into return list
		for (ClassifiedViolation sViolation : violations) {
            // Generate ViolationPathes and attach them to violation nodes
            ViolationPathes vps = generateChop(sViolation.getSink(), sViolation.getSource());
			ClassifiedViolation vio = ClassifiedViolation.createViolation(sViolation.getSink(), sViolation.getSource(), vps, sViolation.getSink().getRequired());
			ret.add(vio);
		}

		long vioend = System.currentTimeMillis();
		debug.outln("Ended viopathgen at " + vioend + " duration: " + (vioend - viostart));
		
		return ret;
	}


	private ViolationPathes generateChop (SecurityNode outNode, SecurityNode violation) {
		Collection<SecurityNode> set = chop(outNode, violation);
		LinkedList<SecurityNode> chopList = new LinkedList<SecurityNode>();

		for (SecurityNode n : set) {
			chopList.add(n);
		}

		ViolationPathes vps = new ViolationPathes();
		ViolationPath vp = new ViolationPath(chopList);

		vps.add(vp);

		return vps;
	}

	public void initChopper(SDG g) {
		chopper = new ContextSensitiveThreadChopper(g);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
    public Collection<SecurityNode> chop (SecurityNode outNode, SecurityNode violation) {
            return (Collection) chopper.chop(violation, outNode);
    }
}
