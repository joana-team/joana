/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2003-2005</p>
 *
 * <p>Company: University of Passau</p>
 *
 * @author Christian Hammer
 * @version 1.0
 */
public class InfFlow {
  private static class A {
    int x;

    void set() { x = 0; }

    void set(int i) { x = i; }

    int get() { return x; }
  }

  private static class B extends A {
    void set() { x = 1; }
  }

  public static void main(String[] args) {
    int secure = 0, pub = 1;
    A o = new A();
    o.set(secure);
    o = new A();
    o.set(pub);
    System.out.println(o.get());
    if (secure==0 && args[0].equals("007"))
      o = new B();
    o.set();
    System.out.println(o.get());
  }
}
