/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/** Server.java
 *  @author Joel C. Adams
 *  @version 1.0.  Copyright 2000.  All rights reserved.
 */

package conc.cliser.kk.cliser;

import java.net.InetAddress;

abstract public class Server extends CommunicatorUser {
    private int          myPort;
    private String       myService;

    /** explicit communicator
     *  @params service, a String naming my service
     *  @params port, an int specifying the port I will use
     * POST: my instance variables have been initialized:
     *        myService == service && myPort == port
     *        && myCommunicator has been initialized by a subclass.
     */
    public Server(String service, int port) {
        myPort = port;
        myService = service;
        this.setCommunicator( this.initCommunicator() ); // TCP vs. UDP
    }

    /** myPort accessor
     *  @return myPort.
     */
    public final int getPort() {
        return myPort;
    }

    /** accessor for my local host
     *  @return the name of my local host
     */
    public final String getHost() {
        String result = null;
        try {
            result = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }

        return result;
    }

    /** myService accessor
     *  @return myService.
     */
    public final String getService() {
        return myService;
    }
}
