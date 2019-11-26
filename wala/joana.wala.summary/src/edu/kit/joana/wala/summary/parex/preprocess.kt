package edu.kit.joana.wala.summary.parex

import java.util.*

/**
 * Alter the neighbor field of all nodes belonging to this graph to
 * exclude normal nodes (all nodes that do not belong to a subclass) from
 * the graph, except a few nodes that should be kept
 */
@JvmOverloads
fun Graph.removeNormalNodes(parallel: Boolean = true, keep: Set<Node>? = null){
    val nodes = callGraph.vertexSet()
    (if (parallel) nodes.stream() else nodes.parallelStream()).forEach { removeNormalNodes(it, keep) }
}

/**
 * Alter the neighbor field of all nodes belonging to this function to
 * exclude normal nodes (all nodes that do not belong to a subclass) from
 * the graph, except a few nodes that should be kept
 */
fun removeNormalNodes(funcNode: FuncNode, keep: Set<Node>? = null){
    val alreadySeen = mutableSetOf<Node>()
    val queue = ArrayDeque<Node>()
    queue.add(funcNode)
    while (queue.isNotEmpty()){
        val cur = queue.first
        val nonNormals = findNonNormalNeighborNodes(cur, keep)
        cur.neighbors = nonNormals
        nonNormals.filter(alreadySeen::add).forEach { queue.add(it) }
    }
}

private fun findNonNormalNeighborNodes(node: Node, keep: Set<Node>? = null): MutableList<Node> {
    val alreadySeen = mutableSetOf<Node>()
    val queue = ArrayDeque<Node>()
    val result = mutableListOf<Node>()
    queue.add(node)
    while (queue.isNotEmpty()){
        val cur = queue.first
        if (cur.javaClass == Node::class.java && (keep == null || keep.contains(cur))){
            cur.neighbors.filter(alreadySeen::add).forEach { queue.add(it) }
        } else {
            result.add(cur)
        }
    }
    return result
}