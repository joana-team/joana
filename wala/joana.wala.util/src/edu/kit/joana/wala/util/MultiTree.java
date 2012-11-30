/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Vector;
/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class MultiTree<E> {
  private E value;
  private Vector<MultiTree<E>> children = new Vector<MultiTree<E>>();

  public MultiTree(E value) {
    this.value = value;
  }

  public void addChild(E childValue) {
    children.add(new MultiTree<E>(childValue));
  }

  public void addChild(MultiTree<E> m) {
    children.add(m);
  }

  public E value() {
    return value;
  }

  public void setValue(E v) {
    value = v;
  }

  public Collection<MultiTree<E>> getChildren() {
    return Collections.unmodifiableCollection(children);
  }

  public String toString() {
	  String str = (value != null ? value.toString() : "null");

	  if (!children.isEmpty()) {
		  for (MultiTree<E> child : getChildren()) {
			  str += "(" + child.toString() + ")";
		  }
	  }

	  return str;
  }

}
