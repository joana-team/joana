/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.toy.conc;

class Thread0 extends Thread {
	private Object o = new Object();

	private String id;

	public Thread0(String id, Object o) {
		this.id = id;
		this.o = o;
	}

	public void run() {
		for (int j = 0; j < 5; j++) {
			for (int i = 0; i < id.length(); i++) {
				synchronized (o) {
					System.out.print(id.charAt(i));
					o = new Object();
					// o.notifyAll();
				}
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

public class ThreadTest2 {

	public static void main(String[] args) {
		Object mon = new Object();
		new Thread0("Claudia", mon).start();
		new Thread0("Martin", mon).start();

	}
}
