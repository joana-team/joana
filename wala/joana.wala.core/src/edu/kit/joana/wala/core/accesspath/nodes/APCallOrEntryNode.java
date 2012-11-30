/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath.nodes;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.wala.classLoader.IField;

import edu.kit.joana.wala.core.PDGNode;

public abstract class APCallOrEntryNode extends APSimplePropagationNode {

	private final APParamNode paramIn[];
	private final APParamNode exc;
	private final APParamNode ret;
	private final Map<IField, APParamNode> static2in = new HashMap<IField, APParamNode>();
	private final Map<IField, APParamNode> static2out = new HashMap<IField, APParamNode>();

	APCallOrEntryNode(final int iindex, final PDGNode node, final APParamNode[] paramIn, final APParamNode ret,
			final APParamNode exc, final Type type) {
		super(iindex, type, node);

		if (paramIn == null) {
			throw new IllegalArgumentException();
		} else if (type != Type.CALL && type != Type.ENTRY) {
			throw new IllegalArgumentException();
		}

		assert paramsNotNull(paramIn);

		this.paramIn = paramIn;
		this.ret = ret;
		this.exc = exc;
	}

	private static final boolean paramsNotNull(final APParamNode[] nodes) {
		for (final APParamNode n : nodes) {
			if (n == null) {
				return false;
			}
		}

		return true;
	}

	public final int getParameterNum() {
		return paramIn.length;
	}

	public final APParamNode getParameterIn(final int num) {
		return paramIn[num];
	}

	public final APParamNode getReturn() {
		return ret;
	}

	public final APParamNode getReturnException() {
		return exc;
	}

	public final APParamNode getParameterStaticIn(final IField f) {
		return static2in.get(f);
	}

	public final void addParameterStaticIn(final IField f, final APParamNode p) {
		static2in.put(f, p);
	}

	public final APParamNode getParameterStaticOut(IField f) {
		return static2out.get(f);
	}

	public final void addParameterStaticOut(final IField f, final APParamNode p) {
		static2out.put(f, p);
	}


	public Iterable<Entry<IField, APParamNode>> getStaticIns() {
		return static2in.entrySet();
	}

	public Iterable<Entry<IField, APParamNode>> getStaticOuts() {
		return static2out.entrySet();
	}

}
