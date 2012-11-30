/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package conc.ds;

/**
 * The DiskScheduler provides a sample implementation of
 * a simple disk scheduling algorithm that was published
 * in Gregory R. Andrews book Concurrent Programming
 * Principles and Practice.  To use this class, you should
 * follow these simple steps to get access to the disk:
 * <ol>
 * <li>DiskScheduler diskScheduler = new DiskScheduler()</li>
 * <li>diskScheduler.request(cylinder)</li>
 * <li>-access the disk-</li>
 * <li>diskScheduler.release()</li>
 * </ol>
 * This provides the caller exclusive access to
 * the disk.
 *
 * @author Todd Wallentine &lt;tcw@cis.ksu.edu&gt;
 * @version $Revision: 1.2 $ - $Date: 2004/02/06 01:45:33 $
 *
 * @observable
 *   EXP DiskMutex(this): (this.count <= 1);
 *   EXP SingleAccess(this): (this.count == 1);
 */
public class DiskScheduler {
    private int position; // this should be private!
    private OrderedQueue currentQ;      // this should be private!
    private OrderedQueue nextQ;      // this should be private!
    private int count;

    /**
     * Create a new DiskScheduler and initialize the position and the
     * two queues.
     *
     * @post count == 0
     * @post position == -1
     * @post currentQ != null
     * @post nextQ != null
     *
     * @assert
     *   POST Initialized: (count == 0 && position == -1 && currentQ != null && nextQ != null);
     */
    public DiskScheduler() {
        count = 0;
        position = -1;
        currentQ = new OrderedQueue();
        nextQ = new OrderedQueue();
    }

    /**
     * Request the disk to be placed at the desired cylinder.  This
     * will only return when that is true.
     *
     * @post cylinder == position
     * @pre currentQ != null
     * @pre nextQ != null
     *
     * @assert
     *    POST CylinderAtRequestedPosition: (cylinder == position);
     * @observable
     *    RETURN Return(this);
     *    INVOKE Invoke(this);
     */
    public synchronized void request(int cylinder) {
        if (position == -1) {
            // the head is at the default position
            position = cylinder;

        } else if ((position != -1) && (cylinder > position)) {
            // the head is at a position that is not what we requested so
            //  our request is added to the current queue
            currentQ.enqueue(cylinder);

            // await(position == cylinder)
            while (position != cylinder) {
                try {
                    wait();

                } catch(InterruptedException ie) { }
            }

        } else if ((position != -1) && (cylinder <= position)) {
            // the head is at a position that is not what we requested or
            //  is already in use so our request is add to the next queue
            nextQ.enqueue(cylinder);

            // await(position == cylinder)
            while (position != cylinder) {
                try {
                    wait();

                } catch(InterruptedException ie) { }
            }
        }

        count++;
    }

    /**
     * Release the hold of the disk at the current position.
     *
     * @pre currentQ != null
     * @pre nextQ != null
     * @post The position is at the next requested position if one exists.  If not, position == -1.
     *
     * @observable
     *    RETURN Return(this);
     *    INVOKE Invoke(this);
     */
    public synchronized void release() {
        if (currentQ.size() > 0) {
            // since there is something in the current queue, we
            //  will take the next request and move the head to
            //  to that position
            position = currentQ.dequeue();

        } else if ((currentQ.size() == 0) && (nextQ.size() > 0)) {
            // since there is nothing in the current queue but
            //  something in the next queue, we will set the
            //  next queue to the current queue and get the
            //  next request
            OrderedQueue temp = currentQ;
            currentQ = nextQ;
            nextQ = temp;
            position = currentQ.dequeue();

        } else if ((currentQ.size() == 0) && (nextQ.size() == 0)) {
            // since there are not requests waiting, set the heads position
            //  to the default so the next request will succeed without waiting
            position = -1;
        }

        count--;

        // now notify all those waiting on a request
        notifyAll();
    }
}
