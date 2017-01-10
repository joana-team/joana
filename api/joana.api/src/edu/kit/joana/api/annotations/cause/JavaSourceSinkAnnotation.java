/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.annotations.cause;

import java.lang.annotation.Annotation;

import edu.kit.joana.api.SPos;
import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

/**
 * TODO: @author Add your name here.
 */
public abstract class JavaSourceSinkAnnotation<S extends Annotation> implements AnnotationCause {

	private final SPos sourcePosition;
	private final Class<S> annotationClass;
	
	public JavaSourceSinkAnnotation(Class<S> annotationClass, SPos sourcePosition) {
		if (annotationClass != Source.class && annotationClass != Sink.class) throw new IllegalArgumentException();
		this.sourcePosition = sourcePosition;
		this.annotationClass = annotationClass;
	}
	
	@Override
	public String toString() {
		return "Caused by a " + annotationClass + "annotation at " + sourcePosition;
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.api.annotations.cause.AnnotationCause#getSourcePosition()
	 */
	@Override
	public SPos getSourcePosition() {
		return sourcePosition;
	}
}
