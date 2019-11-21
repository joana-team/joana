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

val DEFAULT_RELEVANT_EDGES: EnumSet<SDGEdge.Kind> = EnumSet.of(SDGEdge.Kind.DATA_DEP, SDGEdge.Kind.DATA_HEAP, SDGEdge.Kind.DATA_ALIAS, SDGEdge.Kind.DATA_LOOP, SDGEdge.Kind.DATA_DEP_EXPR_VALUE, SDGEdge.Kind.DATA_DEP_EXPR_REFERENCE, SDGEdge.Kind.CONTROL_DEP_COND, SDGEdge.Kind.CONTROL_DEP_UNCOND, SDGEdge.Kind.CONTROL_DEP_EXPR, SDGEdge.Kind.CONTROL_DEP_CALL, SDGEdge.Kind.JUMP_DEP, SDGEdge.Kind.SUMMARY, SDGEdge.Kind.SUMMARY_DATA, SDGEdge.Kind.SUMMARY_NO_ALIAS, SDGEdge.Kind.SYNCHRONIZATION)

/**
 * Convert a SDG to a parex graph
 */
class SDGToGraph(val relevantEdges: Set<SDGEdge.Kind> = DEFAULT_RELEVANT_EDGES,
                 val ignoreSummaryEdges: Boolean = false) {

    fun consider(edgeKind: SDGEdge.Kind): Boolean {
        return (!ignoreSummaryEdges || (edgeKind != SDGEdge.Kind.SUMMARY && edgeKind != SDGEdge.Kind.SUMMARY_DATA && edgeKind != SDGEdge.Kind.SUMMARY_NO_ALIAS)) &&
                relevantEdges.contains(edgeKind)
    }

    fun convert(pack: WorkPackage<SDG>): Graph {
        return convert(pack.graph)
    }

    fun convert(graph: SDG): Graph {
        return Converter(graph).convert()
    }


    private inner class Converter(val sdg: SDG) {
        /**
         * Id of the entry nodes
         */
        private val graph = Graph(FuncNode(sdg.root.id))

        fun convert(): Graph {
            for ((_, entry) in sdg.entryNodesPerProcId) {
                FuncConverter(entry).convert()
            }
            return graph
        }

        val sdgNodeToNode = IdentityHashMap<SDGNode, Node>()

        private inner class FuncConverter(val entry: SDGNode) {

            val funcNode = graph.getOrCreateFuncNode(entry.id)

            internal fun convert() {
                val alreadySeen = mutableSetOf<SDGNode>()
                val queue = ArrayDeque(listOf(entry))
                while (queue.isNotEmpty()) {
                    val sdgNode = queue.pop()
                    if (alreadySeen.contains(sdgNode)) {
                        continue
                    }
                    alreadySeen.add(sdgNode)
                    queue.addAll(visit(sdgNode))
                }
            }

            /**
             * Visits the passed node and returns a list of other nodes to visit, kind of combines accept(·) and next()
             */
            private fun visit(node: SDGNode): Collection<SDGNode> {
                val graphNode = createGraphNodeIfNeeded(node)
                return when {
                    node == entry -> {
                        return outgoing(node).filter { consider(it.kind) || oneOf(it.target.kind, SDGNode.Kind.FORMAL_IN, SDGNode.Kind.FORMAL_OUT, SDGNode.Kind.EXIT) }
                                .map {
                                    if (sdgNodeToNode.containsKey(it.target)){
                                        return@map it.target
                                    }
                                    val n = createGraphNodeIfNeeded(it.target)
                                    when (it.target.kind) {
                                        SDGNode.Kind.FORMAL_IN -> {
                                            funcNode.formalIns.add(n as FormalInNode)
                                            incoming(it.target)
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
                                            outgoing(it.target).filter { e -> e.target.kind == SDGNode.Kind.ACTUAL_OUT }
                                                    .map(SDGEdge::getTarget).forEach { t ->
                                                            n.actualOuts[createGraphNodeIfNeeded(incoming(t)
                                                                    .find { e -> e.source.kind == SDGNode.Kind.CALL }!!.source) as CallNode] =
                                                                    createGraphNodeIfNeeded(t) as OutNode
                                            }
                                        }
                                        else -> funcNode.neighbors.add(n)
                                    }
                                    it.target
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
                        return outgoing(node).filter { it.target.kind == SDGNode.Kind.ACTUAL_OUT }
                                .map { it.target }
                                .distinctBy { it.id }
                                .map { defaultNodeAdd(graphNode, it) }
                    }
                    else ->
                        outgoing(node).filter { consider(it.kind) }
                            .map { it.target }
                            .distinctBy { it.id }
                            .map { defaultNodeAdd(graphNode, it) }
                }
            }

            private fun defaultNodeAdd(graphNode: Node, target: SDGNode): SDGNode {
                graphNode.neighbors.add(createGraphNodeIfNeeded(target))
                return target
            }

            private fun createGraphNodeIfNeeded(node: SDGNode): Node {
                return sdgNodeToNode.computeIfAbsent(node, this::createGraphNode)
            }

            /**
             * Create a bare node
             */
            private fun createGraphNode(node: SDGNode): Node {
                return when (node.kind) {
                    SDGNode.Kind.ACTUAL_IN ->
                        graph.createActualIn(node.id)
                    SDGNode.Kind.FORMAL_IN ->
                        graph.createFormalIn(node.id, funcNode)
                    SDGNode.Kind.CALL ->
                        CallNode(node.id, mutableListOf(), mutableListOf(), graph.getOrCreateFuncNode(sdg.getEntry(node).id),
                            getCallNodeTargets(node))
                    SDGNode.Kind.ACTUAL_OUT ->
                        OutNode(node.id)
                    SDGNode.Kind.FORMAL_OUT, SDGNode.Kind.EXIT ->
                        FormalOutNode(node.id)
                    else ->
                        Node(node.id, mutableListOf())
                }
            }

            private fun getCallNodeTargets(node: SDGNode): List<FuncNode> {
                return outgoing(node).filter { it.kind == SDGEdge.Kind.CALL }.map { graph.getOrCreateFuncNode(it.target.id) }
            }
        }
        private fun outgoing(node: SDGNode) = (sdg as AbstractBaseGraph<SDGNode, SDGEdge>).outgoingEdgesOfUnsafe(node)

        private fun incoming(node: SDGNode) = (sdg as AbstractBaseGraph<SDGNode, SDGEdge>).incomingEdgesOfUnsafe(node)

        private fun <T> oneOf(elem: T, vararg supported: T): Boolean {
            for (t in supported) {
                if (elem == supported) {
                    return true
                }
            }
            return false
        }
    }
}