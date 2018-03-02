/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import edu.kit.joana.ifc.sdg.graph.JoanaGraph;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.util.collections.ArrayMap;
import edu.kit.joana.util.graph.AbstractBaseGraph.DirectedEdgeContainer;


/** A CFG.
 *
 * @author Dennis Giffhorn
 * @version 1.0
 */
public class CFG extends JoanaGraph {
	
	private static final long serialVersionUID = -2359630877247373264L;

	/**
	 * copy constructor - creates a CFG object, which shares all the nodes and edges with the given CFG.
	 * @param g cfg to copy
	 */
	public CFG(CFG g) {
		this();
		addAllVertices(g.vertexSet());
		addAllEdges(g.edgeSet());
		setRoot(g.getRoot());
	}
	
	/**
	 * Constructs an empty CFG.
	 */
	public CFG() {
		// TODO: maybe we want a different Map implementation here?!?!
		super(() -> new ArrayMap<>());
	}
	
	public CFG(Supplier<Map<SDGNode, DirectedEdgeContainer<SDGEdge, SDGEdge[]>>> vertexMapConstructor) {
		super(vertexMapConstructor);
	}
	
	
	
    /** Adds an edge to the graph.
     *
     * @param edge  The edge to add.
     */
    public boolean addEdge(SDGEdge edge) {
    	if (!edge.getKind().isControlFlowEdge()
    			&& edge.getKind() != SDGEdge.Kind.FOLD_INCLUDE
    			&& edge.getKind() != SDGEdge.Kind.HELP)
    		throw new IllegalArgumentException("I am a CFG. Don't add "+edge.getKind()+"-edges!");

    	return super.addEdge(edge);
    }

    /** Creates a subgraph form a sub-collection of the graph's nodes.
     *
     * @param vertices  The collection of nodes.
     */
    public CFG subgraph(Collection<SDGNode> vertices) {
        CFG g = new CFG();

        for (SDGNode n : vertices) {
            g.addVertex(n);
        }

        for (SDGNode n : vertices) {
            for (SDGEdge e : outgoingEdgesOf(n)) {
                if (vertices.contains(e.getTarget())) {
                    g.addEdge(e);
                }
            }
        }

        return g;
    }

    /** Returns the entry node of the procedure a given node belongs to.
     * If the entry node is folded, the fold node is returned.
     *
     * @param node  The node.
     */
    private SDGNode getEntrySlow(SDGNode node){
 	   // TODO: find a more efficient implementation
        SDGNode entry = null;
        java.util.Set<SDGNode> set = vertexSet();

        // Find corresponding entry node of 'node' in the graph.
        // It has the same procedure ID.
        for (SDGNode n : set){
            if (n.getProc() == node.getProc() && n.getKind() == SDGNode.Kind.ENTRY){
                entry = n;

                if (incomingEdgesOf(n).isEmpty()){
                    // maybe folded node
                    for (SDGEdge e : outgoingEdgesOf(n)){
                        if(e.getKind() == SDGEdge.Kind.FOLD_INCLUDE){
                            // folded node
                            entry = e.getTarget();
                            break;
                        }
                    }
                }
            }
        }

        return entry;
    }
    
    /** Returns the entry node of the procedure a given node belongs to.
     * If the entry node is folded, the fold node is returned.
     *
     * @param node  The node.
     */
    public SDGNode getEntry(SDGNode node) {
    	SDGNode entry = entryNodes.get(node.getProc());
    	if (incomingEdgesOf(entry).isEmpty()) {
            for (SDGEdge e : outgoingEdgesOf(entry)) {
                if(e.getKind() == SDGEdge.Kind.FOLD_INCLUDE){
                    // folded node
                    entry = e.getTarget();
                    break;
                }
            }
    	}
    	
    	assert entry.equals(getEntrySlow(node));
    	
    	return entry;
    }

    /**
     * Splits an ICFG into the CFGs of its procedures.
     * The original ICFG remains untouched, the returned CFGs are newly created graphs.
     *
     * @return
     */
    public List<CFG> split() {
    	LinkedList<CFG> result = new LinkedList<CFG>();
    	LinkedList<SDGNode> entries = new LinkedList<SDGNode>();

		// collect the entry nodes
    	for (SDGNode n : vertexSet()) {
    		if (n.getKind() == SDGNode.Kind.ENTRY) {
    			entries.add(n);
    		}
    	}

    	// build the CFG's of the single procedures
    	for (SDGNode entry : entries) {
    		Set<SDGNode> procedure = getNodesOfProcedure(entry);
    		CFG cfg = subgraph(procedure);
    		cfg.setRoot(entry);
    		result.add(cfg);
    	}

    	return result;
    }

    /**
	 * Returns n random nodes of this graph.
	 * Used for evaluations.
	 * Throws an IllegalArgumentException in case n > #nodes.
	 *
	 * @return  A list of nodes, free of duplicates.
	 */
	public List<SDGNode> getNRandomNodes(int n) {
		if (n > vertexSet().size()) {
			throw new IllegalArgumentException("n is too big: the SDG contains only "+vertexSet().size()+" nodes.");
		}

		LinkedList<SDGNode> nodes = new LinkedList<SDGNode>();
		nodes.addAll(vertexSet());
		Collections.shuffle(nodes);

		return nodes.subList(0, n);
	}

	/**
	 * Returns n nodes of this graph.
	 * In particular, for a given n it always returns the same n nodes.
	 * Used for evaluations, particularly in case reproducible results are needed.
	 *
	 * Divides the set of nodes into n segments and takes the first node of each segment.
	 * The offset is used exclude the first -offset- nodes from that procedure.
	 *
	 * Throws an IllegalArgumentException in case offset > #nodes or n > (#nodes - offset).
	 *
	 * @return  A list of nodes, free of duplicates.
	 */
	public List<SDGNode> getNNodes(int n, int offset)
	throws IllegalArgumentException {
		if (offset > vertexSet().size()) {
			throw new IllegalArgumentException("offset is too big: the SDG contains only "+vertexSet().size()+" nodes.");
		}

		if (n > (vertexSet().size() - offset)) {
			throw new IllegalArgumentException("n is too big: the SDG contains only "+vertexSet().size()+" nodes " +
					"and the offset accounts for "+offset+" nodes.");
		}

		int div = (vertexSet().size() - offset) / n;
		int ctr = 0;
		LinkedList<SDGNode> nodes = new LinkedList<SDGNode>();

		for (SDGNode m : vertexSet()) {
			ctr++;

			if ((ctr - offset) > 0  && (ctr - offset) % div == 0) {
				nodes.add(m);
			}

			if (nodes.size() == n) break;
		}

		return nodes;
	}

}
