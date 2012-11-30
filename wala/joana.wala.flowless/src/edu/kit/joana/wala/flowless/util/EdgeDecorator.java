/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.util;

import com.ibm.wala.util.WalaException;

/**
 * @author sfink
 */
public interface EdgeDecorator {

  public static final EdgeDecorator DEFAULT = new EdgeDecorator() {
    public String getLabel(Object o) {
      return o.toString();
    }

    public String getColor(Object o) throws WalaException {
      return "black";
    }

    public String getStyle(Object o) throws WalaException {
      return "solid";
    }

  };

  /**
   * @param o
   * @return the String label for node o
   */
  String getLabel(Object o) throws WalaException;

  String getStyle(Object o) throws WalaException;

  String getColor(Object o) throws WalaException;

}
