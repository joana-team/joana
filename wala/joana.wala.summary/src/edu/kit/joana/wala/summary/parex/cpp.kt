package edu.kit.joana.wala.summary.parex

import edu.kit.joana.ifc.sdg.graph.SDG
import edu.kit.joana.ifc.sdg.graph.SDGEdge
import edu.kit.joana.wala.summary.WorkPackage
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.system.exitProcess

/**
 * Code to interface with the C++ version, to integrate it seamlessly
 */

/**
 * Location of the executable binary
 */
val EXECUTABLE = System.getenv("CPP_SUMMARY")

/**
 * Executes the binary, outputs the error out on stderr and processes input and output in parallel
 */
internal fun <T> execute(conf: String, inputWriter: (OutputStream) -> Unit, outputHandler: (InputStream) -> T): T {
    if (Files.notExists(Paths.get(EXECUTABLE))){
        println("$EXECUTABLE not a valid executable, specify it via the environment variable CPP_SUMMARY")
        exitProcess(1)
    }
    val p = Runtime.getRuntime().exec(arrayOf(EXECUTABLE, conf))
    val stdIn = BufferedInputStream(p.inputStream)
    val stdOut = BufferedOutputStream(p.outputStream)
    val stdErr = BufferedInputStream(p.errorStream)

    // we create three threads, one for each stream
    val writerThread = Thread {
        inputWriter(stdOut)
    }.also(Thread::run)
    val errorHandlerThread = Thread {
        BufferedReader(InputStreamReader(stdErr)).lines().forEach(System.err::println)
    }.also(Thread::run)
    val outputResult = Executors.newSingleThreadExecutor().submit(Callable {
        outputHandler(stdIn)
    })
    writerThread.join()
    errorHandlerThread.join()
    return outputResult.get()
}

typealias SummariesPerActualIn = Map<Int, List<Int>>

typealias SummariesPerFunc = Map<Int, SummariesPerActualIn>

/**
 * Executes the binary and collects the output
 */
fun execute(conf: String, graph: Graph): SummariesPerFunc {
    return execute(conf, { out ->
        Dumper().dump(graph, out)
    }, { input ->
        val dataInput = DataInputStream(input)
        val numberOfFunctions = dataInput.readInt()
        (0 until numberOfFunctions).map {
            val header = GraphProto.SummaryFunctionHeader.parseDelimitedFrom(input)
            header.id to (0 until header.numberOfActins).map {
                GraphProto.SummaryEdgesPerActin.parseDelimitedFrom(input).let { it.actIn to it.actOutsList }
            }.toMap()
        }.toMap()
    })
}

/**
 * Insert the summary edges back into the graph in parallel
 */
@JvmOverloads
fun SDG.insertSummaryEdgesFromMaps(summs: SummariesPerFunc, summaryEdgeKind: SDGEdge.Kind = SDGEdge.Kind.SUMMARY): Int {
    return summs.values.parallelStream().mapToInt { sumsPerActIn ->
        sumsPerActIn.entries.stream().mapToInt { (actIn, actOuts) ->
            with(getNode(actIn)){
                actOuts.forEach { actOut ->
                    val actOutNode = getNode(actOut)
                    addEdgeUnsafe(this, actOutNode, summaryEdgeKind.newEdge(this, actOutNode))
                }
                actOuts.size
            }
        }.sum()
    }.sum()
}

class CPPAnalysis(val conf: String = "") : Analysis {

    /**
     * Raw cpp output
     */
    fun processAndReturn(g: Graph): SummariesPerFunc = execute(conf, g)

    /**
     * Inserts the found summary edges in parallel into the graph
     */
    override fun process(g: Graph) {
        processAndReturn(g).values.parallelStream().forEach { sumsPerActIn ->
            sumsPerActIn.forEach { (actIn, actOuts) ->
                with(g.nodes[actIn] as ActualInNode){
                    actOuts.forEach { actOut ->
                        summaryEdges.add(g.nodes[actOut] as OutNode)
                    }
                }
            }
        }
    }

    override fun compute(pack: WorkPackage<SDG>, relevantEdges: Set<SDGEdge.Kind>, summaryEdgeKind: SDGEdge.Kind): Int {
        return processWithOutInsert(pack, relevantEdges).let {
            pack.graph.insertSummaryEdgesFromMaps(it, summaryEdgeKind)
        }
    }

    /**
     * Inserts the found summary edges in parallel into the sdg
     */
    override fun compute(graph: Graph, sdg: SDG): Int {
        return processAndReturn(graph).let {
            sdg.insertSummaryEdgesFromMaps(it)
        }
    }

    fun processWithOutInsert(pack: WorkPackage<SDG>,
                relevantEdges: Set<SDGEdge.Kind> = DEFAULT_RELEVANT_EDGES,
                ignoreSummaryEdges: Boolean = false): SummariesPerFunc {
        return SDGToGraph(relevantEdges, ignoreSummaryEdges).convert(pack).let(this::processAndReturn)
    }
}