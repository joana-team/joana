/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests;

public class PasswordFile {
    private String[] names = { "A", "B"};
    private String[] passwords = { "x", "y"};

    public boolean check(String user, String password) {
        boolean match = false;

        try {
            for (int i=0; i<names.length; i++) {
                if (names[i].equals(user) && passwords[i].equals(password)) {
                    match = true;
                    break;
                }
            }

        } catch (Throwable t) {};

        return match;
    }

    public static void main(String[] args) {
        System.out.println(new PasswordFile().check(args[0], args[1]));
    }
}
