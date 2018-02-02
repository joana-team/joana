/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.ContextInsensitiveBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.ContextSlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.IPDGSlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;


public class SeqSlicingTests {
	static SDG g;

//    public static void main (String[] args) throws Exception {
//    	// command line configuration
//    	String file = args[0];
//    	int algs = Integer.parseInt(args[1]);
//    	test(file, algs);
//    }

    public static void main (String[] args) throws Exception {
        // explicit configuration
    	String file = Sequentialize.pdgs[15];  // 5 DayTime, 7 DaisyTest, 13 Key, 15 Rec

        String ins = "0";
        String _2p = "0";
        String scs = "0";
        String dcs = "0";
        String ipdg= "1";
        String str = ipdg+dcs+scs+_2p+ins;

        int algs = Integer.parseInt(str);
    	test(file, algs);
    }

    public static void test (String file, int algs) throws Exception {
        /* 1 */
        g = SDG.readFrom(file);
        ThreadInstance ti = g.getThreadsInfo().iterator().next();
        LinkedList<ThreadInstance> l = new LinkedList<ThreadInstance>();
        l.add(ti);
        ThreadsInformation info = new ThreadsInformation(l);
        g.setThreadsInfo(info);
        int[] threads = new int[] {0};
        for (SDGNode n : g.vertexSet()) {
        	n.setThreadNumbers(threads);
        }

        System.out.println("initializing the slicers");
        LinkedList<Slicer> array = new LinkedList<Slicer>();

        if (algs % 2 != 0) {
        	array.addLast(new ContextInsensitiveBackward(g));
        }
        algs = algs / 10;

        if (algs % 2 != 0) {
        	array.addLast(new SummarySlicerBackward(g));
        }
        algs = algs / 10;

        if (algs % 2 != 0) {
        	array.addLast(new ContextSlicerBackward(g, true));
        }
        algs = algs / 10;

        if (algs % 2 != 0) {
        	array.addLast(IPDGSlicerBackward.newIPDGSlicerBackward(g, false));
        }
        algs = algs / 10;

        if (algs % 2 != 0) {
        	array.addLast(IPDGSlicerBackward.newIPDGSlicerBackward(g, true));
        }

        System.out.println(file);
        System.out.println("criteria: "+g.vertexSet().size());

        String str = compare(array, g.vertexSet());
//        String str = compareOne(array, g.getNode(58));

        System.out.println(str);
    }

    @SuppressWarnings("unchecked")
    private static String compare(List<Slicer> slicer, Collection<SDGNode> criteria) {
        int[] size = new int[slicer.size()];
        long[] time = new long[slicer.size()];
        Collection<SDGNode>[] slices = new Collection[slicer.size()];
        int s = 0; int diff = Integer.MAX_VALUE;

        int ctr = 0;

        for (SDGNode crit : criteria) {
            ctr++;
            if ((ctr % 10) != 0) continue;
//            if (crit.getKind() == SDGNode.Kind.ACTUAL_IN) continue;

//            System.out.println("Slice for node "+crit);
            for (int i = 0; i < slicer.size(); i++) {
                long tmp = System.currentTimeMillis();
                slices[i] = slicer.get(i).slice(Collections.singleton(crit));
                time[i] += System.currentTimeMillis() - tmp;
                size[i] += slices[i].size();
//                TreeSet<SDGNode> ordered = new TreeSet<SDGNode>(SDGNode.getIDComparator());
//                ordered.addAll(slices[i]);
//                System.out.println(slicer[i].getClass().getName()+" done: "+ordered);
//                if (i == 3 && slices[i].size() != slices[i-1].size()) {
//                    System.out.println("Slice for node "+crit);
//                    System.out.println(slicer[i-1].getClass().getName()+" done: "+slices[i-1].size());
//                    System.out.println(slicer[i].getClass().getName()+" done: "+slices[i].size());
//
//                    TreeSet<SDGNode> s = new TreeSet<SDGNode>(SDGNode.getIDComparator());
//                    s.addAll(slices[i]);
//                    System.out.println(slicer[i].getClass().getName()+" done: "+s);
//                    s.addAll(slices[i-1]);
//                    s.removeAll(slices[i]);
//                    System.out.println("missing: "+s);
//                }

                if (i == 1 && slices[0].size() != slices[1].size() && Math.abs(slices[0].size()-slices[1].size()) < diff) {
                	diff = Math.abs(slices[0].size()- slices[1].size());
                	s = crit.getId();
                }
            }
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

        String str = "\n";
        for (int i = 0; i < slicer.size(); i++) {
            str += slicer.get(i).getClass().getName()+": "+size[i]+"              time:"+time[i]+"\n";
        }

        str += s+": "+diff+"\n";

        return str;
    }

    @SuppressWarnings({"unchecked", "unused"})
    private static String compareOne(List<Slicer> slicer, SDGNode crit) {
    	if (slicer.size() != 2) throw new RuntimeException("Works with two slicers only!");
    	System.out.println("Slice for node "+crit);

        Collection<SDGNode>[] slices = new Collection[slicer.size()];

        for (int i = 0; i < slicer.size(); i++) {
            slices[i] = slicer.get(i).slice(Collections.singleton(crit));

            TreeSet<SDGNode> ordered = new TreeSet<SDGNode>(SDGNode.getIDComparator());
            ordered.addAll(slices[i]);
            System.out.println(slicer.get(i).getClass().getName()+" done: "+ordered);
        }

        System.out.println(slicer.get(0).getClass().getName()+" size: "+slices[0].size());
        System.out.println(slicer.get(1).getClass().getName()+" size: "+slices[1].size());

        int j = (slices[0].size() > slices[1].size() ? 0 : 1);

        TreeSet<SDGNode> s = new TreeSet<SDGNode>(SDGNode.getIDComparator());
        s.addAll(slices[j]);
        s.removeAll(slices[1-j]);
        System.out.println("missing: "+s);

        for (SDGNode miss : s) {
        	for (SDGEdge e : g.outgoingEdgesOf(miss)) {
        		if (slices[0].contains(e.getTarget()) && slices[1].contains(e.getTarget())) {
        			System.out.println(e);
        		}
        	}
        }

        String str = "\n";

        return str;
    }
}
