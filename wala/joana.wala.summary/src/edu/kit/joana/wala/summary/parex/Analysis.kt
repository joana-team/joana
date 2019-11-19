package edu.kit.joana.wala.summary.parex

import com.ibm.wala.util.MonitorUtil
import edu.kit.joana.ifc.sdg.graph.SDG
import edu.kit.joana.ifc.sdg.graph.SDGEdge
import edu.kit.joana.wala.summary.ISummaryComputer
import edu.kit.joana.wala.summary.WorkPackage

interface Analysis : ISummaryComputer {
    fun process(g: Graph)

    fun process(pack: WorkPackage<SDG>,
                relevantEdges: Set<SDGEdge.Kind> = DEFAULT_RELEVANT_EDGES,
                ignoreSummaryEdges: Boolean = false): Graph {
        return SDGToGraph(relevantEdges, ignoreSummaryEdges).convert(pack).also(this::process)
    }

    fun compute(pack: WorkPackage<SDG>, relevantEdges: Set<SDGEdge.Kind>, summaryEdgeKind: SDGEdge.Kind = SDGEdge.Kind.SUMMARY): Int {
        process(pack, relevantEdges).insertSummaryEdgesIntoSDG(pack.graph, summaryEdgeKind)
        return 0
    }

    override fun compute(pack: WorkPackage<SDG>, parallel: Boolean, progress: MonitorUtil.IProgressMonitor?): Int {
        return compute(pack, DEFAULT_RELEVANT_EDGES)
    }

    override fun computeAdjustedAliasDep(pack: WorkPackage<SDG>, parallel: Boolean, progress: MonitorUtil.IProgressMonitor?): Int {
        val relevantEdges = setOf(SDGEdge.Kind.DATA_DEP, SDGEdge.Kind.DATA_HEAP,
                SDGEdge.Kind.DATA_ALIAS, SDGEdge.Kind.DATA_LOOP,
                SDGEdge.Kind.DATA_DEP_EXPR_VALUE, SDGEdge.Kind.DATA_DEP_EXPR_REFERENCE,
                SDGEdge.Kind.CONTROL_DEP_COND, SDGEdge.Kind.CONTROL_DEP_UNCOND,
                SDGEdge.Kind.CONTROL_DEP_EXPR, SDGEdge.Kind.CONTROL_DEP_CALL,
                SDGEdge.Kind.JUMP_DEP, SDGEdge.Kind.SUMMARY_NO_ALIAS,
                SDGEdge.Kind.SYNCHRONIZATION)
        return compute(pack, relevantEdges, SDGEdge.Kind.SUMMARY_DATA)
    }

    override fun computeFullAliasDataDep(pack: WorkPackage<SDG>, parallel: Boolean, progress: MonitorUtil.IProgressMonitor?): Int {
        return compute(pack, parallel, progress)
    }

    override fun computeHeapDataDep(pack: WorkPackage<SDG>, parallel: Boolean, progress: MonitorUtil.IProgressMonitor?): Int {
        return compute(pack, setOf(SDGEdge.Kind.DATA_DEP, SDGEdge.Kind.DATA_ALIAS, SDGEdge.Kind.SUMMARY_DATA), SDGEdge.Kind.SUMMARY_DATA)
    }

    override fun computeNoAliasDataDep(pack: WorkPackage<SDG>, parallel: Boolean, progress: MonitorUtil.IProgressMonitor?): Int {
        val relevantEdges = setOf(SDGEdge.Kind.DATA_DEP, SDGEdge.Kind.DATA_HEAP,
                SDGEdge.Kind.DATA_LOOP, SDGEdge.Kind.DATA_DEP_EXPR_VALUE,
                SDGEdge.Kind.DATA_DEP_EXPR_REFERENCE,
                SDGEdge.Kind.CONTROL_DEP_COND, SDGEdge.Kind.CONTROL_DEP_UNCOND,
                SDGEdge.Kind.CONTROL_DEP_EXPR, SDGEdge.Kind.CONTROL_DEP_CALL,
                SDGEdge.Kind.JUMP_DEP, SDGEdge.Kind.SUMMARY_NO_ALIAS,
                SDGEdge.Kind.SYNCHRONIZATION, SDGEdge.Kind.SUMMARY_DATA)
        return compute(pack, relevantEdges, SDGEdge.Kind.SUMMARY_NO_ALIAS)
    }

    override fun computePureDataDep(pack: WorkPackage<SDG>, parallel: Boolean, progress: MonitorUtil.IProgressMonitor?): Int {
        return compute(pack, setOf(SDGEdge.Kind.DATA_DEP, SDGEdge.Kind.SUMMARY_DATA))
    }

    fun copyFormalSummariesToActualSummaries(graph: Graph){
        /**
         * Initialize the summary edge lists
         */
        for (actualIn in graph.actualIns) {
            actualIn.summaryEdges = ArrayList(4)
        }
        /**
         * For all FormalIn nodes (they contain the summary edges)
         */
        for (formalIn in graph.formalIns) {
            val connectedFormalOuts = formalIn.summaryEdges
            if (connectedFormalOuts != null) {
                /* formalIn has summary edges, we visit every caller of the function that formalIn belongs to */
                for ((call, actIn) in formalIn.actualIns) {
                    val actualSummaryEdges = actIn.summaryEdges!!
                    for (formalOut in connectedFormalOuts) {
                        // println("${formalIn.id} → ${formalOut.id}")
                        /* The index of formalOut is the same as the index of the corresponding actualOut */
                        (formalOut as FormalOutNode).actualOuts[call]?.let(actualSummaryEdges::add)
                    }
                }
            }
        }
    }

    fun getName(): String = javaClass.simpleName
}