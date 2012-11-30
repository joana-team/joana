/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/** ConcurrentServerTCPCommunicator.java
 *  @author Joel Adams
 *  @version 1.0.  Copyright 2000.  All rights reserved.
 */

package conc.cliser.kk.cliser;

import java.net.Socket;

public class ConcurrentServerTCPCommunicator extends ServerTCPCommunicator {
    /** explicit constructor
     *  @params port, an int
     *  POST: my inherited attributes have been initialized.
     */
    public ConcurrentServerTCPCommunicator(int port) {
        super(port);
    }

    /** definition of abstract ServerTCPCommunicator.accept()
     * PRE: Some client is going to connect to me.
     *  @return a new ClientTCPCommunicator, in which mySocket is set to the return value of myServerSocket.accept()
     */
    public TCPCommunicator accept() {
        Socket sessionSocket = null;
        try {
            sessionSocket = this.getServerSocket().accept();
            //  System.err.println("ConcurrentServerTCPCommunicator.accept():"
            //			     + " using socket " + sessionSocket );

        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }

        return new ClientTCPCommunicator(sessionSocket);
    }
}

