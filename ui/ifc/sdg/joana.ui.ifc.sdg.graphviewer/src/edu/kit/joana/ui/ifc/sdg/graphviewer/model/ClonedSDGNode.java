/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.model;

import edu.kit.joana.ifc.sdg.graph.SDGNode;

/** Klasse, um SDGNodes zu klonen.
 * Situation: Link-Knoten im PDG, die mehrfach verwendet werden,
 *            muessen geklont werden, damit die Parameter-Kanten
 *            nicht kreuz und quer laufen
 * Problem:   JGraphT akzeptiert keine geklonten SDGNodes, wenn deren IDs
 *            uebereinstimmen -> NullPointerExceptions
 * Loesung:   ClonedSDGNode kann eine andere ID erhalten und gaukelt der
 *            graphischen Darstellung die geklonte ID vor.
 *
 *
 * -- Created on August 1, 2007
 *
 * @author  Dennis Giffhorn
 */
public class ClonedSDGNode extends SDGNode {
    // die ID des Klonvaters
    private final int clonedID;

    /**
     * Creates a new instance of ClonedSDGNode
     * @param node  Der Klonvater.
     * @param ID    Eine eindeutige, neue ID.
     */
    public ClonedSDGNode(SDGNode node, int ID) {
        // fast identischer Klon, bis auf die ID
        super(node.getKind(), ID, node.getOperation(), node.getLabel(),
                node.getProc(), node.getType(), node.getSource(),
                node.getSr(), node.getSc(), node.getEr(), node.getEc(),
                node.getBytecodeName(), node.getBytecodeIndex());

        // die ID von node lokal merken
        clonedID = node.getId();
    }

    // toString ueberschreiben, sodass clonedID angezeigt wird.
    @Override
	public String toString() {
        return String.valueOf(clonedID);
    }
}
