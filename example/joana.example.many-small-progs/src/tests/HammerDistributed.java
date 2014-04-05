/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests;

import sensitivity.Security;

public class HammerDistributed {
    public static void main (String[] args) {
        int sec = Security.SECRET;
        int pub = Integer.parseInt(args[2]);

        // 1. no information flow
        HammerA o = new HammerA();
        o.set(sec);
        o = new HammerA();
        o.set(pub);
        Security.PUBLIC = o.get();

        // 2. dynamic dispatch
        if (sec == 0 && args[0].equals("007")) {
            o = new HammerB();
        }
        o.set();
        Security.PUBLIC = o.get();

        // 3. instanceof
        Security.PUBLIC = (o instanceof HammerB ? 0 : 1);
    }
}
