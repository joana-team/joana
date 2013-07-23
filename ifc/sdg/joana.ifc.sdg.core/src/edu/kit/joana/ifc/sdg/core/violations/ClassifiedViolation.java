/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on 21.12.2004
 *
 */
package edu.kit.joana.ifc.sdg.core.violations;

import java.util.Collection;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.metrics.IMetrics;
import edu.kit.joana.ifc.sdg.core.violations.paths.ViolationPath;
import edu.kit.joana.ifc.sdg.core.violations.paths.ViolationPathes;


/**
 * Represents a Violation including all paths between involved nodes
 * holds an object ViolationPathes that actually holds the paths
 *
 * @author naxan
 *
 */
public class ClassifiedViolation implements IIllegalFlow {
	public static class Classification {
		// 0 = highest, MAX_INT = lowest
		private int severity;
		private IMetrics.Rating rating;
		private String description;
		private String name;

		public Classification(String name, String description, int severity, IMetrics.Rating rating) {
			this.name = name;
			this.severity = severity;
			this.description = description;
			this.rating = rating;
		}

		public int getSeverity() {
			return severity;
		}

		public IMetrics.Rating getRating() {
			return rating;
		}

		public String getDescription() {
			return description;
		}

		public String getName() {
			return name;
		}
	}

	public static class Chop {

		private String name;
		private ViolationPathes violationPathes = new ViolationPathes();


		public Chop(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public ViolationPathes getViolationPathes() {
			return violationPathes;
		}

		public void setViolationPathes(ViolationPathes vioPathes) {
			violationPathes = vioPathes;
		}
	}

	protected SecurityNode sink;
	protected SecurityNode source;
	protected String attackerLevel;
	//ViolationPathes violationPathes = new ViolationPathes();
	protected Collection<Classification> classifications;
	protected Collection<Chop> chops = new LinkedList<Chop>();

	protected ClassifiedViolation() {
		classifications = new LinkedList<Classification>();
	}

	public void setSink(SecurityNode sink) {
		this.sink = sink;
	}

	public void setSource(SecurityNode source) {
		this.source = source;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow#getSink()
	 */
	@Override
	public SecurityNode getSink() {
		return sink;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow#getSource()
	 */
	@Override
	public SecurityNode getSource() {
		return source;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow#getAttackerLevel()
	 */
	@Override
	public String getAttackerLevel() {
		return attackerLevel;
	}

	public void addChop(Chop chop) {
		chops.add(chop);
	}

	public Collection<Chop> getChops() {
		return chops;
	}

	public Chop getChop(String name) {
		for (Chop c : chops) {
			if (c.getName().equals(name)) {
				return c;
			}
		}
		return null;
	}

	public void setViolationPathes(ViolationPathes violations) {
		getChop("Standard").setViolationPathes(violations);
	}

	/**
	 * @return  Returns the violationPathes.
	 */
	public ViolationPathes getViolationPathes() {
		return getChop("Standard").getViolationPathes();
	}

	public LinkedList<SecurityNode> getAllInvolvedNodes() {
		LinkedList<SecurityNode> ret = new LinkedList<SecurityNode>();
		for (Chop c : chops) {
			ret.addAll(c.getViolationPathes().getAllInvolvedNodes());
		}
		return ret;
	}

	public String toString() {
	    return "Illicit Flow From SDGNode " + source + " At SDGNode " + sink +", visible for "+attackerLevel;
	}

	public int hashCode() {
	    return source.getId() | (sink.getId() << 16);
	}

	public boolean equals(Object o) {
	    if (o instanceof ClassifiedViolation) {
	        ClassifiedViolation v = ((ClassifiedViolation) o);

	        if (v.sink == sink && v.source == source) {
	            return true;
	        }
        }

	    return false;
    }


	/* classifications */

//	public void setClassifications(Collection<Classification> c) {
//		classifications = c;
//	}

	public Collection<Classification> getClassifications() {
		return classifications;
	}

	public void addClassification(String name, String msg, int severity, IMetrics.Rating rating) {
		Classification c = new Classification(name, msg, severity, rating);
		classifications.add(c);
	}

	public int getHighestSeverity() {
		int i = Integer.MAX_VALUE;

		for (Classification c : classifications) {
			if (c.severity < i) {
				i = c.severity;
			}
		}

		return i;
	}

	/* Factories */

	/**
	 * Einfache Violation fuer ein einzelnes Leck anlegen.
	 */
    public static ClassifiedViolation createViolation(SecurityNode leak, SecurityNode informationSource, String attacker) {
    	if (attacker == null) throw new RuntimeException();
        ClassifiedViolation vio = new ClassifiedViolation();

        vio.setSink(leak);
        vio.setSource(informationSource);
        ViolationPathes vps = new ViolationPathes();
        ViolationPath vpath = new ViolationPath();
        vpath.add(informationSource);
        vpath.add(leak);
        vps.add(vpath);
        vio.addChop(new Chop("Standard"));
        vio.setViolationPathes(vps);
        vio.attackerLevel = attacker;

        return vio;
    }

    /** Violation erzeugen
     *
     * @param leak
     * @param informationSources
     * @param kind
     * @param paths
     * @return
     */
    public static ClassifiedViolation createViolation(SecurityNode leak, SecurityNode informationSource, ViolationPathes paths, String attacker) {
    	if (attacker == null) throw new RuntimeException();
    	ClassifiedViolation vio = new ClassifiedViolation();

        vio.setSink(leak);
        vio.setSource(informationSource);
        vio.addChop(new Chop("Standard"));
        vio.setViolationPathes(paths);
        vio.attackerLevel = attacker;

        return vio;
    }
}
