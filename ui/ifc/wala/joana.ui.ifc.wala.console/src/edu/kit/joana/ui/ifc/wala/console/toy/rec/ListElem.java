/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.toy.rec;

public class ListElem {
	private int data;
	private ListElem next;

	public ListElem(int data) {
		this.data = data;
		this.next = null;
	}

	public ListElem(int data, ListElem next) {
		this.data = data;
		this.next = next;
	}

	public int getData() {
		return data;
	}

	public ListElem getNext() {
		return next;
	}

	public boolean hasNext() {
		return next != null;
	}

	public void setNext(ListElem next) {
		this.next = next;
	}
}
