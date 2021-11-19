package edu.kit.joana.api.sdg.openapi

import com.ibm.wala.ipa.callgraph.CGNode
import edu.kit.joana.ifc.sdg.core.SecurityNode
import edu.kit.joana.ifc.sdg.graph.SDG
import edu.kit.joana.ifc.sdg.graph.SDGEdge
import edu.kit.joana.ifc.sdg.graph.SDGNode
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind.ACTUAL_IN
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind.ACTUAL_OUT
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind.FORMAL_IN
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind.FORMAL_OUT
import edu.kit.joana.ifc.sdg.graph.SDGNode.Operation
import java.util.IdentityHashMap

class Propagation(val exSDG: ExSDG) {

    val sdg
        get() = exSDG.sdg
    val callGraph
        get() = exSDG.callGraph

    /** propagation result for a single function graph */
    data class SinglePropagationResult(
        val entryNode: SDGNode,
        val introducedFis: MutableMap<String, SDGNode> = mutableMapOf(),
        val introducedFos: MutableMap<String, SDGNode> = mutableMapOf(),
        /** variable → call node → ai node */
        val introducedAis: MutableMap<String, MutableMap<SDGNode, SDGNode>> = mutableMapOf(),
        val introducedAos: MutableMap<String, MutableMap<SDGNode, SDGNode>> = mutableMapOf()
    )

    /** a propagation result per server SDG */
    inner class SDGPropagationResult(private val map: MutableMap<SDG, Map<SDGNode, SinglePropagationResult>> = IdentityHashMap()) {

        operator fun get(serverSDG: SDG) = map.computeIfAbsent(serverSDG) { _ ->
            callGraph.vertexSet()
                .associate { sdg.getSDGNode(it.id).let { node -> node to SinglePropagationResult(node) } }
        }

        fun get(serverSDG: SDG, node: SDGNode) = get(serverSDG)[node]!!
    }

    /** combined result of propagations,
     * rationale: share the propagated variables as they are probably directly related if they have the same name*/
    private val propagationResult = SDGPropagationResult()

    /**
     * Create Fis and Fos for the passed variables in the client entry sub graph and the function graphs that call it.
     * Create Ais and Aos and connect them properly.
     * Keep a look for existing propagated variables.
     *
     * Takes current entry node
     * -> find all cg nodes up
     * -> add needed formal ins and formal outs to all cg node up (and current node)
     * -> for each of the cg nodes
     * --> create actual in and out nodes for each caller
     * --> formal in → actual in → parameter in → formal in, formal out → parameter out → actual out → every other actual in (same in the other direction)
     * -> for each caller up: add formal ins and outs
     * -> for each caller
     * --> add actual ins and outs
     * -->
     */
    fun propagateSDGNodesForVariables(clientEntryNode: SDGNode, variablesToIntroduce: Iterable<String>, addException: Boolean) {
        val allCGNodesUp = exSDG.getCGNodesUp(clientEntryNode)
        var newId = sdg.lastId() + 1
        /** add needed formal ins and formal outs (including an _exception_ fo) */

        fun flowEdge(source: SDGNode, sink: SDGNode) {
            sdg.addEdgeAndVertices(source, sink, SDGEdge.Kind.CONTROL_FLOW::newEdge)
        }

        fun controlEdge(source: SDGNode, sink: SDGNode) {
            sdg.addEdgeAndVertices(source, sink, SDGEdge.Kind.CONTROL_DEP_EXPR::newEdge)
        }

        fun node(kind: SDGNode.Kind, op: Operation, cgNode: CGNode, label: String): SDGNode {
            val proc = exSDG.getEntryNode(cgNode).proc
            return SecurityNode(kind, newId++, op, proc, label)
        }

        allCGNodesUp.forEach { cgNode ->
            val entry = exSDG.getEntryNode(cgNode)
            val propagationResult = propagationResult.get(sdg, entry)
            variablesToIntroduce.forEach { variable ->
                if (variable !in propagationResult.introducedFis) {
                    val fiNode = node(FORMAL_IN, Operation.FORMAL_IN, cgNode, "FI $variable")
                    val foNode = node(FORMAL_OUT, Operation.FORMAL_OUT, cgNode, "FO $variable")
                    propagationResult.introducedFis[variable] = fiNode
                    propagationResult.introducedFos[variable] = foNode
                    controlEdge(entry, fiNode)
                    controlEdge(entry, foNode)
                }
            }
            if (addException && !sdg.hasExceptionNode(entry)) {
                val exception = node(FORMAL_OUT, Operation.FORMAL_OUT, cgNode, "_exception_")
                controlEdge(entry, exception)
            }
        }
        allCGNodesUp.forEach { cgNode ->
            exSDG.getCallSites(exSDG.getEntryNode(cgNode)).forEach { (call, cgNode) -> /** for each caller */
                val callerEntryNode = exSDG.getEntryNode(cgNode)
                /** create actual ins and outs and connect them with the call node and the formal ones */
                variablesToIntroduce.forEach { variable ->
                    val propResult = propagationResult.get(sdg, callerEntryNode)
                    if (variable !in propResult.introducedAis || call !in propResult.introducedAis[variable]!!) {
                        /** we have to add this variable */
                        val aiNode = node(ACTUAL_IN, Operation.ACTUAL_IN, cgNode, "AI $variable")
                        val aoNode = node(ACTUAL_OUT, Operation.ACTUAL_OUT, cgNode, "AO $variable")
                        propResult.introducedAis.computeIfAbsent(variable) { HashMap() }[call] = aiNode
                        propResult.introducedAos.computeIfAbsent(variable) { HashMap() }[call] = aoNode
                        controlEdge(call, aiNode)
                        controlEdge(call, aoNode)

                        /** and add parameter in and out edges */
                        sdg.getPossibleTargets(call).forEach { possibleEntry ->
                            propagationResult[sdg][possibleEntry]?.let { calledPropRes ->
                                if (variable in calledPropRes.introducedFis) {
                                    val fi = calledPropRes.introducedFis[variable]!!
                                    val fo = calledPropRes.introducedFos[variable]!!
                                    sdg.addEdge(SDGEdge.Kind.PARAMETER_IN.newEdge(aiNode, fi))
                                    sdg.addEdge(SDGEdge.Kind.PARAMETER_OUT.newEdge(fo, aoNode))
                                }
                            }
                        }

                        propResult.introducedAos.computeIfAbsent(variable) { HashMap() }

                        /** connect ai to all existing oos and the formal in */
                        (setOf(propResult.introducedFis[variable]!!) + propResult.introducedAos[variable]!!.values).forEach { ao ->
                            if (ao != aoNode) {
                                flowEdge(ao, aiNode)
                            }
                        }
                        /** connect ao to all existing ois and the formal out */
                        (setOf(propResult.introducedFos[variable]!!) + propResult.introducedAis[variable]!!.values).forEach { ai ->
                            if (aiNode != ai) {
                                flowEdge(aoNode, ai)
                            }
                        }
                    }
                }
                /** add exception nodes */
                if (addException && !sdg.hasExceptionNode(call)) {
                    val exception = node(ACTUAL_OUT, Operation.ACTUAL_OUT, cgNode, "ret _exception_")
                    controlEdge(call, exception)
                    sdg.getPossibleTargets(call).mapNotNull { sdg.getExceptionNode(it) }.forEach { fo ->
                        sdg.addEdge(SDGEdge.Kind.PARAMETER_OUT.newEdge(fo, exception))
                    }
                    /** connect to fo exception */
                    flowEdge(exception, sdg.getExceptionNode(callerEntryNode)!!)
                }
            }
        }
    }

    fun getFiNode(sdg: SDG, entryNode: SDGNode, variable: String): SDGNode? =
        propagationResult.get(sdg, entryNode).introducedFis[variable]

    fun getFoNode(sdg: SDG, entryNode: SDGNode, variable: String): SDGNode? =
        propagationResult.get(sdg, entryNode).introducedFos[variable]
}
