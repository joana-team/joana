/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package conc.ds;

/**
 * The DiskSchedulerDriver provides the main method that will
 * drive the DiskScheduler example.  It will create a set of
 * disk readers and give them a common disk scheduler.  It will
 * then start all the readers and wait for them to complete.
 *
 * Since this is the main entry point, we will list the properties
 * that are of interest to this application.
 * <dl>
 * <dt>
 * The disk scheduler should only allow one disk read to access the disk at a time.
 * </dt>
 * <dd>
 * The mutual exclusion property is difficult to specify without modifying the
 * DiskScheduler implementation.  To do this, we will add a counter to the DiskScheduler
 * class.  We will then increment it before we return from request and decrement it before
 * we return from release.  This value should never be greater than 1.  We will implement
 * this check with an observable expression that says (count <= 1) and use the Universality,
 * Globally pattern.  We can specify an observable expression for the DiskScheduler named
 * DiskMutex that has an expression: (count <= 1).  This will hold for all DiskSchedulers.
 * </dd>
 *
 * <dt>
 * Each disk reader should be treated fairly so it doesn't get skipped more than once.
 * </dt>
 * <dd>
 * The fairness claim is a difficult one to implement with just BSL.  We need
 * to suppliment the implementation with a few variables.  First, when we place a request
 * into the queues, we will also place another value into the queue with it.  This value
 * will represent the pass in which the request was made.  We will then test this value
 * when we dequeue an item to make sure it wasn't skipped more than a single time.  This means
 * that a new variable will be added to the DiskScheduler that will keep track of the current pass
 * and increment it when a swap of the queues is done.  This can be implemented as a simple
 * LOCATION assertion in BSL.
 * </dd>
 *
 * <dt>
 * Each disk reader should eventually get it's request granted (no starvation).
 * </dt>
 * <dd>
 * To implement this, we will create an INVOKE predicate and a RETURN predicate for
 * the request method in the DiskScheduler.  We will then say that the return responds
 * to the invoke globally (Response, Globally).
 * </dd>
 *
 * <dt>
 * Each disk reader that is granted access to the disk will eventually release the disk.
 * </dt>
 * <dd>
 * To implement this, we will create an INVOKE predicate on request and a RETURN predicate
 * on the release.  We will then say that return responds to the invoke globally (Response, Globally).
 * </dd>
 *
 * <dt>
 * In this example, each disk reader should complete (since it has a set number of reads
 * to perform before it quits.  This is similar to starvation.
 * </dt>
 * <dd>
 * The easiest of these to test for is the completion of each disk reader.  This can
 * be ensured by a property that says that for all disk readers, they return from
 * the run method.  So we create a RETURN predicate for the run method and use the
 * Existence, Globally pattern for each DiskReader.
 * </dd>
 *
 * <dt>
 * The disk head should not move while a disk reader is reading it.
 * </dt>
 * <dd>
 * </dd>
 *
 * <dt>
 * The head should continue to move in the same direction until it reaches the end of the disk
 * and then resets to the start.
 * </dt>
 * <dd>
 * This can be implemented by making sure that the value that was taken before the current value
 * is less than the current value. To do this, we will need to modify the DiskScheduler by adding
 * a field that will keep track of the last value.  When we dequeue, we will test the current value
 * the last value to make sure the current value is always larger.  [We need to skip the first access
 * after a reset.] ???
 * </dd>
 * </dl>
 *
 * @author Todd Wallentine &lt;tcw@cis.ksu.edu&gt;
 * @version $Revision: 1.1.1.1 $ - $Date: 2003/10/21 21:00:42 $
 */
public class DiskSchedulerDriver {
    /**
     * The maximum number of readers to create.
     */
    private static final int MAX_READERS = 3;

    /**
     * The main entry point of the example which will create and start
     * all the disk readers.
     *
     * @param String[] args The command line arguments that are ignored.
     */
    public static void main(String[] args) {
        DiskScheduler ds = new DiskScheduler();

        // create the disk readers
        DiskReader[] diskReaders = new DiskReader[MAX_READERS];
        for (int i = 0; i < MAX_READERS; i++) {
            diskReaders[i] = new DiskReader(ds);
        }

        // start the disk readers
        for (int i = 0; i < MAX_READERS; i++) {
            diskReaders[i].start();
        }

    }
}
