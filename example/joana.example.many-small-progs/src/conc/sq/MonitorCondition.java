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
 *  Interface implemented by Monitor.Condition.
 *  @author Thomas W. Christopher (Tools of Computing LLC)
 *  @version 0.2 Beta
 */

public interface MonitorCondition {
    /**
     * Wait for the condition to hold. Another thread will signal when this happens.
     * @exception InterruptedException If interrupted while waiting.
     * @exception MonitorException If the thread executing this is not inside the Monitor.
     */
    public void await()
    throws InterruptedException, MonitorException;

    /**
     * Signal the condition has occurred. If there are any waiting
     * threads, it signals one of them to resume execution,
     * hands over the monitor to it, and waits to reenter the monitor.
     * @exception InterruptedException If interrupted while trying to reenter the monitor.
     * @exception MonitorException If the thread executing this is not inside the Monitor.
     */
    public void signal()
    throws InterruptedException, MonitorException;

    /**
     * Signal the condition has occurred and leaves the monitor.
     * Equivalent to
     * <blockquote>
     * cond.signal(); mon.leave();
     * </blockquote>
     *
     * If there are any waiting threads, it signals one of them to resume execution and
     * hands over the monitor to it.<p>
     *
     * If this thread has entered the monitor more than once, leaveWithSignal() behaves
     * like signal(). After the signaled thread has run, the signaling thread will
     * reenter the monitor to complete its execution.
     *
     * @exception InterruptedException If interrupted while trying to reenter the monitor.
     * @exception MonitorException If the thread executing this is not inside the Monitor.
     */
    public void leaveWithSignal()
    throws InterruptedException, MonitorException;
}
