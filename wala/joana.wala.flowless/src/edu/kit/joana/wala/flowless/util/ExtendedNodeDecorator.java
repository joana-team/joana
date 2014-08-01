/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.util;

import com.ibm.wala.util.WalaException;
import com.ibm.wala.viz.NodeDecorator;

/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public interface ExtendedNodeDecorator<T> extends NodeDecorator<T> {

	public static ExtendedNodeDecorator<Object> DEFAULT = new DefaultImpl<Object>();
	
	public static class DefaultImpl<V> implements ExtendedNodeDecorator<V> {
		public String getLabel(V o) throws WalaException {
			return o.toString();
		}

		public String getColor(V o) throws WalaException {
			return "blue";
		}

		public String getShape(V o) throws WalaException {
			return "box";
		}
	}

	String getShape(T o) throws WalaException;

	String getColor(T o) throws WalaException;

}
