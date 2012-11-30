/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests;

public class Hammer {
    static class A {
        int x;

        void set() {
            x = 0;
        }

        void set(int i) {
            x = i;
        }

        int get() {
            return x;
        }
    }

    static class B extends A {
        void set() {
            x = 1;
        }
    }


    public static void main (String[] args) {
        int sec = Integer.parseInt(args[1]);
        int pub = Integer.parseInt(args[2]);

        // 1. no information flow
        A o = new A();
        o.set(sec);
        o = new A();
        o.set(pub);
        System.out.println(o.get());

        // 2. dynamic dispatch
        if (sec == 0 && args[0].equals("007")) {
            o = new B();
        }
        o.set();
        System.out.println(o.get());

        // 3. instanceof
        System.out.println(o instanceof B);
    }
}
