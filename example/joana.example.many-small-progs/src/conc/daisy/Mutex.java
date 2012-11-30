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

class Mutex {

    boolean locked;
    int id;

    public Mutex(int id) {
        this.id = id;
        this.locked = false;
    }

    synchronized void acq() {
        Log.log("mutex", "acq " + id + " " + Thread.currentThread());

        while (locked) {
            try {
                this.wait();
            } catch (Exception e) {
                System.out.println(e);
            }
        }

        locked = true;
    }

    synchronized void rel() {
        Log.log("mutex", "rel " + id + " " + Thread.currentThread());
        locked = false;
        this.notify();
    }
}
