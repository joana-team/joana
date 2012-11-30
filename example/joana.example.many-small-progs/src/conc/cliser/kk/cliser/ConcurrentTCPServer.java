/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/** ConcurrentTCPServer.java
 *  @author Joel C. Adams
 *  @version 1.0.  Copyright 2000.  All rights reserved.
 */

package conc.cliser.kk.cliser;

abstract public class ConcurrentTCPServer extends TCPServer {
    private ThreadManager myThreadManager;

    /** explicit constructor
     *  @params service, a String naming my service
     *  @params port, an int specifying the port I will use
     *  @params prototypeThread, a ServiceThread.
     * POST: my (inherited) instance variables have been initialized
     *      && myThreadManager is initialized using prototypeThread.
     */
    public ConcurrentTCPServer(String service, int port, ServiceThread prototypeThread) {
        super(service, port);
        myThreadManager = new ThreadManager(prototypeThread);
    }

    /** definition of abstract CommunicatorUser.initCommunicator()
     *  @return a new ConcurrentServerTCPCommunicator
     *        initialized using myPort.
     */
    protected Communicator initCommunicator() {
        return new ConcurrentServerTCPCommunicator(this.getPort());
    }

    /** default concurrent server behavior
     *  (run forever, accepting connections, and handing them off
     *    to ServiceThreads for servicing).
     */
    public void run() {
        System.err.println("ConcurrentTCPServer: running on "
                + this.getHost() + ":" + this.getPort());
        ServiceThread sessionThread = null;

        for (;;) {
            TCPCommunicator newCommunicator = this.acceptConnection();
            //      System.err.println("ConcurrentTCPServer.run(): "
            //		   + "connection accepted from "
            //		   + newCommunicator.getRemoteHost());

            sessionThread = myThreadManager.getThread();

            if (sessionThread != null) {
                sessionThread.setCommunicator( newCommunicator );
                sessionThread.start();

            } else {
                System.err.println("ConcurrentTCPServer.run()"
                        + " ServiceThread limit reached");
                //		System.exit(1);
            }
        }
    }
}
