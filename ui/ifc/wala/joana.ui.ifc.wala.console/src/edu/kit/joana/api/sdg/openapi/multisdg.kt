/**
 * Idea:
 * The OpenApi component based code deals with multiple SDGs that are connected to each other.
 */

package edu.kit.joana.api.sdg.openapi

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.Iterables
import com.ibm.wala.classLoader.IMethod
import com.ibm.wala.ipa.callgraph.CGNode
import com.ibm.wala.types.ClassLoaderReference
import com.ibm.wala.types.TypeReference
import edu.kit.joana.api.sdg.SDGBuildPreparation
import edu.kit.joana.api.sdg.SDGConfig
import edu.kit.joana.ifc.sdg.graph.SDG
import edu.kit.joana.ifc.sdg.graph.SDG.FormalSummaryEdgeMap
import edu.kit.joana.ifc.sdg.graph.SDGEdge
import edu.kit.joana.ifc.sdg.graph.SDGNode
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind.EXIT
import edu.kit.joana.ui.annotations.openapi.InvokeAllRegisteredOpenApiServerMethods
import edu.kit.joana.ui.annotations.openapi.ModifiedOpenApiClientMethod
import edu.kit.joana.ui.annotations.openapi.RegisteredOpenApiServerMethod
import edu.kit.joana.ui.ifc.wala.console.console.IFCConsole
import edu.kit.joana.ui.ifc.wala.console.console.ImprovedCLI
import edu.kit.joana.util.Iterators
import edu.kit.joana.util.TypeNameUtils
import edu.kit.joana.wala.core.CallGraph
import edu.kit.joana.wala.core.SDGBuilder
import edu.kit.joana.wala.core.SDGBuilder.SDGSummaryAdder
import edu.kit.joana.wala.core.openapi.OpenApiClientDetector
import edu.kit.joana.wala.core.openapi.OpenApiServerDetector
import edu.kit.joana.wala.core.openapi.OperationId
import org.jline.utils.Colors.s
import org.junit.jupiter.api.fail
import org.objectweb.asm.Type
import java.sql.DriverManager.getConnection
import java.util.Optional
import java.util.Queue
import java.util.Stack
import java.util.stream.Collectors

/**
 * Stores the supported operation ids (either server or client) and call graph id
 */
data class MappedOperationIds(val map: BiMap<OperationId, Int> = HashBiMap.create()) : BiMap<OperationId, Int> by map {

    val cgIds: Set<Int>
        get() = values
}

typealias EdgePairs = Set<edu.kit.joana.util.Pair<SDGNode, SDGNode>>

data class Connection(
    val clientEntryNode: SDGNode,
    val serverEntryNode: SDGNode,
    val serverNodeToClientNode: Map<SDGNode, SDGNode>
) {

    fun getClientNode(serverNode: SDGNode) = serverNodeToClientNode[serverNode]
}

fun SDGNode.paramNumber(): Int? {
    if (kind == EXIT) {
        return -2
    }
    if (label == "this") {
        return -1
    }
    if (label.startsWith("param ")) {
        return label.split(" ")[1].toInt()
    }
    return null
}

fun Set<SDGNode>.getParameterNodes(): Map<Int, SDGNode> = filter { it.paramNumber() != null }
    .associateBy { it.paramNumber()!! }

val SDGNode.isMemory get() = label.contains("|UNIQ") || label.contains("|MERGE")

val SDGNode.memoryVariable get() = label.split('|', limit = 3)[2].split("(", ")")[1]

val SDGNode.isMemoryVariable
    get() = isMemory && label.contains("(") && "([])" !in label

fun SDG.getExceptionNode(entry: SDGNode): SDGNode? = getFormalOutsOfProcedure(entry).firstOrNull { it.isException }

fun SDG.hasExceptionNode(entry: SDGNode) = getExceptionNode(entry) != null

fun com.ibm.wala.ipa.callgraph.CallGraph.getTransitiveCallersAndSelf(node: CGNode): Set<CGNode> {
    val nodes = mutableSetOf<CGNode>()
    val queue: Queue<CGNode> = java.util.ArrayDeque()
    queue.offer(node)
    while (queue.isNotEmpty()) {
        val cur = queue.poll()
        if (cur !in nodes) {
            nodes.add(cur)
            getPredNodes(cur).forEach { queue.add(it) }
        }
    }
    return nodes
}

class ExSDG(val sdg: SDG, val callGraph: CallGraph) {
    fun getCGNode(entry: SDGNode) = callGraph.orig.getNode(sdg.getCGNodeId(entry))

    fun getCGNodesUp(entry: SDGNode) = callGraph.orig.getTransitiveCallersAndSelf(getCGNode(entry))

    fun getCallingCGNodes(cgNode: CGNode) = Iterators.stream(callGraph.orig.getPredNodes(cgNode)).collect(Collectors.toSet())

    fun getCallSites(entry: SDGNode) = sdg.getCallers(entry).map { it to getCGNode(sdg.getEntry(it)) }

    fun getEntryNode(cgNode: CGNode) = sdg.getSDGNode(cgNode.graphNodeId)
}

/**
 * Node in the multi SDG graph, referencing
 */
data class MultiSDGNode(
    val name: String,
    private val sdgAdder: SDGSummaryAdder,
    val server: MappedOperationIds,
    val client: MappedOperationIds,
    /** problem: summary edges are set in the calling methods */
    val allInvokeNodeIds: Set<Int>
) {

    /** clientEntryNode ⊗ serverEntryNode ↦ Connection */
    private val clientServerConnections = mutableMapOf<Pair<SDGNode, SDGNode>, Connection>()

    val sdg: SDG
        get() = sdgAdder.sdg

    val callGraph: CallGraph
        get() = sdgAdder.builder.callGraph

    val exSDG = ExSDG(sdg, callGraph)

    val propagation = Propagation(exSDG)

    override fun toString() = name

    /** called on the client SDG for a specific API connection */
    fun getConnection(clientEntryNode: SDGNode, serverSDG: SDG, serverEntryNode: SDGNode) = clientServerConnections.getOrPut(Pair(clientEntryNode, serverEntryNode)) {
        createConnection(
            clientEntryNode,
            serverSDG,
            serverEntryNode
        )
    }

    /**
     * called on the client SDG for a specific API connection
     *
     * @param clientEntryNode the entry node of the client method (e.g. the Api.bla method for ApiImpl.bla)
     */
    fun createConnection(clientEntryNode: SDGNode, serverSDG: SDG, serverEntryNode: SDGNode): Connection {
        // TODO
        //   consider the memory formal ins and outs of server methods → add new nodes to all methods up the call tree

        data class MatchResult(
            /** client to server node */
            val matching: Map<SDGNode, SDGNode>,
            val unmatchedClientNodes: Set<SDGNode>,
            val unmatchedServerNodes: Set<SDGNode>
        )

        fun match(accessor: (SDG, SDGNode) -> Set<SDGNode>): MatchResult {
            val clientNodes = accessor(sdg, clientEntryNode)
            val serverNodes = accessor(serverSDG, serverEntryNode)
            val matches = mutableMapOf<SDGNode, SDGNode>()

            fun addMatch(clientNode: SDGNode, serverNode: SDGNode) {
                println("      match ${clientNode.label} → ${serverNode.label}")
                matches[clientNode] = serverNode
            }

            val clientParameters = clientNodes.getParameterNodes()
            val serverParameters = serverNodes.getParameterNodes()

            serverNodes.forEach { serverNode ->
                val paramNumber = serverNode.paramNumber()
                if (paramNumber != null) {
                    addMatch(clientParameters[paramNumber]!!, serverNode)
                }
            }
            return MatchResult(
                matches, clientNodes.filter { it !in matches }.toSet(),
                matches.values.toSet().let { set -> serverNodes.filter { it !in set }.toSet() }
            )
        }

        data class EqualPairs(
            /** (name, fi, fo) */
            val pairs: List<Triple<String, SDGNode, SDGNode>>,
            val unmatchedFis: Set<SDGNode>,
            val unmatchedFos: Set<SDGNode>,
            /** variable name to node */
            val unmatchedMemoryFis: Map<String, SDGNode>,
            /** variable name to node */
            val unmatchedMemoryFos: Map<String, SDGNode>,
            /** always a Fo but unmatched */ val exception: SDGNode?
        )

        fun toPreprocessedLabelMap(nodes: Iterable<SDGNode>) = nodes.associateBy {
            return@associateBy if (it.isMemoryVariable) {
                it.memoryVariable
            } else {
                it.label
            }
        }

        /* * matched based on label */
        fun matchNodes(fis: Iterable<SDGNode>, fos: Iterable<SDGNode>): EqualPairs {
            val fiLabelMap = toPreprocessedLabelMap(fis)
            val foLabelMap = toPreprocessedLabelMap(fos)
            val fisUnmatched = mutableSetOf<SDGNode>()

            var exception: SDGNode? = null
            val fosUnmatched = fos.filter {
                if (it.isException) {
                    assert(exception == null)
                    exception = it
                    false
                } else {
                    true
                }
            }.toMutableSet()
            val pairs = fiLabelMap.map { entry ->
                foLabelMap[entry.key]?.let {
                    fosUnmatched.remove(it)
                    return@map Triple(entry.key, entry.value, it)
                } ?: run {
                    fisUnmatched.add(entry.value)
                    null
                }
            }.filterNotNull()

            fun removeAndStoreMem(unmatched: MutableSet<SDGNode>): Map<String, SDGNode> {
                val res = mutableMapOf<String, SDGNode>()
                unmatched.removeIf {
                    if (it.isMemoryVariable) {
                        res[it.memoryVariable] = it
                        true
                    } else {
                        false
                    }
                }
                return res
            }

            val unmatchedMemoryFis = removeAndStoreMem(fisUnmatched)
            val unmatchedMemoryFos = removeAndStoreMem(fosUnmatched)

            return EqualPairs(
                pairs, fisUnmatched, fosUnmatched, unmatchedMemoryFis, unmatchedMemoryFos, exception
            )
        }

        val fiMatches = match { sdg, sdgNode -> sdg.getFormalInsOfProcedure(sdgNode) }
        assert(fiMatches.unmatchedClientNodes.isEmpty())
        val foMatches = match { sdg, sdgNode -> sdg.getFormalOutsOfProcedure(sdgNode) }
        assert(foMatches.unmatchedClientNodes.all { it.isException })

        val matchedServerFiFos = matchNodes(fiMatches.unmatchedServerNodes, foMatches.unmatchedServerNodes)

        matchedServerFiFos.pairs.forEach { (x, y, z) -> println("    unmatched pair (${y.label}, ${z.label})") }
        matchedServerFiFos.unmatchedFis.forEach { x -> println("      unmatched single fi ${x.label}") }
        matchedServerFiFos.unmatchedFos.forEach { x -> println("      unmatched single fo ${x.label}") }
        matchedServerFiFos.unmatchedMemoryFis.values.forEach { x -> println("      unmatched single memory fi ${x.label}") }
        matchedServerFiFos.unmatchedMemoryFos.values.forEach { x -> println("      unmatched single memory fo ${x.label}") }

        val variablesToPropagate = (matchedServerFiFos.unmatchedMemoryFis.keys + matchedServerFiFos.unmatchedMemoryFos.keys + matchedServerFiFos.pairs.filter { it.second.isMemoryVariable }.map { it.second.memoryVariable }.toSet())
        println("        variables to propagate $variablesToPropagate")

        propagation.propagateSDGNodesForVariables(clientEntryNode, variablesToPropagate, matchedServerFiFos.exception != null)

        val mapping = mutableMapOf<SDGNode, SDGNode>() // server node → client node
        fiMatches.matching.forEach { (c, s) -> mapping[s] = c }
        foMatches.matching.forEach { (c, s) -> mapping[s] = c }
        serverSDG.getExceptionNode(serverEntryNode)?.let { ex ->
            mapping[ex] = sdg.getExceptionNode(clientEntryNode)!!
        }
        // now add the newly created
        fiMatches.unmatchedServerNodes.forEach { node ->
            if (node.isMemoryVariable) {
                propagation.getFiNode(sdg, clientEntryNode, node.memoryVariable)?.let {
                    mapping[node] = it
                }
            }
        }
        foMatches.unmatchedServerNodes.forEach { node ->
            if (node.isMemoryVariable) {
                propagation.getFoNode(sdg, clientEntryNode, node.memoryVariable)?.let {
                    mapping[node] = it
                }
            }
        }

        /**
         * Idea:
         * create
         */

        return Connection(clientEntryNode, serverEntryNode, mapping)
    }

    fun compute(changedCgIds: Set<Int>?): SDGSummaryAdder.Result {
        return sdgAdder.compute(
            allInvokeNodeIds,
            Optional.ofNullable(if (changedCgIds.isNullOrEmpty()) null else changedCgIds)
        ).also {
         /*   GraphViewer.launch(sdg)
            readLine()*/
        }
    }

    /** add summary edges */
    fun addEdges(clientCgId: Int, clientEntryNode: SDGNode, serverSDG: SDG, serverEntryNode: SDGNode, serverEdges: EdgePairs) {
        val connection = getConnection(clientEntryNode, serverSDG, serverEntryNode)
        serverEdges.forEach { (fi, fo) ->
            connection.getClientNode(fi)?.let { ai ->
                connection.getClientNode(fo)?.let { ao ->
                    sdg.addEdge(SDGEdge.Kind.SUMMARY.newEdge(ai, ao))
                    println("    add edge: $serverEntryNode ${serverEntryNode.label}:   $ai ${ai.label} → $ao ${ao.label}")
                }
            }
        }
    }
    companion object {

        @JvmStatic
        fun create(name: String, builder: SDGBuilder.SDGSummaryBuilder): MultiSDGNode {
            val clientDetector = OpenApiClientDetector()
            val serverDetector = OpenApiServerDetector()

            val clientMap: BiMap<OperationId, Int> = HashBiMap.create()
            val serverMap: BiMap<OperationId, Int> = HashBiMap.create()
            builder.callGraph.getMethodsWithAnnotation(Type.getType(ModifiedOpenApiClientMethod::class.java))
                .forEach { (node, method) -> clientMap[clientDetector.getOperationId(method)] = node.id }
            builder.callGraph.getMethodsWithAnnotation(Type.getType(RegisteredOpenApiServerMethod::class.java))
                .forEach { (node, method) ->
                    serverMap[serverDetector.getOperationId(serverDetector.getImplementingServerMethod(method).get())] = node.id
                }
            val allInvokeNodeIds = builder.callGraph.getMethodsWithAnnotation(Type.getType(InvokeAllRegisteredOpenApiServerMethods::class.java))
                .map { (node, _) -> node.id }.toSet()
            return MultiSDGNode(
                name,
                SDGSummaryAdder(builder, serverMap.values),
                MappedOperationIds(serverMap), MappedOperationIds(clientMap),
                allInvokeNodeIds
            )
        }
    }
}

fun CallGraph.getMethodsWithAnnotation(type: Type): List<Pair<CallGraph.Node, IMethod>> {
    val typeRef = TypeReference.findOrCreate(
        ClassLoaderReference.Application,
        TypeNameUtils.removeSemicolon(type.toString())
    )
    return vertexSet().map { n -> Pair(n, n.node.method) }.filter { (_, m) -> m.annotations.any { a -> a.type == typeRef } }
}

data class IFCConsoleWithMore(val name: String, val ifcConsole: IFCConsole, val miscCommands: List<String>) {
    fun runMiscCommands() {
        ImprovedCLI.process(Iterables.toArray(miscCommands, String::class.java), ifcConsole.createWrapper())
    }
}

fun IFCConsole.process(commands: Iterable<String>) =
    ImprovedCLI.process(Iterables.toArray(commands, String::class.java), createWrapper())

fun Iterable<String>.toIFCConsole(): IFCConsole {
    val ifcConsole = IFCConsole.forConsole(false)
    if (!ifcConsole.process(this)) {
        fail("Error for $this")
    }
    return ifcConsole
}

fun String.toIFCConsole(): IFCConsole = this.split("^['\"];").toIFCConsole()

fun Iterable<String>.toIFCConsoles(): List<IFCConsole> = map { toIFCConsole() }

/** split string when the predicate returns true */
fun String.split(predicate: (Char) -> Boolean): List<String> {
    val ret: MutableList<String> = ArrayList()
    val last: MutableList<Char> = ArrayList()
    forEach {
        if (predicate(it)) {
            ret.add(last.joinToString(""))
            last.clear()
        } else {
            last.add(it)
        }
    }
    ret.add(last.joinToString(""))
    return ret
}

/**
 * split the string into commands:
 * Commands are strings that might contain strings wrapped in quotation marks and escaped chars
 */
fun String.splitIntoCommands(quotationMarks: Set<Char> = setOf('"', '\''), separator: Char = ';'): List<String> {
    val stack: Stack<Char> = Stack()
    var escapeOn = false
    return split {
        if (escapeOn) {
            escapeOn = false
            return@split false
        }
        when (it) {
            '\\' -> escapeOn = true
            in quotationMarks -> {
                if (stack.lastOrNull() == it) {
                    stack.pop()
                } else {
                    stack.push(it)
                }
            }
            separator -> {
                if (stack.isEmpty()) {
                    return@split true
                }
            }
        }
        return@split false
    }
}

fun String.toIFCConsoleWithMore(name: String, ignoreDots: Boolean = false): IFCConsoleWithMore {
    val parts = this.splitIntoCommands().map { it.trim() }
    if ("..." in parts) {
        assert(parts.count { it == "..." } == 1)
        val mid = parts.indexOf("...")
        if (ignoreDots) {
            return IFCConsoleWithMore(name, (parts.subList(0, mid) + parts.subList(mid + 1, parts.size)).toIFCConsole(), emptyList())
        }
        return IFCConsoleWithMore(name, parts.subList(0, mid).toIFCConsole(), parts.subList(mid + 1, parts.size))
    }
    return IFCConsoleWithMore(name, parts.toIFCConsole(), emptyList())
}

/** runs the commands that should be run before the "..." (or all commands with ignoreDots=true),
 * uneven ones are the descriptions of even ones*/
fun Iterable<String>.toIFCConsoleWithMore(ignoreDots: Boolean = false, parallel: Boolean = true): List<IFCConsoleWithMore> {
    return (if (parallel) toList().chunked(2).parallelStream() else toList().chunked(2).stream()).map {
        val name = it[0]!!
        val command = it[1]!!
        println(name)
        println("-".repeat(50))
        command.toIFCConsoleWithMore(name, ignoreDots)
    }.collect(Collectors.toList())
}
fun IFCConsole.printSeparator() {
    println(classPath)
    println("-".repeat(50))
}

fun Iterable<IFCConsole>.run(tag: String? = null) {
    for (ifcConsole in this) {
        ifcConsole.printSeparator()
        val wrapper = ifcConsole.createWrapper()
        wrapper.addAnnotations(tag ?: "")
        wrapper.run(ifcConsole.AnalysisObject("-"))
    }
}

fun Iterable<IFCConsoleWithMore>.toMultiSDGComputer() = MultiSDGComputer.create(map { it.name to it.ifcConsole })

fun Iterable<IFCConsoleWithMore>.runWithMultiSDGComputatation() {
    toMultiSDGComputer().compute()
    for ((name, ifcConsole, miscCommands) in iterator()) {
        ifcConsole.printSeparator()
        if (!ifcConsole.process(miscCommands)) {
            fail("Error for $name")
        }
    }
}

private data class MiscSDGProperties(
    val ifcConsole: IFCConsole,
    val intermediateSDGSummaryBuilder: SDGBuildPreparation.IntermediateSDGSummaryBuilder,
    val sdgConfig: SDGConfig
)

class MultiSDGComputer private constructor(private val nodes: Map<MultiSDGNode, MiscSDGProperties>) {

    companion object {
        @JvmStatic
        fun create(ifcConsoles: List<Pair<String, IFCConsole>>): MultiSDGComputer {
            return MultiSDGComputer(
                ifcConsoles.associate { (name, ifcConsole) ->
                    ifcConsole.createSDGBuilder().get().let {
                        MultiSDGNode.create(name, it.first.builder) to MiscSDGProperties(ifcConsole, it.first, it.second)
                    }
                }
            )
        }
    }

    fun getServers(operationId: OperationId) = nodes.keys.filter { operationId in it.server }
    private fun getClients(operationId: OperationId) = nodes.keys.filter { operationId in it.client }

    fun pushEdges(
        node: MultiSDGNode,
        difference: FormalSummaryEdgeMap,
        func: (
            clientMultiNode: MultiSDGNode,
            clientCgId: Int,
            clientEntryNode: SDGNode,
            serverEntryNode: SDGNode,
            serverEdges: EdgePairs
        ) -> Unit
    ) {
        difference.map.forEach { (entryNode, edges) ->
            val cgId = node.sdg.getCGNodeId(entryNode)
            val operation = node.server.inverse()[cgId]!!
            getClients(operation).forEach {
                println("------ clientEntryNode = ${it.sdg.getSDGNode(it.client[operation]!!).label}")
                func(it, it.client[operation]!!, it.sdg.getSDGNode(it.client[operation]!!), entryNode, edges)
            }
        }
    }

    fun compute(): List<IFCConsole> {

        data class WorklistEntry(val node: MultiSDGNode, var affectedCgIds: MutableSet<Int> = mutableSetOf())

        val alreadyConsidered = HashMap<MultiSDGNode, WorklistEntry>()
        val worklist = ArrayDeque<WorklistEntry>()
        fun push(n: MultiSDGNode, affectedCgIds: Set<Int>? = null) {
            if (affectedCgIds != null && affectedCgIds.isEmpty()) {
                return
            }
            if (!alreadyConsidered.contains(n)) {
                WorklistEntry(n).let {
                    alreadyConsidered[n] = it
                    worklist.add(it)
                }
            }
            //affectedCgIds?.let { alreadyConsidered[n]!!.affectedCgIds.addAll(it) }
        }
        fun pop(): WorklistEntry {
            return worklist.removeFirst().also { alreadyConsidered.remove(it.node) }
        }
        nodes.keys.forEach(::push)
        while (!worklist.isEmpty()) {
            val (cur, affectedCgIds) = pop()
            System.err.println("\n-----> compute ${cur.name}")
            cur.compute(affectedCgIds).let { sums ->
                // setting: we found edges related to call node of an all invoke method
                // the compute method already converted them into formal in → formal out edges
                // we now have to set these edges in the client method
                // e.g. we found an edge   x → ret  in ServerImpl.func(x)  =>  we push an edge to every client

                pushEdges(cur, sums.difference()) { clientMultiNode, clientCgId, clientEntryNode, serverEntryNode, serverEdges ->
                    clientMultiNode.addEdges(clientCgId, clientEntryNode, cur.sdg, serverEntryNode, serverEdges)
                    System.err.println("\n-----> push $clientMultiNode ($clientCgId)")
                    push(clientMultiNode, setOf(clientCgId))
                }
            }
        }
        setProgramsProperly()
        return nodes.values.map { it.ifcConsole }
    }

    private fun setProgramsProperly() {
        nodes.values.forEach { (ifcConsole, sdgBuilder, sdgConfig) ->
            ifcConsole.setSDGProgramWithoutRecomputation(
                sdgBuilder.sdg,
                sdgConfig
            )
        }
    }
}
