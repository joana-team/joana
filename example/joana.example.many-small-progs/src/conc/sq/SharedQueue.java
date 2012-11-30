/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package conc.sq;
/*
To accompany High-Performance Java Platform(tm) Computing:
Threads and Networking, published by Prentice Hall PTR and
Sun Microsystems Press.

Threads and Networking Library
Copyright (C) 1999-2000
Thomas W. Christopher and George K. Thiruvathukal

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
*/
//Test SharedTableOfQueues
//one thread will write strings into a single queue
//another will remove them and check their order
//  they will be removed using a getSkip loop


class SharedQueue extends Thread {
    final static int maxMsg=1000;
    static SharedTableOfQueues<String, String> stoq = new SharedTableOfQueues<String, String>();

    public static void main(String[] x) {
        System.out.println("one thread will write strings into an array");
        System.out.println(" (one queue per element)");
        System.out.println("another will remove them and check their order");
        System.out.println("  they will be removed using a getSkip loop");

        Thread worker = new SharedQueue(true);
        worker.start();
        worker = new SharedQueue(false);
        worker.start();
    }


    // the SharedQueue Object

    int myId, next;
    boolean receiver;

    SharedQueue(boolean i_receive) {
        receiver = i_receive;
    }

    public void run() {
        try {
            String s, nextstr;
            next = 0;
            while (next <= maxMsg) {
                nextstr = ""+next;

                if (receiver) {
                    while ((s = stoq.get("queue")) == null) yield();

                    if (!s.equals(nextstr)) {
                        System.out.println("received "+s+" not "+nextstr);
                    }

                } else {
                    stoq.put("queue",nextstr);
                }

                next++;
            }

            System.out.println((receiver?"receiver ":"sender ")+" done");

        } catch(InterruptedException ie) { }
    }
}
