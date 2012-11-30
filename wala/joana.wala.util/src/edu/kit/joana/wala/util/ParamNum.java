/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.ibm.wala.classLoader.IMethod;

/**
 * @author Juergen Graf <graf@kit.edu>
 */
public final class ParamNum implements Serializable {

	private static final long serialVersionUID = 5017612348197134999L;

	private final int pNum;
	private final boolean isStatic;
	
	public enum PType {
		NORMAL_PARAM(1),	// all normal parameters always start at 1 
		THIS_VAL(0),
		RESULT_VAL(-1),
		EXCEPTION_VAL(-2),
		STATE_VAL(-3),
		ALL_VAL(-4),
		STATIC_VAR_NO_NUM(-5),
		ERROR(Integer.MIN_VALUE + 1),
		UNMAPPED_VAL(Integer.MIN_VALUE);
		
		public final int v;
		
		private PType(final int v) {
			this.v = v;
		}
	}
	
	private static Map<ParamNum, ParamNum> cache = new HashMap<ParamNum, ParamNum>();
	
	public static ParamNum fromIMethod(final IMethod im, final int imNum) {
		if (imNum < 0 && imNum >= im.getNumberOfParameters()) {
			throw new IllegalArgumentException();
		}

		return fromIMethod(im.isStatic(), imNum);
	}

	public static ParamNum fromIMethod(final boolean isStatic, final int imNum) {
		if (imNum < 0) {
			throw new IllegalArgumentException();
		}

		final ParamNum pn;
		
		if (isStatic) {
			pn = new ParamNum(imNum + 1, true);
		} else {
			pn = new ParamNum(imNum, false);
		}
		
		return findOrCreateInCache(pn);
	}
	
	public static ParamNum fromParamNum(final boolean isStatic, final int pNum) {
		final ParamNum pn;
		
		if (isStatic) {
			pn = new ParamNum(pNum, true);
		} else {
			pn = new ParamNum(pNum, false);
		}
		
		return findOrCreateInCache(pn);
	}

	public static ParamNum createSpecial(final PType type) {
		if (type == PType.NORMAL_PARAM || type == PType.THIS_VAL) {
			throw new IllegalArgumentException("This is no special type: " + type);
		}
		
		final ParamNum pn = new ParamNum(type.v, false);
		
		return findOrCreateInCache(pn);
	}
	
	private static ParamNum findOrCreateInCache(final ParamNum pn) {
		final ParamNum cachedPn = cache.get(pn);
		if (cachedPn == null) {
			cache.put(pn, pn);
			return pn;
		}
		
		return cachedPn;
	}
	
	private ParamNum(final int pNum, final boolean isStatic) {
		this.pNum = pNum;
		this.isStatic = isStatic;
	}
	
	public int hashCode() {
		return pNum * (isStatic ? 7 : 13);
	}
	
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		
		if (o instanceof ParamNum) {
			final ParamNum other = (ParamNum) o;
			return pNum == other.pNum && isStatic == other.isStatic;
		}
		
		return false;
	}
	
	public int getNum() {
		return pNum;
	}
	
	public int getMethodReferenceNum() {
		if (!isNormalParam()) {
			throw new IllegalStateException("No MethodReference parameter number for " + pNum + " - " + getType());
		}
		
		return pNum - 1;
	}
	
	public int getIMethodNum() {
		if (!isNormalParam()) {
			throw new IllegalStateException("No IMethod parameter number for " + pNum + " - " + getType());
		}
		
		return (isStatic ? pNum - 1 : pNum);
	}

	public boolean isThisOrParam() {
		return isNormalParam() || isThis();
	}
	
	public boolean isSpecial() {
		return !isThisOrParam();
	}
	
	public boolean isThis() {
		return pNum == PType.THIS_VAL.v;
	}

	public boolean isNormalParam() {
		return pNum >= PType.NORMAL_PARAM.v;
	}

	public boolean isResult() {
		return pNum == PType.RESULT_VAL.v;
	}

	public boolean isException() {
		return pNum == PType.EXCEPTION_VAL.v;
	}

	public boolean isState() {
		return pNum == PType.STATE_VAL.v;
	}

	public boolean isAll() {
		return pNum == PType.ALL_VAL.v;
	}

	public boolean isStaticVarNoNum() {
		return pNum == PType.STATIC_VAR_NO_NUM.v;
	}

	public boolean isError() {
		return pNum == PType.ERROR.v;
	}

	public boolean isUnmapped() {
		return pNum == PType.UNMAPPED_VAL.v;
	}
	
	public PType getType() {
		if (isNormalParam()) {
			return PType.NORMAL_PARAM;
		}
		
		for (final PType pt : PType.values()) {
			if (pNum == pt.v) {
				return pt;
			}
		}
		
		return PType.ERROR;
	}
	
	public String toString() {
		if (isNormalParam()) {
			return PType.NORMAL_PARAM + "_" + pNum + (isStatic ? "_S" : "");
		} else {
			return getType().toString();
		}
	}

	public static ParamNum readIn(final ObjectInputStream in) throws IOException, ClassNotFoundException {
		final ParamNum pn = (ParamNum) in.readObject();
		
		return (pn != null ? findOrCreateInCache(pn) : null);
	}

}
