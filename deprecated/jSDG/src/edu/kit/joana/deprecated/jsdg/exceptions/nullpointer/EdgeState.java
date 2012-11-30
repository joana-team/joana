/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.exceptions.nullpointer;

import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.fixpoint.AbstractVariable;
import com.ibm.wala.fixpoint.FixedPointConstants;
import com.ibm.wala.fixpoint.UnaryOperator;

/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
class EdgeState  extends AbstractVariable<EdgeState> {

	/*
	 * Inital state is UNKNOWN.
	 * Lattice: UNKNOWN < { NULL, NOT_NULL } < BOTH
	 */
	private enum State { UNKNOWN, BOTH, NULL, NOT_NULL };

	private static final int NO_THIS_PTR = -1;

	// maps ssa variable number -> State
	private final State[] vars;

	EdgeState(int maxVarNum, int[] paramVarNums) {
		this(maxVarNum, paramVarNums, NO_THIS_PTR);
	}

	EdgeState(int maxVarNum, int[] paramVarNums, int thisPtrVarNum) {
		this.vars = new State[maxVarNum + 1];
		for (int i = 0; i < vars.length; i++) {
			vars[i] = State.UNKNOWN;
		}

		// they are unknown. We do not definitely known that they always
		// may contain both.
//		for (int i = 0; i < paramVarNums.length; i++) {
//			vars[paramVarNums[i]] = State.BOTH;
//		}

		if (thisPtrVarNum != NO_THIS_PTR) {
			vars[thisPtrVarNum] = State.NOT_NULL;
		}
	}

	static AbstractMeetOperator<EdgeState> meetOperator() {
		return EdgeStateMeet.INSTANCE;
	}

	static UnaryOperator<EdgeState> meetFunction(int varNum, int[] fromVars) {
		return new NullPointerMeetFunction(varNum, fromVars);
	}

	static UnaryOperator<EdgeState> nullifyFunction(int varNum) {
		return new NullPointerNullifyFunction(varNum);
	}

	static UnaryOperator<EdgeState> denullifyFunction(int varNum) {
		return new NullPointerDenullifyFunction(varNum);
	}

	static UnaryOperator<EdgeState> identityFunction() {
		return NullPointerIndentityFunction.INSTANCE;
	}

	boolean isNeverNull(int varNum) {
		assert varNum > 0 && varNum < vars.length;

		return vars[varNum] == State.NOT_NULL;
	}

	boolean isAlwaysNull(int varNum) {
		assert varNum > 0 && varNum < vars.length;

		return vars[varNum] == State.NULL;
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.fixpoint.IVariable#copyState(com.ibm.wala.fixpoint.IVariable)
	 */
	@Override
	public void copyState(EdgeState v) {
		assert v.vars.length == vars.length;

		for (int i = 0; i < v.vars.length; i++) {
			vars[i] = v.vars[i];
		}
	}

	boolean meet(int varNum, State state) {
		if (state != State.UNKNOWN && vars[varNum] != State.BOTH && vars[varNum] != state) {
			if (vars[varNum] == State.UNKNOWN) {
				vars[varNum] = state;
			} else {
				vars[varNum] = State.BOTH;
			}

			return true;
		} else {
			return false;
		}
	}

	boolean meet(EdgeState other) {
		assert other.vars.length == vars.length;

		boolean changed = false;

		for (int i = 0; i < vars.length; i++) {
			changed |= meet(i, other.vars[i]);
		}

		return changed;
	}

	boolean nullify(int varNum) {
		return meet(varNum, State.NULL);
	}

	boolean denullify(int varNum) {
		return meet(varNum, State.NOT_NULL);
	}

	public boolean equals(Object obj) {
		if (obj instanceof EdgeState) {
			EdgeState other = (EdgeState) obj;
			assert vars.length == other.vars.length;

			for (int i = 0; i < vars.length; i++) {
				if (vars[i] != other.vars[i]) {
					return false;
				}
			}

			return true;
		}

		return false;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer("<");
		for (int i = 0; i < vars.length; i++) {
			switch (vars[i]) {
			case BOTH:
				buf.append('*');
				break;
			case NOT_NULL:
				buf.append('1');
				break;
			case NULL:
				buf.append('0');
				break;
			case UNKNOWN:
				buf.append('?');
				break;
			default:
				throw new IllegalStateException();
			}
		}
		buf.append('>');

		return buf.toString();
	}

	private static class EdgeStateMeet extends AbstractMeetOperator<EdgeState> {

		private final static EdgeStateMeet INSTANCE = new EdgeStateMeet();

		private EdgeStateMeet() {}

		/* (non-Javadoc)
		 * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o) {
			return o instanceof EdgeStateMeet;
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#evaluate(com.ibm.wala.fixpoint.IVariable, com.ibm.wala.fixpoint.IVariable[])
		 */
		@Override
		public byte evaluate(EdgeState lhs, EdgeState[] rhs) {
			boolean changed = false;
			for (EdgeState state : rhs) {
				changed |= lhs.meet(state);
			}

			return (changed ? FixedPointConstants.CHANGED : FixedPointConstants.NOT_CHANGED);
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#hashCode()
		 */
		@Override
		public int hashCode() {
			return 4711;
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#toString()
		 */
		@Override
		public String toString() {
			return "EdgeStateMeet";
		}

	}

	private static class NullPointerMeetFunction extends UnaryOperator<EdgeState> {

		private final int varNum;
		private final int[] fromVars;

		private NullPointerMeetFunction(int varNum, int[] fromVars) {
			this.varNum = varNum;
			this.fromVars = fromVars;
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.fixedpoint.impl.UnaryOperator#evaluate(com.ibm.wala.fixpoint.IVariable, com.ibm.wala.fixpoint.IVariable)
		 */
		@Override
		public byte evaluate(EdgeState lhs, EdgeState rhs) {
			byte state = FixedPointConstants.NOT_CHANGED;

			if (!lhs.equals(rhs)) {
				lhs.copyState(rhs);
				state = FixedPointConstants.CHANGED;
			}

			for (int from : fromVars) {
				if (lhs.meet(varNum, rhs.vars[from])) {
					state = FixedPointConstants.CHANGED;
				}
			}

			return state;
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}

			if (o instanceof NullPointerMeetFunction) {
				NullPointerMeetFunction other = (NullPointerMeetFunction) o;
				if (varNum == other.varNum && fromVars.length == other.fromVars.length) {
					for (int i = 0; i < fromVars.length; i++) {
						if (fromVars[i] != other.fromVars[i]) {
							return false;
						}
					}

					return true;
				}
			}

			return false;
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#hashCode()
		 */
		@Override
		public int hashCode() {
			return 11000 + varNum;
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#toString()
		 */
		@Override
		public String toString() {
			StringBuffer str = new StringBuffer("Meet(" + varNum + ", [");

			for (int i = 0; i < fromVars.length; i++) {
				str.append(fromVars[i]);
				str.append(i == fromVars.length - 1 ? "" : ",");
			}

			str.append("])");

			return str.toString();
		}

	}

	private static class NullPointerNullifyFunction extends UnaryOperator<EdgeState> {

		private final int varNum;

		private NullPointerNullifyFunction(int varNum) {
			this.varNum = varNum;
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.fixedpoint.impl.UnaryOperator#evaluate(com.ibm.wala.fixpoint.IVariable, com.ibm.wala.fixpoint.IVariable)
		 */
		@Override
		public byte evaluate(EdgeState lhs, EdgeState rhs) {
			byte state = FixedPointConstants.NOT_CHANGED;

			if (!lhs.equals(rhs)) {
				lhs.copyState(rhs);
				state = FixedPointConstants.CHANGED;
			}

			if (lhs.nullify(varNum)) {
				state = FixedPointConstants.CHANGED;
			}

			return state;
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o) {
			return o instanceof NullPointerNullifyFunction && ((NullPointerNullifyFunction) o).varNum == varNum;
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#hashCode()
		 */
		@Override
		public int hashCode() {
			return 47000 + varNum;
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#toString()
		 */
		@Override
		public String toString() {
			return "Nullify(" + varNum + ")";
		}

	}

	private static class NullPointerDenullifyFunction extends UnaryOperator<EdgeState> {

		private final int varNum;

		private NullPointerDenullifyFunction(int varNum) {
			assert varNum >= 0;
			this.varNum = varNum;
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.fixedpoint.impl.UnaryOperator#evaluate(com.ibm.wala.fixpoint.IVariable, com.ibm.wala.fixpoint.IVariable)
		 */
		@Override
		public byte evaluate(EdgeState lhs, EdgeState rhs) {
			byte state = FixedPointConstants.NOT_CHANGED;

			if (!lhs.equals(rhs)) {
				lhs.copyState(rhs);
				state = FixedPointConstants.CHANGED;
			}

			if (lhs.denullify(varNum)) {
				state = FixedPointConstants.CHANGED;
			}

			return state;
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o) {
			return o instanceof NullPointerDenullifyFunction && ((NullPointerDenullifyFunction) o).varNum == varNum;
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#hashCode()
		 */
		@Override
		public int hashCode() {
			return -47000 - varNum;
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#toString()
		 */
		@Override
		public String toString() {
			return "Denullify(" + varNum + ")";
		}

	}

	private static class NullPointerIndentityFunction extends UnaryOperator<EdgeState> {

		private static final NullPointerIndentityFunction INSTANCE = new NullPointerIndentityFunction();

		private NullPointerIndentityFunction() {
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.fixedpoint.impl.UnaryOperator#evaluate(com.ibm.wala.fixpoint.IVariable, com.ibm.wala.fixpoint.IVariable)
		 */
		@Override
		public byte evaluate(EdgeState lhs, EdgeState rhs) {
			if (lhs.equals(rhs)) {
				return FixedPointConstants.NOT_CHANGED;
			} else {
				lhs.copyState(rhs);
				return FixedPointConstants.CHANGED;
			}
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o) {
			return o instanceof NullPointerIndentityFunction;
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#hashCode()
		 */
		@Override
		public int hashCode() {
			return 8911;
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#toString()
		 */
		@Override
		public String toString() {
			return "Id";
		}

	}

}
