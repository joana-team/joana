/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.wala.objecttree;

import java.util.HashMap;
import java.util.Set;


import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;

import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.util.Util;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public abstract class AbstractKey2Origin implements IKey2Origin {

	private HashMap<InstanceKey, Set<InstanceKeyOrigin>> iKeyOrigin;
	private HashMap<PointerKey, Set<PointerKeyOrigin>> pKeyOrigin;

	private HashMap<Integer, InstanceKeyOriginImpl> iKeys;
	private HashMap<Integer, PointerKeyOriginImpl> pKeys;

	public AbstractKey2Origin() {
		iKeyOrigin = new HashMap<InstanceKey, Set<InstanceKeyOrigin>>();
		pKeyOrigin = new HashMap<PointerKey, Set<PointerKeyOrigin>>();
		iKeys = new HashMap<Integer, InstanceKeyOriginImpl>();
		pKeys = new HashMap<Integer, PointerKeyOriginImpl>();
	}

	void addOrigin(InstanceKey iKey, InstanceKeyOrigin origin) {
		Set<InstanceKeyOrigin> set = iKeyOrigin.get(iKey);
		if (set == null) {
			set = HashSetFactory.make();
			iKeyOrigin.put(iKey, set);
		}
		set.add(origin);
	}

	void addOrigin(PointerKey pKey, PointerKeyOrigin origin) {
		Set<PointerKeyOrigin> set = pKeyOrigin.get(pKey);
		if (set == null) {
			set = HashSetFactory.make();
			pKeyOrigin.put(pKey, set);
		}
		set.add(origin);
	}

	public Set<InstanceKeyOrigin> getOrigin(InstanceKey iKey) {
		return iKeyOrigin.get(iKey);
	}

	public Set<PointerKeyOrigin> getOrigin(PointerKey pKey) {
		return pKeyOrigin.get(pKey);
	}

	private InstanceKeyOrigin checkExistance(InstanceKeyOriginImpl ik) {
		InstanceKeyOrigin origin = iKeys.get(ik.hashCode);

		if (origin == null) {
			iKeys.put(ik.hashCode, ik);
			origin = ik;
		}

		return origin;
	}

	public InstanceKeyOrigin findOrigin(CGNode node, NewSiteReference nsRef) {
		InstanceKeyOriginImpl ik = new InstanceKeyOriginImpl(node, nsRef);

		return checkExistance(ik);
	}

	public InstanceKeyOrigin findOrigin(CGNode node, ProgramCounter counter, TypeReference tRef) {
		InstanceKeyOriginImpl ik = new InstanceKeyOriginImpl(node, counter, tRef);

		return checkExistance(ik);
	}

	public InstanceKeyOrigin findOrigin(TypeReference tRef) {
		InstanceKeyOriginImpl ik = new InstanceKeyOriginImpl(tRef);

		return checkExistance(ik);
	}

	private class InstanceKeyOriginImpl extends InstanceKeyOrigin {

		private final NewSiteReference nsRef;
		private final CGNode node;
		private final ProgramCounter counter;
		private final TypeReference tRef;
		private final Type type;
		private final int hashCode;

		private InstanceKeyOriginImpl(CGNode node, NewSiteReference nsRef) {
			this.nsRef = nsRef;
			this.node = node;
			this.counter = new ProgramCounter(nsRef.getProgramCounter());
			this.tRef = null;
			this.type = Type.ALLOCATION;
			this.hashCode = calcHashCode();
		}

		private InstanceKeyOriginImpl(CGNode node, ProgramCounter counter, TypeReference tRef) {
			this.nsRef = null;
			this.node = node;
			this.counter = counter;
			this.tRef = tRef;
			this.type = Type.PEI;
			this.hashCode = calcHashCode();
		}

		private InstanceKeyOriginImpl(TypeReference tRef) {
			this.nsRef = null;
			this.node = null;
			this.counter = null;
			this.tRef = tRef;
			this.type = Type.CLASS;
			this.hashCode = calcHashCode();
		}

		public ProgramCounter getCounter() {
			return counter;
		}

		public CGNode getNode() {
			return node;
		}

		public NewSiteReference getNsRef() {
			return nsRef;
		}

		public TypeReference getTRef() {
			return tRef;
		}

		public Type getType() {
			return type;
		}

		public boolean equals(Object obj) {
			if (obj instanceof InstanceKeyOriginImpl) {
				return hashCode == obj.hashCode();
/*				InstanceKeyOriginImpl iKey = (InstanceKeyOriginImpl) obj;
				return iKey.type == type && iKey.nsRef == nsRef &&
					iKey.counter == counter && iKey.node == node &&
					iKey.tRef == tRef; */
			}

			return false;
		}

		private int calcHashCode() {
			int tmp = 4711;

			if (nsRef != null) {
				tmp += nsRef.hashCode();
			}
			if (node != null) {
				tmp += node.hashCode();
			}
			if (counter != null) {
				tmp += counter.hashCode();
			}
			if (tRef != null) {
				tmp += tRef.hashCode();
			}
			tmp += type.hashCode();

			return tmp;
		}

		public int hashCode() {
			return hashCode;
		}

		public String toString() {
			String str = null;

			switch (type) {
			case ALLOCATION:
				str = "new " + Util.typeName(nsRef.getDeclaredType().getName())
					+ " @ " + Util.methodName(node.getMethod());
				break;
			case CLASS:
				str = "class " + Util.typeName(tRef.getName());
				break;
			case PEI:
				str = "pei " + counter + " @ " +
					Util.methodName(node.getMethod());
				break;
			}

			return str;
		}
	}

	private PointerKeyOrigin checkExistance(PointerKeyOriginImpl pk) {
		PointerKeyOrigin origin = pKeys.get(pk.hashCode);

		if (origin == null) {
			pKeys.put(pk.hashCode, pk);
			origin = pk;
		}

		return origin;
	}

	public PointerKeyOrigin findOrigin(ParameterField field) {
		PointerKeyOriginImpl pk = new PointerKeyOriginImpl(field);

		return checkExistance(pk);
	}

	public PointerKeyOrigin findOrigin(InstanceKey iKey, ParameterField field) {
		PointerKeyOriginImpl pk = new PointerKeyOriginImpl(iKey, field);

		return checkExistance(pk);
	}

	public PointerKeyOrigin findOrigin(CGNode node, int ssaVar) {
		PointerKeyOriginImpl pk = new PointerKeyOriginImpl(node, ssaVar);

		return checkExistance(pk);
	}


	private class PointerKeyOriginImpl extends PointerKeyOrigin {

		private final ParameterField field;
		private final InstanceKey iKey;
		private final Type type;
		private final int ssaVar;
		private final CGNode node;
		private final int hashCode;

		private PointerKeyOriginImpl(InstanceKey iKey, ParameterField field) {
			this.field = field;
			this.iKey = iKey;
			this.ssaVar = -1;
			this.node = null;
			this.type = Type.INSTANCE_FIELD;
			this.hashCode = calcHashCode();
		}

		private PointerKeyOriginImpl(ParameterField field) {
			this.field = field;
			this.iKey = null;
			this.ssaVar = -1;
			this.node = null;
			if (field.isStatic()) {
				this.type = Type.STATIC_FIELD;
			} else if (field.isArray()) {
				this.type = Type.ARRAY_FIELD;
			} else {
				throw new IllegalArgumentException("Field is neither static nor array: " + field);
			}
			this.hashCode = calcHashCode();
		}

		private PointerKeyOriginImpl(CGNode node, int ssaVar) {
			this.field = null;
			this.iKey = null;
			this.ssaVar = ssaVar;
			this.node = node;
			this.type = Type.SSA_VAR;
			this.hashCode = calcHashCode();
		}

		public ParameterField getField() {
			return field;
		}

		public InstanceKey getIKey() {
			return iKey;
		}

		public Type getType() {
			return type;
		}

		public boolean equals(Object obj) {
			if (obj instanceof PointerKeyOriginImpl) {
				return hashCode == obj.hashCode();
				/*
				PointerKeyOriginImpl pkOrig = (PointerKeyOriginImpl) obj;
				return pkOrig.type == type && pkOrig.field == field &&
					pkOrig.iKey == iKey && pkOrig.node == node &&
					pkOrig.ssaVar == ssaVar; */
			}

			return false;
		}

		private int calcHashCode() {
			int tmp = 4711;

			if (iKey != null) {
				tmp += iKey.hashCode();
			}
			if (field != null) {
				tmp += field.hashCode();
			}
			if (node != null) {
				tmp += node.hashCode();
			}
			tmp += ssaVar;
			tmp += type.hashCode();

			return tmp;
		}

		public int hashCode() {
			return hashCode;
		}

		public String toString() {
			String str;
			if (type == Type.STATIC_FIELD) {
				str = "<static> " + Util.fieldName(field);
			} else if (type == Type.INSTANCE_FIELD) {
				str = iKey.toString() + " " + Util.fieldName(field);
			} else {
				str = Util.methodName(node.getMethod()) + "[v" + ssaVar + "]";
			}

			return str;
		}

		public CGNode getNode() {
			return node;
		}

		public int getSSA() {
			return ssaVar;
		}
	}

}
