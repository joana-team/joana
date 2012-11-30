/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

public class PDGField {

	private final boolean isWrite;

	public final PDGNode node;
	public final ParameterField field;
	public final PDGNode base;
	public final PDGNode accfield;
	public final PDGNode index;

	private PDGField(PDGNode node, ParameterField field, boolean isWrite, PDGNode base, PDGNode accfield, PDGNode index) {
		this.node = node;
		this.field = field;
		this.isWrite = isWrite;
		this.base = base;
		this.accfield = accfield;
		this.index = index;
	}

	public static PDGField fieldGet(PDGNode instr, PDGNode base, PDGNode accfield, ParameterField field) {
		assert instr.getKind() == PDGNode.Kind.HREAD;
		assert field.isField() && !field.isStatic();
		PDGField f = new PDGField(instr, field, false, base, accfield, null);
		return f;
	}

	public static PDGField fieldSet(PDGNode instr, PDGNode base, PDGNode accfield, ParameterField field) {
		assert instr.getKind() == PDGNode.Kind.HWRITE;
		assert field.isField() && !field.isStatic();
		PDGField f = new PDGField(instr, field, true, base, accfield, null);
		return f;
	}

	public static PDGField fieldGetStatic(PDGNode instr, PDGNode accfield, ParameterField field) {
		assert instr.getKind() == PDGNode.Kind.HREAD;
		assert field.isStatic();
		PDGField f = new PDGField(instr, field, false, null, accfield, null);
		return f;
	}

	public static PDGField fieldSetStatic(PDGNode instr, PDGNode accfield, ParameterField field) {
		assert instr.getKind() == PDGNode.Kind.HWRITE;
		assert field.isStatic();
		PDGField f = new PDGField(instr, field, true, null, accfield, null);
		return f;
	}

	public static PDGField arrayGet(PDGNode instr, PDGNode base, PDGNode accfield, PDGNode index, ParameterField field) {
		assert instr.getKind() == PDGNode.Kind.HREAD;
		assert field.isArray();
		PDGField f = new PDGField(instr, field, false, base, accfield, index);
		return f;
	}

	public static PDGField arraySet(PDGNode instr, PDGNode base, PDGNode accfield, PDGNode index, ParameterField field) {
		assert instr.getKind() == PDGNode.Kind.HWRITE;
		assert field.isArray();
		PDGField f = new PDGField(instr, field, true, base, accfield, index);
		return f;
	}

	public static PDGField formIn(PDGNode fIn, ParameterField field) {
		assert fIn.getKind() == PDGNode.Kind.FORMAL_IN;
		PDGField f = new PDGField(fIn, field, false, null, null, null);
		return f;
	}

	public static PDGField formOut(PDGNode fOut, ParameterField field) {
		assert fOut.getKind() == PDGNode.Kind.FORMAL_OUT;
		PDGField f = new PDGField(fOut, field, true, null, null, null);
		return f;
	}


	public static PDGField actIn(PDGNode aIn, ParameterField field) {
		assert aIn.getKind() == PDGNode.Kind.ACTUAL_IN;
		PDGField f = new PDGField(aIn, field, false, null, null, null);
		return f;
	}

	public static PDGField actOut(PDGNode aOut, ParameterField field) {
		assert aOut.getKind() == PDGNode.Kind.ACTUAL_OUT;
		PDGField f = new PDGField(aOut, field, true, null, null, null);
		return f;
	}

	public boolean isRead() {
		return !isWrite;
	}

	public boolean isWrite() {
		return isWrite;
	}

	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (obj instanceof PDGField) {
			PDGField other = (PDGField) obj;
			return node.equals(other.node) && field.equals(other.field);
		}

		return false;
	}

	public int hashCode() {
		return node.hashCode();
	}

	public String toString() {
		return "(" + node + "->" + field + ")";
	}
}
