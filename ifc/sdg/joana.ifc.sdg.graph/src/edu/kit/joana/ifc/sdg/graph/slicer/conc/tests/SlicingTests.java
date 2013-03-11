/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.tests;

import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;

import edu.kit.joana.ifc.sdg.graph.PDGs;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;


public class SlicingTests {
	static SDG g;

    public static void main (String[] args) throws Exception {
        /* 1 */
    	String file = PDGs.pdgs[1];

        g = SDG.readFrom(file);
//        LinkedList<SDGEdge> ll = new LinkedList<SDGEdge>();
//        for (SDGEdge e : g.edgeSet()) {
//        	if (e.getKind() == SDGEdge.Kind.INTERFERENCE) {
//        		ll.add(e);
//        	}
//        }

        System.out.println("initializing the slicers");

        Slicer one = new edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PBackward(g);
//        NandaBackward one = new NandaBackward(g);
//        edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.experimental.NandaBackward two = new edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.experimental.NandaBackward(g);
//        Slicer two = new edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke.Slicer(g);
//        Slicer three = new edu.kit.joana.ifc.sdg.graph.slicer.conc.simple.SimpleConcurrentSlicer(g);
//        SummarySlicerBackward two = new SummarySlicerBackward(g);
//        Slicer three = new edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke.Slicer(g);
        Slicer four = new edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke.Slicer(g);
//        Slicer four = new edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke.experimental.SlicerJITCompiler(g);

//        NandaForward two = new NandaForward(g);
//        edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.experimental.NandaForward one = new edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.experimental.NandaForward(g);

        Slicer[] array = {one, /*two, three,*/ four};

        System.out.println(file);
        System.out.println("criteria: "+g.vertexSet().size());

        String str = compare(array, g.vertexSet());
//        String str = compareOne(array, g.getNode(40));

        System.out.println(str);
    }

    private static String compare(Slicer[] slicer, Collection<SDGNode> criteria) {
        int[] size = new int[slicer.length];
        long[] time = new long[slicer.length];
        @SuppressWarnings("unchecked")
        final Collection<SDGNode>[] slices = (Collection<SDGNode>[]) new Collection[slicer.length];
        int s = 0; int diff = Integer.MAX_VALUE;

        int ctr = 0;

        for (SDGNode crit : criteria) {
            ctr++;
            if ((ctr % 10) != 0) continue;
//            if (crit.getKind() == SDGNode.Kind.ACTUAL_IN) continue;

//            System.out.println("Slice for node "+crit);
            for (int i = 0; i < slicer.length; i++) {
                long tmp = System.currentTimeMillis();
                slices[i] = slicer[i].slice(Collections.singleton(crit));
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
            if (ctr % 1 == 0) {
                System.out.print(".");
            }
            if (ctr % 10 == 0) {
                System.out.print(ctr);
            }
            if (ctr % 100 == 0) {
                System.out.println();
            }
        }

        String str = "\n";
        for (int i = 0; i < slicer.length; i++) {
            str += slicer[i].getClass().getName()+": "+size[i]+"              time:"+time[i]+"\n";
        }

        str += s+": "+diff+"\n";

        return str;
    }

    @SuppressWarnings("unused")
    private static String compareOne(Slicer[] slicer, SDGNode crit) {
    	@SuppressWarnings("unchecked")
        final Collection<SDGNode>[] slices = (Collection<SDGNode>[]) new Collection[slicer.length];

        for (int i = 0; i < slicer.length; i++) {
            slices[i] = slicer[i].slice(Collections.singleton(crit));

            TreeSet<SDGNode> ordered = new TreeSet<SDGNode>(SDGNode.getIDComparator());
            ordered.addAll(slices[i]);
            System.out.println(slicer[i].getClass().getName()+" done: "+ordered);

            if (i == 1) {
                System.out.println("Slice for node "+crit);
                System.out.println(slicer[i-1].getClass().getName()+" done: "+slices[i-1].size());
                System.out.println(slicer[i].getClass().getName()+" done: "+slices[i].size());

                TreeSet<SDGNode> s = new TreeSet<SDGNode>(SDGNode.getIDComparator());
                s.addAll(slices[i]);
                s.removeAll(slices[i-1]);
                System.out.println("missing: "+s);

                for (SDGNode miss : s) {
                	for (SDGEdge e : g.incomingEdgesOf(miss)) {
                		if (slices[i-1].contains(e.getSource()) && slices[i].contains(e.getSource())) {
                			System.out.println(e);
                		}
                	}
                }
            }
        }

        String str = "\n";

        return str;
    }
}
