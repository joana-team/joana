/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on 21.12.2004
 *
 */
package edu.kit.joana.ifc.sdg.core.violations.paths;

import java.util.Collection;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.core.SecurityNode;


/***
 * Represents a single ViolationPath
 * internal representation is done as LinkedList of SDGNode
 * @author naxan
 *
 */
public class ViolationPath implements Cloneable {
	private int[] stmtList;
	private final LinkedList<SecurityNode> path = new LinkedList<SecurityNode>();

	public ViolationPath() {}

	/**
	 * use given path as internal representation
	 * @param path LinkedLiost of SDGNodes
	 */
	public ViolationPath(LinkedList<SecurityNode> path) {
		this.path.addAll(path);
	}

	/**
	 * Adds a node the to path
	 * @param node
	 */
	public void add(SecurityNode node) {
		path.add(node);
	}

	/**
	 * Adds a node the to path
	 * @param node
	 */
	public void addFirst(SecurityNode node) {
		path.addFirst(node);
	}

	/**
	 *
	 * @return List of nodes as they are in path
	 */
	public LinkedList<SecurityNode> getPathList() {
		return new LinkedList<SecurityNode>(path);
	}

	/**
	 * compares to another ViolationPath
	 * @return true, iff internal LinkedLists equal each other
	 */
	@Override
	public boolean equals(Object comp) {
		if (comp instanceof ViolationPath) {
			return ((ViolationPath)comp).getPathList().equals(path);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
	    return getPathList().hashCode();
	}

	/**
	 * @return human-readable String representation of ViolationPath
	 */
	@Override
	public String toString() {
		StringBuffer ret = new StringBuffer();

		for (SecurityNode n : path) {
			ret.append(n.getId());
			ret.append(" -> ");
		}
		ret.delete(ret.length()-4, ret.length());
		return ret.toString();
	}

	/**
	 * generates a clone
	 * also clones internal representation
	 */
	@Override
	public Object clone() {
		return new ViolationPath(path);
	}

	public Collection<SecurityNode> getAllInvolvedNodes() {
		return java.util.Collections.unmodifiableCollection(path);
	}

	public int[] reduceToStatements() {
		if (stmtList == null) {
			stmtList = createStmtList();
		}

		return stmtList;
	}

	private int[] createStmtList() {
		LinkedList<Integer> l = new LinkedList<Integer>();
		int top = -1;

		for (SecurityNode n : path) {
			int line = n.getSr();

			if (line != top) {
				l.add(line);
				top = line;
			}
		}

		int[] array = new int[l.size()];

		for (int i = 0; i < array.length; i++) {
			array[i] = l.get(i);
		}

		return array;
	}
}
