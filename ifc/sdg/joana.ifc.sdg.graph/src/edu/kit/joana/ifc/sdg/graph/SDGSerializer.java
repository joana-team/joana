/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on May 14, 2004
 *
 */
package edu.kit.joana.ifc.sdg.graph;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import gnu.trove.iterator.TIntIterator;


/**
 * @author hammer
 *
 */
public final class SDGSerializer {

    private SDGSerializer() {}

    static Iterator<SDGNode> orderedNodes(JoanaGraph g) {
        SortedSet<SDGNode> set = new TreeSet<SDGNode>(SDGNode.getIDComparator());
        set.addAll(g.vertexSet());
        return set.iterator();
    }

    public static String toPDGFormat(JoanaGraph g) {
    	StringWriter string = new StringWriter();
    	toPDGFormat(g, new PrintWriter(string));
    	return string.toString();
    }

    public static void toPDGFormat(JoanaGraph g, OutputStream out) {
    	PrintWriter pw = new PrintWriter(out);
    	toPDGFormat(g, pw);
    }

    public static void toPDGFormat(JoanaGraph g, PrintWriter pw) {
        pw.print("SDG ");

        if (g.getName() != null) {
            pw.print("\"");pw.print(g.getName());pw.print("\" ");
        }

        pw.print("{\n");

        if (g instanceof SDG && ((SDG)g).getJoanaCompiler()) {
            pw.print("JComp\n");
        }

        for (Iterator<SDGNode> iter = orderedNodes(g); iter.hasNext();) {
            SDGNode n = iter.next();

            pw.print(n.getKind().toString());
            pw.print(" ");
            pw.print(n.getId());
            pw.print(" {\n");
            pw.print("O ");
            pw.print(n.getOperation());
            pw.print(";\n");

            if (n.getLabel() != null) {
                pw.print("V \"");pw.print(n.getLabel());pw.print("\";\n");
            }

            if (n.getType() != null) {
                pw.print("T \"");pw.print(n.getType());pw.print("\";\n");
            }

            pw.print("P ");
            pw.print(n.getProc());
            pw.print(";\n");

            if (n.getSource() != null) {
                pw.print("S \"");pw.print(n.getSource());pw.print("\":");
                pw.print(n.getSr());
                pw.print(',');
                pw.print(n.getSc());
                pw.print('-');
                pw.print(n.getEr());
                pw.print(',');
                pw.print(n.getEc());
                pw.print(";\n");
            }

            if (n.getBytecodeName() != null) {
            	pw.print("B \""); pw.print(n.getBytecodeName()); pw.print("\":");
            	pw.print(n.getBytecodeIndex());
                pw.print(";\n");
            }

            if (n.getThreadNumbers() != null && n.getThreadNumbers().length > 0) {
                pw.print("Z ");
                pw.print(n.getThreadNumbers()[0]);

                for (int i = 1; i < n.getThreadNumbers().length; i++) {
                    pw.print(", "+ n.getThreadNumbers()[i]);
                }

                pw.print(";\n");
            }

            final int[] allocSites = n.getAllocationSites();
            if (allocSites != null && allocSites.length > 0) {
                pw.print("A ");
                pw.print(allocSites[0]);

                for (int i = 1; i < allocSites.length; i++) {
                    pw.print(", "+ allocSites[i]);
                }

                pw.print(";\n");
            }

            if (n.mayBeNonTerminating()) {
            	pw.print("N;\n");
            }

            if (n.getAliasDataSources() != null) {
            	pw.print("D ");
            	final TIntIterator it = n.getAliasDataSources().iterator();
            	while (it.hasNext()) {
            		final int id = it.next();
           			pw.print(it.hasNext() ? id + ", " : id);
            	}
            	pw.print(";\n");
            }

            if (n.getClassLoader() != null) {
            	pw.print("C \"" + n.getClassLoader() + "\";\n");
            }

            if (n.getUnresolvedCallTarget() != null) {
                pw.print("U \"" + n.getUnresolvedCallTarget() + "\";\n");
            }
            
            final String[] localDefNames = n.getLocalDefNames();
            if (localDefNames != null && localDefNames.length > 0) {
                pw.print("LD [");
                pw.print("\""  + localDefNames[0] + "\"");

                for (int i = 1; i < localDefNames.length; i++) {
                	assert (localDefNames[i] == null || !localDefNames[i].contains("\""));
                    pw.print(", \"" + localDefNames[i] + "\"");
                }

                pw.print("];\n");
            }
            
            final String[] localUseNames = n.getLocalUseNames();
            if (localUseNames != null && localUseNames.length > 0) {
                pw.print("LU [");
                pw.print("\""  + localUseNames[0] + "\"");

                for (int i = 1; i < localUseNames.length; i++) {
                	// until wala fixes its local variable name resolution, we have to deal with null names :/
                	assert (localUseNames[i] == null || !localUseNames[i].contains("\""));
                    pw.print(", \"" + localUseNames[i] + "\"");
                }

                pw.print("];\n");
            }

            printPDGDependencies(g,n, pw);
            pw.print("}\n");
        }

        if (g.getThreadsInfo() != null)
            for (ThreadInstance ti : g.getThreadsInfo()) {
                pw.print(ti);
            }

        pw.print("}\n");
        pw.close();
    }

    private static void printPDGDependencies(JoanaGraph g, SDGNode n, PrintWriter pw) {
        for (SDGEdge e : g.outgoingEdgesOf(n)) {
            SDGNode node = e.getTarget();
            String kind = e.getKind().toString();
            pw.print(kind + " " + node.getId());
            if (e.getLabel() != null) {
                pw.print(": \"" + e.getLabel() + "\"");
            }
            pw.print(";\n");
        }
    }

}
