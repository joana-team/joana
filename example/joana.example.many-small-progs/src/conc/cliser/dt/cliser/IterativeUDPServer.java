/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/** IterativeUDPServer.java
 *  @author Joel C. Adams
 *  @version 1.0.  Copyright 2000.  All rights reserved.
 */

package conc.cliser.dt.cliser;

abstract public class IterativeUDPServer extends UDPServer {
    /** explicit communicator
     *  @params service, a String naming my service
     *  @params port, an int specifying the port I will use
     * POST: my (inherited) instance variables have been initialized.
     */
    public IterativeUDPServer(String service, int port) {
        super(service, port);
    }

    /** default iterative server behavior:
     *  (run forever, interacting with different clients)
     */
    public void run()     {
        System.err.println("IterativeUDPServer running on "
                + this.getHost() + ":" + this.getPort());

        for (;;) {
            this.interactWithClient();
            //      System.err.println("Server: service completed for "
            //               + ((UDPCommunicator)getCommunicator()).getLastSender() );
        }
    }

    /** method defined by subclass specifying server's interaction with client
     *  POST: my service has been performed for that connection's client
     */
    abstract public void interactWithClient();
}
