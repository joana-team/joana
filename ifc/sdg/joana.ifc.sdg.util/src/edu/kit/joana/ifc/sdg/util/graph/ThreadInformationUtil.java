/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.util.graph;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;

/**
 * Some utility methods that deal with Threads and their Creation-Relation 
 */
public class ThreadInformationUtil {

	public static boolean isAncestor(final ThreadInstance ti1, final ThreadInstance ti2) {
		if (ti1.getThreadContext() == null) {
			return true;
		}
		if (ti2.getThreadContext() == null) {
			return false;
		}
		return isSuffixOf(ti1.getThreadContext(), ti2.getThreadContext())
				&& !ti1.getThreadContext().equals(ti2.getThreadContext());
	}

	public static <A> boolean isSuffixOf(final List<A> ls1, final List<A> ls2) {
		if (ls1 == null) {
			return true;
		}
		if (ls2 == null) {
			return false;
		}
		if (ls1.size() > ls2.size()) {
			return false;
		}
		final List<A> ls1Rev = new LinkedList<A>(ls1);
		final List<A> ls2Rev = new LinkedList<A>(ls2);
		Collections.reverse(ls1Rev);
		Collections.reverse(ls2Rev);
		return isPrefixOf(ls1Rev, ls2Rev);
	}

	public static <A> boolean isPrefixOf(final List<A> ls1, final List<A> ls2) {
		if (ls1 == null) {
			return true;
		}
		if (ls2 == null) {
			return false;
		}
		if (ls1.size() > ls2.size()) {
			return false;
		}
		final Iterator<A> iter1 = ls1.iterator();
		final Iterator<A> iter2 = ls2.iterator();
		while (iter1.hasNext()) {
			final A x1 = iter1.next();
			final A x2 = iter2.next();
			if (!x1.equals(x2)) {
				return false;
			}
		}
		return true;
	}

	public static DirectedGraph<ThreadInstance, DefaultEdge> buildThreadCreationTree(
			final ThreadsInformation threadInfo) {
		final DirectedGraph<ThreadInstance, DefaultEdge> tct = new DefaultDirectedGraph<ThreadInstance, DefaultEdge>(
				DefaultEdge.class);
		for (final ThreadInstance ti1 : threadInfo) {
			if (ti1.getThreadContext().isEmpty()) {
				continue;
			}
			ThreadInstance lowestAnc = null;
			for (final ThreadInstance ti2 : threadInfo) {
				if ((lowestAnc == null) || (isAncestor(ti2, ti1) && isAncestor(lowestAnc, ti2))) {
					lowestAnc = ti2;
				}
			}
			tct.addVertex(lowestAnc);
			tct.addVertex(ti1);
			tct.addEdge(lowestAnc, ti1);
		}
		return tct;
	}

}
