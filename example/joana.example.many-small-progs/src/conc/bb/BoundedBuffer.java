/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package conc.bb;

class BoundedBuffer { // designed for a single producer thread
  // and a single consumer thread

    //~ Instance variables .....................................................

    protected int numSlots = 0;
    protected Object[] buffer = null;
    protected int putIn = 0;
    protected int takeOut = 0;
    protected int count = 0;

    //~ Constructors ...........................................................

    /*@ invariant count <= numSlots;
      @ invariant count >= 0;
      @ invariant buffer.length == numSlots;
      @*/

    /*@ behavior
       @   requires numSlots > 0;
       @   assignable this.numSlots, this.buffer, this.putIn, this.takeOut, this.count;
       @   ensures \fresh(buffer) && buffer.length == numSlots && this.numSlots == numSlots;
       @also
       @ behavior
       @   requires numSlots <= 0;
       @   signals (Exception e) e instanceof IllegalArgumentException;
       @*/
    public BoundedBuffer(int numSlots) {
        if (numSlots <= 0) {
            throw new IllegalArgumentException();
        }

        this.numSlots = numSlots;
        buffer = new Object[numSlots];
    }

    //~ Methods ................................................................

    /*@ behavior
       @   requires value != null;
       @   when count != numSlots;
       @   assignable buffer[*], putIn, count;
       @   ensures  putIn >= 0 && putIn < numSlots;
       @*/
    public synchronized void deposit(Object value) {
        while (count == numSlots) {
            try {
                wait();
            } catch (InterruptedException e) { }
        }

        buffer[putIn] = value;
        putIn = (putIn + 1) % numSlots;
        count++; // wake up the consumer

        if (count == 1) {
            notify(); // since it might be waiting
        }
    }

    /*@ behavior
       @   when count != 0;
       @   assignable buffer[*], takeOut, count;
       @   ensures takeOut >= 0 && takeOut < numSlots &&
       @           \result != null;
       @*/
    public synchronized Object fetch() {
        Object value;

        while (count == 0) {
            try {
                wait();
            } catch (InterruptedException e) { }
        }

        value = buffer[takeOut];
        takeOut = (takeOut + 1) % numSlots;
        count--; // wake up the producer

        if (count == (numSlots - 1)) {
            notify(); // since it might be waiting
        }

        return value;
    }
}
