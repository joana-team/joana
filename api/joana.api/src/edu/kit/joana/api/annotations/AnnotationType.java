/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.annotations;

import edu.kit.joana.api.sdg.AcceptAllNodeFilter;
import edu.kit.joana.api.sdg.NodeFilter;
import edu.kit.joana.api.sdg.SinkNodeFilter;
import edu.kit.joana.api.sdg.SourceNodeFilter;

public enum AnnotationType {
	SOURCE, SINK, DECLASS;

	public String toString() {
		switch (this) {
		case SOURCE:
			return "SOURCE";
		case SINK:
			return "SINK";
		default:/** case DECLASS: */
			return "DECLASS";
		}
	}
	
	public NodeFilter getNodeFilter() {
		switch (this) {
		case SOURCE:
			return new SourceNodeFilter();
		case SINK:
			return new SinkNodeFilter();
		default:
			return new AcceptAllNodeFilter();
		}
	}

	public static AnnotationType fromString(String s) {
		for (AnnotationType t : AnnotationType.values())
			if (s.equals(t.toString()))
				return t;
		throw new IllegalArgumentException(s
				+ " does not denote a valid annotation type!");
	}
}