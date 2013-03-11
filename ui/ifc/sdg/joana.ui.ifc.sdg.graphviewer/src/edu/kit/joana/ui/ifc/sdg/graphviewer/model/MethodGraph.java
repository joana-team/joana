/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * MethodGraph.java
 *
 * Created on 17. Oktober 2005, 17:10
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.model;

import java.util.Hashtable;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


/**
 * This class represents a procedure dependence graph (PDG). A PDG is created
 * from a system dependence graph (SDG). The PDG is in the form of a JGraph
 * component.
 *
 * @author Siegfried Weber
 */
public class MethodGraph extends Graph {

	/**
	 * the PDG
	 */
	private SDG method;
	/**
	 * the proc ID of this PDG
	 */
	private int procID;
	/**
	 * the method names of all PDGs
	 */
	// private List<String> titles;
	private String name;
	private SDG completeSDG;
	private Hashtable<Integer, String> linkedMethods;

	/**
	 * Creates a new instance of MethodGraph.
	 *
	 * @param method
	 *            a PDG
	 * @param procID
	 *            a proc ID
	 * @param titles
	 *            a list of method names
	 */
	public MethodGraph(SDG sdg, int procID) {
		this.completeSDG = sdg;
		this.procID = procID;
		linkedMethods = new Hashtable<Integer, String>();
		init(sdg);
	}

	public SDG getCompleteSDG() {
		return completeSDG;
	}

	private void init(SDG sdg) {
		method = new SDG();
		name = "";

		Set<SDGNode> nodes = sdg.vertexSet();

		// nodes
		for (SDGNode n : nodes) {
			int currentProc = n.getProc();

			// pick nodes that belong to chosen method
			if (currentProc == procID) {
				method.addVertex(n);

				if (n.getKind().equals(SDGNode.Kind.ENTRY)) {
					name = n.getLabel().toString();
				}

			} else if (n.getKind().equals(SDGNode.Kind.ENTRY)) {
				linkedMethods.put(n.getProc(), n.getLabel().toString());
			}
		}

		// edges
		for (SDGEdge edge : sdg.edgeSet()) {
			boolean sourceInProc = edge.getSource().getProc() == procID;
			boolean targetInProc = edge.getTarget().getProc() == procID;
			SDGNode node = null;
			if (sourceInProc && !targetInProc){
				node = edge.getTarget();
			}
			if (!sourceInProc && targetInProc){
				node = edge.getSource();
			}
			if (node != null)
				method.addVertex(node);

			if (sourceInProc || targetInProc)
				method.addEdge(new UniqueSDGEdge(edge.getSource(), edge.getTarget(), edge.getKind(), edge.getLabel()));
		}

		method.setName(name);
		method.setEdgeFactory(UniqueSDGEdge.FACTORY);
	}

	/**
	 * Returns the PDG.
	 *
	 * @return the PDG
	 */
	public SDG getSDG() {
		return method;
	}

	/**
	 * Returns the proc ID.
	 *
	 * @return the proc ID
	 */
	public int getProcID() {
		return procID;
	}

	/**
	 * Returns the method name of the specified proc ID.
	 *
	 * @param proc
	 *            a proc ID
	 * @return the method name of the specified proc ID
	 */
	public String getTitle(int proc) {
		if (proc == procID) {
			return name;

		} else {
			return linkedMethods.get(proc);
		}
	}

	/**
	 * Returns the method name.
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


	@Override
	public String toString() {
		return method.toString();
	}
}
