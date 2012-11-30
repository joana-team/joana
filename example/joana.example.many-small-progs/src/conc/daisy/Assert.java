/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * This file is part of the Daisy distribution.  This software is
 * distributed 'as is' without any guarantees whatsoever. It may be
 * used freely for research but may not be used in any commercial
 * products.  The contents of this distribution should not be posted
 * on the web or distributed without the consent of the authors.
 *
 * Authors: Cormac Flanagan, Stephen N. Freund, Shaz Qadeer
 * Contact: Shaz Qadeer (qadeer@microsoft.com)
 */

package conc.daisy;

/**
 * Various routines for testing assertions and reporting errors.
 */
public class Assert {
    static boolean useException = false;

    /**
     * If passed true, assertion failures will generate Exceptions
     * rather than terminating execution.
     */
    static public void assertFailWithException(boolean b) {
        useException = b;
    }

    /**
     * If passed false, this method well report an assertion failure
     * and exit the program.
     */
    //@ ensures b
    static public void notFalse(boolean b) {
        if (!b) {
            String s = "assertion failed\n" + Debug.getStackDump();
            if (useException) {
                throw new RuntimeException(s);
            } else {
                System.err.println(s);
                ShutDown.exit(0);
            }
        }
    }

    /**
     * If passed false, this method well report an assertion failure,
     * print the string passed in, and exit the program.
     */
    //@ ensures b
    static public void notFalse(boolean b, String e) {
        if (!b) {
            String s = "assertion failed: " + e + "\n" + Debug.getStackDump();
            if (useException) {
                throw new RuntimeException(s);
            } else {
                System.err.println(s);
                ShutDown.exit(0);
            }
        }
    }

    /**
     * Prints the exception e, dumps the stack where e occurred, and exits.
     */
    //@ ensures false
    static public void fail(Throwable e) {
        System.err.println("fail: " + e);
        e.printStackTrace();
        ShutDown.exit(0);
    }

    /**
     * Prints the exception e and message s, dumps the stack where e
     * occurred, and exits.
     */
    //@ ensures false
    static public void fail(Throwable e, String s) {
        System.err.println("fail: " + s);
        fail(e);
    }

    /**
     * Prints the message s, dumps the current stack, and exits.
     */
    //@ ensures false
    static public void fail(String s) {
        fail(new Throwable(), s);
    }

    /**
     * Same as fail, but does not exit.
     */
    static public void notify(String s) {
        System.err.println(s);
    }

    static public void notify(Throwable e) {
        System.err.println("notify: " + e);
        e.printStackTrace();
    }

    static public void notify(Throwable e, String s) {
        System.err.println("notify: " + s);
        notify(e);
    }
}
