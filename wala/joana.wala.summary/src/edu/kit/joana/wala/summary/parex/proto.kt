package edu.kit.joana.wala.summary.parex

import com.google.protobuf.GeneratedMessageV3
import edu.kit.joana.wala.summary.parex.GraphProto.NodeHeader.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

private fun Col<out Node>.mapIds(g: Graph): List<Int> {
    return map {
        g.printableId(it.id)
    }.filter { it != -1 }
}


private fun List<Pair<Int, Int>>.mapIds(graph: Graph): Map<Int, Int> {
    return map { graph.printableId(it.first) to graph.printableId(it.second) }
            .filter { it.first != -1 && it.second != -1 }.toMap()
}

/**
 * Deals with handling proto files
 */

class Dumper {

    fun dump(graph: Graph, fileName: String) {
        dump(graph, Files.newOutputStream(Paths.get(fileName)))
    }

    /**
    Idea: On the wire
    1. Dumps the graph header
    2. Dumps the function ids
    3. Dumps the node headers
    4. Dumps the body of each individual node
     */
    fun dump(graph: Graph, out: OutputStream) {

        val dataOut = DataOutputStream(out)

        dumpHeader(graph).writeDelimitedTo(out)

        // dump function ids
        graph.forEachNodeInPrintOrder { n ->
            if (n is FuncNode) {
                dataOut.writeInt(graph.printableId(n.id))
            }
        }

        // dump header
        graph.forEachNodeInPrintOrder { n ->
            dumpHeader(graph, n, dataOut)
        }

        // dump bodies
        graph.forEachNodeInPrintOrder { n ->
            dumpNodeType(n, dataOut)
            dumpBody(graph, n)?.writeDelimitedTo(out)
        }
    }

    internal fun dumpHeader(graph: Graph): GraphProto.GraphHeader {
        return GraphProto.GraphHeader.newBuilder()
                .setEntry(graph.printableId(graph.entry.id))
                .setNumberOfNodes(graph.numberOfIds())
                .setNumberOfFunctions(graph.funcMap.size)
                .setNodeHeaderBytes(graph.nodesInPrintOrder().parallelStream().mapToInt { n -> if (n is CallNode || n is FormalInNode) 2 else 1 }.sum() * 4)
                .build()
    }

    internal fun dumpHeader(graph: Graph, node: Node?, output: DataOutputStream) {
        dumpNodeType(node, output)
        if (node is CallNode) {
            output.writeInt(graph.printableId(node.owner.id))
        }
        if (node is FormalInNode) {
            output.writeInt(graph.printableId(node.owner.id))
        }
    }

    internal fun dumpNodeType(node: Node?, output: DataOutputStream) {
        output.writeInt(when (node) {
            null -> NONE_VALUE
            is CallNode -> CALL_NODE_VALUE
            is FuncNode -> FUNC_NODE_VALUE
            is ActualInNode -> ACTUAL_IN_NODE_VALUE
            is FormalInNode -> FORMAL_IN_NODE_VALUE
            is FormalOutNode -> FORMAL_OUT_NODE_VALUE
            is OutNode -> ACTUAL_OUT_NODE_VALUE
            else -> NORMAL_NODE_VALUE
        })
    }

    internal fun dumpBody(graph: Graph, node: Node?): GeneratedMessageV3? {
        return when (node) {
            null -> null
            is CallNode ->
                GraphProto.CallNode.newBuilder()
                        .addAllActualIns(node.actualIns.mapIds(graph))
                        .addAllActualOuts(node.actualOuts.mapIds(graph))
                        .addAllTargets(node.targets.map(Node::id).map(graph::printableId))
                        .addAllNeighbors(node.neighbors.mapIds(graph))
                        .setOwner(graph.printableId(node.owner.id))
                        .build()
            is FuncNode ->
                GraphProto.FuncNode.newBuilder()
                        .addAllFormalIns(node.formalIns.mapIds(graph))
                        .addAllFormalOuts(node.formalOuts.mapIds(graph))
                        .addAllCallees(node.callees.mapIds(graph))
                        .addAllCallers(node.callers.mapIds(graph))
                        .addAllNeighbors(node.neighbors.mapIds(graph))
                        .build()
            is ActualInNode ->
                GraphProto.ActualInNode.newBuilder()
                        .putAllFormalIns(node.formalIns.entries.map { it.key.id to it.value.id }.mapIds(graph))
                        .setCallNode(if (node.callNode != null) graph.printableId(node.callNode!!.id) else -1)
                        .addAllNeighbors(node.neighbors.mapIds(graph))
                        .build()
            is FormalInNode ->
                GraphProto.FormalInNode.newBuilder()
                        .putAllActualIns(node.actualIns.entries.map { it.key.id to it.value.id }.mapIds(graph))
                        .addAllNeighbors(node.neighbors.mapIds(graph))
                        .setOwner(graph.printableId(node.owner.id))
                        .build()
            is FormalOutNode ->
                GraphProto.FormalOutNode.newBuilder()
                        .putAllActualOuts(node.actualOuts.entries.map { it.key.id to it.value.id }.mapIds(graph))
                        .addAllNeighbors(node.neighbors.mapIds(graph))
                        .build()
            is OutNode ->
                GraphProto.ActualOutNode.newBuilder()
                        .addAllNeighbors(node.neighbors.mapIds(graph))
                        .build()
            else ->
                GraphProto.NormalNode.newBuilder()
                        .addAllNeighbors(node.neighbors.mapIds(graph))
                        .build()
        }
    }
}

/**
 * 1. Reads the graph
 * 2. Reads each individual node
 */
class Loader {

    fun load(fileName: String): Graph {
        return load(Files.newInputStream(Paths.get(fileName)))
    }

    fun load(input: InputStream): Graph {
        val graphHeader = GraphProto.GraphHeader.parseDelimitedFrom(input)

        val dataInput = DataInputStream(input)

        val entry = graphHeader.entry
        val nodes = Array<Node?>(graphHeader.numberOfNodes) {
            null
        }

        // load all function headers
        val funcs = loadFunctions(graphHeader.numberOfFunctions, dataInput, nodes)

        loadNodesHeaders(dataInput, nodes)

        loadNodeBodies(dataInput, nodes)

        val nodeList = listOf(*nodes)

        val graph = Graph(nodes[entry] as FuncNode, nodeList,
                nodeList.parallelStream().filter { it is ActualInNode }.map { it as ActualInNode }.collect(Collectors.toList()),
                nodeList.parallelStream().filter { it is FormalInNode }.map { it as FormalInNode }.collect(Collectors.toList()),
                funcs.map { it.id to it }.toMap())


        val calledFuncsPerFunc = funcs.parallelStream().map { f -> f to f.callees.flatMap { it.targets } }.collect(Collectors.toList())

        calledFuncsPerFunc.forEach { (func, called) ->
            with(graph.callGraph) {
                addVertex(func)
                called.forEach {
                    addVertex(it)
                    addEdge(func, it)
                }
            }
        }
        Files.newBufferedWriter(Paths.get("node_ids_pg.txt")).let { writer ->
            graph.nodes.forEach {
                if (it != null) {
                    writer.write("${it.id} " + when (it) {
                        null -> NONE_VALUE
                        is CallNode -> CALL_NODE_VALUE
                        is FuncNode -> FUNC_NODE_VALUE
                        is ActualInNode -> ACTUAL_IN_NODE_VALUE
                        is FormalInNode -> FORMAL_IN_NODE_VALUE
                        is FormalOutNode -> FORMAL_OUT_NODE_VALUE
                        is OutNode -> ACTUAL_OUT_NODE_VALUE
                        else -> NORMAL_NODE_VALUE
                    }.toString() + "\n")
                } else {
                    writer.write("\n")
                }
            }
            writer.flush()
        }
        return graph
    }

    internal fun loadFunctions(number: Int, input: DataInputStream, nodes: Array<Node?>): List<FuncNode> {
        return (0 until number).map {
            val id = input.readInt()
            FuncNode(id).also { f -> nodes[id] = f }
        }
    }

    internal fun loadNodesHeaders(input: DataInputStream, nodes: Array<Node?>) {
        for (id in nodes.indices) {
            val type = input.readInt()
            nodes[id] = when (type) {
                CALL_NODE_VALUE -> {
                    val ownerId = input.readInt()
                    CallNode(id, mutableListOf(), owner = nodes[ownerId] as FuncNode, targets = mutableListOf())
                }
                ACTUAL_IN_NODE_VALUE -> ActualInNode(id)
                ACTUAL_OUT_NODE_VALUE -> OutNode(id)
                FORMAL_IN_NODE_VALUE -> {
                    val ownerId = input.readInt()
                    FormalInNode(id, actualIns = mutableMapOf(), owner = nodes[ownerId] as FuncNode)
                }
                FORMAL_OUT_NODE_VALUE -> FormalOutNode(id, mutableListOf(), mutableMapOf())
                NORMAL_NODE_VALUE -> Node(id, mutableListOf())
                else -> nodes[id]
            }
        }
    }

    internal fun loadNodeBodies(input: DataInputStream, nodes: Array<Node?>) {
        for (id in nodes.indices) {
            input.readInt()
            val node = nodes[id]
            when (node) {
                is CallNode -> {
                    val call = GraphProto.CallNode.parseDelimitedFrom(input)
                    node.actualIns.addAll(call.actualInsList.map { nodes[it] as ActualInNode })
                    node.actualOuts.addAll(call.actualOutsList.map { nodes[it] as OutNode })
                    node.targets.addAll(call.targetsList.map { nodes[it] as FuncNode })
                    node.neighbors.addAll(call.neighborsList.map { nodes[it] as Node })
                }
                is FuncNode -> {
                    val func = GraphProto.FuncNode.parseDelimitedFrom(input)
                    node.formalIns.addAll(func.formalInsList.map { nodes[it] as FormalInNode })
                    node.formalOuts.addAll(func.formalOutsList.map { nodes[it] as FormalOutNode })
                    node.callees.addAll(func.calleesList.map { nodes[it] as CallNode })
                    node.callers.addAll(func.callersList.map { nodes[it] as CallNode })
                }
                is ActualInNode -> {
                    val actIn = GraphProto.ActualInNode.parseDelimitedFrom(input)
                    node.callNode = if (actIn.callNode < 0) null else nodes[actIn.callNode] as CallNode
                    actIn.formalInsMap.forEach { f, fi ->
                        node.formalIns[nodes[f] as FuncNode] = nodes[fi] as FormalInNode
                    }
                    node.neighbors.addAll(actIn.neighborsList.map { nodes[it] as Node })
                }
                is FormalInNode -> {
                    val formIn = GraphProto.FormalInNode.parseDelimitedFrom(input)
                    formIn.actualInsMap.forEach { c, ai ->
                        node.actualIns[nodes[c] as CallNode] = nodes[ai] as ActualInNode
                    }
                    node.neighbors.addAll(formIn.neighborsList.map { nodes[it] as Node })
                }
                is FormalOutNode -> {
                    val formOut = GraphProto.FormalOutNode.parseDelimitedFrom(input)
                    formOut.actualOutsMap.forEach { c, ao ->
                        node.actualOuts[nodes[c] as CallNode] = nodes[ao] as OutNode
                    }
                    node.neighbors.addAll(formOut.neighborsList.map { nodes[it] as Node })
                }
                is OutNode -> {
                    val actOut = GraphProto.FormalOutNode.parseDelimitedFrom(input)
                    node.neighbors.addAll(actOut.neighborsList.map { nodes[it] as Node })
                }
                is Node -> {
                    val normNode = GraphProto.NormalNode.parseDelimitedFrom(input)
                    node.neighbors.addAll(normNode.neighborsList.map { nodes[it] as Node })
                }
            }
        }
    }
}