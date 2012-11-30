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
//import java.util.LinkedList;
//import java.util.List;
//
//import edu.kit.joana.ifc.sdg.core.SecurityNode;
//import edu.kit.joana.ifc.sdg.core.conc.Element;
//import edu.kit.joana.ifc.sdg.graph.SDG;
//import edu.kit.joana.ifc.sdg.graph.SDGNode;
//import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicer;
//import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;
//import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerForward;
//
//public class Preconditions {
//    /**
//     * Wrappt den Slicer, um SecurityNodes zu behandeln.
//     *
//     * @author giffhorn
//     */
//    class Slicer {
//        SummarySlicer bs = new SummarySlicerBackward(libSDG);
//        SummarySlicer fs = new SummarySlicerForward(libSDG);
//
//        private Collection<SecurityNode> backslice(SecurityNode n) {
//            List<SDGNode> l = new LinkedList<SDGNode>();
//            l.add(n);
//
//            Collection<SDGNode> slice = bs.slice(l);
//            HashSet<SecurityNode> hs = new HashSet<SecurityNode>();
//
//            for (SDGNode m : slice) {
//                hs.add((SecurityNode) m);
//            }
//
//            return hs;
//        }
//
//        private Collection<SecurityNode> forwslice(SecurityNode n) {
//            List<SDGNode> l = new LinkedList<SDGNode>();
//            l.add(n);
//
//            Collection<SDGNode> slice = fs.slice(l);
//            HashSet<SecurityNode> hs = new HashSet<SecurityNode>();
//
//            for (SDGNode m : slice) {
//                hs.add((SecurityNode) m);
//            }
//
//            return hs;
//        }
//    }
//
//    private Slicer slicer;
//    private SDG libSDG;
//    private List<VerifyCondition> verifyConditions;
//    private List<PropagateCondition> propagateConditions;
//
//    public Preconditions(SDG libSDG) {
//        this.libSDG = libSDG;
//        this.slicer = new Slicer();
//        this.verifyConditions = new LinkedList<VerifyCondition>();
//        this.propagateConditions = new LinkedList<PropagateCondition>();
//    }
//
//    public String toString() {
//        String str = "Conditions for calling "+libSDG.getName()+"\n";
//
//        str += "Verify-Conditions: \n";
//
//        for (VerifyCondition c : verifyConditions) {
//            str += c.toString() + "\n";
//        }
//
//        str += "Propagate-Conditions: \n";
//
//        for (PropagateCondition c : propagateConditions) {
//            str += c.toString() + "\n";
//        }
//
//        return str;
//    }
//
//
//    public void computeConditions(SDGNode entry) {
//        // formal-in und formal-out Knoten sammeln
//        Collection<SecurityNode>[] fifos = collectFormalNodes(entry);
//
//        // Bedingungen berechnen
//        computeVerifyConditions(fifos);
//        computePropagateConditions(fifos);
//    }
//
//    private void computeVerifyConditions(Collection<SecurityNode>[] fifos) {
//        for (SecurityNode n : fifos[0]) {
//            Collection<SecurityNode> slice = slicer.forwslice(n);
//            //System.out.println("slice for "+n+" : "+slice);
//            // alle enthaltenen fi Knoten
//            slice.retainAll(fifos[1]);
//
//            // Bedingung bilden
//            VerifyCondition cond = new VerifyCondition(n, slice);
//            verifyConditions.add(cond);
//        }
//    }
//
//    private void computePropagateConditions(Collection<SecurityNode>[] fifos) {
//        for (SecurityNode n : fifos[1]) {
//            Collection<SecurityNode> slice = slicer.backslice(n);
//
//            // alle enthaltenen fo Knoten
//            slice.retainAll(fifos[0]);
//
//            // Bedingung bilden
//            PropagateCondition cond = new PropagateCondition(n, slice);
//            propagateConditions.add(cond);
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    private Collection<SecurityNode>[] collectFormalNodes(SDGNode entry) {
//        List<SecurityNode>[] fifos = new List[2];
//
//        fifos[0] = new LinkedList<SecurityNode>();
//        fifos[1] = new LinkedList<SecurityNode>();
//
//        for (SDGNode n : libSDG.getParametersFor(entry)) {
//        	if (n.getKind() == SDGNode.Kind.FORMAL_IN) {
//        		fifos[0].add((SecurityNode) n);
//
//        	} else if (n.getKind() == SDGNode.Kind.FORMAL_OUT) {
//        		fifos[1].add((SecurityNode) n);
//        	}
//        }
//
//        return fifos;
//    }
//
//
//    /** Erster Anwendungsfall: Bei einem Programm mit einer bereits bestehenden
//     * Sicherheitsanalyse soll eine Bibliotheksfunktion ausgewechselt werden.
//     *
//     * Die Methode wird fuer jede call site der Bibliotheksfunktion aufgerufen
//     * und analysiert, ob der Aufruf bzgl. der Sicherheitslevel der Parameter
//     * sicher ist.
//     *
//     * @param parameter Die FIFO-Knoten des Aufrufers.
//     * @param lattice   Der Sicherheitsverband.
//     * @param sdg       Der komplette SDG.
//     * @return          Eine Menge an Sicherheitsverletzungen.
//     *                  Ist die Menge leer, ist der Aufruf sicher.
//     */
////    public Set<SimpleViolation> verifyLibraryUsage(Set<SecurityNode> parameter, IStaticLattice<String> lattice, SDG sdg) {
////        Set<SimpleViolation> vios = new HashSet<SimpleViolation>();
////
////        for (VerifyCondition c : verifyConditions) {
////            List<SimpleViolation> l = c.check(lattice);
////            vios.addAll(l);
////        }
////
////        return vios;
////    }
//
//    /** Zweiter Anwendungsfall: Ein zu analysierendes Programm verwendet eine
//     * Bibliotheksfunktion.
//     *
//     * Trifft die Analyse auf einen FO-Knoten der Bibliotheksfunktion, koennen mit
//     * dieser Methode die Elemente fur die ereichbaren FI-Knoten bestimmt
//     * werden, ohne dass die Analyse die Bibliotheksfunktion betreten muss.
//     *
//     */
//    public Collection<Element> propagateSecurityLevels(Element formOut) {
//        HashSet<Element> s = new HashSet<Element>();
//
//        for (PropagateCondition c : propagateConditions) {
//            Collection<Element> elem = c.propagate(formOut);
//            s.addAll(elem);
//        }
//
//        return s;
//    }
//}
