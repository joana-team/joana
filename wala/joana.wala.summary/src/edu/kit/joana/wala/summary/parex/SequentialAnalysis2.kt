package edu.kit.joana.wala.summary.parex

import java.util.*
import kotlin.collections.HashSet

/**
 * A basic sequential analysis.
 *
 * The goal is to present clean and understandable code for a basic summary edge analysis
 */
class SequentialAnalysis2 : Analysis {

    /**
     * functions can register on other functions that they currently depend on
     */
    /*val waitingForFuncNode = HashMap<FuncNode, MutableSet<FuncNode>>()

    private fun wait(func: FuncNode, other: FuncNode) {
        waitingForFuncNode.getOrPut(func, { HashSet() }).add(other)
    }

    private fun get(func: FuncNode): Set<FuncNode> {
        return waitingForFuncNode[func] ?: emptySet()
    }

    private fun clear(func: FuncNode){
        waitingForFuncNode[func]?.clear()
    }*/

    class State(val formInsPerActIn: MutableMap<InNode, MutableSet<InNode>> = HashMap())

    val states = HashMap<FuncNode, State>()

    override fun process(g: Graph) {
        worklist(g.entry, initialEntries(g), this::process)
        copyFormalSummariesToActualSummaries(g)
    }

    open fun initialEntries(g: Graph): Collection<FuncNode> = g.callGraph.vertexSet()

    open fun worklist(start: FuncNode, initialElements: Collection<FuncNode>, process: (FuncNode) -> Iterable<FuncNode>){
        edu.kit.joana.wala.summary.parex.worklist(start, initialElements, process)
    }

    fun process(funcNode: FuncNode): Iterable<FuncNode> {
        val state = states.getOrPut(funcNode, { State() })
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
                     * We reached a call node through a normal (not an in) node
                     */
                    is CallNode -> {
                        addToQueue(cur.neighbors)
                    }
                    /**
                     * Only actual in nodes here
                     */
                    is ActualInNode -> {
                        state.formInsPerActIn.getOrPut(cur, { HashSet() }).add(fi)
                        cur.callNode?.let { call ->
                            call.targets.forEach { target ->
                                val formalIn = cur.formalIns[target]
                                if (formalIn?.summaryEdges != null) {
                                    addToQueue(formalIn.summaryEdges!!.map { call.actualOut(it as FormalOutNode) as Node })
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
            if (fi.summaryEdges == null){
                fi.summaryEdges = ArrayList()
            }
            if (fi.summaryEdges?.contains(fo) != true){
                (fo as FormalOutNode).actualOuts.forEach {(call, _) -> toReeval.add(call.owner)}
            }
            fi.summaryEdges!!.add(fo)
        }
        return toReeval
    }
}