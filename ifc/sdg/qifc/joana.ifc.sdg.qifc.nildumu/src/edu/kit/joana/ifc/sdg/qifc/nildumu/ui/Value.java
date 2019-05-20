/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu.ui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import edu.kit.joana.ui.annotations.Source;

/**
 * Annotates sources ({@link Source}) with a value.
 * It allows to configure which bits are seen as unknown.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
public @interface Value {
	String value() default "0buuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu";
}
