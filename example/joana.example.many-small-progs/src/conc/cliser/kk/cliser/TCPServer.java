/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/** TCPServer.java
 *  @author Joel C. Adams
 *  @version 1.0.  Copyright 2000.  All rights reserved.
 */

package conc.cliser.kk.cliser;

abstract public class TCPServer extends Server {
    /** explicit communicator
     *  @params service, a String naming my service
     *  @params port, an int specifying the port I will use
     * POST: my (inherited) instance variables have been initialized.
     */
    TCPServer(String service, int port) {
        super(service, port);
    }

    /** utility to accept a connection
     *  PRE: myCommunicator is initialized.
     *  @return the TCPCommunicator returned by myCommunicator.accept().
     */
    protected TCPCommunicator acceptConnection() {
        return ((ServerTCPCommunicator)getCommunicator()).accept();
    }
}
