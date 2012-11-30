/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.exceptions.zea;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

public class MethodCFG {
    private final CGNode method;
    private ExplodedControlFlowGraph ecfg;
    private IR ir;
    private MethodReference mr;
    private DefUse defUses;
    private SymbolTable symbols;

    private ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg;

    private FlowGraph origGraph;
    private FlowGraph graph;

    private final TypeReference[] ignoreExceptions;

    MethodCFG(CGNode method, TypeReference[] ignoreExceptions) {
        this.method = method;
        this.ignoreExceptions = ignoreExceptions;
    }

    public FlowGraph getGraph() {
        return graph;
    }

    public FlowGraph getOriginalGraph() {
        return origGraph;
    }

    public ExplodedControlFlowGraph getECFG() {
        return ecfg;
    }

    public boolean init(AnalysisCache cache) throws UnsoundGraphException {
        ir = method.getIR();
        if (ir == null) {
            System.out.println("No IR for " + mr);
            return false;
        }

        defUses = cache.getSSACache().findOrCreateDU(ir, Everywhere.EVERYWHERE);
        symbols = ir.getSymbolTable();

        ecfg = ExplodedControlFlowGraph.make(ir);
        GraphIntegrity.check(ecfg);
        cfg = ir.getControlFlowGraph();
        GraphIntegrity.check(cfg);

        DotWriter.setSymbols(symbols);

        graph = new FlowGraph(method);
        graph.init(ecfg, ignoreExceptions);
        graph.simplify();
        graph.detectLoops();
        origGraph = graph.copy();
        origGraph.detectLoops();
        return true;
    }

    void purgeExceptions() {
        FlowAnalisys flow = graph.purge(defUses, symbols, cfg, ecfg);
        DotWriter.setFlowAnalizer(flow);
    }

    public void write(FlowGraph graph, String dotFile) {
        DotWriter.write(graph, dotFile);
    }
}
