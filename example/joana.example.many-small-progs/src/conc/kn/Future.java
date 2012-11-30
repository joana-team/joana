/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
Copyright (c) 2000, Thomas W. Christopher and George K. Thiruvathukal

Java and High Performance Computing (JHPC) Organzization
Tools of Computing LLC

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

The names Java and High-Performance Computing (JHPC) Organization,
Tools of Computing LLC, and/or the names of its contributors may not
be used to endorse or promote products derived from this software
without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

This license is based on version 2 of the BSD license. For more
information on Open Source licenses, please visit
http://opensource.org.
*/

package conc.kn;

import java.util.Stack;

/**
 * An assign-once variable that allows consumers to wait for a value to be produced.
 * @author Thomas W. Christopher (Tools of Computing LLC)
 * @version 0.2 Beta
 */
public class Future extends SimpleFuture implements RunDelayed{
    /**
     * The Stack containing waiting Runnables.
     * (Stack so its quick and easy to have them wait and to remove them.)
     */
    protected Stack<Object> runnablesWaiting = null;

    /**
     * The default queue into which to put (runDelayed) runnables that are
     * waiting on any of these Futures. The limit on the number
     * of threads that can be running the (runDelayed) objects is the
     * amount of memory available. The (runDelayed) objects are placed
     * in this queue by default when a Future is given a value.
     */
    protected static RunQueue classRunQueue = new RunQueue();

    /**
     * The queue into which to put (runDelayed) runnables that are
     * waiting on this Future. The limit on the number
     * of threads that can be running the (runDelayed) objects is the
     * amount of memory available. They are placed in the queue when
     * the Future is given a value.
     */
    protected RunQueue runQueue = classRunQueue;

    /**
     * Create a Future with no value yet assigned.
     */
    public Future() {
        super();
    }

    /**
     * Create a Future with a value initially assigned.
     * @param val The value the Future is to be initialized with.
     */
    public Future(Object val) {
        super(val);
    }

    /**
     * Assigns a value to the Future and notifies all waiting threads.
     * Attempts to change a previously assigned value will be ignored.
     * @param val The value to be assigned to the Future.
     */
    public synchronized void setValue(Object val) {
        super.setValue(val);
        if (runnablesWaiting!=null){
            while (!runnablesWaiting.empty()) {
                getRunQueue().run((Runnable)runnablesWaiting.pop());
            }
            runnablesWaiting=null;
        }
    }

    /**
     * Get the RunQueue for a Future object. The run queue should be
     * changed with setRunQueue for more precise control.
     * @return The RunQueue that objects runDelayed on a Future object will be placed in.
     */
    public RunQueue getRunQueue() {
        return runQueue;
    }

    /**
     * Set the RunQueue for a Future object.
     */
    public void setRunQueue(RunQueue rq) {
        runQueue = rq;
    }

    /**
     * Get the RunQueue for the Future class.
     * @return The RunQueue that  objects runDelayed on a pure Future will be placed in.
     */
    public static RunQueue getClassRunQueue() {
        return classRunQueue;
    }

    /**
     * Schedule a runnable object to execute when the Future has its value set.
     */
    public synchronized void runDelayed(Runnable r) {
        if (value != this) runQueue.run(r);
        else {
            if (runnablesWaiting == null)
                runnablesWaiting = new Stack<Object>();
            runnablesWaiting.push(r);
        }
    }
}
