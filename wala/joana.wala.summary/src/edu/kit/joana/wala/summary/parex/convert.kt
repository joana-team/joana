/**
 * Convert an SDG graph to a parex graph and back
 */

package edu.kit.joana.wala.summary.parex

import edu.kit.joana.ifc.sdg.graph.SDG
import edu.kit.joana.ifc.sdg.graph.SDGEdge
import edu.kit.joana.ifc.sdg.graph.SDGNode
import edu.kit.joana.util.graph.AbstractBaseGraph
import edu.kit.joana.wala.summary.WorkPackage
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.stream.Collectors

val DEFAULT_RELEVANT_EDGES: EnumSet<SDGEdge.Kind> = EnumSet.of(SDGEdge.Kind.DATA_DEP, SDGEdge.Kind.DATA_HEAP, SDGEdge.Kind.DATA_ALIAS, SDGEdge.Kind.DATA_LOOP, SDGEdge.Kind.DATA_DEP_EXPR_VALUE, SDGEdge.Kind.DATA_DEP_EXPR_REFERENCE, SDGEdge.Kind.CONTROL_DEP_COND, SDGEdge.Kind.CONTROL_DEP_UNCOND, SDGEdge.Kind.CONTROL_DEP_EXPR, SDGEdge.Kind.CONTROL_DEP_CALL, SDGEdge.Kind.JUMP_DEP, SDGEdge.Kind.SUMMARY, SDGEdge.Kind.SUMMARY_DATA, SDGEdge.Kind.SUMMARY_NO_ALIAS, SDGEdge.Kind.SYNCHRONIZATION)

/**
 * Convert a SDG to a parex graph
 */
class SDGToGraph(val relevantEdges: Set<SDGEdge.Kind> = DEFAULT_RELEVANT_EDGES,
                 val ignoreSummaryEdges: Boolean = true) {

    fun consider(edgeKind: SDGEdge.Kind): Boolean {
        return (!ignoreSummaryEdges || (edgeKind != SDGEdge.Kind.SUMMARY && edgeKind != SDGEdge.Kind.SUMMARY_DATA && edgeKind != SDGEdge.Kind.SUMMARY_NO_ALIAS)) &&
                relevantEdges.contains(edgeKind)
    }

    fun considerForEntry(edge: SDGEdge): Boolean {
        return consider(edge.kind) || edge.target.kind.oneOf(SDGNode.Kind.FORMAL_IN, SDGNode.Kind.FORMAL_OUT, SDGNode.Kind.EXIT)
    }

    fun convert(pack: WorkPackage<SDG>, parallel: Boolean = false): Graph {
        return convert(pack.graph, parallel)
    }

    @JvmOverloads
    fun convert(graph: SDG, parallel: Boolean = false, executor: ExecutorService? = null): Graph {
        if (parallel){
            return SDGToGraph2(relevantEdges, ignoreSummaryEdges).convert(graph, executor)
        }
        return Converter(graph).convert()
    }


    private inner class Converter(val sdg: SDG) {
        /**
         * Id of the entry nodes
         */
        val graph = Graph(FuncNode(sdg.root.id))

        fun convert(): Graph {
            sdg.entryNodesPerProcId.values.stream().forEach {
                FuncConverter(it).convert()
            }
            return graph
        }

        val sdgNodeToNode = IdentityHashMap<SDGNode, Node>()

        internal open inner class FuncConverter(val entry: SDGNode) {

            val funcNode = graph.getOrCreateFuncNode(entry.id)

            internal open fun convert() {
                getNodesInFunc(entry).forEach(this::process)
            }

            /**
             * Visits the passed node and returns a list of other nodes to visit, kind of combines accept(·) and next()
             */
            internal fun process(node: SDGNode) {
                val graphNode = createGraphNodeIfNeeded(node)
                return when {
                    node == entry -> {
                        outgoing(node).filter { considerForEntry(it) }
                                .map { it.target }
                                .distinct()
                                .forEach {
                                    if (hasNodeFor(it)){
                                        return@forEach
                                    }
                                    val n = createGraphNodeIfNeeded(it)
                                    when (it.kind) {
                                        SDGNode.Kind.FORMAL_IN -> {
                                            funcNode.formalIns.add(n as FormalInNode)
                                            incoming(it)
                                                    .map(SDGEdge::getSource)
                                                    .filter { e -> e.kind == SDGNode.Kind.ACTUAL_IN }
                                                    .distinct()
                                                    .forEach { t ->
                                                        val actInNode = createGraphNodeIfNeeded(t) as ActualInNode
                                                        val callNode = actInNode.callNode ?: createGraphNodeIfNeeded(incoming(t).find { e -> e.source.kind == SDGNode.Kind.CALL }!!.source) as CallNode
                                                        n.actualIns[callNode] = actInNode
                                                        actInNode.formalIns[funcNode] = n
                                                    }
                                        }
                                        SDGNode.Kind.FORMAL_OUT, SDGNode.Kind.EXIT  -> {
                                            funcNode.formalOuts.add(n as FormalOutNode)
                                            outgoing(it).filter { e -> e.target.kind == SDGNode.Kind.ACTUAL_OUT }
                                                    .map(SDGEdge::getTarget).forEach { t ->
                                                            n.actualOuts[createGraphNodeIfNeeded(incoming(t)
                                                                    .find { e -> e.source.kind == SDGNode.Kind.CALL }!!.source) as CallNode] =
                                                                    createGraphNodeIfNeeded(t) as OutNode
                                            }
                                        }
                                        else -> funcNode.neighbors.add(n)
                                    }
                                }
                    }
                    graphNode is CallNode -> {
                        outgoing(node)
                                .map { it.target }
                                .filter { it.kind == SDGNode.Kind.ACTUAL_IN }
                                .distinctBy { it.id }
                                .forEach {
                                    (createGraphNodeIfNeeded(it) as ActualInNode).also { n ->
                                        n.callNode = graphNode
                                        graphNode.actualIns.add(n)
                                    }
                                }
                        graphNode.targets.forEach {
                            it.callers.add(graphNode)
                            graph.callGraph.addEdge(funcNode, it)
                        }
                        graphNode.owner.callees.add(graphNode)
                        outgoing(node).filter { it.target.kind == SDGNode.Kind.ACTUAL_OUT }
                                .map { it.target }
                                .distinctBy { it.id }
                                .forEach { graphNode.actualOuts.add(createGraphNodeIfNeeded(it)); }
                        outgoing(node).filter { consider(it.kind) && it.target.kind != SDGNode.Kind.ACTUAL_OUT
                                && it.target.kind != SDGNode.Kind.ACTUAL_IN}
                                .map { it.target }
                                .distinctBy { it.id }
                                .forEach { defaultNodeAdd(graphNode, it) }
                    }
                    else ->
                        outgoing(node).filter { consider(it.kind) }
                            .map { it.target }
                            .distinctBy { it.id }
                            .forEach { defaultNodeAdd(graphNode, it) }
                }
            }

            private fun defaultNodeAdd(graphNode: Node, target: SDGNode): SDGNode {
                graphNode.neighbors.add(createGraphNodeIfNeeded(target))
                return target
            }

            open fun createGraphNodeIfNeeded(node: SDGNode): Node {
                return createGraphNodeIfNeeded(funcNode, node)
            }
        }

        internal fun createGraphNodeIfNeeded(funcNode: FuncNode, node: SDGNode): Node {
            return sdgNodeToNode.computeIfAbsent(node) {
                createGraphNode(funcNode, node)
            }
        }

        /**
         * Create a bare node
         */
        private fun createGraphNode(funcNode: FuncNode, node: SDGNode): Node {
            return when (node.kind) {
                SDGNode.Kind.ACTUAL_IN ->
                    graph.createActualIn(node.id)
                SDGNode.Kind.FORMAL_IN ->
                    graph.createFormalIn(node.id, funcNode)
                SDGNode.Kind.CALL ->
                    CallNode(node.id, mutableListOf(), mutableListOf(), mutableListOf(), graph.getOrCreateFuncNode(sdg.getEntry(node).id),
                            getCallNodeTargets(node)).also(graph::addNode)
                SDGNode.Kind.ACTUAL_OUT ->
                    OutNode(node.id).also(graph::addNode)
                SDGNode.Kind.FORMAL_OUT, SDGNode.Kind.EXIT ->
                    FormalOutNode(node.id).also(graph::addNode)
                else ->
                    Node(node.id, mutableListOf()).also(graph::addNode)
            }
        }

        private fun hasNodeFor(target: SDGNode?): Boolean {
            return sdgNodeToNode.containsKey(target)
        }

        internal fun getCallNodeTargets(node: SDGNode): MutableList<FuncNode> {
            return outgoing(node).filter { it.kind == SDGEdge.Kind.CALL }.map { graph.getOrCreateFuncNode(it.target.id) }.toMutableList()
        }

        private fun outgoing(node: SDGNode) = (sdg as AbstractBaseGraph<SDGNode, SDGEdge>).outgoingEdgesOfUnsafe(node)

        private fun incoming(node: SDGNode) = (sdg as AbstractBaseGraph<SDGNode, SDGEdge>).incomingEdgesOfUnsafe(node)

        internal fun getNodesInFunc(entry: SDGNode): Set<SDGNode> {
            val alreadySeen = mutableSetOf<SDGNode>()
            val queue = ArrayDeque(listOf(entry))
            while (queue.isNotEmpty()) {
                val sdgNode = queue.pop()
                if (alreadySeen.contains(sdgNode)) {
                    continue
                }
                alreadySeen.add(sdgNode)
                queue.addAll(when (sdgNode.kind) {
                    SDGNode.Kind.ENTRY -> outgoing(sdgNode).filter { considerForEntry(it) }.map { it.target }
                    else -> outgoing(sdgNode).filter { consider(it.kind) }.map { it.target }
                })
            }
            return alreadySeen
        }
    }
}


/**
 * Convert a SDG to a parex graph in parallel
 */
class SDGToGraph2(val relevantEdges: Set<SDGEdge.Kind> = DEFAULT_RELEVANT_EDGES,
                 val ignoreSummaryEdges: Boolean = false) {

    fun consider(edgeKind: SDGEdge.Kind): Boolean {
        return (!ignoreSummaryEdges || (edgeKind != SDGEdge.Kind.SUMMARY && edgeKind != SDGEdge.Kind.SUMMARY_DATA && edgeKind != SDGEdge.Kind.SUMMARY_NO_ALIAS)) &&
                relevantEdges.contains(edgeKind)
    }

    fun considerForEntry(edge: SDGEdge): Boolean {
        return consider(edge.kind) || edge.target.kind.oneOf(SDGNode.Kind.FORMAL_IN, SDGNode.Kind.FORMAL_OUT, SDGNode.Kind.EXIT)
    }

    fun convert(pack: WorkPackage<SDG>): Graph {
        return convert(pack.graph)
    }

    @JvmOverloads
    public fun convert(graph: SDG, executor: ExecutorService? = null): Graph {
        return ParallelConverter(graph, executor ?: Executors.newWorkStealingPool()).convert()
    }

    /**
     * Converts the graph in parallel in different stages
     */
    inner class ParallelConverter(val sdg: SDG, val executor: ExecutorService) {

        /**
         * Id of the entry nodes
         */
        val graph = Graph(FuncNode(sdg.root.id))

        fun convert(): Graph {

            val procs = sdg.entryNodesPerProcId

            /**
             * Add functions
             */
            procs.values.forEach {
                it.customData = graph.getOrCreateFuncNode(it.id)
            }

            /**
             * Collect the nodes that belong to each function
             */
            val nodesPerProc = executor.submit(Callable {
                val map = mutableMapOf<Int, Set<SDGNode>>()
                procs.values.parallelStream().map { Pair(it.id, getNodesInFunc(it)) }.collect(Collectors.toList()).forEach {
                    map[it.first] = it.second
                }
                return@Callable map
            }).get() as Map<Int, Set<SDGNode>>


            /**
             * Add other nodes (and actual ins)
             */
            executor.submit {
                (graph.actualIns as MutableList).addAll(procs.values.parallelStream()
                        .flatMap { createNonFuncNodes(it.customData as FuncNode, nodesPerProc[it.id]!!).stream() }.collect(Collectors.toList()))
            }.get()

            /**
             * Find maximum node id
             */
            val maxId = executor.submit(Callable {
                nodesPerProc.values.parallelStream().mapToInt { it.maxBy { n -> n.id }?.id ?: Integer.MAX_VALUE }.max().asInt
            }).get()

            /**
             * Add nodes
             */
            executor.submit {
                val array = Array<Node?>(maxId + 1) {null}
                nodesPerProc.values.parallelStream().forEach { it.forEach { n -> array[n.id] = n.customData as Node } }
                graph.nodes = Arrays.asList(*array) as List<Node?>
            }.get()

            /**
             * Set connections
             */
            val calledFunctionsPerFunc = executor.submit(Callable<Map<FuncNode, Set<FuncNode>>> {
                procs.values.parallelStream().map { n ->
                    n to ConnectionAdder(n, nodesPerProc[n.id]!!).convert()
                }.collect(Collectors.toMap({it.first.customData as FuncNode}, {it.second}))
            }).get() as Map<FuncNode, Set<FuncNode>>

            /**
             * Collect formal ins
             */
            executor.submit {
                (graph.formalIns as MutableList).addAll(procs.values.parallelStream()
                        .flatMap { (it.customData as FuncNode).formalIns.stream() }.collect(Collectors.toList()))
            }.get()

            /**
             * Build call graph and update FuncNode.callers sequentially
             */
            calledFunctionsPerFunc.forEach { (caller, called) ->
                called.forEach { calledFunc ->
                    graph.callGraph.addEdge(caller, calledFunc)
                }
                caller.callees.forEach { call ->
                    call.targets.forEach { calledFunc ->
                        calledFunc.callers.add(call)
                    }
                }
            }
            return graph
        }

        /**
         * Be aware to collect the formal ins later
         */
        private fun createNonFuncNodes(funcNode: FuncNode, nodes: Set<SDGNode>): List<ActualInNode> {
            val actualIns = mutableListOf<ActualInNode>()
            nodes.forEach { node ->
                when (node.kind) {
                    SDGNode.Kind.ACTUAL_IN ->
                        node.customData = ActualInNode(node.id).also { actualIns.add(it) }
                    SDGNode.Kind.FORMAL_IN ->
                        node.customData = FormalInNode(node.id, funcNode)
                    SDGNode.Kind.CALL ->
                        node.customData = CallNode(node.id, mutableListOf(), mutableListOf(), mutableListOf(), sdg.getEntry(node).customData as FuncNode,
                                getCallNodeTargets(node))
                    SDGNode.Kind.ACTUAL_OUT ->
                        node.customData = OutNode(node.id)
                    SDGNode.Kind.FORMAL_OUT, SDGNode.Kind.EXIT ->
                        node.customData = FormalOutNode(node.id)
                    SDGNode.Kind.ENTRY -> {
                    }
                    else ->
                        node.customData = Node(node.id, mutableListOf())
                }
            }
            return actualIns
        }

        /**
         * Important: Does not change the call graph or called FuncNodes (FuncNode.callers has to be altered later)
         */
        internal inner class ConnectionAdder(val entry: SDGNode, val nodes: Set<SDGNode>) {
            private val calledFunctions = mutableSetOf<FuncNode>()

            fun convert(): Set<FuncNode> {
                calledFunctions.clear()
                nodes.forEach(this::process)
                return calledFunctions
            }

            fun SDGNode.getGraphNode(): Node {
                return customData as Node
            }

            val funcNode = graph.getOrCreateFuncNode(entry.id)

            /**
             * Visits the passed node and returns a list of other nodes to visit, kind of combines accept(·) and next()
             */
            internal fun process(node: SDGNode) {
                val graphNode = node.getGraphNode()
                when {
                    node == entry -> {
                        outgoing(node).filter { considerForEntry(it) }
                                .map { it.target }
                                .distinct()
                                .forEach {
                                    if (hasNodeFor(it)) {
                                        return@forEach
                                    }
                                    val n = it.getGraphNode()
                                    when (it.kind) {
                                        SDGNode.Kind.FORMAL_IN -> {
                                            funcNode.formalIns.add(n as FormalInNode)
                                            incoming(it)
                                                    .map(SDGEdge::getSource)
                                                    .filter { e -> e.kind == SDGNode.Kind.ACTUAL_IN }
                                                    .distinct()
                                                    .forEach { t ->
                                                        val actInNode = t.getGraphNode() as ActualInNode
                                                        val callNode = actInNode.callNode
                                                                ?: (incoming(t).find { e -> e.source.kind == SDGNode.Kind.CALL }!!.source).getGraphNode() as CallNode
                                                        n.actualIns[callNode] = actInNode
                                                        actInNode.formalIns[funcNode] = n
                                                    }
                                        }
                                        SDGNode.Kind.FORMAL_OUT, SDGNode.Kind.EXIT -> {
                                            funcNode.formalOuts.add(n as FormalOutNode)
                                            outgoing(it).filter { e -> e.target.kind == SDGNode.Kind.ACTUAL_OUT }
                                                    .map(SDGEdge::getTarget).forEach { t ->
                                                        n.actualOuts[(incoming(t)
                                                                .find { e -> e.source.kind == SDGNode.Kind.CALL }!!.source).getGraphNode() as CallNode] =
                                                                t.getGraphNode() as OutNode
                                                    }
                                        }
                                        else -> funcNode.neighbors.add(n)
                                    }
                                }
                    }
                    graphNode is CallNode -> {
                        outgoing(node)
                                .map { it.target }
                                .filter { it.kind == SDGNode.Kind.ACTUAL_IN }
                                .distinctBy { it.id }
                                .forEach {
                                    (it.getGraphNode() as ActualInNode).also { n ->
                                        n.callNode = graphNode
                                        graphNode.actualIns.add(n)
                                    }
                                }
                        graphNode.targets.forEach {
                            calledFunctions.add(it)
                        }
                        graphNode.owner.callees.add(graphNode)
                        outgoing(node).filter { it.target.kind == SDGNode.Kind.ACTUAL_OUT }
                                .map { it.target }
                                .distinctBy { it.id }
                                .forEach { graphNode.actualOuts.add(it.getGraphNode()); }
                        outgoing(node).filter { consider(it.kind) && it.target.kind != SDGNode.Kind.ACTUAL_OUT
                                && it.target.kind != SDGNode.Kind.ACTUAL_IN}
                                .map { it.target }
                                .distinctBy { it.id }
                                .forEach { defaultNodeAdd(graphNode, it) }
                    }
                    else ->
                        outgoing(node).filter { consider(it.kind) }
                                .map { it.target }
                                .distinctBy { it.id }
                                .forEach { defaultNodeAdd(graphNode, it) }
                }
            }

            private fun defaultNodeAdd(graphNode: Node, target: SDGNode): SDGNode {
                graphNode.neighbors.add(target.getGraphNode())
                return target
            }
        }

        val sdgNodeToNode = IdentityHashMap<SDGNode, Node>()

        private fun hasNodeFor(target: SDGNode?): Boolean {
            return sdgNodeToNode.containsKey(target)
        }

        internal fun getCallNodeTargets(node: SDGNode): MutableList<FuncNode> {
            return outgoing(node).filter { it.kind == SDGEdge.Kind.CALL }.map { graph.getOrCreateFuncNode(it.target.id) }.toMutableList()
        }

        private fun outgoing(node: SDGNode) = (sdg as AbstractBaseGraph<SDGNode, SDGEdge>).outgoingEdgesOfUnsafe(node)

        private fun incoming(node: SDGNode) = (sdg as AbstractBaseGraph<SDGNode, SDGEdge>).incomingEdgesOfUnsafe(node)

        internal fun getNodesInFunc(entry: SDGNode): Set<SDGNode> {
            val alreadySeen = mutableSetOf<SDGNode>()
            val queue = ArrayDeque(listOf(entry))
            while (queue.isNotEmpty()) {
                val sdgNode = queue.pop()
                if (alreadySeen.contains(sdgNode)) {
                    continue
                }
                alreadySeen.add(sdgNode)
                queue.addAll(when (sdgNode.kind) {
                    SDGNode.Kind.ENTRY -> outgoing(sdgNode).filter { considerForEntry(it) }.map { it.target }
                    else -> outgoing(sdgNode).filter { consider(it.kind) }.map { it.target }
                })
            }
            return alreadySeen
        }
    }
}