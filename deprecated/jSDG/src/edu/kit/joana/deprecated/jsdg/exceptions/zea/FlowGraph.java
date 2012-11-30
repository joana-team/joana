/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.exceptions.zea;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Map.Entry;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.TypeReference;

public class FlowGraph {
    /**
     * main entry to this graph. a graph may end up having multiple entries due
     * to unconnected graphs, but this entry is the only one connected to the
     * main entry of the method
     */
    private Node entry = null;

    /**
     * main exit to this graph. a graph may end up having multiple exits due to
     * unconnected graphs or flattened loops, and all of them may be counted as
     * legitimate main exits, but we only count this one.
     */
    private Node exit = null;

    /** list of nodes with corresponding indices */
    private TreeMap<Integer, Node> nodes;

    /** list of loops in this node (only contains indices) */
    private LoopList loops;

    /** subgraph of loops in this node (full subgraph) */
    private Vector<FlowGraph> loopBlockSubgraphs;

    /** cgNode for class inheritance checks */
    private final CGNode cgNode;

    public FlowGraph(CGNode cgNode) {
        this.cgNode = cgNode;
    }

    Node getEntry() {
        return entry;
    }

    Node getExit() {
        return exit;
    }

    Collection<Node> getNodes() {
        return nodes.values();
    }

    Set<Integer> getNodeIndices() {
        return nodes.keySet();
    }

    boolean containsNode(int i) {
        return nodes.keySet().contains(i);
    }

    LoopList getLoops() {
        return loops;
    }

    FlowGraph copy() {
        FlowGraph graph = new FlowGraph(cgNode);
        graph.entry = entry;
        graph.exit = exit;
        graph.nodes = new TreeMap<Integer, Node>();

        checkConsistency();

        for (Entry<Integer, Node> entry : nodes.entrySet()) {
            Node node = graph.getNode(entry.getKey());
            node.initNode(entry.getValue().getBlock());

            for (Node child : entry.getValue().getChildren()) {
                node.getChildren().add(graph.getNode(child.getIndex()));
            }
            for (Node parent : entry.getValue().getParents()) {
                node.getParents().add(graph.getNode(parent.getIndex()));
            }
            for (Node parent : entry.getValue().getCaught()) {
                node.getCaught().add(graph.getNode(parent.getIndex()));
            }
            for (Entry<TypeReference, Node> handler : entry.getValue()
                    .getThrown().entrySet()) {
                node.getThrown().put(handler.getKey(),
                        graph.getNode(handler.getValue().getIndex()));
            }
            if (entry.getValue().hasCondClause()) {
                Node yesNode = graph
                        .getNode(entry.getValue().getCondClause().yesNode
                                .getIndex());
                Node noNode = graph
                        .getNode(entry.getValue().getCondClause().noNode
                                .getIndex());
                node.createCondClause(yesNode, noNode);
            }
        }
        for (Entry<Integer, Node> entry : graph.nodes.entrySet()) {
            if (entry.getValue().getBlock() == null) {
                System.out.println(entry.getKey() + " is null, orig is "
                        + nodes.get(entry.getKey()));
            }
        }
        graph.checkConsistency();
        return graph;
    }

    void init(ExplodedControlFlowGraph ecfg, TypeReference[] ignoreExceptions) {
        nodes = new TreeMap<Integer, Node>();

        // create hashset of ignored exceptions
        HashSet<TypeReference> ignored = new HashSet<TypeReference>();
        for (TypeReference type : ignoreExceptions) {
            ignored.add(type);
        }

        // call createFromBlock() to create nodes, but taking in account
        // that for each catcher, all of the nodes that throw into it must be
        // created before!
        TreeSet<Integer> checked = new TreeSet<Integer>(); // nodes that have
                                                           // been created
        TreeSet<Integer> candidates = new TreeSet<Integer>(); // nodes that will
                                                              // be checked this
                                                              // round
        TreeSet<Integer> newlyChecked = new TreeSet<Integer>(); // checked nodes
                                                                // that will be
                                                                // created

        for (IExplodedBasicBlock block : ecfg) {
            candidates.add(block.getNumber());
        }

        // exit has no exceptional predecessors, even if nodes have it as
        // successor!
        createFromBlock(ecfg.exit(), ecfg);
        newlyChecked.add(ecfg.exit().getNumber());
        candidates.remove(ecfg.exit().getNumber());

        while (!candidates.isEmpty()) {
            for (Integer candidate : candidates) {
                boolean parentsChecked = true;
                IExplodedBasicBlock block = ecfg.getNode(candidate);
                for (IExplodedBasicBlock parent : ecfg
                        .getExceptionalPredecessors(block)) {
                    if (!checked.contains(parent.getNumber())) {
                        parentsChecked = false;
                        break;
                    }
                }
                if (!parentsChecked)
                    continue;
                createFromBlock(block, ecfg);
                newlyChecked.add(block.getNumber());
            }
            checked.addAll(newlyChecked);
            candidates.removeAll(checked);
            newlyChecked.clear();
        }

        // now remove the exceptions we want to be ignored
        for (Node node : nodes.values()) {
            HashSet<TypeReference> exceptions = new HashSet<TypeReference>(node
                    .getThrownExceptions());
            exceptions.retainAll(ignored);
            for (TypeReference exception : exceptions) {
                FlowAnalisys.removeException(node, exception);
            }
        }

        // remove exceptions from monitor exits (since the catcher for their NPE
        // is somehow inside the monitor block)
        for (Node node : nodes.values()) {
            SSAInstruction istr = node.getBlock().getInstruction();
            if (istr instanceof SSAMonitorInstruction
                    && !((SSAMonitorInstruction) istr).isMonitorEnter()) {
                for (TypeReference exception : node.getThrown().keySet()) {
                    FlowAnalisys.removeException(node, exception);
                }
            }
        }

        entry = getNode(ecfg.entry().getNumber());
        exit = getNode(ecfg.exit().getNumber());
        checkConsistency();
    }

    private Class<?> getClassFromReference(TypeReference ref) {
        // Set<IClass> impl = cgNode.getClassHierarchy().getImplementors(ref);
        //
        // for (IClass cls : impl) {
        // if (cgNode.getClassHierarchy().isSubclassOf(.., ..))
        // }

        // TODO! Implement this
        String name = ref.getName().toString();
        name = name.replace('/', '.');
        name = name.substring(1, name.length());
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private Node createFromBlock(IExplodedBasicBlock block,
            ExplodedControlFlowGraph ecfg) {
        Node node = getNode(block.getNumber());

        // init intern stuff
        node.initNode(block);

        // get normal and exceptional predecessors
        // for some silly reason, predecessors are not set in EXIT
        if (block != ecfg.exit()) {
            for (IExplodedBasicBlock parent : ecfg.getNormalPredecessors(block)) {
                node.getParents().add(getNode(parent.getNumber()));
            }
            for (IExplodedBasicBlock parent : ecfg
                    .getExceptionalPredecessors(block)) {
                node.getCaught().add(getNode(parent.getNumber()));
            }
        }

        // get normal successors
        for (IExplodedBasicBlock child : ecfg.getNormalSuccessors(block)) {
            node.getChildren().add(getNode(child.getNumber()));
        }

        // does it have an instruction? if not, not much else to do.
        SSAInstruction istr;
        if (block.isCatchBlock()) {
            istr = block.getCatchInstruction();
        } else {
            istr = block.getInstruction();
            if (istr == null) {
                return node; // nothing else to do
            }
        }
        // is it a conditional? set yes and no successors
        if (istr instanceof SSAConditionalBranchInstruction) {
            Iterator<IExplodedBasicBlock> it = ecfg.getNormalSuccessors(block)
                    .iterator();
            Node yesNode = getNode(it.next().getNumber());
            Node noNode = getNode(it.next().getNumber());
            node.createCondClause(yesNode, noNode);
        }
        // does it throw exceptions?
        if (istr.isPEI()) {
            // add them to list
            HashSet<TypeReference> exceptions = new HashSet<TypeReference>();
            exceptions.addAll(istr.getExceptionTypes());
            for (TypeReference exception : exceptions) {
                node.getThrown().put(exception,
                        getNode(ecfg.exit().getNumber()));
            }
            // exception types gave back an empty set, but exceptional
            // successors exist!
            if (exceptions.size() == 0
                    && ecfg.getExceptionalSuccessors(block).size() != 0) {
                System.out
                        .println("Warning: Node "
                                + node
                                + " has no exceptions, but has exceptional successors!");
            }
        }
        if (block.isCatchBlock()) { // do we catch exceptions?
            // System.out.println(node + " is a catch block!");
            HashSet<TypeReference> caughtRefs = new HashSet<TypeReference>();
            Iterator<TypeReference> it = block.getCaughtExceptionTypes();
            while (it.hasNext())
                caughtRefs.add(it.next());

            for (IExplodedBasicBlock pred : ecfg
                    .getExceptionalPredecessors(block)) {
                Node parent = getNode(pred.getNumber());

                // now let's get those entries from parent which haven't been
                // solved
                HashSet<TypeReference> unhandled = new HashSet<TypeReference>();
                for (Entry<TypeReference, Node> entry : parent.getThrown()
                        .entrySet()) {
                    if (entry.getValue().equals(
                            getNode(ecfg.exit().getNumber()))) {
                        unhandled.add(entry.getKey());
                    }
                }

                if (unhandled.size() == 0) {
                    // node says that it catches an exception, but the thrower
                    // doesn't!
                    System.out.println("Warning: " + node.getIndex()
                            + " says that it catches an exception " + "from "
                            + parent.getIndex() + ", but it disagrees!");
                    parent.getThrown().put(caughtRefs.iterator().next(), node);
                    System.out.println("Adding link from " + parent + " to "
                            + node);
                } else if (unhandled.size() == 1) {
                    // if only one kind of exception remains, we can tell for
                    // sure that it is caught here
                    TypeReference ref = unhandled.iterator().next();
                    parent.getThrown().put(ref, node);
                } else {
                    // no direct matches? let's try to guess which ones get
                    // caught here.

                    // find all typerefs that are a direct match to the ones we
                    // catch. we know that we are responsible for those.
                    HashSet<TypeReference> common = new HashSet<TypeReference>();
                    common.addAll(caughtRefs);
                    common.retainAll(unhandled);
                    for (TypeReference ref : common) {
                        parent.getThrown().put(ref, node);
                    }

                    // from those left, either we catch them, or we catch a
                    // superclass of those.
                    // unfortunately, it seems that the library can't tell us
                    // which class is a superclass of another (actually it does,
                    // TODO!)
                    HashSet<TypeReference> remaining = new HashSet<TypeReference>();
                    remaining.addAll(caughtRefs);
                    remaining.removeAll(common);

                    // now we check each node's remaining exceptions with each
                    // of the parent's unhandled exceptions for polymorphism.
                    for (TypeReference rem : remaining) {
                        Class<?> remClass = getClassFromReference(rem);
                        if (remClass == null)
                            continue;
                        for (TypeReference unh : unhandled) {
                            Class<?> unhClass = getClassFromReference(unh);
                            if (unhClass == null)
                                continue;
                            if (remClass.isAssignableFrom(unhClass)) {
                                parent.getThrown().put(unh, node);
                            }
                        }

                    }
                }
            }
        }
        return node;
    }

    /** Remove nodes with a null instruction. */
    void simplify() {
        Iterator<Integer> iterator = nodes.keySet().iterator();
        while (iterator.hasNext()) {
            int key = iterator.next();
            Node node = nodes.get(key);

            if (node == entry || node == exit) {
                continue;
            }
            // do we need to get rid of this node?
            if (node.getBlock().getInstruction() == null
                    && !node.getBlock().isCatchBlock()) {
                // blocks with a null instruction only have one normal child;
                // get it and remove this node from the parent list.
                iterator.remove();
                Node child = node.getChildren().first();
                child.getParents().remove(node);

                // now check the parents of node
                for (Node parent : node.getParents()) {
                    // remove this node from the list of their children
                    parent.getChildren().remove(node);
                    // tell the node's parent about the node's child
                    parent.getChildren().add(child);
                    // and viceversa
                    child.getParents().add(parent);

                    // parent check with conditional clauses
                    if (parent.hasCondClause()) {
                        if (parent.getCondClause().yesNode.equals(node)) {
                            parent.getCondClause().yesNode = child;
                        }
                        if (parent.getCondClause().noNode.equals(node)) {
                            parent.getCondClause().noNode = child;
                        }
                    }
                }
                // now check the exceptional parents, tell them about the new
                // catcher.
                for (Node parent : node.getCaught()) {
                    for (Entry<TypeReference, Node> entry : parent.getThrown()
                            .entrySet()) {
                        if (entry.getValue().equals(node)) {
                            entry.setValue(child);
                        }
                        child.getCaught().add(parent);
                    }
                }
            }
        }
        checkConsistency();
    }

    /**
     * Creates a list of subgraphs from top level loops, then flattens the
     * nested loops.
     */
    void detectLoops() {
        TreeSet<Integer> nodeList = new TreeSet<Integer>(nodes.keySet());
        loops = new LoopList(this, nodeList, entry.getIndex(), exit.getIndex());
        loopBlockSubgraphs = new Vector<FlowGraph>();

        new LoopDetector().detectLoops(loops);

        for (LoopList loop : loops.subLoops) {
            FlowGraph graph = this.copy();
            graph.loopBlockSubgraphs = new Vector<FlowGraph>();
            graph.loops = loop;
            graph.nodes.keySet().retainAll(loop.getNodeIndices());
            graph.entry = graph.getNode(loop.getEntry());
            graph.exit = graph.getNode(loop.getExit());
            graph.destroyLoops();
            loopBlockSubgraphs.add(graph);
            // DotWriter.write(graph, "out/cycle-" + graph.hashCode() + ".dot");
        }

    }

    /** Creates a FlowAnalysis and purges unnecessary exceptions. */
    FlowAnalisys purge(DefUse defUses, SymbolTable symbols,
            ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg,
            ExplodedControlFlowGraph ecfg) {
        FlowAnalisys flow = new FlowAnalisys();
        flow.start(this, defUses, symbols, cfg, ecfg);
        return flow;
    }

    /**
     * Creates a node with index i. The whole purpose of this function (instead
     * of using 'nodes.get(i)' directly) is to make sure that the node with
     * index i exists before it is used.
     */
    Node getNode(int i) {
        if (!nodes.containsKey(i)) {
            Node n = new Node(i);
            nodes.put(i, n);
        }
        return nodes.get(i);
    }

    /** find a loop which has node as its main entry */
    FlowGraph findLoopByHead(Node node) {
        if (loopBlockSubgraphs != null) {
            for (FlowGraph loopGraph : loopBlockSubgraphs) {
                if (loopGraph.entry.equals(node)) {
                    return loopGraph;
                }
            }
        }
        return null;
    }

    /** Flattens all nested loops inside this graph. */
    private void destroyLoops() {
        destroyLoops(loops);
    }

    /** Flattens all nested loops inside graph */
    private void destroyLoops(LoopList graph) {
        if (getNode(graph.getEntry()).getParents().contains(
                getNode(graph.getExit()))) {
            // System.out.println("Removing link from " + graph.exit + " to " +
            // graph.entry);
            getNode(graph.getExit()).getChildren().remove(
                    getNode(graph.getEntry()));
            getNode(graph.getEntry()).getParents().remove(
                    getNode(graph.getExit()));
        }
        for (LoopList sub : graph.subLoops) {
            destroyLoops(sub);
        }
    }

    private void checkConsistency() {
        for (Entry<Integer, Node> entry : nodes.entrySet()) {
            Node node = getNode(entry.getKey());
            if (node.getBlock() == null)
                throw new IllegalArgumentException(node.toString());
            if (node.getChildren() == null)
                throw new IllegalArgumentException(node.toString());
            if (node.getParents() == null)
                throw new IllegalArgumentException(node.toString());
            if (node.getThrown() == null)
                throw new IllegalArgumentException(node.toString());
            if (node.getCaught() == null)
                throw new IllegalArgumentException(node.toString());

            for (Node child : entry.getValue().getChildren()) {
                if (child == null)
                    throw new IllegalArgumentException(node.toString());
            }
            for (Node parent : entry.getValue().getParents()) {
                if (parent == null)
                    throw new IllegalArgumentException(node.toString());
            }
            for (Node parent : entry.getValue().getCaught()) {
                if (parent == null)
                    throw new IllegalArgumentException(node.toString());
            }
            for (Entry<TypeReference, Node> handler : entry.getValue()
                    .getThrown().entrySet()) {
                if (handler.getValue() == null)
                    throw new IllegalArgumentException(node.toString());
            }
            if (entry.getValue().hasCondClause()) {
                if (node.getCondClause().yesNode == null)
                    throw new IllegalArgumentException(node.toString());
                if (node.getCondClause().noNode == null)
                    throw new IllegalArgumentException(node.toString());
            }
        }
    }

    int countExceptions() {
        int counter = 0;

        for (Node node : nodes.values()) {
            counter += node.getThrown().size();
        }

        return counter;
    }

    boolean hasEdge(int src, int dst) {
        Node srcNode = nodes.get(src);
        Node dstNode = nodes.get(dst);
        if (srcNode == null || dstNode == null)
            return false;
        return srcNode.getChildren().contains(dstNode)
                || srcNode.getCatchers().contains(dstNode);
    }

    boolean hasNormalEdge(int src, int dst) {
        Node srcNode = nodes.get(src);
        Node dstNode = nodes.get(dst);
        if (srcNode == null || dstNode == null)
            return false;
        return srcNode.getChildren().contains(dstNode);
    }

    boolean hasExceptionalEdge(int src, int dst) {
        Node srcNode = nodes.get(src);
        Node dstNode = nodes.get(dst);
        if (srcNode == null || dstNode == null)
            return false;
        return srcNode.getCatchers().contains(dstNode);
    }

}
