/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/** ThreadManager.java
 *  @author Joel C. Adams
 *  @version 1.0.1.  Copyright 2000.  All rights reserved.
 *
 *  Invariant: threadsInUse < threadCount
 *               -> myThreadList.getFirst() is a ready to use ServiceThread
 *          && threadsInUse > 0
 *               -> myThreadList.getLast() == the last ServiceThread used
 *  Revision History:
 *    4/2/2002 - JCA - removed redundant 'myThreadList.addLast(result)'
 *                     from getThread().
 *                     Thanks to quentin.delance@atior.com
 */

package conc.cliser.kk.cliser;

import java.util.LinkedList;

public class ThreadManager extends Object {
    public static final int INITIAL_NUM_THREADS = 5;
    public static final int MAX_NUM_THREADS = 15;

    private LinkedList<Runnable> myThreadList;     // collection of threads
    private int        myThreadCount,    // size of collection
                       threadsInUse,     // how many are in use
                       myThreadMaximum;  // how many there can be
    private ServiceThread myProtoThread; // prototype thread (cloneable)

    public ThreadManager(ServiceThread protoThread) {
        this(INITIAL_NUM_THREADS, MAX_NUM_THREADS, protoThread);
    }

    public ThreadManager(int numThreads, int maxThreads, ServiceThread protoThread) {
        myProtoThread = protoThread;
        myProtoThread.setManager(this);
        myThreadMaximum = maxThreads;
        myThreadList = new LinkedList<Runnable>();

        for (int i = 1; i <= numThreads; i++) {
            ServiceThread newThread = ((ServiceThread) myProtoThread.clone());
            myThreadList.add(newThread);
        }
        myThreadCount = numThreads;
        threadsInUse = 0;
    }

    synchronized public ServiceThread getThread() {
        ServiceThread result = null;
        if (threadsInUse <= myThreadMaximum) {
            if (threadsInUse < myThreadCount) {
                result = (ServiceThread) myThreadList.removeFirst();

            } else if (threadsInUse == myThreadCount) {
                result = (ServiceThread) myProtoThread.clone();
                myThreadCount++;

            } else {
                System.err.println("ThreadManager.getThread(): "
                        + "threadsInUse (" + threadsInUse
                        + ") > myThreadCount ("
                        + myThreadCount + ")");
                System.exit(1);
            }

            threadsInUse++;
            myThreadList.addLast(result);

        } else {
            System.err.println("ThreadManager.getThread() WARNING: "
			       + "thread count at maximum ("
			       + myThreadMaximum
                   + "), connections may be lost.");
        }

        return result;
    }

    synchronized public void notifyDone(ServiceThread aThread) {
        int itsIndex = myThreadList.indexOf(aThread);
        myThreadList.addFirst(myThreadList.remove(itsIndex));
        threadsInUse--;
    }
}
