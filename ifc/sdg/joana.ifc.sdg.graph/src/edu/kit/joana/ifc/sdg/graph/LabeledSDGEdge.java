/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph;

import edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind;

/**
 * TODO: @author Add your name here.
 */
public class LabeledSDGEdge extends SDGEdge {
	private final String label;
	
	/**
	 * Creates an SDGEdge of kind `kind' from source to sink and labels it with `label'.
	 */
	public LabeledSDGEdge(SDGNode source, SDGNode sink, Kind kind, String label) {
		super(source, sink, kind);
		this.label = label;
	}

	@Override
	public String toString() {
		return new String(getSource() + " -" + getKind() + "-" + (label == null ? "" : label) + "> " + getTarget());
	}
	
	@Override
	public boolean hasLabel() {
		return label != null;
	}

	@Override
	public String getLabel() {
		return label;
	}
	
    public boolean equals(Object o) {
    	if (this == o) return true;

        if (!(o instanceof SDGEdge)) {
            return false;
        }
        SDGEdge edge = (SDGEdge) o;

        if (kind != edge.kind) return false;
        if ((label == null) != (edge.getLabel() == null)) return false;
        if ((label != null) && !label.equals(edge.getLabel())) return false;
        if (!getSource().equals(edge.getSource())) return false;
        return getTarget().equals(edge.getTarget());
    }
}
