/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.conc;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import edu.kit.joana.ifc.sdg.core.SecurityNode;


/**
 * Ein annotierter SecurityNode.
 *
 * Der Knoten ist mit einem Sicherheitslevel annotiert, der die oberere Schranke
 * fuer eingehende Informationslevel darstellt. Dieses Level wird bei der Traversion
 * propagiert.
 *
 * Weiterhin ist der Knoten mit allen Sicherheitsleveln annotiert, die auf dem bisher
 * traversierten Pfad nicht mit label interferieren duerfen. Diese Menge kann beim
 * Antreffen von Deklassifikationen modifiziert werden.
 *
 * @author giffhorn
 */
public class Element {
    /**
     * Verwaltet die bisher besuchten Elemente. Erkennt beim Einfuegen redundante Elemente.
     *
     * @author giffhorn
     */
    public static class ElementSet {
        // realisierung durch eine map
        private final HashMap<Element, Collection<String>> map;

        /**
         * Initialisiert die Map.
         */
        public ElementSet() {
            map = new HashMap<Element, Collection<String>>();
        }

        /**
         * Fuegt Element e hinzu, falls es nicht redundant ist.
         * e ist redundant, falls der Knoten insgesamt bereits mit allen
         * Sicherheitsleveln besucht wurde, die e enthaelt.
         *
         * @param e  Das neue Element.
         * @return   false, falls e redundant ist.
         */
        public boolean add(Element e) {
            boolean in = false;
            // alle bisherigen levels
            Collection<String> labels = map.get(e);

            if (labels == null) {
                // noch nie besucht, alle level von e merken
                in = true;
                labels = new HashSet<String>();
                labels.addAll(e.labels);
                map.put(e, labels);

            } else if (!labels.containsAll(e.labels)) {
                // e enthaelt level, mit denen der knoten in e noch
                // nicht besucht wurde, daher nicht redundant
                in = true;
                labels.addAll(e.labels);
            }

            return in;
        }

        /**
         * Liefert alle Level, mit denen n, annotiert mit label, bereits besucht wurde.
         * Kann null returnen.
         *
         * @param n      Ein SecurityNode.
         * @param label  Ein Sicherheitslevel.
         * @return       Eine Menge Sicherheitslevel oder null.
         */
        public Collection<String> get(SecurityNode n, String label) {
            Element e = new Element(n, label);
            return map.get(e);
        }

        /**
         * Leert das ElementSet.
         */
        public void clear() {
            map.clear();
        }
    }

    SecurityNode node;         // der aktuelle knoten
    String label;              // pruefe den SDG gegen dieses label
    Collection<String> labels; // diese labels sind erlaubt

    /**
     * Initialisiert ein Element.
     *
     * @param n   Ein SecurityNode.
     * @param l   Das zu propagierende Sicherheitslevel.
     * @param ls  Die Level, die l aktuell nicht beeinflussen duerfen.
     */
    public Element(SecurityNode n, String l, Collection<String> ls) {
        node = n;
        label = l;
        labels = ls;
    }

    /**
     * Initialisiert ein Element.
     * Vorsicht: Laesst labels uninitialisiert. Wird fuer Algorithmen verwendet,
     * die labels nicht benoetigen.
     *
     * @param n
     * @param l
     */
    Element(SecurityNode n, String l) {
        node = n;
        label = l;
    }

    public String getLevel() {
        return label;
    }

    public SecurityNode getNode() {
        return node;
    }

    public Collection<String> getLabels() {
        return labels;
    }

    public int hashCode() {
        // die labels sind unwichtig
        return node.hashCode() + 31 * label.hashCode();
    }

    public boolean equals(Object o) {
        // die labels sind unwichtig
        if (o instanceof Element) {
            Element e = (Element) o;
            return e.node == node && e.label.equals(label);

        } else {
            return false;
        }
    }

    public String toString() {
        return "(" + node + ", " + label + ", " + labels +")";
    }
}
