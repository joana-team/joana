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
package joana.api.testdata.toy.rec;

import static edu.kit.joana.api.annotations.Annotations.*;

public class MyList {
	private ListElem head = null;

	public void add(int e) {
		if (head == null) {
			head = new ListElem(e);
		} else {
			ListElem current = head;
			while (current.hasNext()) {
				current = current.getNext();
			}
			current.setNext(new ListElem(e));
		}
	}

	public boolean isEmpty() {
		return head == null;
	}

	public int getSize() {
		if (isEmpty())
			return 0;
		else {
			ListElem current = head;
			int size = 1;
			while (current.hasNext()) {
				size++;
				current = current.getNext();
			}
			return size;
		}
	}

	public static void main(String[] args) {
		MyList l = new MyList();
		for (int i = 0; i < 10; i++)
			l.add(SECRET);
		// head value is (influenced by) value of SECRET
		leak(toggle(l.head.getData()));

		// but the lists size isn't
		leak(l.getSize());
	}

}
