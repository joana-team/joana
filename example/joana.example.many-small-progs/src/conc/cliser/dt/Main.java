/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package conc.cliser.dt;

/**
 * The Main class provides a way to start the server and client for the daytime client/server
 * example.  This simply queries the server once and quits.
 *
 * I found that if the main thread continues after starting the server the system will hang.  Therefore,
 * I added a sleep so the server is started before the client request comes in.  This will be one good way to show a deadlock (
 * remove the Thread.sleep() call).
 *
 * @author Todd Wallentine tcw AT cis ksu edu
 * @version $Revision: 1.1 $ - $Date: 2004/04/23 14:28:33 $
 */
public class Main {

    public static final String HOST = "localhost";
    public static final String PORT = "5013";

    public static void main(String[] args)
    throws Exception {

        // run the Daytime server
        DaytimeServerThread dst = new DaytimeServerThread();
        dst.start();

        // wait a little bit so the server can start up properly before sending a message.
        Thread.sleep(1000);

        // run the Daytime client
        DaytimeUDPClient.main(new String[] {Main.HOST, Main.PORT});

        // stop the server?
        dst.stop();
    }
}

class DaytimeServerThread extends Thread {
    public void run() {
        DaytimeIterativeUDPServer.main(new String[] {Main.PORT});
    }
}
