/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.function.Function;

import org.jgrapht.DirectedGraph;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.NodeDecorator;

/**
 * Write jgrapht and wala graphs to .dot format.
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class WriteGraphToDot {

    private WriteGraphToDot() {}

    public static File writeCfgToDot(final ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> cfg, final IR ir,
    		final String title, final String fileName) {
		return writeDotFile(cfg, new NodeDecorator<IExplodedBasicBlock>() {
			@Override
			public String getLabel(IExplodedBasicBlock bb) throws WalaException {
				String phis = "";
				for (final Iterator<SSAPhiInstruction> phit = bb.iteratePhis(); phit.hasNext();) {
					final SSAPhiInstruction phi = phit.next();
					phis += PrettyWalaNames.instr2string(ir, phi, ir.getControlFlowGraph()) + "|";
				}
				
				if (bb.isEntryBlock()) {
					return (phis.isEmpty() ? "ENTRY" : "ENTRY (" + phis + ")");
				} else if (bb.isExitBlock()) {
					return (phis.isEmpty() ? "EXIT" : "EXIT (" + phis + ")");
				} else if (bb.getInstruction() == null) {
					return (phis.isEmpty() ? "nop-"+bb.getNumber() : "nop-"+bb.getNumber()+ "(" + phis + ")");
				} else {
					final String instr = PrettyWalaNames.instr2string(ir, bb.getInstruction(), ir.getControlFlowGraph());
					final String escaped = instr.replace("\"","\\\"");
					return (phis.isEmpty() ? escaped : "(" + phis +") " + escaped);
				}
			}
		}, title, fileName);
    }
    
    public static <T> File writeDotFile(Graph<T> g, NodeDecorator<T> labels, String title, String dotfile) {
    	File f = null;
    	
    	try { 
    		f = DotUtil.writeDotFile(g, labels, title, dotfile);
    	} catch (WalaException e) {
    		e.printStackTrace();
    	}
    	
    	return f;
    }
    
    public static <V, E> void write(DirectedGraph<V, E> g, String fileName) throws FileNotFoundException {
    	EdgeFilter<E> filter = new EdgeFilter<E>() {
			public boolean accept(E edge) {
				return true;
			}
		};

    	write(g, fileName, filter, v -> getId(v));
    }

    public static <V, E> void write(DirectedGraph<V, E> g, String fileName, EdgeFilter<E> filter, Function<V, String> idProvider) throws FileNotFoundException {
        PrintWriter out = new PrintWriter(fileName);

        //System.out.println("Writing '" + fileName + "'");

        out.print("digraph \"DirectedGraph\" { \n graph [label=\"");
        out.print(g.toString());
        out.print("\", labelloc=t, concentrate=true]; ");
        out.print("center=true;fontsize=12;node [fontsize=12];edge [fontsize=12]; \n");

        for (V node : g.vertexSet()) {
            out.print("   \"");
            out.print(idProvider.apply(node));
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
                out.print(idProvider.apply((src)));
                out.print("\" -> \"");
                out.print(idProvider.apply((tgt)));
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

    public static String sanitizeFileName(final String string) {
        return string.replace(';', '_').replace('/', '.').replace('\\', '_').replace('>', '_').replace('<', '_');
    }
    
    public static String fixupDirectoryName(final String dir) {
    	if (dir == null || dir.isEmpty()) {
    		return "";
    	}
    	
    	if (!dir.endsWith(File.separator) && !dir.endsWith("/")) {
    		return dir + File.separator;
    	}
    	
    	return dir;
    }

}
