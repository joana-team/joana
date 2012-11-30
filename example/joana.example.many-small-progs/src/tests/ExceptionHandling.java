/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests;

public class ExceptionHandling {
    static int x;
    static int y;

    public static void main(String[] args) {
        try {
            foo();
            System.out.println(x);

        } catch(Exception e) {
            System.out.println(x);
        }
        System.out.println(x);
    }

    static void foo() throws Exception {
        x = 0;
        if (y > 10) {
            throw new Exception();
        }
        x = 1;
    }
}
