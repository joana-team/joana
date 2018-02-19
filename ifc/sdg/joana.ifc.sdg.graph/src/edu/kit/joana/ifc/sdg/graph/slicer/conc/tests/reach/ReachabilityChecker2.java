/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.tests.reach;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.krinke.ReachabilityCache;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.FoldedCFG;


/** A class for examining whether a context can reach a second contaxt in a CFG.
 *
 * @author Dennis Giffhorn
 * @version 1.0
 */
public class ReachabilityChecker2 {
    /** Cache for already tested reachabilities. */
    private final ReachabilityCache<DynamicContext> cache;
    /** A folded version of the ICFG that is folded with Krinke's two-pass folding algorithm. */
    private final FoldedCFG foldedIcfg;

    /** Creates a new instance of ReachabilityChecker
     * Needs a ICFG that shall be used for the reaching tests and a folded
     * version of the ICFG that is folded with Krinke's two-pass folding algorithm.
     *
     * @param icfg  The ICFG to work on.
     * @param folded_icfg  A folded version of the ICFG.
     */
    public ReachabilityChecker2(FoldedCFG foldedIcfg) {
        this.cache = new ReachabilityCache<>();
        this.foldedIcfg = foldedIcfg;
    }

    /** Grants access to the cache for inheriting classes.
     */
    protected ReachabilityCache<DynamicContext> getCache() {
        return cache;
    }

    /** Tests if a source context can reach a target context.
     *
     * @param source  The source context.
     * @param target  The target.
     */
    public boolean reaches(DynamicContext source, DynamicContext target) {

        // if target is a context of an exit vertex of a thread, return true
        if (target.isEmpty()) {
            return true;
        }

        if (source.isEmpty()) {
            return false;
        }

        // first, check cache
        // if not cached, traverse ICFG
        if (cache.contains(source, target)&& cache.isReaching(source, target)) {
            return true;

        } else {
        	DynamicContext context_copy = source.copy();

            // traverse graph to see if context reaches target
            // save result in cache
            return reachable(context_copy, target);
        }
    }

    /** Tests if a source context can reach a target context.
     *
     * @param source  The source context.
     * @param target  The target.
     */
    protected boolean reachable(DynamicContext context, DynamicContext target) {
        // worklist and marked list
        LinkedList<DynamicContext> worklist = new LinkedList<DynamicContext>();
        HashSet<DynamicContext> marked = new HashSet<DynamicContext>();

        // init both lists with 'context'
        worklist.add(context);
        marked.add(context);

        // Build all contexts resulting from traversing outgoing control-flow,
        // call and return edges. If 'next' and 'target' are equal, return 'true'.
        // If worklist is empty, return 'false'.
        while (!worklist.isEmpty()) {
        	DynamicContext next = worklist.poll();

            // test whether 'target' is reached
            if (equals(next, target)) {
                return true;
            }

            // traverse forward, but do not cross thread boundaries
            // source and target contexts are always in the same thread
            for (SDGEdge edge : foldedIcfg.outgoingEdgesOf(next.getNode())) {

                if (edge.getKind() == SDGEdge.Kind.CONTROL_FLOW) {
//                    System.out.println("\nCF edge from "+ edge.getSource()+" to "+ edge.getTarget());

                    // build new context
                	DynamicContext new_c = step(next, edge.getTarget());

                    // add new context to worklist
                    if (marked.add(new_c)) {
//                        System.out.println("new context: "+new_c);
                        worklist.add(new_c);
                    }

                } else if (edge.getKind() == SDGEdge.Kind.CALL) {

//                    System.out.println("\nCALL edge from "+ edge.getSource()+" to "+ edge.getTarget());
                	DynamicContext new_c = down(next, edge.getTarget());

                    // add new context to worklist
                    if (isSuffix(new_c, target) && marked.add(new_c)) {
//                        System.out.println("new context: "+new_c);
                        worklist.add(new_c);
                    }

                } else if (edge.getKind() == SDGEdge.Kind.RETURN) {

//                    System.out.println("\nRETURN edge from "+ edge.getSource()+" to "+ edge.getTarget());

                    // get the call site belonging to the return edge
                    SDGNode callSite = getCallSiteFor(edge.getTarget());

                    // if context and call site match, traverse return edge
                    if (match(next, callSite)) {
                    	DynamicContext new_c = up(next, edge.getTarget());

                        // add new context to worklist
                        if (marked.add(new_c)) {
//                            System.out.println("new context: "+new_c);
                            worklist.add(new_c);
                        }
                    }
                }
            }
        }

        // source context doesn't reach target
        return false;
    }

    /** Tests if Context c is a suffix of 'of'.
     * @param c  The suspected suffix.
     * @param of  The other Context.
     */
    protected boolean isSuffix(DynamicContext c, DynamicContext of) {
        return c.isSuffixOf(of);
    }

    /** Gets a call site belonging to a return site.
     * @param returnPoint  The return site.
     * @return  The call site or null.
     */
    private SDGNode getCallSiteFor(SDGNode returnPoint) {
        if (returnPoint.getKind() == SDGNode.Kind.FOLDED) {
            return returnPoint;
        }

        List<SDGEdge> inc = foldedIcfg.getIncomingEdgesOfKind(returnPoint, SDGEdge.Kind.CONTROL_FLOW);
        return inc.isEmpty() ? null : inc.get(0).getSource();
    }

    /** Tests if one equals two.
     */
    protected boolean equals(DynamicContext one, DynamicContext two) {
        return one.equals(two);
    }

// === context building methods ===

    /** Handles the traversion of intraprocedural edges.
     * The new context is simply build by replacing the top node of the old
     * context with the target of the traversed edge.
     *
     * @param old  The old context.
     * @param target  The traversed edge's target.
     */
    private DynamicContext step(DynamicContext old, SDGNode target) {
    	DynamicContext c = old.copyWithNewNode(target);

        return c;
    }

    /** Handles the traversion into a called procedure.
     * The new context is build by adding the target of the traversed edge
     * on top of the old context.
     *
     * @param old  The old context.
     * @param target  The traversed edge's target.
     */
    private DynamicContext down(DynamicContext old, SDGNode target) {
    	DynamicContext c = old.copyWithNewNode(target);

        c.push(old.getNode());

        return c;
    }

    /** Handles the traversion into a calling procedure.
     * The new context is build by removing the two topmost vertices of the
     * old context and then adding the target of the traversed edge
     * on top.
     *
     * @param old  The old context.
     * @param target  The traversed edge's target.
     */
    private DynamicContext up(DynamicContext old, SDGNode target) {
    	DynamicContext c = old.copyWithNewNode(target);

        c.pop();

        return c;
    }

    /** Checks whether a given call site corresponds with the recent call site of
     * a given context.
     *
     * @param con  The context.
     * @param callSite  The call site.
     */
    private boolean match(DynamicContext con, SDGNode callSite) {
        return con.size() <= 1 || con.top() == callSite;
    }
}
