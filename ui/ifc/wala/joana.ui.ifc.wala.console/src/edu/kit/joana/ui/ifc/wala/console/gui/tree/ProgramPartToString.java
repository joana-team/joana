/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.gui.tree;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import edu.kit.joana.api.sdg.SDGAttribute;
import edu.kit.joana.api.sdg.SDGClass;
import edu.kit.joana.api.sdg.SDGInstruction;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGMethodExitNode;
import edu.kit.joana.api.sdg.SDGParameter;
import edu.kit.joana.api.sdg.SDGPhi;
import edu.kit.joana.api.sdg.SDGProgramPartVisitor;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;

class StdProgramPartToString extends ProgramPartToString {

	private static final Logger debug = Log.getLogger(Log.L_CONSOLE_DEBUG); 
	
	@Override
	public String visitClass(SDGClass cl, Void data) {
		return cl.toString();
	}

	@Override
	public String visitAttribute(SDGAttribute a, Void data) {
		return a.getName() + ": " + a.getType();
	}

	@Override
	public String visitMethod(SDGMethod m, Void data) {
		return m.toString();
	}

	@Override
	public String visitParameter(SDGParameter p, Void data) {
		return p.getInRoot().getLabel() + ": " + p.getType().toHRString();
	}

	@Override
	public String visitExit(SDGMethodExitNode e, Void data) {
		return "exit";
	}

	private static final NumberFormat NF = new DecimalFormat("0000");
	@Override
	public String visitInstruction(SDGInstruction i, Void data) {
		final SDGNode node = i.getNode();

		if (debug.isEnabled()) {
			return NF.format(i.getBytecodeIndex()) + ": " + node.getOperation()
				+ " # " + node.getLabel() + " of type " + node.getType();
		} else {
			return NF.format(i.getBytecodeIndex()) + ": " + node.getLabel();
		}
	}

	@Override
	public String visitPhi(SDGPhi phi, Void data) {
		return phi.toString();
	}

}

public abstract class ProgramPartToString extends SDGProgramPartVisitor<String, Void> {

	private static final ProgramPartToString STD = new StdProgramPartToString();

	public static ProgramPartToString getStandard() {
		return STD;
	}

}
