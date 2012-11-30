/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.pointsto;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.graph.INodeWithNumber;
import com.ibm.wala.util.strings.Atom;

import edu.kit.joana.wala.util.ParamNum;
import edu.kit.joana.wala.util.ParamNum.PType;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public abstract class PtsParameter implements INodeWithNumber, Serializable {

	private static final long serialVersionUID = 8450609110062859528L;

	private static int ID = 0;
	public final int id;
	private final TypeReference type;
	private final Map<String, PtsParameter> children = new HashMap<String, PtsParameter>();

	private int graphNodeId = -1;

	protected static int nextID() {
		return ID++;
	}

	PtsParameter(final int id, final TypeReference type) {
		if (type == null) {
			throw new IllegalArgumentException("Type my not be null.");
		}

		this.id = id;
		this.type = type;
	}

	public abstract void accept(PtsParameterVisitor visitor);

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeInt(id);
		out.writeInt(graphNodeId);
		out.writeObject(type);
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		try {
			final Field fId = PtsParameter.class.getDeclaredField("id");
			fId.setAccessible(true);
			final int newId = in.readInt();
			fId.setInt(this, newId);
			fId.setAccessible(false);

			final Field fGraphId = PtsParameter.class.getDeclaredField("graphNodeId");
			fGraphId.setAccessible(true);
			final int newGraphId = in.readInt();
			fGraphId.set(this, newGraphId);
			fGraphId.setAccessible(false);

			final Field fType = PtsParameter.class.getDeclaredField("type");
			fType.setAccessible(true);
			final TypeReference newType = (TypeReference) in.readObject();
			fType.set(this, newType);
			fType.setAccessible(false);
		} catch (SecurityException e) {
			throw new IOException(e);
		} catch (NoSuchFieldException e) {
			throw new IOException(e);
		} catch (IllegalArgumentException e) {
			throw new IOException(e);
		} catch (IllegalAccessException e) {
			throw new IOException(e);
		}
	}

	public int getGraphNodeId() {
		return graphNodeId;
	}

	public void setGraphNodeId(int number) {
		graphNodeId = number;
	}

	public Collection<PtsParameter> getChildren() {
		return children.values();
	}

	/**
	 * Computes a set of all children and nodes that are reachable through those children.
	 * @return A set of all children and nodes that are reachable through those children
	 */
	public Set<PtsParameter> getReachableChildren() {
		Set<PtsParameter> all = new HashSet<PtsParameter>();

		for (PtsParameter child : children.values()) {
			all.add(child);
			all.addAll(child.getReachableChildren());
		}

		return all;
	}

	public boolean hasArrayFieldChild() {
		if (type.isArrayType()) {
			final String name = ArrayFieldParameter.createName(type.getArrayElementType());
			return children.containsKey(name);
		}

		return false;
	}

	public PtsParameter getArrayFieldChild() {
		PtsParameter result = null;

		if (type.isArrayType()) {
			final String name = ArrayFieldParameter.createName(type.getArrayElementType());
			result = getChild(name);
		}

		return result;
	}

	public PtsParameter getChild(String name) {
		return children.get(name);
	}

	public boolean hasChild(String name) {
		return children.containsKey(name);
	}

	public PtsParameter getChild(IField name) {
		return getChild(NormalFieldParameter.getName(name));
	}

	public boolean hasChild(IField name) {
		return hasChild(NormalFieldParameter.getName(name));
	}

	public boolean hasChildren() {
		return children.size() > 0;
	}

	public TypeReference getType() {
		return type;
	}

	public int countSubtreeElements() {
		int number = 1;

		if (hasChildren()) {
			for (PtsParameter param : getChildren()) {
				number += param.countSubtreeElements();
			}
		}

		return number;
	}

	public String toString() {
		return "#" + id + " " + getName() + ":" + countSubtreeElements();
	}

	public String toLongString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName());
		if (hasChildren()) {
			sb.append("(");

			for (PtsParameter child : getChildren()) {
				sb.append(child.toLongString());
			}

			sb.append(")");
		}

		return sb.toString();
	}

	public PtsParameter getParent() {
		return null;
	}

	public final boolean isPrimitive() {
		return type.isPrimitiveType();
	}

	public boolean hasParent() {
		return !isRoot();
	}

	abstract public String getName();

	abstract public boolean isRoot();

	abstract public boolean hasParent(IField field);

	private static boolean equals(TypeReference t1, TypeReference t2) {
		return t1.getName().equals(t2.getName());
	}

	private static boolean equals(FieldReference t1, FieldReference t2) {
		return t1.getName().equals(t2.getName()) && equals(t1.getFieldType(), t2.getFieldType()) && equals(t1.getDeclaringClass(), t2.getDeclaringClass());
	}

	/**
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static class RootParameter extends PtsParameter {

		private static final long serialVersionUID = -7822761963437362325L;

		private final ParamNum parameterNum;
		private final MethodReference method;
		private final String name;

		/**
		 * Creates a root parameter. The parameterNum argument is expected to be already converted into the
		 * ParamNumberUtil format, where this pointer is -1, ...
		 */
		public RootParameter(final IMethod method, final ParamNum parameterNum) {
			this(method.getReference(), parameterNum);
		}

		public RootParameter(final MethodReference method, final ParamNum parameterNum) {
			super(nextID(), getParameterType(method, parameterNum));

			this.method = method;
			this.parameterNum = parameterNum;

			final PType val = parameterNum.getType();
			switch (val) {
			case EXCEPTION_VAL:
				this.name = method.getSelector().getName().toString() + "@exc";
				break;
			case RESULT_VAL:
				this.name = method.getSelector().getName().toString() + "@result";
				break;
			case THIS_VAL:
				this.name = method.getSelector().getName().toString() + "@this";
				break;
			case NORMAL_PARAM:
				this.name = method.getSelector().getName().toString() + "@" + parameterNum.getNum();
				break;
			default:
				throw new IllegalArgumentException("illegal parameter number for root param: " + parameterNum
						+ " - " + val);
			}
		}

		private RootParameter(final int id, final String name, final TypeReference tref, final ParamNum parameterNum) {
			super(id, tref);

			this.method = null;
			this.parameterNum = parameterNum;
			this.name = name;
		}

		public static RootParameter create(final int id, final String name, final String typeName, final ParamNum paramNum) {
			final TypeReference tref = TypeReference.findOrCreate(ClassLoaderReference.Application, typeName);

			final RootParameter rp = new RootParameter(id, name, tref, paramNum);

			return rp;
		}

		public void accept(PtsParameterVisitor visitor) {
			visitor.visit(this);
		}

		private void writeObject(java.io.ObjectOutputStream out) throws IOException {
			out.writeObject(parameterNum);
			out.writeObject(name);
		}

		private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
			try {
				final Field pN = getClass().getDeclaredField("parameterNum");
				pN.setAccessible(true);
				final ParamNum pNval = ParamNum.readIn(in);
				pN.set(this, pNval);
				pN.setAccessible(false);

				final Field n = getClass().getDeclaredField("name");
				n.setAccessible(true);
				final String nval = (String) in.readObject();
				n.set(this, nval);
				n.setAccessible(false);
			} catch (SecurityException e) {
				throw new IOException(e);
			} catch (NoSuchFieldException e) {
				throw new IOException(e);
			} catch (IllegalArgumentException e) {
				throw new IOException(e);
			} catch (IllegalAccessException e) {
				throw new IOException(e);
			}
		}

		/**
		 * Expects parameterNum to be converted to ParamNumberUtil format. 
		 */
		private static TypeReference getParameterType(MethodReference method, ParamNum parameterNum) {
			final PType val = parameterNum.getType();
			
			switch (val) {
			case EXCEPTION_VAL:
				// TODO look for declared exceptions - could further improve precision, but is not very likely to be used
				// Big investment, small benefit -> We leave it out.
				return TypeReference.JavaLangException;
			case RESULT_VAL:
				return method.getReturnType();
			case THIS_VAL:
				return method.getDeclaringClass();
			case NORMAL_PARAM:
				final int mrParamNum = parameterNum.getMethodReferenceNum();
				return method.getParameterType(mrParamNum);
			default:
				throw new IllegalArgumentException("Cannot determin type of paramenter no. " + parameterNum
						+ " of kind " + val);
			}
		}

		public String getName() {
			return name;
		}

		public int hashCode() {
			return name.hashCode();
		}

		public ParamNum getParamNum() {
			return parameterNum;
		}

		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			} else if (obj instanceof RootParameter) {
				RootParameter other = (RootParameter) obj;

				if (!parameterNum.isStaticVarNoNum()) {
					return parameterNum.equals(other.parameterNum)
						&& ((method != null && method.equals(other.method))
								|| (name.equals(other.name) && PtsParameter.equals(getType(), other.getType())));
				} else {
					return parameterNum.equals(other.parameterNum) && name.equals(other.name)
						&& PtsParameter.equals(getType(), other.getType());
				}
			}

			return false;
		}

		public String toString() {
			return "#" + id + " " + getName() + ":" + countSubtreeElements();
		}

		@Override
		public boolean hasParent(IField field) {
			return false;
		}

		@Override
		public boolean isRoot() {
			return true;
		}

	}

	/**
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static class NormalFieldParameter extends FieldParameter {

		private static final long serialVersionUID = 7826886795011098925L;

		private final FieldReference field;

		public NormalFieldParameter(final PtsParameter parent, final IField field) {
			super(nextID(), field.getFieldTypeReference(), parent, getName(field));

			this.field = field.getReference();
		}

		public void accept(PtsParameterVisitor visitor) {
			visitor.visit(this);
		}

		private NormalFieldParameter(final int id, final PtsParameter parent, final String name, final TypeReference tref) {
			super(id, tref, parent, name);

			this.field = FieldReference.findOrCreate(parent.getType(), Atom.findOrCreateAsciiAtom(name), tref);
		}

		public static NormalFieldParameter create(final int id, final PtsParameter parent, final String name, final String typeName) {
			final TypeReference tref = TypeReference.findOrCreate(ClassLoaderReference.Application, typeName);

			final NormalFieldParameter fp =  new NormalFieldParameter(id, parent, name, tref);

			return fp;
		}

		private static String getName(IField field) {
			return field.getName().toString();
		}

		public int hashCode() {
			return getName().hashCode() + (23 * getType().getName().hashCode());
		}

		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			} else if (obj instanceof NormalFieldParameter) {
				NormalFieldParameter other = (NormalFieldParameter) obj;

				return PtsParameter.equals(field, other.field) && parent.equals(other.parent);
			}

			return false;
		}

		@Override
		public boolean hasParent(IField field) {
			return this.field.equals(field.getReference()) || parent.hasParent(field);
		}

		public FieldReference getFieldRef() {
			return field;
		}

	}

	/**
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static class ArrayFieldParameter extends FieldParameter {

		private static final long serialVersionUID = 8316164208741231847L;

		public ArrayFieldParameter(FieldParameter parent, TypeReference elementType) {
			super(nextID(), elementType, parent, createName(elementType));
		}

		public ArrayFieldParameter(RootParameter parent, TypeReference elementType) {
			super(nextID(), elementType, parent, createName(elementType));
		}

		private ArrayFieldParameter(final int id, FieldParameter parent, TypeReference elementType) {
			super(id, elementType, parent, createName(elementType));
		}

		private ArrayFieldParameter(final int id, RootParameter parent, TypeReference elementType) {
			super(id, elementType, parent, createName(elementType));
		}

		public static PtsParameter create(final int id, final PtsParameter parent, final String typeName) {
			final TypeReference elemType = TypeReference.findOrCreate(ClassLoaderReference.Application, typeName);

			final ArrayFieldParameter afp;

			if (parent instanceof RootParameter) {
				afp = new  ArrayFieldParameter(id, (RootParameter) parent, elemType);
			} else if (parent instanceof FieldParameter) {
				afp = new ArrayFieldParameter(id, (FieldParameter) parent, elemType);
			} else {
				throw new IllegalArgumentException("parent shoud be root or field node, but is: " + parent.getClass().getName());
			}

			return afp;
		}

		public void accept(PtsParameterVisitor visitor) {
			visitor.visit(this);
		}

		private static String createName(TypeReference type) {
			return "[" + type.getName() + "]";
		}

		public int hashCode() {
			return parent.hashCode() + (31 * getType().getName().hashCode());
		}

		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			} else if (obj instanceof ArrayFieldParameter) {
				ArrayFieldParameter other = (ArrayFieldParameter) obj;

				return PtsParameter.equals(getType(), other.getType()) && parent.equals(other.parent);
			}

			return false;
		}

		@Override
		public boolean hasParent(IField field) {
			return parent.hasParent(field);
		}

	}

	/**
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static abstract class FieldParameter extends PtsParameter {

		private static final long serialVersionUID = 6422273848943443868L;

		final PtsParameter parent;
		final String name;

		private FieldParameter(final int id, final TypeReference type, final PtsParameter parent, final String name) {
			super(id, type);

			if (parent == null) {
				throw new IllegalArgumentException("Parent may not be null.");
			}

			this.parent = parent;
			this.name = name;
			this.parent.children.put(name, this);
		}

		@Override
		public boolean isRoot() {
			return false;
		}

		public final String getName() {
			return name;
		}

		public PtsParameter getParent() {
			return parent;
		}

	}

}
