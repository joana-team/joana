/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;



final class BytecodeCentricSDGMethodPartWriter extends SDGProgramPartWriter {

	static final BytecodeCentricSDGMethodPartWriter INSTANCE = new BytecodeCentricSDGMethodPartWriter();

	private BytecodeCentricSDGMethodPartWriter() {
	}

	@Override
	public String writeSDGProgramPart(SDGProgramPart part) {
		return part.acceptVisitor(this, null);
	}

	@Override
	protected String visitMethod(SDGMethod m, Void data) {
		return m.getSignature().toBCString();
	}

	@Override
	protected String visitParameter(SDGFormalParameter p, Void data) {
		return p.getOwningMethod().getSignature().toBCString() + "->" + "p"
				+ p.getIndex();
	}

	@Override
	protected String visitExit(SDGMethodExitNode e, Void data) {
		return e.getOwningMethod().getSignature().toBCString() + "->" + "exit";
	}

	@Override
	protected String visitInstruction(SDGInstruction i, Void data) {
		return i.getOwningMethod().getSignature().toBCString() + ":"
				+ i.getBytecodeIndex();
	}

	@Override
	protected String visitPhi(SDGPhi phi, Void data) {
		throw new UnsupportedOperationException("not implemented yet!");
	}

	@Override
	protected String visitClass(SDGClass cl, Void data) {
		return cl.getTypeName().toString();
	}

	@Override
	protected String visitAttribute(SDGAttribute a, Void data) {
		return a.toString();
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitActualParameter(edu.kit.joana.api.sdg.SDGActualParameter, java.lang.Object)
	 */
	@Override
	protected String visitActualParameter(SDGActualParameter ap, Void data) {
		return visitCall(ap.getOwningCall(), data) + "->" + "a" + ap.getIndex();
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
		return visitCall(c.getOwningCall(), data) + "->" + "ret";
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitCallExceptionNode(edu.kit.joana.api.sdg.SDGCallExceptionNode, java.lang.Object)
	 */
	@Override
	protected String visitCallExceptionNode(SDGCallExceptionNode c, Void data) {
		return visitCall(c.getOwningCall(), data) + "->" + "exc";
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitException(edu.kit.joana.api.sdg.SDGMethodExceptionNode, java.lang.Object)
	 */
	@Override
	protected String visitException(SDGMethodExceptionNode e, Void data) {
		return e.getOwningMethod().getSignature().toBCString() + "->" + "exc";
	}

}

public abstract class SDGProgramPartWriter extends SDGProgramPartVisitor<String, Void> {
	public abstract String writeSDGProgramPart(SDGProgramPart part);

	public static SDGProgramPartWriter getStandardVersion() {
		return BytecodeCentricSDGMethodPartWriter.INSTANCE;
	}
}
