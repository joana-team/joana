/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.io;

import java.io.PrintStream;
import java.util.Collection;

import edu.kit.joana.api.annotations.IFCAnnotation;
import edu.kit.joana.api.annotations.AnnotationType;
import edu.kit.joana.api.sdg.SDGAttribute;
import edu.kit.joana.api.sdg.SDGClass;
import edu.kit.joana.api.sdg.SDGInstruction;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGMethodExitNode;
import edu.kit.joana.api.sdg.SDGFormalParameter;
import edu.kit.joana.api.sdg.SDGPhi;
import edu.kit.joana.api.sdg.SDGProgramPartVisitor;




public class IFCAnnotationDumper extends SDGProgramPartVisitor<Void, Void> {

	protected PrintStream out;

	public IFCAnnotationDumper(PrintStream out) {
		this.out = out;
	}

	private void dumpType(AnnotationType type) {
		out.print(type);
	}

	private void dumpLevel(String level) {
		out.print(level);
	}

	@Override
	protected Void visitMethod(SDGMethod method, Void v) {
		out.print(method.getSignature().toBCString());
		return null;
	}

	@Override
	protected Void visitParameter(SDGFormalParameter param, Void v) {
		out.print("param ");
		out.print(param.getIndex());
		out.print(" of method ");
		visitMethod(param.getOwner(), v);
		return null;
		// param \\d+ of method .*
	}

	@Override
	protected Void visitInstruction(SDGInstruction instr, Void v) {
		out.print("instr ");
		out.print("(");
		visitMethod(instr.getOwner(), v);
		out.print(":");
		out.print(instr.getBytecodeIndex());
		out.print(")");
		return null;
		// instr \\((.*?):(\\d+)\\)

	}

	protected Void visitExit(SDGMethodExitNode exitNode, Void v) {
		out.print("exit node of method ");
		visitMethod(exitNode.getOwner(), v);
		return null;
	}

	public void dumpAnnotation(IFCAnnotation ann) {
		dumpType(ann.getType());
		out.print("(");
		dumpLevel(ann.getLevel1());
		if (ann.getType() == AnnotationType.DECLASS) {
			out.print("->");
			dumpLevel(ann.getLevel2());
		}
		out.print(") - ");
		ann.getProgramPart().acceptVisitor(this, null);
	}

	public void dumpAnnotations(Collection<IFCAnnotation> annotations) {
		for (IFCAnnotation ann : annotations) {
			dumpAnnotation(ann);
			out.println();
		}
	}

	@Override
	protected Void visitPhi(SDGPhi phi, Void data) {
		throw new UnsupportedOperationException("not implemented yet!");
	}

	@Override
	protected Void visitClass(SDGClass cl, Void data) {
		out.print(cl.getTypeName());
		return null;
	}

	@Override
	protected Void visitAttribute(SDGAttribute a, Void data) {
		out.print(a.getName());
		return null;
	}

}
