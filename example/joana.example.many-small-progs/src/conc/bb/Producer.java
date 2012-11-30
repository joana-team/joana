/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package conc.bb;

class Producer implements Runnable {
	//~ Instance variables .....................................................

	protected BoundedBuffer bb = null;

	//~ Constructors ...........................................................

	/*@ behavior
 	   @   requires bb != null;
 	   @   assignable this.bb;
	   @   ensures this.bb == bb;
	   @*/
	public Producer(BoundedBuffer bb) {
		this.bb = bb;
	}

	//~ Methods ................................................................

	/*@also
	  @ behavior
	  @   requires this.bb != null;
	  @   diverges true;
	  @   ensures false;
	  @*/
	public void run() {
		Object item;

		while (true) {
			item = new Object();
			bb.deposit(item);
		}
	}
}
