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

import java.util.Stack;

/**
A class used to provide FIFO queues for the other classes
in the thread package. THIS IS NOT THREAD-SAFE. DO NOT
USE THIS ALONE FOR INTER-THREAD COMMUNICATION. This
must be contained within a locked object.
@author Thomas W. Christopher (Tools of Computing LLC)
@version 0.2 Beta
*/

public class QueueComponent<Value> {
    Stack<Value> hd = new Stack<Value>();
    Stack<Value> tl = new Stack<Value>();

    /**
     * Removes and returns the first element in the queue.
     * @exception EmptyQueueException if the queue is empty.
     */
    public Value get()
    throws EmptyQueueException {
        if (!hd.empty()) return hd.pop();

        while (!tl.empty()) hd.push(tl.pop());
        if (hd.empty()) throw new EmptyQueueException();

        return hd.pop();
    }

    /**
     * Returns a reference to the first element in the queue without removing it.
     * @exception EmptyQueueException if the queue is empty.
     */
    public Value firstElement()
    throws EmptyQueueException {
        if (!hd.empty()) return hd.peek();

        while (!tl.empty()) hd.push(tl.pop());
        if (hd.empty()) throw new EmptyQueueException();

        return hd.peek();
    }

    /**
     * Enqueues its parameter.
     * @param elem the value to be enqueued.
     */
    public void put(Value elem) {
        tl.push(elem);
    }

    /**
     * Returns true if the queue is empty; false, if it is not empty.
     */
    public boolean isEmpty() {
        return hd.empty() && tl.empty();
    }

    /**
     * Removes all elements from the queue, leaving it empty.
     */
    public void clear() {
        hd.setSize(0); tl.setSize(0);
    }

    /**
     * Tests the queue.
     */
    public static void main(String[] args)
    throws NumberFormatException {
        int total,init;
        int i;
        QueueComponent<String> q = new QueueComponent<String>();
        String s="abc";
        for (i = 0; i < s.length(); i++) {
            q.put(s.substring(i, i+1));
        }

        String t="";
        while (!q.isEmpty()) {
            t += q.get();
        }

        if (!s.equals(t)) {
            System.out.println("Bug. Put in \"" + s + "\", got \"" + t + "\"");

        } else {
            System.out.println("Tests okay.");
        }

        if (args.length < 1) {
            System.out.println("usage: java QueueComponent total [init]");
            System.exit(0);
        }

        total = Integer.parseInt(args[0]);
        init = args.length<2 ? 0 : Integer.parseInt(args[1]);
        if (total<init) {
            System.out.println(
            "total elements to enqueue must be greater than initial");
            System.exit(0);
        }

        q = new QueueComponent<String>();

        long startTime=System.currentTimeMillis();
        for (i = 0; i < init; i++) {
            q.put("X");
        }

        for (; i < total; i++) {
            q.put("X");
            q.get();
        }

        while (!q.isEmpty()) {
            q.get();
        }

        System.out.println(System.currentTimeMillis() - startTime);
    }
}
