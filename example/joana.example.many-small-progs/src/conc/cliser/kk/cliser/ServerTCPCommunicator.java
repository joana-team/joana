/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/** ServerTCPCommunicator.java
 *  @author Joel C Adams
 *  @version 1.0.  Copyright 2000.  All rights reserved.
 */

package conc.cliser.kk.cliser;

import java.net.ServerSocket;
import java.net.Socket;

abstract public class ServerTCPCommunicator extends TCPCommunicator {
    private ServerSocket myServerSocket;

    /** explicit constructor
     *  @params port, an int
     *  POST: myServerSocket is a TCP ServerSocket listening at port.
     */
    public ServerTCPCommunicator(int port) {
        super("", port);
        try {
            myServerSocket = new ServerSocket(port);
        } catch (Exception e) {
            System.err.println(e); System.exit(1);
        }
    }

    /** definition of abstract TCPCommunicator.initSocket()
     *  POST: mySocket == null
     *         (mySocket will be set by abstract accept()).
     */
    protected Socket initSocket() {
        return null;
    }

    /** abstract method for a server Communicator to accept connections
     * PRE: Some client is going to connect to me
     * @return a TCPCommunicator back to that client
     */
    abstract public TCPCommunicator accept();

    /** myServerSocket accessor
     *  @return myServerSocket
     */
    public final ServerSocket getServerSocket() {
        return myServerSocket;
    }
}
