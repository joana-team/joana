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
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicer;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.Iterative2PhaseSlicer;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.krinke.OptimizedKrinke;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.NandaFactory;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.simple.SimpleConcurrentSlicer;


/**
 *
 *
 * -- Created on October 14, 2005
 *
 * @author  Dennis Giffhorn
 * @deprecated
 */
public class SliceTool {

    /**
     * Creates a new instance of Slicer
     */
    private SliceTool() {

    }

    public static void main(String[] args) throws Exception {
        args = new String[4];
        args[0] = "/home/st/giffhorn/pdg/threads/dupes/tests.PrecisionTest1.pdg";
        args[1] = "-a";
        args[2] = "-K";
        args[3] = "-NO";

        SDG graph = SDG.readFrom(args[0]);
        SDG graph2 = SDG.readFrom(args[0]);
        //SDG graph = SDG.readFrom(args[0]);
        //SDG graph2 = SDG.readFrom(args[0]);

        if (args[1].equals("-a")) {
            Slicer slicer = initSlicer(graph, args[2]);
            sliceAll(graph, slicer);

        } else if (args[1].equals("-o")) {
            Slicer slicer = initSlicer(graph, args[2]);
            SDGNode c = graph.getNode(Integer.parseInt(args[3]));
            sliceOne(c, slicer);

        } else if (args[1].equals("-t")) {
            Slicer slicer = initSlicer(graph, args[2]);
            timedAll(graph, slicer, Integer.parseInt(args[3]));

        } else if (args[1].equals("-m")) {
            Slicer slicer = initSlicer(graph, args[2]);
            measureAll(graph, slicer, Integer.parseInt(args[3]));

        }else if (args[1].equals("-c")) {
            Slicer one = initSlicer(graph, args[2]);
            Slicer two = initSlicer(graph2, args[3]);

            compare(one, two, graph, graph2);

        } else if (args[1].equals("-co")) {
            Slicer one = initSlicer(graph, args[2]);
            Slicer two = initSlicer(graph2, args[3]);
            SDGNode c1 = graph.getNode(Integer.parseInt(args[4]));
            SDGNode c2 = graph2.getNode(Integer.parseInt(args[4]));

            compareOne(one, two, c1, c2);
        }
    }

    public static Slicer initSlicer(SDG graph, String name) {
        Slicer slicer = null;
        Set<SDGEdge.Kind> omitt3d = EnumSet.of(SDGEdge.Kind.INTERFERENCE);

        System.out.println("Initialise slicer...");

        if (name.equals("-CSS")) {

            // init backward summary slicer
            SummarySlicer ssb = new SummarySlicerBackward(graph, omitt3d);

            slicer = ssb;

        } else if (name.equals("-I2P")) {
            Iterative2PhaseSlicer i2p = new I2PBackward(graph);

            slicer = i2p;

        } else if (name.equals("-K")) {
            OptimizedKrinke k = new OptimizedKrinke(graph);

            slicer = k;

        } /*else if (name.equals("-UK")) {
            UnoptimisedKrinke uk = new UnoptimisedKrinke(graph);

            slicer = uk;

        } else if (name.equals("-CK")) {
            ChoppingKrinke k = new ChoppingKrinke(graph);

            slicer = k;

        } else if (name.equals("-SK")) {
            SubsumingKrinke k = new SubsumingKrinke(graph);

            slicer = k;

        }   else if (name.equals("-RPN")) {
            RealisablePathNanda n = new RealisablePathNanda(graph);

            slicer = n;

        }  */else if (name.equals("-N")) {
            Slicer n = NandaFactory.createNandaBackward(graph);

            slicer = n;

        } else if (name.equals("-S")) {
            SimpleConcurrentSlicer b = new SimpleConcurrentSlicer(graph);

            slicer = b;

        } else if (name.equals("-DK")) {
            edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke.old.Slicer b = new edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke.old.Slicer(graph);

            slicer = b;

        } else if (name.equals("-DKF")) {
            edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke.old.Slicer b = new edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke.old.SlicerFlat(graph);

            slicer = b;

        } /*else if (name.equals("-SD")) {
            edu.kit.joana.ifc.sdg.graph.slicer.conc.superduper.Slicer b = new edu.kit.joana.ifc.sdg.graph.slicer.conc.superduper.Slicer(graph);

            slicer = b;

        }*/

        System.out.println("Slicer initialised...");

        return slicer;
    }

    public static void sliceAll(SDG graph, Slicer slicer) {
        Collection<SDGNode> slice = new LinkedList<SDGNode>();
        Set<SDGNode> nodes = graph.vertexSet();

        for (SDGNode criterion : nodes) {
            if (criterion.getKind() == SDGNode.Kind.FORMAL_IN ||
                    criterion.getKind() == SDGNode.Kind.FORMAL_OUT ||
                    criterion.getKind() == SDGNode.Kind.ACTUAL_OUT ||
                    criterion.getKind() == SDGNode.Kind.ACTUAL_IN ||
                    criterion.getOperation() == SDGNode.Operation.COMPOUND) continue;

            java.util.Collection<SDGNode> temp = slicer.slice(Collections.singleton(criterion));
            slice.clear();
            for (SDGNode node : temp) {
                if (node.getKind() != SDGNode.Kind.FORMAL_IN && node.getKind() != SDGNode.Kind.FORMAL_OUT
                        && node.getKind() != SDGNode.Kind.ACTUAL_OUT && node.getKind() != SDGNode.Kind.ACTUAL_IN
                        && node.getOperation() != SDGNode.Operation.COMPOUND) {

                    slice.add(node);
                }
            }

            System.out.println("----------------------------------------------------------------------");
            System.out.println("slicing "+criterion);
            System.out.println("slice size: "+ slice.size());
            System.out.println("\nslice result: ");
            System.out.println(slice);
        }
    }

    public static void timedAll(SDG graph, Slicer slicer, int steps) {
        Set<SDGNode> nodes = graph.vertexSet();
        List<SDGNode> set = new LinkedList<SDGNode>();
        //int all = 0;

        for (SDGNode n : nodes) {
            if (n.getKind() == SDGNode.Kind.FORMAL_IN ||
                    n.getKind() == SDGNode.Kind.FORMAL_OUT ||
                    n.getKind() == SDGNode.Kind.ACTUAL_OUT ||
                    n.getKind() == SDGNode.Kind.ACTUAL_IN ||
                    n.getOperation() == SDGNode.Operation.COMPOUND) {

            } else {
                set.add(n);
            }
        }



        int sls = 0;
        for (int id = 1; id < set.size(); id += steps) {
            sls++;
        }

        System.out.println("nodes: "+nodes.size());
        System.out.println("edges: "+graph.edgeSet().size());
        System.out.println("slices: "+sls);

        for (int i = 0; i < 1; i++) {
            long tm = System.currentTimeMillis();

            for (int id = 1; id < set.size(); id += steps) {
                //System.out.println(set.get(id));
                slicer.slice(Collections.singleton(set.get(id)));
            }

            tm = System.currentTimeMillis() - tm;
            System.out.print(tm+", ");

        }

        System.out.println();
    }

    public static void measureAll(SDG graph, Slicer slicer, int steps) {
        Set<SDGNode> nodes = graph.vertexSet();
        java.util.Collection<SDGNode> temp = null;
        List<SDGNode> set = new LinkedList<SDGNode>();
        Collection<SDGNode> slice = new LinkedList<SDGNode>();
        int all = 0;

        for (SDGNode n : nodes) {
            if (n.getKind() == SDGNode.Kind.FORMAL_IN ||
                    n.getKind() == SDGNode.Kind.FORMAL_OUT ||
                    n.getKind() == SDGNode.Kind.ACTUAL_OUT ||
                    n.getKind() == SDGNode.Kind.ACTUAL_IN ||
                    n.getOperation() == SDGNode.Operation.COMPOUND) {

            } else {
                set.add(n);
            }
        }



        int sls = 0;
        for (int id = 1; id < set.size(); id += steps) {
            sls++;
        }

        System.out.println("nodes: "+nodes.size());
        System.out.println("edges: "+graph.edgeSet().size());
        System.out.println("slices: "+sls);

        for (int i = 0; i < 1; i++) {
            for (int id = 1; id < set.size(); id += steps) {
                //System.out.println(set.get(id));
                temp = slicer.slice(Collections.singleton(set.get(id)));

                slice.clear();
                for (SDGNode node : temp) {
                    if (node.getKind() != SDGNode.Kind.FORMAL_IN && node.getKind() != SDGNode.Kind.FORMAL_OUT
                            && node.getKind() != SDGNode.Kind.ACTUAL_OUT && node.getKind() != SDGNode.Kind.ACTUAL_IN
                            && node.getOperation() != SDGNode.Operation.COMPOUND) {

                        slice.add(node);
                    }
                }
                all += slice.size();
            }

            /*if (slicer instanceof edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.Nanda)&
             *
                System.out.println(((Nanda)slicer).einfuegen +" ("+((Nanda)slicer).erreichbarkeit+")");

            if (slicer instanceof edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.NandaRepaired)
                System.out.println(((NandaRepaired)slicer).einfuegen
                        +" ("+((NandaRepaired)slicer).erreichbarkeit+")");

             if (slicer instanceof edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.OptimizedNandaRepaired)
                System.out.println(((OptimizedNandaRepaired)slicer).erreichbarkeit);*/


            /*if (slicer instanceof edu.kit.joana.ifc.sdg.graph.slicer.conc.krinke.ChoppingKrinke)
                System.out.println(((ChoppingKrinke)slicer).eingefuegt);

            if (slicer instanceof edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke.Slicer) {
                System.out.println(((edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke.Slicer)slicer).eingefuegt);
                //System.out.println("(optimised: "+((edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke.Slicer)slicer).opt()+")");
                //System.out.println("(optimised_cache: "+((edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke.Slicer)slicer).opt1()+")");
                //System.out.println("(optimised_reach: "+((edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke.Slicer)slicer).opt2()+")");
            }*/

            System.out.println("size of slices: "+all);

        }
    }

    public static void sliceOne(SDGNode criterion, Slicer slicer) {
        if (criterion.getKind() == SDGNode.Kind.FORMAL_IN ||
                criterion.getKind() == SDGNode.Kind.FORMAL_OUT ||
                criterion.getKind() == SDGNode.Kind.ACTUAL_OUT ||
                criterion.getKind() == SDGNode.Kind.ACTUAL_IN ||
                criterion.getOperation() == SDGNode.Operation.COMPOUND) {

            System.out.println("no slice for "+criterion.getKind());
            return;
        }

        Collection<SDGNode> slice = new LinkedList<SDGNode>();

        java.util.Collection<SDGNode> temp = slicer.slice(Collections.singleton(criterion));
        slice.clear();
        for (SDGNode node : temp) {
            //if (node.getKind() != SDGNode.Kind.FORMAL_IN && node.getKind() != SDGNode.Kind.FORMAL_OUT
            //        && node.getKind() != SDGNode.Kind.ACTUAL_OUT && node.getKind() != SDGNode.Kind.ACTUAL_IN
            //        && node.getOperation() != SDGNode.Operation.COMPOUND) {

                slice.add(node);
            //}
        }

        System.out.println("slicing "+criterion);
        System.out.println("slice size: "+ slice.size());

        System.out.println("\nslice result: ");
        System.out.println(slice);
    }

    private static void compare(Slicer one, Slicer two, SDG g1, SDG g2) {
        LinkedList<SDGNode> diff = new LinkedList<SDGNode>();
        LinkedList<SDGNode> oneSlice = new LinkedList<SDGNode>();
        LinkedList<SDGNode> twoSlice = new LinkedList<SDGNode>();

        int size = g1.vertexSet().size();

        if (g2.vertexSet().size() > size) size = g2.vertexSet().size();

        for (int pos = 0; pos < size; pos += 1) {
            SDGNode criterion1 = g1.getNode(pos);
            SDGNode criterion2 = g2.getNode(pos);

            if (criterion1 == null || criterion2 == null
                    || criterion1.getId() != criterion2.getId()) continue;

            if (criterion1.getKind() == SDGNode.Kind.FORMAL_IN ||
                    criterion1.getKind() == SDGNode.Kind.FORMAL_OUT ||
                    criterion1.getKind() == SDGNode.Kind.ACTUAL_OUT ||
                    criterion1.getKind() == SDGNode.Kind.ACTUAL_IN ||
                    criterion1.getOperation() == SDGNode.Operation.COMPOUND) continue;

            java.util.Collection<SDGNode> temp = one.slice(Collections.singleton(criterion1));
            oneSlice.clear();
            for (SDGNode node : temp) {
                if (node.getKind() != SDGNode.Kind.FORMAL_IN && node.getKind() != SDGNode.Kind.FORMAL_OUT
                        && node.getKind() != SDGNode.Kind.ACTUAL_OUT && node.getKind() != SDGNode.Kind.ACTUAL_IN
                        && node.getOperation() != SDGNode.Operation.COMPOUND) {

                    oneSlice.addLast(node);
                }
            }

            temp = two.slice(Collections.singleton(criterion2));
            twoSlice.clear();
            for (SDGNode node : temp) {
                if (node.getKind() != SDGNode.Kind.FORMAL_IN && node.getKind() != SDGNode.Kind.FORMAL_OUT
                        && node.getKind() != SDGNode.Kind.ACTUAL_OUT && node.getKind() != SDGNode.Kind.ACTUAL_IN
                        && node.getOperation() != SDGNode.Operation.COMPOUND) {

                    twoSlice.addLast(node);
                }
            }

            if (oneSlice.size() < twoSlice.size() || oneSlice.size() > twoSlice.size()) {
                diff.addFirst(criterion1);

            } else if (oneSlice.size() == twoSlice.size()) {
                temp.clear();
                temp.addAll(oneSlice);
                temp.removeAll(twoSlice);

                if (!temp.isEmpty()) {
                    diff.addFirst(criterion1);
                }
            }


            System.out.println("----------------------------------------------------------------------");
            System.out.println("Slicing "+criterion1);
            System.out.println("first size: "+ oneSlice.size() + "\nsecond size: " + twoSlice.size());

            System.out.println();
            System.out.println("first slicer result: ");
            System.out.println(oneSlice);

            System.out.println();
            System.out.println("second slicer result: ");
            System.out.println(twoSlice);

            System.out.println();
            System.out.println("differences: ");
            if (oneSlice.size() > twoSlice.size()) {
                oneSlice.removeAll(twoSlice);
                System.out.println(oneSlice);

            } else if (oneSlice.size() < twoSlice.size()) {
                twoSlice.removeAll(oneSlice);
                System.out.println(twoSlice);

            } else {
                oneSlice.removeAll(twoSlice);
                System.out.println(oneSlice);
            }

        }

        System.out.println("***********************");
        System.out.println("different results: ");
        System.out.println(diff);
    }

    private static void compareOne(Slicer one, Slicer two, SDGNode criterion1, SDGNode criterion2) {
        if (criterion1.getKind() == SDGNode.Kind.FORMAL_IN ||
                criterion1.getKind() == SDGNode.Kind.FORMAL_OUT ||
                criterion1.getKind() == SDGNode.Kind.ACTUAL_OUT ||
                criterion1.getKind() == SDGNode.Kind.ACTUAL_IN ||
                criterion1.getOperation() == SDGNode.Operation.COMPOUND) {

            System.out.println("no slice for "+criterion1.getKind());
            return;
        }

        LinkedList<SDGNode> diff = new LinkedList<SDGNode>();
        LinkedList<SDGNode> oneSlice = new LinkedList<SDGNode>();
        LinkedList<SDGNode> twoSlice = new LinkedList<SDGNode>();

        java.util.Collection<SDGNode> temp = one.slice(Collections.singleton(criterion1));
        oneSlice.clear();
        for (SDGNode node : temp) {
            //if (node.getKind() != SDGNode.Kind.FORMAL_IN && node.getKind() != SDGNode.Kind.FORMAL_OUT
            //        && node.getKind() != SDGNode.Kind.ACTUAL_OUT && node.getKind() != SDGNode.Kind.ACTUAL_IN) {

                oneSlice.addLast(node);
            //}
        }

        temp = two.slice(Collections.singleton(criterion2));
        twoSlice.clear();
        for (SDGNode node : temp) {
            //if (node.getKind() != SDGNode.Kind.FORMAL_IN && node.getKind() != SDGNode.Kind.FORMAL_OUT
            //        && node.getKind() != SDGNode.Kind.ACTUAL_OUT && node.getKind() != SDGNode.Kind.ACTUAL_IN) {

                twoSlice.addLast(node);
            //}
        }

        if (oneSlice.size() < twoSlice.size() || oneSlice.size() > twoSlice.size()) {
            diff.addFirst(criterion1);

        } else if (oneSlice.size() == twoSlice.size()) {
            temp.clear();
            temp.addAll(oneSlice);
            temp.removeAll(twoSlice);

            if (!temp.isEmpty()) {
                diff.addFirst(criterion1);
            }
        }

        System.out.println("----------------------------------------------------------------------");
        System.out.println("Slicing "+criterion1);
        System.out.println("first size: "+ oneSlice.size() + "\nsecond size: " + twoSlice.size());

        System.out.println();
        System.out.println("first slicer result: ");
        System.out.println(oneSlice);

        System.out.println();
        System.out.println("second slicer result: ");
        System.out.println(twoSlice);

        System.out.println();
        System.out.println("differences: ");
        if (oneSlice.size() > twoSlice.size()) {
            oneSlice.removeAll(twoSlice);
            System.out.println(oneSlice);

        } else if (oneSlice.size() < twoSlice.size()) {
            twoSlice.removeAll(oneSlice);
            System.out.println(twoSlice);

        } else {
            oneSlice.removeAll(twoSlice);
            System.out.println(oneSlice);
        }
    }
}
