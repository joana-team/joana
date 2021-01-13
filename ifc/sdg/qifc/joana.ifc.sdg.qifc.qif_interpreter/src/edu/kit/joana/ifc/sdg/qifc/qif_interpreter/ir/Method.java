package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeName;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.wala.core.PDG;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

		m.printInstructions();

		int paramMum = m.sdgMethod.getParameters().size();
		System.out.println("Parameter Types");
		for(int i = 0; i < paramMum; i++) {
			System.out.println(m.pdg.getParamType(i).getName());
		}

		return m;
	}

	// try some stuff
	private void printInstructions() {
		List<SSAInstruction> instructionList = Arrays.asList(ir.getInstructions()).stream().filter(i -> !(i == null)).collect(
				Collectors.toList());Collectors.toList();

		for (SSAInstruction i: instructionList) {
			System.out.println(i.toString());
			System.out.println("Def: " + i.getDef());
			int uses = i.getNumberOfUses();
			for (int j = 0; j < uses; j++) {
				System.out.println(i.getUse(j) + " " + ir.getSymbolTable().getValueString(i.getUse(j)));
				if (ir.getSymbolTable().isConstant(i.getUse(j))) {
					System.out.println("Constant: " + ir.getSymbolTable().getConstantValue(i.getUse(j)));
				}
			}
		}
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

}
