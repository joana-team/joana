package edu.kit.joana.wala.summary.parex

import java.util.*

/**
 * A basic sequential analysis.
 *
 * The goal is to present clean and understandable code for a basic summary edge analysis
 */
class SequentialAnalysis : Analysis {

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
        worklist(g.entry, g.entry.reachableFuncNodes() + setOf(g.entry)) {
            val toWait = process(it)
            if (toWait.any()){
                return@worklist toWait.plusElement(it)
            }
            return@worklist toWait
        }
        copyFormalSummariesToActualSummaries(g)
    }

    fun process(funcNode: FuncNode): Iterable<FuncNode> {
        val state = states.getOrPut(funcNode, { State() })
        val toWait = HashSet<FuncNode>()
        val summaryEdges = ArrayList<Pair<InNode, OutNode>>()
        funcNode.formalIns.forEach { fi ->
            val alreadySeen = HashSet<Node>()
            val queue: Queue<Node> = ArrayDeque<Node>()
            queue.addAll(fi.neighbors)
            while (queue.isNotEmpty()){
                when (val cur = queue.poll()) {
                    is CallNode -> {
                        println("Error")
                    }
                    /**
                     * Only actual in nodes here
                     */
                    is ActualInNode -> {
                        state.formInsPerActIn.getOrPut(cur, { HashSet() }).add(fi)
                        (cur.neighbors.getOrNull(0) as CallNode?)?.let { call ->
                            call.targets.forEach { target ->
                                cur.formalIns[target]?.let { formalIn ->
                                    if (formalIn.summaryEdges != null) {
                                        queue.addAll(formalIn.summaryEdges!!.map { call.actualOut(it as FormalOutNode) })
                                    } else {
                                        /**
                                         * The formal in is not yet connected, we have to wait…
                                         */
                                        /**
                                         * The formal in is not yet connected, we have to wait…
                                         */
                                        toWait.add(target)
                                    }
                                }
                            }
                        }
                    }
                    is FormalOutNode -> {
                        summaryEdges.add(Pair(fi, cur))
                    }
                    else -> {
                        cur.neighbors.filter { it !in alreadySeen }.forEach { queue.add(it); alreadySeen.add(it) }
                    }
                }
            }
        }
        summaryEdges.forEach { (fi, fo) ->
            if (fi.summaryEdges == null){
                fi.summaryEdges = ArrayList()
            }
            fi.summaryEdges!!.add(fo)
        }
        return toWait
    }
}