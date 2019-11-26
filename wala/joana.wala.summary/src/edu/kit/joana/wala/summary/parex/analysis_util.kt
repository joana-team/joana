package edu.kit.joana.wala.summary.parex

import java.util.*
import kotlin.collections.HashSet

fun <T> worklist(initialElements: Collection<T>, process: (T) -> Iterable<T>) {
    val queue = ArrayDeque<T>()
    queue.addAll(initialElements)
    val inQueue = HashSet<T>()
    while (queue.isNotEmpty()) {
        process(queue.pop().also { inQueue.remove(it) }).forEach {
            if (!inQueue.contains(it)) {
                inQueue.add(it)
                queue.add(it)
            }
        }
    }
}