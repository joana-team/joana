/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph;
/*
 * Created on Feb 25, 2004
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

import edu.kit.joana.util.SourceLocation;
import edu.kit.joana.util.collections.ArrayMap;
import edu.kit.joana.util.collections.SimpleVector;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * Represents a concurrent system dependence graph (cSDG).
 * It can either be used to create SDGs by hand or to read-in an existing .pdg file.
 *
 * @see SDGEdge
 * @see SDGNode
 * @see SDGNode.NodeFactory
 */
public class SDG extends JoanaGraph implements Cloneable {
   
    public static final int DEFAULT_VERSION = 1;
    
    /** Indicates if the SDG contains precise source code info. */
	private boolean joanaCompiler;

	/** Caches the entry nodes of each procedure (maps procedure ID -> entry node).
	 * Lazily initialized by <code>getEntry</code>. */
    private final TIntObjectHashMap<SDGNode> entryCache = new TIntObjectHashMap<SDGNode>();
    private final TIntObjectHashMap<SDGNode> exitCache = new TIntObjectHashMap<SDGNode>();

    /** maps sdg nodes to ssa instruction indices */
    private TIntIntMap node2iindex = null;

    /** represents "no ssa instruction index" - returned by {@link #getInstructionIndex(SDGNode)} if there is no
     *  ssa instruction index for the given node
     */
    public static final int UNDEFINED_IINDEX = -1;

    private TIntIntMap entry2CGNode = null;
    private TIntIntMap cgNodeToEntry = null;
    /** represents "no ssa instruction index" - returned by {@link #getCGNodeId(SDGNode)} if there is no
     *  call graph node for the given node (for example, if the given node is not an entry node)
     */
    public static final int UNDEFINED_CGNODEID = -1;


    private String fileName = null;

    /**
     * Creates an empty SDG.
     */
    public SDG() {
        super(() -> new SimpleVector<>(5, 256));
        this.joanaCompiler = false;
    }

    /**
     * Creates an empty SDG with a name.
     */
    public SDG(String name) {
        super(name, () -> new SimpleVector<>(5, 256));
        this.joanaCompiler = false;
    }

	/**
	 * Returns a shallow copy of this SDG.
     */
    public SDG clone() {
    	SDG data = new SDG();

        // copy nodes and edges
        for (SDGNode n : vertexSet()) {
            data.addVertex(n);
        }

        for (SDGEdge e : edgeSet()) {
            data.addEdge(e);
        }

        // copy the other attributes
        data.name = name;
        data.SDGFactory = SDGFactory;
        data.root = root;
        data.ti = ti;
        data.joanaCompiler = joanaCompiler;

        return data;
    }

    /**
     * Set this flag to true if the SDGNodes in the SDG contain precise source code info.
     */
    public void setJoanaCompiler(boolean b) {
    	joanaCompiler = b;
    }

    /**
     * Sets the mapping of sdg nodes to respective ssa instruction indices.
     * @param node2iindex mapping
     */
    public void setNode2Instr(TIntIntMap node2iindex) {
    	this.node2iindex = node2iindex;
    }

    public void setEntryToCGNode(TIntIntMap entry2CGNode) {
    	this.entry2CGNode = entry2CGNode;
    }

	private TIntIntMap computeCGNodeToEntryMap(TIntIntMap entry2cgNode) {
		TIntIntMap ret = new TIntIntHashMap();
		for (SDGNode n : vertexSet()) {
			if (n.getKind() == SDGNode.Kind.ENTRY) {
				ret.put(entry2cgNode.get(n.getId()), n.getId());
			}
		}
		return ret;
	}

	/**
     *
     * @return  `true' if the SDGNodes in the SDG contain precise source code info.
     */
    public boolean getJoanaCompiler() {
    	return joanaCompiler;
    }

    /**
     * Returns the call node of a call site.
     *
     * @param actual  A call node or an actual-in/out node that belongs to a call site.
     */
    public SDGNode getCallSiteFor(SDGNode actual) {
        if (actual.getKind() != SDGNode.Kind.CALL && actual.getKind() != SDGNode.Kind.ACTUAL_IN && actual.getKind() != SDGNode.Kind.ACTUAL_OUT) {
            throw new IllegalArgumentException("Can only determine the call site for an actual or call node!!");
        }
        if (actual.getKind() == SDGNode.Kind.CALL) {
            return actual; // call nodes belong to their own call site
        } else {
            SDGNode n = actual;
            // follow control-dependence-expression edges from the source
            // node of 'edge' to the call node
            while(true){
                // the loop terminates because control-expression graphs are acyclic and because we
                // ensured at the beginning of this method that a call node will be reached
                for(SDGEdge e : incomingEdgesOf(n)){
                    if(e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR){
                        if(e.getSource().getKind() == SDGNode.Kind.CALL){
                            return e.getSource();
                        }
                        n = e.getSource();
                        break;
                    }
                 }
            }
        }
    }

    /**
     * Returns the call node of a call site.
     *
     * @param edge  An edge of the call site. Permitted are FORK-, CALL-
     * PARAMETER_IN-, PARAMETER_OUT-, FORK_IN-, FORK_OUT-, and RETURN edges.
     */
    public SDGNode getCallSiteFor(SDGEdge edge){ /* TODO: extend with JOIN edges */
        // easiest case
        if(edge.getKind() == SDGEdge.Kind.CALL || edge.getKind() == SDGEdge.Kind.FORK){
            return edge.getSource();
        }

        // we have to traverse to the actual-parameter graph of the call site and from there
        // to the call node
        SDGNode actualNode = null;

        if (edge.getKind() == SDGEdge.Kind.PARAMETER_IN || edge.getKind() == SDGEdge.Kind.FORK_IN) {
        	actualNode = edge.getSource();

        } else if (edge.getKind() == SDGEdge.Kind.PARAMETER_OUT
        		|| edge.getKind() == SDGEdge.Kind.FORK_OUT
        		|| edge.getKind() == SDGEdge.Kind.RETURN) {

        	actualNode = edge.getTarget();

        } else {
        	throw new IllegalArgumentException("Wrong kind of edge: "+edge.getKind()+" ! See the Javadoc description.");
        }

        return this.getCallSiteFor(actualNode);
    }

    /**
     * Returns the call site corresponding to a given interprocedural edge.
     *
     * @param edge  An edge of the call site. Permitted are FORK-, CALL-
     * PARAMETER_IN-, PARAMETER_OUT-, FORK_IN-, FORK_OUT-, and RETURN edges.
     * @return  The first node in the tuple is the call node, the second the entry node.
     */
    public SDGNodeTuple getCallEntryFor(SDGEdge edge){ /* TODO: extend with JOIN edges */
        // easiest case
        if(edge.getKind() == SDGEdge.Kind.CALL || edge.getKind() == SDGEdge.Kind.FORK){
            return new SDGNodeTuple(edge.getSource(), edge.getTarget());
        }

        boolean out = false;

        if (edge.getKind() == SDGEdge.Kind.PARAMETER_OUT
        		|| edge.getKind() == SDGEdge.Kind.FORK_OUT
        		|| edge.getKind() == SDGEdge.Kind.RETURN) {

        	out = true;

        } else if (edge.getKind() != SDGEdge.Kind.PARAMETER_IN && edge.getKind() != SDGEdge.Kind.FORK_IN) {
        	throw new IllegalArgumentException("Wrong kind of edge: "+edge.getKind()+" ! See the Javadoc description.");
        }

        SDGNode call = (out ? getCallSiteFor(edge.getTarget()) : getCallSiteFor(edge.getSource()));
        SDGNode entry = (out ? getEntry(edge.getSource()) : getEntry(edge.getTarget()));

        return new SDGNodeTuple(call, entry);
    }

    /**
     * Returns all parameter nodes of a call- or entry node.
     *
     * @param site  A call- or entry node.
     * @return A set (HashSet) containing the parameter nodes.
     */
    public Collection<SDGNode> getParametersFor(SDGNode site) {
    	if (site.getKind() != SDGNode.Kind.CALL && site.getKind() != SDGNode.Kind.ENTRY) {
        	throw new IllegalArgumentException("Wrong kind of node: "+site.getKind()+" ! See the Javadoc description.");
    	}

    	HashSet<SDGNode> result = new HashSet<SDGNode>();
    	LinkedList<SDGNode> wl = new LinkedList<SDGNode>();

    	wl.add(site);
    	result.add(site);

    	while(!wl.isEmpty()) {
    		SDGNode next = wl.poll();

    		for (SDGEdge e : getOutgoingEdgesOfKind(next, SDGEdge.Kind.CONTROL_DEP_EXPR)) {
    			if (result.add(e.getTarget())) {
    				wl.add(e.getTarget());
    			}
    		}
    	}

    	return result;
    }

    /**
     * Returns the subgraph confined by the given nodes.
     * The subgraph contains only the nodes in the collection and all edges between them.
     * All other attributes of the subgraph have initial values (e.g. name == null).
     *
     * @param vertices  Each node in the collection must be part of this graph.
     */
    public SDG subgraph(Collection<SDGNode> vertices) {
    	SDG g = new SDG();

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

    /**
     * Returns the entry node of the procedure that is specified by the given node.
     */
    private synchronized SDGNode getEntrySlow(SDGNode node){
    	if (entryCache.get(node.getProc()) == null) {
	        SDGNode entry = null;
	        LinkedList<SDGNode> wl = new LinkedList<SDGNode>();
	        HashSet<SDGNode> set = new HashSet<SDGNode>();
	        wl.add(node);
	        set.add(node);

	        // Find corresponding entry node of 'node' in the graph.
	        // It has the same procedure ID.
	        while (!wl.isEmpty()) {
	        	SDGNode n = wl.poll();

	        	if (n.getKind() == SDGNode.Kind.ENTRY){
	        		entry = n;
	        		break;
	        	}

	        	for (SDGEdge e : incomingEdgesOf(n)) {
	        		if (e.getKind() != SDGEdge.Kind.CONTROL_DEP_CALL
	        				&& e.getKind() != SDGEdge.Kind.CONTROL_DEP_COND
	        				&& e.getKind() != SDGEdge.Kind.CONTROL_DEP_EXPR
	        				&& e.getKind() != SDGEdge.Kind.CONTROL_DEP_UNCOND
	        				&& e.getKind() != SDGEdge.Kind.HELP)
	        			continue;

	        		if (set.add(e.getSource())) {
	        			wl.addFirst(e.getSource());
	        		}
	        	}
	        }

	        if (entry == null) throw new RuntimeException("no entry for "+node);

	        entryCache.put(node.getProc(), entry);
	        return entry;

    	} else {
    		return entryCache.get(node.getProc());
    	}
    }
    
    public synchronized SDGNode getEntry(SDGNode node) {
    	final SDGNode entry = entryNodes.get(node.getProc());
    	
    	assert entry.equals(getEntrySlow(node));
    	
    	return entry;
    }

    
    @Override
    public synchronized SDGNode getExit(SDGNode node) {
    	final SDGNode exit = exitNodes.get(node.getProc());
    	
    	final SDGNode exitSlow;
    	assert ((exitSlow = getExitSlow(node)) == null) || exit.equals(exitSlow);
    	
    	return exit;
    }
    
    public synchronized SDGNode getExitSlow(SDGNode node){
    	if (exitCache.get(node.getProc()) == null) {
	        SDGNode entry = getEntry(node);
	        SDGNode exit = null;
        	for (SDGEdge e : getOutgoingEdgesOfKind(entry, SDGEdge.Kind.CONTROL_FLOW)) {
        		if (e.getTarget().getKind() == SDGNode.Kind.EXIT) {
        			exitCache.put(node.getProc(), e.getTarget());
        			exit = e.getTarget();
        			break;
        		}
        	}
        	return exit;
    	} else {
    		return exitCache.get(node.getProc());
    	}
    }

	/**
	 * Returns all call nodes that call the given entry node.
	 */
	public Collection<SDGNode> getCallers(SDGNode entryNode) {
		LinkedList<SDGNode> ret = new LinkedList<SDGNode>();

		for (SDGEdge wEdge : incomingEdgesOf(entryNode)) {
			if (wEdge.getSource().getKind() == SDGNode.Kind.CALL) {
				ret.add(wEdge.getSource());
			}
		}

		return ret;
	}

	public List<SDGNodeTuple> getAllCallSites() {
		List<SDGNodeTuple> result = new LinkedList<SDGNodeTuple>();

		for (SDGNode n : vertexSet()) {
			if (n.getKind() == SDGNode.Kind.CALL) {
				List<SDGEdge> l = getOutgoingEdgesOfKind(n, SDGEdge.Kind.CONTROL_FLOW);
//				if (l.size() != 1) throw new RuntimeException(); // irregular SDG
				if (l.isEmpty()) continue;
				result.add(new SDGNodeTuple(n, l.get(0).getTarget()));
			}
		}

		return result;
	}

	/**
	 * Returns the formal-out nodes connected with the given actual-out node.
	 */
	public Collection<SDGNode> getFormalOuts(SDGNode actualOut) {
		LinkedList<SDGNode> ret = new LinkedList<SDGNode>();

		for (SDGEdge e : incomingEdgesOf(actualOut)) {
			if (e.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
				ret.add(e.getSource());
			}
		}

		return ret;
	}

	/**
	 * Returns all formal-out nodes of a procedure.
	 * Note that the exit node accounts as a formal-out node and is therefore added to the result.
	 *
	 * @param entry  The entry node of the procedure.
	 */
	public Set<SDGNode> getFormalOutsOfProcedure(SDGNode entry) {
		LinkedList<SDGNode> wl = new LinkedList<SDGNode>();
		HashSet<SDGNode> fo = new HashSet<SDGNode>();

		wl.add(entry);

		while (!wl.isEmpty()) {
			SDGNode next = wl.poll();

			// traverse the object trees of the formal parameters
			// and collect the encountered formal-out nodes
			for (SDGEdge e : outgoingEdgesOf(next)) {
				if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
					wl.add(e.getTarget());

				}

				if (e.getTarget().getKind() == SDGNode.Kind.FORMAL_OUT) {
					fo.add(e.getTarget());
				}
			}
		}
		
		fo.add(getExit(entry));

		return fo;
	}

	/**
	 * Returns all formal-in nodes of a procedure.
	 *
	 * @param entry  The entry node of the procedure.
	 */
	public Set<SDGNode> getFormalInsOfProcedure(SDGNode entry) {
		LinkedList<SDGNode> wl = new LinkedList<SDGNode>();
		HashSet<SDGNode> fo = new HashSet<SDGNode>();

		wl.add(entry);

		while (!wl.isEmpty()) {
			SDGNode next = wl.poll();

			// traverse the object trees of the formal parameters
			// and collect the encountered formal-in nodes
			for (SDGEdge e : outgoingEdgesOf(next)) {
				if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
					wl.add(e.getTarget());

				}

				if (e.getTarget().getKind() == SDGNode.Kind.FORMAL_IN) {
					fo.add(e.getTarget());
				}
			}
		}

		return fo;
	}

	/**
	 * Returns the formal-in nodes connected with the given actual-in node.
	 */
	public Collection<SDGNode> getFormalIns(SDGNode actualIn) {
		LinkedList<SDGNode> ret = new LinkedList<SDGNode>();

		for (SDGEdge e : outgoingEdgesOf(actualIn)) {
			if (e.getKind() == SDGEdge.Kind.PARAMETER_IN
					|| e.getKind() == SDGEdge.Kind.FORK_IN) {
				ret.add(e.getTarget());
			}
		}

		return ret;
	}

	/**
	 * Returns all entry nodes called by the given call node.
	 */
	public Collection<SDGNode> getPossibleTargets(SDGNode call) {
		LinkedList<SDGNode> ret = new LinkedList<SDGNode>();

		for (SDGEdge e : outgoingEdgesOf(call)) {
			if (e.getKind() == SDGEdge.Kind.CALL) {
				ret.add(e.getTarget());
			}
		}

		return ret;
	}

	/**
	 * Returns the index of the ssa instruction corresponding to the given sdg node. If
	 * the given node has no ssa instruction index or the information is not available,
	 * {@link SDG#UNDEFINED_IINDEX} is returned.
	 * @param node node to retrieve ssa instruction index of
	 * @return the index of the ssa instruction corresponding to the given sdg node or
	 * {@link SDG#UNDEFINED_IINDEX}, if there is none available
	 */
	public int getInstructionIndex(SDGNode node) {
		if (node2iindex != null && node != null && node2iindex.containsKey(node.getId())) {
			return node2iindex.get(node.getId());
		} else {
			return UNDEFINED_IINDEX;
		}
	}

	/**
	 * Returns the id of the call graph node corresponding to the given sdg node. If
	 * the given node has no corresponding call graph node (for example, if the given
	 * node is no entry node) or the information is not available,
	 * {@link SDG#UNDEFINED_CGNODEID} is returned.
	 * @param node node to retrieve call graph node id of
	 * @return the id of the call graph node corresponding to the given sdg node or
	 * {@link SDG#UNDEFINED_CGNODEID}, if there is none available
	 */
	public int getCGNodeId(SDGNode node) {
		if (entry2CGNode != null && node != null && entry2CGNode.containsKey(node.getId())) {
			return entry2CGNode.get(node.getId());
		} else {
			return UNDEFINED_CGNODEID;
		}
	}

	/**
	 * Returns the entry node of the procedure dependence graph corresponding to the
	 * given call graph node. If the information is not available or if the given
	 * call graph node has no corresponding PDG, {@code null} is returned.
	 * @param cgNodeId call graph node for which we want to know the corresponding SDG entry node
	 * @return entry node of the PDG corresponding to the given call graph node, or {@code null}
	 * if this information is not available or if the call graph node has no corresponding
	 * SDG entry node
	 */
	public SDGNode getSDGNode(int cgNodeId) {
		if (entry2CGNode == null) {
			// information not available --> return null
			return null;
		}

		// information is available
		if (cgNodeToEntry == null) {
			// have not inverted the map yet...do it first!
			this.cgNodeToEntry = computeCGNodeToEntryMap(this.entry2CGNode);
		}
		// do we have a value for the given id?
		if (this.cgNodeToEntry.containsKey(cgNodeId)) {
			// return that value!
			return getNode(this.cgNodeToEntry.get(cgNodeId));
		} else {
			// no value --> return null
			return null;
		}
	}

	/**
	 * Returns all actual-out nodes connected with the given formal-out node.
	 */
	public Collection<SDGNode> getActualOuts(SDGNode formalOut) {
		LinkedList<SDGNode> ret = new LinkedList<SDGNode>();

		for (SDGEdge e : outgoingEdgesOf(formalOut)) {
			if (e.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
				ret.add(e.getTarget());
			}
		}

		return ret;
	}

	/**
	 * Returns the actual-out node connected with the given formal-out node in the call site specified by the given call node.
	 *
	 * @return `null' if the the call- and formal-out node are unrelated.
	 */
	public SDGNode getActualOut(SDGNode callNode, SDGNode formalOut) {
		for (SDGNode actOut : getActualOuts(formalOut)) {
			if (callNode == getCallSiteFor(actOut)) {
				return actOut;
			}
		}
		return null;
	}

	/**
	 * Returns all actual-in nodes connected with the given formal-in node.
	 */
	public Collection<SDGNode> getActualIns(SDGNode formalIn) {
		LinkedList<SDGNode> ret = new LinkedList<SDGNode>();

		for (SDGEdge e : incomingEdgesOf(formalIn)) {
			if ((e.getKind() == SDGEdge.Kind.PARAMETER_IN
					|| e.getKind() == SDGEdge.Kind.FORK_IN)
					&& e.getSource().getKind() == SDGNode.Kind.ACTUAL_IN) {
				ret.add(e.getSource());
			}
		}

		return ret;
	}

	/**
	 * Returns the actual-in node connected with the given formal-in node in the call site specified by the given call node.
	 *
	 * @return `null' if the the call- and formal-in node are unrelated.
	 */
	public SDGNode getActualIn(SDGNode callNode, SDGNode formalIn) {
		for (SDGNode actIn: getActualIns(formalIn)) {
			if (callNode == getCallSiteFor(actIn)) {
				return actIn;
			}
		}
		return null;
	}

	/**
	 * Returns all {formal-in, entry}/formal-out pairs belonging to the given {actual-in, call}/actual-out pair.
	 * Warning:  The given {actual-in, call}/actual-out pair should stem from the same call site.
	 * This is _not_ checked by the procedure.
	 *
	 * @param actIn  An actual-in- or call node.
	 * @param actOut  An actual-out node.
	 * @return  A set of SDGNodeTuples, where the first node of each tuple is a formal-in- or entry node
	 * and the second node is a formal-out node.
	 */
	public Collection<SDGNodeTuple> getAllFormalPairs(SDGNode actIn, SDGNode actOut) {
		LinkedList<SDGNodeTuple> result = new LinkedList<SDGNodeTuple>();
		HashMap<Integer, SDGNode> map = new HashMap<Integer, SDGNode>();
		Collection<SDGNode> fos = getFormalOuts(actOut);
		Collection<SDGNode> fis = (actIn.getKind() == SDGNode.Kind.CALL ? getPossibleTargets(actIn) : getFormalIns(actIn));

		for (SDGNode fo : fos) {
			map.put(fo.getProc(), fo);
		}

		for (SDGNode fi : fis) {
			SDGNode fo = map.get(fi.getProc());
			if (fo != null) {
				result.add(new SDGNodeTuple(fi, fo));
			}
		}

		return result;
	}

	/**
	 * Checks if the given edge connects a class initializer with the rest of the SDG.
	 * This is done via call- or parameter-in edges whose _source_ is a formal-out node (only Christian Hammer knows why).
	 */
	public boolean isClassInitializer(SDGEdge edge) {
        return (edge.getKind() == SDGEdge.Kind.CALL || edge.getKind() == SDGEdge.Kind.PARAMETER_IN)
        		&& edge.getSource().getKind() == SDGNode.Kind.FORMAL_OUT;
    }

    /**
     * Returns all summary edges of the given call site.
     *
     * @param call  A call node specifying the call site.
     */
	public Collection<SDGEdge> getSummaryEdges(SDGNode call) {
		LinkedList<SDGNode> wl = new LinkedList<SDGNode>();
		LinkedList<SDGEdge> se = new LinkedList<SDGEdge>();

		wl.add(call);

		while (!wl.isEmpty()) {
			SDGNode next = wl.poll();

			// traverse the object trees of the actual parameters and collect the
			// encountered summary edges
			for (SDGEdge e : outgoingEdgesOf(next)) {
				if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR && e.getTarget().getKind() == SDGNode.Kind.ACTUAL_IN) {
					wl.add(e.getTarget());

				} else if (e.getKind() == SDGEdge.Kind.SUMMARY) {
					se.add(e);
				}
			}
		}

		return se;
	}

	/**
	 * Returns n random nodes of this graph.
	 * Used for evaluations.
	 * Throws an IllegalArgumentException in case n > #nodes.
	 * @param random the random number generator to be used
	 * @return  A list of nodes, free of duplicates.
	 */
	public List<SDGNode> getNRandomNodes(int n, Random random) {
		if (n > vertexSet().size()) {
			throw new IllegalArgumentException("n is too big: the SDG contains only "+vertexSet().size()+" nodes.");
		}

		LinkedList<SDGNode> nodes = new LinkedList<SDGNode>();
		nodes.addAll(vertexSet());
		Collections.shuffle(nodes, random);

		return nodes.subList(0, n);
	}
	
	/**
	 * Returns n random nodes of this graph.
	 * Used for evaluations.
	 * Throws an IllegalArgumentException in case n > #nodes.
	 *
	 * @return  A list of nodes, free of duplicates.
	 */
	public List<SDGNode> getNRandomNodes(int n) {
		return getNRandomNodes(n, new Random());
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

	/**
	 * Returns a HashMap that maps procedure IDs to all nodes of that procedure.
	 * The map is generated each time the procedure is called. If you need the result more than once, you
	 * might want to cache it locally.
	 */
	public TIntObjectHashMap<HashSet<SDGNode>> sortNodesByProcedure() {
		TIntObjectHashMap<HashSet<SDGNode>> result = new TIntObjectHashMap<HashSet<SDGNode>>();

		for (SDGNode n : vertexSet()) {
			HashSet<SDGNode> proc = result.get(n.getProc());

			if (proc == null) {
				proc = new HashSet<SDGNode>();
				result.put(n.getProc(), proc);
			}

			proc.add(n);
		}

		return result;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

    /* ********* */
    /* FACTORIES */

    /**
     * Read in a graph from file with SDGManualParser. Use this for large SDGs if normal parser runs out of heap space.
     * This parser is more fragile and does less error checking. However it is optimized for minimal memory usage.
     * @param sdgFile file which is parsed
     * @throws IOException if file does not exist
     * @return a SDG representation of the file
     */
    public static SDG readFromAndUseLessHeap(final String sdgFile) throws IOException {
    	return readFromAndUseLessHeap(sdgFile, null);
    }

    /**
     * Read in a graph from file with SDGManualParser. Use this for large SDGs if normal parser runs out of heap space.
     * This parser is more fragile and does less error checking. However it is optimized for minimal memory usage.
     * @param sdgFile file which is parsed
     * @param nodeFactory factory that is used to create SDGNodes
     * @throws IOException if file does not exist
     * @return a SDG representation of the file
     */
    public static SDG readFromAndUseLessHeap(final String sdgFile, final SDGNode.NodeFactory nodeFactory)
    		throws IOException {
    	final InputStream in = new FileInputStream(sdgFile);
    	final SDG sdg = readFromAndUseLessHeap(in, nodeFactory);
    	in.close();
    	final int sepIndex = sdgFile.lastIndexOf(File.separator);
    	final String fileName = (sepIndex > 0 ? sdgFile.substring(sepIndex) : sdgFile);
    	sdg.setFileName(fileName);

    	return sdg;
    }

    /**
     * Read in a graph from an input stream with SDGManualParser. Use this for large SDGs if normal parser runs out of
     * heap space. This parser is more fragile and does less error checking. However it is optimized for minimal memory
     * usage.
     * @param in InputStream which is parsed
     * @throws IOException if file does not exist
     * @return a SDG representation of the file
     */
    public static SDG readFromAndUseLessHeap(final InputStream in) throws IOException {
    	return readFromAndUseLessHeap(in, null);
    }

    /**
     * Read in a graph from an input stream with SDGManualParser. Use this for large SDGs if normal parser runs out of
     * heap space. This parser is more fragile and does less error checking. However it is optimized for minimal memory
     * usage.
     * @param in InputStream which is parsed
     * @param nodeFactory factory that is used to create SDGNodes
     * @throws IOException if file does not exist
     * @return a SDG representation of the file
     */
    public static SDG readFromAndUseLessHeap(final InputStream in, final SDGNode.NodeFactory nodeFactory)
    		throws IOException {
    	SDG sdg = null;
    	try {
    		sdg = SDGManualParser.parse(in, nodeFactory);
    	} catch (RecognitionException e) {
    		throw new IOException(e);
    	}

    	return sdg;
    }

	/**
	 * Parses a graph, using the ANTLR grammar <code>SDG_.g</code>
	 *
	 * @param sdgFile file which is parsed
	 * @throws IOException if file does not exist
	 * @return a SDG representation of the file
	 */
	public static SDG readFrom(String sdgFile) throws IOException {
		SDG_Lexer lexer = new SDG_Lexer(new ANTLRFileStream(sdgFile));
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		SDG_Parser parser = new SDG_Parser(tokens);
		SDG sdg;
		try {
			sdg = parser.sdg_file();
		} catch (RecognitionException e) {
			throw new IOException(e);
		} finally {
			SourceLocation.clearSourceLocationPool();
		}
	
		final int sepIndex = sdgFile.lastIndexOf(File.separator);
		String fileName = (sepIndex > 0 ? sdgFile.substring(sepIndex) : sdgFile);
		sdg.setFileName(fileName);
	
		return sdg;
	}

	/**
     * Parses a graph, using the ANTLR grammar <code>SDG_.g</code>
     *
     * @param sdgFile file which is parsed
     * @param nodeFactory defines the subclass of SDGNode which shall be used for node creation.
     * @throws IOException if file does not exist
     * @return a SDG representation of the file
     */
    public static SDG readFrom(String sdgFile, SDGNode.NodeFactory nodeFactory) throws IOException {
    	SDG_Lexer lexer = new SDG_Lexer(new ANTLRFileStream(sdgFile));
    	CommonTokenStream tokens = new CommonTokenStream();
    	tokens.setTokenSource(lexer);
    	SDG_Parser parser = new SDG_Parser(tokens);
    	parser.setNodeFactory(nodeFactory);
    	SDG sdg;
    	try {
    		sdg = parser.sdg_file();
    	} catch (RecognitionException e) {
    		throw new IOException(e);
    	} finally {
			SourceLocation.clearSourceLocationPool();
		}

    	final int sepIndex = sdgFile.lastIndexOf(File.separator);
    	String fileName = (sepIndex > 0 ? sdgFile.substring(sepIndex) : sdgFile);
    	sdg.setFileName(fileName);

    	return sdg;
    }

    /**
     * Parses a graph, using the ANTLR grammar <code>SDG_.g</code>
     *
     * @param sdgFile file which is parsed
     * @throws IOException if file does not exist
     * @return a SDG representation of the stream
     */
    public static SDG readFrom(Reader sdgFile) throws IOException {
    	SDG_Lexer lexer = new SDG_Lexer(new ANTLRReaderStream(sdgFile));
    	CommonTokenStream tokens = new CommonTokenStream();
    	tokens.setTokenSource(lexer);
    	SDG_Parser parser = new SDG_Parser(tokens);
    	SDG sdg;
    	try {
    		sdg = parser.sdg_file();
    	} catch (RecognitionException e) {
    		throw new IOException(e);
    	} finally {
			SourceLocation.clearSourceLocationPool();
		}

    	// no filename can be set here. -> Set to null initially.

    	return sdg;
    }
}

