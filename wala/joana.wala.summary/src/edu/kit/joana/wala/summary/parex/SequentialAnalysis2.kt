package edu.kit.joana.wala.summary.parex

import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

/**
 * An improved sequential analysis.
 *
 * When-ever a new summary edge is found, the analysis starts from the actual out node on and not from the entry node.
 * This should be an improvement over {@link SequentialAnalysis}
 */
class SequentialAnalysis2 : Analysis {


    private data class FormalInState(val queue: Queue<Node>, val alreadySeen: MutableSet<Node>) {
        fun addToQueue(xs: Collection<Node>){
            xs.filter { it !in alreadySeen }.distinct().forEach { x ->
                queue.add(x)
                alreadySeen.add(x)
            }
        }
        fun addToQueue(node: Node){
            if (!alreadySeen.contains(node)){
                queue.add(node)
                alreadySeen.add(node)
            }
        }
        fun forceAddToQueue(node: Node){
            if (!queue.contains(node)){
                queue.add(node)
            }
        }
    }

    private data class State(val subStates: Map<FormalInNode, FormalInState>)

    private val FuncNode.state: State
        get() {
            if (data?.javaClass != State::class.java) {
                data = State(formalIns.map { it to FormalInState(ArrayDeque(it.neighbors), it.neighbors.toMutableSet()) }.toMap())
            }
            return data!! as State
        }

    private data class QueueItem(val funcNode: FuncNode, val actualInToOut: Map<ActualInNode, Collection<OutNode>>? = null)

    override fun process(g: Graph) {
        worklist(initialEntries(g), this::process)
    }

    private fun initialEntries(g: Graph): Collection<QueueItem> = g.callGraph.vertexSet().map { QueueItem(it) }

    private fun process(item: QueueItem): Iterable<QueueItem> {
        println(item)
        val (funcNode, inToOut) = item
        /**
         * Try to add summary edges and return directly if nothing changed
         */
        var changed = false
        if (inToOut != null){
            for ((ai, os) in inToOut){
                val sums = ai.summaryEdges ?: mutableListOf()
                for (o in os) {
                    if (!sums.contains(o)){
                        sums.add(o)
                        changed = true
                    }
                }
                ai.summaryEdges = sums
            }
        } else {
            /**
             * Entry case
             */
            changed = true
        }

        if (!changed){
            return emptyList()
        }

        /**
         * Process
         */
        val changes = process(funcNode)

        /**
         * Add to queue
         */
        val meta = mutableMapOf<FuncNode, MutableMap<ActualInNode, Collection<OutNode>>>()
        for ((fi, fos) in changes) {
            for ((callNode, ai) in fi.actualIns) {
                fos.mapNotNull { it.actualOuts[callNode] }.let {
                    if (it.isNotEmpty()){
                        meta.computeIfAbsent(callNode.owner, {HashMap()})[ai] = it
                    }
                }
            }
        }
        return meta.map { (f, m) -> QueueItem(f, m) }
    }

    /**
     * Starts where it stopped, returns a possibly empty list of found summary edges
     */
    private fun process(funcNode: FuncNode): Map<FormalInNode, Set<FormalOutNode>> {
        val formalInToOut = mutableMapOf<FormalInNode, MutableSet<FormalOutNode>>()
        funcNode.state.subStates.forEach outer@{(fi, state) ->
            val laterQueue = mutableListOf<Node>()
            val (queue, _) = state
            while (queue.isNotEmpty()){
                when (val cur = queue.poll()) {
                    /**
                     * Only actual in nodes here
                     */
                    is ActualInNode -> {
                        /**
                         * Summary edges present?
                         *  => walk over the call node
                         * else?
                         *  => add the current node later to the queue for reevaluation
                         */
                        cur.summaryEdges?.let(state::addToQueue) ?: laterQueue.add(cur)
                        state.addToQueue(cur.neighbors)
                    }
                    is FormalOutNode -> {
                        if (fi.summaryEdges == null || !fi.summaryEdges!!.contains(cur)) {
                            formalInToOut.computeIfAbsent(fi, { HashSet() }).add(cur)
                        }
                    }
                    else -> {
                        state.addToQueue(cur.neighbors)
                    }
                }
            }
            laterQueue.forEach { state.forceAddToQueue(it) }
        }
        return formalInToOut
    }
}