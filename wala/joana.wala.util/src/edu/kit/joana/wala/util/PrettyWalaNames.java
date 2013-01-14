/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrikeBT.BinaryOpInstruction;
import com.ibm.wala.shrikeBT.ConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IComparisonInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.shrikeBT.UnaryOpInstruction;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAComparisonInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstanceofInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

/**
 * Contains utility methods that convert WALA internal elements, like IField, IMethod etc. to a nice human readable
 * string.
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public final class PrettyWalaNames {

	private PrettyWalaNames() {
		throw new UnsupportedOperationException("No instance of this class allowed.");
	}
	
	public static final String ir2string(final IR ir) {
		final StringBuilder sb = new StringBuilder("IR of " + methodName(ir.getMethod(), true) + "\n");
		
		final SSACFG cfg = ir.getControlFlowGraph();
		
		for (final Iterator<SSAInstruction> it = ir.iterateNormalInstructions(); it.hasNext();) {
			final SSAInstruction instr = it.next();
			if (instr == null) continue;			
			sb.append(instr2string(ir, instr, cfg) + "\n");
		}
		
		boolean hasPhis = false;
		for (final Iterator<? extends SSAInstruction> it = ir.iteratePhis(); it.hasNext();) {
			if (!hasPhis) {
				hasPhis = true;
				sb.append("PHIs:\n");
			}
			final SSAInstruction instr = it.next();
			if (instr == null) continue;			
			sb.append("\t" + instr2string(ir, instr, cfg) + "\n");
		}		
		
		return sb.toString();
	}

	private static String iindex2str(final int iindex) {
		return String.format("%04d", iindex);
	}
	
	public static final String instr2string(final IR ir, final SSAInstruction instr, final SSACFG cfg) {
		final StringBuffer txt = (instr.iindex >= 0 
				? new StringBuffer(iindex2str(instr.iindex) + ":\t")
				: new StringBuffer());
		final SymbolTable stab = ir.getSymbolTable();

		if (instr instanceof SSAFieldAccessInstruction) {
			final SSAFieldAccessInstruction facc = (SSAFieldAccessInstruction) instr;
			final FieldReference fref = facc.getDeclaredField();
			String fieldName;
			if (facc.isStatic()) {
				fieldName = type2string(fref.getDeclaringClass()) + "." + fref.getName();
			} else {
				fieldName = val2string(stab, facc.getRef()) + "." + facc.getDeclaredField().getName();
			}
			
			if (instr instanceof SSAGetInstruction) {
				SSAGetInstruction fget = (SSAGetInstruction) instr;
				txt.append(val2string(stab, fget.getDef()) + " = " + fieldName);
			} else if (instr instanceof SSAPutInstruction) {
				SSAPutInstruction fset = (SSAPutInstruction) instr;
				txt.append(fieldName + " = " + val2string(stab, fset.getVal()));
			}
		} else if (instr instanceof SSAInvokeInstruction) {
			final SSAInvokeInstruction invk = (SSAInvokeInstruction) instr;
			txt.append(call2string(stab, invk));
		} else if (instr instanceof SSANewInstruction) {
			final SSANewInstruction snew = (SSANewInstruction) instr;
			txt.append(val2string(stab, snew.getDef()) + " = new " + type2string(snew.getConcreteType().getName()));
		} else if (instr instanceof SSAPhiInstruction) {
			final SSAPhiInstruction phi = (SSAPhiInstruction) instr;
			txt.append(val2string(stab, phi.getDef()) + " = phi ");
			for (int i = 0; i < phi.getNumberOfUses(); i++) {
				txt.append(val2string(stab, phi.getUse(i)));
				if (i + 1 < phi.getNumberOfUses()) {
					txt.append(", ");
				}
			}
		} else if (instr instanceof SSAArrayLoadInstruction) {
			final SSAArrayLoadInstruction ai = (SSAArrayLoadInstruction) instr;
			txt.append(val2string(stab, instr.getDef()) + " = " + val2string(stab, ai.getArrayRef())
					+ "[" + val2string(stab, ai.getIndex()) + "]");
		} else if (instr instanceof SSAArrayStoreInstruction) {
			final SSAArrayStoreInstruction ai = (SSAArrayStoreInstruction) instr;
			txt.append(val2string(stab, ai.getArrayRef()) + "[" + val2string(stab, ai.getIndex()) + "] = "
					+ val2string(stab, ai.getValue()));
		} else if (instr instanceof SSAArrayLengthInstruction) {
			final SSAArrayLengthInstruction ai = (SSAArrayLengthInstruction) instr;
			txt.append(val2string(stab, ai.getDef()) + " = " + val2string(stab, ai.getArrayRef()) + ".length");
		} else if (instr instanceof SSAConditionalBranchInstruction) {
			final SSAConditionalBranchInstruction cond = (SSAConditionalBranchInstruction) instr;
			final ConditionalBranchInstruction.IOperator op = cond.getOperator();
			assert cond.getNumberOfUses() == 2;
			txt.append("if (" + val2string(stab, cond.getUse(0)) + " " + op2str(op) + " "
					+ val2string(stab, cond.getUse(1)) + ")");
			
			if (cfg != null) {
				final ISSABasicBlock ifBlock = ir.getBasicBlockForInstruction(cond);
				final SSAInstruction[] instrs = ir.getInstructions();
				final int trueNext = searchNextIIndex(instrs, cfg, ifBlock);
				txt.append(" goto " + iindex2str(trueNext));
			}
		} else if (instr instanceof SSAUnaryOpInstruction) {
			final SSAUnaryOpInstruction unop = (SSAUnaryOpInstruction) instr;
			final IUnaryOpInstruction.IOperator op = unop.getOpcode();
			txt.append(val2string(stab, unop.getDef()) + " = " + op2str(op) + " " + val2string(stab, unop.getUse(0)));
		} else if (instr instanceof SSABinaryOpInstruction) {
			final SSABinaryOpInstruction binop = (SSABinaryOpInstruction) instr;
			final IBinaryOpInstruction.IOperator op = binop.getOperator();
			txt.append(val2string(stab, binop.getDef()) + " = " + val2string(stab, binop.getUse(0)) + " " + op2str(op)
					+ " " + val2string(stab, binop.getUse(1)));
		} else if (instr instanceof SSAGotoInstruction) {
			final SSAGotoInstruction gotoi = (SSAGotoInstruction) instr;
			txt.append("goto");
			
			if (cfg != null) {
				final ISSABasicBlock thisBlock = ir.getBasicBlockForInstruction(gotoi);
				final SSAInstruction[] instrs = ir.getInstructions();
				final int nextInstr = searchNextIIndex(instrs, cfg, thisBlock);
				txt.append(" " + iindex2str(nextInstr));
			}
		} else if (instr instanceof SSACheckCastInstruction) {
			final SSACheckCastInstruction chk = (SSACheckCastInstruction) instr;
			txt.append(val2string(stab, chk.getResult()) + " = (");
			final TypeReference[] trefs = chk.getDeclaredResultTypes();
			for (int i = 0; i < trefs.length; i++) {
				txt.append(type2string(trefs[i]));
				if (i < trefs.length - 1) {
					txt.append(" | ");
				}
			}
			txt.append(") " + val2string(stab, chk.getVal()));
		} else if (instr instanceof SSAInstanceofInstruction) {
			final SSAInstanceofInstruction insof = (SSAInstanceofInstruction) instr;
			txt.append(val2string(stab, insof.getDef()) + " = " + val2string(stab, insof.getRef()));
			txt.append(" instanceof " + type2string(insof.getCheckedType()));
		} else if (instr instanceof SSAReturnInstruction) {
			final SSAReturnInstruction ret = (SSAReturnInstruction) instr;
			txt.append("return");
			if (!ret.returnsVoid()) {
				txt.append(" " + val2string(stab, ret.getResult()));
			}
		} else if (instr instanceof SSAComparisonInstruction) {
			final SSAComparisonInstruction cmp = (SSAComparisonInstruction) instr;
			final IComparisonInstruction.Operator op = cmp.getOperator();
			txt.append(val2string(stab, cmp.getDef()) + " = " + val2string(stab, cmp.getUse(0)) + " " + op2str(op)
					+ " " + val2string(stab, cmp.getUse(1)));
		} else {
			txt.append(instr.toString(stab));
		}

		return txt.toString();
	}
	
	private static final int searchNextIIndex(final SSAInstruction[] instrs, final SSACFG cfg, final ISSABasicBlock block) {
		if (block.isExitBlock()) {
			return -1;
		}
		
		final Collection<ISSABasicBlock> succs = cfg.getNormalSuccessors(block);
		final Iterator<ISSABasicBlock> it = succs.iterator();
		final ISSABasicBlock next = it.next();
		
		for (int i = next.getFirstInstructionIndex(); i <= next.getLastInstructionIndex(); i++) {
			if (instrs[i] != null) {
				return i;
			}
		}
		
		return searchNextIIndex(instrs, cfg, next);
	}
	
	public static final String call2string(final SymbolTable stab, final SSAInvokeInstruction invk) {
		final StringBuilder txt = new StringBuilder();
		if (invk.getDeclaredResultType() != TypeReference.Void) {
			txt.append(val2string(stab, invk.getDef()) + " = ");
		}
		if (!invk.isStatic()) {
			txt.append(val2string(stab, invk.getReceiver()) + ".");
		}
		
		final MethodReference method = invk.getDeclaredTarget();
		txt.append(simpleTypeName(method.getDeclaringClass().getName()));
		txt.append(".");
		final String mName = method.getName().toString().replace("<", "_").replace(">", "_");
		txt.append(mName);
		txt.append("(");
		
		for (int i = (invk.isStatic() ? 0 : 1); i < invk.getNumberOfUses(); i++) {
			txt.append(val2string(stab, invk.getUse(i)));
			if (i < invk.getNumberOfUses() - 1) {
				txt.append(", ");
			}
		}

		txt.append(")");
		
		return txt.toString();
	}

	public static final String val2string(final SymbolTable stab, final int val) {
		if (stab.isConstant(val)) {
			if (stab.isBooleanConstant(val)) {
				return (stab.isTrue(val) ? "#true" : "#false");
			} else if (stab.isNumberConstant(val)) {
				if (stab.isIntegerConstant(val)) {
					return "#" + stab.getIntValue(val);
				} else if (stab.isDoubleConstant(val)) {
					return "#" + stab.getDoubleValue(val);
				} else if (stab.isFloatConstant(val)) {
					return "#" + stab.getFloatValue(val);
				} else if (stab.isLongConstant(val)) {
					return "#" + stab.getLongValue(val);
				}
			} else if (stab.isStringConstant(val)) {
				return "#\"" + shorten(stab.getStringValue(val), 20) + "\"";
			} else if (stab.isNullConstant(val)) {
				return "#null";
			}

			final Object objVal = stab.getConstantValue(val);
			if (objVal != null) {
				return "#" + shorten(objVal.toString(), 20);
			}
		}

		return "v" + val;
	}
	
	public static final String shorten(final String str, final int maxLength) {
		return (str != null && str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str);
	}

	public static final String methodName(final IMethod method) {
		return methodName(method, false);
	}
	
	public static final String methodName(final IMethod method, final boolean printParamNum) {
		return methodName(method.getReference(), (printParamNum ? (method.isStatic() ? 0 : 1) : -1));
	}

	public static final String methodName(final MethodReference method) {
		return methodName(method, -1);
	}
	
	public static final String methodName(MethodReference method, final int printParamVal) {
		StringBuilder name =
			new StringBuilder(simpleTypeName(method.getDeclaringClass().getName()));

		name.append(".");
		String mName = method.getName().toString();
		mName = mName.replace("<", "_").replace(">", "_");
		name.append(mName);
		name.append("(");
		int paramValNum = printParamVal;
		for (int i = 0; i < method.getNumberOfParameters(); i++) {
			paramValNum++;
			TypeReference tRef = method.getParameterType(i);
			if (i != 0) {
				name.append(printParamVal >= 0 ? ", " : ",");
			}
			if (tRef.isPrimitiveType()) {
				if (tRef == TypeReference.Char) {
					name.append("char");
				} else if (tRef == TypeReference.Byte) {
					name.append("byte");
				} else if (tRef == TypeReference.Boolean) {
					name.append("boolean");
				} else if (tRef == TypeReference.Int) {
					name.append("int");
				} else if (tRef == TypeReference.Long) {
					name.append("long");
				} else if (tRef == TypeReference.Short) {
					name.append("short");
				} else if (tRef == TypeReference.Double) {
					name.append("double");
				} else if (tRef == TypeReference.Float) {
					name.append("float");
				} else {
					name.append("?" + tRef.getName());
				}
			} else {
				name.append(simpleTypeName(tRef.getName()));
			}
			
			if (printParamVal >= 0) {
				name.append(" v" + paramValNum);
			}
		}

		name.append(")");

		return name.toString();
	}

	public static final String simpleTypeName(TypeReference tref) {
		return simpleTypeName(tref.getName());
	}

	public static final String type2string(TypeReference tref) {
		return type2string(tref.getName());
	}
	
	public static final String type2string(TypeName tName) {
		if (tName.isArrayType()) {
			return type2string(tName.getInnermostElementType()) + "[]";
		}

		if (tName.isPrimitiveType()) {
			switch (tName.toString().charAt(0)) {
			case 'Z': return "boolean";
			case 'S': return "short";
			case 'J': return "long";
			case 'I': return "int";
			case 'F': return "float";
			case 'D': return "double";
			case 'C': return "char";
			case 'B': return "byte";
			default: return tName.toString();
			}
		} else {
			StringBuilder test = new StringBuilder(tName.toString().replace('/', '.'));
	
			// remove 'L' in front of object type
			test.deleteCharAt(0);
	
			return test.toString();
		}
	}

	
	/**
	 * Create a human readable typename from a TypeName object
	 * convert sth like [Ljava/lang/String to java.lang.String[]
	 * @param tName
	 * @return type name
	 */
	public static final String simpleTypeName(TypeName tName) {
		StringBuilder test = new StringBuilder(tName.toString().replace('/', '.'));

		while (test.charAt(0) == '[') {
			test.deleteCharAt(0);
			test.append("[]");
		}

		// remove 'L' in front of object type
		test.deleteCharAt(0);

		return test.toString();
	}


	public static final String bcTypeName(IClass cls) {
		return bcTypeName(cls.getReference());
	}

	public static final String bcTypeName(TypeReference tRef) {
		String bcType = tRef.getName().toString();

		return bcType;
	}

	public static final String simpleFieldName(FieldReference f) {
		String bcField = f.getName().toString();
		return bcField;
	}

	public static final String simpleFieldName(IField f) {
		String bcField = f.getName().toString();
		return bcField;
	}

	public static final String bcFieldName(IField f) {
		String bcField = f.getName().toString();
		String bcClass = bcTypeName(f.getDeclaringClass().getReference());
		return bcClass + "." + bcField;
	}

	public static final String sourceFileName(TypeName name) {
		assert (name.isClassType());

		String source = name.toString();
		if (source.indexOf('$') > 0) {
			// remove inner classes stuff
			source = source.substring(0, source.indexOf('$'));
		}

		// remove 'L'
		source = source.substring(1);
		source = source + ".java";

		return source;
	}

	/**
	 * Transcribes the elements of the given integer set into an array and returns that array.
	 * @param intSet integer set to transcribe
	 * @return the elements of the given integer set as an array
	 */
	public static int[] intSetToArray(Set<Integer> intSet) {
		if (intSet == null) {
			throw new IllegalArgumentException();
		}
		int[] ret = new int[intSet.size()];
		int count = 0;
		for (int x : intSet) {
			ret[count] = x;
			count++;
		}
		return ret;
	}

	public static String op2str(UnaryOpInstruction.IOperator op) {
		if (op instanceof UnaryOpInstruction.Operator) {
			switch ((UnaryOpInstruction.Operator) op) {
			case NEG:
				return "-";
			}
		}

		return "?";
	}

	public static String op2str(BinaryOpInstruction.IOperator op) {
		if (op instanceof BinaryOpInstruction.Operator) {
			switch ((BinaryOpInstruction.Operator) op) {
			case ADD:
				return "+";
			case AND:
				return "&&";
			case DIV:
				return "/";
			case MUL:
				return "*";
			case OR:
				return "||";
			case REM:
				return "%";
			case SUB:
				return "-";
			case XOR:
				return "^";
			}
		}

		return "?";
	}

	public static String op2str(ConditionalBranchInstruction.IOperator op) {
		if (op instanceof ConditionalBranchInstruction.Operator) {
			switch ((ConditionalBranchInstruction.Operator) op) {
			case EQ:
				return "==";
			case GE:
				return ">=";
			case GT:
				return ">";
			case LE:
				return "<=";
			case LT:
				return "<";
			case NE:
				return "!=";
			}
		}

		return "?";
	}

	public static String op2str(IComparisonInstruction.Operator opcode) {
		switch (opcode) {
		case CMP:
			return "==";
		case CMPG:
			return ">";
		case CMPL:
			return "<";
		}
		
		return opcode.name();
	}

}
