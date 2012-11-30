/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.killdef;

import com.ibm.wala.classLoader.CallSiteReference;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 * @param <T>
 */
public final class CallAccess<T> extends Access<T> {

	private final CallSiteReference csr;

	public CallAccess(final int iindex, final CallSiteReference csr) {
		super(iindex);
		this.csr = csr;
	}

	@Override
	public CallSiteReference getCallSite() {
		return csr;
	}

	@Override
	public Kind getKind() {
		return Kind.CALL;
	}

	@Override
	public boolean isSameAccess(final Access<T> other) {
		return this == other || (other != null && other.iindex == iindex);
	}

	public String toString() {
		return "call " + csr.getDeclaredTarget().getName() + "()";
	}
}
