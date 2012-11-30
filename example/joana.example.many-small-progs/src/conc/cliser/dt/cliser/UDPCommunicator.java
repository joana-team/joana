/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/** UDPCommunicator.java
 *  @author Joel Adams
 *  @version 1.0.  Copright 2000.  All rights reserved.
 */

package conc.cliser.dt.cliser;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

abstract public class UDPCommunicator extends Communicator {
    // Default packet size
    public final static int PACKET_MAX = 128;

    private DatagramSocket mySocket;
    private DatagramPacket myLastPacket;
    private String         myRemoteHost;

    /** explicit constructor
     *  @params remoteHost, a String
     *  @params port, an int
     *  POST: my inherited attributes have been initialized.
     */
    public UDPCommunicator(String remoteHost, int port) {
        super(port);

        try {
            myRemoteHost = remoteHost;
            mySocket = initSocket();
            myLastPacket = null;
            //	  System.err.println("UDPCommunicator built: "
            //	                     + mySocket.getLocalAddress() + ":"
            //                           + mySocket.getPort());

        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }
    }

    /** polymorphic socket initialization method
     *  @return a DatagramSocket initialized appropriately.
     */
    abstract protected DatagramSocket initSocket();


    /** pseudo copy constructor
     *  @params aCommunicator, a UDPCommunicator
     *  POST: I am a shallow copy of aCommunicator.
     */
    public UDPCommunicator(UDPCommunicator aCommunicator) {
        super(aCommunicator.getRemotePort());

        if (aCommunicator != null) {
            myRemoteHost = aCommunicator.getRemoteHost();
            mySocket = aCommunicator.getSocket();

        } else {
            myRemoteHost = null;
            mySocket = null;
        }

        myLastPacket = null;
    }

    /** destructor
     *  POST: mySocket is closed.
     */
    protected void finalize()
    throws Throwable {
        mySocket.close();
    }

    /** local host accessor
     *  @return the host on which I am being used.
     */
    public final String getLocalHost() {
        String result = null;
        try {
            if (mySocket == null)
                result = InetAddress.getLocalHost().getHostName();
            else
                result = mySocket.getLocalAddress().getHostName();

        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }

        return result;
    }

    /** remote host accessor
     *  @return the remote host to which I am connected
     *           (or from which I last heard).
     */
    public final String getRemoteHost() {
        DatagramPacket lastPacket = this.getLastPacket();
        if (lastPacket != null)
            return lastPacket.getAddress().getHostName();
        else
            return myRemoteHost;
    }

    /** remote port accessor
     *  @return the remote port to which I am connected
     *           (or from which I last heard).
     */
    public final int getRemotePort() {
        DatagramPacket lastPacket = this.getLastPacket();
        if (lastPacket != null)
            return lastPacket.getPort();
        else if (mySocket != null)
            return mySocket.getPort();
        else
            return this.getPort();  // ??
    }

    /** local port accessor
     *  @return the (local) port I am using
     */
    public final int getLocalPort() {
        return this.getSocket().getLocalPort();
    }

    /** accessor to retrieve last sender from last packet
     *  PRE: Someone has sent me a packet.
     *  @return the sender of the last packet I received
     */
    public final InetAddress getLastSender() {
        InetAddress result = null;

        try {
            if (myLastPacket == null)
                result =InetAddress.getByName( this.getRemoteHost() );
            else
                result = myLastPacket.getAddress();

        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }

        return result;
    }

    /** accessor to retrieve port from last packet
     *  PRE: Someone has sent me a packet.
     *  @return the port of the last packet I received
     */
    public final int getLastSendersPort() {
        if (myLastPacket == null)
            return this.getPort();
        else
            return myLastPacket.getPort();
    }

    /**  wrapper for DatagramSocket.setSoTimeout
     *   @receive milliseconds, an int.
     *   @throws SocketException if nothing has been received
     *            in milliseconds time.
     */
    public final void setTimeOut(int milliseconds)
    throws SocketException {
        mySocket.setSoTimeout(milliseconds);
    }

    /** send() primitive
     *  @params message, a String.
     *  POST: message has been sent via me.
     */
    public void send(String message) {
        try {
            //      System.err.println("UDPCommunicator.send() "
            //                         + "sending: '" + message + "' to "
            //			 + getLastSender() + ":"
            //                         + getLastSendersPort() );
            DatagramPacket packet
                    = new DatagramPacket(
                            message.getBytes(),
                            message.length(),
                            this.getLastSender(),
                            this.getLastSendersPort());
            mySocket.send(packet);
            //      System.err.println("UDPCommunicator.send():"
            //                         + "sent: '" + message + "' to "
            //			 + getLastSender() + ":"
            //                         + getLastSendersPort() );

        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }
    }

    /** receive() primitive
     *  PRE: Someone is sending me a message.
     *  @return the next message sent to me.
     */
    public String receive() {
        String result = "";
        byte [] buffer = new byte[PACKET_MAX];
        DatagramPacket packet = new DatagramPacket(buffer, PACKET_MAX);

        try {
            //          System.err.println("UDPCommunicator.receive():"
            //                             + "Waiting to receive via "
            //                             + mySocket.getLocalAddress() + ":"
            //                             + mySocket.getPort());
            mySocket.receive(packet);
            myLastPacket = packet;
            result = new String(packet.getData());
            //          System.err.println("UDPCommunicator.receive():"
            //                             + "received: '" + result
            //                             + "' from " + this.getLastSender() + ":"
            //                             + this.getLastSendersPort() );

        } catch (Exception e) {
            System.err.println(e); // System.exit(1);
            //	    try { this.wait(); } catch (InterruptedException ie) {}
        }

        return result;
    }

    /** mySocket accessor
     *  @return mySocket
     */
    protected final DatagramSocket getSocket() {
        return mySocket;
    }

    /** myLastPacket accessor
     *  @return myLastPacket
     */
    protected final DatagramPacket getLastPacket() {
        return myLastPacket;
    }

    /** myLastPacket mutator
     *  @params aPacket, a DatagramPacket.
     *  POST: myLastPacket = aPacket.
     */
    protected void setLastPacket(DatagramPacket aPacket) {
        myLastPacket = aPacket;
    }
}
