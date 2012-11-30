/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/** ServerUDPCommunicator.java
 *  @author Joel C Adams
 *  @version 1.0.  Copyright 2000.  All rights reserved.
 */

package conc.cliser.dt.cliser;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ServerUDPCommunicator extends UDPCommunicator {
   /** explicit constructor
    *  @params port, an int
    *  POST: my inherited attributes have been initialized.
    */
    public ServerUDPCommunicator(int port) {
       super("", port);
    }

    /** pseudo copy constructor
     *  @params sudpCommunicator, a ServerUDPCommunicator
     *  POST: I am a shallow copy of sudpCommunicator.
     */
    public ServerUDPCommunicator(ServerUDPCommunicator sudpCommunicator) {
        super(sudpCommunicator);
    }

     /** definition of abstract UDPCommunicator.initSocket()
     *  PRE: myPort is initialized.
     *  @return a DatagramSocket initialized to myPort.
     */
    protected DatagramSocket initSocket() {
        DatagramSocket result = null;

        try {
            result = new DatagramSocket(super.getPort());
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }

        return result;
    }


   /** method to get the first packet
     *  @return the next packet sent to me.
     */
    public DatagramPacket acceptPacket() {
        byte [] buffer = new byte[PACKET_MAX];
        DatagramPacket packet = new DatagramPacket(buffer, PACKET_MAX);

        try {
            //       System.err.println("UDPCommunicator.receiveLine():"
            //                           + "Waiting to receive via "
            //                           + getSocket().getLocalAddress() + ":"
            //                           + getSocket().getPort());
            getSocket().receive(packet);
            this.setLastPacket(packet);
            //          System.err.println("UDPCommunicator.receiveLine():"
            //                             + "received packet from "
            //                             + this.getLastSender() + ":"
            //                             + this.getLastSendersPort() );

        } catch (Exception e) {
            System.err.println(e);
        }

        return packet;
     }
}
