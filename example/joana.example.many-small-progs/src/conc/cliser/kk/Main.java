/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package conc.cliser.kk;

/**
 * This Main class provides a main method to start up a server thread and run the client.
 *
 * @author Todd Wallentine tcw AT cis ksu edu
 * @version $Revision: 1.1 $ - $Date: 2004/04/23 14:25:19 $
 */
public class Main {
	public static final String HOST = "localhost";
	public static final String PORT = "5013";

	public static void main(String[] args)
	throws Exception {

		// run the server
		KnockKnockServerThread kkst = new KnockKnockServerThread();
		kkst.start();

		// wait a little bit for the server to start up
		Thread.sleep(1000);

		// run the client
		KnockKnockTCPClient.main(new String[] {Main.HOST, Main.PORT});

		// stop the server?
		//kkst.stop();
		//kkst.interrupt();
		System.exit(0);
	}
}

class KnockKnockServerThread extends Thread {
	public void run() {
		KnockKnockConcurrentTCPServer.main(new String[] {Main.PORT});
	}
}
