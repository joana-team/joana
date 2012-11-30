/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda;

import java.util.Comparator;

/**
 * Represents a topological number.
 * Contains the topological number, a reference to the belonging
 * ISCR and the ID of its thread.
 *
 * -- Created on March 9, 2006
 *
 * @author  Dennis Giffhorn
 */
public class TopologicalNumber {
	private static class TopNrComparator implements Comparator<TopologicalNumber> {
		public int compare(TopologicalNumber o1, TopologicalNumber o2) {
			return o1.getNumber() - o2.getNumber();
		}
	}

	public static final TopologicalNumber NONRESTRICTIVE = new TopologicalNumber();
	public static final TopologicalNumber NONE = new TopologicalNumber();

	private static TopNrComparator tnrComp;
	private static int ctr = 0;

	public static TopNrComparator getComparator() {
		if (tnrComp == null) {
			tnrComp = new TopNrComparator();
		}

		return tnrComp;
	}

    /** The topological number. */
    private int number;
    /** The ID of the enclosing procedure. */
    private int proc;
    private int hash;

    /**
     * Creates a new instance of TopologicalNumber
     */
    public TopologicalNumber() {//int t) {
    	number = -1;
    	proc = -1;
//        threads = t;
        hash = ctr;
        ctr++;
    }

    /**
     * Returns the topological number.
     */
    public int getNumber() {
        return number;
    }

    public void setNumber(int nr) {
    	number = nr;
    }

    public int getProcID() {
        return proc;
    }

    public void setProcID(int id) {
    	proc = id;
    }

    /**
     * Returns a String representation.
     */
    public String toString() {
        return "(" + number + ", " + proc + /*", " + thread +*/ ")";
    }

    public int hashCode() {
    	return hash;
    }
}
