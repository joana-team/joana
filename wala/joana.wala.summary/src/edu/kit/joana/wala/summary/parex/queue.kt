package edu.kit.joana.wala.summary.parex

import gnu.trove.set.hash.TIntHashSet
import java.util.*

/**
 * Basic queue of nodes that ensures that nodes enter the queue only once
 */
class NodeQueue<T:Node>() {
    private val queue = ArrayDeque<T>()
    private val alreadySeen = TIntHashSet()

    constructor(initialElement: T) : this() {
        push(initialElement)
    }
    constructor(initialElements: Collection<T>) : this() {
        push(initialElements)
    }


    fun push(node: T) {
        if (alreadySeen.add(node.id)){
            queue.add(node)
        }
    }

    fun push(nodes: Collection<T>) {
        nodes.forEach(this::push)
    }

    fun poll(): T = queue.pop()

    fun isNotEmpty() = queue.isNotEmpty()

    fun addAlreadySeen(node: T) = alreadySeen.add(node.id)

    fun alreadySeen(node: T) = alreadySeen.contains(node.id)
}