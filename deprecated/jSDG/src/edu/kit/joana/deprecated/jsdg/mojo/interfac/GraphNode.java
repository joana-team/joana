/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.mojo.interfac;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.graph.INodeWithNumber;
import com.ibm.wala.util.strings.Atom;

/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public abstract class GraphNode implements INodeWithNumber {

	public static enum Mode { IN, OUT, BOTH }

	private Mode mode;

	private static int counter = 0;

	private int id = counter++;

	private GraphNode(Mode mode) {
		this.mode = mode;
	}

	public abstract TypeReference getType();

	public abstract Atom getName();

	public abstract IClass getDeclaringClass();

	public abstract boolean isStatic();

	public int hashCode() {
		return getName().hashCode() + 4711 * getType().hashCode();
	}

	public abstract boolean equals(Object obj);

	private final static Atom THIS = Atom.findOrCreate("this".getBytes());

	public static GraphNode makeMethodParam(IMethod method, int paramNum) {
		Atom name;
		if (!method.isStatic() && paramNum == 0) {
			name = THIS;
		} else {
			String tmp = method.getLocalVariableName(0, paramNum);
			if (tmp == null) {
				tmp = "p" + paramNum;
			}
			name = Atom.findOrCreate(tmp.getBytes());
		}

		TypeReference type = method.getParameterType(paramNum);

		return new LocalVarNode(method, type, name, Mode.IN);
	}

	private final static Atom RETURN = Atom.findOrCreate("return".getBytes());

	public static GraphNode makeMethodReturn(IMethod method) {
		if (method.getReturnType() == TypeReference.Void) {
			throw new IllegalArgumentException("No return value for void methods.");
		}

		return new LocalVarNode(method, method.getReturnType(), RETURN, Mode.OUT);
	}

	private final static Atom EXCEPTION = Atom.findOrCreate("throws".getBytes());

	public static GraphNode makeMethodException(IMethod method) {
		return new LocalVarNode(method, TypeReference.JavaLangThrowable, EXCEPTION, Mode.OUT);
	}

	public static GraphNode makeObjectField(IField field, Mode mode) {
		return new ObjectFieldNode(field, mode);
	}

	public GraphNode makeArrayContentField(GraphNode parent, TypeReference contentType, Mode mode) {
		return new ArrayContentNode(parent, contentType, mode);
	}

	public int getGraphNodeId() {
		return id;
	}

	public void setGraphNodeId(int number) {
		this.id = number;
	}


	public Mode getMode() {
		return mode;
	}

	public boolean isIn() {
		return mode == Mode.IN || mode == Mode.BOTH;
	}

	public boolean isOut() {
		return mode == Mode.OUT || mode == Mode.BOTH;
	}

	public void setIn() {
		if (mode != Mode.IN) {
			mode = Mode.BOTH;
		}
	}

	public void setOut() {
		if (mode != Mode.OUT) {
			mode = Mode.OUT;
		}
	}

	private static class LocalVarNode extends GraphNode {

		private final TypeReference type;
		private final Atom name;
		private final IMethod method;

		private LocalVarNode(IMethod method, TypeReference type, Atom name, Mode mode) {
			super(mode);
			this.method = method;
			this.type = type;
			this.name = name;
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.deprecated.jsdg.mojo.interfac.GraphNode#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj instanceof LocalVarNode) {
				LocalVarNode other = (LocalVarNode) obj;
				return getName() == other.getName() && getType().equals(other.getType())
					&& method.equals(other.method);
			}

			return false;
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.deprecated.jsdg.mojo.interfac.GraphNode#getDeclaringClass()
		 */
		@Override
		public IClass getDeclaringClass() {
			return method.getDeclaringClass();
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.deprecated.jsdg.mojo.interfac.GraphNode#getName()
		 */
		@Override
		public Atom getName() {
			return name;
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.deprecated.jsdg.mojo.interfac.GraphNode#getType()
		 */
		@Override
		public TypeReference getType() {
			return type;
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.deprecated.jsdg.mojo.interfac.GraphNode#isStatic()
		 */
		@Override
		public boolean isStatic() {
			return false;
		}

		public String toString() {
			return name + "@" + method.getName() + ":" + type.getName();
		}

	}

	private static class ObjectFieldNode extends GraphNode {

		private final IField field;

		private ObjectFieldNode(IField field, Mode mode) {
			super(mode);
			this.field = field;
		}

		public String toString() {
			return field.getName() + ":" + field.getFieldTypeReference().getName();
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.deprecated.jsdg.mojo.interfac.GraphNode#getType()
		 */
		@Override
		public TypeReference getType() {
			return field.getFieldTypeReference();
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.deprecated.jsdg.mojo.interfac.GraphNode#getName()
		 */
		@Override
		public Atom getName() {
			return field.getName();
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.deprecated.jsdg.mojo.interfac.GraphNode#getDeclaringClass()
		 */
		@Override
		public IClass getDeclaringClass() {
			return field.getDeclaringClass();
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.deprecated.jsdg.mojo.interfac.GraphNode#isStatic()
		 */
		@Override
		public boolean isStatic() {
			return field.isStatic();
		}

		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj instanceof ObjectFieldNode) {
				ObjectFieldNode other = (ObjectFieldNode) obj;
				return getName() == other.getName() && getType().equals(other.getType())
					&& getDeclaringClass().equals(other.getDeclaringClass());
			}

			return false;
		}


	}

	private static class ArrayContentNode extends GraphNode {

		private static final Atom ARR = Atom.findOrCreate("[]".getBytes());

		private final TypeReference type;

		private final GraphNode parent;

		private ArrayContentNode(GraphNode parent, TypeReference type, Mode mode) {
			super(mode);

			assert type != null;
			assert parent != null;

			this.type = type;
			this.parent = parent;
		}

		public String toString() {
			return "[" + type.getName() + "]";
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.deprecated.jsdg.mojo.interfac.GraphNode#getType()
		 */
		@Override
		public TypeReference getType() {
			return type;
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.deprecated.jsdg.mojo.interfac.GraphNode#getName()
		 */
		@Override
		public Atom getName() {
			return ARR;
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.deprecated.jsdg.mojo.interfac.GraphNode#getDeclaringClass()
		 */
		@Override
		public IClass getDeclaringClass() {
			return parent.getDeclaringClass();
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.deprecated.jsdg.mojo.interfac.GraphNode#isStatic()
		 */
		@Override
		public boolean isStatic() {
			return false;
		}

		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj instanceof ArrayContentNode) {
				ArrayContentNode other = (ArrayContentNode) obj;
				return getType().equals(other.getType()) && parent.equals(other.parent);
			}

			return false;
		}
	}
}
