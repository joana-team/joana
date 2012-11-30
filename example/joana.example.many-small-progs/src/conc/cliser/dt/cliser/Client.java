/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/** Client.java
 *  @author Joel Adams
 *  @version 1.0. Copyright 2000.  All rights reserved.
 */

package conc.cliser.dt.cliser;

/** Client provides an abstract class for storing attributes
 *   common to all kinds of clients.
 *  <P>
 *  See the constructor for the specific attributes.
 */
abstract public class Client extends CommunicatorUser {
    private String       myService;
    private String       myServer;
    private int          myPort;
    private Communicator myCommunicator;

    /** Explicit-value constructor.
     *  @param service, a String naming my service
     *  @param remoteHost, a String naming my remote host
     *  @param port, an int specifying the port I will use
     * <P>
     * POST: my instance variables have been initialized:<BR>
     *        myService == <TT>service</TT><BR>
     *        && myServer == <TT>remoteHost</TT><BR>
     *        && myPort == <TT>port</TT><BR>
     *        && myCommunicator has been initialized by a subclass
     *            via abstract <TT>CommunicatorUser.initCommunicator()<TT>.
     *  @see CommunicatorUser
     */
    public Client(String service, String remoteHost, int port) {
        myService = service;
        myServer = remoteHost;
        myPort = port;
        setCommunicator(this.initCommunicator());
    }

    /** What the client does during its lifetime.
     *  Currently just calls interactWithServer()
     *  (but can be overridden).
     *  <P>
     *  PRE: myCommunicator has been initialized for my server.<BR>
     *  POST: I have interacted with my server.
     */
    public void run() {
        this.interactWithServer();
    }

    /** What the client does in interacting with its server (abstract).
     *  <P>
     *  PRE: myCommunicator has been initialized for my server.<BR>
     *  POST: I have finished interacting with my server.
     */
    abstract public void interactWithServer();


    /** Access myService attribute.
     *  @return myService.
     */
    public final String getService() {
        return myService;
    }

    /** Access myServer attribute.
     *  @return myServer
     */
    public final String getServer() {
        return myServer;
    }

    /** Access myPort attribute.
     *  @return myPort
     */
    public final int getPort() {
        return myPort;
    }
}
