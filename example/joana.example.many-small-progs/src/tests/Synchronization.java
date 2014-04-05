/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests;

import java.util.LinkedList;

import sensitivity.Security;

public class Synchronization {

	public static void main(String[] args) {
		Buffer b = new Buffer();
		Prod p = new Prod(b);
		Cons c = new Cons(b);

		p.start();
		c.start();
	}
}

class Buffer {
    LinkedList a;

    Buffer() {
        a = new LinkedList();
    }

	public void put(Object o) {
		synchronized (this) {
	        a.addFirst(o);
		}
	}

	public Object get() {
		synchronized (this) {
	        return a.poll();
		}
	}
}

class Prod extends Thread {
	private Buffer b;

	public Prod(Buffer b) {
		this.b = b;
	}

	public void run() {
		while(true) {
		    int i = Security.SECRET;
		    b.put(i);
		}
	}
}

class Cons extends Thread {
	private Buffer b;

	public Cons(Buffer b) {
		this.b = b;
	}

	public void run() {
		while(true) {
			Object o = b.get();
			Security.PUBLIC = o.hashCode();
			System.out.println(o);
		}
	}
}
