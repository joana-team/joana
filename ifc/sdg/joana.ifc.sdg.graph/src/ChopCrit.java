/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
import edu.kit.joana.ifc.sdg.graph.SDGNode;

public class ChopCrit extends Object{
    SDGNode source;
    SDGNode target;

    public ChopCrit(SDGNode s, SDGNode t) {
        source = s;
        target = t;
    }

    public String toString() {
        return "("+source+", "+target+")";
    }
}
