/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package conc.ds;

/**
 * The OrderedQueue class provides a very crude int queue
 * implementation that provides ordering of the items in the queue.
 * This will expand as the amount of data
 * in the queue increases but will never shrink.
 *
 * This is not a fast implementation by any means but it should
 * provide the functionality that is needed.  It will not order
 * the elements as they are entered but will return the lowest value
 * in the queue when dequeue is called.
 *
 * @author Todd Wallentine &lt;tcw@cis.ksu.edu&gt;
 * @version $Revision: 1.1.1.1 $ - $Date: 2003/10/21 21:00:42 $
 */
public class OrderedQueue {
    private int[] q;
    private int head;
    private int tail;
    private int size;
    private static final int INIT_Q_SIZE = 3;

    /**
     * Create a new empty queue.
     */
    public OrderedQueue() {
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
        if (size == q.length) {
            int[] temp = new int[2 * q.length];

            for (int k = 0; k < size; k++) {
                temp[k] = q[head];
                if(head == q.length - 1) {
                    head = 0;

                } else {
                    head++;
                }
            }

            q = temp;
            head = 0;
            tail = size - 1;
        }

        q[tail] = i;
        if (tail == q.length - 1) {
            tail = 0;

        } else {
            tail++;
        }

        size++;
    }

    /**
     * Remove the lowest element in the queue.
     *
     * @return int The lowest value in the queue.
     * @post The return value is lower than all other values in the queue currently.
     */
    public int dequeue() {
        // find the lowest value in the queue and swap it with the current head
        if (size > 1) {
            int lowIndex = head;
            int currentIndex = head + 1;

            for (int i = 0; i < size - 1; i++) {
                if(q[currentIndex] < q[lowIndex]) {
                    lowIndex = currentIndex;
                }

                if(currentIndex == q.length - 1) {
                    // we have found the end of the array and need to start at
                    //  the beginning.
                    currentIndex = 0;

                } else {
                    currentIndex++;
                }
            }

            if (lowIndex != head) {
                int temp = q[head];
                q[head] = q[lowIndex];
                q[lowIndex] = temp;
            }
        }

        int returnValue = q[head];
        if (head == q.length - 1) {
            head = 0;

        } else {
            head++;
        }
        size--;

        return(returnValue);
    }

    public void print() {
        if (q == null) {
            System.out.println("Error.");

        } else if(size == 0) {
            System.out.println("Empty.");

        } else {
            int j = head;

            for (int i = 0; i < size; i++) {
                System.out.println("queue[" + i + "] = " + q[j]);

                if(j == q.length - 1) {
                    j = 0;

                } else {
                    j++;
                }
            }
        }
    }
}
