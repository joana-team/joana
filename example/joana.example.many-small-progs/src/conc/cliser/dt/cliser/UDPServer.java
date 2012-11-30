/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/** UDPServer.java
 *  @author Joel C. Adams
 *  @version 1.0.  Copyright 2000.  All rights reserved.
 */

package conc.cliser.dt.cliser;

abstract public class UDPServer extends Server {
    /** explicit communicator
     *  @params service, a String naming my service
     *  @params port, an int specifying the port I will use
     * POST: my (inherited) instance variables have been initialized.
     */
    UDPServer(String service, int port) {
        super(service, port);
    }

    /** definition of abstract CommunicatorUser.initCommunicator()
     *  PRE: myPort has been initialized.
     *  @return a new ServerUDPCommunicator initialized using myPort
     */
    protected Communicator initCommunicator() {
        return new ServerUDPCommunicator(this.getPort());
    }
}
