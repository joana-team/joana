/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.pathslicing;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.Chopper;
import edu.kit.joana.ifc.sdg.graph.chopper.SameLevelICFGChopper;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;


/**
 * PathSlicer class for doing a PathSlice on a CFG
 * @author Martin
 *
 */
public class PathSlicer {

	VariableMapReader rdr;
	CFG graph;
	SameLevelICFGChopper chopper;

	/**
	 * Constructor of the PathSLicer
	 * @param rdr a variableMapReader
	 * @param graph the cfg where to slice on
	 */
	public PathSlicer(VariableMapReader rdr, CFG graph) {
		this.rdr = rdr;
		this.graph = graph;
		this.chopper = new SameLevelICFGChopper(graph);
	}

	/**
	 * Does a pathSLice on a given path
	 * @param path path on which to perform the Pathslice
	 * @return pathsliced Path
	 */
	public List<SDGNode> pathSlice(List<SDGNode> path) {

		LinkedList<SDGNode> ret = new LinkedList<SDGNode>();
		int pos = path.size() - 1;
		if (pos < 0) {
			return path; //Pfad enthaelt kein element
		}
		SDGNode old = path.get(pos);
		ret.push(old);
		LinkedList<Integer> lVar = new LinkedList<Integer>(); //Liste mit den gelesenen Variablen
		lVar.addAll(rdr.getUse(old.getId()));
		for (int i : rdr.getRef(old.getId())) {
			lVar.addAll(rdr.getPto(i));
		}
		pos--;

		while (pos > 0)	{

			SDGNode e = path.get(pos);
 			if (take(path, lVar, old, e)) { // ueberpruefen der 3 Bedingungen
				ret.push(e);				// Eine Bedingung erfuellt: zum ergebnis hinzufuegen und
				old = e;
				lVar.removeAll(rdr.getDef(old.getId())); // Liste der gelesenen Variablen updaten
				for (int i : rdr.getMod(old.getId())) {
					lVar.removeAll(rdr.getPto(i));
				}
				lVar.addAll(rdr.getUse(old.getId()));
				for (int i : rdr.getRef(old.getId())) {
					lVar.addAll(rdr.getPto(i));
				}
			}
			pos--;
		}

		return ret;
	}

	/*
	 * ueberprueft Die aktuelle Kante auf die 3 Bedingungen
	 */
	private boolean take(List<SDGNode> path, LinkedList<Integer> lVar, SDGNode old, SDGNode n) {

		HashSet<Integer> temp = new HashSet<Integer>(rdr.getDef(n.getId()));
		for (int i : rdr.getRef(n.getId())) {
			temp.addAll(rdr.getPto(i));
		}
		temp.retainAll(lVar);
		if (!temp.isEmpty()) {	// 1. Eine der gelesenen Variablen wird geschrieben
			return true;
		} else if (!Dominator.postDominator(graph).get(n).contains(old)) { // 2. Der alte Knoten postDominiert nicht den aktuellen
			return true;
		} else { // 3. Auf einem alternativen Pfad wird eine entsprechende Variable geschrieben
			if (!Chopper.testSameLevelSetCriteria(Collections.singleton(n), Collections.singleton(old))) {
				if (n.getKind() == SDGNode.Kind.CALL && old.getKind() == SDGNode.Kind.ENTRY) {
					return true;
				}
				return false;
			}
			Collection<SDGNode> chop = chopper.chop(n, old);
			chop.removeAll(clearPathNodes(path, n, old));
			for (SDGNode m : chop) {
				temp = new HashSet<Integer>(rdr.getDef(m.getId()));
				for (int i : rdr.getRef(m.getId())) {
					temp.addAll(rdr.getPto(i));
				}
				temp.retainAll(lVar);
				if (!temp.isEmpty()) {
					System.out.println(3);
					return true;
				}
			}
		}
		return false;
	}

	/*
	 * Erstellt einen Teilpfad von Knoten n bis Knoten old
	 */
	private LinkedList<SDGNode> clearPathNodes(List<SDGNode> path, SDGNode n, SDGNode old) {
		SDGNode t = n;
		int i = path.indexOf(n);
		LinkedList<SDGNode> ret = new LinkedList<SDGNode>();
		ret.add(n);

		while (t != old) {
			t = path.get(i++);
			ret.add(t);
		}

		return ret;
	}
}
