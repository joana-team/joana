package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.stream.IntStream;

/**
 * Subclasses represent values of a specific type.
 * Captures all necessary information about a value to analyze and run the program
 */
public abstract class Value {

	private int valNum;
	private int width;
	private Type type;
	private final Stack<Pair<Integer, Object>> val;
	private Formula[] deps;
	private Variable[] vars;
	private List<BasicBlock> useBlocks;
	private boolean leaked;
	private boolean isConstant;
	private boolean influencesLeak;
	private boolean isDefaultInit;
	private boolean isParameter;

	public Value(int valNum) {
		this.valNum = valNum;
		this.leaked = false;
		this.isConstant = false;
		this.influencesLeak = true;
		this.val = new Stack<>();
		this.isParameter = false;
		this.isDefaultInit = false;
	}

	public void addVars(Variable[] vars) {
		assert (vars.length == width);
		this.vars = vars;
	}

	public static Value createPrimitiveByType(int valNum, Type type) {
		switch (type) {
		case INTEGER:
			return new Int(valNum);
		default:
			return null;
		}
	}

	public static Value createConstant(int valNum, Type type, Object value) {
		if (type == Type.INTEGER) {
			Int i = new Int(valNum);
			i.setConstant(true);
			i.setVal(value, -1);
			return i;
		}
		throw new IllegalStateException("Unexpected value: " + type);
	}

	public Formula getDepForBit(int i) {
		checkWidth(i);
		return this.deps[i];
	}

	public void setDepforBit(int i, Formula dep) {
		checkWidth(i);
		this.deps[i] = dep;
	}

	private void checkWidth(int i) {
		if (i >= this.deps.length) {
			throw new ArrayIndexOutOfBoundsException("Invalid Array Access: Value type only has width " + this.type.bitwidth());
		}
	}

	public abstract boolean verifyType(Object val);

	// --------------------------- getters and setters ---------------------------------------

	public int getValNum() {
		return valNum;
	}

	public void setValNum(int valNum) {
		this.valNum = valNum;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Object getVal() {
		return val.peek().snd;
	}

	public boolean assigned() {
		return !(val == null);
	}

	/**
	 * contract: in each function call, at most 1 element can be added to an object's value stack
	 * if the value is already marked as modified, we need to pop a value, before we can add the new one
	 *
	 * @param val
	 */
	public void setVal(Object val, int recursionDepth) {
		if (!verifyType(val)) {
			throw new IllegalArgumentException("Error: Wrong input parameter. Expected type " + this.type);
		}
		if (isConstant) {
			this.val.push(Pair.make(-1, val));
		} else {
			this.val.push(Pair.make(recursionDepth, val));
		}
	}

	/**
	 * clean up after function call is finished
	 */
	public void resetValueToDepth(int recursionDepth) {
		if (this.val.isEmpty()) {
			return;
		}
		Pair<Integer, Object> top = this.val.peek();
		if (top.fst > recursionDepth) {
			this.val.pop();
		}
	}

	public Formula[] getDeps() {
		return deps;
	}

	public void setDeps(Formula[] deps) {
		this.deps = deps;
		this.isDefaultInit = false;
	}

	@Override public String toString() {
		return String.format("%d %s %s", this.valNum, this.val.toString(), Arrays.toString(this.deps)) + ((leaked) ? " Leaked" : "");
	}

	public void leak() {
		this.leaked = true;
	}

	public boolean isConstant() {
		return isConstant;
	}

	public void setConstant(boolean constant) {
		isConstant = constant;
	}

	public boolean isLeaked() {
		return leaked;
	}

	public Variable[] getVars() {
		return this.vars;
	}

	public abstract boolean isArrayType();

	public abstract String getValAsString();

	public abstract void setConstantBitMask(BitLatticeValue[] constantBits);

	public abstract BitLatticeValue[] getConstantBitMask();

	public boolean influencesLeak() {
		return this.influencesLeak;
	}

	public boolean isEffectivelyConstant(Method m) {
		return false;
	}

	public void setInfluencesLeak(boolean influence) {
		this.influencesLeak = influence;
	}

	public abstract boolean[] getConstantBits();

	public void setDefaultInit(boolean b) {
		this.isDefaultInit = b;
	}

	public List<BasicBlock> useBlocks() {
		return new ArrayList<>();
	}

	public Formula[] getMaskedDeps() {
		return this.deps;
	}

	public enum BitLatticeValue {
		ZERO(LogicUtil.ff.constant(false)), ONE(LogicUtil.ff.constant(true)), UNKNOWN(null);

		private final Formula asPropFormula;

		BitLatticeValue(Formula propFormula) {
			this.asPropFormula = propFormula;
		}

		public Formula asPropFormula() {
			return this.asPropFormula;
		}

		public static BitLatticeValue[] fromFormula(Formula[] formulas) {
			return Arrays.stream(formulas).map(f -> {
				if (f.isConstantFormula() && f.equals(LogicUtil.ff.constant(true))) {
					return ONE;
				} else if (f.isConstantFormula() && f.equals(LogicUtil.ff.constant(false))) {
					return ZERO;
				} else {
					return UNKNOWN;
				}
			}).toArray(BitLatticeValue[]::new);
		}

		public static String toStringLiteral(Formula[] f) {
			return toStringLiteral(fromFormula(f));
		}

		public static String toStringLiteral(BitLatticeValue[] bits) {
			return "0b" + Arrays.stream(bits).map(b -> {
				switch (b) {
				case ZERO:
					return "0";
				case ONE:
					return "1";
				default:
					return "u";
				}
			}).reduce("", (s, str) -> s + str);
		}

		public static BitLatticeValue[] defaultUnknown(int length) {
			BitLatticeValue[] res = new BitLatticeValue[length];
			Arrays.fill(res, UNKNOWN);
			return res;
		}

		public static BitLatticeValue[][] defaultUnknown(int arrayLength, int elementWidth) {
			return IntStream.range(0, arrayLength).mapToObj(i -> defaultUnknown(elementWidth))
					.toArray(BitLatticeValue[][]::new);
		}

		public static String toStringLiteral(BitLatticeValue[][] array) {
			return Arrays.stream(array).map(BitLatticeValue::toStringLiteral).reduce("", (s, t) -> s + "_" + t);
		}

		public static String toStringLiteral(Value v) {
			if (v.isArrayType()) {
				return toStringLiteral(Arrays.stream(((Array<? extends Value>) v).getValueDependencies())
						.map(BitLatticeValue::fromFormula).toArray(BitLatticeValue[][]::new));
			} else {
				return (!v.isDefaultInit) ?
						toStringLiteral(v.getDeps()) :
						toStringLiteral(defaultUnknown(v.getWidth()));
			}
		}

		public static Formula restriction(BitLatticeValue[] constantBits, Formula[] val) {
			return IntStream.range(0, constantBits.length).filter(i -> constantBits[i] != UNKNOWN)
					.mapToObj(i -> LogicUtil.ff.equivalence(constantBits[i].asPropFormula, val[i]))
					.reduce(LogicUtil.ff.constant(true), LogicUtil.ff::and);
		}
	}

	public boolean isParameter() {
		return isParameter;
	}

	public void setParameter(boolean parameter) {
		isParameter = parameter;
	}

	public boolean dynAnalysisNecessary(Method m) {
		return this.influencesLeak && !this.isEffectivelyConstant(m);
	}

	public boolean isDefaultInit() {
		return this.isDefaultInit;
	}

	public void setUseBlocks(List<BasicBlock> useBlocks) {
		this.useBlocks = useBlocks;
	}
}