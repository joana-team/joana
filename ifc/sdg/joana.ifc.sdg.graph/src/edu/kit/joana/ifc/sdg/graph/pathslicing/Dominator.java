/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.pathslicing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;


/**
 * Klasse zur Berechnung eines Dominatorgraphen aus einem Kontrollflussgraph
 * @author Martin Seidel
 *
 */
public final class Dominator {

	private Dominator() {}

	/**
	 * Liefert die rueckwaerts sortierte Post-Order darstellung eines Kontrollflussgraphen
	 * @param graph der Kontrollflussgraph
	 * @return eine sortierte Liste mit den Knoten des Graphen
	 */
	public static LinkedList<SDGNode> reversePostOrder(CFG graph) {

		LinkedList<SDGNode> temp = new LinkedList<SDGNode>();
		HashSet<SDGNode> set = new HashSet<SDGNode>();

		SDGNode top = graph.getRoot();
		if (top == null) {
			throw new RuntimeException("Root ist null");
		}

		postOrder(top, graph, set, temp);

		Iterator<SDGNode> it = temp.descendingIterator();
		LinkedList<SDGNode> ret = new LinkedList<SDGNode>();

		while (it.hasNext()) {
			ret.add(it.next()); //rueckwaerts umsortieren
		}

		return ret;
	}


	/*
	 * erstellt eine Post-Order darstellung eines CFG
	 */
	private static void postOrder(SDGNode n, CFG graph, HashSet<SDGNode> set, LinkedList<SDGNode> list) {

		for (SDGEdge e : graph.outgoingEdgesOf(n)) {
			if (set.add(e.getTarget())) {
				postOrder(e.getTarget(), graph, set, list);
			}
		}

		if (!list.contains(n)) {
			list.add(n);
		}

	}

	/**
	 * Liefert die umgekehrte rueckwaerts sortierte Post-Order darstellung eines Kontrollflussgraphen
	 * @param graph der Kontrollflussgraph
	 * @return eine sortierte Liste mit den Knoten des Graphen
	 */
	public static LinkedList<SDGNode> postReversePostOrder(CFG graph) {

		LinkedList<SDGNode> temp = new LinkedList<SDGNode>();
		HashSet<SDGNode> set = new HashSet<SDGNode>();

		SDGNode top = graph.getRoot();
		if (top == null) {
			throw new RuntimeException("Root ist null");
		}

		while (graph.outDegreeOf(top) > 0) {  //Exit Knoten des graphen finden
			for (SDGEdge e : graph.outgoingEdgesOf(top)) {
				if (set.add(e.getTarget())) {
					top = e.getTarget();
					break;
				}
			}
		}

		set.clear();
		postPostOrder(top, graph, set, temp);

		Iterator<SDGNode> it = temp.descendingIterator();
		LinkedList<SDGNode> ret = new LinkedList<SDGNode>();

		while (it.hasNext()) {
			ret.add(it.next()); //rueckwaerts umsortieren
		}

		return ret;
	}

	/*
	 * erstellt eine umgekehrte Post-Order darstellung eines CFG
	 */
	private static void postPostOrder(SDGNode n, CFG graph, HashSet<SDGNode> set, LinkedList<SDGNode> list) {

		for (SDGEdge e : graph.incomingEdgesOf(n)) {
			if (set.add(e.getSource())) {
				postPostOrder(e.getSource(), graph, set, list);
			}
		}

		if (!list.contains(n)) {
			list.add(n);
		}
	}

	/**
	 * Liefert fuer jeden Knoten ein Set mit Dominierten Knoten
	 * @param g Der CFG auf den der Algorithmus anfewandt wird.
	 * @return Eine HashMap mit den Zuordnungen
	 */
	public static CFG dominator(CFG g) {
		HashMap<SDGNode, Set<SDGNode>> dom = new HashMap<SDGNode, Set<SDGNode>>();
		for (SDGNode n : g.vertexSet()) {
			dom.put(n, new HashSet<SDGNode>(g.vertexSet()));
		}
		boolean changed = true;

		LinkedList<SDGNode> rorder = reversePostOrder(g);

		while (changed) {
			changed = false;
			for (SDGNode n : rorder) {
				Set<SDGNode> new_set = null;
				for (SDGEdge e : g.incomingEdgesOf(n)) { //Schnittmenge der vorgaenger
					SDGNode p = e.getSource();
					if (new_set == null) {
						new_set = new HashSet<SDGNode>(dom.get(p));
					} else {
						new_set.retainAll(dom.get(p));
					}
				}
				if (new_set == null) {
					new_set = new HashSet<SDGNode>();
				}
				new_set.add(n);
				if (!new_set.equals(dom.get(n))) {
					dom.put(n, new_set);
					changed = true;
				}
			}
		}

		CFG ret = new CFG();
		ret.addAllVertices(g.vertexSet());
		ret.setRoot(g.getRoot());
		for (SDGNode n : rorder) {
			for (SDGNode d : dom.get(n)) {
				if (d == n) {
					continue;
				}
				SDGEdge e =  SDGEdge.Kind.CONTROL_FLOW.newEdge(d, n);
				ret.addEdge(d, n, e);
			}
			for (SDGNode d : dom.get(n)) { //Nicht direkte Kannten entfernen
				if (d == n) {
					continue;
				}
				for (SDGNode dd : dom.get(d)) {
					if (dd == d) {
						continue;
					}
					ret.removeEdge(dd, n);
				}
			}
		}

		return ret;
	}


	/**
	 * Liefert fuer jeden Knoten ein Set mit PostDominierten Knoten
	 * @param g Der CFG auf den der Algorithmus anfewandt wird.
	 * @return Eine HashMap mit den Zuordnungen
	 */
	public static HashMap<SDGNode, Set<SDGNode>> postDominator(CFG g) {
		HashMap<SDGNode, Set<SDGNode>> dom = new HashMap<SDGNode, Set<SDGNode>>();
		for (SDGNode n : g.vertexSet()) {
			dom.put(n, new HashSet<SDGNode>(g.vertexSet()));
		}
		boolean changed = true;

		LinkedList<SDGNode> rorder = postReversePostOrder(g);

		while (changed) {
			changed = false;
			for (SDGNode n : rorder) {
				Set<SDGNode> new_set = null;
				for (SDGEdge e : g.outgoingEdgesOf(n)) { //Schnittmenge der vorgaenger
					SDGNode p = e.getTarget();
					if (new_set == null) {
						new_set = new HashSet<SDGNode>(dom.get(p));
					} else {
						new_set.retainAll(dom.get(p));
					}
				}
				if (new_set == null) {
					new_set = new HashSet<SDGNode>();
				}
				new_set.add(n);
				if (!new_set.equals(dom.get(n))) {
					dom.put(n, new_set);
					changed = true;
				}
			}
		}

		CFG ret = new CFG();
		ret.addAllVertices(g.vertexSet());
		ret.setRoot(g.getRoot());
		for (SDGNode n : rorder) {
			for (SDGNode d : dom.get(n)) {
				if (d == n) {
					continue;
				}
				SDGEdge e =  SDGEdge.Kind.CONTROL_FLOW.newEdge(d, n);
				ret.addEdge(d, n, e);
			}
			for (SDGNode d : dom.get(n)) { //Nicht direkte Kannten entfernen
				if (d == n) {
					continue;
				}
				for (SDGNode dd : dom.get(d)) {
					if (dd == d) {
						continue;
					}
					ret.removeEdge(dd, n);
				}
			}
		}

		return dom;
	}

}
