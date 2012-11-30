/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package conc.ds;

import java.util.Random;

/**
 * The DiskReader class provides a thread that will actually
 * read from the disk after it requests access to the disk and
 * will then release the disk to the next reader.  It will
 * make random requests on the head until it has reached the
 * maximum number of requests per reader.
 *
 * @author Todd Wallentine &lt;tcw@cis.ksu.edu&gt;
 * @version $Revision: 1.2 $ - $Date: 2004/02/06 01:45:33 $
 *
 * @observable
 *   EXP Active(this): (active == true);
 */
public class DiskReader extends Thread {

    /**
     * The maximum number of reads any one reader can perform.
     */
    public static final int MAX_READS = 5;

    /**
     * The maximum number of cylinders that exist on the disk.
     */
    public static final int MAX_CYLINDER = 5;

    /**
     * The DiskScheduler to use to get access to a cylinder on the
     * disk.
     */
    private DiskScheduler diskScheduler;

    /**
     * A flag to tell if this disk reader is active.
     */
    private boolean active;

    /**
     * Create a new disk reader that will use the given disk scheduler
     * to get access to the disk.
     *
     * @param DiskScheduler ds The disk scheduler to use to get access to the disk.
     */
    public DiskReader(DiskScheduler ds) {
        diskScheduler = ds;
    }

    /**
     * The run method will randomly select cylinders to read from
     * using MAX_CYLINDER as the max number of cylinders available
     * on the disk and Bandera.randomInt(int) to choose from that
     * set({0..MAX_CYLINDER}).  For each iteration in the loop
     * this will request and the release the cylinder.
     *
     * @observable
     *   RETURN Return(this);
     */
    public void run() {
    	Random random = new Random();

    	for (int i = 0; i < MAX_READS; i++) {
    	    // randomly grab a cylinder to read from
    	    int cylinder = random.nextInt(MAX_CYLINDER);

    	    // request access to the cylinder
    	    diskScheduler.request(cylinder);
    	    active = true;

    	    // play with the disk

    	    // release the disk scheduler to move to another cylinder
    	    diskScheduler.release();
    	    active = false;
    	}
    }
}
