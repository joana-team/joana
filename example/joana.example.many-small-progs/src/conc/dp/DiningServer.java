/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package conc.dp;

class DiningServer {
    //~ Static variables/initializers ..........................................
    protected static final int THINKING = 0;
    protected static final int HUNGRY = 1;
    protected static final int STARVING = 2;
    protected static final int EATING = 3;

    //~ Instance variables .....................................................
    protected boolean checkStarving = false;
    protected int numPhils = 0;
    protected int[] state = null;

    //~ Constructors ...........................................................
    /*@ invariant !(\forall int i; i >= 0 && i < state.length; state[i] == EATING);
      @ invariant !(\forall int i; i >= 0 && i < state.length; state[i] == STARVING);
      @*/

    /*@ behavior
      @   assignable this.numPhils, this.checkStarving, state;
      @   ensures this.numPhils == numPhils && this.checkStarving == checkStarving &&
      @           \fresh(state) && state.length == numPhils;
      @*/
    public DiningServer(int numPhils, boolean checkStarving) {
        this.numPhils = numPhils;
        this.checkStarving = checkStarving;
        state = new int[numPhils];

        for (int i = 0; i < numPhils; i++) {
            state[i] = THINKING;
        }
    }

    //~ Methods ................................................................

    /*@ behavior
      @   requires state[i] == EATING;
      @   assignable state[*];
      @   ensures state[i] == THINKING;
      @*/
    public synchronized void putForks(int i) {
        state[i] = THINKING;
        test(left(i), checkStarving);
        test(right(i), checkStarving);
        notifyAll();
    }

    /*@ behavior
      @   requires state[i] == THINKING;
      @   assignable state[i];
      @   ensures state[i] == EATING;
      @*/
    public synchronized void takeForks(int i) {
        state[i] = HUNGRY;
        test(i, false);

        while (state[i] != EATING) {
            try {
                wait();

            } catch (InterruptedException e) { }
        }
    }

    /*@ behavior
      @   ensures \result == (numPhils + i -1) % numPhils;
      @*/
    protected /*@ pure @*/ final int left(int i) {
        return ((numPhils + i) - 1) % numPhils;
    }

    /*@ behavior
      @   ensures \result == (i + 1) % numPhils;
      @*/
    protected /*@ pure @*/ final int right(int i) {
        return (i + 1) % numPhils;
    }

    /*@ behavior
      @   requires state[k] == HUNGRY && state[left(k)] != STARVING && state[right(k)] != STARVING;
      @   assignable state[k];
      @   ensures state[k] == STARVING;
      @ also
      @   requires state[k] != HUNGRY || state[left(k)] == STARVING || state[right(k)] == STARVING;
      @   assignable \nothing;
      @   ensures \not_modified(state[k]);
      @*/
    protected void seeIfStarving(int k) {
        if ((state[k] == HUNGRY)
                && (state[left(k)] != STARVING)
                && (state[right(k)] != STARVING)) {

            state[k] = STARVING;
        }
    }

    /*@ behavior
      @   requires state[left(k)] != EATING && state[left(k)] != STARVING &&
      @            (state[k] == HUNGRY || state[k] == STARVING) &&
      @            state[right(k)] != STARVING && state[right(k)] != EATING;
      @   assignable state[k];
      @   ensures state[k] == EATING;
      @ also
      @   requires state[left(k)] == EATING || state[left(k)] == STARVING ||
      @            (state[k] != HUNGRY && state[k] != STARVING) ||
      @            state[right(k)] == STARVING || state[right(k)] == EATING &&
      @            checkStarving;
      @   assignable state[k];
      @   ensures state[k] == STARVING || \not_modified(state[k]);
      @ also
      @   requires state[left(k)] == EATING || state[left(k)] == STARVING ||
      @            (state[k] != HUNGRY && state[k] != STARVING) ||
      @            state[right(k)] == STARVING || state[right(k)] == EATING &&
      @            !checkStarving;
      @   assignable \nothing;
      @   ensures \not_modified(state[k]);
      @*/
    protected void test(int k, boolean checkStarving) {
        if ((state[left(k)] != EATING) && (state[left(k)] != STARVING)
                && ((state[k] == HUNGRY) || (state[k] == STARVING))
                && (state[right(k)] != STARVING) && (state[right(k)] != EATING)) {

            state[k] = EATING;

        } else if (checkStarving) {
            seeIfStarving(k); // simplistic naive check for starvation
        }
    }
}
