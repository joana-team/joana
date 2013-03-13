/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.spec.ast;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor.FlowAstException;
import edu.kit.joana.wala.util.ParamNum;
import edu.kit.joana.wala.util.ParamNum.PType;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class SimpleParameter extends Parameter {

	/**
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static abstract class Part {

		public static enum Type { NORMAL, WILDCARD, ARRAY, RESULT, EXC, STATE };

		public final String name;

		public Part(String name) {
			this.name = name;
		}

		public String toString() {
			return name;
		}

		public boolean equals(Object other) {
			return (other instanceof Part ? equals((Part) other) : false); 
		}
		
		public boolean equals(Part other) {
			return getType() == other.getType() && name.equals(other.name);
		}

		public abstract Type getType();
	}

	/**
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static class NormalPart extends Part {

		public NormalPart(String name) {
			super(name);
		}

		@Override
		public Type getType() {
			return Type.NORMAL;
		}

	}

	/**
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static class Wildcard extends Part {

		public Wildcard() {
			super("*");
		}

		@Override
		public Type getType() {
			return Type.WILDCARD;
		}

	}

	/**
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static class ArrayContent extends Part {

		public ArrayContent() {
			super("[]");
		}

		@Override
		public Type getType() {
			return Type.ARRAY;
		}

	}

	/**
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static class Result extends Part {

		public Result() {
			super("\\result");
		}

		@Override
		public Type getType() {
			return Type.RESULT;
		}

	}

	/**
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static class ExceptionValue extends Part {

		public ExceptionValue() {
			super("\\exc");
		}

		@Override
		public Type getType() {
			return Type.EXC;
		}

	}

	/**
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static class ProgramState extends Part {

		public ProgramState() {
			super("\\state");
		}

		@Override
		public Type getType() {
			return Type.STATE;
		}

	}

	private final LinkedList<Part> parts = new LinkedList<Part>();

	private ParamNum mapped2param = ParamNum.createSpecial(PType.UNMAPPED_VAL);

	public SimpleParameter() {}

	public SimpleParameter(Part part) {
		this.parts.add(part);
	}

	/**
	 * Returns parameter number in special format. 
	 */
	public ParamNum getMappedTo() {
		return mapped2param;
	}

	public boolean isMapped() {
		return !mapped2param.isUnmapped();
	}

	public boolean isStatic() {
		return mapped2param.isStaticVarNoNum();
	}

	public void setMappedTo(final ParamNum paramNum) {
		this.mapped2param = paramNum;
	}

	public boolean endsWithWildcard() {
		return parts.getLast().getType() == Part.Type.WILDCARD;
	}

	public List<Part> getParts() {
		return Collections.unmodifiableList(parts);
	}

	public void addFirstPart(Part part) {
		this.parts.addFirst(part);
	}

	public void addLastPart(Part part) {
		this.parts.addLast(part);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (Part p : parts) {
			sb.append(p.toString());
			sb.append('.');
		}

		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}

		return sb.toString();
	}

	@Override
	public Type getType() {
		return Type.PARAM;
	}

	@Override
	public void accept(FlowAstVisitor visitor) throws FlowAstException {
		visitor.visit(this);
	}

	public Part getRoot() {
		return parts.getFirst();
	}

	public boolean equals(Parameter other) {
		if (other instanceof SimpleParameter) {
			SimpleParameter spOther = (SimpleParameter) other;

			if (parts.size() == spOther.parts.size()) {
				for (int index = 0; index < parts.size(); index++) {
					Part thisPart = parts.get(index);
					Part otherPart = spOther.parts.get(index);

					if (!thisPart.equals(otherPart)) {
						return false;
					}
				}

				return true;
			}
		}

		return false;
	}

}
