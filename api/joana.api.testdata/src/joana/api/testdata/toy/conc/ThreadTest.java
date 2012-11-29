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
package edu.kit.joana.ui.ifc.wala.console.toy.conc;

class SharedObject {
	private String content;

	public void setContent(String str) {
		this.content = str;
	}

	public String getContent() {
		return content;
	}

	public void processContent() {
		// do something very important
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

class MyThread implements Runnable {
	private SharedObject so;
	private static String secret = "I am confidential.";

	public MyThread(SharedObject so) {
		this.so = so;
	}

	@Override
	public void run() {
		synchronized (so) {
			so.setContent(secret);
			so.processContent();
			so.setContent("I am public.");
		}
	}
}

class EvilThread implements Runnable {
	private SharedObject so;

	public EvilThread(SharedObject so) {
		this.so = so;
	}

	@Override
	public void run() {
		synchronized (so) {
			System.out.println(so.getContent());
		}
	}
}

public class ThreadTest {

	public static void main(String[] args) {
		Object o = new Object();
		synchronized (o) {
			o = new Object();
		}
	}

}
