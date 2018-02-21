/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util;

import java.util.function.Function;

/**
 * TODO: @author Add your name here.
 */
public class Reference<T> {
	private T t;
	public Reference(T t) {
		this.t = t;
	}
	
	public void set(T t) {
		this.t = t;
	}
	
	public T get() {
		return t;
	}
	
	public T apply(Function<T, T> f) {
		t = f.apply(t);
		return t;
	}
}
