/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/** ClientTCPCommunicator.java
 *  @author Joel C. Adams
 *  @version 1.0.  Copyright 2000.  All rights reserved.
 */

package conc.cliser.kk.cliser;

import java.net.Socket;

public class ClientTCPCommunicator extends TCPCommunicator {
    /** explicit constructor
     *  @params remoteHost, a String
     *  @params port, an int
     *  POST: my instance variables have been initialized.
     */
    public ClientTCPCommunicator(String remoteHost, int port) {
        super(remoteHost, port);
    }

    /** explicit constructor
     *  @params aSocket, a Socket
     *  POST: my instance variables have been initialized.
     */
    public ClientTCPCommunicator(Socket aSocket) {
        super(aSocket);
    }

    /** definition for abstract TCPCommunicator.initSocket()
     *  POST: super.mySocket == a new TCP Socket
     *          connected to (myRemoteHost, myPort).
     */
    protected Socket initSocket() {
        Socket result = null;

        try {
            result = new Socket(this.getRemoteHost(), this.getRemotePort());

        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }

        return result;
    }
}

