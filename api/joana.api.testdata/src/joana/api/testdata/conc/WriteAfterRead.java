/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.conc;

/**
 * In this example, two threads are spawned. The first thread reads a shared
 * value, the second thread overwrites it. The second thread is spawned after
 * the first has been terminated. Analysis should conclude that the read cannot
 * influence the write.
 * 
 * @author Martin Mohr
 */
public class WriteAfterRead {
	
	public static void main(String[] args) throws InterruptedException {
		Shared s = new Shared();
		ReadingThread t1 = new ReadingThread(s);
		WritingThread t2 = new WritingThread(s);
		
		t1.start();
		t1.join();
		t2.start();
	}
}

class Shared {
	int x;
}

class ReadingThread extends Thread {

	Shared s;

	ReadingThread(Shared s) {
		this.s = s;
	}
	
	public void run() {
		if (this.s.x == 42) {
			System.out.println("leak!");
		}
	}
}

class WritingThread extends Thread {

	Shared s;

	WritingThread(Shared s) {
		this.s = s;
	}
	
	public void run() {
		this.s.x = 41;
	}

}
