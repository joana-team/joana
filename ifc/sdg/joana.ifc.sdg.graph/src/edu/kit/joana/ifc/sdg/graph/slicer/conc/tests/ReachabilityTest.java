/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.graph.slicer.conc.tests;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.PDGs;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.tests.reach.ReachabilityChecker1;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.tests.reach.ReachabilityChecker2;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.tests.reach.ReachabilityChecker3;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.FoldedCFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.GraphFolder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;


/**
 *
 *
 * -- Created on November 7, 2007
 *
 * @author  Dennis Giffhorn
 */
@SuppressWarnings("unused")
public class ReachabilityTest {
	static class ContextPair {
		DynamicContext from;
		DynamicContext to;

		ContextPair(DynamicContext from, DynamicContext to) {
			this.from = from;
			this.to = to;
		}
	}

    /** the corresponding interprocedural control flow graph */
    private CFG icfg;
    /** the call graph (contains calls and entries) */
//    private BipartiteCallGraph call;
    /** the folded icfg for reachability checking purpose */
    private FoldedCFG foldedIcfg;
    /** The graph to be sliced. */
    private SDG sdg;
    /** The bipartit call graph of 'ipdg'. */
//    private FoldedBipartiteCallGraph foldedCall;

    /** realises the reachability checking algorithm */
	private ReachabilityChecker1 reachable1;
    private ReachabilityChecker2 reachable2;
    private ReachabilityChecker3 reachable3;
    private DynamicContextManager man;

    /**
     * Creates a new instance of ReachabilityTest
     */
    public ReachabilityTest(SDG graph) {
        sdg = graph;

//        call = CallGraphBuilder.buildBipartiteCallGraph(ipdg);
//        foldedCall = GraphFolder.foldCallGraph(call);
        man = new DynamicContextManager(sdg);

        // build the threaded ICFG
        icfg = ICFGBuilder.extractICFG(sdg);

        // fold ICFG with Krinke's two-pass folding algorithm
        foldedIcfg = GraphFolder.twoPassFolding(icfg);

        reachable1 = new ReachabilityChecker1(foldedIcfg);
        reachable2 = new ReachabilityChecker2(foldedIcfg);
        reachable3 = new ReachabilityChecker3(foldedIcfg);
    }

    public void test(int number) {
        List<ContextPair> crits = createCriteria(number);
        long r1 = 0l;
        long r2 = 0l;
        long r3 = 0l;
        long tmp = 0l;
        System.out.println("All criteria created");

        System.out.println("Starting reachability analysis...");

//        tmp = System.currentTimeMillis();
//        for (ContextPair p : crits) {
//        	System.out.print(".");
//			reachable1.reaches(p.from, p.to);
//        }
//        r1 = System.currentTimeMillis() - tmp;
//        System.out.println();

        tmp = System.currentTimeMillis();
        for (ContextPair p : crits) {
        	System.out.print(".");
			reachable2.reaches(p.from, p.to);
        }
        r2 = System.currentTimeMillis() - tmp;
        System.out.println();

        tmp = System.currentTimeMillis();
        for (ContextPair p : crits) {
			reachable3.reaches(p.from, p.to);
        }
        r3 = System.currentTimeMillis() - tmp;

        System.out.println("done!");
        System.out.println("- invocations : "+number);
        System.out.println("- Brute force : "+r1);
        System.out.println("- Guided      : "+r2);
        System.out.println("- Elegant     : "+r3);
    }

    private List<ContextPair> createCriteria(int nr) {
    	// pick random nodes
    	List<SDGNode> nodes = sdg.getNRandomNodes(nr);

    	// create all of their contexts
        List<DynamicContext> cons = new LinkedList<DynamicContext>();

        ProgressCounter ctr = new ProgressCounter(1);
        for (SDGNode node : nodes) {
        	for (Context c : man.getContextsOf(node, 0)) {
	        	DynamicContext mapped = map(c);
	        	cons.add(mapped);
        	}
        	ctr.tick();
        }
    	System.out.println();

        // pick random contexts
        LinkedList<ContextPair> result = new LinkedList<ContextPair>();

		Collections.shuffle(cons);
        cons = cons.subList(0, nr);

        List<DynamicContext> copy = new LinkedList<DynamicContext>();
        copy.addAll(cons);
		Collections.shuffle(copy);

		for (int i = 0; i < nr; i++) {
			ContextPair p = new ContextPair(cons.get(i), copy.get(i));
			result.add(p);
		}

		return result;
    }

    protected DynamicContext map(Context origin) {
        // list for building the context that maps to Context 'con'
        LinkedList<SDGNode> res = new LinkedList<SDGNode>();

        // for every vertex in the call stack of 'con' determine the fold vertex in graph 'to'
        for (SDGNode node : origin.getCallStack()) {

            // add found vertex to list 'res'
            if (node.getKind() == SDGNode.Kind.FOLDED) {

                // If the folded node is induced by a cycle of return edges,
                // and the topmost call site of 'context' is the folded call cycle
                // according to the return-cycle, create a context without this
                // topmost call site and add it to the initial worklist.
                if (node.getLabel() == GraphFolder.FOLDED_RETURN) {
                    // creates a context without the the folded call cycle
                    // according to the return-cycle
                    SDGNode call = returnCall(node);

                    if (res.size() == 0 || call != res.getLast()) {
                        res.addLast(call);
                    }
                }

                // maps node to the belonging node of the CFG
                SDGNode x = map(node);

                // prohibit redundant piling of Contexts
                if (res.size() == 0 || x != res.getLast()) {
                    res.addLast(x);
                }

            } else {
                res.addLast(foldedIcfg.map(node));
            }
        }

        SDGNode node = foldedIcfg.map(origin.getNode());
        if (res.size() > 0 && node == res.getLast()) {
            res.removeLast();
        }

        return new DynamicContext(res, node, origin.getThread());
    }

    protected SDGNode returnCall(SDGNode returnFold) {
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        SDGNode callFold = null;

        worklist.add(returnFold);

        // find the corresponding folded call cycle
        loop:
            while (!worklist.isEmpty()) {
                SDGNode next = worklist.poll();
                for (SDGEdge cf : foldedIcfg.getIncomingEdgesOfKind(next, SDGEdge.Kind.CONTROL_FLOW)) {
                    if (cf.getSource().getKind() == SDGNode.Kind.FOLDED
                            && cf.getSource().getLabel() == GraphFolder.FOLDED_CALL) {

                        callFold = cf.getSource();
                        break loop;
                    }
                    worklist.addFirst(cf.getSource());
                }
            }

        return callFold;
    }

    protected SDGNode map(SDGNode node) {
        // 1. get one of the unmapped nodes
        SDGNode unmapped = man.unmap(node);

        // 2. get mapping in the other graph
        return foldedIcfg.map(unmapped);
    }

    public static void main(String[] args) throws Exception {
        for (String str :  pdgs) {
	        SDG graph = SDG.readFrom(str);
	        ReachabilityTest test = new ReachabilityTest(graph);
	        System.out.println(str);
	        test.test(20);
        }
    }

    static String[] pdgs = {
//    	"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/Barcode.pdg",
//		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/Cellsafe.pdg",
//		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/conc.ac.AlarmClock.pdg",
//		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/conc.auto.EnvDriver.pdg",
//		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/conc.bb.ProducerConsumer.pdg",
//		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/conc.cliser.dt.Main.pdg",
//		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/conc.cliser.kk.Main.pdg",
		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/conc.daisy.DaisyTest.pdg"};//,
//		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/conc.dp.DiningPhilosophers.pdg",
//		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/conc.ds.DiskSchedulerDriver.pdg",
//		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/conc.lg.LaplaceGrid.pdg",
//		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/conc.sq.SharedQueue.pdg",
//		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/conc.TimeTravel.pdg",
//		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/GoldenSMS_KeyManagement.pdg",
//		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/GoldenSMS_Message.pdg",
//		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/GoldenSMS_Reception.pdg",
//		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/Guitar.pdg",
//		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/HyperM.pdg",
//		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/J2MESafe.pdg",
//		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/Logger.pdg",
//		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/Maza.pdg",
//		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/Podcast.pdg"};
}
