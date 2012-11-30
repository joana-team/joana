/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/** ClientUDPCommunicator.java
 *  @author Joel C. Adams
 *  @version 1.0.  Copyright 2000.  All rights reserved.
 */

package conc.cliser.dt.cliser;

import java.net.DatagramSocket;

public class ClientUDPCommunicator extends UDPCommunicator {
    public ClientUDPCommunicator(String remoteHost, int port) {
        super(remoteHost, port);
    }

    /** definition of abstract UDPCommunicator.initSocket()
     *  @return a DatagramSocket initialized for a client.
     */
    protected DatagramSocket initSocket() {
        DatagramSocket result = null;

        try {
            result = new DatagramSocket();
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }

        return result;
    }
}
