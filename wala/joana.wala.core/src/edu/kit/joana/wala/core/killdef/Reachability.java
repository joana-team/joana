/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.killdef;

import java.util.Map;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.util.graph.dominators.Dominators;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.killdef.Access.Kind;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 * @param <T>
 */
public class Reachability<T> {

	private final CGNode currentMethod;
	private final IFieldsMayMod fMayMod;
	private final Map<T, OrdinalSet<Access<T>>> mayReach;
	private final Map<T, OrdinalSet<Access<T>>> mustReach;
	private final Dominators<T> dom;

	public Reachability(final CGNode currentMethod, final IFieldsMayMod fMayMod,
			final Map<T, OrdinalSet<Access<T>>> mayReach, final Map<T, OrdinalSet<Access<T>>> mustReach,
			final Dominators<T> dom) {
		this.fMayMod = fMayMod;
		this.mayReach = mayReach;
		this.mustReach = mustReach;
		this.dom = dom;
		this.currentMethod = currentMethod;
	}

	public boolean isReachFromTo(final Access<T> from, final Access<T> to) {
		final OrdinalSet<Access<T>> rTo = getMayReachingTo(to);

		return rTo.contains(from);
	}

	public boolean isConnected(final Access<T> a1, final Access<T> a2) {
		return isReachFromTo(a1, a2) || isReachFromTo(a2, a1);
	}

	private OrdinalSet<Access<T>> getMayReachingTo(final Access<T> acc) {
		final OrdinalSet<Access<T>> rTo = mayReach.get(acc.getNode());

		return rTo;
	}

	private OrdinalSet<Access<T>> getMustReachingTo(final Access<T> acc) {
		final OrdinalSet<Access<T>> rTo = mustReach.get(acc.getNode());

		return rTo;
	}

	public FieldAccess<T> findLastWriteDominating(final FieldAccess<T> acc) {
		final OrdinalSet<Access<T>> must = getMustReachingTo(acc);
		final OrdinalSet<Access<T>> may = getMayReachingTo(acc);

		FieldAccess<T> iWriteDom = null;
		for (Access<T> mustAcc : must) {
			if (mustAcc.isWrite() && mustAcc.getField().equals(acc.getField())) {
				if (iWriteDom == null) {
					iWriteDom = (FieldAccess<T>) mustAcc;
				} else if (isDominating(iWriteDom, mustAcc)){
					iWriteDom = (FieldAccess<T>) mustAcc;
				}
			}
		}

		if (iWriteDom == null) {
			return null;
		}

		for (final Access<T> mayAcc : may) {
			if (mayAcc == iWriteDom) continue;

			if (mayAcc.isWrite()) {
				if (mayAcc.getField().equals(iWriteDom.getField()) && isDominating(iWriteDom, mayAcc)) {
					// a possible write is between the dominating write and the access
					return null;
				}
			} else if (mayAcc.getKind() == Kind.CALL) {
				if (isDominating(iWriteDom, mayAcc)
						&& fMayMod.mayCallModField(currentMethod, mayAcc.getCallSite(), iWriteDom.getField())) {
					// a possible call to a method that may modify the field is in between the domination write and
					// the access
					return null;
				}
			}
		}

		return iWriteDom;
	}

	public boolean isDominating(final Access<T> mayDominate, final Access<T> mayBeDominated) {
		final T mayDomNode = mayDominate.getNode();
		final T mayBeDomNode = mayBeDominated.getNode();

		return dom.isDominatedBy(mayBeDomNode, mayDomNode);
	}

	public boolean isWriteInBetween(final Access<T> from, final Access<T> to, final ParameterField field) {
		if (!isDominating(from, to)) {
			return true;
		}

		final OrdinalSet<Access<T>> rFrom = getMustReachingTo(from);
		final OrdinalSet<Access<T>> rTo = getMayReachingTo(to);

		for (final Access<T> a : rTo) {
			if (rFrom.contains(a) || !isDominating(from, a)) continue;
			// in between from -> to
			switch (a.getKind()) {
			case CALL: {
				final CallSiteReference csr = a.getCallSite();
				if (fMayMod.mayCallModField(currentMethod, csr, field)) {
					return true;
				}
			} break;
			case ARRAY:
			case FIELD:
			case STATIC: {
				if (a.isWrite() && a.getField().equals(field)) {
					return true;
				}
			} break;
			}
		}

		return false;
	}
}
