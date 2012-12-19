/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.rec;

import static edu.kit.joana.api.annotations.Annotations.*;

public class MyList2 {
	private int data;
	private MyList2 tail;

	public MyList2(int data, MyList2 tail) {
		this.data = data;
		this.tail = tail;
	}

	public void add(Integer data) {
		if (tail == null) {
			tail = new MyList2(data, null);
			if (this.data == 0)
				System.out.println(data);
			else
				System.out.println(data * data);
		} else
			tail.add(data);
	}

	public int getSize() {
		if (tail == null)
			return 1;
		else
			return 1 + tail.getSize();
	}

	public Integer getHeadData() {
		return data;
	}

	public MyList2 getTail() {
		return tail;
	}

	public static void main(String[] args) {
		MyList2 l = new MyList2(0, null);
		for (int i = 1; i < 10; i++)
			l.add(SECRET);
		// head value is (influenced by) value of SECRET
		leak(toggle(l.data));

		// but the lists size isn't
		//leak(l.getSize());
	}
}
