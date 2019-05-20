/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu.interproc;

import static edu.kit.joana.ifc.sdg.qifc.nildumu.BasicLogger.log;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.Context.INFTY;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.ds;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.kit.joana.ifc.sdg.qifc.nildumu.Context;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Dominators;
import edu.kit.joana.ifc.sdg.qifc.nildumu.DotRegistry;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Method;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Program;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Dominators.Node;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.Bit;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.Value;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.DefaultMap;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.Util.Box;

/**
 * A summary-edge based handler. It creates for each function beforehand summary edges:
 * these edges connect the parameter bits and the return bits. The analysis assumes that all
 * parameter bits might have a statically unknown value. The summary-edge analysis builds the
 * summary edges using a fix point iteration over the call graph. Each analysis of a method
 * runs the normal analysis of the method body and uses the prior summary edges if a method is
 * called in the body. The resulting bit graph is then reduced.
 * <p/>
 * It supports induction.
 * <p/>
 * Induction starts with no edges between parameter bits and return bits and iterates till no
 * new connection between a return bit and a parameter bit is added. It only works for programs
 * without recursion.
 * <p/>
 * <p/>
 * The default reduction policy is to connect all return bits with all parameter bits that they
 * depend upon.
 * And improved version (mincut reduction) includes the minimal cut bits of the bit graph from
 * the return to the parameter bits, assuming that the return bits have infinite weights.
 */
public class SummaryHandler extends MethodInvocationHandler {

    public static enum Reduction {
        BASIC,
        MINCUT;
    }

    final Path dotFolder;

    final SummaryHandler.Reduction reductionMode;

    final int callStringMaxRec;

    Map<Method, BitGraph> methodGraphs;

    Dominators<Method> callGraph;
    
    Map<Method, CallSite> callSites;

    public SummaryHandler(Path dotFolder, SummaryHandler.Reduction reductionMode, int callStringMaxRec) {
        this.reductionMode = reductionMode;
        this.callStringMaxRec = callStringMaxRec;
        this.dotFolder = dotFolder;
    }

    @Override
    public void setup(Program program) {
        callGraph = program.getMethodDominators();
        callSites = new DefaultMap<Method, CallSite>((map, m) -> {
        	return new CallSite(m);
        });
        DotRegistry.get().storeFiles();
        Context c = program.context;
        Map<Node<Method>, BitGraph> state = new HashMap<>();
        MethodInvocationHandler handler = createHandler(m -> state.get(callGraph.getNodeForElement(m)));
        Box<Integer> iteration = new Box<>(0);
        callGraph.<BitGraph>worklist((node, s) -> {
            if (node.isEntryNode()){
                return s.get(node);
            }
            iteration.val += 1;
            BitGraph graph = methodIteration(program.context, node.getElem(), handler, s.get(node).parameters);
            String name = String.format("%3d %s", iteration.val, node.getElem().toBCString());
            if (dotFolder != null){
                graph.writeDotGraph(dotFolder, name, true);
            }
            DotRegistry.get().store("summary", name,
                    () -> () -> graph.createDotGraph("", true));
            BitGraph reducedGraph = reduce(c, graph);
            if (dotFolder != null){
                graph.writeDotGraph(dotFolder, name + " [reduced]", false);
            }
            DotRegistry.get().store("summary",  name + " [reduced]",
                    () -> () -> reducedGraph.createDotGraph("", false));
            return reducedGraph;
        }, node ->  {
            BitGraph graph = bot(program, node.getElem());
            String name = String.format("%3d %s", iteration.val, node.getElem().toBCString());
            if (dotFolder != null){
                graph.writeDotGraph(dotFolder, name, false);
            }
            DotRegistry.get().store("summary", name,
                    () -> () -> graph.createDotGraph("", false));
            return graph;
        }
        , node -> node.getIns().stream().filter(n -> !n.isEntryNode()).collect(Collectors.toSet()),
        state);
        methodGraphs = state.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getElem(), Map.Entry::getValue));
    }

    BitGraph bot(Program program, Method method){
        List<Value> parameters = generateParameters(program, method);
        return new BitGraph(program.context, parameters, program.createUnknownValue(method.method.getSignature().getReturnType()));
    }

    List<Value> generateParameters(Program program, Method method){
        //log(() -> method.toString());
    	return method.getParameters().stream().map(p ->
            program.createUnknownValue(p.getType())
        ).collect(Collectors.toList());
    }

    BitGraph methodIteration(Context c, Method method, MethodInvocationHandler handler, List<Value> parameters){
        c.pushNewMethodInvocationState(callSites.get(method), parameters.stream().flatMap(Value::stream).collect(Collectors.toSet()));
        for (int i = 0; i < parameters.size(); i++) {
            c.setParamValue(i + 1, parameters.get(i));
        }
        c.forceMethodInvocationHandler(handler);
        c.fixPointIteration(method.entry);
        Value ret = c.getReturnValue();
        c.popMethodInvocationState();
        c.forceMethodInvocationHandler(this);
        return new BitGraph(c, parameters, ret);
    }

    MethodInvocationHandler createHandler(Function<Method, BitGraph> curVersion){
    	MethodInvocationHandler handler = new MethodInvocationHandler() {
            @Override
            public Value analyze(Context c, CallSite callSite, List<Value> arguments) {
                return curVersion.apply(callSite.method).applyToArgs(c, arguments);
            }
            
            @Override
            public String getName() {
            	return "internal";
            }
        };
        if (callStringMaxRec > 0){
            return new InliningHandler(callStringMaxRec, handler);
        }
        return handler;
    }

    BitGraph reduce(Context context, BitGraph bitGraph){
        switch (reductionMode) {
            case BASIC:
                return basicReduce(context, bitGraph);
            case MINCUT:
                return minCutReduce(context, bitGraph);
        }
        return null;
    }

    /**
     * basic implementation, just connects a result bit with all reachable parameter bits
     */
    BitGraph basicReduce(Context context, BitGraph bitGraph){
        DefaultMap<Bit, Bit> newBits = new DefaultMap<Bit, Bit>((map, bit) -> {
            return BitGraph.cloneBit(context, bit, ds.create(bitGraph.calcReachableParamBits(bit)));
        });
        Value ret = bitGraph.returnValue.map(newBits::get);
        ret.node(bitGraph.returnValue.node());
        return new BitGraph(context, bitGraph.parameters, ret);
    }

    BitGraph minCutReduce(Context context, BitGraph bitGraph) {
        Set<Bit> anchorBits = new HashSet<>(bitGraph.parameterBits);
        Set<Bit> minCutBits = bitGraph.minCutBits(bitGraph.returnValue.bitSet(), bitGraph.parameterBits, INFTY);
        anchorBits.addAll(minCutBits);
        Map<Bit, Bit> newBits = new HashMap<>();
        // create the new bits
        Stream.concat(bitGraph.returnValue.stream(), minCutBits.stream()).forEach(b -> {
            Set<Bit> reachable = bitGraph.calcReachableBits(b, anchorBits);
            if (!b.deps().contains(b)){
                reachable.remove(b);
            }
            Bit newB = BitGraph.cloneBit(context, b, ds.create(reachable));
            newB.value(b.value());
            newBits.put(b, newB);
        });
        bitGraph.parameterBits.forEach(b -> newBits.put(b, b));
        // update the control dependencies
        newBits.forEach((o, b) -> {
            b.alterDependencies(newBits::get);
        });
        Value ret = bitGraph.returnValue.map(newBits::get);
        ret.node(bitGraph.returnValue.node());
        BitGraph newGraph = new BitGraph(context, bitGraph.parameters, ret);
        //assert !isReachable || newGraph.calcReachableBits(newGraph.returnValue.get(1), newGraph.parameters.get(0).bitSet()).size() > 0;
        return newGraph;
    }

    @Override
    public Value analyze(Context c, CallSite callSite, List<Value> arguments) { 
    	return methodGraphs.get(callSite.method).applyToArgs(c, arguments);
    }
    
    @Override
    public String getName() {
    	return "summary";
    }
}