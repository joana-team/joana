/**
 * Idea:
 * The OpenApi component based code deals with multiple SDGs that are connected to each other.
 */

package edu.kit.joana.api.sdg.openapi

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.Iterables
import com.ibm.wala.classLoader.IMethod
import com.ibm.wala.types.ClassLoaderReference
import com.ibm.wala.types.TypeReference
import edu.kit.joana.api.sdg.SDGBuildPreparation
import edu.kit.joana.api.sdg.SDGConfig
import edu.kit.joana.ifc.sdg.graph.SDG
import edu.kit.joana.ifc.sdg.graph.SDG.FormalSummaryEdgeMap
import edu.kit.joana.ifc.sdg.graph.SDGNode
import edu.kit.joana.ui.annotations.openapi.InvokeAllRegisteredOpenApiServerMethods
import edu.kit.joana.ui.annotations.openapi.ModifiedOpenApiClientMethod
import edu.kit.joana.ui.annotations.openapi.RegisteredOpenApiServerMethod
import edu.kit.joana.ui.ifc.sdg.graphviewer.GraphViewer
import edu.kit.joana.ui.ifc.wala.console.console.IFCConsole
import edu.kit.joana.ui.ifc.wala.console.console.ImprovedCLI
import edu.kit.joana.util.TypeNameUtils
import edu.kit.joana.wala.core.CallGraph
import edu.kit.joana.wala.core.SDGBuilder
import edu.kit.joana.wala.core.SDGBuilder.SDGSummaryAdder
import edu.kit.joana.wala.core.openapi.OpenApiClientDetector
import edu.kit.joana.wala.core.openapi.OpenApiServerDetector
import edu.kit.joana.wala.core.openapi.OperationId
import org.junit.jupiter.api.fail
import org.objectweb.asm.Type
import java.util.* // ktlint-disable no-wildcard-imports
import kotlin.collections.ArrayDeque
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Stores the supported operation ids (either server or client) and call graph id
 */
class MappedOperationIds(val map: BiMap<OperationId, Int> = HashBiMap.create()) : BiMap<OperationId, Int> by map {

    val cgIds: Set<Int>
        get() = values
}

/**
 * Node in the multi SDG graph, referencing
 */
class MultiSDGNode(
    private val sdgAdder: SDGSummaryAdder,
    val server: MappedOperationIds,
    val client: MappedOperationIds,
    val allInvokeNodeIds: Set<Int>
) {

    val sdg: SDG
        get() = sdgAdder.sdg

    fun compute(changedCgIds: Set<Int>?): SDGBuilder.SDGSummaryAdder.Result {
        return sdgAdder.compute(
            allInvokeNodeIds,
            Optional.ofNullable(if (changedCgIds.isNullOrEmpty()) null else changedCgIds)
        )/*.also {
            GraphViewer.launch(sdg)
            readLine()
        }*/
    }

    companion object {

        @JvmStatic
        fun create(builder: SDGBuilder.SDGSummaryBuilder): MultiSDGNode {
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
                SDGBuilder.SDGSummaryAdder(builder, serverMap.values),
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

data class IFCConsoleWithMore(val ifcConsole: IFCConsole, val miscCommands: List<String>) {
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

fun String.toIFCConsoleWithMore(ignoreDots: Boolean = false): IFCConsoleWithMore {
    val parts = this.splitIntoCommands().map { it.trim() }
    if ("..." in parts) {
        assert(parts.count { it == "..." } == 1)
        val mid = parts.indexOf("...")
        if (ignoreDots) {
            return IFCConsoleWithMore((parts.subList(0, mid) + parts.subList(mid + 1, parts.size)).toIFCConsole(), emptyList())
        }
        return IFCConsoleWithMore(parts.subList(0, mid).toIFCConsole(), parts.subList(mid + 1, parts.size))
    }
    return IFCConsoleWithMore(parts.toIFCConsole(), emptyList())
}

/** runs the commands that should be run before the "..." (or all commands with ignoreDots=true)*/
fun Iterable<String>.toIFCConsoleWithMore(ignoreDots: Boolean = false): List<IFCConsoleWithMore> = map {
    println(it.split(";")[0])
    println("-".repeat(50))
    it.toIFCConsoleWithMore(ignoreDots)
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

fun Iterable<IFCConsoleWithMore>.toMultiSDGComputer() = MultiSDGComputer.create(map { it.ifcConsole })

fun Iterable<IFCConsoleWithMore>.runWithMultiSDGComputatation() {
    toMultiSDGComputer().compute()
    for ((ifcConsole, miscCommands) in iterator()) {
        ifcConsole.printSeparator()
        if (!ifcConsole.process(miscCommands)) {
            fail("Error for $this")
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
        fun create(ifcConsoles: List<IFCConsole>): MultiSDGComputer {
            return MultiSDGComputer(
                ifcConsoles.associate { ifcConsole ->
                    ifcConsole.createSDGBuilder().get().let {
                        MultiSDGNode.create(it.first.builder) to MiscSDGProperties(ifcConsole, it.first, it.second)
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
            node: MultiSDGNode,
            cgId: Int,
            otherEntryNode: SDGNode,
            otherEdges: Set<edu.kit.joana.util.Pair<SDGNode, SDGNode>>
        ) -> Unit
    ) {
        difference.map.forEach { (entryNode, edges) ->
            val cgId = node.sdg.getCGNodeId(entryNode)
            val operation = node.server.inverse()[cgId]!!
            getClients(operation).forEach { func(it, it.client[operation]!!, entryNode, edges) }
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
            affectedCgIds?.let { alreadyConsidered[n]!!.affectedCgIds.addAll(it) }
        }
        nodes.keys.forEach(::push)
        while (!worklist.isEmpty()) {
            val (cur, affectedCgIds) = worklist.removeFirst()
            cur.compute(affectedCgIds).let { sums ->
                pushEdges(cur, sums.difference()) { node, cgId, otherEntryNode, otherEdges ->
                    push(node, setOf(cgId))
                    System.err.println("found $otherEntryNode $otherEdges")
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
