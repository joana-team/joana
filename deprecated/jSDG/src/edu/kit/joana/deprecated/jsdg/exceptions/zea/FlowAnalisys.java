/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.exceptions.zea;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction.Operator;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.Value;
import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.TypeReference;

public class FlowAnalisys {
    private TreeMap<Node, Context> contexts;

    private FlowGraph graph;

    private DefUse defUses;

    private SymbolTable symbols;

    private ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg;

    private ExplodedControlFlowGraph ecfg;

    private TreeMap<Node, Vector<Integer>> phiDefinitions;

    private TreeMap<Node, Vector<Integer>> piDefinitions;

    void start(FlowGraph graph, DefUse defUses, SymbolTable symbols,
            ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg,
            ExplodedControlFlowGraph ecfg) {
        this.graph = graph;
        this.defUses = defUses;
        this.symbols = symbols;
        this.ecfg = ecfg;
        this.cfg = cfg;

        contexts = new TreeMap<Node, Context>();
        startGraph(graph, new TreeSet<Node>());
    }

    TreeMap<Node, Vector<Integer>> getPhiDefinitions() {
        return phiDefinitions;
    }

    TreeMap<Node, Vector<Integer>> getPiDefinitions() {
        return piDefinitions;
    }

    DefUse getDefUses() {
        return defUses;
    }

    /**
     * does a flow analysis of the subgraph, and then again and again until the
     * graph doesn't change anymore (fixed point calculations)
     */
    private void checkCycleGraph(FlowGraph cycleGraph,
            TreeSet<Node> alreadyChecked) {
        boolean innerChanged = true;

        while (innerChanged) {
            innerChanged = startGraph(cycleGraph, alreadyChecked);
            if (innerChanged) {
                // graph and cycleGraph are independent graphs.
                // but if an exception was removed in cycleGraph,
                // we need to synchronize the changes back to graph.
                for (int i : cycleGraph.getNodeIndices()) {
                    Node orig = graph.getNode(i);
                    Node changed = cycleGraph.getNode(i);
                    orig.getThrownExceptions().retainAll(
                            changed.getThrown().keySet());
                }
                // System.out.println("Context changed! Repeating.");
            }
            Context startContext = contexts.get(cycleGraph.getEntry());
            Context endContext = contexts.get(cycleGraph.getExit());
            if (endContext == null) {
                System.out.println("ROFL end context from "
                        + cycleGraph.getExit() + " is null!");
            }
            // fuse the context of the first node with the context of the exit
            Context newContext = startContext.intersect(endContext);
            contexts.put(cycleGraph.getEntry(), newContext);
        }
    }

    /** flow analysis stuff! */
    private boolean startGraph(FlowGraph graph, TreeSet<Node> alreadyChecked) {
        boolean graphHasChanged = false; // has the graph changed? used by fixed
        // point calcs
        TreeSet<Node> checked = new TreeSet<Node>(alreadyChecked);
        TreeSet<Node> candidates = new TreeSet<Node>();
        Vector<Node> newlyChecked = new Vector<Node>();

        // 1. add entry as first candidate.
        // 2. add its children to the candidate list.
        // 3. for each candidate, check if each of the parents are in the
        // checked list.
        // if yes, go on. if not, continue to the next candidate.
        // 4. check node for exceptions, remove unnecessary exceptions.
        // 5. remove node from candidate list, add it to checked list, add
        // children to candidates.
        // 6. see if removing exceptions created an orphan catcher.
        // if yes, add catcher to candidate list.
        // 7. check if candidate list is empty
        // if yes, finished. if not, go to 3.

        candidates.add(graph.getEntry());
        candidates.add(graph.getExit()); // sometimes exit is unconnected to the
        // rest of the graph

        findPhiDefinitions();

        // System.out.println("------ Starting graph " + graph.nodes.keySet());

        while (!candidates.isEmpty()) {
            for (Node candidate : candidates) {
                boolean allParentsChecked = true;

                // System.out.println("Analysing " + candidate);

                FlowGraph loopSubgraph = graph.findLoopByHead(candidate);

                TreeSet<Node> parents = new TreeSet<Node>();
                // add normal predecessors
                parents.addAll(candidate.getParents());
                // add exceptional predecessors
                parents.addAll(candidate.getCaught());

                for (Node parent : parents) {
                    // has the parent been checked, or is the parent part of a
                    // loop?
                    if (checked.contains(parent)
                            || (loopSubgraph != null && loopSubgraph
                                    .containsNode(parent.getIndex()))) {
                        // check ok! parent is either checked or is inside loop
                        // and can be ignored
                    } else {
                        // parent check failed
                        allParentsChecked = false;
                        break;
                    }
                    // System.out.println("allParentsChecked " +
                    // allParentsChecked);
                }
                if (!allParentsChecked) {
                    // System.out.println("Not all parents checked, continue!");
                    continue;
                }

                if (loopSubgraph != null) {
                    // is the candidate the entry of a loop? do flow
                    // analysis of subgraph first.
                    checkCycleGraph(loopSubgraph, checked);
                    // add all nodes in loop as checked.
                    for (int i : loopSubgraph.getNodeIndices()) {
                        newlyChecked.add(graph.getNode(i));
                    }
                    break;
                }

                // create a context for this node as a fusion of the parents'
                // contexts
                Context context = null;
                if (candidate.equals(graph.getEntry())
                        && contexts.containsKey(graph.getEntry())) {
                    // do not touch the context if the node is the entry!
                    // if it's the head of a cycle, it was set to the sum of the
                    // cycle contexts
                    context = contexts.get(graph.getEntry());
                    if (context == null) {
                        System.out.println("Context is null for entry "
                                + candidate);
                    }
                } else {
                    if (candidate.getParents().isEmpty()
                            && candidate.getCaught().isEmpty()) {
                        // is this an orphan node?
                        if (candidate.getIndex() != ecfg.entry().getNumber()
                                && candidate.getIndex() != ecfg.exit()
                                        .getNumber()) {
                            System.out
                                    .println("Warning! Candidate "
                                            + candidate
                                            + " has no parents, make sure this makes sense.");
                        }
                        context = createBasicContext();
                    } else {
                        // fuse the contexts of the parents
                        for (Node parent : parents) {
                            if (parent == null) {
                                System.out
                                        .println("Context is null for parent "
                                                + parent);
                            }
                            Context transitionalContext = getContext(parent,
                                    candidate);
                            if (context == null) {
                                context = transitionalContext;
                            } else if (transitionalContext != null) {
                                context = context.fuse(transitionalContext);
                            }
                        }
                    }
                }
                if (context == null) {
                    // the fusion of the parents' contexts is null? should only
                    // happen to the entry!
                    if (candidate.getIndex() != ecfg.entry().getNumber())
                        throw new IllegalArgumentException();
                }

                if (candidate.hasInstruction()) {
                    // do only checks if the candidate has an instruction
                    checkNewDefinitions(candidate, context);
                    checkPhiDefinitionsForNode(candidate, context);
                    graphHasChanged |= checkExceptions(candidate, context);
                }
                contexts.put(candidate, context);
                newlyChecked.add(candidate);
            }
            for (Node checkedNode : newlyChecked) {
                // move the newly checked nodes from the candidate to the
                // checked list
                candidates.remove(checkedNode);
                checked.add(checkedNode);

                TreeSet<Node> children = new TreeSet<Node>();
                children.addAll(checkedNode.getChildren());
                children.addAll(checkedNode.getCatchers());
                for (Node child : children) {
                    // get children that are inside graph, haven't been checked
                    // already, and aren't the primary exit (which sometimes
                    // acts weird, like having no parents)
                    if (graph.containsNode(child.getIndex())
                            && !checked.contains(child)
                            && child.getIndex() != ecfg.exit().getNumber()) {
                        candidates.add(child);
                    }
                }
            }

            // add new orphan nodes that haven't been checked
            for (Node node : graph.getNodes()) {
                if (node.getParents().size() + node.getCaught().size() == 0
                        && !checked.contains(node)) {
                    System.out.println("Node " + node
                            + " has become an orphan. Adding to candidates.");
                    candidates.add(node);
                }
            }

            if (newlyChecked.isEmpty()) {
                // list did not change at all, we don't know what to do with the
                // candidates! one of the dependencies could not be filled.
                // (this should not happen anymore!)
                System.out.println("Error: Failed to clear candidate list.");
                System.err.println("Error: Failed to clear candidate list.");
                // throw new RuntimeException("Fix this already!");
            }
            newlyChecked.clear();
        }
        return graphHasChanged;
    }

    /**
     * create a basic context, setting all local variables to UNSEEN, null
     * constants to NULL, and known non-null references ('this', string
     * constants) to NOT_NULL
     */
    private Context createBasicContext() {
        Context context = new Context();
        context.addNotNull(1, graph.getEntry()); // "this" object
        for (int i = 2; i <= symbols.getMaxValueNumber(); i++) {
            Value value = symbols.getValue(i);
            if (value == null) {
                context.addUnseen(i);
            } else if (value.isNullConstant()) {
                context.addNull(i, graph.getEntry());
            } else if (value.isStringConstant()) {
                context.addNotNull(i, graph.getEntry());
            } else {
                context.addUnseen(i);
            }
        }
        return context;
    }

    private void findPhiDefinitions() {
        phiDefinitions = new TreeMap<Node, Vector<Integer>>();
        piDefinitions = new TreeMap<Node, Vector<Integer>>();
        // definitions = new TreeMap<Node, Vector<Integer>>();

        // this is so messy. phi definitions are not in the symbol table,
        // but in the original control flow graph.
        for (ISSABasicBlock block : cfg) {
            Iterator<SSAPhiInstruction> phis = block.iteratePhis();
            if (!phis.hasNext()) {
                continue;
            }
            // however, the blocks in the cfg are not the same as in the ecfg,
            // so we ask the ecfg for the corresponding block
            IExplodedBasicBlock iblock = ecfg.getBlockForInstruction(block
                    .getFirstInstructionIndex());
            // also, since we deleted nodes with NOP, we need to find the next
            // one with a valid instruction by following the children
            while (iblock.getInstruction() == null) {
                // luckily, NOP blocks only have one child to follow
                iblock = ecfg.getNormalSuccessors(iblock).iterator().next();
            }
            // now we have the node where the definition happens, fill list
            Node node = graph.getNode(iblock.getNumber());
            Vector<Integer> phiList = new Vector<Integer>();
            while (phis.hasNext()) {
                int value = phis.next().getDef();
                phiList.add(value);
                // System.out.println("PHI " + node + " : " + value);
            }
            if (node == null) {
                System.out.println("Warning: phi node with block number "
                        + iblock.getNumber() + " is null!");
            }
            phiDefinitions.put(node, phiList);
        }

        // same as above, but for pis
        for (ISSABasicBlock block : cfg) {
            Iterator<SSAPiInstruction> pis = block.iteratePis();
            if (!pis.hasNext()) {
                continue;
            }
            // fortunately, we now use the last instruction of the cfg block,
            // which always corresponds directly to an ecfg block.
            IExplodedBasicBlock iblock = ecfg.getBlockForInstruction(block
                    .getLastInstructionIndex());
            // fill list as usual
            Node node = graph.getNode(iblock.getNumber());
            Vector<Integer> piList = new Vector<Integer>();
            while (pis.hasNext()) {
                int value = pis.next().getDef();
                piList.add(value);
                // System.out.println("PI " + node + " : " + value);
            }
            piDefinitions.put(node, piList);
        }
    }

    /**
     * does this node have a phi definition? then fuse all their values
     * together.
     */
    private void checkPhiDefinitionsForNode(Node node, Context context) {
        if (phiDefinitions.containsKey(node)) {
            Vector<Integer> phiList = phiDefinitions.get(node);
            for (int value : phiList) {
                SSAPhiInstruction phi = (SSAPhiInstruction) defUses
                        .getDef(value);
                // ((PhiValue)symbols.getValue(value)).getPhiInstruction();
                boolean isNotNull = true;
                // System.out.println(node + ": phi $" + value + " = "
                // + DotWriter.wrap(phi.getUse(0)) + ", " +
                // DotWriter.wrap(phi.getUse(1)));
                for (int i = 0; i < phi.getNumberOfUses(); i++) {
                    if (!context.isNotNull(phi.getUse(i))
                            && !context.isUnseen(phi.getUse(i))) {
                        isNotNull = false;
                        break;
                    }
                }
                if (isNotNull) {
                    context.addNotNull(value, node);
                } else {
                    context.addUnknown(value);
                }
            }
        }
    }

    private boolean checkNullExceptions(Node node, Context context) {
        SSAInstruction istr = node.getBlock().getInstruction();

        int valueNumber = 0;
        if (istr instanceof SSANewInstruction) {
            // 'new' instructions are implied to produce a non-null reference
            // (if they do, and it's not an exception, then it's usually a
            // non-recoverable error)
            valueNumber = ((SSANewInstruction) istr).getDef();
        } else if (istr.getNumberOfUses() > 0
                && (istr instanceof SSAFieldAccessInstruction
                        || istr instanceof SSAInvokeInstruction
                        || istr instanceof SSAArrayLengthInstruction
                        || istr instanceof SSAMonitorInstruction || istr instanceof SSAArrayReferenceInstruction)) {
            // if an instruction reads from a variable, it's thought to be
            // non-null from this point forward
            valueNumber = istr.getUse(0);
        }
        if (valueNumber > 0) {
            if (context.isNotNull(valueNumber)) {
                // if we know it to be non-null, remove NPE
                boolean ret = removeException(node,
                        TypeReference.JavaLangNullPointerException);
                return ret;
            } else {
                // else add it as non-null for the rest of the program
                context.addNotNull(valueNumber, node);
            }
        }
        return false;
    }

    /**
     * Removes the given exception from the node. This is a central function to
     * keep track of when and where an exception is removed.
     */
    static boolean removeException(Node node, TypeReference exception) {
        Node catcher = node.getThrown().get(exception);
        // in cyclic fixed-point calcs the same exception might be found
        // many times, check if it was removed already
        if (catcher != null) {
            catcher.getCaught().remove(node);
        }
        return node.getThrown().remove(exception) != null;
    }

    private void checkForNullComparison(SSAConditionalBranchInstruction branch,
            Node parent, Node child, Context context) {
        // is the parent a conditional with a comparison (equals / not equals)
        // against a null pointer? and is 'child' the branch where the variable
        // doesn't equal null?
        if (context.isNull(branch.getUse(1))) {
            if ((branch.getOperator().equals(Operator.EQ) && child
                    .equals(parent.getCondClause().noNode))
                    || (branch.getOperator().equals(Operator.NE) && child
                            .equals(parent.getCondClause().yesNode))) {
                // then we know that variable is not null in this branch!
                context.addNotNull(branch.getUse(0), parent);
            }
        }
    }

    private void checkPiDefinitionsForNode(Node parent, Node child,
            Context context) {
        if (piDefinitions.containsKey(parent)) {
            // conditionals usually have a pi definition which
            // changes between the yes / no branches.
            Vector<Integer> piList = piDefinitions.get(parent);
            SSAPiInstruction pi = (SSAPiInstruction) defUses.getDef(piList
                    .get(0));

            assert (pi.getSuccessor() <= pi.getPiBlock());
            assert (piList.size() == 2);

            int sourceValue = pi.getUse(0);
            int destValue;

            if (child.equals(parent.getCondClause().yesNode)) {
                destValue = piList.get(1);
            } else {
                destValue = piList.get(0);
            }
            context.piValue(sourceValue, destValue, parent);
        }
    }

    /**
     * check if a new variable has been defined, and change it from an UNSEEN to
     * an UNKNOWN.
     */
    private void checkNewDefinitions(Node candidate, Context context) {
        SSAInstruction istr = candidate.getBlock().getInstruction();
        for (int i = 0; i < istr.getNumberOfDefs(); i++) {
            int value = istr.getDef(i);
            context.addUnknown(value);
        }
    }

    /**
     * get the context of the parent, and see if it changes in the transition
     * between parent and child (pi defs and branches)
     */
    private Context getContext(Node parent, Node child) {
        Context context = contexts.get(parent);
        if (context == null) {
            System.out.println("Context for parent " + parent + " is null!");
        }
        if (parent.hasCondClause()) {
            SSAConditionalBranchInstruction branch = (SSAConditionalBranchInstruction) parent
                    .getBlock().getInstruction();
            Context result = context.copy();
            checkForNullComparison(branch, parent, child, result);
            checkPiDefinitionsForNode(parent, child, result);
            return result;
        } else {
            return context;
        }
    }

    private boolean checkExceptions(Node node, Context context) {
        return checkNullExceptions(node, context);
    }

}
