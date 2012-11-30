/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
 package edu.kit.joana.ifc.sdg.graph.slicer.conc.tests;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.PDGs;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.Chopper;
import edu.kit.joana.ifc.sdg.graph.chopper.conc.ContextSensitiveThreadChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.conc.FixedPointChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.conc.SimpleThreadChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.conc.ThreadChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.conc.VerySimpleThreadChopper;


public class ChoppingTests {
    public static void main (String[] args) throws Exception {
        /* 1 */
        String file = PDGs.pdgs[10];

        SDG g = SDG.readFrom(file);

        LinkedList<ChopCrit> ctrir = createCriteria(g);

        Chopper zero = new VerySimpleThreadChopper(g);
        Chopper one = new SimpleThreadChopper(g);
        Chopper two = new FixedPointChopper(g);
        Chopper three = new ContextSensitiveThreadChopper(g);
//        SDGChopper five = new AlmostTimeSensitiveThreadChopper(g);
        Chopper six = new ThreadChopper(g);

        Chopper[] array = {zero, one, two, three, /*five,*/ six};

        System.out.println(file);
        System.out.println("criteria: "+ctrir.size());

        String str = compareNonEmpty(new VerySimpleThreadChopper(g), array, ctrir);
        System.out.println(str);
        //emptyness(array, ctrir);
    }

    private static LinkedList<ChopCrit> createCriteria(SDG g) {
        LinkedList<ChopCrit> result = new LinkedList<ChopCrit>();
        Set<SDGNode> rawNodes = g.vertexSet();
        Set<SDGNode> nodes = new HashSet<SDGNode>();

        for (SDGNode n : rawNodes) {
//            if (n.getKind() == SDGNode.Kind.ACTUAL_IN
//                    || n.getKind() == SDGNode.Kind.ACTUAL_OUT
//                    || n.getKind() == SDGNode.Kind.FORMAL_IN
//                    || n.getKind() == SDGNode.Kind.FORMAL_OUT) {
//
//                continue;
//            }

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

    @SuppressWarnings("unchecked")
    private static String compare(Chopper[] chopper, LinkedList<ChopCrit> criteria) {
        int[] size = new int[chopper.length];
        long[] time = new long[chopper.length];
        Collection<SDGNode>[] chops = new Collection[chopper.length];

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

            if (ctr % 1 == 0) {
                System.out.print(".");
            }
            if (ctr % 10 == 0) {
                System.out.print(ctr);
            }
            if (ctr % 100 == 0) {
                System.out.println();
            }

//                System.out.print(".");
//
//                if (ctr % 10 == 0) {
//                    System.out.print(ctr);
//                }
//                if (ctr % 100000 == 0) {
//                    System.out.println();
//                }
        }

        String str = "\n";
        for (int i = 0; i < chopper.length; i++) {
            str += chopper[i].getClass().getName()+": "+size[i]+"              time:"+time[i]+"\n";
        }

        return str;
    }

    private static String compareNonEmpty(Chopper filter, Chopper[] chopper, LinkedList<ChopCrit> criteria) {
        LinkedList<ChopCrit> nonEmpty = new LinkedList<ChopCrit>();

        int ctr = 0;
        for (ChopCrit crit : criteria) {
            ctr++;
//                if (ctr % 100 == 0) {
//                    System.out.print(",");
//                }
//                if (ctr % 1000 == 0) {
//                    System.out.print("*");
//                }
//                if (ctr % 10000 == 0) {
//                    System.out.print(ctr);
//                }
//                if (ctr % 100000 == 0) {
//                    System.out.println();
//                }

            Collection<SDGNode> tmp = filter.chop(crit.source, crit.target);

            if (!tmp.isEmpty()) {
                nonEmpty.add(crit);
            }
        }

        System.out.println("criteria: "+nonEmpty.size());

        return compare(chopper, nonEmpty);
    }

    @SuppressWarnings("unused")
    private static void emptyness(Chopper[] chopper, LinkedList<ChopCrit> criteria) {
        int[] empty = new int[chopper.length];

        int ctr = 0;
        for (ChopCrit crit : criteria) {
        	ctr++;
        	for (int i = 0; i < chopper.length; i++) {
        		Collection<SDGNode> chop = chopper[i].chop(crit.source, crit.target);
        		if (chop.isEmpty()) empty[i]++;
        	}

//                if (ctr % 1000 == 0) {
	//                    System.out.print(".");
//                }
//                if (ctr % 10000 == 0) {
//                    System.out.print(ctr);
//                }
//                if (ctr % 100000 == 0) {
//                    System.out.println();
//                }

        	System.out.print(".");

        	if (ctr % 10 == 0) {
        		System.out.print(ctr);
        	}
        	if (ctr % 100000 == 0) {
        		System.out.println();
        	}
        }

        System.out.println("--------------------------------");
        for (int i = 0; i < chopper.length; i++) {
            System.out.println(chopper[i].getClass().getName()+": "+empty[i]);
        }
    }
}

class ChopCrit {
    SDGNode source;
    SDGNode target;

    public ChopCrit(SDGNode s, SDGNode t) {
        source = s;
        target = t;
    }

    public String toString() {
        return "("+source+", "+target+")";
    }
}
