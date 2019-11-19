/**
 * Convert an SDG graph to a parex graph and back
 */

package edu.kit.joana.wala.summary.parex

import edu.kit.joana.ifc.sdg.graph.SDG
import edu.kit.joana.ifc.sdg.graph.SDGEdge
import edu.kit.joana.ifc.sdg.graph.SDGNode
import edu.kit.joana.util.graph.AbstractBaseGraph
import edu.kit.joana.wala.summary.WorkPackage
import gnu.trove.set.hash.TIntHashSet
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


    private inner class Converter(val sdg: SDG, val start: SDGNode) {
        private val seenProcs = TIntHashSet()
        /**
         * Id of the entry nodes
         */
        private val procQueue = ArrayDeque<Int>()
        private val graph = Graph(FuncNode(start.id))

        fun convert(): Graph {
            procQueue.add(graph.entry.id)
            while (procQueue.isNotEmpty()) {
                val proc = procQueue.pop()
                if (seenProcs.contains(proc)) {
                    continue
                }
                seenProcs.add(proc)
                FuncConverter(graph.getOrCreateFuncNode(proc)).convert()
            }
            seenProcs.clear()
            return graph
        }

        val sdgNodeToNode = IdentityHashMap<SDGNode, Node>()

        private inner class FuncConverter(val funcNode: FuncNode) {
            val entry: SDGNode = sdg.getNode(funcNode.id)

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
                                            incoming(it.target).map(SDGEdge::getSource).filter { e -> e.kind == SDGNode.Kind.ACTUAL_IN }
                                                    .distinct().forEach { t ->
                                                        val callNode = createGraphNodeIfNeeded(incoming(t).find { e -> e.source.kind == SDGNode.Kind.CALL }!!.source) as CallNode
                                                        val actInNode = createGraphNodeIfNeeded(t) as ActualInNode
                                                        n.actualIns[callNode] = actInNode
                                                        actInNode.formalIns[funcNode] = n
                                                    }
                                        }
                                        SDGNode.Kind.FORMAL_OUT, SDGNode.Kind.EXIT  -> {
                                            if (funcNode.id == 29){
                                                println(it)
                                            }
                                            funcNode.formalOuts.add(n as FormalOutNode)
                                            outgoing(it.target).filter { e -> e.target.kind == SDGNode.Kind.ACTUAL_OUT }
                                                    .map(SDGEdge::getTarget).forEach { t ->
                                                            (n as FormalOutNode).actualOuts[createGraphNodeIfNeeded(incoming(t).find { e -> e.source.kind == SDGNode.Kind.CALL }!!.source) as CallNode] =
                                                                    createGraphNodeIfNeeded(t) as OutNode
                                            }
                                        }
                                        else -> funcNode.neighbors.add(n)
                                    }
                                    it.target
                                }
                    }
                    /**
                     * Call nodes are degraded to normal nodes if they do not have a call edge
                     */
                    graphNode is CallNode -> {
                        outgoing(node).filter { it.target.kind == SDGNode.Kind.ACTUAL_IN }
                                .forEach {
                                    (createGraphNodeIfNeeded(it.target) as ActualInNode).callNode = createGraphNodeIfNeeded(it.source) as CallNode
                                }
                        outgoing(node).filter { it.target.kind == SDGNode.Kind.ACTUAL_IN }
                                .map { it.target }.distinct()
                                .filter { it.kind == SDGNode.Kind.ACTUAL_IN }
                                .forEach {
                                    createGraphNodeIfNeeded(it).also { n ->
                                        graphNode.actualIns.add(n)
                                    }
                                }
                        graphNode.targets.forEach { it.callers.add(graphNode) }
                        graphNode.owner.callees.add(graphNode)
                        graphNode.targets.forEach { procQueue.push(it.id) }
                        return outgoing(node).filter { it.target.kind == SDGNode.Kind.ACTUAL_OUT }.distinct()
                                .map { defaultNodeAdd(graphNode, it) }
                    }
                    else ->
                        outgoing(node).filter { consider(it.kind) }
                                .map { defaultNodeAdd(graphNode, it) }
                }
            }

            private inline fun defaultNodeAdd(graphNode: Node, edge: SDGEdge): SDGNode {
                graphNode.neighbors.add(createGraphNodeIfNeeded(edge.target))
                return edge.target
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

    @JvmOverloads
    fun convert(graph: SDG, root: SDGNode = graph.root): Graph {
        return Converter(graph, root).convert()
    }
}