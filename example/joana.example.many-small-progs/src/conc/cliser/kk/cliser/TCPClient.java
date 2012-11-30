/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/** TCPCLient.java
 *  @author Joel Adams
 *  @version 1.0.  Copyright 2000.  All rights reserved.
 */

package conc.cliser.kk.cliser;

abstract public class TCPClient extends Client {
    /** explicit communicator
     *  @params service, a String naming my service
     *  @params remoteHost, a String naming my remote host
     *  @params port, an int specifying the port I will use
     * POST: my (inherited) instance variables have been initialized.
     */
    public TCPClient(String service, String remoteHost, int port) {
        super(service, remoteHost, port);
    }

    /** definition of abstract Client.initCommunicator()
     *  PRE: myServer and myPort are initialized.
     *  @return a new ClientTCPCommunicator initialized
     *     for myServer and myPort.
     */
    protected Communicator initCommunicator() {
        return new ClientTCPCommunicator(this.getServer(), this.getPort());
    }
}


