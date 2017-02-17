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

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.interfaces.ProgressAnnouncer;
import edu.kit.joana.ifc.sdg.core.interfaces.ProgressListener;
import edu.kit.joana.ifc.sdg.core.libraries.LibraryPropagationRules.HookInElement;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.SummaryMergedChopper;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.GraphModifier;


/** Fuehrt modulares IFC fuer Bibliotheken durch.
 *
 * @author giffhorn
 *
 */
public class LibraryIFC implements ProgressAnnouncer {
    private final ArrayList<ProgressListener> pls = new ArrayList<ProgressListener>();
    // der SDG
    private final SDG g;
    // Deklassifikationen an Summary-Kanten
    private HashMap<SDGEdge, Collection<SDGNode>> summaryDeclass;

    /**
     * Initialisiert die Analyse.
     *
     * @param g  Ein SDG.
     */
    public LibraryIFC(SDG g) {
        this.g = g;
    }

    /** Berechnet die Propagation der Sicherheitsstufen von Formal-Out zu Formal-In Knoten.
     *
     * @param libEntry  Der Startknoten der Bibsliothek.
     * @return  Die Propagationen.
     */
    public LibraryPropagationRules computeConditions(SecurityNode libEntry) {
        LibraryPropagationRules pre = new LibraryPropagationRules(libEntry.getLabel());

        // bestimme die summary-deklassifikationen
        summaryDeclass = computeSummaryDeclassification(libEntry);
//        System.out.println(summaryDeclass);

        // bestimme alle FO-Knoten in der Basis-Methode
        LinkedList<HookInElement> criteria = collectCriteria(libEntry);
//        System.out.println(criteria);

        // suche die erreichbaren FI-Knoten und sammle deklassifikationen auf
        for (HookInElement e : criteria) {
//            System.out.println("slice for "+e);
            pre.addCondition(slice(e));
//            System.out.println("**********************************************");
        }

        return pre;
    }

    /**
     * Berechnet die Deklassifikationen an den Summary-Kanten.
     * Da nur die Pfade von FO- zu FI-Knoten in der Basismethode der Bibliothek
     * betrachtet werden, muessen auch nur die Summary-Kanten in der Basismethode
     * behandelt werden.
     *
     * @param libEntry  Der Startknoten der Basismethode.
     * @return  Eine Map von Summary-Kanten auf Deklassifikationen.
     */
    private HashMap<SDGEdge, Collection<SDGNode>> computeSummaryDeclassification(SecurityNode libEntry) {
        HashMap<SDGEdge, Collection<SDGNode>> map = new HashMap<SDGEdge, Collection<SDGNode>>();

        // 1. alle summary-kanten in der basismethode sammeln
        LinkedList<SDGEdge> edges = new LinkedList<SDGEdge>();

        for (SDGEdge e : g.edgeSet()) {
            if (e.getKind() == SDGEdge.Kind.SUMMARY && e.getTarget().getProc() == libEntry.getProc()) {
                edges.add(e);
            }
        }

        // 2. sammel alle deklassifikationen und berechne die betroffenen summary-kanten aus `edges'
        Set<SDGNode> barrier = new HashSet<SDGNode>();

        for (SDGNode nn : g.vertexSet()) {
            SecurityNode n = (SecurityNode) nn;
            if (n.isDeclassification()) {
                barrier.add(n);
            }
        }

        Collection<SDGEdge> sum = GraphModifier.blockSummaryEdges(g, barrier);
        sum.retainAll(edges);

        // 3. berechne fuer jede dieser summary-kanten, welche deklassifikationen involviert sind
        SummaryMergedChopper chopper = new SummaryMergedChopper(g);
        for (SDGEdge e : sum) {
            Collection<SDGNode> chop = chopper.chop(e.getSource(), e.getTarget());
            chop.retainAll(barrier);
            map.put(e, chop);
        }

        return map;
    }

    /**
     * Sammelt die FO- und Exit-Knoten der Basismethode auf.
     *
     * @param libEntry  Der Startknoten der Basismethode.
     * @return  Die FO- und Exit-Knoten, als HookInElement-Objekte
     */
    private LinkedList<HookInElement> collectCriteria(SecurityNode libEntry) {
        LinkedList<HookInElement> criteria = new LinkedList<HookInElement>();

        // suche alle FO-Knoten in der Basismethode
        for (SDGNode o :  g.vertexSet())  {
            if (o.getProc() == libEntry.getProc() &&
                    (o.getKind() == SDGNode.Kind.FORMAL_OUT || o.getKind() == SDGNode.Kind.EXIT)) {

                SecurityNode temp = (SecurityNode) o;
                // erzeuge die sets fuer die deklassifikationen
                Collection<SecurityNode> d = new HashSet<SecurityNode>();
                Collection<SecurityNode> s = new HashSet<SecurityNode>();

                // behandle den fall, dass temp selbst deklassifiziert
                if (temp.isDeclassification()) {
                    d.add(temp);
                }

                HookInElement e = new HookInElement(temp, d, s);
                criteria.add(e);
            }
        }

        return criteria;
    }

    /**
     * Fuehrt den IFC-Slice fuer einen FO-Knoten durch.
     * Da wir nur Pfade zu den FI-Knoten suchen, ist dies eine intra-prozedurale Traversion,
     * die die Summary-Deklassifikation ausnutzt.
     *
     * @param criterion  Ein FO-Knoten.
     * @return  Die Propagationsregel fuer criterion.
     */
    private PropagationRule slice(HookInElement criterion) {
        HashSet<HookInElement> hooks = new HashSet<HookInElement>();
        LinkedList<HookInElement> wl = new LinkedList<HookInElement>();
        Set<HookInElement> marked = new HashSet<HookInElement>();

        wl.add(criterion);
        marked.add(criterion);

        // we only need an intra-procedural traversal of the base method,
        // because the called methods were already treated before by the
        // summary edge declassification
        while (!wl.isEmpty()) {
            HookInElement next = wl.poll();
//                System.out.println(next);
            if (next.getNode().getKind() == SDGNode.Kind.FORMAL_IN || next.getNode().getKind() == SDGNode.Kind.ENTRY) {
                // pfad gefunden
                hooks.add(next);
            }

            // intra-prozedurale traversion
            for (SDGEdge edge : g.incomingEdgesOf(next.node)) {
                if (edge.getKind().isIntraSDGEdge()) {
                    // berechne in jedem schritt die moeglichen deklassifikationen
                    HookInElement newElement = declass(next, edge);

                    if (marked.add(newElement)) {
                        wl.add(newElement);
                    }
                }
            }
        }

        return new PropagationRule(criterion.getNode(), hooks);
    }

    /**
     * Berechnet fuer die Traversion einer Kante die anfallenden Deklassifikationen.
     *
     * @param oldElement  Das Element vor der Traversion.
     * @param e  Die zu traversierende Kante.
     * @return  Das aktualisierte Element.
     */
    private HookInElement declass(HookInElement oldElement, SDGEdge e) {
        // get the node
        SecurityNode reached = (SecurityNode) e.getSource();
        // create new set of declassifications
        Collection<SecurityNode> d = new HashSet<SecurityNode>();
        Collection<SecurityNode> s = new HashSet<SecurityNode>();
        d.addAll(oldElement.declass);
        s.addAll(oldElement.summaryDeclass);

        // falls der erreichte knoten eine deklassifikation ist,
        // wende diese deklassifikation an
        if (reached.isDeclassification()) {
            d.add(reached);
        }

        // falls die kante eine summary-kante ist,
        // sammle die moeglichen deklassifikationen auf
        if (e.getKind() == SDGEdge.Kind.SUMMARY) {
            Collection<SDGNode> rules = summaryDeclass.get(e);

            if (rules != null) {
                for (SDGNode n : rules) {
                    s.add((SecurityNode) n);
                }
            }
        }

        return new HookInElement(reached, d, s);
    }


    /* ProgressListener */

    public void addProgressListener(ProgressListener pl) {
        this.pls.add(pl);
    }

    public void removeProgressListener(ProgressListener pl) {
        this.pls.remove(pl);
    }
}
