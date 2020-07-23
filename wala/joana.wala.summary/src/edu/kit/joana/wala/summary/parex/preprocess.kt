package edu.kit.joana.wala.summary.parex

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.stream.IntStream
import java.util.stream.Stream

/**
 * Alter the neighbor field of all nodes belonging to this graph to
 * exclude normal nodes (all nodes that do not belong to a subclass) from
 * the graph, except a few nodes that should be kept
 */
@JvmOverloads
fun Graph.removeNormalNodes(parallel: Boolean = false, executor: ExecutorService? = null, alter: Boolean = false, keep: Set<Node>? = null){
    val nodes = callGraph.vertexSet()
    val processNodes = { stream: Stream<Node?> ->
        stream.forEach {
            if (it != null && it.javaClass == Node::class.java && keep?.contains(it) != false){
                (this.nodes as MutableList)[it.id] = null
            }
        }
    }
    if (parallel){
        (executor ?: Executors.newWorkStealingPool()).submit {
            nodes.parallelStream().forEach { removeNormalNodes(it, alter, keep) }
            processNodes(this.nodes.parallelStream())
        }.get()
    } else {
        nodes.stream().forEach {
            removeNormalNodes(it, alter, keep)
            processNodes(this.nodes.stream())
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
        queue.push(cur.let {
            when (it){
                is FuncNode -> it.curNeighbors() + it.formalIns
                else -> it.outgoing(true)
            }}.filter { it !is FuncNode })
    }
}

/**
 * Run removeUnusedNodes afterwards, modifies this (don't use it)
 */
fun Graph.setEntry(entry: Int): Graph {
    assert(nodes[entry] is FuncNode)
    (nodes[entry] as FuncNode).callers.clear()
    return Graph(nodes[entry] as FuncNode, nodes, actualIns, formalIns, funcMap, callGraph);
}

/**
 * Removes the passed nodes, alters the passed graph, use the returned graph instead
 */
@JvmOverloads
fun Graph.removeNodes(rmNodes: Set<Node>, parallel: Boolean = false, executor: ExecutorService? = null): Graph {
    assert(!rmNodes.contains(entry))
    val funcs = callGraph.vertexSet().filter { !rmNodes.contains(it) }
    val processor = { fs: Stream<FuncNode> ->
        fs.forEach { f: FuncNode ->
            val proc = { rmNodes: Set<Node> ->
                (f.reachable { it: Node ->
                    it.outgoing(true)
                            .filter { it !is FuncNode && !rmNodes.contains(it) }
                }).flatMap { n -> rmNodes.mapNotNull { n.removeNode(it) } }.toSet()
            }
            var res = proc(rmNodes)
            while (res.isNotEmpty()){
                nodes = nodes.map {
                    if (res.contains(it)){
                        null
                    } else {
                        it
                    }
                }
                res = proc(res)
            }
        }
    }
    if (parallel) {
        (executor ?: Executors.newWorkStealingPool()).submit {
            processor(funcs.parallelStream())
        }.get()
    } else {
        processor(funcs.stream())
    }
    val newNodes = nodes.map {
        if (rmNodes.contains(it)){
            null
        } else {
            it
        }
    }
    val newActualIns = actualIns.filter { !rmNodes.contains(it) }.toMutableList()
    val newFormalIns = formalIns.filter { !rmNodes.contains(it) }.toMutableList()
    val newFuncMap = funcMap.filter { (_, v) -> !rmNodes.contains(v) }.toMutableMap()
    rmNodes.forEach {
        when (it) {
            is FuncNode -> {
                callGraph.removeVertex(it)
            }
        }
    }
    return Graph(entry, newNodes, newActualIns, newFormalIns, newFuncMap, callGraph)
}

@JvmOverloads
fun Graph.findUnreachableNodes(remove_acts: Boolean = false): Set<Node> {
    val reachable = entry.reachable { it: Node ->
        when (it){
            is FuncNode -> it.curNeighbors() + it.callees + it.formalIns + it.formalOuts
            else -> it.outgoing(false)
        }
    }
    val checkActualInNode = {act: ActualInNode ->
        act.callNode?.owner?.callees?.contains(act.callNode!!) ?: true
    }
    return nodes.filterNotNull().filter { !reachable.contains(it) && (remove_acts || it !is ActualInNode || checkActualInNode(it))}.toSet()
}

@JvmOverloads
fun Graph.removeUnreachableNodes(remove_acts: Boolean = false, parallel: Boolean = false, executor: ExecutorService? = null, first: Boolean = true): Graph {
    val unreachable = findUnreachableNodes(remove_acts)
    if (unreachable.isNotEmpty()){
        var ret = removeNodes(unreachable, parallel, executor).removeUnreachableNodes(remove_acts, parallel, executor)
        if (first){
            for (node in 1..5) {
                ret = ret.removeUnreachableNodes(remove_acts, parallel, executor, false)
            }
        }
        return ret
    }
    return this
}


/**
 * Remove all reference to the passed node in this node
 *
 * @return node that should also be removed because its invariants no longer hold
 */
fun Node.removeNode(nodeToRemove: Node): Node? {
    neighbors.remove(nodeToRemove)
    reducedNeighbors?.remove(nodeToRemove)

    fun <T : Node> rem(vararg nodes: MutableCollection<in T>) {
        nodes.forEach { it.remove(nodeToRemove) }
    }

    fun <T : Node, T2 : Node> rem(nodes: MutableMap<in T, in T2>) {
        nodes.remove(nodeToRemove)
        nodes.entries.filter { (_, v) -> v == nodeToRemove }.forEach { nodes.remove(it) }
    }

    when (this) {
        is CallNode -> {
            rem(actualIns, actualOuts, targets)
            if (owner == nodeToRemove || targets.isEmpty()){
                return this
            }
        }
        is FuncNode -> {
            listOf(callers, callees, formalIns, formalOuts).forEach { rem(it) }
        }
        is ActualInNode -> {
            rem(formalIns)
            if (callNode == nodeToRemove){
                return this
            }
        }
        is FormalInNode -> {
            rem(actualIns)
        }
        is FormalOutNode -> {
            rem(actualOuts)
        }
    }
    return null
}

private fun findNonNormalNeighborNodes(node: Node, keep: Set<Node>? = null): MutableList<Node> {
    val queue = NodeQueue<Node>()
    queue.addAlreadySeen(node)
    queue.push(node.neighbors)
    val result = mutableListOf<Node>()
    while (queue.isNotEmpty()) {
        val cur = queue.poll()
        if (cur.javaClass == Node::class.java && (keep == null || keep.contains(cur))) {
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