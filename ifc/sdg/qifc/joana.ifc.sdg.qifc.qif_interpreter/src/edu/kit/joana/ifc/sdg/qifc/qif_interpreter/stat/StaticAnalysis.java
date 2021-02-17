package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.Util;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.OutOfScopeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;

import java.util.List;

public class StaticAnalysis {

	private final Program program;
	private final Method entry;
	private final FormulaFactory f;

	public StaticAnalysis(Program program) throws InvalidClassFileException {
		this.program = program;
		this.entry = program.getEntryMethod();
		this.f = new FormulaFactory();
	}

	/*
	constructor used for testing
	 */
	public StaticAnalysis(FormulaFactory ff) {
		this.program = null;
		this.entry = null;
		this.f = ff;
	}

	private Formula[] createVars(int valNum, Type type) {
		// For now we dont allow user-defined types
		assert(type != Type.CUSTOM);

		Formula[] vars = new Formula[type.bitwidth()];
		for (int i = 0; i < type.bitwidth(); i++) {
			Variable var = f.variable(generateVarName(valNum, i));
			vars[i] = f.literal(var.name(), true);
		}
		return vars;
	}

	private String generateVarName(int valNum, int bit) {
		return valNum + "::" + bit;
	}

	/**
	 * turns character '0' into 'false', everything else into 'true'
	 * @param arr a character array
	 * @return array representing the input characters as boolean constants
	 */
	public Formula[] asFormulaArray(char[] arr) {
		Formula[] form = new Formula[arr.length];

		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == '0') {
				form[i] = f.constant(false);
			} else {
				form[i] = f.constant(true);
			}
		}
		return form;
	}

	/**
	 * returns the two's complement of number n as a char array of a specified length.
	 * The least significant bit will be at the highest index of the array
	 * @param n the converted number
	 * @return two's complement of n
	 */
	public char[] twosComplement(int n, int length) {
		char[] binary = Integer.toBinaryString(Math.abs(n)).toCharArray();

		binary = trim(binary, length, '0');

		if (n >= 0) {
			return binary;
		}

		// invert
		for (int i = 0; i < binary.length; i++) {
			binary[i] = (binary[i] == '0') ? '1' : '0';
		}

		// subtract 1
		for (int i = binary.length - 1; i >= 0; i--) {
			if (binary[i] == '0') {
				binary[i] = '1';
				break;
			} else {
				binary[i] = '0';
			}
		}
		return binary;
	}

	// trim the array arr to the specified size. If arr is longer than size, the front is cut off.
	// If arr is shorter than size, it is prefixed with the appropriate amount of the placeholder char 0
	private char[] trim(char[] arr, int size, char placeholder) {
		char[] trimmed = new char[size];
		for (int i = 0; i < size; i++) {
			trimmed[size - 1 - i] = (arr.length - 1 - i >= 0) ? arr[arr.length - 1 - i] : placeholder;
		}
		return trimmed;
	}

	public void computeSATDeps() {
		// create literals for method parameters
		int[] params = this.entry.getIr().getParameterValueNumbers();
		for (int i = 1; i < params.length; i++) {
			entry.setDepsForvalue(params[i], createVars(params[i], entry.getParamType(i)));
		}

		// initialize formula arrays for all constant values
		entry.getProgramValues().values().stream().filter(Value::isConstant).forEach(c -> {
			try {
				c.setDeps(asFormulaArray(binaryRep(c.getVal(), c.getType())));
			} catch (UnexpectedTypeException e) {
				e.printStackTrace();
			}
		});

		SATVisitor sv = new SATVisitor();

		for (BBlock bBlock: program.getEntryMethod().getCFG().getBlocks()) {
			try {
				sv.visitBlock(program.getEntryMethod(), bBlock, -1);
			} catch (OutOfScopeException e) {
				e.printStackTrace();
			}
		}
	}

	private char[] binaryRep(Object val, Type type) throws UnexpectedTypeException {
		if (type == Type.INTEGER) {
			return twosComplement((Integer) val, Type.INTEGER.bitwidth());
		} else {
			throw new UnexpectedTypeException(type);
		}
	}

	public class SATVisitor implements SSAInstruction.IVisitor {
		private static final String OUTPUT_FUNCTION = "edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out.print(I)V";

		private boolean containsOutOfScopeInstruction;
		private SSAInstruction outOfScopeInstruction;
		private BBlock block;
		private Method m;

		public void visitBlock(Method m, BBlock b, int prevBlock) throws OutOfScopeException {
			this.block = b;
			this.m = m;

			for (SSAInstruction i: b.instructions()) {
				i.visit(this);
			}

			if (containsOutOfScopeInstruction) {
				throw new OutOfScopeException(outOfScopeInstruction);
			}
		}

		public SATVisitor() {
			this.containsOutOfScopeInstruction = false;
		}

		@Override
		public void visitGoto(SSAGotoInstruction instruction) {
			containsOutOfScopeInstruction = true;
			outOfScopeInstruction = instruction;
		}

		@Override public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
			containsOutOfScopeInstruction = true;
			outOfScopeInstruction = instruction;

		}

		@Override public void visitArrayStore(SSAArrayStoreInstruction instruction) {
			containsOutOfScopeInstruction = true;
			outOfScopeInstruction = instruction;
		}

		@Override
		public void visitComparison(SSAComparisonInstruction instruction) {
			containsOutOfScopeInstruction = true;
			outOfScopeInstruction = instruction;
		}

		@Override
		public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {
			int op1ValNum = instruction.getUse(0);
			int op2ValNum = instruction.getUse(1);

			assert program != null;
			if (entry.getValue(op1ValNum).getDeps() == null) {
				// if there doesn't exist a value object for this valueNumber at this point, it has to be constant
				createConstant(op1ValNum);
			}
			Formula[] op1 = entry.getDepsForValue(op1ValNum);

			if (entry.getValue(op2ValNum).getDeps() == null) {
				createConstant(op2ValNum);
			}
			Formula[] op2 = entry.getDepsForValue(op2ValNum);

			IConditionalBranchInstruction.Operator operator = (IConditionalBranchInstruction.Operator) instruction.getOperator();

			Formula defForm;
			Formula[] diff = sub(op1, op2);

			switch (operator) {
			case EQ:
				defForm = equalsZero(diff);
				break;
			case NE:
				defForm = f.not(equalsZero(diff));
				break;
			case LT:
				defForm = f.equivalence(f.constant(true), diff[0]);
				break;
			case GE:
				defForm = f.equivalence(f.constant(false), diff[0]);
				break;
			case GT:
				defForm = f.and(f.not(equalsZero(diff)), f.equivalence(f.constant(false), diff[0]));
				break;
			case LE:
				defForm = f.or(equalsZero(diff), f.equivalence(f.constant(true), diff[0]));
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + operator);
			}

			List<ISSABasicBlock> succs = Util.asList(block.getCFG().getWalaCFG().getNormalSuccessors(block.getWalaBasicBLock()));
			assert(succs.size() == 2);

			int trueIdx = instruction.getTarget();
			int falseIdx = succs.stream().filter(b -> b.getNumber() != instruction.getTarget()).findFirst().get().getNumber();
		}

		private Formula equalsZero(Formula[] diff) {

			Formula res = f.equivalence(f.constant(false), diff[0]);
			for (int i = 1; i < diff.length; i++) {
				res = f.and(f.equivalence(f.constant(false), diff[i]));
			}
			return res;
		}

		@Override public void visitSwitch(SSASwitchInstruction instruction) {
			containsOutOfScopeInstruction = true;
			outOfScopeInstruction = instruction;
		}

		@Override
		public void visitReturn(SSAReturnInstruction instruction) {
			// do nothing
		}

		@Override public void visitGet(SSAGetInstruction instruction) {
			containsOutOfScopeInstruction = true;
			outOfScopeInstruction = instruction;
		}

		@Override public void visitPut(SSAPutInstruction instruction) {
			containsOutOfScopeInstruction = true;
			outOfScopeInstruction = instruction;
		}

		@Override public void visitInvoke(SSAInvokeInstruction instruction) {
			if (instruction.getCallSite().getDeclaredTarget().getSignature().equals(OUTPUT_FUNCTION)) {
				m.getValue(instruction.getUse(0)).leak();
			}
		}

		@Override public void visitNew(SSANewInstruction instruction) {
			containsOutOfScopeInstruction = true;
			outOfScopeInstruction = instruction;
		}

		@Override public void visitArrayLength(SSAArrayLengthInstruction instruction) {
			containsOutOfScopeInstruction = true;
			outOfScopeInstruction = instruction;
		}

		@Override public void visitThrow(SSAThrowInstruction instruction) {
			containsOutOfScopeInstruction = true;
			outOfScopeInstruction = instruction;
		}

		@Override public void visitMonitor(SSAMonitorInstruction instruction) {
			containsOutOfScopeInstruction = true;
			outOfScopeInstruction = instruction;
		}

		@Override public void visitCheckCast(SSACheckCastInstruction instruction) {
			containsOutOfScopeInstruction = true;
			outOfScopeInstruction = instruction;
		}

		@Override public void visitInstanceof(SSAInstanceofInstruction instruction) {
			containsOutOfScopeInstruction = true;
			outOfScopeInstruction = instruction;
		}

		@Override
		public void visitPhi(SSAPhiInstruction instruction) {
			containsOutOfScopeInstruction = true;
			outOfScopeInstruction = instruction;
		}

		@Override
		public void visitPi(SSAPiInstruction instruction) {
			containsOutOfScopeInstruction = true;
			outOfScopeInstruction = instruction;
		}

		@Override public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {
			containsOutOfScopeInstruction = true;
			outOfScopeInstruction = instruction;
		}

		@Override public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {
			containsOutOfScopeInstruction = true;
			outOfScopeInstruction = instruction;
		}

		@Override public void visitBinaryOp(SSABinaryOpInstruction instruction) {
			Formula[] op1 = entry.getDepsForValue(instruction.getUse(0));
			Formula[] op2 = entry.getDepsForValue(instruction.getUse(1));

			int def = instruction.getDef();

			IBinaryOpInstruction.Operator operator = (IBinaryOpInstruction.Operator) instruction.getOperator();

			// make sure Value object for def exists
			if (!entry.hasValue(def)) {
				Value defVal = Value.createByType(def, Type.getResultType(operator, entry.type(instruction.getUse(0)), entry.type(instruction.getUse(1))));
				entry.addValue(def, defVal);
			}
			Formula[] defForm = null;
			switch (operator) {
			case SUB:
				defForm = sub(op1, op2);
				break;
			case ADD:
				defForm = add(op1, op2);
				break;
			case MUL:
				defForm = mult(op1, op2);
				break;
			case DIV:
			case REM:
				break;
			case AND:
				defForm = and(op1, op2);
				break;
			case OR:
				defForm = or(op1, op2);
				break;
			case XOR:
				defForm = xor(op1, op2);
				break;
			}

			assert defForm != null;
			entry.setDepsForvalue(def, defForm);
		}

		@Override public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
			int def = instruction.getDef();
			int opValNum = instruction.getUse(0);

			assert program != null;
			if (!entry.hasValue(opValNum)) {
				createConstant(opValNum);
			}
			Formula[] op = entry.getDepsForValue(opValNum);

			IUnaryOpInstruction.Operator operator = (IUnaryOpInstruction.Operator) instruction.getOpcode();

			// make sure Value object for def exists
			if (!entry.hasValue(def)) {
				Value defVal = Value.createByType(def, Type.getResultType(operator, entry.type(opValNum)));
				entry.addValue(def, defVal);
			}

			if (operator == IUnaryOpInstruction.Operator.NEG) {
				entry.setDepsForvalue(def, neg(op));
			}
		}

		@Override public void visitConversion(SSAConversionInstruction instruction) {
			containsOutOfScopeInstruction = true;
			outOfScopeInstruction = instruction;
		}

		// create formula for bitwise or of op1 and op2 and assign to def
		public Formula[] or(Formula[] op1, Formula[] op2) {
			Formula[] defForm = new Formula[op1.length];

			for (int i = 0; i < op1.length; i++) {
				defForm[i] = f.or(op1[i], op2[i]);
			}
			return defForm;
		}

		// create formula for bitwise or of op1 and op2 and assign to def
		public Formula[] xor(Formula[] op1, Formula[] op2) {
			Formula[] defForm = new Formula[op1.length];

			for (int i = 0; i < op1.length; i++) {
				defForm[i] = xor(op1[i], op2[i]);
			}
			return defForm;
		}

		public Formula xor(Formula op1, Formula op2) {
			return f.and(f.or(op1, op2), f.or(f.not(op1), f.not(op2)));
		}

		// create formula for bitwise or of op1 and op2 and assign to def
		public Formula[] and(Formula[] op1, Formula[] op2) {

			Formula[] defForm = new Formula[op1.length];

			for (int i = 0; i < op1.length; i++) {
				defForm[i] = f.and(op1[i], op2[i]);
			}
			return defForm;
		}

		public Formula[] add(Formula[] op1, Formula[] op2) {
			Formula[] res = new Formula[op1.length];
			Formula carry = f.constant(false);

			for (int i = op1.length - 1; i >= 0; i--) {
				res[i] = xor(xor(op1[i], op2[i]), carry);
				carry = f.or(f.or(f.and(op1[i], op2[i]), f.and(op1[i], carry), f.and(op2[i], carry)));
			}
			return res;
		}

		public Formula[] sub(Formula[] a, Formula[] b) {
			Formula carry = f.constant(false);
			Formula[] res = new Formula[a.length];

			for (int i = a.length - 1; i >= 0; i--) {
				res[i] = f.and(f.or(a[i], xor(b[i], carry)), f.or(f.not(a[i]), f.equivalence(b[i], carry)));
				carry = f.or(f.and(a[i], b[i], carry), f.and(f.not(a[i]), f.or(b[i], carry)));
			}

			return res;
		}

		public Formula[] mult(Formula[] op1, Formula[] op2) {
			Formula[][]carry = new Formula[op1.length][op1.length];

			for (int i = op1.length - 1; i >= 0; i--) {

				for (int j = op1.length - 1; j >= 0; j--) {
					if (j > i) {
						carry[i][j] = f.constant(false);
					} else {
						carry[i][j] = f.and(op1[i], op2[j + (op1.length - i - 1)]);
					}
				}
			}

			Formula[] res = asFormulaArray(twosComplement(0, op1.length));
			for(int i = 1; i < op1.length; i++) {
				res = add(res, carry[i]);
			}
			res = sub(res, carry[0]);
			return res;
		}

		public Formula[] neg(Formula[] op) {
			Formula[] defForm = new Formula[op.length];

			// invert
			for (int i = 0; i < op.length; i++) {
				defForm[i] = f.not(op[i]);
			}

			// add 1
			defForm = add(defForm, asFormulaArray(twosComplement(1, defForm.length)));
			return defForm;
		}
	}

	private void createConstant(int op1) {
		Int constant = (Int) entry.getValue(op1);
		assert constant != null;
		constant.setDeps(asFormulaArray(twosComplement((Integer) constant.getVal(), constant.getWidth())));
	}
}
