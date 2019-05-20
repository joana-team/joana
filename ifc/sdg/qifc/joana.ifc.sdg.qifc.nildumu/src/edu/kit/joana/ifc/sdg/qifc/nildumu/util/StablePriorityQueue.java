/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu.util;

import java.util.AbstractQueue;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Iterators;

/**
 * Queue that uses the insertion order of elements if they they are
 * equal
 */
public class StablePriorityQueue<E> extends AbstractQueue<E>{
	
	private static class QueueElement<E> {
		private static int ELEM_COUNTER = 0;
		private final int number;
		private final E element;
		
		public QueueElement(E element) {
			this.number = ELEM_COUNTER++;
			this.element = element;
		}
		
		@Override
		public boolean equals(Object obj) {
			return obj instanceof QueueElement && ((QueueElement)obj).element.equals(element);
		}
		
		@Override
		public int hashCode() {
			return element.hashCode();
		}
	}

	private final PriorityQueue<QueueElement<E>> queue;
	
	public StablePriorityQueue(Comparator<E> comparator) {
		queue = new PriorityQueue<>((a, b) -> 
			ComparisonChain.start().compare(a.element, b.element, comparator).compare(a.number, b.number)
			.result());
	}
	
	/**
	 * Only allows elements that are not yet present in the queue
	 */
	@Override
	public boolean offer(E e) {
		assert !queue.contains(new QueueElement<>(e));
		return queue.offer(new QueueElement<>(e));
	}

	@Override
	public E poll() {
		return queue.poll().element;
	}

	@Override
	public E peek() {
		return queue.peek().element;
	}

	@Override
	public Iterator<E> iterator() {
		return Iterators.transform(queue.iterator(), e -> e.element);
	}

	@Override
	public int size() {
		return queue.size();
	}
	
	@Override
	public boolean contains(Object o) {
		return queue.contains(new QueueElement<E>((E)o));
	}
}
