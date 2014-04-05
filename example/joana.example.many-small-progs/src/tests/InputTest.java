/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests;

import sensitivity.Security;

public class InputTest {
    public static void main(String[] args) {
        int low = 0;
        int PIN = inputPIN();

        while (PIN-- != 0) {
            low = input();
        }

        Security.PUBLIC = low;
        System.out.println(low);
    }

    private static int inputPIN() {
        return Security.SECRET;
    }

    private static int input() {
        return 0;
    }
}
