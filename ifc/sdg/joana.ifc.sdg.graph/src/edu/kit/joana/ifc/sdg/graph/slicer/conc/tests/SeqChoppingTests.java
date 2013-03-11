/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.tests;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.PDGs;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.Chopper;
import edu.kit.joana.ifc.sdg.graph.chopper.InsensitiveIntersectionChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.IntersectionChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.NonSameLevelChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.Opt1Chopper;


public class SeqChoppingTests {
    static class Input {
        String file;
        int steps;

        Input(String f, int s) {
            file = f;
            steps = s;
        }
    }

    public static void main (String[] args) throws Exception {
    	String file = PDGs.pdgs[0];
        SDG sdg = SDG.readFrom(file);
        removeThreadEdges(sdg);

        LinkedList<ChopCrit> ctrir = createCriteria(sdg);
//        LinkedList<ChopCrit> ctrir = new LinkedList<ChopCrit>();
//        ctrir.add(new ChopCrit(g.getNode(2463), g.getNode(2463)));

        Chopper one = new InsensitiveIntersectionChopper(sdg);
        Chopper two = new IntersectionChopper(sdg);
        Opt1Chopper three = new Opt1Chopper(sdg);
        Chopper four = new NonSameLevelChopper(sdg);

        Chopper[] array = {one, two, three, four};

        System.out.println(file);
        System.out.println("criteria: "+ctrir.size());

        String str = compare(array, ctrir);
        System.out.println(str);

//        emptyness(array, ctrir);
//        diff(three, four, ctrir);

//        File f = new File(i.file+".chop");
//        BufferedWriter w = new BufferedWriter(new FileWriter(f));
//        w.write(str);
//        w.flush();
//        w.close();
    }

    @SuppressWarnings("unused")
    private static LinkedList<ChopCrit> createLotsOfCriteria(SDG g) {
        LinkedList<ChopCrit> result = new LinkedList<ChopCrit>();
        Set<SDGNode> nodes = g.vertexSet();
        HashMap<Integer, HashSet<SDGNode>> threads = new HashMap<Integer, HashSet<SDGNode>>();

        for (SDGNode n : nodes) {
            for (int t : n.getThreadNumbers()) {
                HashSet<SDGNode> s = threads.get(t);
                if (s == null) {
                    s = new HashSet<SDGNode>();
                    threads.put(t, s);
                }
                s.add(n);
            }
        }

        for (Integer t : threads.keySet()) {
            HashSet<SDGNode> ndoes = threads.get(t);
            SDGNode[] sorted = ndoes.toArray(new SDGNode[0]);

            for (int i = 0, j = sorted.length-1; i < sorted.length && j >= 0; i++, j--) {
                ChopCrit c = new ChopCrit(sorted[i], sorted[j]);
                result.add(c);
            }
        }

        return result;
    }

    private static LinkedList<ChopCrit> createCriteria(SDG g) {
        LinkedList<ChopCrit> result = new LinkedList<ChopCrit>();
        Set<SDGNode> rawNodes = g.vertexSet();
        Set<SDGNode> nodes = new HashSet<SDGNode>();

        for (SDGNode n : rawNodes) {
            if (n.getKind() == SDGNode.Kind.ACTUAL_IN
                    || n.getKind() == SDGNode.Kind.ACTUAL_OUT
                    || n.getKind() == SDGNode.Kind.FORMAL_IN
                    || n.getKind() == SDGNode.Kind.FORMAL_OUT) {

                continue;
            }

            nodes.add(n);
        }

        int mid = nodes.size() / 2;

        for (int i = 1; i < nodes.size(); i += 10) {
            boolean turn = false;
            int x = mid;
            while (!turn || x < mid) {
                ChopCrit c = new ChopCrit(getNode(g, i), getNode(g ,x));
                result.add(c);

                x += 100;
                if (x >= nodes.size()) {
                    x = (x % nodes.size() +1);
                    turn = true;
                }
            }
        }

        return result;
    }

    private static SDGNode getNode(SDG g, int i) {
        SDGNode result = null;
        while (result == null) {
            result = g.getNode(i);
            i++;
        }
        return result;
    }

    private static String compare(Chopper[] chopper, LinkedList<ChopCrit> criteria) {
        int[] size = new int[chopper.length];
        long[] time = new long[chopper.length];
        @SuppressWarnings("unchecked")
        final Collection<SDGNode>[] chops = (Collection<SDGNode>[]) new Collection[chopper.length];

        int ctr = 0;

        for (ChopCrit crit : criteria) {
            ctr++;

            for (int i = 0; i < chopper.length; i++) {
                long tmp = System.currentTimeMillis();
                chops[i] = chopper[i].chop(crit.source, crit.target);
                time[i] += System.currentTimeMillis() - tmp;
                size[i] += chops[i].size();
                //System.out.println(chopper[i].getClass().getName()+": done"+" "+chop.size());
            }

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
        for (int i = 0; i < chopper.length; i++) {
            str += chopper[i].getClass().getName()+": "+size[i]+"              time:"+time[i]+"\n";
        }

        return str;
    }

    @SuppressWarnings("unused")
    private static void emptyness(Chopper[] chopper, LinkedList<ChopCrit> criteria) {
        int[] empty = new int[chopper.length];
        System.out.println("Starting");

        int ctr = 0;
        for (ChopCrit crit : criteria) {
            ctr++;
            for (int i = 0; i < chopper.length; i++) {
                Collection<SDGNode> chop = chopper[i].chop(crit.source, crit.target);
                if (chop.isEmpty()) empty[i]++;
            }

            if (ctr % 1000 == 0) {
                System.out.print(".");
            }
            if (ctr % 10000 == 0) {
                System.out.print(ctr);
            }
            if (ctr % 100000 == 0) {
                System.out.println();
            }
        }
        System.out.println(ctr);

        System.out.println("--------------------------------");
        for (int i = 0; i < chopper.length; i++) {
            System.out.println(chopper[i].getClass().getName()+": "+empty[i]);
        }
    }

    @SuppressWarnings("unused")
    private static void diff(Chopper one, Chopper two, LinkedList<ChopCrit> criteria) {
        int diff = 0;
        int size1 = 0;
        int size2 = 0;
        System.out.println("Starting");

        int ctr = 0;
        for (ChopCrit crit : criteria) {
            ctr++;

            Collection<SDGNode> chop1 = one.chop(crit.source, crit.target);
            Collection<SDGNode> chop2 = two.chop(crit.source, crit.target);

            size1 += chop1.size();
            size2 += chop2.size();

            if (chop1.size() != chop2.size()) {
                diff++;
            }

            if (ctr % 1000 == 0) {
                System.out.print(".");
            }
            if (ctr % 10000 == 0) {
                System.out.print(ctr);
            }
            if (ctr % 100000 == 0) {
                System.out.println();
            }
        }
        System.out.println(ctr);

        System.out.println("--------------------------------");
        System.out.println(one.getClass().getName()+": "+size1);
        System.out.println(two.getClass().getName()+": "+size2);
        System.out.println("different chops: "+diff);
    }

    private static void removeThreadEdges(SDG g) {
        LinkedList<SDGEdge> remove = new LinkedList<SDGEdge>();
        LinkedList<SDGEdge> replace = new LinkedList<SDGEdge>();
        for (SDGEdge e : g.edgeSet()) {
            if (e.getKind().isThreadEdge()) {
                if (e.getKind() == SDGEdge.Kind.FORK) {
                    replace.add(e);
                } else {
                    remove.add(e);
                }
            }
        }

        g.removeAllEdges(remove);
        g.removeAllEdges(replace);

        for (SDGEdge e : replace) {
            SDGEdge f = new SDGEdge(e.getSource(), e.getTarget(), SDGEdge.Kind.CALL);
            g.addEdge(f);
        }
    }
}
