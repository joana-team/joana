package edu.kit.joana.wala.summary.parex

import edu.kit.joana.ifc.sdg.graph.SDG
import edu.kit.joana.ifc.sdg.graph.SDGEdge
import edu.kit.joana.wala.summary.WorkPackage
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.system.exitProcess

/**
 * Code to interface with the C++ version, to integrate it seamlessly
 */

/**
 * Location of the executable binary
 */
val EXECUTABLE: String? = System.getenv("CPP_SUMMARY")

/**
 * Executes the binary, outputs the error out on stderr and processes input and output in parallel
 */
internal fun execute(conf: String, inputWriter: (OutputStream) -> Unit, outputHandler: (InputStream) -> Unit) {
    if (EXECUTABLE == null || Files.notExists(Paths.get(EXECUTABLE))){
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
        stdOut.close()
    }.also(Thread::start)
   val errorHandlerThread = Thread {
        BufferedReader(InputStreamReader(stdErr)).lines().forEach(System.err::println)
    }.also(Thread::start)
    val outputHandlerThread = Thread {
        outputHandler(stdIn)
    }.also(Thread::start)
    writerThread.join()
    errorHandlerThread.join()
    outputHandlerThread.join()
    p.waitFor()
}

typealias SummariesPerActualIn = Map<Int, List<Int>>

typealias SummariesPerFunc = Map<Int, SummariesPerActualIn>

typealias SummariesPerFuncStream = Stream<Pair<Int, SummariesPerActualIn>>

/**
 * Executes the binary and collects the output
 */
fun execute(conf: String, graph: Graph, sumsHandler: (SummariesPerFuncStream) -> Unit) {
    execute(conf, { out ->
        Dumper().dump(graph, out)
    }, { input ->
        val dataInput = DataInputStream(input)
        val numberOfFunctions = dataInput.readInt()
        (0 until numberOfFunctions).toList().stream().map {
            val header = GraphProto.SummaryFunctionHeader.parseDelimitedFrom(input)
            header.id to (0 until header.numberOfActins).map {
                GraphProto.SummaryEdgesPerActin.parseDelimitedFrom(input).let { it.actIn to it.actOutsList }
            }.toMap()
        }.let(sumsHandler)
    })
}

/**
 * Insert the summary edges back into the graph in parallel
 */
@JvmOverloads
fun SDG.insertSummaryEdgesFromStream(summs: SummariesPerFuncStream, summaryEdgeKind: SDGEdge.Kind = SDGEdge.Kind.SUMMARY): Int {
    val executor = Executors.newWorkStealingPool()
    return executor.invokeAll<Int>(summs.map { (func, sumsPerActIn) ->
        Callable {
            sumsPerActIn.entries.stream().mapToInt { (actIn, actOuts) ->
                with(getNode(actIn)){
                    actOuts.forEach { actOut ->
                        val actOutNode = getNode(actOut)
                        addEdgeUnsafe(this, actOutNode, summaryEdgeKind.newEdge(this, actOutNode))
                    }
                    actOuts.size
                }
            }.sum()
        }
    }.collect(Collectors.toList())).stream().mapToInt { f: Future<Int> -> f.get()}.sum()
}

class CPPAnalysis(val conf: String = "") : Analysis {

    /**
     * Raw cpp output
     */
    fun processAndReturn(g: Graph, sumsHandler: (SummariesPerFuncStream) -> Unit) = execute(conf, g, sumsHandler)

    /**
     * Inserts the found summary edges in parallel into the graph
     */
    override fun process(g: Graph) {
        processAndReturn(g) { sumsStream ->
            sumsStream.forEach { (funcId, sumsPerActIn) ->
                sumsPerActIn.forEach { (actIn, actOuts) ->
                    with(g.nodes[actIn] as ActualInNode){
                        actOuts.forEach { actOut ->
                            summaryEdges.add(g.nodes[actOut] as OutNode)
                        }
                    }
                }
            }
        }
    }

    override fun compute(pack: WorkPackage<SDG>, relevantEdges: Set<SDGEdge.Kind>, summaryEdgeKind: SDGEdge.Kind): Int {
        var num = 0
        processWithOutInsert(pack, { sumsStream ->
            num = pack.graph.insertSummaryEdgesFromStream(sumsStream, summaryEdgeKind)
        }, relevantEdges)
        return num
    }

    /**
     * Inserts the found summary edges in parallel into the sdg
     */
    override fun compute(graph: Graph, sdg: SDG): Int {
        var num = 0
        processAndReturn(graph) { sumsStream ->
            num = sdg.insertSummaryEdgesFromStream(sumsStream)
        }
        return num
    }

    fun processWithOutInsert(pack: WorkPackage<SDG>,
                             sumsHandler: (SummariesPerFuncStream) -> Unit,
                relevantEdges: Set<SDGEdge.Kind> = DEFAULT_RELEVANT_EDGES,
                ignoreSummaryEdges: Boolean = false) {
        SDGToGraph(relevantEdges, ignoreSummaryEdges).convert(pack).let {
            processAndReturn(it, sumsHandler)
        }
    }
}