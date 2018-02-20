/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


/**
 * A two-phase-slicer that additionally computes summary edges.
 *
 * -- Created on February 6, 2006
 *
 * @author  Christian Hammer, Dennis Giffhorn
 * @deprecated  Works only with Hammer's SDGs (see the XXX-Flag in Method addSummaries)
 */
@Deprecated
public abstract class IncrementalSummarySlicer implements Slicer {
	private static final SDGEdge.Kind KIND = SDGEdge.Kind.HELP;
    protected Set<SDGEdge.Kind> omittedEdges = SDGEdge.Kind.threadEdges();
    protected SDG graph;
	Set<SDGNode> slice;
	LinkedList<SDGNode> worklist;
	LinkedList<SDGEdge> worklistDown;
	SortedSet<SDGEdge> pathEdge;

    /**
     * Creates a new instance of AbstractIncrementalSlicer
     */
    public IncrementalSummarySlicer(SDG g) {
        graph = g;
    }

    /**
     * Creates a new instance of AbstractIncrementalSlicer
     */
    public IncrementalSummarySlicer(SDG g, Set<SDGEdge.Kind> omitted) {
        graph = g;
        omittedEdges = omitted;
    }

    public void setGraph(SDG graph) {
        this.graph = graph;
    }

    public Collection<SDGNode> slice(SDGNode criterion) {
    	return slice(Collections.singleton(criterion));
    }

    public Collection<SDGNode> slice(Collection<SDGNode> c) {
    	slice = new HashSet<SDGNode>(c);//SubSet(g.vertexSet());
    	worklist = new LinkedList<SDGNode>();
    	worklistDown = new LinkedList<SDGEdge>();
    	pathEdge = new TreeSet<SDGEdge>(new Comparator<SDGEdge>() {
            public int compare(SDGEdge arg0, SDGEdge arg1) {
                return SDGNode.getIDComparator().compare(arg0.getSource(), arg1.getSource());
            }
    	});
    	for (SDGNode n : c) {
    		if (isTransitiveStart(n)) {
    			SDGEdge e =  KIND.newEdge(n, n);
				worklistDown.add(e);
				pathEdge.add(e);
    		} else {
    			worklist.add(n);
    		}
    	}

    	while (!worklist.isEmpty()) {
    		while (!worklist.isEmpty()) {
    			SDGNode w = worklist.poll();

    			for (SDGEdge e : edgesToTraverse(w)) {
    				if (!e.getKind().isSDGEdge() || omittedEdges.contains(e.getKind()))
    					continue;

    				SDGNode v = reachedNode(e);

    				if (!slice.contains(v)) {
    					if (downwardsEdge(e)) {
    						addPathEdge(v,  KIND.newEdge(v, v));

    					} else {
    						worklist.add(v);
    						slice.add(v);
    					}
    				}
    			}
    		}

    		// Summary edge computation
    		while (!worklistDown.isEmpty()) {
    			SDGEdge e1 = worklistDown.poll();
    			SDGNode s = e1.getSource(), w = e1.getTarget();

    			if (summaryFound(s)) {
    				addSummaries(e1);

    			} else {
    				for (SDGEdge e : edgesToTraverse(s)) {
    					if (!(phase2Edge(e)))
    						continue;

    					SDGNode v = reachedNode(e);

    					// follow descending edges for the slice
    					// but start a new path for summary edges
    					SDGEdge ee =  KIND.newEdge(v, downwardsEdge(e) ? v : w);
    					addPathEdge(v, ee);
    				}
    			}
    		}
    	}
    	Set<SDGNode> result = slice;
    	slice = null;
    	pathEdge = null;
    	worklist = null;
    	worklistDown = null;
    	return result;
    }

	private void addPathEdge(SDGNode newNode, SDGEdge edge) {
		if (!pathEdge.contains(edge)) {
			pathEdge.add(edge);
			worklistDown.add(edge);
			slice.add(newNode);
		}
	}

	private void addSummaries(SDGEdge currentPathEdge) {
		// transitive dependence found
		// connect corresponding actual-in and actual-out nodes
		for (SDGEdge pi : graph.getIncomingEdgesOfKind(reachedNode(currentPathEdge), SDGEdge.Kind.PARAMETER_IN)) {
			SDGNode ai = pi.getSource();

			SDGNode root = root(ai);
			for (SDGEdge po : graph.getOutgoingEdgesOfKindUnsafe(startedNode(currentPathEdge), SDGEdge.Kind.PARAMETER_OUT)) {
				SDGNode ao = po.getTarget();

				if (root == root(ao)) {

					SDGEdge sum =  SDGEdge.Kind.SUMMARY.newEdge(ai, ao);
					graph.addEdge(sum);
					// swap ai and ao iff forward
					ao = startedNode(sum);
					ai = reachedNode(sum);

					SortedSet<SDGEdge> toCheck = pathEdge.tailSet( KIND.newEdge(ao, ao));
					if (!toCheck.first().getSource().equals(ao) && slice.contains(ao) && !slice.contains(ai)) {
						// corresponding actual-out node is in slice but not in pathedge
						// => phase 1
						worklist.add(ai);
					} else for (SDGEdge path : new LinkedList<SDGEdge>(toCheck.headSet( KIND.newEdge(
							ao = graph.getNode(ao.getId() + 1), ao)))) { // XXX hack
						SDGEdge ne =  KIND.newEdge(ai, path.getTarget());
						addPathEdge(ai, ne);
					}
					break; // corresponding parameter pair done
				}
			}
		}
	}


	protected abstract boolean isTransitiveStart(SDGNode n);

    protected abstract Collection<SDGEdge> edgesToTraverse(SDGNode node);

    protected abstract SDGNode reachedNode(SDGEdge edge);

    protected abstract SDGNode startedNode(SDGEdge edge);

    protected abstract boolean downwardsEdge(SDGEdge edge);

    protected abstract boolean phase2Edge(SDGEdge edge);

    protected abstract boolean summaryFound(SDGNode reached);

    protected SDGNode root(SDGNode n) {
        while (true) {
            SDGNode parent = null;

            for (SDGEdge e : graph.incomingEdgesOf(n)) {
                if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
                    parent = e.getSource();
                    break;
                }
            }

            if (parent != null && parent.isParameter())
                n = parent;

            else // root found
                return parent;
        }
    }




    /* main- method */

    public static void main(String[] args) throws IOException {
        SDG g = SDG.readFrom("/Users/hammer/ifMain.Company.pdg");

        SDGNode c = g.getNode(453);
        IncrementalSummarySlicer slicer = new IncrementalSummaryForward(g);
        Collection<SDGNode> slice = slicer.slice(Collections.singleton(c));
        //Collection<SDGNode> slice1 = /*SDGSlicer.*/sliceMDG(g, Collections.singleton(c));
        System.out.println(slice.size());// + " " + slice1.size());

        System.out.println(slice);
        //slice.removeAll(slice1);
        //System.out.println(slice);
    }
}
