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

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.conc.Element;
import edu.kit.joana.ifc.sdg.core.libraries.LibraryPropagationRules.HookInElement;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;


/**
 * Eine Propagationsregel fuer einen FormOut-Knoten einer Bibliothek.
 *
 * @author giffhorn
 */
public class PropagationRule {
    // der FO-knoten
    protected SecurityNode fo;
    // die erreichbaren FI-knoten, mit abstrakten annotationen versehen
    protected Collection<HookInElement> elems;

    /**
     * Erzeug eine neue Propagationsregel.
     *
     * @param fo     Ein FO-Knoten.
     * @param elems  Die  erreichbaren FI-Knoten, mit abstrakten Annotationen versehen.
     */
	public PropagationRule(SecurityNode fo, Collection<HookInElement> elems) {
	    this.fo = fo;
        this.elems = elems;
    }

	/**
	 * Erhaelt eine konkrete Annotation fuer den FO-Knoten und berechnet daraus die konkreten
	 * Annotationen der erreichbaren FI-Knoten.
	 *
	 * @param fo       Der FO-Knoten mit konkreten Annotationen.
	 * @param lattice  Der passende Sicherheitsverband.
	 * @return         Die erreichbaren FI-Knoten mit konkreten Annotationen.
	 */
	public Collection<Element> propagate(Element fo, IStaticLattice<String> lattice) {
	    Collection<Element> coll = new HashSet<Element>();

	    // berechne alle level, die prinzipiell nicht mit fo interferieren duerfen
	    Collection<String> levels = lattice.collectNoninterferingElements(fo.getLevel());

	    for (HookInElement he : elems) {
	        // berechne fuer jeden abstrakt annotierten FI-knoten konkrete annotationen
	        Collection<Element> e = he.hookIn(fo.getLevel(), levels, lattice);
	        coll.addAll(e);
	    }

        return coll;
	}

	/**
	 * Getter fuer den FormOut-Knoten.
	 *
	 * @return
	 */
	public SecurityNode getNode() {
	    return fo;
	}

	public String toString() {
	    String str = "FO-Node "+fo+" induces the following Elements:\n";
	    for (HookInElement he : elems) {
	        str += he.toString()+"\n";
	    }
	    return str;
	}
}
