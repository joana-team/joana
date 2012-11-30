/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package examples;


public class MonitorExample {
	Lock a = new Lock();
	Lock b = new Lock();

	public void nestedMonitor(){
		synchronized (a) {
			synchronized (b) {
				someMethod();
			}
		}
	}
	public void singleMonitor(){
		synchronized (a) {
			someMethod();
		}
	}

	public synchronized void syncMethod() {
		someMethod();
	}

	private int someMethod () {return 0;}
}
