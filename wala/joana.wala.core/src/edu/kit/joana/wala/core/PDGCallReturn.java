/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import java.util.Iterator;

/**
 * A tuple for the return values of a call node. This may include the normal return value of the method as
 * well as the exception value that is potentially thrown by the called method. Keep in mind that void methods
 * do not have a return value, therefore the field retVal may be null. This is also true for excVal.
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public final class PDGCallReturn implements Iterable<PDGNode> {

	public final PDGNode retVal;
	public final PDGNode excVal;

	public PDGCallReturn(final PDGNode retVal, final PDGNode excVal) {
		this.retVal = retVal;
		this.excVal = excVal;
	}

	public int hashCode() {
		return ((retVal == null ? 1 : retVal.hashCode()) * 13) + ((excVal == null ? 1 : excVal.hashCode()) * 27);
	}

	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof PDGCallReturn) {
			final PDGCallReturn other = (PDGCallReturn) obj;

			return (retVal == other.retVal || (retVal != null && retVal.equals(other.retVal)))
					&& (excVal == other.excVal || (excVal != null && excVal.equals(other.excVal)));
		}

		return false;
	}

	/**
	 * When adapted to other languages there may be more then 1 normal return value. The order should be
	 * all normal return values first, then the exception return value. This order is used to construct
	 * the control flow.
	 */
	@Override
	public Iterator<PDGNode> iterator() {
		return new Iterator<PDGNode>() {

			private PDGNode next = (retVal == null ? excVal : retVal);

			@Override
			public boolean hasNext() {
				return next != null;
			}

			@Override
			public PDGNode next() {
				final PDGNode n = next;

				if (n == retVal) {
					next = excVal;
				} else if (n == excVal) {
					next = null;
				}

				return n;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

}
