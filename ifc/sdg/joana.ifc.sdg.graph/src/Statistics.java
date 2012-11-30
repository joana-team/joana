/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

import java.util.Collection;
import java.util.HashSet;

import edu.kit.joana.ifc.sdg.graph.JoanaGraph;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;


public class Statistics {

    public static void main (String[] args) throws Exception {
//        for (String s : pdgs) {
    	for (String s : Sequentialize.pdgs) {
        	SDG g = SDG.readFrom(s);

        	System.out.println(s);
        	nodes(g);
        	edges(g);
        	procs(g);
        	threads(g);
        	System.out.println("\n");

//        	CFG cfg = ICFGBuilder.extractICFG(g);
//        	removeParams(cfg);
//        	System.out.println(s);
//        	nodes(cfg);
//        	edges(cfg);
//        	procs(cfg);
//        	threads(cfg);
//        	System.out.println("\n");
        }
    }

    public static void nodes(SDG g) {
    	System.out.println("Nodes: "+g.vertexSet().size());
    }

    public static void nodes(CFG g) {
    	int ctr = 0;
    	for (SDGNode n : g.vertexSet()) {
    		if (n.getKind() == SDGNode.Kind.ACTUAL_IN
    				|| n.getKind() == SDGNode.Kind.ACTUAL_OUT
    				|| n.getKind() == SDGNode.Kind.FORMAL_IN
    				|| n.getKind() == SDGNode.Kind.FORMAL_OUT)
    			continue;
    		ctr++;
    	}
    	System.out.println("Nodes: "+ctr);
    }

    public static void edges(SDG g) {
    	int x = 0;
    	for (SDGEdge e : g.edgeSet()) {
    		if (e.getKind().isSDGEdge()) x++;
    	}
    	System.out.println("Edges: "+x);
//    	System.out.println("Edges: "+g.edgeSet().size());
    }

    public static void edges(CFG g) {
    	int x = 0;
    	for (SDGEdge e : g.edgeSet()) {
    		SDGNode s = e.getSource();
    		SDGNode t = e.getTarget();

    		if ((e.getKind() == SDGEdge.Kind.CONTROL_FLOW)
    			&& (s.getKind() == SDGNode.Kind.ACTUAL_IN
    				|| s.getKind() == SDGNode.Kind.ACTUAL_OUT
    				|| s.getKind() == SDGNode.Kind.FORMAL_IN
    				|| s.getKind() == SDGNode.Kind.FORMAL_OUT))
    			continue;

    		x++;
    	}
    	System.out.println("Edges: "+x);
    }

    public static void procs(JoanaGraph g) {
    	int procs = 0;
    	for (SDGNode n : g.vertexSet()) {
    		if (n.getProc() > procs) procs = n.getProc();
    	}

    	System.out.println("Procs: "+(procs+1));
    }

    public static void threads(JoanaGraph g) {
    	System.out.println(g.getThreadsInfo());
    }

    private static void removeParams(CFG g) {
    	HashSet<SDGNode> set = new HashSet<SDGNode>();
    	for (SDGNode n : g.vertexSet()) {
    		if (n.getKind() == SDGNode.Kind.ACTUAL_IN
    				|| n.getKind() == SDGNode.Kind.ACTUAL_OUT
    				|| n.getKind() == SDGNode.Kind.FORMAL_IN
    				|| n.getKind() == SDGNode.Kind.FORMAL_OUT)
	    		set.add(n);
    	}

    	for (SDGNode n : set) {
    		Collection<SDGEdge> inc = g.incomingEdgesOf(n);
    		Collection<SDGEdge> out = g.outgoingEdgesOf(n);


    	}

    }
}
