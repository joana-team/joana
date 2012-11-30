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

package conc.sq;

/**
A Monitor with condition variables, similar to those
defined by Hoare and Brinch-Hansen. A class with methods
declared synchronized is Java's standard version of
a monitor.<p>
The condition variables are provided by the inner class
Condition.<p>
To use this monitor to protect your class,
you must either have your class extend Monitor, like
this bounded buffer example:<p>
<blockquote>
<pre>
*class BoundedBuffer3 extends Monitor{
* Condition notEmpty=new Condition();
* Condition notFull=new Condition();
*
* volatile int hd=0,tl=0;
* Object[] buffer;
* public BoundedBuffer3(int size) {
*    buffer=new Object[size];
* }
* public void put(Object v)
*        throws InterruptedException {
*    enter();
*    if(tl - hd >= buffer.length) notFull.await();
*    buffer[tl++ % buffer.length] = v;
*    notEmpty.signal();
*    leave();
* }
* public Object get()
*        throws InterruptedException {
*    enter();
*    Object v;
*    if (tl==hd) notEmpty.await();
*    v = buffer[hd++ % buffer.length];
*    notFull.signal();
*    leave();
*    return v;
* }
*}
</pre>
</blockquote>
or use a separate monitor
<blockquote>
<pre>
*class BoundedBuffer4 {
* Monitor mon = new Monitor();
* Monitor.Condition notEmpty = mon.new Condition();
* Monitor.Condition notFull = mon.new Condition();
*
* volatile int hd=0,tl=0;
* Object[] buffer;
* public BoundedBuffer4(int size) {
*    buffer=new Object[size];
* }
* public void put(Object v)
*        throws InterruptedException {
*    mon.enter();
*    if(tl - hd >= buffer.length) notFull.await();
*    buffer[tl++ % buffer.length] = v;
*    notEmpty.signal();
*    mon.leave();
* }
* public Object get()
*        throws InterruptedException {
*    mon.enter();
*    Object v;
*    if (tl==hd) notEmpty.await();
*    v = buffer[hd++ % buffer.length];
*    notFull.leaveWithSignal();
*    return v;
* }
*}
</pre>
</blockquote>
A Monitor must be entered with an enter() call.
It is left by a call of leave(). You may enter
the same monitor more than once, e.g. calling one
monitor-protected method from another. You must
leave, of course, as many times as you enter.<p>

Condition is an inner class. It must be created
within a monitor object. It represents a condition
that a thread may wish to wait on. <p>
To wait for condition C to hold, a thread calls
<blockquote>
C.await();
</blockquote>
To signal that a condition holds, a thread calls
<blockquote>
C.signal();
</blockquote>
If one or more threads are awaiting the condition
at the time of a signal, one of them will be given
control of the monitor immediately. The thread
executing the signal() will wait to reacquire the
monitor. <p>
To both signal a condition and leave the monitor
simultaneously, you can call leaveWithSignal().
It is more efficient than a signal() followed by a
leave(), since it does not have to wait to reacquire
the monitor before executing the leave().
<blockquote>
C.leaveWithSignal();
</blockquote>
Be sure you declare the fields of the Monitor-protected
class to be <b>volatile</b>.<p>
@author Thomas W. Christopher (Tools of Computing LLC)
@version 0.2 Beta
*/

public class Monitor{
    /**
     * Semaphore to lock the Monitor:
     * <blockquote>
     * monitorEntry.down();
     * </blockquote>
     * locks the monitor on entry,
     * <blockquote>
     * monitorEntry.up();
     * </blockquote>
     * unlocks the monitor on exit.
     */
    Semaphore monitorEntry = new Semaphore(1);

    /**
     * The current thread. When the monitor is not in use,
     * <b>monitorEntry==null</b>. This is checked to see if
     * the monitor is being reentered by the current owner or not.
     */
    volatile Thread current = null;

    /**
     * The number of times the monitor's current owner has entered
     * it minus the number of times it has exited it.
     */
    volatile int monitorEntryCount = 0;

    /**
     * The inner class for condition variables.
     */
    public class Condition implements MonitorCondition {
        /**
         * The number of threads waiting on this condition.
         */
        volatile int waiting = 0;

        /**
         * The semaphore upon which the waiting threads wait.
         * <blockquote>
         * waitCond.down();
         * </blockquote>
         * to wait.
         * <blockquote>
         * waitCond.up();
         * </blockquote>
         * to to signal one of the waiting threads to resume execution.
         */
        Semaphore waitCond = new Semaphore(0);

        /**
         * Wait for the condition to hold. Another thread will signal when this happens.
         * @exception InterruptedException If interrupted while waiting.
         * @exception MonitorException If the thread executing this is not
         * inside the Monitor.
         */
        public void await()
        throws InterruptedException, MonitorException {
            if (current!=Thread.currentThread())
                throw new MonitorException("await()");

            int count = monitorEntryCount;
            monitorEntryCount = 0;
            current = null;
            waiting++;
            monitorEntry.up();
            waitCond.down();
            current = Thread.currentThread();
            monitorEntryCount = count;
        }

        /**
         * Signal the condition has occurred. If there are any waiting
         * threads, it signals one of them to resume execution,
         * hands over the monitor to it, and waits to reenter the monitor.
         * @exception MonitorException If the thread executing this is not
         * inside the Monitor.
         */
        public void signal()
        throws MonitorException {
            if (current!=Thread.currentThread())
                throw new MonitorException("signal()");

            if (waiting>0) {
                waiting--;
                int count = monitorEntryCount;
                monitorEntryCount = 0;
                current = null;
                waitCond.up();
                boolean interrupted = Thread.interrupted();

                for(;;) try {
                    monitorEntry.down();
                    break;

                } catch (InterruptedException ie) {
                    interrupted = true;
                }

                current = Thread.currentThread();
                monitorEntryCount = count;

                if (interrupted) current.interrupt();
            }
        }

        /**
         * Signal the condition has occurred and leaves the monitor.
         * Equivalent to
         * <blockquote>
         * cond.signal(); mon.leave();
         * </blockquote>
         * If there are any waiting
         * threads, it signals one of them to resume execution and
         * hands over the monitor to it.<p>
         *
         * If this thread has entered the monitor more than once,
         * leaveWithSignal() behaves like signal(). After the signaled
         * thread has run, the signaling thread will reenter the monitor
         * to complete its execution.
         *
         * @exception MonitorException If the thread executing this is not inside the Monitor.
         */
        public void leaveWithSignal()
        throws MonitorException {
            if (current != Thread.currentThread())
                throw new MonitorException("leaveWithSignal()");

            monitorEntryCount--;

            if (waiting > 0) {
                waiting--;

                if (monitorEntryCount > 0) {
                    int count = monitorEntryCount;
                    monitorEntryCount = 0;
                    current = null;
                    waitCond.up();
                    boolean interrupted = Thread.interrupted();

                    for(;;) try {
                        monitorEntry.down();
                        break;

                    } catch (InterruptedException ie) {
                        interrupted = true;
                    }

                    monitorEntryCount = count;
                    current = Thread.currentThread();

                    if (interrupted) current.interrupt();

                } else {
                    current = null;
                    waitCond.up();
                }

            } else {
                if (monitorEntryCount == 0) {
                    current = null;
                    monitorEntry.up();
                }
            }
        }
    }

    /**
     * Enter the monitor. Call this before accessing the fields of the
     * protected object. (The fields must be declared <b>volatile</b>.)
     */
    public void enter() {
        if (current == Thread.currentThread()) monitorEntryCount++;

        else {
            boolean interrupted = Thread.interrupted();

            for(;;) {
                try {
                    monitorEntry.down();
                    break;

                } catch (InterruptedException ie) {
                    interrupted = true;
                }
            }

            current = Thread.currentThread();
            monitorEntryCount = 1;

            if (interrupted) current.interrupt();
        }
    }

    /**
     * Leave the monitor. Do not access fields of the protected
     * object after calling leave().
     * @exception MonitorException If the thread executing this is not inside the Monitor.
     */
    public void leave()
    throws MonitorException {
        if (current != Thread.currentThread())
            throw new MonitorException("leave()");

        monitorEntryCount--;

        if (monitorEntryCount == 0) {
            current = null;
            monitorEntry.up();
        }
    }

    /**
     * Lock for a monitor left temporarily by release(). This Lock
     * can be used by the same thread to reacquire the Monitor once.
     */
    private class Lock implements MonitorLock {
        int n = monitorEntryCount;
        Thread owner = current;

        /**
         * Enter the monitor again after an earlier release.
         * @exception MonitorException If thread attempting to reacquire
         * 	a Monitor not released by this thread or if this Lock
         *  has been previously used.
         */
        public void reacquire()
        throws MonitorException {
            if (owner != Thread.currentThread())
                throw new MonitorException("attempt to reacquire Monitor by a different thread");

            boolean interrupted = Thread.interrupted();

            for(;;) try {
                    monitorEntry.down(); break;
            } catch (InterruptedException ie) {
                interrupted=true;
            }

            current = owner;
            monitorEntryCount = n;
            owner = null;

            if (interrupted) current.interrupt();
        }
    }

    /**
     * Leave the monitor temporarily. Get the entry count to supply
     * to reenter later. Note that if the thread has entered several
     * monitors, it can release and reacquire any or all of them, no
     * matter where it is in the program.
     *
     * @return The entry count--the number of times the monitor
     *  has been entered but not yet left. This value must be
     *  supplied to reacquire() when the monitor is reentered later.
     * @exception MonitorException If the thread executing this is not inside the Monitor.
     */
    public MonitorLock release()
    throws MonitorException {
        if (current != Thread.currentThread())
            throw new MonitorException("release()");

        Lock L = new Lock();
        current = null;
        monitorEntryCount = 0;
        monitorEntry.up();

        return L;
    }
}
