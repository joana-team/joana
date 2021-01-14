package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.IR;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.wala.core.PDG;

/**
 *  data class to cluster together some objects pertaining to a method
 */
public class Method {

	private PDG pdg;

	public IR getIr() {
		return ir;
	}

	private IR ir;
	private SDGMethod sdgMethod;
	private CGNode cg;

	public static Method getEntryMethodFromProgram(Program p) {
		Method m = new Method();
		m.pdg = p.findEntrypointPDG();
		m.cg = m.pdg.cgNode;
		m.ir = m.cg.getIR();
		m.sdgMethod = p.getEntryMethod().snd;

		return m;
	}

	/**
	 * Returns the type of the i-th parameter of the function
	 * @param i the index of the parameter. This is **not** the value number of the parameter in the SSA representation!
	 * @return matchign type, if none is found it is assumed that the type is userdefined and CUSTOM is returned
	 */
	public Type getParamType(int i) {
		String name = pdg.getParamType(i).getName().toString();

		switch(name) {
		case "I":
			return Type.INTEGER;
		default:
			return Type.CUSTOM;
		}
	}

	public boolean isConstant(int valueNum) {
		return ir.getSymbolTable().isConstant(valueNum);
	}

	public int getIntConstant(int valNum) {
		assert (ir.getSymbolTable().isIntegerConstant(valNum));
		return (int) ir.getSymbolTable().getConstantValue(valNum);
	}

	// TODO: how do we find out what type a local variable is ????
	// is the information already stored somewhere or do we have to keep track of it ourselves ???
	public Type getType(int valueNum) {
		return null;
	}

}
