/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.violations.paths;

import java.util.Iterator;
import java.util.LinkedHashSet;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;


/**
 * Memorizes a path.
 *
 * @author giffhorn
 *
 */
public class Path<C extends Context> {
	private final C current;
	private LinkedHashSet<C> path = new LinkedHashSet<>();
	private int step = 0;

	public Path(C c) {
		path.add(c);
		current = c;
	}

	private Path(C c, LinkedHashSet<C> p, int s) {
		path = p;
		current = c;
		step = s;
	}

	public SDGNode getCurrentNode() {
		return current.getNode();
	}

	public C getCurrent() {
		return current;
	}

	public int getThread() {
		return current.getThread();
	}

	public int getStep() {
		return step;
	}

	public void incStep() {
		step++;
	}

	public boolean contains(Context con) {
		return path.contains(con);
	}

	@SuppressWarnings("unchecked")
	public Path<C> prepend(C con) {
		if (con == null) throw new NullPointerException();
		LinkedHashSet<C> l = (LinkedHashSet<C>) path.clone();
		l.add(con);
		return new Path<C>(con, l, step);
	}

	public ViolationPath convert() {
		ViolationPath v = new ViolationPath();

		for (Context c : path.toArray(new Context[]{})) {
			v.addFirst( (SecurityNode) c.getNode());
		}

		return v;
	}

	public String toString() {
		StringBuilder b = new StringBuilder();
		Iterator<C> i = path.iterator();

		while(i.hasNext()) {
			Context c = i.next();
			b.append(c.getNode().getId()+" <- ");
		}

		return b.toString();

	}
}
