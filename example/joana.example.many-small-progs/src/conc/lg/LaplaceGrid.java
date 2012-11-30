/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package conc.lg;

/**
 * <h3>Jacobi Integration of Laplace's Equation</h3>
 * <p><b>Problem description:</b><br/>
 * This program solves a differential equation (Laplace's equation) over a rectangular grid.
 * It starts with an initial guess at the solution and iterates to a final solution by
 * averaging over the values of the four nearest neighbors. Each grid point is updated from
 * the previous values of its neighbors. It turns out that the convergence
 * is much faster if the update is done using more recent values of the neighbors.
 * <br/>
 * In our parallel solution, the input grid is divided into square partition and each thread
 * computes a partition. When its computation is done, it passes the border values of its partition
 * to its neighbor partitions (threads) and waits for border values of its neighbor partitions.
 * Then the thread can proceed to next iteration.
 * </p><p>
 * <b>Implementation description:</b><br>
  * ---Hand-threaded--- solution.
 *  Used synchronization primitives - synchronous message passing.
 *  Simulation of Jacobi iteration using random input values.
 * </p>
 *
 * @author Jan Miksatko (honza AT ksu edu)
 * @author Jan Antolik (jano AT ksu edu)
 * @version $Revision: 1.1 $ - $Date: 2004/06/18 17:00:26 $
 */
public class LaplaceGrid {
    /**
     * initial values, let's suppose that the N and M values are divisible by PARTITION_SIZE
     */
    public final static int N = 100;
    public final static int M = 100;
    public final static int PARTITON_SIZE = 50;               // i.e. 4 threads needed
    public final static int NUM_OF_ITERATIONS = 10000;
    public static final int LEFT = 0, TOP = 1, RIGHT = 2, BOTTOM = 3;

    public static void main(String[] args)  {
        //  double[][] grid = new double[N][M];     // matrix with input values can be intialized here
        int partitionSizeX = N / PARTITON_SIZE;
        int partitionSizeY = M / PARTITON_SIZE;

        long start = System.currentTimeMillis();

        /** synchronization for message passing between partitions */
        MessageQueue<double[]>[][][] synch = new MessageQueue[partitionSizeX][partitionSizeY][4];
        /** report the end of the computation */
        MessageQueue<double[]> done = new MessageQueue<double[]> ();

        // create sychronization objects for computation
        for (int x = 0; x < partitionSizeX; x++)
            for (int y = 0; y < partitionSizeY; y++) {
                    synch[x][y][LEFT] = (x == 0) ? null : new MessageQueue<double[]>();
                    synch[x][y][TOP] = (y == 0) ? null : new MessageQueue<double[]>();
                    synch[x][y][RIGHT] = (x == partitionSizeX - 1) ? null : new MessageQueue<double[]>();
                    synch[x][y][BOTTOM] = (y == partitionSizeY - 1) ? null : new MessageQueue<double[]>();
            }

        // start the workers
        for (int m = 0; m < partitionSizeX; m++)
            for (int n = 0; n < partitionSizeY; n++) {
                MessageQueue<double[]> in[] = new MessageQueue[4];
                in[LEFT] = (m == 0) ? null : synch[m-1][n][RIGHT];
                in[TOP] = (n == 0) ? null : synch[m][n-1][BOTTOM];
                in[RIGHT] = (m == partitionSizeX - 1) ? null : synch[m+1][n][LEFT];
                in[BOTTOM] = (n == partitionSizeY - 1) ? null : synch[m][n+1][TOP];
                (new Partition("p[" + m + ","+n+"]", PARTITON_SIZE, synch[m][n], in, done)).start();
            }


        System.out.println("Computing...");
        // let the workers finish their job
        for (int m = 0; m < partitionSizeX; m++)
            for (int n = 0; n < partitionSizeY; n++)
                // results of the computation of the partition can be recieved here
                done.recieve();
        System.out.println("Work done in [ms]: " + (System.currentTimeMillis() - start));
    }
}
