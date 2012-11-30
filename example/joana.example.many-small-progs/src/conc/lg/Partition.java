/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package conc.lg;
import java.util.Random;


/**
 * Parition computation<br>
 * (assertions added to check the correctness of synch)
 *
 * @author Jan Miksatko
 * @author Jan Antolik
 */
class Partition extends Thread {
    private int size;
    private String id;
    private double values[][];
    private double tempValues[][];
    private MessageQueue<double[]>[] out, in;
    private MessageQueue<double[]> listener;
    private int iter;       // current iteration - just for assertion checking

    /**
     * Initialize partition (input values can be passed here
     * @param size          size of partition
     * @param outNeighbours     neighbours to send results to
     * @param inNeighbours      neighbours to recieve results from
     * @param resultListener    listener that recieves a notification when the computation is finished
     */
    public Partition(String id, int size, MessageQueue<double[]>[] outNeighbours,
            MessageQueue<double[]>[] inNeighbours, MessageQueue<double[]> resultListener) {

        this.size = size;
        this.id = id;
        this.out = outNeighbours;
        this.in = inNeighbours;
        listener = resultListener;

        // dummy values
        Random rnd = new Random();
        values = new double[size+2][size+2];
        for (int i = 1; i < values.length - 1; i++) {
            for (int j = 1; j < values[i].length - 1; j++) {
                values[i][j] = rnd.nextDouble() * (10 * LaplaceGrid.M * LaplaceGrid.N);
            }
        }

        // init shadow
        for (int i = 0; i < this.size +2 ; i++) {
            values[0][i]=0;
            values[i][0]=0;
            values[this.size+1][i]=0;
            values[i][this.size+1]=0;
        }

        tempValues = new double[size][size];
    }


    /**
     * Jaccobi iteration itself
     */
    public void run() {
        System.out.println(id + " computation of a partition started");
        for (iter = 0; iter < LaplaceGrid.NUM_OF_ITERATIONS; iter++) {
            sendLeft();
            sendTop();
            sendRight();
            sendBottom();

            recieveLeft();
            recieveTop();
            recieveRight();
            recieveBottom();

            compute();
//            System.out.println(id + " iteration: " + iter);
        }


        System.out.println(id + " computation of a partition finished, notifying the listener");
        listener.send(new double[0]);
    }

    private void compute() {
        for(int i=1; i <= size; i++)
            for(int j=1; j <= size; j++)
                tempValues[i-1][j-1] = (values[i-1][j] + values[i+1][j] + values[i][j+1] + values[i][j-1]) / 4;
        for(int i=1; i <= size; i++)
            for(int j=1; j <= size; j++)
                values[i][j] = tempValues[i-1][j-1];
    }

    private void recieveLeft() {
        if (in[LaplaceGrid.LEFT] != null) {
            double[] t = in[LaplaceGrid.LEFT].recieve();       // without assertion checking
/*            Object[] val = (Object[]) in[LaplaceGrid.LEFT].recieve();
            assert ((Integer)val[1]).intValue() - iter < 1;
            double t[] = (double[]) val[0];                 */

            for (int i = 0; i < size; i++)
                  values[i+1][0] = t[i];
        }
    }

    private void sendLeft() {
        if (out[LaplaceGrid.LEFT] != null) {
            double[] t = new double[size];
            for (int i = 0; i < size; i++)
                  t[i] = values[i+1][1];
            out[LaplaceGrid.LEFT].send(t);                                // without assertion checking
//            out[LaplaceGrid.LEFT].send(new Object[] {t, new Integer(iter)});
        }
    }

    private void recieveRight() {
        if (in[LaplaceGrid.RIGHT] != null) {
            double[] t = in[LaplaceGrid.RIGHT].recieve();
            /*Object[] val = (Object[]) in[LaplaceGrid.RIGHT].recieve();
            assert ((Integer)val[1]).intValue() - iter < 1;
            double t[] = (double[]) val[0]; */
            for (int i = 0; i < size; i++)
                  values[i+1][size+1] = t[i];
        }
    }

    private void sendRight() {
        if (out[LaplaceGrid.RIGHT] != null) {
            double[] t = new double[size];
            for (int i = 0; i < size; i++)
                  t[i] = values[i+1][size];
            out[LaplaceGrid.RIGHT].send(t);
//            out[LaplaceGrid.RIGHT].send(new Object[] {t, new Integer(iter)});
        }
    }

    private void recieveTop() {
        if (in[LaplaceGrid.TOP] != null) {
            double[] t = in[LaplaceGrid.TOP].recieve();
/*
            Object[] val = (Object[]) in[LaplaceGrid.TOP].recieve();
            assert ((Integer)val[1]).intValue() - iter < 1;
            double t[] = (double[]) val[0];
*/
            for (int i = 0; i < size; i++)
                  values[0][i+1] = t[i];
        }
    }

    private void sendTop() {
        if (out[LaplaceGrid.TOP] != null) {
            double[] t = new double[size];
            for (int i = 0; i < size; i++)
                  t[i] = values[1][i+1];
            out[LaplaceGrid.TOP].send(t);
//            out[LaplaceGrid.TOP].send(new Object[] {t, new Integer(iter)});
        }
    }

    private void recieveBottom() {
        if (in[LaplaceGrid.BOTTOM] != null) {
            double[] t = in[LaplaceGrid.BOTTOM].recieve();
/*
            Object[] val = (Object[]) in[LaplaceGrid.BOTTOM].recieve();
            assert ((Integer)val[1]).intValue() - iter < 1;
            double t[] = (double[]) val[0];
*/
            for (int i = 0; i < size; i++)
                  values[size+1][i+1] = t[i];
        }
    }

    private void sendBottom() {
        if (out[LaplaceGrid.BOTTOM] != null) {
            double[] t = new double[size];
            for (int i = 0; i < size; i++)
                  t[i] = values[size][i+1];
            out[LaplaceGrid.BOTTOM].send(t);
//            out[LaplaceGrid.BOTTOM].send(new Object[] {t, new Integer(iter)});
        }
    }
}

