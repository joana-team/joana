package edu.kit.joana.wala.summary.parex

import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashMap
import kotlin.math.min

/**
 * Comput
 */
class PreComputationResult {

}

/**
 * Trivially parallelized sequential analysis, that assigns each thread a number of functions it looks out for
 *
 * What it does not: it does no work balancing between the threads, nor does it calculate strongly connected components
 */
class BasicParallelAnalysis(val numberOfThreads: Int = Runtime.getRuntime().availableProcessors()) : SequentialAnalysis2() {

    override fun process(g: Graph) {
        Computers(g, numberOfThreads).run()
    }

    /**
     * Processes only queue items for specific function nodes
     */
    internal inner class Computer(val assignedFuncNodes: Set<FuncNode>,
                                  val itemHandler: (List<QueueItem>) -> Unit,
                                  val queueItemCounter: AtomicInteger,
                                  val queue: ConcurrentLinkedDeque<QueueItem> = ConcurrentLinkedDeque()) : Runnable {

        fun initQueue(){
            queueItemCounter.addAndGet(assignedFuncNodes.size)
            assignedFuncNodes.forEach { func ->
                queue.offer(QueueItem(func))
                func.data = State(func.formalIns.map { fi ->
                    /**
                     * Each formal in node gets its own queue state that is initialized with the current neighbors of the formal in
                     * node. We can assume that the formal in node itself will never be part of this queue
                     */
                    fi to NodeQueue(fi.curNeighbors())
                }.toMap())
            }
        }

        override fun run() {
            while (queueItemCounter.get() > 0){
                queue.pollFirst()?.let { item ->
                    val newItems = process(item)
                    queueItemCounter.addAndGet(newItems.size - 1)
                    itemHandler.invoke(newItems)
                }
            }
        }

        fun offer(item: QueueItem){
            queue.offer(item)
        }
    }

    internal inner class Computers(val g: Graph, val numberOfThreads: Int){

        val computers: List<Computer>
        val computersPerFunc: Map<FuncNode, Computer>
        val queueItemCounter: AtomicInteger = AtomicInteger(0)

        init {
            val funcs = g.callGraph.vertexSet().toList()
            Collections.shuffle(funcs)
            val funcsPerThread = Math.ceil(funcs.size * 1.0 / (numberOfThreads - 1)).toInt()
            computersPerFunc = HashMap()
            computers = (0 until min(numberOfThreads, funcs.size)).map { i ->
                val fs = ((i * funcsPerThread) until min((i + 1) * funcsPerThread, funcs.size))
                        .map {
                            funcs[it]
                        }
                        .toSet()
                val comp = Computer(fs, this::handleNewItems, queueItemCounter)
                comp.initQueue()
                fs.forEach {
                    computersPerFunc[it] = comp
                }
                comp
            }.toList()
        }

        fun run(){
            val threads = computers.map(::Thread)
            threads.forEach(Thread::start)
            while (queueItemCounter.get() != 0){
                Thread.sleep(1)
            }
        }

        internal fun handleNewItems(items: Iterable<QueueItem>){
            items.forEach { item ->
                computersPerFunc[item.funcNode]?.offer(item)
            }
        }
    }
}

