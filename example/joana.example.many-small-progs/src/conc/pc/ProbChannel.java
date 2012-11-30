/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package conc.pc;

import sensitivity.Security;

class Thread1 extends Thread {

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		ProbChannel.x = 0;
		Security.leak(ProbChannel.x);
	}
	
}

class Thread2 extends Thread {
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		ProbChannel.y = Security.SECRET;
		while (ProbChannel.y != 0) {
			ProbChannel.y--;
		}
		ProbChannel.x = 1;
		Security.leak(2);
	}
	
	
}


/**
 * @author Martin Mohr (transscripted from Giffhorn's PhD thesis)
 */
public class ProbChannel {
	
	static int x, y;
	
	
	
	public static void main(String[] args) {
		new Thread1().start();
		new Thread2().start();
	}
	

}
