/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.graphs;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.jgrapht.DirectedGraph;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class WriteGraphToDot {

    private WriteGraphToDot() {}

    public static <V, E> void write(DirectedGraph<V, E> g, String fileName) throws FileNotFoundException {
    	EdgeFilter<E> filter = new EdgeFilter<E>() {
			public boolean accept(E edge) {
				return true;
			}
		};

    	write(g, fileName, filter);
    }

    public static <V, E> void write(DirectedGraph<V, E> g, String fileName, EdgeFilter<E> filter) throws FileNotFoundException {
        PrintWriter out = new PrintWriter(fileName);

        //System.out.println("Writing '" + fileName + "'");

        out.print("digraph \"DirectedGraph\" { \n graph [label=\"");
        out.print(g.toString());
        out.print("\", labelloc=t, concentrate=true]; ");
        out.print("center=true;fontsize=12;node [fontsize=12];edge [fontsize=12]; \n");

        for (V node : g.vertexSet()) {
            out.print("   \"");
            out.print(getId(node));
            out.print("\" ");
            out.print("[label=\"");
            out.print(node.toString());
            out.print("\" shape=\"box\" color=\"blue\" ] \n");
        }

        for (V src : g.vertexSet()) {
            for (E e : g.outgoingEdgesOf(src)) {
            	if (!filter.accept(e)) {
            		continue;
            	}

                V tgt = g.getEdgeTarget(e);

                out.print(" \"");
                out.print(getId(src));
                out.print("\" -> \"");
                out.print(getId(tgt));
                out.print("\" ");
                out.print("[label=\"");
                out.print(e.toString());
                out.print("\"]\n");
            }
        }

        out.print("\n}");

        out.flush();
        out.close();
    }

    private static String getId(Object o) {
        return "" + System.identityHashCode(o);
    }

    public static String sanitizeFileName(String string) {
        return string.replace(';', '_').replace('/', '.').replace('\\', '_').replace('>', '_').replace('<', '_');
    }

}
