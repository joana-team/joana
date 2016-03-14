/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.conc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.ifc.sdg.core.IFC;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.conc.Element.ElementSet;
import edu.kit.joana.ifc.sdg.core.interfaces.ProgressAnnouncer;
import edu.kit.joana.ifc.sdg.core.interfaces.ProgressListener;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.GraphModifier;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;


/** Ueberprueft einen gegebenen SDG auf possibilistische Noninterferenz.
 * Verwendet intransitive Deklassifikation nach der Idee von Krinke.
 *
 * @author giffhorn
 */
public class BarrierIFCSlicer extends IFC<String> implements ProgressAnnouncer {
	
    private ArrayList<ProgressListener> pls = new ArrayList<ProgressListener>();
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
    public BarrierIFCSlicer(SDG g, IStaticLattice<String> l) {
    	super(g, l);
//        long time = System.currentTimeMillis();
        summaryDeclass = computeSummaryDeclassification();
//        time = System.currentTimeMillis() - time;
//        System.out.println("summary declass. : "+time);
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
                r.in = n.getRequired();
                r.out = n.getProvided();

                Set<SDGNode> s = rules.get(r);
                if (s == null) {
                    s = new HashSet<SDGNode>();
                    rules.put(r, s);
                }
            }
        }
//        System.out.println("rules: "+rules.keySet());

        // 2. for every rule, collect all declassifications annotated with that rule
        for (Map.Entry<Rule, Set<SDGNode>> p : rules.entrySet()) {
        	Rule r = p.getKey();
            Set<SDGNode> s = p.getValue();

            for (SecurityNode n : declass) {
                if (l.leastUpperBound(n.getRequired(), r.in).equals(r.in)
                        && l.leastUpperBound(n.getProvided(), r.out).equals(n.getProvided())){
                    s.add(n);
                }
            }
//            System.out.println("    rule: "+r);
//            System.out.println("    declass: "+rules.get(r));
        }

        // 3. for each rule, compute the summary declassifications
        for (Map.Entry<Rule, Set<SDGNode>> p : rules.entrySet()) {
            // collect all declassifications which allow Rule r
            Rule r = p.getKey();
        	Set<SDGNode> barrier = p.getValue();

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
    public Set<ClassifiedViolation> checkIFlow() {
        // bestimme alle kritischen Punkte
        LinkedList<Element> criteria = collectCriteria();
        Set<ClassifiedViolation> set = new HashSet<ClassifiedViolation>();

        // pruefe jeden kritischen Punkt auf possibilistische noninterferenz
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
                // baue ein entsprechendes Element
                HashSet<String> set = new HashSet<String>();
                set.add(label);
                Element e = new Element(temp, label, set);
                criteria.add(e);

            } else if (temp.isInformationSource()) {
            	// dasselbe fuer informationsquellen
                String label = temp.getProvided();
                HashSet<String> set = new HashSet<String>();
                set.add(label);
                Element e = new Element(temp, label, set);
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
        LinkedList<Element> worklist_0 = new LinkedList<Element>();
        LinkedList<Element> worklist_1 = new LinkedList<Element>();
        LinkedList<Element> worklist_2 = new LinkedList<Element>();
        ElementSet visited_0_1 = new ElementSet();
        ElementSet visited_2 = new ElementSet();
        HashSet<ClassifiedViolation> vio = new HashSet<ClassifiedViolation>();

        worklist_0.add(criterion);
        visited_0_1.add(criterion);

        while (!worklist_0.isEmpty()) {
            // init the next iteration
            visited_2.clear();

            worklist_1.add(worklist_0.poll());
            visited_0_1.add(worklist_1.peek());

            // === phase 1 ===
            // only ascend to calling procedures
            while (!worklist_1.isEmpty()) {
                Element next = worklist_1.poll();
//                System.out.println(next);

                for (SDGEdge edge : g.incomingEdgesOf(next.node)) {
                    // apply declassifications
                    if (edge.getKind() == SDGEdge.Kind.INTERFERENCE
                            || edge.getKind() == SDGEdge.Kind.FORK_IN
                            || edge.getKind() == SDGEdge.Kind.FORK_OUT
                            || edge.getKind() == SDGEdge.Kind.FORK) {
                        // handle inter-threadual edges

                        Element newElement = declass(next, edge, visited_0_1);

                        if (newElement == null) continue;

                        // security check
                        SecurityNode reached = (SecurityNode) edge.getSource();
                        if (reached.isInformationSource() && isLeaking(newElement.labels, reached.getProvided())) {
                        	if (criterion.node.isInformationSink()) {
                        		vio.add(ClassifiedViolation.createViolation(criterion.node, reached, criterion.node.getRequired()));

                        	} else if (criterion.node.isInformationSource()) {
                        		vio.add(ClassifiedViolation.createViolation(criterion.node, reached, criterion.node.getProvided()));
                        	}
                        }

                        if (visited_0_1.add(newElement)) {
                            worklist_0.add(newElement);
                        }

                    } else if (edge.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
                        // ascend into a calling procedure
                        Element newElement = declass(next, edge, visited_2);

                        if (newElement == null) continue;

                        // security check
                        SecurityNode reached = (SecurityNode) edge.getSource();
                        if (reached.isInformationSource() && isLeaking(newElement.labels, reached.getProvided())) {
                            if (criterion.node.isInformationSink()) {
                        		vio.add(ClassifiedViolation.createViolation(criterion.node, reached, criterion.node.getRequired()));

                        	} else if (criterion.node.isInformationSource()) {
                        		vio.add(ClassifiedViolation.createViolation(criterion.node, reached, criterion.node.getProvided()));
                        	}
                        }


                        if (visited_2.add(newElement)) {
                            worklist_2.add(newElement);
                        }

                    } else if (edge.getKind().isSDGEdge()) {
                        // for intra-procedural and descending edges
                        Element newElement = declass(next, edge, visited_0_1);

                        if (newElement == null) continue;

                        // security check
                        SecurityNode reached = (SecurityNode) edge.getSource();
                        if (reached.isInformationSource() && isLeaking(newElement.labels, reached.getProvided())) {
                            if (criterion.node.isInformationSink()) {
                        		vio.add(ClassifiedViolation.createViolation(criterion.node, reached, criterion.node.getRequired()));

                        	} else if (criterion.node.isInformationSource()) {
                        		vio.add(ClassifiedViolation.createViolation(criterion.node, reached, criterion.node.getProvided()));
                        	}
                        }

                        if (visited_0_1.add(newElement)) {
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

                for (SDGEdge edge : g.incomingEdgesOf(next.node)) {
                    if (edge.getKind() == SDGEdge.Kind.INTERFERENCE) {
                        // handle interference edges: create new elements for worklist_0
                        // apply declassifications
                        Element newElement = declass(next, edge, visited_0_1);

                        if (newElement == null) continue;

                        // security check
                        SecurityNode reached = (SecurityNode) edge.getSource();
                        if (reached.isInformationSource() && isLeaking(newElement.labels, reached.getProvided())) {
                            if (criterion.node.isInformationSink()) {
                        		vio.add(ClassifiedViolation.createViolation(criterion.node, reached, criterion.node.getRequired()));

                        	} else if (criterion.node.isInformationSource()) {
                        		vio.add(ClassifiedViolation.createViolation(criterion.node, reached, criterion.node.getProvided()));
                        	}
                        }

                        if (visited_0_1.add(newElement)) {
                            worklist_0.add(newElement);
                        }

                    } else if (edge.getKind().isSDGEdge()
                            && edge.getKind() != SDGEdge.Kind.CALL
                            && edge.getKind() != SDGEdge.Kind.PARAMETER_IN
                            && edge.getKind() != SDGEdge.Kind.FORK
                            && edge.getKind() != SDGEdge.Kind.FORK_IN ) {

                        // intra-procedural and param-out edges
                        // apply declassifications
                        Element newElement = declass(next, edge, visited_2);

                        if (newElement == null) continue;

                        // security check
                        SecurityNode reached = (SecurityNode) edge.getSource();
                        if (reached.isInformationSource() && isLeaking(newElement.labels, reached.getProvided())) {
                            if (criterion.node.isInformationSink()) {
                        		vio.add(ClassifiedViolation.createViolation(criterion.node, reached, criterion.node.getRequired()));

                        	} else if (criterion.node.isInformationSource()) {
                        		vio.add(ClassifiedViolation.createViolation(criterion.node, reached, criterion.node.getProvided()));
                        	}
                        }

                        if (visited_2.add(newElement)) {
                            worklist_2.add(newElement);
                        }
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
        newLabels.addAll(oldElement.labels);

        // add freshly declassified labels
        if (reached.isDeclassification()
        		&& l.leastUpperBound(reached.getProvided(), oldElement.getLevel()).equals(oldElement.getLevel())) {

            // reached's source level is declassified
            newLabels.add(reached.getRequired());
        }

        if (e.getKind() == SDGEdge.Kind.SUMMARY) {
            // add the applying declassifications labels in the summarized paths
            Set<Rule> rules = summaryDeclass.get(e);

            if (rules != null) {
                for (Rule r : rules) {
                    if (l.leastUpperBound(r.out(), oldElement.getLevel()).equals(oldElement.getLevel())) {
                        newLabels.add(r.in);
                    }
                }
            }
        }

        // check if the new element would be redundant
        Collection<String> markedLabels = set.get(reached, oldElement.label);
        if (markedLabels == null || !markedLabels.containsAll(newLabels)) {
            //marked.addAll(newLabels);
            Element newElement = new Element(reached, oldElement.label, newLabels);
            return newElement;

        } else {
        	return null;
        }
    }

    private boolean isLeaking(Collection<String> labels, String source) {
    	for (String l : labels) {
    		if (this.l.leastUpperBound(source, l).equals(l)) {
    			// sichtbarkeit ist erlaubt
    			return false;
    		}
    	}

    	return true;
    }
}
