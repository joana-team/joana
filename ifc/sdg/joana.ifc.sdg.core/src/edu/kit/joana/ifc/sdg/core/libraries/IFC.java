/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.libraries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.conc.Element;
import edu.kit.joana.ifc.sdg.core.conc.Rule;
import edu.kit.joana.ifc.sdg.core.conc.ViolationComparator;
import edu.kit.joana.ifc.sdg.core.conc.Element.ElementSet;
import edu.kit.joana.ifc.sdg.core.interfaces.ProgressAnnouncer;
import edu.kit.joana.ifc.sdg.core.interfaces.ProgressListener;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.GraphModifier;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;


/** Verwendet modulares IFC fuer sequentielle Programme.
 *
 * Dies ist eine Proof-of-Concept-Implementierung, die entsprechend angepasst werden muss,
 * sobald echte modulare SDGs zur Verfuegung stehen. Insbesondere fehlen:
 *  - eine Datenbank fuer die zur Verfuegung stehenden Bibliotheken
 *  - das Mapping von Actual-Knoten auf Formal-Knoten in einer Bibliothek
 *
 * @author giffhorn
 */
public class IFC implements ProgressAnnouncer {
    private ArrayList<ProgressListener> pls = new ArrayList<ProgressListener>();

    private IStaticLattice<String> l;                    // der sicherheitsverband
    private SDG g;                                     // der SDG
    private HashMap<SDGEdge, Set<Rule>> summaryDeclass;  // deklassifikationen an den summary-kanten

    /**
     * Initialisiert den IFC-Algorithmus.
     * Der uebergebene SDG muss annotiert sein, der Sicherheitsverband dazu passen.
     *
     * Der Konstruktor berechnet ausserdem die Deklassifikationen an den Summary-Kanten.
     *
     * @param g  Ein SDG.
     * @param l  Ein Sicherheitsverband.
     */
    public IFC(SDG g, IStaticLattice<String> l) {
        this.l = l;
        this.g = g;
        summaryDeclass = computeSummaryDeclassification();
//        for (SDGEdge e : summaryDeclass.keySet()) {
//            System.out.println(e+": "+summaryDeclass.get(e));
//        }
    }

    /**
     * Berechnet die Deklassifikationen an den Summary-Kanten.
     *
     * @return Eine Map von Summary-Kanten auf die garantiert auftretenden Deklassifikationen.
     */
    private HashMap<SDGEdge, Set<Rule>> computeSummaryDeclassification() {
        HashMap<SDGEdge, Set<Rule>> result = new HashMap<SDGEdge, Set<Rule>>();
        HashSet<SecurityNode> declass = new HashSet<SecurityNode>();
        HashMap<Rule, Set<SDGNode>> rules = new HashMap<Rule, Set<SDGNode>>();

        // 1. collect the different declassification rules in the SDG
        for (SDGNode nn : g.vertexSet()) {
            SecurityNode n = (SecurityNode) nn;
            if (n.isDeclassification()) {
                declass.add(n);
                Rule r = new Rule();
                r.in(n.getRequired());
                r.out(n.getProvided());

                Set<SDGNode> s = rules.get(r);
                if (s == null) {
                    s = new HashSet<SDGNode>();
                    rules.put(r, s);
                }
            }
        }
//        System.out.println("rules: "+rules.keySet());

        // 2. for every rule, collect all declassifications annotated with that rule
        for (Rule r : rules.keySet()) {
            Set<SDGNode> s = rules.get(r);

            for (SecurityNode n : declass) {
                if (l.leastUpperBound(n.getRequired(), r.in()).equals(r.in())
                        && l.leastUpperBound(n.getProvided(), r.out()).equals(n.getProvided())){
                    s.add(n);
                }
            }
//            System.out.println("    rule: "+r);
//            System.out.println("    declass: "+rules.get(r));
        }

        // 3. for each rule, compute the summary declassifications
        for (Rule r : rules.keySet()) {
            // collect all declassifications which allow Rule r
            Set<SDGNode> barrier = rules.get(r);

            // compute affected edges
            Collection<SDGEdge> sum = GraphModifier.blockSummaryEdges(g, barrier);
//            System.out.println("  edges: "+sum);
            for (SDGEdge e : sum) {
                Set<Rule> set = result.get(e);

                if (set == null) {
                    set = new HashSet<Rule>();
                    result.put(e, set);
                }

                set.add(r);
            }
        }

        return result;
    }

    /* ProgressListener */

    public void addProgressListener(ProgressListener pl) {
        this.pls.add(pl);
    }

    public void removeProgressListener(ProgressListener pl) {
        this.pls.remove(pl);
    }

    /** Fuehrt den Sicherheitscheck aus.
     *
     * @return Die Menge der gefundenen Sicherheitsverletzungen.
     */
    public Set<ClassifiedViolation> check() {
        // bestimme alle kritischen Punkte
        LinkedList<Element> criteria = collectCriteria();
        Set<ClassifiedViolation> set = new HashSet<ClassifiedViolation>();

        // pruefe jeden kritischen Punkt auf noninterferenz
        for (Element e : criteria) {
//            System.out.println("slice for "+e);
//            System.out.println("**********************************************");
            set.addAll(slice(e));
        }

        return set;
    }

    /** Bestimmt die kritischen Punkte im SDG.
     *
     * @return Eine Liste aller als Quelle oder Senke annotierter Knoten.
     */
    private LinkedList<Element> collectCriteria() {
        LinkedList<Element> criteria = new LinkedList<Element>();

        // suche alle annotierten knoten (keine deklassifikationen)
        for (SDGNode o :  g.vertexSet())  {
            SecurityNode temp = (SecurityNode) o;

            if (temp.isInformationSink()) {
                String label = temp.getRequired();
                // suche alle label, die `label' nicht beeinflussen duerfen
                Collection<String> bad = LatticeUtil.collectNoninterferingElements(label, this.l);
                // baue ein entsprechendes Element
                Element e = new Element(temp, label, bad);
                criteria.add(e);

            } else if (temp.isInformationSource()) {
                // dasselbe fuer informationsquellen
                String label = temp.getProvided();
                Collection<String> bad = LatticeUtil.collectNoninterferingElements(label, this.l);
                Element e = new Element(temp, label, bad);
                criteria.add(e);
            }
        }

        return criteria;
    }

    /**
     * Berechnet IFC fuer einen annotierten Knoten.
     *
     * @param criterion  Der annotierte Knoten.
     * @return           Die gefundenen Sicherheitsverletzungen.
     */
    private Set<ClassifiedViolation> slice(Element criterion) {
        LinkedList<Element> worklist_1 = new LinkedList<Element>();
        LinkedList<Element> worklist_2 = new LinkedList<Element>();
        ElementSet visited = new ElementSet();
        TreeSet<ClassifiedViolation> vio = new TreeSet<ClassifiedViolation>(ViolationComparator.COMP);

        worklist_1.add(criterion);
        visited.add(criterion);

        // === phase 1 ===
        // only ascend to calling procedures
        while (!worklist_1.isEmpty()) {
            Element next = worklist_1.poll();
//            System.out.println(next);

            // modular IFC
            if (next.getNode().getKind() == SDGNode.Kind.ACTUAL_OUT) {
                // get the propagation rules of the called library
                LibraryPropagationRules pre = getLibrary(next.getNode());

                if (pre != null) {
//                    System.out.println("IFC library: "+pre.getName());
                    // map the AO-node to the fitting FO-node in the library
                    Element formOut = mapping(next);
//                    System.out.println("    "+"entering: "+formOut);
                    // retrieve the annotations of the reachable FI-nodes
                    Collection<Element> elems = pre.propagateSecurityLevels(formOut, l);
//                    System.out.println("    "+"leaving:  "+elems);
                    // map the FI-nodes to the fitting AI-nodes
                    elems = mapping(next, elems);
//                    System.out.println("    "+"mapping:  "+elems);

                    // add the annotated AI-nodes to the worklist
                    for (Element e : elems) {
                        if (visited.add(e)) {
                            worklist_1.add(e);
                        }
                    }
                    continue; // TODO: hack zum testen, im produktivsystem loeschen
                }
            }

            for (SDGEdge edge : g.incomingEdgesOf(next.getNode())) {
                if (next.getNode().getKind() == SDGNode.Kind.ACTUAL_OUT
                        && edge.getSource().getKind() == SDGNode.Kind.CALL) {
                    // a shiny little hack
                    continue;

                } else if (edge.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
                    // traverse the edge and apply possible declassifications
                    Element newElement = declass(next, edge, visited);

                    if (newElement == null) continue;

                    // security check
                    SecurityNode reached = (SecurityNode) edge.getSource();
                    if (reached.isInformationSource() && newElement.getLabels().contains(reached.getProvided())) {
                        vio.add(ClassifiedViolation.createViolation(criterion.getNode(), reached, criterion.getNode().getRequired()));
                    }

                    // add the new element to the worklist
                    if (visited.add(newElement)) {
                        worklist_2.add(newElement);
                    }

                } else if (edge.getKind().isSDGEdge()) {
                    // traverse the edge and apply possible declassifications
                    Element newElement = declass(next, edge, visited);

                    if (newElement == null) continue;

                    // security check
                    SecurityNode reached = (SecurityNode) edge.getSource();
                    if (reached.isInformationSource() && newElement.getLabels().contains(reached.getProvided())) {
                        vio.add(ClassifiedViolation.createViolation(criterion.getNode(), reached, criterion.getNode().getRequired()));
                    }

                    // add the new element to the worklist
                    if (visited.add(newElement)) {
                        worklist_1.add(newElement);
                    }
                }
            }
        }

        // === phase 2 ===
        // visit all transitively called procedures
        while (!worklist_2.isEmpty()) {
            Element next = worklist_2.poll();
//                System.out.println(next);

            // modular IFC
            if (next.getNode().getKind() == SDGNode.Kind.ACTUAL_OUT) {
                // get the propagation rules of the called library
                LibraryPropagationRules pre = getLibrary(next.getNode());

                if (pre != null) {
                    // map the AO-node to the fitting FO-node in the library
                    Element formOut = mapping(next);
                    // retrieve the annotations of the reachable FI-nodes
                    Collection<Element> elems = pre.propagateSecurityLevels(formOut, l);
                    // map the FI-nodes to the fitting AI-nodes
                    elems = mapping(next, elems);

                    // add the annotated AI-nodes to the worklist
                    for (Element e : elems) {
                        if (visited.add(e)) {
                            worklist_2.add(e);
                        }
                    }

                    continue; // TODO: hack zum testen, im produktivsystem loeschen
                }
            }

            for (SDGEdge edge : g.incomingEdgesOf(next.getNode())) {
                if (edge.getKind().isSDGEdge()
                        && edge.getKind() != SDGEdge.Kind.CALL
                        && edge.getKind() != SDGEdge.Kind.PARAMETER_IN
                        && edge.getKind() != SDGEdge.Kind.FORK
                        && edge.getKind() != SDGEdge.Kind.FORK_IN ) {

                    // traverse the edge and apply possible declassifications
                    Element newElement = declass(next, edge, visited);

                    if (newElement == null) continue;

                    // security check
                    SecurityNode reached = (SecurityNode) edge.getSource();
                    if (reached.isInformationSource() && newElement.getLabels().contains(reached.getProvided())) {
                        vio.add(ClassifiedViolation.createViolation(criterion.getNode(), reached, criterion.getNode().getRequired()));
                    }

                    // add the new element to the worklist
                    if (visited.add(newElement)) {
                        worklist_2.add(newElement);
                    }
                }
            }
        }

        return vio;
    }

    /**
     * Traversiert eine Kante im SDG und wendet auftretende Deklassifikationen an.
     *
     * Die Methode prueft unter anderem, ob die Traversion redundant ist. In diesem
     * Fall wird null returnt.
     *
     * @param oldElement  Der Ausgangsknoten.
     * @param e           Eine eingehende Kante.
     * @param set         Enthaelt die bisher besuchten Elemente.
     * @return            Den erreichten Knoten mitsamt passender Annotationen oder null.
     */
    private Element declass(Element oldElement, SDGEdge e, ElementSet set) {
        // get the node
        SecurityNode reached = (SecurityNode) e.getSource();
        // create new set of labels
        Set<String> newLabels = new HashSet<String>();
        newLabels.addAll(oldElement.getLabels());

        // remove all already used labels
        Collection<String> marked = set.get(reached, oldElement.getLevel());
        if (marked != null) {
            newLabels.removeAll(marked);
        }

        if (!newLabels.isEmpty()) {
            // remove freshly declassified labels
            if (reached.isDeclassification()
                    && l.leastUpperBound(reached.getProvided(), oldElement.getLevel()).equals(oldElement.getLevel())) {

                // iflowin is declassified
                newLabels.removeAll(LatticeUtil.collectAllLowerElements(reached.getRequired(), l));
            }

            if (e.getKind() == SDGEdge.Kind.SUMMARY) {
                // remove the declassified labels in the summarized paths
                Set<Rule> rules = summaryDeclass.get(e);

                if (rules != null) {
                    for (Rule r : rules) {
                        if (l.leastUpperBound(r.out(), oldElement.getLevel()).equals(oldElement.getLevel())) {
                            newLabels.removeAll(LatticeUtil.collectAllLowerElements(r.in(), l));
                        }
                    }
                }
            }

            // if newLabels is still not empty, create a new Element and return it
            if (!newLabels.isEmpty()) {
                //marked.addAll(newLabels);
                Element newElement = new Element(reached, oldElement.getLevel(), newLabels);

                return newElement;
            }
        }

        // newLabels was empty
        return null;
    }


    /* TODO: Testtreiber, muessen im echten System ersetzt werden! */
    private HashMap<Integer, LibraryPropagationRules> preconditions = new HashMap<Integer, LibraryPropagationRules>();

    public void addPrecondition(int proc, LibraryPropagationRules pre) {
        preconditions.put(proc, pre);
    }

    private LibraryPropagationRules getLibrary(SecurityNode actOut) {
        for (SDGEdge po : g.getIncomingEdgesOfKind(actOut, SDGEdge.Kind.PARAMETER_OUT)) {
            return preconditions.get(po.getSource().getProc());
        }
        return null;
    }

    private Element mapping(Element actOut) {
        SecurityNode formOut = null;

        for (SDGEdge po : g.getIncomingEdgesOfKind(actOut.getNode(), SDGEdge.Kind.PARAMETER_OUT)) {
            formOut = (SecurityNode) po.getSource();
        }

        HashSet<String> labels = new HashSet<String>();
        labels.addAll(actOut.getLabels());

        Element e = new Element(formOut, actOut.getLevel(), labels);
        return e;
    }

    private Collection<Element> mapping(Element actOut, Collection<Element> formIns) {
        HashSet<Element> mapped = new HashSet<Element>();
        SDGNode call = g.getCallSiteFor(actOut.getNode());
        Collection<SDGNode> callSite = g.getParametersFor(call);

        for (Element e : formIns) {
            for (SDGEdge f : g.incomingEdgesOf(e.getNode())) {
                if (f.getKind() == SDGEdge.Kind.CALL || f.getKind() == SDGEdge.Kind.PARAMETER_IN) {
                    if (callSite.contains(f.getSource())) {
                        Element x = new Element((SecurityNode) f.getSource(), e.getLevel(), e.getLabels());
                        mapped.add(x);
                    }
                }
            }
        }

        return mapped;
    }
}
