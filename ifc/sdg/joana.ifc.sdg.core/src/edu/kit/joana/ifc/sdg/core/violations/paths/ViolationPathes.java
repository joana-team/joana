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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import edu.kit.joana.ifc.sdg.core.SecurityNode;

/**
 * Represents a bulk of Violation Pathes
 * Offers method to merge with other ViolationPathes
 *
 * @author naxan
 *
 */
public class ViolationPathes {

	ArrayList<ViolationPath> vpathes = new ArrayList<ViolationPath>();

	/**
	 * returns a ArrayList containing copies of all ViolationPath objects<br>
	 * present in this ViolationPathes
	 * @return ArrayList of ViolatonPath
	 */
	public ArrayList<ViolationPath> getPathesListCopy() {
		ArrayList<ViolationPath> ret = new ArrayList<ViolationPath>();
		for (int i = 0; i < vpathes.size(); i++) {
			ret.add((ViolationPath)(vpathes.get(i)).clone());
		}
		return ret;
	}

	/**
	 * returns the internal ArrayList containing all ViolationPath objects<br>
	 * present in this ViolationPathes
	 * @return ArrayList of ViolatonPath
	 */
	public ArrayList<ViolationPath> getPathesList() {
		return vpathes;
	}

	/**
	 * Adds ViolationPath vp to internal ArrayList if not already there
	 * @param vp ViolationPath to add
	 */
	public boolean add(ViolationPath vp) {
		if (vpathes.contains(vp)) {
			return false;
		}
		
		vpathes.add(vp);
		return true;
	}

	public boolean remove(ViolationPath vp) {
		return vpathes.remove(vp);
	}

	public boolean isEmpty() {
		return vpathes.isEmpty();
	}

	/**
	 * @return human-readable String representing this ViolationPathes
	 */
	public String toString() {
		StringBuffer ret = new StringBuffer();
		for (int i = 0; i < vpathes.size(); i++) {
			ret.append(vpathes.get(i).toString());
			ret.append("\n");
		}
		return ret.toString();
	}

	/**
	 * @return LinkedList containing every node contained in some ViolationPath
	 */
	public Collection<SecurityNode> getAllInvolvedNodes() {
		Collection<SecurityNode> ret = new HashSet<SecurityNode>();
		for (int i = 0; i < vpathes.size(); i++) {
			ret.addAll(vpathes.get(i).getAllInvolvedNodes());
		}
		return ret;
	}
}
