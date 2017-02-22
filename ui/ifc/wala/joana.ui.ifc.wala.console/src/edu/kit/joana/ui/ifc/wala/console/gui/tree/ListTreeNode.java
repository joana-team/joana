/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.gui.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import edu.kit.joana.api.sdg.SDGProgramPart;



public class ListTreeNode<E extends SDGProgramPart> extends IFCTreeNode {

	private static final long serialVersionUID = -5211579612433666728L;
	private final List<E> list = new ArrayList<E>();
	private final String name;

	public ListTreeNode(Collection<E> things, boolean annotatable, String name, Kind kind) {
		this(things, null, annotatable, name, kind);
	}

	public ListTreeNode(Collection<E> things, Comparator<E> thingsComp, boolean annotatable, String name, Kind kind) {
		super(things, true, annotatable, kind);
		this.list.addAll(things);
		if (thingsComp != null) {
			Collections.sort(this.list, thingsComp);
		}
		this.name = name;
	}

	@Override
	public String toStringPrefix() {
		return name + " (" + list.size() + ")";
	}

	protected boolean matchesPart(SDGProgramPart part) {
		return false;
	}
}
