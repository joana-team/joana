/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/* CommunicatorUser.java
 * @author: Joel C. Adams
 * @version: 1.0.  Copyright 2000.  All rights reserved.
 */

package conc.cliser.dt.cliser;

abstract public class CommunicatorUser {
    private Communicator myCommunicator;


    /** default constructor
     */
    CommunicatorUser() {
        myCommunicator = null;
    }

    /** abstract method for Communicator initialization
     *  @return a Communicator appropriate to the subclass
     *          that defines this method.
     */
    abstract protected Communicator initCommunicator();

    /** myCommunicator accessor
     *  @return myCommunicator
     */
    public Communicator getCommunicator() {
        return myCommunicator;
    }

    /** myCommunicator mutator
     *  @params aCommunicator, a Communicator.
     *  POST: myCommunicator == aCommunicator.
     */
    protected void setCommunicator(Communicator aCommunicator) {
        myCommunicator = aCommunicator;
    }

    /** send() primitive (for convenience, not necessity)
     *  @params aMessage, a String.
     *  POST: aMessage has been sent via this.getCommunicator().
     */
    public void send(String aMessage) {
        getCommunicator().send(aMessage);
    }

    /** receive() primitive (for convenience, not necessity)
     *  PRE: Someone will send me a message via this.getCommunicator()
     *  @return the message someone sends me
     */
    public String receive() {
        return getCommunicator().receive();
    }

    /* TODO: Add these to Communicator

    public void sendObject(Serializable object)
    {
	getCommunicator().sendObject(object);
    }

    public Serializable receiveObject()
    {
	return getCommunicator().receiveObject();
    }
     */
}
