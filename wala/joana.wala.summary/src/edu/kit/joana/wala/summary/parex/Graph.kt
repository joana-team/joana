package edu.kit.joana.wala.summary.parex

import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

typealias Col<T> = MutableList<T>

class Graph(val entry: FuncNode) {
    val actualIns: MutableList<ActualInNode> = ArrayList()
    val formalIns: MutableList<FormalInNode> = ArrayList()
    val funcMap: MutableMap<Int, FuncNode> = HashMap()

    init {
        addFuncNode(entry)
    }

    fun createActualIn(id: Int, neighbors: Col<Node> = mutableListOf()): ActualInNode {
        val actualIn = ActualInNode(id)
        actualIns.add(actualIn)
        return actualIn
    }

    fun createFormalIn(id: Int, funcNode: FuncNode, neighbors: Col<Node> = mutableListOf()): FormalInNode {
        val formalIn = FormalInNode(id, funcNode)
        formalIns.add(formalIn)
        return formalIn
    }

    private fun addFuncNode(f: FuncNode): FuncNode {
        funcMap[f.id] = f
        return f
    }

    fun getOrCreateFuncNode(id: Int): FuncNode {
        return funcMap[id] ?: addFuncNode(FuncNode(id))
    }

    fun removeSummaryEdges(){
        actualIns.forEach {
            it.summaryEdges = null
        }
    }
}

/**
 * A basic node that has connections to neighboring nodes, from init to exit
 */
open class Node(
        val id: Int,
        /**
         * Neighbors with edges in exit to init direction
         */
        val neighbors: Col<Node>,
        /**
         * Custom data
         */
        var data: Any? = null) {
    fun add(vararg args: Node) {
        neighbors.addAll(args)
    }

    open fun outgoing(hideCallGraph: Boolean = false): List<Node> = neighbors.toList()

    fun <T : Node> reachable(next: (T) -> Collection<T>): Set<T> {
        val queue: ArrayDeque<T> = ArrayDeque()
        val seen: MutableSet<T> = HashSet()
        queue.add(this as T)
        while (!queue.isEmpty()) {
            val head = queue.pop()
            seen.add(head)
            queue.addAll(next(head).filter { !seen.contains(it) })
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
        /**
         * Owning function node
         */
        val owner: FuncNode,
        /**
         * Target function node
         */
        val targets: Collection<FuncNode>) : Node(id, neighbors) {

    override fun outgoing(hideCallGraph: Boolean): List<Node> = (if (hideCallGraph) targets else targets + owner) + neighbors

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
        val formalOuts: Col<OutNode> = mutableListOf()) : Node(id, neighbors as Col<Node>) {

    override fun outgoing(hideCallGraph: Boolean): List<Node> = super.outgoing(hideCallGraph) +
            (if (hideCallGraph) emptyList() else callers + callees) + formalIns + formalOuts

    fun reachableFuncNodes(): Set<FuncNode> = reachable { it.callees.flatMap { cal -> cal.targets } }
}

open class InNode(id: Int, neighbors: Col<Node> = mutableListOf(), var summaryEdges: Col<OutNode>? = null) : Node(id, neighbors) {
    override fun outgoing(hideCallGraph: Boolean): List<Node> = super.outgoing(hideCallGraph) + (summaryEdges ?: Collections.emptyList())
}

// nxm mapping (n ≠ m, but no other relation) for actual ins to formal in

class ActualInNode(id: Int, var callNode: CallNode? = null, neighbors: Col<Node> = mutableListOf(), val formalIns: MutableMap<FuncNode, FormalInNode> = mutableMapOf()) : InNode(id, neighbors) {
    override fun outgoing(hideCallGraph: Boolean): List<Node> = super.outgoing(hideCallGraph) + ((callNode?.let { listOf(it) }) ?: listOf())
}

class FormalInNode(id: Int, val funcNode: FuncNode, neighbors: Col<Node> = mutableListOf(), val actualIns: MutableMap<CallNode, ActualInNode> = mutableMapOf()) : InNode(id, neighbors)

open class OutNode(id: Int, neighbors: Col<Node> = mutableListOf()) : Node(id, neighbors)

class FormalOutNode(id: Int, neighbors: Col<Node> = mutableListOf(), val actualOuts: MutableMap<CallNode, OutNode> = mutableMapOf()) : OutNode(id, neighbors)