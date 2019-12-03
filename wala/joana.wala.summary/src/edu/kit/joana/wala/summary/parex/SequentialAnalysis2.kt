package edu.kit.joana.wala.summary.parex

/**
 * An improved sequential analysis.
 *
 * When-ever a new summary edge is found, the analysis starts from the actual out node on and not from the entry node.
 * This should be an improvement over {@link SequentialAnalysis}
 */
open class SequentialAnalysis2 : Analysis {

    internal data class State(val subStates: Map<FormalInNode, NodeQueue<Node>>)

    /**
     * A helper property to access the state for each function node, stored in its data field
     */
    internal val FuncNode.state: State
        get() = data!! as State

    /**
     * An item in the inter-procedural worklist that states that the analysis of another funcNode found for a given funcNode
     * the following new summary edges that should be added
     *
     * actualInToOut == null: the first evaluation of a function
     */
    internal data class QueueItem(val funcNode: FuncNode, val actualInToOut: Map<ActualInNode, Collection<OutNode>>? = null)

    override fun process(g: Graph) {
        initFuncStates(g)
        worklist(initialEntries(g), this::process)
    }

    /**
     * Initialize each the state for each function node
     */
    private fun initFuncStates(g: Graph){
        g.callGraph.vertexSet().forEach { func ->
            func.data = State(func.formalIns.map { fi ->
                /**
                 * Each formal in node gets its own queue state that is initialized with the current neighbors of the formal in
                 * node. We can assume that the formal in node itself will never be part of this queue
                 */
                fi to NodeQueue(fi.curNeighbors())
            }.toMap())
        }
    }

    internal fun initialEntries(g: Graph): Collection<QueueItem> = g.callGraph.vertexSet().map { QueueItem(it) }

    internal fun process(item: QueueItem): List<QueueItem> {
        val (funcNode, inToOut) = item
        /**
         * Try to add summary edges and return directly if nothing changed
         */
        if (inToOut != null){
            var changed = false
            for ((ai, os) in inToOut){
                for (o in os) {
                    if (!ai.summaryEdges.contains(o)){
                        /**
                         * We found a new summary edge
                         */
                        ai.summaryEdges.add(o)
                        /**
                         * add to queue if ai ∈ alreadySeen and o ∉ alreadySeen
                         * i.e. if we found an previously not seen actual out that is connected to an already seen actual in node
                         * (if we have not yet seen the actual in node, then the actual out node will be added to the queue later
                         * anyway)
                         */
                        funcNode.state.subStates.forEach { (_, state) ->
                            if (state.alreadySeen(ai) && !state.alreadySeen(o)){
                                state.push(o)
                            }
                        }
                        changed = true
                    }
                }
            }
            if (!changed){
                return emptyList()
            }
        }



        /**
         * Process the function and find new connections between formal in and formal out nodes
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
        funcNode.state.subStates.forEach {(fi, state) ->
            /**
             * Start for each formal in node where the last iteration ended
             */
            while (state.isNotEmpty()){
                when (val cur = state.poll()) {
                    is ActualInNode -> {
                        /**
                         * Use summary edges if they are there
                         */
                        state.push(cur.summaryEdges)
                        state.push(cur.curNeighbors())
                    }
                    is FormalOutNode -> {
                        if (!fi.summaryEdges.contains(cur)) {
                            /**
                             * Add the newly found connection between the current formal in and formal out nodes
                             */
                            formalInToOut.computeIfAbsent(fi, { HashSet() }).add(cur)
                        }
                    }
                    else -> {
                        state.push(cur.curNeighbors())
                    }
                }
            }
        }
        return formalInToOut
    }
}