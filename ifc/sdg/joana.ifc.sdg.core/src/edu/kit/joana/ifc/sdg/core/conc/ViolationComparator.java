/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 *
 */
package edu.kit.joana.ifc.sdg.core.conc;

import java.util.Comparator;

import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;


/**
 * Ein Comparator fuer Violations, die nur aus zwei Knoten bestehen.
 * Violation.out stellt das Leck dar, der einzige Knoten in Violation.violating
 * die Quelle, die geleckt wird.
 *
 * @author giffhorn
 */
public class ViolationComparator implements Comparator<ClassifiedViolation> {
    /**
     * Eine Instanz des Komparators
     */
    public static final ViolationComparator COMP = new ViolationComparator();

    /**
     * Die hier gebildeten Violations bestehen immer nur aus 2 Knoten.
     * Daher vergleicht die Methode paarweise die IDs dieser beiden Knoten,
     * um die beiden Violations miteinander zu vergleichen.
     */
    public int compare(ClassifiedViolation a, ClassifiedViolation b) {
        if (a.getSource().getId() > b.getSource().getId()) {
            return 1;

        } else if (a.getSource().getId() < b.getSource().getId()) {
            return -1;
        }

        if (a.getSink().getId() > b.getSink().getId()) {
            return 1;

        } else if (a.getSink().getId() < b.getSink().getId()) {
            return -1;
        }

        return 0;
    }
}
