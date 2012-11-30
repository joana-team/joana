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

import java.util.Vector;

/**
 * This class provide services for gracefully shutting down a program.
 * Depending upon finalizers to deallocate resources correctly is
 * problematic sense as the program shuts down, there's no guarantee
 * that they will be called while the program is in a reasonable state.
 *
 * This class provides a way to run code while the system exits but
 * before objects would be finalized during shutdown.
 */
public class ShutDown {
    /** list of runnable objects */
    static private Vector<Runnable> v = new Vector<Runnable>();

    /** add runnable object r to the list of objects to execute prior to shut down */
    static public void runOnExit(Runnable r) {
        v.addElement(r);
    }

    /**
     * Replaces {@link System#exit(int)} with a version that executes the previously
     * recorded runnable objects.
     */
    static public void exit(int status) {
        for (int i = 0; i < v.size(); i++) {
            Runnable r = v.elementAt(i);
            r.run();
        }
        System.exit(status);
    }
}
