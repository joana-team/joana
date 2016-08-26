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

import edu.kit.joana.api.annotations.AnnotationType;
import edu.kit.joana.api.annotations.IFCAnnotation;
import edu.kit.joana.api.sdg.SDGActualParameter;
import edu.kit.joana.api.sdg.SDGAttribute;
import edu.kit.joana.api.sdg.SDGCall;
import edu.kit.joana.api.sdg.SDGCallExceptionNode;
import edu.kit.joana.api.sdg.SDGCallReturnNode;
import edu.kit.joana.api.sdg.SDGClass;
import edu.kit.joana.api.sdg.SDGFieldOfParameter;
import edu.kit.joana.api.sdg.SDGFormalParameter;
import edu.kit.joana.api.sdg.SDGInstruction;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGMethodExceptionNode;
import edu.kit.joana.api.sdg.SDGMethodExitNode;
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

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitCall(edu.kit.joana.api.sdg.SDGCall, java.lang.Object)
	 */
	@Override
	protected Void visitCall(SDGCall c, Void data) {
		visitInstruction(c, data);
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitActualParameter(edu.kit.joana.api.sdg.SDGActualParameter, java.lang.Object)
	 */
	@Override
	protected Void visitActualParameter(SDGActualParameter ap, Void data) {
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitCallExceptionNode(edu.kit.joana.api.sdg.SDGCallExceptionNode, java.lang.Object)
	 */
	@Override
	protected Void visitCallExceptionNode(SDGCallExceptionNode c, Void data) {
		out.print("exception node of call ");
		visitCall(c.getOwningCall(), data);
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitCallReturnNode(edu.kit.joana.api.sdg.SDGCallReturnNode, java.lang.Object)
	 */
	@Override
	protected Void visitCallReturnNode(SDGCallReturnNode c, Void data) {
		out.print("return parameter node of call ");
		visitCall(c.getOwningCall(), data);
		return null;
	}

	protected Void visitExit(SDGMethodExitNode exitNode, Void v) {
		out.print("exit node of method ");
		visitMethod(exitNode.getOwningMethod(), v);
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

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitException(edu.kit.joana.api.sdg.SDGMethodExceptionNode, java.lang.Object)
	 */
	@Override
	protected Void visitException(SDGMethodExceptionNode excNode, Void data) {
		out.print("exceptionNode node of method ");
		visitMethod(excNode.getOwningMethod(), data);
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitFieldOfParameter(edu.kit.joana.api.sdg.SDGFieldOfParameter, java.lang.Object)
	 */
	@Override
	protected Void visitFieldOfParameter(SDGFieldOfParameter fop, Void data) {
		out.print(String.format("field '%s' of ", fop.getFieldName()));
		fop.getParent().acceptVisitor(this, data);
		return null;
	}

}
