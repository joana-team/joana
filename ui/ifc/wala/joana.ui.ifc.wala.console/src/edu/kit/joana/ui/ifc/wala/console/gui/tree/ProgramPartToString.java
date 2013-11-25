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

import edu.kit.joana.api.sdg.SDGActualParameter;
import edu.kit.joana.api.sdg.SDGAttribute;
import edu.kit.joana.api.sdg.SDGCall;
import edu.kit.joana.api.sdg.SDGCallExceptionNode;
import edu.kit.joana.api.sdg.SDGCallReturnNode;
import edu.kit.joana.api.sdg.SDGClass;
import edu.kit.joana.api.sdg.SDGInstruction;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGMethodExitNode;
import edu.kit.joana.api.sdg.SDGFormalParameter;
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
	public String visitParameter(SDGFormalParameter p, Void data) {
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

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitActualParameter(edu.kit.joana.api.sdg.SDGActualParameter, java.lang.Object)
	 */
	@Override
	protected String visitActualParameter(SDGActualParameter ap, Void data) {
		throw new UnsupportedOperationException("not implemented yet!");
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitCall(edu.kit.joana.api.sdg.SDGCall, java.lang.Object)
	 */
	@Override
	protected String visitCall(SDGCall c, Void data) {
		return visitInstruction(c, data);
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitCallReturnNode(edu.kit.joana.api.sdg.SDGCallReturnNode, java.lang.Object)
	 */
	@Override
	protected String visitCallReturnNode(SDGCallReturnNode c, Void data) {
		return "ret";
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitCallExceptionNode(edu.kit.joana.api.sdg.SDGCallExceptionNode, java.lang.Object)
	 */
	@Override
	protected String visitCallExceptionNode(SDGCallExceptionNode c, Void data) {
		return "exc";
	}
}

public abstract class ProgramPartToString extends SDGProgramPartVisitor<String, Void> {

	private static final ProgramPartToString STD = new StdProgramPartToString();

	public static ProgramPartToString getStandard() {
		return STD;
	}

}
