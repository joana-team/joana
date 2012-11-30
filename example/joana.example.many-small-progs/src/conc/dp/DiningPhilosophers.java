/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package conc.dp;


class DiningPhilosophers {
    //~ Methods ................................................................

    public static void main(String[] args) {
        int numPhilosophers = 5;

        if ((args != null) && (args.length > 0)) {
        	numPhilosophers = Integer.parseInt(args[0]);
        }

        boolean checkStarving = true;

        // create the DiningServer object
        DiningServer ds = new DiningServer(numPhilosophers, checkStarving);

        // create the Philosophers (they have self-starting threads)
        for (int i = 0; i < numPhilosophers; i++) {
            new Philosopher(i, ds);
        }
    }
}
