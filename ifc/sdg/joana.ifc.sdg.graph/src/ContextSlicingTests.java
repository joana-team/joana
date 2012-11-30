/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import edu.kit.joana.ifc.sdg.graph.PDGs;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.C2PBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.ContextManager;


public class ContextSlicingTests {
	static SDG g;


//    public static void main (String[] args) throws Exception {
//    	// command line configuration
//    	String file = args[0];
//    	int algs = Integer.parseInt(args[1]);
//    	test(file, algs);
//    }

    public static void main (String[] args) throws Exception {
        // explicit configuration
//    	int id = Integer.parseInt(args[0]);
    	String file = PDGs.pdgs[7];	// 5 DayTime, 7 DaisyTest, 13 Key, 15 Rec
    	System.out.println(file);
        /* 1 */
        g = SDG.readFrom(file);
//        ThreadInstance ti = g.getThreadsInfo().iterator().next();
//        LinkedList<ThreadInstance> l = new LinkedList<ThreadInstance>();
//        l.add(ti);
//        ThreadsInformation info = new ThreadsInformation(l);
//        g.setThreadsInfo(info);
//        int[] threads = new int[] {0};
//        for (SDGNode n : g.vertexSet()) {
//        	n.setThreadNumbers(threads);
//        }

        System.out.println("initializing the slicers");
        Slicer one = new SummarySlicerBackward(g);
        C2PBackward two = new C2PBackward(g);

        int[] size = new int[3];
        long[] time = new long[3];
        Collection<SDGNode>[] slices = new Collection[3];

        int ctr = 0;

        for (SDGNode crit : g.vertexSet()) {
            ctr++;
            if ((ctr % 1) != 0) continue;
//            if (crit.getKind() == SDGNode.Kind.ACTUAL_IN) continue;

//          System.out.println("Slice for node "+crit);

            // summary slicer
            long tmp = System.currentTimeMillis();
            slices[0] = one.slice(Collections.singleton(crit));
            time[0] += System.currentTimeMillis() - tmp;
            size[0] += slices[0].size();

            // context slicer 1
            ContextManager man = two.getMan();
            Collection<Context> cons = man.getAllContextsOf(crit);
            Context con = cons.iterator().next();
            tmp = System.currentTimeMillis();
            slices[1] = two.contextSlice(Collections.singleton(con));
            time[1] += System.currentTimeMillis() - tmp;
            size[1] += slices[1].size();

            // context slicer 2
            for (Iterator<Context> iter = cons.iterator();iter.hasNext();) {
            	con = iter.next();
            }
            tmp = System.currentTimeMillis();
            slices[2] = two.contextSlice(Collections.singleton(con));
            time[2] += System.currentTimeMillis() - tmp;
            size[2] += slices[2].size();

//            System.out.println("************************************** ");
            if (ctr % 10 == 0) {
                System.out.print(".");
            }
            if (ctr % 100 == 0) {
                System.out.print(ctr);
            }
            if (ctr % 1000 == 0) {
                System.out.println();
            }
        }

        System.out.println();
        System.out.println("Summary Slicer: "+size[0]+"              time: "+time[0]);
        System.out.println("Context Slicer(1): "+size[1]+"              time: "+time[1]);
        System.out.println("Context Slicer(2): "+size[2]+"              time: "+time[2]);
    }
}
