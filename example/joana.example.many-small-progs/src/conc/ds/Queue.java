/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package conc.ds;

/**
 * The Queue class provides a very crude int queue
 * implementation.  This will expand as the amount of data
 * in the queue increases but will never shrink.
 *
 * @author Todd Wallentine &lt;tcw@cis.ksu.edu&gt;
 * @version $Revision: 1.1.1.1 $ - $Date: 2003/10/21 21:00:42 $
 */
public class Queue {
    private int[] q;
    private int head;
    private int tail;
    private int size;
    private static final int INIT_Q_SIZE = 3;

    /**
     * Create a new empty queue.
     */
    public Queue() {
        q = new int[INIT_Q_SIZE];
        head = 0;
        tail = 0;
        size = 0;
    }

    /**
     * Get the size of the queue.
     */
    public int size() {
        return(size);
    }

    /**
     * Add the int to the queue.
     */
    public void enqueue(int i) {
        // if we are full, increase the size of the q
        if(head == tail) {
            int[] temp = new int[2 * q.length];
            boolean done = false;
            int j = 0;

            while(!done) {
                temp[j++] = q[head];

                if(head == tail) {
                    done = true;
                }

                if(head == q.length - 1) {
                    head = 0;
                } else {

                    head++;
                }
            }
        }

        if(tail == q.length - 1) {
            tail = 0;

        } else {
            tail++;
        }

        q[tail] = i;
        size++;
    }

    /**
     * Remove the first element in the queue and return it.
     */
    public int dequeue() {
        int returnValue = q[head];

        if(head == q.length - 1) {
            head = 0;

        } else {
            head++;
        }

        size--;
        return(returnValue);
    }
}
