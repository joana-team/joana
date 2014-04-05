/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests.probch;

import java.util.LinkedList;


public final class LinearMailbox implements ReadMailbox, WriteMailbox {
    private LinkedList data; // LiFo queue
    private boolean empty;

    public LinearMailbox() {
        data = new LinkedList();
    }

    public Object clone()
    throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public synchronized void put (Object msg) throws InterruptedException {
        // make a deep copy, so the producer won't change the data underway
        Object copy = DeepCopy.copy(msg);

        while (empty == false) {    //wait till the buffer becomes empty
            try { wait(); }
            catch (InterruptedException e) {throw e;}
        }

        data.addLast(copy);
        empty = false;
//        System.out.println("Producer: put..." + msg);
        notify();
    }

    public synchronized Object get () throws InterruptedException {
        while (empty == true)  {    //wait till something appears in the buffer
            try {   wait(); }
            catch (InterruptedException e) {throw e;}
        }
        empty = true;
        notify();
        Object val = data.poll();
//        System.out.println("Consumer: got..." + val);
        return val;
    }
}
