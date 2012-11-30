/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/** Communicator.java defines an abstraction for TCP and UDP sockets.
 * @author Joel C Adams
 * @version 1.0.  Copyright 2000. All Rights Reserved
 */

package conc.cliser.kk.cliser;

abstract public class Communicator {
    private int myPort;

    /** default constructor
     *  POST: myPort == a default value.
     */
    public Communicator() {
        myPort = -1;
    }

    /** explicit constructor
     *  @params port, an int
     *  POST: myPort == port
     */
    public Communicator(int port) {
        myPort = port;
    }

    /** port accessor
     *  @return myPort
     */
    public final int getPort() {
        return myPort;
    }

    /** abstract accessors for my:
     *   local port, local host
     *   remote port, remote host
     */
    abstract public int getLocalPort();

    abstract public int getRemotePort();

    abstract public String getLocalHost();

    abstract public String getRemoteHost();


    /** abstract communication 'send' primitive
     *  @params message, a String.
     *  POST: message has been sent via me.
     */
    abstract public void send(String message);

    /** abstract communication 'receive' primitive
     *  PRE: someone is sending a message to me
     *  @return the message received
     */
    abstract public String receive();

    /** communication 'send-object' primitive
     *  @params object, a communicable object
     *  POST: object has been sent via me.
    abstract public void sendObject(Serializable object);
     */

    /** communication 'receive-object' primitive
     *  PRE: someone is sending an object to me
     *  @return the object received
    abstract public Serializable receiveObject();
     */

    /** String converter
     *  @return a String rendition of myself
     */
    public String toString() {
        return getLocalHost() + ":" + getLocalPort() + "<-->"
        + getRemoteHost() + ":" + getRemotePort();
    }
}


