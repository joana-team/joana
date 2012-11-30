/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.libraries.old;
//package edu.kit.joana.ifc.sdg.core.libraries.old;
//
//import java.util.Collection;
//import java.util.LinkedList;
//import java.util.List;
//
//import edu.kit.joana.ifc.sdg.core.SecurityNode;
//import edu.kit.joana.ifc.sdg.core.violations.Violation;
//import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
//
///**
// * Zum Einbinden einer neuen Bibliothek; laufen von den FI-Knoten zu den FO-Knoten.
// *
// * @author giffhorn
// *
// */
//public class VerifyCondition {
//    protected SecurityNode fi;
//    protected Collection<SecurityNode> fos;
//
//	public VerifyCondition(SecurityNode fi, Collection<SecurityNode> fos) {
//        this.fi = fi;
//        this.fos = fos;
//    }
//
//	public List<Violation> check(IStaticLattice<String> lattice) {
//	    List<Violation> l = new LinkedList<Violation>();
//
//	    // check that P(f) <= R(fo) for all f in fos
//        for (SecurityNode f : fos) {
//            if (!lattice.leastUpperBound(fi.getProvided(), f.getRequired()).equals(f.getRequired())) {
//                Violation v = Violation.createViolation(fi, f, fi.getProvided());
//                l.add(v);
//            }
//        }
//
//        return l;
//	}
//
//	public String toString() {
//	    return "lvl("+fi+") = inf("+fos+")";
//	}
//}
