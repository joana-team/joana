/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import edu.kit.joana.ui.annotations.Level;

/**
 * 
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD,ElementType.METHOD})
public @interface Sink {
	Level level();
	AnnotationPolicy annotate() default AnnotationPolicy.ANNOTATE_USAGES;
}
