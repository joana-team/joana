/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests;

import sensitivity.Security;

public class ProbPasswordFile extends Thread {
    private static String user;
    private static String[] names = { "A", "B"};
    private static String[] passwords = { (Security.SECRET > 0 ? "x" : "z"), "y"};

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
        ProbPasswordFile pw = new ProbPasswordFile();
        user = args[0];

        pw.start();
        boolean val = pw.check(args[0], args[1]);
        Security.PUBLIC = 23;
        System.out.println(val);
    }

    public void run() {
        boolean match = false;

        try {
            for (int i=0; i<names.length; i++) {
                if (names[i].equals(user) && "ABCDEFGH".equals("ABCDEFGH")) {
                    match = true;
                    break;
                }
            }
        } catch (Throwable t) {};

        Security.PUBLIC = 8;
        System.out.println("8 chars");
    }
}
