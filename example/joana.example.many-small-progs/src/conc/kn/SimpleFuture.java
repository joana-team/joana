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

/**
 * An assign-once variable that allows consumers to wait for a value to be produced.
 * @author Thomas W. Christopher (Tools of Computing LLC)
 * @version 0.2 Beta
 */

public class SimpleFuture {
    /**
     * The assign-once variable.
     */
    protected Object value;

    /**
     * Create a SimpleFuture with no value yet assigned.
     */
    public SimpleFuture() {
        value = this;
    }

    /**
     * Create a SimpleFuture with a value initially assigned.
     * @param val The value the SimpleFuture is to be initialized with.
     */
    public SimpleFuture(Object val) {
        value = val;
    }

    /**
     * Waits until a value has been assigned to the SimpleFuture, then returns it.
     * @return The value assigned.
     * @exception InterruptedException if the thread is interrupted while waiting
     *  for a value to be assigned.
     */
    public synchronized Object getValue()
    throws InterruptedException {
        while (value == this) wait();
        return value;
    }

    /**
     * Checks to see if a value has been assigned to the SimpleFuture yet.
     * @return true if value has been assigned, false otherwise.
     */
    public synchronized boolean isSet() {
        return (value != this);
    }

    /**
     * Assigns a value to the SimpleFuture and notifies all waiting threads.
     * Attempts to change a previously assigned value will be ignored.
     * @param val The value to be assigned to the SimpleFuture.
     */
    public synchronized void setValue(Object val) {
        if (value != this) return;
        value = val;
        notifyAll();
    }
}
