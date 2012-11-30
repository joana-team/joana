/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph.controlflow.ntscd;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;


/** Computes Non-termination sensitive control flow as defined by Ranganath et al.
 * The algorithm uses the same underlying idea as that of Ranganath.
 *
 * @author giffhorn
 */
public class NTSCD {
    private CFG icfg;
    private List<SDGEdge> tmpEdges;
    private HashMap<SDGNode, HashSet<SDGNode>> allPathRelation;

    public NTSCD(CFG icfg) {
        this.icfg = icfg;
        tmpEdges = new LinkedList<SDGEdge>();
        allPathRelation = new HashMap<SDGNode, HashSet<SDGNode>>();
    }

    public void compute() {
        removeInterproceduralEdges();
        computeAllPathRelation();
        evaluatePathRelation();
    }

    private void removeInterproceduralEdges() {
        for (SDGEdge e : icfg.edgeSet()) {
            if (e.getKind() == SDGEdge.Kind.CALL
                    || e.getKind() == SDGEdge.Kind.RETURN
                    || e.getKind() == SDGEdge.Kind.FORK
                    || e.getKind() == SDGEdge.Kind.JOIN) {

                tmpEdges.add(e);
            }
        }

        for (SDGEdge e : tmpEdges) {
            icfg.removeEdge(e);
        }
    }

    private void computeAllPathRelation() {
        List<SDGNode> exits = exitNodes();

        for (SDGNode exit : exits) {
            computeAllPathRelation(exit);
        }
    }

    private void computeAllPathRelation(SDGNode exit) {
        boolean changed = true;

        while (changed) {
            changed = singleRun(exit);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean singleRun(SDGNode exit) {
        boolean changed = false;
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        TreeSet<SDGEdge> marked = new TreeSet<SDGEdge>(SDGEdge.getComparator());

        worklist.add(exit);

        while (!worklist.isEmpty()) {
            SDGNode n = worklist.poll();

            for (SDGEdge e : icfg.incomingEdgesOf(n)) {
                if (!marked.add(e)) continue;

                SDGNode pre = e.getSource();
                worklist.add(pre);

                HashSet<SDGNode> prePaths = allPathRelation.get(pre);

                if (icfg.outgoingEdgesOf(pre).size() == 1 && prePaths == null) {
                    // pre reaches always n
                    updatePaths(pre, n);
                    changed = true;

                } else {
                    LinkedList<HashSet<SDGNode>> all = new LinkedList<HashSet<SDGNode>>();

                    for (SDGEdge f : icfg.outgoingEdgesOf(pre)) {
                        HashSet<SDGNode> paths = allPathRelation.get(f.getTarget());
                        HashSet<SDGNode> clone = new HashSet<SDGNode>();

                        if (paths != null) {
                            clone = (HashSet<SDGNode>) paths.clone();
                        }

                        clone.add(f.getTarget());
                        all.add(clone);
                    }

                    HashSet<SDGNode> base = all.poll();

                    for (HashSet<SDGNode> l : all) {
                        base.retainAll(l);
                    }

                    if (!base.isEmpty() && (prePaths == null || !prePaths.containsAll(base))) {
                        updatePaths(pre, base);
                        changed = true;
                    }
                }
            }
        }

        return changed;
    }

    private void updatePaths(SDGNode from, SDGNode to) {
        HashSet<SDGNode> paths = allPathRelation.get(from);

        if (paths == null) {
            paths = new HashSet<SDGNode>();
        }

        paths.add(to);
        allPathRelation.put(from, paths);
    }

    private void updatePaths(SDGNode from, HashSet<SDGNode>  to) {
        HashSet<SDGNode> paths = allPathRelation.get(from);

        if (paths == null) {
            paths = new HashSet<SDGNode>();
        }

        paths.addAll(to);
        allPathRelation.put(from, paths);
    }

    private List<SDGNode> exitNodes() {
        LinkedList<SDGNode> l = new LinkedList<SDGNode>();

        for (SDGNode n : icfg.vertexSet()) {
            if (n.getKind() == SDGNode.Kind.EXIT) {
                l.add(n);
            }
        }

        return l;
    }

    private void evaluatePathRelation() {
        LinkedList<SDGEdge> newEdges = new LinkedList<SDGEdge>();

        for (SDGNode n : icfg.vertexSet()) {
            LinkedList<SDGNode> nodes = new LinkedList<SDGNode>();

            // collect all nodes that are reachable on all paths starting at one successor
            for (SDGEdge e : icfg.outgoingEdgesOf(n)) {
                nodes.add(e.getTarget());
                HashSet<SDGNode> paths = allPathRelation.get(e.getTarget());
                if (paths != null) {
                    nodes.addAll(paths);
                }
            }

            // now check for any of these nodes if there is a path starting at another
            // successor that does not reach it -> NTSCD-edge
            for (SDGNode m : nodes) {
                for (SDGEdge e : icfg.outgoingEdgesOf(n)) {
                    if (e.getTarget() != m) {
                        HashSet<SDGNode> paths = allPathRelation.get(e.getTarget());

                        if (paths == null || !paths.contains(m)) {
                            SDGEdge ntscd = new SDGEdge(n, m, SDGEdge.Kind.NTSCD);

                            newEdges.add(ntscd);
                        }
                    }
                }
            }
        }

        for (SDGEdge e : newEdges) {
            icfg.addEdge(e);
        }
    }


    public static void main(String[] args) throws Throwable {
        //SDG g = SDG.readFrom("/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Misc/Main.pdg");
        SDG g = SDG.readFrom("/afs/info.uni-karlsruhe.de/user/giffhorn/giffhorn/pdg/tests.Dijkstra.pdg");
        CFG icfg = ICFGBuilder.extractICFG(g);
        //GraphModifier.inlineParameterVertices(sdg, icfg);
        NTSCD ntscd = new NTSCD(icfg);
        ntscd.compute();

        List<SDGEdge> l = new LinkedList<SDGEdge>();

        for (SDGEdge e : g.edgeSet()) {
            if (e.getKind().isIntraSDGEdge()
                    && e.getKind() != SDGEdge.Kind.CONTROL_DEP_UNCOND
                    && e.getKind() != SDGEdge.Kind.CONTROL_DEP_COND
                    && e.getKind() != SDGEdge.Kind.CONTROL_DEP_EXPR
                    && e.getKind() != SDGEdge.Kind.CONTROL_DEP_CALL) {

                l.add(e);
            }
        }

        for (SDGEdge e : l) {
            g.removeEdge(e);
        }

        for (SDGEdge e : icfg.edgeSet()) {
            if (e.getKind() == SDGEdge.Kind.NTSCD) {
                g.addEdge(new SDGEdge(e.getSource(), e.getTarget(), SDGEdge.Kind.DATA_DEP));
            }
        }

        String content = SDGSerializer.toPDGFormat(g);
        File f = new File("/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/Dijkstra.pdg");
        FileWriter w = new FileWriter(f);

        w.write(content);
        w.flush();
        w.close();


        /*int length = 0;
        for (SDGNode n : sdg.vertexSet()) {
            if (n.getId() > length) {
                length = n.getId();
            }
        }

        boolean[][] b = new boolean[length+1][length+1];

        for (SDGEdge e : icfg.edgeSet()) {
            if (e.getKind() == SDGEdge.Kind.NTSCD) {
                b[e.getSource().getId()][e.getTarget().getId()] = true;
            }
        }

        for (SDGEdge e : sdg.edgeSet()) {
            if (e.getKind().isSDGEdge()) {
                b[e.getSource().getId()][e.getTarget().getId()] = false;
            }
        }

        for (int i = 0; i < b.length; i++) {
            for (int x = 0; x < b.length; x++) {
                if (b[i][x]) {
                    //System.out.println(i+" -NTSCD-> "+x);
                }
            }
        }

        System.out.println(icfg);*/
    }
}
