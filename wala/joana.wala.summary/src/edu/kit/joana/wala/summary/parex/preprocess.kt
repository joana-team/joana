package edu.kit.joana.wala.summary.parex

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Alter the neighbor field of all nodes belonging to this graph to
 * exclude normal nodes (all nodes that do not belong to a subclass) from
 * the graph, except a few nodes that should be kept
 */
@JvmOverloads
fun Graph.removeNormalNodes(parallel: Boolean = false, executor: ExecutorService? = null, alter: Boolean = false, keep: Set<Node>? = null){
    val nodes = callGraph.vertexSet()
    if (parallel){
        (executor ?: Executors.newWorkStealingPool()).submit {
            nodes.parallelStream().forEach { removeNormalNodes(it, alter, keep) }
        }.get()
    } else {
        nodes.stream().forEach {
            removeNormalNodes(it, alter, keep)
        }
    }
}

/**
 * Alter the neighbor field of all nodes belonging to this function to
 * exclude normal nodes (all nodes that do not belong to a subclass) from
 * the graph, except a few nodes that should be kept
 */
fun removeNormalNodes(funcNode: FuncNode, alter: Boolean = false, keep: Set<Node>? = null){
    val queue = NodeQueue<Node>(funcNode)
    while (queue.isNotEmpty()){
        val cur = queue.poll()
        val nonNormals = findNonNormalNeighborNodes(cur, keep)
        if (alter) {
            cur.neighbors = nonNormals
        } else {
            cur.reducedNeighbors = nonNormals
        }
        queue.push(nonNormals)
    }
}

private fun findNonNormalNeighborNodes(node: Node, keep: Set<Node>? = null): MutableList<Node> {
    val queue = NodeQueue<Node>()
    queue.addAlreadySeen(node)
    queue.push(node.neighbors)
    val result = mutableListOf<Node>()
    while (queue.isNotEmpty()){
        val cur = queue.poll()
        if (cur.javaClass == Node::class.java && (keep == null || keep.contains(cur))){
            queue.push(cur.neighbors)
        } else {
            result.add(cur)
        }
    }
    return result
}

/**
 * Assert that reducedNeighbors contains only non normal nodes
 */
fun assertValidity(graph: Graph) {
    val res = graph.getAllNodes().filter { n ->
        n.reducedNeighbors?.any { it.javaClass == Node::class.java } ?: false
    }.joinToString("\n") { n -> "$n -> ${n.curNeighbors()}" }
    if (res.isNotEmpty()){
        throw AssertionError(res)
    }
}