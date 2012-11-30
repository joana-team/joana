/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.barrier;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


/** A 2-phase level slicer.
 * It counts the minimal number of edge traversals each visited node is away from the slicing criterion.
 *
 * -- Created on September 6, 2005
 *
 * @author  Christian Hammer, Dennis Giffhorn
 */
/**
 * @author giffhorn
 *
 */
public abstract class CSLevelSlicer implements LevelSlicer {
    private boolean DEBUG = false;
    protected Set<SDGEdge.Kind> omittedEdges = SDGEdge.Kind.threadEdges();
    protected SDG g;

    interface EdgePredicate {
        public boolean phase1();
        public boolean follow(SDGEdge e);
        public boolean saveInOtherWorklist(SDGEdge e);
    }

    /**
     * Creates a new instance of CSLevelSlicer
     */
    public CSLevelSlicer(SDG graph, Set<SDGEdge.Kind> omit) {
        this.g = graph;
        this.omittedEdges = omit;
    }

    public CSLevelSlicer(Set<SDGEdge.Kind> omit) {
        this.omittedEdges = omit;
    }

    public CSLevelSlicer(SDG graph) {
        this.g = graph;
    }

    public void setGraph(SDG graph) {
        g = graph;
    }

    public Map<SDGNode, Integer> slice(SDGNode n) {
    	return slice(Collections.singleton(n));
    }

    public Map<SDGNode, Integer> slice(Collection<SDGNode> criteria) {
        Map<SDGNode, SDGNode> slice = new TreeMap<SDGNode, SDGNode>(SDGNode.getIDComparator());
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        LinkedList<SDGNode> nextWorklist = new LinkedList<SDGNode>();
        Map<SDGNode, Integer> level = new HashMap<SDGNode, Integer>();
        EdgePredicate p = phase1Predicate();

        worklist.addAll(criteria);

        for (SDGNode v : criteria) {
            slice.put(v, phase1Predicate().phase1() ? v : null);
            level.put(v,0);
        }

        while (!worklist.isEmpty()) {

            while (!worklist.isEmpty()) {
                SDGNode w = worklist.poll();

                for (SDGEdge e : edgesToTraverse(w)) {

                    if (!e.getKind().isSDGEdge() || omittedEdges.contains(e.getKind()))
                        continue;

                    SDGNode v = reachedNode(e);

                    if (!slice.containsKey(v) ||
                            (p.phase1() && slice.get(v) == null)) {

                        // if node was not yet added or node was added in phase2
                        if (p.saveInOtherWorklist(e)) {
                            if (DEBUG) System.out.println("OTHER\t" + v);
//                            System.out.println("with "+e.getKind()+" to: "+v);
                            nextWorklist.add(v);
                            slice.put(v, p.phase1() ? v : null);
                            level.put(v,(level.get(w)+1));

                        } else if (p.follow(e)) {
                            if (DEBUG) System.out.println("FOLLOW\t" + v);
//                            System.out.println("with "+e.getKind()+" to: "+v);
                            worklist.add(v);
                            slice.put(v, p.phase1() ? v : null);
                            level.put(v,(level.get(w)+1));
                        }
                    }
                }
            }

            // swap worklists and predicates
            if (DEBUG) System.out.println("swap");

            worklist = nextWorklist;
            p =  phase2Predicate();
        }

        return level;
    }


    public Map<SDGNode, Integer> slice(SDGNode n, int maxSteps) {
    	return slice(Collections.singleton(n), maxSteps);
    }

    public Map<SDGNode, Integer> slice(Collection<SDGNode> criteria, int maxSteps) {
        Map<SDGNode, SDGNode> slice = new TreeMap<SDGNode, SDGNode>(SDGNode.getIDComparator());
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        LinkedList<SDGNode> nextWorklist = new LinkedList<SDGNode>();
        Map<SDGNode, Integer> level = new HashMap<SDGNode, Integer>();
        EdgePredicate p = phase1Predicate();

        worklist.addAll(criteria);

        for (SDGNode v : criteria) {
            slice.put(v, phase1Predicate().phase1() ? v : null);
            level.put(v,0);
        }

        while (!worklist.isEmpty()) {

            while (!worklist.isEmpty()) {
                SDGNode w = worklist.poll();

                if (level.get(w) == maxSteps) continue;

                for (SDGEdge e : edgesToTraverse(w)) {
                    if (!e.getKind().isSDGEdge() || omittedEdges.contains(e.getKind()))
                        continue;

                    SDGNode v = reachedNode(e);

                    if ((!slice.containsKey(v) || (p.phase1() && slice.get(v) == null))) {
                        // if node was not yet added or node was added in phase2
                        if (p.saveInOtherWorklist(e)) {
                            if (DEBUG) System.out.println("OTHER\t" + v);
//                            System.out.println("with "+e.getKind()+" to: "+v);
                            nextWorklist.add(v);
                            slice.put(v, p.phase1() ? v : null);
                            level.put(v,(level.get(w)+1));

                        } else if (p.follow(e)) {
                            if (DEBUG) System.out.println("FOLLOW\t" + v);
//                            System.out.println("with "+e.getKind()+" to: "+v);
                            worklist.add(v);
                            slice.put(v, p.phase1() ? v : null);
                            level.put(v,(level.get(w)+1));
                        }
                    }
                }
            }

            // swap worklists and predicates
            if (DEBUG) System.out.println("swap");

            worklist = nextWorklist;
            p =  phase2Predicate();
        }

        return level;
    }

    public void setOmittedEdges(Set<SDGEdge.Kind> omit){
        this.omittedEdges = omit;
    }

    protected abstract Collection<SDGEdge> edgesToTraverse(SDGNode node);

    protected abstract SDGNode reachedNode(SDGEdge edge);

    protected abstract EdgePredicate phase1Predicate();

    protected abstract EdgePredicate phase2Predicate();

}
