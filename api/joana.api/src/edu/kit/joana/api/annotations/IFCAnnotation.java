/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.annotations;

import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGProgramPart;

public class IFCAnnotation {

	private final AnnotationType type;
	private final String level1;
	private final String level2;
	private final SDGProgramPart annotatedPart;
	private final SDGMethod context;

	public IFCAnnotation(AnnotationType type, String level, SDGProgramPart annotatedPart) {
		this(type, level, annotatedPart, null);
	}

	public IFCAnnotation(AnnotationType type, String level, SDGProgramPart annotatedPart, SDGMethod context) {
		if (type == AnnotationType.DECLASS || type == null || level == null || annotatedPart == null) {
			throw new IllegalArgumentException();
		}

		this.type = type;
		this.level1 = level;
		this.level2 = null;
		this.annotatedPart = annotatedPart;
		this.context = context;
	}

	public IFCAnnotation(String level1, String level2, SDGProgramPart annotatedPart) {
		if (level1 == null || level2 == null || annotatedPart == null) {
			throw new IllegalArgumentException();
		}
		
		this.type = AnnotationType.DECLASS;
		this.level1 = level1;
		this.level2 = level2;
		this.annotatedPart = annotatedPart;
		this.context = null;
	}
	
	private IFCAnnotation(AnnotationType type, String level1, String level2, SDGProgramPart annotatedPart, SDGMethod context) {
		this.type = type;
		this.level1 = level1;
		this.level2 = level2;
		this.annotatedPart = annotatedPart;
		this.context = context;
	}
	
	/**
	 * Returns an IFC annotation which is equivalent to this IFC annotation with the exception of the
	 * annotated program part
	 * @param newPPart program part of new annotation
	 * @return an IFC annotation which is equivalent to this IFC annotation with the exception of the
	 * annotated program part
	 */
	public IFCAnnotation transferTo(SDGProgramPart newPPart) {
		return new IFCAnnotation(type, level1, level2, newPPart, context);
	}

	public AnnotationType getType() {
		return type;
	}

	public String getLevel1() {
		return level1;
	}

	public String getLevel2() {
		if (type != AnnotationType.DECLASS) {
			throw new IllegalStateException();
		}
		return level2;
	}

	public SDGProgramPart getProgramPart() {
		return annotatedPart;
	}

	public SDGMethod getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((annotatedPart == null) ? 0 : annotatedPart.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof IFCAnnotation)) {
			return false;
		}
		IFCAnnotation other = (IFCAnnotation) obj;
		if (annotatedPart == null) {
			if (other.annotatedPart != null) {
				return false;
			}
		} else if (!annotatedPart.equals(other.annotatedPart)) {
			return false;
		}
		if (type != other.type) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getType() + "/" + annotatedPart;
	}
}
