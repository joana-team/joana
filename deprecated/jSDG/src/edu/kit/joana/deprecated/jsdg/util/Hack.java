/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;


/**
 * @author giffhorn
 *
 */
public class Hack {
    public static void main (String[] args) throws Exception {
        /* in */
//        String str = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/j2me.Barcode.pdg";
//        String str = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/j2me.J2MESafe.pdg";
//        String str = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/j2me.KeePass.pdg";
        String str = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/conc.cliser.kk.Main.pdg";
//        String str = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/j2me.hyperm.pdg";

       /* out */
        //String name = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/j2me.Barcode.hack.pdg";
//        String name = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/j2me.J2MESafe.hack.pdg";
//        String name = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/j2me.KeePass.hack.pdg";
        String name = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/conc.cliser.kk.Main2.pdg";
//        String name = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/j2me.hyperm.hack.pdg";


        hack(str, name);
    }

    public static void hack(String inFile, String outFile) throws IOException {
        Log.info("*** HACK ***");
        long time = System.currentTimeMillis();
        SDG sdg = SDG.readFrom(inFile);
        time = System.currentTimeMillis() - time;
        Log.info("graph parsing: "+((double)time/100)+" seconds");
        time = System.currentTimeMillis();
        hack1(sdg);
        time = System.currentTimeMillis() - time;
        Log.info("hack1: "+((double)time/100)+" seconds");
        time = System.currentTimeMillis();
        hack2(sdg);
        time = System.currentTimeMillis() - time;
        Log.info("hack2: "+((double)time/100)+" seconds");

        String content = SDGSerializer.toPDGFormat(sdg);
        File f = new File(outFile);
        FileWriter w = new FileWriter(f);

        w.write(content);
        w.flush();
        w.close();
        Log.info("*** MWAHAHA! ***");
    }

    public static SDG hack(SDG sdg) throws IOException {
        Log.info("*** HACK ***");

        long time = System.currentTimeMillis();
        time = System.currentTimeMillis() - time;
        Log.info("graph parsing: "+((double)time/100)+" seconds");
        time = System.currentTimeMillis();
        hack1(sdg);
        time = System.currentTimeMillis() - time;
        Log.info("hack1: "+((double)time/100)+" seconds");
        time = System.currentTimeMillis();
        hack2(sdg);
        time = System.currentTimeMillis() - time;
        Log.info("hack2: "+((double)time/100)+" seconds");

        Log.info("*** MWAHAHA! ***");
        return sdg;
    }

    private static void hack1(SDG sdg) {
        HashMap<Integer, SDGNode> i = new HashMap<Integer, SDGNode>();
        HashMap<SDGNode, List<SDGNode>> m = new HashMap<SDGNode, List<SDGNode>>();
        Set<SDGNode> nodes = sdg.vertexSet();

        for (SDGNode n : nodes) {
            if (n.getKind() == SDGNode.Kind.ENTRY) {
                i.put(n.getProc(), n);
                m.put(n, new LinkedList<SDGNode>());
            }
        }

        for (SDGNode n : nodes) {
            if (n.getKind() == SDGNode.Kind.EXIT) {
                SDGNode entry = i.get(n.getProc());
                List<SDGNode> l = m.get(entry);
                l.add(n);
                m.put(entry, l);
            }
        }

        for (SDGNode entry : m.keySet()) {
            if (entry.getId() == 1) continue;

            List<SDGNode> l = m.get(entry);
            SDGNode exit = null;

            for (SDGNode e : l) {
                if (!e.getLabel().equals("_exception_")) {
                    exit = e;
                    break;
                }
            }

            if (exit == null) throw new NullPointerException(""+entry);

            boolean ecfe = false;
            for (SDGEdge e : sdg.outgoingEdgesOf(entry)) {
                if (e.getKind() == SDGEdge.Kind.CONTROL_FLOW && e.getTarget() == exit) {
                    ecfe = true;
                }
            }
            if (!ecfe) {
                Log.warn("(1) missing control flow: "+entry+" --> "+exit
                   		+ " " + entry.getLabel() + "->" + exit.getLabel() + " in " + sdg.getEntry(entry).getLabel());
                sdg.addEdge(new SDGEdge(entry, exit, SDGEdge.Kind.CONTROL_FLOW));
            }

            for (SDGNode e : l) {
                if (e == exit) continue;

                e.kind = SDGNode.Kind.FORMAL_OUT;
                e.operation = SDGNode.Operation.FORMAL_OUT;

                boolean ecffo = false;
                for (SDGEdge edge : sdg.outgoingEdgesOf(entry)) {
                    if (edge.getKind() == SDGEdge.Kind.CONTROL_FLOW && edge.getTarget() == e) {
                        ecffo = true;
                    }
                }
                if (!ecffo) {
                    Log.warn("(2) missing control flow: "+e+" --> "+exit
                    		+ " " + e.getLabel() + "->" + exit.getLabel() + " in " + sdg.getEntry(e).getLabel());
                    sdg.addEdge(new SDGEdge(entry, e, SDGEdge.Kind.CONTROL_FLOW));
                }

                boolean focffe = false;
                for (SDGEdge edge : sdg.outgoingEdgesOf(e)) {
                    if (edge.getKind() == SDGEdge.Kind.CONTROL_FLOW && edge.getTarget() == exit) {
                        focffe = true;
                    }
                }
                if (!focffe) {
                    Log.warn("(3) missing control flow: "+e+" --> "+exit
                    		+ " " + e.getLabel() + "->" + exit.getLabel() + " in " + sdg.getEntry(e).getLabel());
                    sdg.addEdge(new SDGEdge(e, exit, SDGEdge.Kind.CONTROL_FLOW));
                }
            }
        }


        // phi-knoten loeschen
//        LinkedList<SDGNode> phis = new LinkedList<SDGNode>();
//
//        for (SDGNode n : nodes) {
//            if (n.getLabel().startsWith("phi v")) {
//                phis.add(n);
//            }
//        }
//
//        while (!phis.isEmpty()) {
//            SDGNode phi = phis.poll();
//
//            LinkedList<SDGEdge> ie = new LinkedList<SDGEdge>();
//            LinkedList<SDGEdge> oe = new LinkedList<SDGEdge>();
//            HashSet<SDGNode> in = new HashSet<SDGNode>();
//            HashSet<SDGNode> on = new HashSet<SDGNode>();
//
//            for (SDGEdge e : sdg.incomingEdgesOf(phi)) {
//                ie.add(e);
//                in.add(e.getSource());
//            }
//
//            for (SDGEdge e : sdg.outgoingEdgesOf(phi)) {
//                oe.add(e);
//                on.add(e.getTarget());
//            }
//
//            // umbiegen
//            for (SDGEdge e : ie) {
//                for (SDGNode n : on) {
//                    sdg.addEdge(new SDGEdge(e.getSource(), n, e.getKind()));
//                }
//            }
//
//            for (SDGEdge e : oe) {
//                for (SDGNode n : in) {
//                    sdg.addEdge(new SDGEdge(n, e.getTarget(), e.getKind()));
//                }
//            }
//
//            // loeschen
//            sdg.removeAllEdges(ie);
//            sdg.removeAllEdges(oe);
//            sdg.removeVertex(phi);
//        }

        // catch-knoten loeschen
//        nodes = sdg.vertexSet();
//        phis = new LinkedList<SDGNode>();
//
//        for (SDGNode n : nodes) {
//            if (n.getLabel().startsWith("v0 = catch")) {
//                phis.add(n);
//            }
//        }
//
//        while (!phis.isEmpty()) {
//            SDGNode phi = phis.poll();
//
//            LinkedList<SDGEdge> ie = new LinkedList<SDGEdge>();
//            LinkedList<SDGEdge> oe = new LinkedList<SDGEdge>();
//            HashSet<SDGNode> in = new HashSet<SDGNode>();
//            HashSet<SDGNode> on = new HashSet<SDGNode>();
//
//            for (SDGEdge e : sdg.incomingEdgesOf(phi)) {
//                ie.add(e);
//                in.add(e.getSource());
//            }
//
//            for (SDGEdge e : sdg.outgoingEdgesOf(phi)) {
//                oe.add(e);
//                on.add(e.getTarget());
//            }
//
//            // umbiegen
//            for (SDGEdge e : ie) {
//                for (SDGNode n : on) {
//                    sdg.addEdge(new SDGEdge(e.getSource(), n, e.getKind()));
//                }
//            }
//
//            for (SDGEdge e : oe) {
//                for (SDGNode n : in) {
//                    sdg.addEdge(new SDGEdge(n, e.getTarget(), e.getKind()));
//                }
//            }
//
//            // loeschen
//            sdg.removeAllEdges(ie);
//            sdg.removeAllEdges(oe);
//            sdg.removeVertex(phi);
//        }

        // ctrl fluss reparieren
        HashMap<Integer, SDGNode> procExit = new HashMap<Integer, SDGNode>();
        LinkedList<SDGNode> loose = new LinkedList<SDGNode>();
        for (SDGNode n : nodes) {
            if (n.getKind() != SDGNode.Kind.EXIT && n.getKind() != SDGNode.Kind.ACTUAL_IN && n.getKind() != SDGNode.Kind.ACTUAL_OUT
                    && n.getKind() != SDGNode.Kind.FORMAL_IN && n.getKind() != SDGNode.Kind.FORMAL_OUT) {

                boolean ok = false;
                for (SDGEdge e : sdg.outgoingEdgesOf(n)) {
                    if (e.getKind() == SDGEdge.Kind.CONTROL_FLOW) {
                        ok = true;
                    }
                }
                if (!ok) loose.add(n);
            }

            if (n.getKind() == SDGNode.Kind.EXIT) {
                procExit.put(n.getProc(), n);
            }
        }

        for (SDGNode n : loose) {
            SDGNode exit = procExit.get(n.getProc());
            if (exit != null) {
                Log.warn("(4) missing control flow: "+n+" --> "+exit
                		+ " " + n.getLabel() + "->" + exit.getLabel() + " in " + sdg.getEntry(n).getLabel());
                sdg.addEdge(new SDGEdge(n, exit, SDGEdge.Kind.CONTROL_FLOW));
            }
        }
    }

    private static void hack2(SDG g) {
        Set<SDGNode> v = g.vertexSet();
        CFG cfg = ICFGBuilder.extractICFG(g);
        List<SDGEdge> fix = new LinkedList<SDGEdge>();

        // entry-exit maps
        HashMap<Integer, SDGNode> entry = new HashMap<Integer, SDGNode>();
        HashMap<Integer, SDGNode> exit = new HashMap<Integer, SDGNode>();
        for (SDGNode n : v) {
            if (n.getKind() == SDGNode.Kind.ENTRY) {
                entry.put(n.getProc(), n);
            }
        }

        for (SDGNode n : v) {
            if (n.getKind() == SDGNode.Kind.EXIT) {
                exit.put(n.getProc(), n);
            }
        }

        // get loose nodes
        for (SDGNode n : cfg.vertexSet()) {
            if (n.getKind() == SDGNode.Kind.ENTRY) continue;

            boolean b = false;
            for (SDGEdge e : cfg.incomingEdgesOf(n)) {
                if (e.getKind() == SDGEdge.Kind.CONTROL_FLOW || e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.FORK) {
                    b = true;
                }
            }

            if (!b) {
                // connect with entry
            	SDGNode e = entry.get(n.getProc());
                Log.info("(5) missing control flow: "+ e +" --> "+n
                		+ " " + e.getLabel() + "->" + n.getLabel() + " in " + g.getEntry(e).getLabel()
                		+ " [" + e.getKind() + " > "+ n.getKind() + "]");
                fix.add(new SDGEdge(entry.get(n.getProc()), n, SDGEdge.Kind.CONTROL_FLOW));
            }
        }

        for (SDGNode n : cfg.vertexSet()) {
            if (n.getKind() == SDGNode.Kind.EXIT) continue;

            boolean b = false;
            for (SDGEdge e : cfg.outgoingEdgesOf(n)) {
                if (e.getKind() == SDGEdge.Kind.CONTROL_FLOW || e.getKind() == SDGEdge.Kind.RETURN || e.getKind() == SDGEdge.Kind.CALL) {
                    b = true;
                }
            }

            if (!b && exit.get(n.getProc()) != null) {
                // connect with exit
            	SDGNode tmp = exit.get(n.getProc());
                Log.warn("(6) missing control flow: "+n+" --> "+ tmp
                		+ " " + n.getLabel() + "->" + tmp.getLabel() + " in " + g.getEntry(n).getLabel());
                fix.add(new SDGEdge(n, exit.get(n.getProc()), SDGEdge.Kind.CONTROL_FLOW));
            }
        }

        // add the edges
        for (SDGEdge e : fix) {
            g.addEdge(e);
        }
    }
}
