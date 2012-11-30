/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.exceptions.zea;

import java.util.TreeMap;

enum Info {
    UNKNOWN, UNSEEN, NULL, NOT_NULL,
}

public class Context {
    private TreeMap<Integer, Info> values;

    private TreeMap<Integer, Node> definitions;

    Context() {
        values = new TreeMap<Integer, Info>();
        definitions = new TreeMap<Integer, Node>();
    }

    Context copy() {
        Context c = new Context();
        c.values.putAll(values);
        c.definitions.putAll(definitions);
        return c;
    }

    Context fuse(Context other) {
        Context c = new Context();

        for (Integer i : values.keySet()) {
            Info thisVal = values.get(i);
            Info otherVal = other.values.get(i);
            Info phi = Context.phi(thisVal, otherVal);

            c.values.put(i, phi);
            if (phi == Info.NOT_NULL) {
                c.definitions.put(i, definitions.get(i));
            }
        }

        return c;
    }

    Context intersect(Context other) {
        return fuse(other);
    }

    void addUnseen(int value) {
        values.put(value, Info.UNSEEN);
    }

    void addNull(int value, Node definition) {
        definitions.put(value, definition);
        values.put(value, Info.NULL);
    }

    void addNotNull(int value, Node definition) {
        definitions.put(value, definition);
        values.put(value, Info.NOT_NULL);
    }

    void addUnknown(int value) {
        values.put(value, Info.UNKNOWN);
    }

    boolean isNull(int value) {
        return values.get(value) == Info.NULL;
    }

    boolean isNotNull(int value) {
        return values.get(value) == Info.NOT_NULL;
    }

    boolean isUnseen(int value) {
        return values.get(value) == Info.UNSEEN;
    }

    void piValue(int src, int dest, Node definition) {
        if (values.containsKey(src)) {
            values.put(dest, values.get(src));
            definitions.put(dest, definition);
        }
    }

    Node getDefinition(int value) {
        return definitions.get(value);
    }

    public String toString() {
        String s = "{";
        for (Integer i : values.keySet()) {
            s += " $" + i + "=" + values.get(i);
        }
        s += " }";
        return s;
    }

    static Info phi(Info a, Info b) {
        if (a == Info.UNSEEN)
            return b;
        if (b == Info.UNSEEN)
            return a;
        if (a == Info.UNKNOWN)
            return a;
        if (b == Info.UNKNOWN)
            return b;
        return Info.NOT_NULL; // only left
    }

}
