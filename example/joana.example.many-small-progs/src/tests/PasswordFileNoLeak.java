/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests;

import sensitivity.Security;

public class PasswordFileNoLeak {
  private String[] names = { "A", "B"};
  private String[] passwords = { (Security.SECRET > 0 ? "x" : "z"), "y"};

  public boolean check(String user, String password) {
    boolean match = false;
    try {
      for (int i=0; i<names.length; i++) {
        if (names[i].equals(user) && passwords[i].equals(password)) {
          match = true;
          break;
        }
      }
    }
    catch (Throwable t) {};
    return match;
 }

  public static void main(String[] args) {
    PasswordFileNoLeak lt = new PasswordFileNoLeak();
    boolean b = lt.check(args[0], args[1]);
    Security.PUBLIC = 42;
    System.out.println(b);
  }
}
