/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph;

import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.jgrapht.EdgeFactory;

import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;
import edu.kit.joana.util.graph.AbstractJoanaGraph;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * This is the abstract superclass or our graph classes. We currently distinguish
 * - SDGs,
 * - CFGs,
 * - and call graphs.
 *
 *  The JoanaGraph bundles as much common code as possible.
 *
 *  A word to SDGNodes and SDGEdges in JoanaGraphs:
 *  - Nodes in a JoanaGraph and in graphs derived from it via algorithms in the edu.kit.joana.ifc.sdg.graph.slicer.graph.building package
 *    (e.g. call graphs) can be uniquely identified by their object reference. None of those algorithms duplicates
 *    existing nodes. <br>This should remain common practice in forthcoming algorithms! Don't duplicate nodes!</br>
 *
 *  - However, this is not the case for SDGEdges. <br>Do not compare SDGEdges via `==' !</br>
 *
 *  - Each node in a JoanaGraph has a unique ID. However, there may exist nodes with negative IDs
 *    (conventionally assigned to fold nodes that are created when cycles are folded).
 *    Furthermore, the enumeration of nodes does not follow a strictly scheme. It is _not_ the case that the nodes
 *    of the same procedure occupy a closed range of IDs. It is _not_ the case that a node has a smaller ID than its
 *    successors, and so on.
 *    Finally, the total range of IDs may have gaps. If there is a node with ID=2 and one with ID=4 it is
 *    entirely possible that there is no node with ID=3 at all. This happens, for example, if a CFG is derived
 *    from an SDG, because nodes that are specific to the SDG are not added to the CFG.
 *    As a consequence: <br>Don't make any assumptions about node IDs besides their uniqueness!</br>
 *
 *
 * @author giffhorn
 * @see edu.kit.joana.ifc.sdg.graph.SDG
 * @see edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG
 * @see edu.kit.joana.ifc.sdg.graph.slicer.graph.CallGraph
 */
public abstract class JoanaGraph extends AbstractJoanaGraph<SDGNode, SDGEdge> {

	/** The name of the graph. */
	protected String name;
	/** Stores the nodes and edges. Managed by the underlying JGraphT library.
	 * *Don't even think about modifying it manually!*  */
	protected EdgeFactory<SDGNode, SDGEdge> SDGFactory;
	/** Stores information about the threads of the program. */
	protected ThreadsInformation ti;
	/** The root node. */
	protected SDGNode root;
	private final TIntObjectHashMap<SDGNode> id2node = new TIntObjectHashMap<SDGNode>();
	
	protected final Map<Integer, SDGNode> entryNodes = new HashMap<>();

	/**
	 * Creates a totally empty JoanaGraph.
	 */
    public JoanaGraph() {
        super(SDGEdge.class);
    }

    /**
     * Creates an empty JoanaGraph with a name.
     */
    public JoanaGraph(String name) {
        this();
        this.name = name;
    }

    /**
     * @return information about the threads.
     */
    public ThreadsInformation getThreadsInfo() {
        return ti;
    }

    /**
     * Sets the thread information to the given value.
     * @param ti
     */
    public void setThreadsInfo(ThreadsInformation ti) {
        this.ti = ti;
    }

    /**
     * @return the number of threads that are modeled in the graph.
     */
    public int getNumberOfThreads() {
        return ti.getNumberOfThreads();
    }

    /**
     * Returns the root of the graph.
     * Lazily initializes the root attribute.
     * Can return null if no root exists.
     */
    public SDGNode getRoot() {
    	return root == null ? root() : root;
    }

    public void setRoot(SDGNode n) {
    	root = n;
    }

    /**
     * Determines the root of the graph and returns it.
     * By convention, the SDGNode with ID = 1 is assumed to be the root.
     * If no such node exists, it returns the first found node with an in-degree of 0.
     * If no such node exists, it returns null.
     */
    private SDGNode root() {
    	root = getNode(1);

    	if (root == null || this.inDegreeOf(root) > 0) {
    		for (SDGNode n : vertexSet()) {
    			if (this.inDegreeOf(n) == 0 && !this.isFolded(n)) {
    				root = n;
    				break;
    			}
    		}
    	}

    	return root;
    }

    /**
     * @return `true' if the given node is folded.
     */
    public boolean isFolded(SDGNode node){
    	// search for outgoing FOLD_INCLUDE edges
    	for (SDGEdge e : outgoingEdgesOf(node)) {
    		if (e.getKind() == SDGEdge.Kind.FOLD_INCLUDE) {
    			return true;
    		}
    	}
    	return false;
    }

    /**
     * Returns the node with the given ID.
     * @param id
     * @return  the corresponding node or null, if no such node exists.
     */
    public SDGNode getNode(int id) {
  	    return id2node.get(id);
    }

    /**
     * @return the name of the graph. Can be null.
     */
    public String getName() {
    	return name;
    }

    /**
     * Sets the name of the graph.
     */
    public void setName(String name) {
    	this.name = name;
    }

    /**
     * Checks whether a given vertex has outgoing edges of a given kind.
     * @param node  The vertex to check.
     * @param kind  The demanded kind of edges.
     * @return `true' if node has outgoing edges of kind `kind'.
     */
    public boolean hasOutgoingEdgesOfKind(SDGNode node, SDGEdge.Kind kind) {
    	for (SDGEdge e : outgoingEdgesOf(node)) {
    		if (e.getKind() == kind) {
    			return true;
    		}
    	}

    	return false;
    }

    /**
     * Returns a list with all outgoing edges of a given kind of a given vertex.
     *
     * @param node  The vertex whose edges are needed.
     * @param kind  The demanded kind of edges.
     */
    public List<SDGEdge> getOutgoingEdgesOfKind(SDGNode node, SDGEdge.Kind kind) {
    	LinkedList<SDGEdge> res = new LinkedList<SDGEdge>();

    	for (SDGEdge e : outgoingEdgesOf(node)) {
    		if (e.getKind() == kind) {
    			res.add(e);
    		}
    	}

    	return res;
    }

    /**
     * Checks whether a given vertex has incoming edges of a given kind.
     *
     * @param node  The vertex to check.
     * @param kind  The demanded kind of edges.
     * @return `true' if node has incoming edges of kind `kind'.
     */
    public boolean hasIncomingEdgesOfKind(SDGNode node, SDGEdge.Kind kind) {
    	for (SDGEdge e : incomingEdgesOf(node)) {
    		if (e.getKind() == kind) {
    			return true;
    		}
    	}

    	return false;
    }

    /**
     * Returns a list with all incoming edges of a given kind of a given vertex.
     *
     * @param node  The vertex whose edges are needed.
     * @param kind  The demanded kind of edges.
     */
    public List<SDGEdge> getIncomingEdgesOfKind(SDGNode node, SDGEdge.Kind kind) {
    	LinkedList<SDGEdge> res = new LinkedList<SDGEdge>();

    	for (SDGEdge e : incomingEdgesOf(node)) {
    		if (e.getKind() == kind) {
    			res.add(e);
    		}
    	}

    	return res;
    }

   @Override
   public boolean addVertex(SDGNode node) {
	   boolean isNew = super.addVertex(node);
	   if (isNew) {
		   id2node.put(node.getId(), node);
	   }
	   
	   if (node.getKind().equals(SDGNode.Kind.ENTRY)) {
		   final SDGNode entry = entryNodes.put(node.getProc(), node);
		   if (entry != null && !entry.equals(node)) {
			   throw new IllegalArgumentException();
		   }
	   }

	   return isNew;
   }

   @Override
   public boolean removeVertex(SDGNode node) {
	   boolean isRemoved = super.removeVertex(node);

	   if (isRemoved) {
		   id2node.remove(node.getId());
	   }

	   return isRemoved;
   }

   public final int getMaxNodeID() {
	   int max = -1;
	   for (final SDGNode node : vertexSet()) {
		   if (node.getId() > max) {
			   max = node.getId();
		   }
	   }

	   return max;
   }

   /**
    * Adds an edge to the graph.
    * The source and target nodes of edge have to be in the graph, otherwise a NullPointerException is thrown.
    * @return `false' if the edge already exists.
    */
  	public boolean addEdge(SDGEdge edge) {
  		return super.addEdge(edge.getSource(), edge.getTarget(), edge);
  	}

  	/**
     * Adds a collection of edges to the graph.
     * The source and target nodes of the edges have to be in the graph, otherwise a NullPointerException is thrown.
     */
  	public void addAllEdges(Collection<SDGEdge> edges) {
  		for (SDGEdge edge : edges) {
			addEdge(edge);
		}
  	}

  	/**
     * Adds a collection of nodes to the graph.
     */
    public void addAllVertices(Collection<SDGNode> values) {
    	for (SDGNode node : values) {
    		addVertex(node);
		}
    }

    @Override
    public EdgeFactory<SDGNode, SDGEdge> getEdgeFactory() {
    	if (SDGFactory == null)
    		SDGFactory = new SDGFactory();
    	return SDGFactory;
    }

    /**
     * Replaces the edge factory.
     */
    public void setEdgeFactory(EdgeFactory<SDGNode, SDGEdge> fac) {
    	SDGFactory = fac;
    }

    /**
     * Sorts all nodes according to their procedure IDs.
     * The result is a map that maps the entry node of each procedure to the set of nodes of that procedure.
     *
     * @return the map.
     */
    public Map<SDGNode, Set<SDGNode>> sortByProcedures() {
    	HashMap<SDGNode, Set<SDGNode>> map = new HashMap<SDGNode, Set<SDGNode>>();
    	TIntObjectHashMap<Set<SDGNode>> aux = new TIntObjectHashMap<Set<SDGNode>>();

    	for (SDGNode n : vertexSet()) {
    		final int procId = n.getProc();
    		Set<SDGNode> set = aux.get(procId);

    		if (set == null) {
    			set = new HashSet<SDGNode>();
    			aux.put(procId, set);
    		}

    		set.add(n);

    		if (n.getKind() == SDGNode.Kind.ENTRY) {
    			map.put(n, null);
    		}
    	}

    	for (SDGNode entry : map.keySet()) {
    		map.put(entry, aux.get(entry.getProc()));
    	}

    	return map;
    }

    /**
     * @return `true' if the given node is a fork node.
     */
    public boolean isFork(SDGNode node) {
    	// there is no special kind for fork nodes - see if there are any outgoing fork edges
        for (SDGEdge edge : outgoingEdgesOf(node)) {
            if (edge.getKind() == SDGEdge.Kind.FORK ){
                return true;
            }
        }

        return false;
    }

    /**
     * @return `true' if the given node is the entry of a thread.
     */
    public boolean isThreadStart(SDGNode node) {
    	// see if there is any incoming fork edge
        for (SDGEdge edge : incomingEdgesOf(node)) {
            if (edge.getKind() == SDGEdge.Kind.FORK ){
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the highest ID of any node in the graph.
     */
    public int lastId() {
        int id = 0;

        for (SDGNode n : this.vertexSet()) {
            if (n.getId() > id)
                id = n.getId();
        }

        return id;
    }

    /**
     * Returns the highest ID of any procedure in the graph.
     */
    public int lastProc() {
        int id = 0;

        for (SDGNode n : this.vertexSet()) {
            if (n.getProc() > id)
                id = n.getProc();
        }

        return id;
    }

    /**
     * Returns a list containing all nodes that belong to the procedure of the given node.
     */
    protected List<SDGNode> getNodesOfProcedureSlow(SDGNode proc) {
        LinkedList<SDGNode> l = new LinkedList<SDGNode>();

        for (SDGNode n : vertexSet()) {
            if (n.getProc() == proc.getProc()) {
                l.add(n);
            }
        }

        return l;
    }   
    /**
     * Returns a list containing all nodes that belong to the procedure of the given node.
     */
    public Set<SDGNode> getNodesOfProcedure(final SDGNode node) {
    	final SDGNode entry = getEntry(node);
    	final int procedure = node.getProc();
    	
    	final Queue<SDGNode> wl = new LinkedList<>();
    	final Set<SDGNode> found = new HashSet<>();
    	
    	wl.offer(entry);
    	found.add(entry);
    	
    	{ 
    		SDGNode next;
    		while ((next = wl.poll()) != null) {
    			for (SDGEdge e : outgoingEdgesOf(next)) {
    				final SDGNode candidate = e.getTarget();
    				
    				if (candidate.getProc() != procedure) continue;
    				
    				if (found.add(candidate)) {
    					wl.offer(candidate);
    				}
    			}
    		}
    	}
    	
    	List<SDGNode> foundSlow;
    	Set<SDGNode> foundSlowSet = new HashSet<>();
    	assert ((foundSlow = getNodesOfProcedureSlow(node)) != null && (foundSlowSet.addAll(foundSlow) || true) && (foundSlowSet.size() == found.size()) && foundSlow.containsAll(found) && found.containsAll(foundSlow));
    	
        return found;
    }


    /* abstract methods */

    /**
     * Returns the subgraph consisting of the given nodes and all edges in between them.
     */
    public abstract JoanaGraph subgraph(Collection<SDGNode> vertices);

    /**
     * Returns the entry node of a procedure.
     *
     * @param node  A node that belongs to the desired procedure.
     * @return      The entry node.
     */
    public abstract SDGNode getEntry(SDGNode node);
}

