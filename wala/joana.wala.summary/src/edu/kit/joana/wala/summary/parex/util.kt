package edu.kit.joana.wala.summary.parex

import edu.kit.joana.ifc.sdg.graph.SDG
import edu.kit.joana.ifc.sdg.graph.SDGEdge
import edu.kit.joana.ifc.sdg.graph.SDGNode
import edu.kit.joana.util.maps.MultiHashMap
import edu.kit.joana.util.maps.MultiMap
import org.jgrapht.DirectedGraph
import org.jgrapht.ext.DOTExporter
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.UnmodifiableDirectedGraph
import org.jgrapht.graph.builder.DirectedGraphBuilder
import java.io.FileWriter
import java.util.AbstractMap.SimpleEntry
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaGetter

@JvmOverloads
fun nodeGraphT(graph: Graph, root: FuncNode? = null, restrictToFunc: Boolean = false, nodeFilter: (Node) -> Boolean = { true }):
        UnmodifiableDirectedGraph<Node, DefaultEdge> {
    val jgraph = org.jgrapht.graph.DefaultDirectedGraph<Node, DefaultEdge>(DefaultEdge::class.java)
    val builder = DirectedGraphBuilder(jgraph)
    (root?.reachable { it: Node ->
        if (restrictToFunc && it is FuncNode && it != root){
            emptyList()
        } else {
            it.outgoing(restrictToFunc)
        }
    } ?: graph.getAllNodes()).filter(nodeFilter).let { reachable ->
        reachable.forEach  { builder.addVertex(it) }
        reachable.forEach { it.outgoing().filter(nodeFilter).forEach { t -> builder.addEdge(it, t) } }
    }

    return builder.buildUnmodifiable()
}

@JvmOverloads
fun <T: Node> exportDot(graph: DirectedGraph<T, DefaultEdge>, fileName: String, sdg: SDG? = null) {
    DOTExporter<T, DefaultEdge>({
        it.id.toString()
    }, {
        it.toString() + ("|" + sdg?.getNode(it.id)?.label + "|" + sdg?.getNode(it.id)?.kind + "|" + sdg?.getNode(it.id)?.proc)
    }, { edge: DefaultEdge ->
        val source = graph.getEdgeSource(edge)
        val target = graph.getEdgeTarget(edge)
        val res = source::class.java.methods.filter {
            if (it.name.startsWith("get")) {
                val mem = it.invoke(source)
                mem == target || (mem is Collection<*> && mem.contains(target))
            } else {
                false
            }
        }
        res.joinToString(",") { it.name.substring(3).toLowerCase() }
    }).export(FileWriter(fileName), graph)
}


fun Graph.getAllNodes(): Set<Node> {
    return callGraph.vertexSet().flatMap { it.reachable { n: Node ->
        n.outgoing(hideCallGraph = true)
    } + it}.toSet()
}

fun findDuplicateNodes(graph: Graph): Map<Pair<Node, String>, Set<Node>> {
    return graph.getAllNodes().flatMap { n -> findDuplicateNodes(n).map { Pair(n, it.key) to it.value } }.toMap()
}

fun findDuplicateNodes(node: Node):  Map<String, Set<Node>> {
    return node::class.memberProperties.map{ prop -> Pair(prop.name, prop.javaGetter!!.invoke(node).let { value ->
        when (value){
            is Collection<*> -> duplicates(value).map { it as Node }.toSet()
            else -> setOf()
        }
    })}.filter { it.second.isNotEmpty() }
            .map { it.first to it.second }.toMap()
}

fun <T> duplicates(col: Collection<T>): Set<T> {
    return col.groupBy { it }.filter { it.value.size > 1 }.map { it.key }.toSet()
}

/**
 * Insert the summary edges back into the graph
 */
@JvmOverloads
fun Graph.insertSummaryEdgesIntoSDG(sdg: SDG, summaryEdgeKind: SDGEdge.Kind = SDGEdge.Kind.SUMMARY){
    for (actualIn in actualIns) {
        val sdgInNode = sdg.getNode(actualIn.id)
        actualIn.summaryEdges?.forEach { actualOut ->
            val sdgOutNode = sdg.getNode(actualOut.id)
            sdg.addEdge(sdgInNode, sdgOutNode , summaryEdgeKind.newEdge(sdgInNode, sdgOutNode))
        }
    }
}

/**
 * Result of comparing two multi maps
 */
data class MultiMapCompResult<K, V>(
        val missing: MultiMap<K, V>, val additional: MultiMap<K, V>){

    fun matches(): Boolean {
        return missing.keySet().isEmpty() && additional.keySet().isEmpty()
    }

    @JvmOverloads
    fun format(kToS: (K) -> String, vToS: (V) -> String, map: MultiMap<K, V>? = null): String {
        if (map == null){
            val res = mutableListOf<String>()
            if (missing.keySet().isNotEmpty()){
                res.add("missing:")
                res.add(format(kToS, vToS, missing))
            }
            if (additional.keySet().isNotEmpty()){
                res.add("additional:")
                res.add(format(kToS, vToS, additional))
            }
            return res.joinToString("\n")
        }
        return map.keySet().flatMap {
            val key = kToS(it)
            val entries = map.get(it).iterator()
            val space = " ".repeat(key.length)
            listOf(key + " → " + vToS(entries.next())) + entries.asSequence().map { e -> space + " → " + vToS(e) }
        }.joinToString("\n")
    }
}

/**
 * Compare the summary edges with the one in the graph
 *
 * Use case: run another summary edge computation implementation on the SDG and compare it
 *
 * The base line is the sdg
 */
@JvmOverloads
fun Graph.compareSummaryEdges(sdg: SDG, summaryEdgeKind: SDGEdge.Kind = SDGEdge.Kind.SUMMARY): MultiMapCompResult<SDGNode, SDGNode> {
    return getSummaryEdgeMap(sdg).diffRelatedTo(sdg.getSummaryEdgesOfSDG(summaryEdgeKind))
}

internal fun Graph.getSummaryEdgeMap(sdg: SDG): MultiMap<SDGNode, SDGNode> {
    val map = MultiHashMap<SDGNode, SDGNode>()
    this.actualIns.forEach {
        val sdgNode = sdg.getNode(it.id)
        it.summaryEdges?.forEach { out ->
            map.add(sdgNode, sdg.getNode(out.id))
        }
    }
    return map
}

@JvmOverloads
fun SDG.getSummaryEdgesOfSDG(summaryEdgeKind: SDGEdge.Kind = SDGEdge.Kind.SUMMARY): MultiMap<SDGNode, SDGNode> {
    val map = MultiHashMap<SDGNode, SDGNode>()
    vertexSet().stream().filter { it.kind == SDGNode.Kind.ACTUAL_IN }.forEach {
        val sdgNode = getNode(it.id)
        outgoingEdgesOfUnsafe(sdgNode).filter { edge -> edge.kind == summaryEdgeKind }.forEach { edge -> map.add(sdgNode, edge.target) }
    }
    return map
}

fun <K, V> MultiMap<K, V>.diffRelatedTo(base: MultiMap<K, V>): MultiMapCompResult<K, V> {
    return MultiMapCompResult(base.additionsComparedTo(this), this.additionsComparedTo(base))
}

fun <K, V> MultiMap<K, V>.additionsComparedTo(base: MultiMap<K, V>): MultiMap<K, V> {
    this.entrySet().map { SimpleEntry(it.key, it.value.filter { v -> !base.get(it.key).contains(v) }) }
            .filter { (_, v) -> v.isNotEmpty() }.let {
        val map = MultiHashMap<K, V>()
        it.forEach { (k, vs) -> vs.forEach { v -> map.add(k, v)} }
        return map
    }
}