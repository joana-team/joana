/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.conc;

/**
 * Stellt eine Deklassifikation dar.
 * Besteht aus einem eingehenden und einem ausgehenden Sicherheitslevel.
 *
 * @author giffhorn
 */
public class Rule {
    String in;  // eingehend
    String out; // ausgehend

    public boolean equals(Object o) {
        if (o instanceof Rule) {
            Rule r = (Rule) o;
            return r.in.equals(in) && r.out.equals(out);

        } else {
            return false;
        }
    }

    public String in() {
        return in;
    }

    public String out() {
        return out;
    }

    public void in(String s) {
       in = s;
    }

    public void out(String s) {
        out = s;
    }

    public int hashCode() {
        return 31 * in.hashCode() + out.hashCode();
    }

    public String toString() {
        return in + " --> " + out;
    }
}
