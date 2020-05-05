package edu.kit.joana.wala.summary.parex

import org.jgrapht.DirectedGraph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

typealias Col<T> = MutableList<T>

typealias CallGraph = DirectedGraph<FuncNode, DefaultEdge>

open class Graph(val entry: FuncNode,
                 /**
             * index == node.id
             */
            var nodes: List<Node?> = ArrayList(),
                 val actualIns: List<ActualInNode> = ArrayList(),
                 val formalIns: List<FormalInNode> = ArrayList(),
                 val funcMap: Map<Int, FuncNode> = HashMap(),
                 val callGraph: CallGraph = DefaultDirectedGraph(DefaultEdge::class.java)) {

    init {
        addFuncNode(entry).also {
            if (nodes.isEmpty()){
                addNode(it)
            }
        }
    }

    fun createActualIn(id: Int): ActualInNode {
        val actualIn = ActualInNode(id)
        (actualIns as MutableList).add(actualIn)
        addNode(actualIn)
        return actualIn
    }

    fun createFormalIn(id: Int, funcNode: FuncNode): FormalInNode {
        val formalIn = FormalInNode(id, funcNode)
        (formalIns as MutableList).add(formalIn)
        addNode(formalIn)
        return formalIn
    }

    fun addFuncNode(f: FuncNode): FuncNode {
        (funcMap as MutableMap)[f.id] = f
        callGraph.addVertex(f)
        return f
    }

    fun getOrCreateFuncNode(id: Int): FuncNode {
        return funcMap[id] ?: addFuncNode(FuncNode(id).also(this::addNode))
    }

    fun removeSummaryEdges(){
        actualIns.forEach {
            it.summaryEdges = mutableListOf()
        }
    }

    /**
     * Only works if the nodes field contains a mutable list (the default)
     */
    fun addNode(node: Node){
        (nodes as ArrayList<Node?>).ensureCapacity(node.id)
        while (nodes.size <= node.id) {
            (nodes as ArrayList<Node?>).add(null)
        }
        (nodes as ArrayList<Node?>)[node.id] = node
    }


    /**
     * Reorders the nodes so that the node ids are consecutive for each function:
     *
     * entry node
     * node 1 of function
     * …
     * node n of function
     * entry node
     * …
     */
    fun reorderNodes(): ReorderedGraph {
        val oldIdToNewId: Array<Int> = Array<Int>(nodes.size){-1}
        // collect all nodes belonging to a function, excluding the function node
        val entryToNodes = funcMap.values.parallelStream().map { func ->
            func to func.reachable { n: Node ->
                val outgoing = n.outgoing(true).filter { it !is FuncNode }
                if (n is CallNode){
                    outgoing + n.actualOuts + n.actualIns
                } else {
                    outgoing
                }
            }.toList()
        }.collect(Collectors.toMap({it: Pair<Node, List<Node>> -> it.first}, {it: Pair<Node, List<Node>> -> it.second}))
        val startIds = mutableMapOf<FuncNode, Int>()
        // store the start ids
        var prev = 0
        funcMap.values.forEach { func ->
            startIds[func] = prev
            prev += entryToNodes[func]!!.size
        }
        val newIdToOldId: Array<Int> = Array<Int>(prev){-1}
        // create new ids for each node
        funcMap.values.parallelStream().forEach { func ->
            val startId = startIds[func]
            newIdToOldId[startId!!] = func.id
            oldIdToNewId[func.id] = startId
            entryToNodes[func]!!.filter { it.id != func.id }.forEachIndexed { index, node ->
                val id = startId + index + 1
                newIdToOldId[id] = node.id
                oldIdToNewId[node.id] = id
            }
        }
        return ReorderedGraph(this, oldIdToNewId, newIdToOldId)
    }

    open fun printableId(id: Int): Int = id

    open fun graphId(printableId: Int): Int = printableId

    open fun printableIds(ids: List<Int>): List<Int> = ids

    open fun graphId(printableIds: List<Int>): List<Int> = printableIds

    open fun forEachNodeInPrintOrder(action: (Node?) -> Unit) {
        nodes.forEach(action)
    }

    open fun nodesInPrintOrder() = nodes

    open fun numberOfIds(): Int = nodes.size
}


class ReorderedGraph(g: Graph, val oldIdToNewId: Array<Int> = arrayOf(), val newIdToOldId: Array<Int> = arrayOf()) :
        Graph(g.entry, g.nodes, g.actualIns, g.formalIns, g.funcMap, g.callGraph) {

    override fun printableId(id: Int): Int = oldIdToNewId[id]
    override fun graphId(printableId: Int): Int = newIdToOldId[printableId]
    override fun printableIds(ids: List<Int>): List<Int> = ids.map(oldIdToNewId::get)
    override fun graphId(printableIds: List<Int>): List<Int> = printableIds.map(newIdToOldId::get)

    override fun forEachNodeInPrintOrder(action: (Node?) -> Unit) {
        newIdToOldId.forEach {
            action(nodes[it])
        }
    }

    override fun nodesInPrintOrder(): List<Node> = newIdToOldId.map { nodes[it]!! }

    override fun numberOfIds(): Int = newIdToOldId.size
}

/**
 * A basic node that has connections to neighboring nodes, from init to exit
 */
open class Node(
        val id: Int,
        /**
         * Neighbors with edges in exit to init direction
         */
        var neighbors: Col<Node>,
        var reducedNeighbors: Col<Node>? = null,
        /**
         * Custom data
         */
        var data: Any? = null) {
    fun add(vararg args: Node) {
        neighbors.addAll(args)
    }

    open fun outgoing(hideCallGraph: Boolean = false): List<Node> = curNeighbors().toList()

    /**
     * includes self
     */
    fun <T : Node> reachable(next: (T) -> Collection<T>): Set<T> {
        val queue: ArrayDeque<T> = ArrayDeque()
        val seen: MutableSet<T> = HashSet()
        queue.add(this as T)
        seen.add(this as T)
        while (!queue.isEmpty()) {
            val head = queue.pop()
            val ns = next(head).filter { !seen.contains(it) }.distinct()
            try {
                queue.addAll(ns)
                seen.addAll(ns)
            } catch (e: Exception) {
                println("${ns.size}, ${seen.size}, ${queue.size}")
            }
        }
        return seen
    }

    override fun toString(): String {
        return "${javaClass.simpleName}($id)"
    }

    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return id
    }

    fun curNeighbors(): Col<Node> = reducedNeighbors ?: neighbors

}

/**
 * Node representing a call statement, edges to it by actual in nodes
 */
class CallNode(
        id: Int,
        /**
         * ActualOut nodes
         */
        neighbors: Col<Node>,
        val actualIns: Col<Node> = mutableListOf(),
        val actualOuts: Col<Node> = mutableListOf(),
        /**
         * Owning function node
         */
        val owner: FuncNode,
        /**
         * Target function node
         */
        val targets: Col<FuncNode>) : Node(id, neighbors) {

    override fun outgoing(hideCallGraph: Boolean): List<Node> = (if (hideCallGraph) targets else targets + owner) + curNeighbors() + actualOuts

    fun actualOut(formalOut: FormalOutNode): OutNode? {
        return formalOut.actualOuts[this]
    }
}

class FuncNode(
        /**
         * Id of the entry node
         */
        id: Int,
        /**
         * Roots the graph of statements that belong to this function
         */
        neighbors: Col<Node> = mutableListOf(),
        /**
         * Calls to other functions
         * ∀ c ∈ neighbors: c.owner = this
         */
        val callees: Col<CallNode> = mutableListOf(),
        /**
         * ∀ c ∈ callers: c.target = this
         */
        val callers: Col<CallNode> = mutableListOf(),
        /**
         * Formal in nodes
         */
        val formalIns: Col<FormalInNode> = mutableListOf(),
        /**
         * Formal out nodes
         */
        val formalOuts: Col<OutNode> = mutableListOf()) : Node(id, neighbors) {

    override fun outgoing(hideCallGraph: Boolean): List<Node> = super.outgoing(hideCallGraph) +
            (if (hideCallGraph) emptyList() else callers + callees) + formalIns + formalOuts

    fun reachableFuncNodes(): Set<FuncNode> = reachable { it.callees.flatMap { cal -> cal.targets } }
}

open class InNode(id: Int, neighbors: Col<Node> = mutableListOf(), var summaryEdges: Col<OutNode> = mutableListOf()) : Node(id, neighbors) {
    override fun outgoing(hideCallGraph: Boolean): List<Node> = super.outgoing(hideCallGraph) + summaryEdges
}

// nxm mapping (n ≠ m, but no other relation) for actual ins to formal in

class ActualInNode(id: Int, var callNode: CallNode? = null, neighbors: Col<Node> = mutableListOf(), val formalIns: MutableMap<FuncNode, FormalInNode> = mutableMapOf()) : InNode(id, neighbors) {
    override fun outgoing(hideCallGraph: Boolean): List<Node> = super.outgoing(hideCallGraph) + ((callNode?.let { listOf(it) }) ?: listOf())
}

class FormalInNode(id: Int, val owner: FuncNode, neighbors: Col<Node> = mutableListOf(), val actualIns: MutableMap<CallNode, ActualInNode> = mutableMapOf()) : InNode(id, neighbors)

open class OutNode(id: Int, neighbors: Col<Node> = mutableListOf()) : Node(id, neighbors)

class FormalOutNode(id: Int, neighbors: Col<Node> = mutableListOf(), val actualOuts: MutableMap<CallNode, OutNode> = mutableMapOf()) : OutNode(id, neighbors)