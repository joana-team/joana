/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*****************************************************************************
 *                                                                           *
 *   Intraprocedural Chopping Algorithm                                      *
 *                                                                           *
 *   This file is part of the chopper package of the sdg library.            *
 *   The used chopping algorithms are desribed in Jens Krinke's PhD thesis   *
 *   "Advanced Slicing of Sequential and Concurrent Programs".               *
 *                                                                           *
 *   authors:                                                                *
 *   Bernd Nuernberger, nuerberg@fmi.uni-passau.de                           *
 *                                                                           *
 *****************************************************************************/

package edu.kit.joana.ifc.sdg.graph.chopper;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import edu.kit.joana.ifc.sdg.graph.PDGs;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


/**
 * The IntraproceduralChopper (CIC) computes intra-procedural chops.
 *
 * It computes an intra-procedural backward slice for the target criterion and
 * then an intra-procedural forward slice for the source criterion on the sub-graph
 * spanned by the nodes in the backward slice.
 *
 * This algorithm is an integral part of almost every other chopping algorithm.
 *
 * @author Bernd Nuernberger
 * @since 1.5
 */
public class IntraproceduralChopper extends Chopper {
    /**
     * Constructs an intra-procedural chopper for a given SDG.
     * @param g   The SDG to operate on. Can be null. Can be a cSDG.
     */
    public IntraproceduralChopper(SDG g) {
        super(g);
    }

    /**
     * Nothing to do here.
     */
    protected void onSetGraph() { }

	/**
     * Computes an intra-procedural chop from <code>sourceSet</code> to <code>sinkSet</code>.
     *
     * @param sourceSet  The source criterion set. Should not contain null, should not be empty.
     * @param sinkSet    The target criterion set. Should not contain null, should not be empty.
     * @return           The chop (a HashSet).
     * @throws InvalidCriterionException, if the nodes in sourceSet and targetSet do not belong to the same procedure
     * or if one of the sets is empty.
     */
    public Collection<SDGNode> chop(Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet)
    throws InvalidCriterionException {// the bouncer
    	if (!Chopper.testSameLevelSetCriteria(sourceSet, sinkSet)) {
    		// Thou shall not pass!
    		throw new InvalidCriterionException("This is not a same-level chopping criterion: "+sourceSet+", "+sinkSet);
        }

        // === initialization ===
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        Set<SDGNode> visitedBackward = new HashSet<SDGNode>();
        Set<SDGNode> visitedForward = new HashSet<SDGNode>();

        // === backward phase ===

        worklist.addAll(sinkSet);
        visitedBackward.addAll(sinkSet);

        while (!worklist.isEmpty()) {
            SDGNode n = worklist.pop();

            for (SDGEdge e : sdg.incomingEdgesOf(n)) {
                SDGEdge.Kind kind = e.getKind();
                if (!kind.isSDGEdge() || !kind.isIntraproceduralEdge()) continue;

                SDGNode m = e.getSource();

                if (visitedBackward.add(m)) {
                    worklist.push(m);
                }
            }
        }

        // === forward phase ===

        sourceSet.retainAll(visitedBackward);
        worklist.addAll(sourceSet);
        visitedForward.addAll(sourceSet);

        while (!worklist.isEmpty()) {
            SDGNode n = worklist.pop();

            for (SDGEdge e : sdg.outgoingEdgesOf(n)) {

                SDGEdge.Kind kind = e.getKind();
                if (!kind.isSDGEdge() || !kind.isIntraproceduralEdge()) continue;

                SDGNode m = e.getTarget();

                if (visitedBackward.contains(m) && visitedForward.add(m)) {
                    worklist.push(m);
                }
            }
        }

        return visitedForward;
    }


    /**
     * Test driver for debugging purposes.
     */
    public static void main (String[] args) throws Exception {
        SDG g = SDG.readFrom(PDGs.pdgs[2]);
        LinkedList<SDGEdge> l = new LinkedList<SDGEdge>();
        for (SDGEdge e : g.edgeSet()) {
        	if (e.getKind().isThreadEdge()) l.add(e);
        }
        for (SDGEdge e : l) {
        	g.removeEdge(e);
        	if (e.getKind() == SDGEdge.Kind.FORK) g.addEdge(new SDGEdge(e.getSource(), e.getTarget(), SDGEdge.Kind.CALL));
        }

        IntraproceduralChopper chopper = new IntraproceduralChopper(g);

        // single
        ChoppingCriterion c = new ChoppingCriterion(g.getNode(2532), g.getNode(2546));
        TreeSet<SDGNode> s = new TreeSet<SDGNode>(SDGNode.getIDComparator());
        s.addAll(chopper.chop(c));
        System.out.println(s);
    }
}
