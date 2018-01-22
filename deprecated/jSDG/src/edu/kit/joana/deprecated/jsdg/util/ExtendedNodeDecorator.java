/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.util;

import com.ibm.wala.util.WalaException;
import com.ibm.wala.viz.NodeDecorator;

/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public interface ExtendedNodeDecorator extends NodeDecorator<Object> {

	public static final ExtendedNodeDecorator DEFAULT = new ExtendedNodeDecorator() {
		public String getLabel(Object o) {
			return o.toString();
		}

		public String getColor(Object o) throws WalaException {
			return "blue";
		}

		public String getShape(Object o) throws WalaException {
			return "box";
		}
	};

	public static class DefaultImpl implements ExtendedNodeDecorator {
		public String getColor(Object o) throws WalaException {
			return DEFAULT.getColor(o);
		}

		public String getLabel(Object o) throws WalaException {
			return DEFAULT.getLabel(o);
		}

		public String getShape(Object o) throws WalaException {
			return DEFAULT.getShape(o);
		}
	}

	String getShape(Object o) throws WalaException;

	String getColor(Object o) throws WalaException;

}
