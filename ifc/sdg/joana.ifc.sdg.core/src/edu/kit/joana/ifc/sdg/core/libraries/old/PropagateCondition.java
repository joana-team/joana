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
//import java.util.HashSet;
//
//import edu.kit.joana.ifc.sdg.core.SecurityNode;
//import edu.kit.joana.ifc.sdg.core.conc.Element;
//
///**
// * Zum Propagieren der Sicherheitsstufen; laufen von FO-Knoten zu FI-Knoten.
// *
// * @author giffhorn
// *
// */
//public class PropagateCondition {
//    protected SecurityNode fo;
//    protected Collection<SecurityNode> fis;
//
//	public PropagateCondition(SecurityNode fo, Collection<SecurityNode> fis) {
//        this.fo = fo;
//        this.fis = fis;
//    }
//
////	public String propagate(IStaticLattice<String> lattice) {
////	    // compute a P(fi) <= R(f) for all f in fos
////	    String lvl = lattice.getTop();
////        for (SecurityNode f : fos) {
////            lvl = lattice.greatestLowerBound(lvl, f.getRequired());
////        }
////
////        return lvl;
////	}
//
//	public Collection<Element> propagate(Element formOut) {
//	    HashSet<Element> s = new HashSet<Element>();
//	    for (SecurityNode fi : fis) {
//	        Element e = new Element(fi, formOut.getLevel(), formOut.getRestrictions());
//	        s.add(e);
//	    }
//	    return s;
//	}
//
//	public String toString() {
//	    return "lvl("+fo+") = sup("+fis+")";
//	}
//
//	public SecurityNode getNode() {
//	    return fo;
//	}
//}
