/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.viewer.model;

import java.util.Collection;
import java.util.LinkedList;


import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ui.ifc.sdg.viewer.algorithms.Algorithm;

/**
 * Graph combines a Joana SDG with a CompilationUnit (here a Java source file) and a set of slicing algorithms.
 *
 * @author giffhorn
 */
public class Graph {
	// the Java source file
	private ICompilationUnit file;
	// the SDG
	private SDG graph;
	// path to sdg
	private String sdgPath;
	public String getSdgPath() {
		return sdgPath;
	}
	// the children
	private LinkedList<ChosenAlgorithm> children;


	/** Creates a new Graph object.
	 *
	 * @param sdg   A SDG.
	 * @param file  A Java source file.
	 */
	public Graph(SDG graph, ICompilationUnit file, String path) {
		this.graph = graph;
		this.file = file;
		this.sdgPath = path;
		children = new LinkedList<ChosenAlgorithm>();
	}

	public String getName() {
		return file.getElementName();
	}

	/** Returns the SDG.
	 *
	 * @return  The SDG.
	 */
	public SDG getGraph() {
		return graph;
	}

	/** Returns the nodes of the SDG.
	 *
	 * @return The nodes as a sorted Collection.
	 */
	public Collection<SDGNode> getNodes() {
		return graph.vertexSet();
	}

	/** Adds a new Child
	 *
	 * @param child  The new child.
	 */
	public void addChild(ChosenAlgorithm child) {
		children.add(child);
		child.setParent(this);
	}

	/*
	 * (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.Parent#removeChild(edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.TreeNode)
	 */
	public void removeChild(ChosenAlgorithm child) {
		children.remove(child);
		child.setParent(null);
	}

	/*
	 * (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.Parent#getChildren()
	 */
	public LinkedList<ChosenAlgorithm> getChildren() {
		return children;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.Parent#hasChildren()
	 */
	public boolean hasChildren() {
		return children.size() > 0;
	}

	public IProject getProject() {
		return file.getResource().getProject();
	}

	/** Returns true if this SDG already contains the given Algorithm.
	 * It checks if it already contains a child with the same name.
	 *
	 * @param alg  The algorithm.
	 */
	public boolean contains(Algorithm alg) {
		for (ChosenAlgorithm a : children) {
			if (a.getName().equals(alg.getName())) {
				return true;
			}
		}

		return false;
	}
}
