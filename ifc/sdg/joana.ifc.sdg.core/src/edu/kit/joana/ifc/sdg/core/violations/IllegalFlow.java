/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.violations;

/**
 * @author Martin Mohr
 */
public class IllegalFlow<T> implements IIllegalFlow<T> {
	
	private final T source;
	private final T sink;
	private final String attackerLevel;
	
	public IllegalFlow(T source, T sink, String attackerLevel) {
		this.source = source;
		this.sink = sink;
		this.attackerLevel = attackerLevel;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolation#accept(edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor)
	 */
	@Override
	public void accept(IViolationVisitor<T> v) {
		v.visitIllegalFlow(this);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow#getSource()
	 */
	@Override
	public T getSource() {
		return source;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow#getSink()
	 */
	@Override
	public T getSink() {
		return sink;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow#getAttackerLevel()
	 */
	@Override
	public String getAttackerLevel() {
		return attackerLevel;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attackerLevel == null) ? 0 : attackerLevel.hashCode());
		result = prime * result + ((sink == null) ? 0 : sink.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		return result;
	}

	/* (non-Javadoc)
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
		if (!(obj instanceof IllegalFlow)) {
			return false;
		}
		@SuppressWarnings("rawtypes")
		IllegalFlow other = (IllegalFlow) obj;
		if (attackerLevel == null) {
			if (other.attackerLevel != null) {
				return false;
			}
		} else if (!attackerLevel.equals(other.attackerLevel)) {
			return false;
		}
		if (sink == null) {
			if (other.sink != null) {
				return false;
			}
		} else if (!sink.equals(other.sink)) {
			return false;
		}
		if (source == null) {
			if (other.source != null) {
				return false;
			}
		} else if (!source.equals(other.source)) {
			return false;
		}
		return true;
	}

}
