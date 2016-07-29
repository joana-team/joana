/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.libraries;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.conc.Element;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;


/**
 * Die Propagationsregeln fuer eine Bibliothek.
 *
 * Berechnet fuer eine konkrete Annotation eines Formal-Out-Knotens der Basismethode der
 * Bibliothek, welche Annotationen sich fuer die erreichbaren Formal-in-Knoten ergeben.
 *
 * @author giffhorn
 */
public class LibraryPropagationRules {
    /**
     * Eine Abstraktion der Worklist-Elemente, die im IFC-Algorithmus verwendet werden.
     *
     * Speichert sich die anzuwendenden Deklassifikationen, die dann angewandt werden
     * koennen, wenn eine konkrete Annotation bekannt ist.
     *
     * Es wird dabei unterschieden zwischen garantiert auftretenden und evtl. auftretenden
     * Deklassifikationen. Bei letzteren wird das HookUpElement dupliziert und nur die Kopie
     * modifiziert.
     *
     * @author giffhorn
     */
    public static class HookInElement {
        // der aktuelle knoten
        SecurityNode node;
        // alle garantiert auftretenden deklassifikationen auf dem pfad vom FO-knoten zu node
        Collection<SecurityNode> declass;
        // moeglicherweise auftretende deklassifikationen auf dem pfad vom FO-knoten zu node
        Collection<SecurityNode> summaryDeclass;

        /**
         * Erzeugt eine neue Instanz.
         *
         * @param n  Ein Knoten.
         * @param dec  Alle garantiert auftretenden Deklassifikationen auf dem Pfad zu n.
         * @param sum  Moeglicherweise auftretende Deklassifikationen auf dem Pfad zu n.
         */
        public HookInElement(SecurityNode n, Collection<SecurityNode> dec, Collection<SecurityNode> sum) {
            node = n;
            declass = dec;
            summaryDeclass = sum;
        }

        public SecurityNode getNode() {
            return node;
        }

        /**
         * Wendet eine konkrete Belegung von Sicherheitsstufen auf dieses Element an.
         *
         * @param foLevel  Die Sicherheitsstufe des urspruenglichen Formal-Out-Knotens.
         * @param levels   Die Sicherheitsstufen, die nicht mit foLevel interferieren duerfen.
         * @param lattice  Der Sicherheitsverband.
         * @return         Das entsprechende Element fuer den IFC-Algorithmus.
         */
        public Collection<Element> hookIn(String foLevel, Collection<String> levels, IStaticLattice<String> lattice) {
            HashSet<Element> set = new HashSet<Element>();

            // entferne alle garantiert deklassifizierten level aus levels
            for (SecurityNode s : declass) {
                if (lattice.leastUpperBound(s.getProvided(), foLevel).equals(foLevel)) {
                    levels.removeAll(lattice.collectAllLowerElements(s.getRequired()));
                }
            }

            // behandle summary-deklassifikationen
            if (summaryDeclass.isEmpty()) {
                // falls es keine gibt, Element erzeugen und abspeichern
                HashSet<String> lvl = new HashSet<String>();
                lvl.addAll(levels);
                Element e = new Element(node, foLevel, lvl);
                set.add(e);

            } else {
                // erzeuge fuer jede deklassifikation ein neues Element und wende die
                // deklassifikation an
                for (SecurityNode s : summaryDeclass) {
                    HashSet<String> lvl = new HashSet<String>();
                    lvl.addAll(levels);
                    lvl.removeAll(lattice.collectAllLowerElements(s.getRequired()));
                    Element e = new Element(node, foLevel, lvl);
                    set.add(e);
                }
            }

            return set;
        }

        public int hashCode() {
            // die labels sind unwichtig
            return node.hashCode() + 31 * declass.hashCode();
        }

        public boolean equals(Object o) {
            if (o instanceof HookInElement) {
                HookInElement e = (HookInElement) o;

                return e.node == node && e.declass.equals(declass);

            } else {
                return false;
            }
        }

        public String toString() {
            return "(" + node + ", " + declass + ", " + summaryDeclass + ")";
        }
    }

    // der name der bibliothek
    private String name;
    // fuer jeden FO-knoten der bibliothek gibt es eine propagationsregel
    private List<PropagationRule> conditions;

    /**
     * Initialisierung.
     *
     * @param name  Der Name der Bibliothek.
     */
    public LibraryPropagationRules(String name) {
        this.name = name;
        this.conditions = new LinkedList<PropagationRule>();
    }

    public String getName() {
        return name;
    }

    public String toString() {
        String str = "Conditions for calling "+name+"\n";

        for (PropagationRule c : conditions) {
            str += c.toString() + "\n";
        }

        return str;
    }

    void addCondition(PropagationRule con) {
        conditions.add(con);
    }

    /** Berechnet fuer eine konkrete Annotation eines Form-Out-Knotens die sich daraus ergebenden Annotationen der
     * erreichbaren Form-in-Knoten.
     *
     * @param formOut     Ein annotierter Form-Out-Knoten dieser Bibliothek.
     * @param lattice     Ein passender Sicherheitsverband.
     * @return            Die Menge der erreichbaren Form-In-Knoten, annotiert mit Sicherheitsstufen.
     */
    public Collection<Element> propagateSecurityLevels(Element formOut, IStaticLattice<String> lattice) {
        HashSet<Element> m = new HashSet<Element>();

        for (PropagationRule c : conditions) {
            // suche die passende Propagationsregel...
            if (c.getNode().getId() == formOut.getNode().getId()) {
                // und wende sie an
                Collection<Element> elems = c.propagate(formOut, lattice);
                m.addAll(elems);
            }
        }

        return m;
    }
}
