/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package examples.testdata;

public class C extends Thread {
	static Cell cell;

	public static void main(String[] args) {
		C a = new C();
		cell = new Cell();
		synchronized(a){
			a.start();
			print(cell.x);
		}
	}

	public void run() {
		synchronized(this){
			print(cell.x);
			cell.x = 17;
		}
		cell.x = 42;
	}
	
	static void print(int x){
		// this is the sink
	}
}

class Cell {
	int x = 0;
}
