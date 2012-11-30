/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/** TCPCommunicator.java
 *  @author Joel Adams
 *  @version 1.0.  Copright 2000.  All rights reserved.
 */

package conc.cliser.kk.cliser;

import java.net.*;      // Socket
import java.io.*;       // PrintWriter, BufferedReader, ...

abstract public class TCPCommunicator extends Communicator {
    private Socket         mySocket;
    private BufferedReader myReader;
    private PrintWriter    myWriter;
    private String         myRemoteHost;

    /** default constructor
     *  POST: my instance variables are initialized to default values:
     *         myRemoteHost == null && mySocket == null.
     */
    public TCPCommunicator() {
        super();
        myRemoteHost = null;
        mySocket = null;
    }

    /** explicit constructor
     *  @params remoteHost, a String
     *  @params port, an int
     *  POST: my instance variables are initialized to supplied values:
     *         myRemoteHost.equal(remoteHost)
     *         && mySocket is initialized to (remoteHost, port)
     *            (port is local for a server, remote for a client)
     *         && myReader and myWriter are "wrappers" for mySocket.
     */
    public TCPCommunicator(String remoteHost, int port) {
        super(port);
        myRemoteHost = remoteHost;
        mySocket = initSocket();  // polymorphic (client vs server)

        if (mySocket != null) {
            initReaderAndWriter();
        } else {
            myReader = null;
            myWriter = null;
        }
    }

    /* explicit constructor
     * @params aSocket, a Socket
     * POST: my instance variables are initialized to supplied values:
     *        myRemoteHost equals the host associated with aSocket
     *        && mySocket equals aSocket
     *        && myReader and myWriter are "wrappers" for mySocket.
     */
    public TCPCommunicator(Socket aSocket) {
        super(aSocket.getPort());

        if (aSocket != null) {
            myRemoteHost = aSocket.getInetAddress().getHostName();
            mySocket = aSocket;
            initReaderAndWriter();

        } else {
            myRemoteHost = null;
	        mySocket = null;
	        myReader = null;
	        myWriter = null;
        }
    }

    /** initialize myReader and myWriter
     * PRE: mySocket has been initialized
     * POST: myReader is a wrapper for mySocket.getInputStream()
     *      && myWriter is a wrapper for mySocket.getOutputStream()
     */
    protected void initReaderAndWriter() {
        try {
            myReader = new BufferedReader(
                    new InputStreamReader(mySocket.getInputStream()));

            myWriter = new PrintWriter(mySocket.getOutputStream(), true);

        } catch (IOException e) {
            System.err.println(e); System.exit(1);
        }
    }

    /** destructor
     * POST: mySocket has been closed
     */
    protected void finalize()
    throws Throwable {
        mySocket.close();
    }

    /** send() primitive
     *  @params message, a String
     *  POST: message has been sent via me.
     */
    public void send(String message) {
        if (myWriter != null)
            myWriter.println(message);
    }

    /** receive() primitive
     *  PRE: Someone is sending a message to me.
     *  @return the message someone sent to me.
     */
    public String receive() {
        String result = "";
        if (myReader != null) {
            try {
                result = myReader.readLine();
            } catch (IOException e) {
                System.err.println("TCPCommunicator.receive()" + e);
            }
        }

        return result;
    }

    /** abstract socket initialization method
     *  (different in client and server Communicators)
     */
    abstract protected Socket initSocket();

    /** mySocket mutator (useable only by subclasses)
     *  @param aSocket, a Socket
     *  POST: mySocket == aSocket.
     */
    protected void setSocket(Socket aSocket) {
        if (aSocket == null) {
            System.err.println("TCPCommunicator.setSocket()"
			       + " null Socket argument received");
            System.exit(1);
        }

        mySocket = aSocket;
    }

    /** mySocket accessor
     *  @return mySocket
     */
    protected final Socket getSocket() {
        return mySocket;
    }

    /** local port accessor
     *  @return the port I am using on my machine
     */
    public final int getLocalPort() {
        if (mySocket == null) {
            System.err.println("TCPCommunicator.getLocalPort():"
			       + " socket uninitialized");
            System.exit(1);
        }

        return mySocket.getLocalPort();
    }

    /** local host accessor
     *  @return the machine on which I am being used
     */
    public final String getLocalHost() {
        if (mySocket == null) {
            System.err.println("TCPCommunicator.getLocalHost():"
			       + " socket uninitialized");
            System.exit(1);
        }

        return mySocket.getLocalAddress().getHostName();
    }

    /** remote port accessor
     *  @return the port on a remote host to which I am connected
     */
    public final int getRemotePort() {
        if (mySocket == null)
            return super.getPort();
        else
            return mySocket.getPort();
    }

    /** remote host accessor
     *  @return the remote host to which I am connected
     */
    public final String getRemoteHost() {
        if (mySocket == null)
            return myRemoteHost;
        else
            return mySocket.getInetAddress().getHostName();
    }
}
