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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


/**
 * A context-insensitive slicer.
 *
 * @author hammer, giffhorn
 */
public abstract class ContextInsensitiveSlicer implements Slicer {
    protected Set<SDGEdge.Kind> omittedEdges = SDGEdge.Kind.threadEdges();
    protected SDG g;

    /**
     * Creates a new instance of ContextInsensitiveSlicer
     */
    protected ContextInsensitiveSlicer(SDG graph, Set<SDGEdge.Kind> omit) {
        this.g = graph;
        this.omittedEdges = omit;
    }

    /**
     * Creates a new instance of ContextInsensitiveSlicer
     */
    protected ContextInsensitiveSlicer(SDG graph) {
        this.g = graph;
    }

    public void setGraph(SDG graph) {
        g = graph;
    }

    public Collection<SDGNode> slice(SDGNode criterion) {
    	return slice(Collections.singleton(criterion));
    }

    public Collection<SDGNode> slice(Collection<SDGNode> criteria) {
    	HashSet<SDGNode> slice = new HashSet<SDGNode>();
    	LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
    	worklist.addAll(criteria);

    	for (SDGNode node : criteria) {
    		slice.add(node);
    	}

    	while (!worklist.isEmpty()) {
    		SDGNode w = worklist.poll();

    		for (SDGEdge e : edgesToTraverse(w)) {
    			if (!e.getKind().isSDGEdge() || omittedEdges.contains(e.getKind()))
    				continue;

    			SDGNode v = reachedNode(e);

    			if (slice.add(v)) {
    				worklist.addFirst(v);
    			}
    		}
    	}

    	return slice;
    }

    public Collection<SDGNode> slice(Collection<SDGNode> criteria, Collection<SDGNode> subgraph) {
    	HashSet<SDGNode> slice = new HashSet<SDGNode>();
    	LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
    	worklist.addAll(criteria);

    	for (SDGNode node : criteria) {
    		slice.add(node);
    	}

    	while (!worklist.isEmpty()) {
    		SDGNode w = worklist.poll();

    		for (SDGEdge e : edgesToTraverse(w)) {
    			if (!e.getKind().isSDGEdge() || omittedEdges.contains(e.getKind()))
    				continue;

    			SDGNode v = reachedNode(e);

    			if (!subgraph.contains(v)) continue;

    			if (slice.add(v)) {
    				worklist.addFirst(v);
    			}
    		}
    	}

    	return slice;
    }

    public void setOmittedEdges(Set<SDGEdge.Kind> omit){
        this.omittedEdges = omit;
    }

    protected abstract Collection<SDGEdge> edgesToTraverse(SDGNode node);

    protected abstract SDGNode reachedNode(SDGEdge edge);



    /* main- method */

    public static void main(String[] args) throws IOException {
        edu.kit.joana.ifc.sdg.graph.SDG g = edu.kit.joana.ifc.sdg.graph.SDG.readFrom("/home/st/hammer/scratch/pdg/tests.ProducerConsumer.pdg");
        SDGNode c = g.getNode(180);
        ContextInsensitiveSlicer slicer = new ContextInsensitiveBackward(g);
        Collection<SDGNode> slice = slicer.slice(Collections.singleton(c));
        //Collection<SDGNode> slice1 = /*SDGSlicer.*/sliceMDG(g, Collections.singleton(c));
        //System.out.println(slice.size() + " " + slice1.size());

        System.out.println(slice);
        //slice.removeAll(slice1);
        //System.out.println(slice);
    }
}
