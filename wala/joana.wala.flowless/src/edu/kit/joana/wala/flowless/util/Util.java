/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.ConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IComparisonInstruction;
import com.ibm.wala.shrikeBT.UnaryOpInstruction;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.OrdinalSet;

/**
 * Contains some utility methods
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public final class Util {

	private Util() {}

	private final static Pattern p =
		Pattern.compile("[a-zA-Z0-9.\\-,:! \\t?'\\\\/\\(\\)\\{\\}\\[\\]].*");

	public static String sanitizeLabel(Object obj) {
		if (obj != null) {
			String label = obj.toString().replace('"', '\'');
			if (label.length() > 20) {
				label = label.substring(0, 17) + "...";
			}
			Matcher m = p.matcher(label);
			if (m.matches()) {
				return label;
			} else {
				return "...";
			}
		} else {
			return "?";
		}
	}

	private final static int MAX_FILENAME_LENGHT = 200;
	private static int fileNameId = 0;

	public static void dumpSSA(IR ir, String outDir) throws IOException {
		BufferedWriter bOut;
		File dir = new File(outDir + "/ssa/");
		if (!dir.exists()) {
			dir.mkdir();
		}
		String fileName = methodName(ir.getMethod());
		if (fileName.length() > MAX_FILENAME_LENGHT) {
			fileName = fileName.substring(MAX_FILENAME_LENGHT) + fileNameId++;
		}
		bOut = new BufferedWriter(new FileWriter(outDir + "/ssa/" + fileName +".ssa"));

		bOut.write("IR of " + methodName(ir.getMethod()) + ":\n");
		/*
		SSAInstruction[] instr = ir.getInstructions();
		for (int i = 0; i < instr.length; i++) {
			String txt = prettyInstruction(ir, i);
			if (txt != null) {
				bOut.write(txt);
			}
		}*/
		int i = 0;
		for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext(); i++) {
			SSAInstruction instr = it.next();
			String txt = prettyInstruction(ir, instr, i);
			if (txt != null) {
				bOut.write(txt);
			}
		}
		bOut.flush();
		bOut.close();
	}

	public static void dumpSSA(IR ir, IBasicBlock<?> bb, PrintStream out) {
		dumpSSA(ir, bb.getFirstInstructionIndex(),
				bb.getLastInstructionIndex() + 1, out);
	}

	public static void dumpSSA(IR ir, PrintStream out) {
		dumpSSA(ir, 0, ir.getInstructions().length, out);
	}

	public static void dumpCatchExceptionSSA(IR ir, PrintStream out) {
		for (Iterator<? extends SSAInstruction> it = ir.iterateCatchInstructions(); it.hasNext();) {
			SSAGetCaughtExceptionInstruction instr =
				(SSAGetCaughtExceptionInstruction) it.next();

			String txt = "bb" + instr.getBasicBlockNumber() + "\t" +
				instr.toString(ir.getSymbolTable());

			out.println(txt);
		}
	}

	public static void dumpPhiSSA(IR ir, PrintStream out) {
		for (Iterator<? extends SSAInstruction> it = ir.iteratePhis(); it.hasNext();) {
			SSAInstruction instr = it.next();
			String txt = instr.toString(ir.getSymbolTable());
			out.println(txt);
		}
	}

	public static void dumpSSA(IR ir, int start, int end, PrintStream out) {
		if (out == null) {
			return;
		}

		out.println("\nIR of " + methodName(ir.getMethod()) + " [" + start +
				"-" + end+ "]:");

		for (Iterator<? extends SSAInstruction> it = ir.iteratePhis(); it.hasNext();) {
			String txt = prettyInstruction(ir, it.next(), -1);
			if (txt != null) {
				out.println(txt);
			}
		}

		for (int i = start; i < end; i++) {
			SSAInstruction instr = ir.getInstructions()[i];
			String txt = prettyInstruction(ir, instr, i);
			if (txt != null) {
				out.print(txt);
			}
		}

		out.println();
	}




	public static <T> void addAllToSet(Set<T> set, Iterator<? extends T> it) {
		while (it.hasNext()) {
			set.add(it.next());
		}
	}

	private static class IteratorWrapper<T> implements Iterable<T> {

		private Iterator<T> iter;

		public IteratorWrapper(Iterator<T> iter) {
			this.iter = iter;
		}

		public Iterator<T> iterator() {
			return iter;
		}

	}

	public static String operator2String(UnaryOpInstruction.IOperator op) {
		if (op instanceof UnaryOpInstruction.Operator) {
			switch ((UnaryOpInstruction.Operator) op) {
			case NEG:
				return "-";
			}
		}

		return "?";
	}

	public static String operator2String(ConditionalBranchInstruction.IOperator op) {
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

	public static String opcode2String(IComparisonInstruction.Operator opcode) {
		return opcode.name();
//		Field [] fields = Constants.class.getFields();
//		for (int i = 0; i < fields.length; i++) {
//			if (fields[i].getName().startsWith("OP_")) {
//				try {
//					Short sf = fields[i].getShort(Constants.class);
//					if (sf == opcode) {
//						return fields[i].getName().substring(3);
//					}
//				} catch (IllegalArgumentException e) {
//				} catch (IllegalAccessException e) {}
//			}
//		}
//
//		return "?";
		/*
		switch(opcode) {
		case Constants.OP_lcmp:
	        return "lcmp";
		case Constants.OP_fcmpl:
	        return "fcmpl";
	    case Constants.OP_dcmpl:
	        return "dcmpl";
	    case Constants.OP_dcmpg:
	        return "dcmpg";
	    case Constants.OP_fcmpg:
	        return "fcmpg";
		default:
			return "?";
		}*/
	}

	public static <T> Iterable<T> makeIterable(Iterator<T> iter) {
		return new IteratorWrapper<T>(iter);
	}

	public static IMethod searchMethod(Iterable<Entrypoint> pts, String method) {
		for (Entrypoint p : pts) {
			if (p.getMethod() != null && p.getMethod().getSelector() != null &&
					p.getMethod().getSelector().toString().equals(method)) {
				return p.getMethod();
			}
		}

		return null;
	}

	public static Iterable<Entrypoint> makeMainEntrypoints(AnalysisScope scope, final IClassHierarchy cha, String className) {
		return com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
	}

	public static <K, T> Set<T> findOrCreateSet(Map<K, Set<T>> M, K key) {
		if (M == null) {
			throw new IllegalArgumentException("map is null");
		}

		Set<T> result = M.get(key);
		if (result == null) {
			result = new TreeSet<T>();
			M.put(key, result);
		}

		return result;
	}

	public static String prettyBasicBlock(IExplodedBasicBlock bb) {
		if (bb.isEntryBlock()) {
			return "ENTRY";
		} else if (bb.isExitBlock()) {
			return "EXIT";
		} else if (bb.isCatchBlock()) {
			String tmp = "CATCH " + Util.prettyShortInstruction(bb.getCatchInstruction());
			if (bb.getLastInstruction() != null) {
				tmp += " >>> " + Util.prettyShortInstruction(bb.getLastInstruction());
			}
			return tmp;
		} else if (bb.getInstruction() != null) {
			SSAInstruction instr = bb.getInstruction();
			return Util.prettyShortInstruction(instr);
		} else {
			Iterator<SSAPhiInstruction> it = bb.iteratePhis();
			if (it.hasNext()) {
				StringBuilder str = new StringBuilder();
				while (it.hasNext()) {
					SSAPhiInstruction phi = it.next();
					str.append(Util.prettyShortInstruction(phi));
					if (it.hasNext()) {
						str.append('\n');
					}
				}

				return str.toString();
			} else {
				return "SKIP";
			}
		}
	}

	public static String prettyShortInstruction(SSAInstruction instr) {
		if (instr instanceof SSAFieldAccessInstruction) {
			SSAFieldAccessInstruction facc = (SSAFieldAccessInstruction) instr;
			String fieldName;
			if (facc.isStatic()) {
				fieldName = Util.fieldName(facc.getDeclaredField());
			} else {
				fieldName = "v" + facc.getRef() + "." + facc.getDeclaredField().getName();
			}
			if (instr instanceof SSAGetInstruction) {
				SSAGetInstruction fget = (SSAGetInstruction) instr;
				return "v" + fget.getDef() + " = " + fieldName;
			} else if (instr instanceof SSAPutInstruction) {
				SSAPutInstruction fset = (SSAPutInstruction) instr;
				return fieldName + " = v" + fset.getVal();
			}
		} else if (instr instanceof SSAInvokeInstruction) {
			SSAInvokeInstruction invk = (SSAInvokeInstruction) instr;
			if (invk.getDeclaredResultType() == TypeReference.Void) {
				return "call " + Util.methodName(invk.getDeclaredTarget());
			} else {
				return "v" + invk.getDef() + " = call " + Util.methodName(invk.getDeclaredTarget());
			}
		} else if (instr instanceof SSANewInstruction) {
			SSANewInstruction snew = (SSANewInstruction) instr;
			return "v" + snew.getDef() + " = new " + Util.typeName(snew.getConcreteType().getName());
		} else if (instr instanceof SSAPhiInstruction) {
			SSAPhiInstruction phi = (SSAPhiInstruction) instr;
			StringBuilder str = new StringBuilder();
			str.append("v" + phi.getDef() + " = phi ");
			for (int i = 0; i < phi.getNumberOfUses(); i++) {
				str.append("v" + phi.getUse(i));
				if (i+1 < phi.getNumberOfUses()) {
					str.append(',');
				}
			}

			return str.toString();
		} else if (instr instanceof SSAArrayLoadInstruction) {
			SSAArrayLoadInstruction ai = (SSAArrayLoadInstruction) instr;
			return "v" + instr.getDef() + " = v" + ai.getArrayRef() + "[v" + ai.getIndex() + "]";
		} else if (instr instanceof SSAArrayStoreInstruction) {
			SSAArrayStoreInstruction ai = (SSAArrayStoreInstruction) instr;
			return "v" + ai.getArrayRef() + "[v" + ai.getIndex() + "] = v" + ai.getValue();
		} else if (instr instanceof SSAArrayLengthInstruction) {
			SSAArrayLengthInstruction ai = (SSAArrayLengthInstruction) instr;
			return "v" + ai.getDef() + " = v" + ai.getArrayRef() + ".length";
		}

		return instr.toString();
	}

	public static String prettyInstruction(IR ir, SSAInstruction instr, int num) {
		if (instr != null) {
			SymbolTable stab = ir.getSymbolTable();
			String txt = instr.toString(stab);

			for (int i = 0; i < instr.getNumberOfUses(); i++) {
				int use = instr.getUse(i);
				if (use < 0) {
					continue;
				}

//				Value v = stab.getValue(use);
//
//				if (v != null && v instanceof PhiValue) {
//					PhiValue phi = (PhiValue) v;
//					txt += " (" + phi.getPhiInstruction().toString(stab) + ")";
//
//				} else {
				String [] names = ir.getLocalNames(num, use);
				if (names != null) {
					for (int j = 0; j < names.length; j++) {
						txt += " [v" + use + ":use " + names[j] + "]";
					}
				}
//				}
			}

			for (int i = 0; i < instr.getNumberOfDefs(); i++) {
				int def = instr.getDef(i);
				if (def < 0) {
					continue;
				}

				String [] names = ir.getLocalNames(num, def);
				if (names != null) {
					for (int j = 0; j < names.length; j++) {
						txt += " [v" + def + ":def " + names[j] + "]";
					}
				} else {
					txt += " [v" + def + ": def]";
				}
			}

			ISSABasicBlock block = ir.getBasicBlockForInstruction(instr);

			return "bb" + block.getNumber() + "\t" +num + "\t: " + txt + "\n";
		} else {
			return "\t" + num + "\t: nop\n";
		}
	}

	public static final String methodName(IMethod method) {
		return methodName(method.getReference());
	}

	/**
	 * Create a human readable typename from a TypeName object
	 * convert sth like [Ljava/lang/String to java.lang.String[]
	 * @param tName
	 * @return type name
	 */
	public static final String typeName(TypeName tName) {
		StringBuilder test =
			new StringBuilder(tName.toString().replace('/', '.').replace('$', '.'));

		while (test.charAt(0) == '[') {
			test.deleteCharAt(0);
			test.append("[]");
		}

		// remove 'L' in front of object type
		test.deleteCharAt(0);

		return test.toString();
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

	public static final String methodName(MethodReference mRef) {
		StringBuilder name =
			new StringBuilder(typeName(mRef.getDeclaringClass().getName()));

		name.append(".");
		name.append(mRef.getName().toString());
		name.append("(");
		for (int i = 0; i < mRef.getNumberOfParameters(); i++) {
			TypeReference tRef = mRef.getParameterType(i);
			if (i != 0) {
				name.append(",");
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
				name.append(typeName(tRef.getName()));
			}
		}

		name.append(")");

		return name.toString();
	}

	public static String fieldName(IField field) {
		return fieldName(field.getReference());
	}

	public static String fieldName(FieldReference field) {
		TypeName type = field.getDeclaringClass().getName();

		return typeName(type) + "." + field.getName();
	}

}
