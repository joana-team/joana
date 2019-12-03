package edu.kit.joana.wala.summary.parex

import java.util.*
import kotlin.collections.HashSet

/**
 * A basic sequential analysis.
 *
 * The goal is to present clean and understandable code for a basic summary edge analysis
 */
class SequentialAnalysis : Analysis {

    override fun process(g: Graph) {
        worklist(initialEntries(g), this::process)
        copyFormalSummariesToActualSummaries(g)
    }

    fun initialEntries(g: Graph): Collection<FuncNode> = g.callGraph.vertexSet()

    fun process(funcNode: FuncNode): Iterable<FuncNode> {
        val summaryEdges = ArrayList<Pair<InNode, OutNode>>()
        for (fi in funcNode.formalIns) {
            val alreadySeen = HashSet<Node>()
            val queue: Queue<Node> = ArrayDeque<Node>()
            val addToQueue = { xs: Collection<Node> ->
                xs.filter { it !in alreadySeen }.distinct().forEach { x ->
                    queue.add(x)
                    alreadySeen.add(x)
                }
            }
            addToQueue(fi.neighbors)
            while (queue.isNotEmpty()){
                when (val cur = queue.poll()) {
                    /**
                     * Only actual in nodes here
                     */
                    is ActualInNode -> {
                        cur.callNode?.let { call ->
                            call.targets.forEach { target ->
                                val formalIn = cur.formalIns[target]
                                if (formalIn?.summaryEdges != null) {
                                    addToQueue(formalIn.summaryEdges.map { call.actualOut(it as FormalOutNode) as Node })
                                }
                            }
                        }
                        addToQueue(cur.neighbors)
                    }
                    is FormalOutNode -> {
                        summaryEdges.add(Pair(fi, cur))
                    }
                    else -> {
                        addToQueue(cur.neighbors)
                    }
                }
            }
        }
        val toReeval = HashSet<FuncNode>()
        for ((fi, fo) in summaryEdges) {
            if (!fi.summaryEdges.contains(fo)){
                (fo as FormalOutNode).actualOuts.forEach {(call, _) -> toReeval.add(call.owner)}
            }
            fi.summaryEdges.add(fo)
        }
        return toReeval
    }
}