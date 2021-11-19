/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.util;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a location in the bytecode. For most nodes this maps to an instruction
 * in the bytecode, which is identified by the bytecode name of the method and the
 * index of the bytecode instruction.
 *
 * There is a special case for parameter nodes. The bytecode locations of these
 * nodes do not correspond to a location in the bytecode, they contain information
 * about the field type and name. They can be identified by their bytecode index
 * that matches one of the constants ROOT_PARAMETER, STATIC_FIELD, OBJECT_FIELD
 * or ARRAY_FIELD. This applies also to PHI nodes, whose bytecode index matches
 * the constant PHI.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public final class BytecodeLocation {

	public final static int UNDEFINED_POS_IN_BYTECODE = -1;
	public final static int ROOT_PARAMETER = -2;
	public final static int STATIC_FIELD = -3;
	public final static int OBJECT_FIELD = -4;
	public final static int ARRAY_FIELD = -5;
	public final static int BASE_FIELD = -6;
	public final static int ARRAY_INDEX = -7;
	public static final int PHI = -8;
	public final static int CALL_RET = -9;

	public final String bcMethod;
	public final int bcIndex;

	public static String bcIndexToString(int bcIndex) {
		if (bcIndex>=0) {
			return "index " + bcIndex;
		} else {
			switch (bcIndex) {
			case UNDEFINED_POS_IN_BYTECODE:
				return "undefined";
			case ROOT_PARAMETER:
				return "root param";
			case STATIC_FIELD:
				return "static field";
			case OBJECT_FIELD:
				return "object field";
			case ARRAY_FIELD:
				return "array field";
			case BASE_FIELD:
				return "base field";
			case ARRAY_INDEX:
				return "array index";
			case PHI:
				return "phi";
			default:
				throw new IllegalArgumentException();
			}
		}
	}

	public BytecodeLocation(final String bcMethod, final int bcIndex) {
		if (bcMethod == null) {
			throw new IllegalArgumentException("bcMethod may not be null.");
		} else if (bcIndex < 0 && bcIndex != UNDEFINED_POS_IN_BYTECODE && bcIndex != ROOT_PARAMETER
				&& bcIndex != STATIC_FIELD && bcIndex != OBJECT_FIELD && bcIndex != ARRAY_FIELD
				&& bcIndex != BASE_FIELD && bcIndex != ARRAY_INDEX) {
			throw new IllegalArgumentException("bytecode index must be >= 0 or correspond to one of the "
					+"special constants defined in BytecodeLocation class.");
		}

		this.bcMethod = bcMethod;
		this.bcIndex = bcIndex;
	}

	public final String toString() {
		return "\"" + bcMethod + "\":" + bcIndex;
	}

	public static String getRootParamName(int paramIndex) {
		return ROOT_PARAM_PREFIX + paramIndex;
	}


	/**
	 * Returns whether this node represents a formal parameter, but is neither an exit node nor an exception node
	 * @return true if this node represents a formal parameter, but is neither an exit node nor an exception node, and false otherwise
	 */
	public static boolean isNormalFormalParameter(SDGNode n) {
		return n.getBytecodeIndex() == ROOT_PARAMETER && (n.getKind() == Kind.FORMAL_IN || n.getKind() == Kind.FORMAL_OUT) && n.getKind() != SDGNode.Kind.EXIT && !BytecodeLocation.EXCEPTION_PARAM.equals(n.getBytecodeName());
	}

	public static boolean isPhiNode(SDGNode n) {
		return n.getBytecodeIndex() == PHI;
	}

	public static boolean isCallRetNode(SDGNode n) {
		return n.getBytecodeIndex() == CALL_RET;
	}

	/**
	 * Returns the index of a parameter label.
	 * @param paramLabel parameter label to extract the index from
	 * @return index of parameter if given string is really a parameter label
	 * as returned by {@link BytecodeLocation#getRootParamIndex(String)}, some
	 * negative number otherwise
	 */
	public static int getRootParamIndex(String paramLabel) {
		if (paramLabel != null) {
			Matcher m = ROOT_PARAM_LABEL_PATTERN.matcher(paramLabel);
			if (m.matches()) {
				return Integer.parseInt(m.group(1));
			}
		}
		return -1;
	}

	public static final String ROOT_PARAM_PREFIX = "<param> ";
	private static final Pattern ROOT_PARAM_LABEL_PATTERN = Pattern.compile(ROOT_PARAM_PREFIX + "(\\d+)");
	public static final String EXCEPTION_PARAM = "<exception>";
	public static final String RETURN_PARAM = "<exit>";
	public static final String BASE_PARAM = "<base>";
	public static final String ARRAY_PARAM = "<[]>";
	public static final String INDEX_PARAM = "<index>";
	public static final String UNKNOWN_PARAM = "<???>";
	public static final String PHI_NODE = "<phi>";




}
