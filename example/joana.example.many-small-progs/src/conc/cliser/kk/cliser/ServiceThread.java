/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/** ServiceThread.java
 *  @author Joel C. Adams
 *  @version 1.0 Copyright 2000.  All rights reserved.
 *
 */

package conc.cliser.kk.cliser;

abstract public class ServiceThread extends CommunicatorUser implements Runnable, Cloneable {
    private static int     numThreads = 0;

    private Communicator   myCommunicator;
    private Thread         myThread;
    private int            myThreadID;
    private ThreadManager  myManager;

    /** default constructor
     *  POST: my instance variables are set to default values.
     */
    public ServiceThread() {
        this(null);
    }

    /** explicit constructor
     *  @params aManager, a ThreadManager.
     *  POST: myManager == aManager
     *       && my other instance variables (except myCommunicator)
     *           are initialized.
     */
    public ServiceThread(ThreadManager aManager) {
      	myCommunicator = null;
      	myManager = aManager;
      	this.setThreadID();
	    myThread = new Thread(this);
    }

    /** myThreadID mutator
     *  POST: myThreadID == the entry value of numThreads
     *        && numThreads has been incremented.
     */
    protected void setThreadID() {
        myThreadID = numThreads++;
    }

    /** accessor for myThreadID
     *  @return myThreadID.
     */
    public final int getThreadID() {
        return myThreadID;
    }

    /** method defined by subclass specifying thread's interaction with client
     *  PRE: A connection has been accepted.
     *  POST: my service has been performed for that connection's client
     */
    abstract public void interactWithClient();

    /** default behavior for a service thread:
     *   run forever, interacting with a client, and then sleeping.
     */
    synchronized public void run() {
    	for (;;) {
    	    System.err.println(this.toString() + " running...");
    	    //  System.err.println(this.toString() + ".run(): "
    	    //		   + "performing service via "
    	    //		   + myCommunicator);
    	    this.interactWithClient();
    	    //  System.err.println(this.toString() + ".run():
    	    //         + "service completed for "
    	    //         + myCommunicator.getRemoteHost() + ":"
    	    //		   + myCommunicator.getRemotePort()
    	    //		   + " using local port "
    	    //         + myCommunicator.getLocalPort() );
            this.awaitNextJob();
    	}
    }

    /** utility to put me to sleep until I'm given something to do
     *  PRE: I've finished providing a client with service.
     *  POST: I've gone to sleep.
     */
    synchronized protected void awaitNextJob() {
        myManager.notifyDone(this);              // update manager
        try {
            this.wait();                         // go to sleep
        } catch (Exception e) { }
    }

    /** utility to wake me up (or start me the first time)
     *  POST: I am awake and working.
     */
    synchronized public void start() {
        if (myThread.isAlive()) {                // if I'm sleeping
            // System.err.println(this.toString()
            //		   + ".start():, notifying ...");
           this.notify();                        // wake me up

        } else {                                 // I've not run before

            // System.err.println(this.toString()
            //		   + ".start(), starting myThread...");
           myThread.start();                     // start my thread
        }
    }

    /** definition for CommunicatorUser.initCommunicator()
     *  @return null.
     */
    protected Communicator initCommunicator() {
        return null;
    }

    /** accessor for myCommunicator
     *  (overrides CommunicatorUser.getCommunicator())
     *  @return myCommunicator
     */
    synchronized public Communicator getCommunicator() {
        return myCommunicator;
    }

    /** mutator for myCommunicator
     *  @params aCommunicator, a Communicator.
     *  POST: myCommunicator == aCommunicator.
     */
    synchronized public void setCommunicator(Communicator aCommunicator) {
        if (aCommunicator == null) {
            System.err.println(this.toString() + ".setCommunicator():"
			       + " null Communicator argument received");
            System.exit(1);
        }

        myCommunicator = aCommunicator;
    }

    /** accessor for myThread
     *  @return myThread.
     */
    public final Thread getThread() {
        return myThread;
    }

    /** mutator for myThread
     *  @params aThread, a Thread.
     *  POST: myThread == aThread.
     */
    public void setThread(Thread aThread) {
        if (aThread == null) {
            System.err.println(this.toString() + ".setThread(): "
			       + "null Thread argument received");
            System.exit(1);
        }

        myThread = aThread;
    }

    /** accessor for myManager
     *  @return myManager
     */
    public final ThreadManager getManager() {
        return myManager;
    }

    /** mutator for myManager
     *  @params manager, a ThreadManager.
     *  POST: myManager == manager.
     */
    public void setManager(ThreadManager manager) {
        if (manager == null) {
            System.err.println(this.toString() + ".setManager():"
			       + " null ThreadManager argument received");
            System.exit(1);
        }

        myManager = manager;
    }

    /** method to clone myself (implements Cloneable interface)
     *  @return a copy of myself.
     */
    public Object clone() {
        Object result = null;

        try {
            result = super.clone();
            ((ServiceThread) result).setThread(new Thread( (Runnable) result) );
            ((ServiceThread) result).setThreadID();

        } catch (CloneNotSupportedException e) {
    	    // should never happen because this class implements
    	    // Cloneable.
            throw new InternalError();
        } // try

        return result;
    }

    /** String converter
     *  @return a String representation of myself.
     */
    public String toString() {
        return "ServiceThread#" + myThreadID;
    }
}
