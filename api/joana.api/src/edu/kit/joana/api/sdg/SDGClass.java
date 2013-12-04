/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.util.JavaType;
import edu.kit.joana.util.Pair;




public class SDGClass implements SDGProgramPart {

	private final JavaType typeName;
	private final Set<SDGNode> declNodes;
	private final Set<SDGAttribute> attributes;
	private final Set<SDGMethod> methods;

	SDGClass(JavaType typeName, Collection<SDGNode> declNodes, Map<String, Pair<Set<SDGNode>, Set<SDGNode>>> attributeNodes,
			Set<SDGNode> methodEntryNodes, SDG sdg) {
		this.typeName = typeName;
		this.declNodes = new HashSet<SDGNode>();
		this.declNodes.addAll(declNodes);
		this.attributes = createSDGAttributes(attributeNodes);
		this.methods = createSDGMethods(methodEntryNodes, sdg);
	}

	private Set<SDGAttribute> createSDGAttributes(Map<String, Pair<Set<SDGNode>, Set<SDGNode>>> attributeNodes) {
		Set<SDGAttribute> ret = new HashSet<SDGAttribute>();
		if (attributeNodes != null) {
			for (Map.Entry<String, Pair<Set<SDGNode>, Set<SDGNode>>> entry : attributeNodes.entrySet()) {
				ret.add(new SDGAttribute(this, entry.getKey(), entry.getValue().getFirst(), entry.getValue().getSecond()));
			}
		}
		return ret;
	}

	private Set<SDGMethod> createSDGMethods(Set<SDGNode> methodEntryNodes,
			SDG sdg) {
		Set<SDGMethod> ret = new HashSet<SDGMethod>();
		if (methodEntryNodes != null) {
			for (SDGNode entry : methodEntryNodes) {
				ret.add(new SDGMethod(sdg, entry));
			}
		}
		return ret;
	}

	public String getDescription() {
		StringBuffer sb = new StringBuffer();
		sb.append("class ");
		sb.append(typeName.toHRString());
		sb.append("\n");
		sb.append("attributes: ");
		sb.append("\n");
		for (SDGAttribute attribute : attributes) {
			sb.append("\t");
			sb.append(attribute.getName());
			sb.append(": ");
			sb.append(attribute.getType());
			sb.append("\n");
		}

		sb.append("methods: ");
		sb.append("\n");
		for (SDGMethod method : methods) {
			sb.append("\t");
			sb.append(method.getSignature().toStringHRShort());
			sb.append("\n");
		}

		return sb.toString();
	}

	public Set<SDGNode> getDeclarationNodes() {
		return declNodes;
	}

	public Set<SDGAttribute> getAttributes() {
		return attributes;
	}

	public Set<SDGMethod> getMethods() {
		return methods;
	}

	public String toString() {
		return typeName.toHRString();
	}

	public JavaType getTypeName() {
		return typeName;
	}

	@Override
	public <R, D> R acceptVisitor(SDGProgramPartVisitor<R, D> v, D data) {
		return v.visitClass(this, data);
	}

	@Override
	public SDGMethod getOwningMethod() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
		+ ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result + ((methods == null) ? 0 : methods.hashCode());
		result = prime * result
		+ ((typeName == null) ? 0 : typeName.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof SDGClass)) {
			return false;
		}
		SDGClass other = (SDGClass) obj;
		if (attributes == null) {
			if (other.attributes != null) {
				return false;
			}
		} else if (!attributes.equals(other.attributes)) {
			return false;
		}
		if (methods == null) {
			if (other.methods != null) {
				return false;
			}
		} else if (!methods.equals(other.methods)) {
			return false;
		}
		if (typeName == null) {
			if (other.typeName != null) {
				return false;
			}
		} else if (!typeName.equals(other.typeName)) {
			return false;
		}
		return true;
	}

	@Override
	public boolean covers(SDGNode node) {

		for (SDGNode declNode: getDeclarationNodes()) {
			if (node.equals(declNode)) {
				return true;
			}
		}

		for (SDGAttribute a : getAttributes()) {
			if (a.covers(node)) {
				return true;
			}
		}

		for (SDGMethod m : getMethods()) {
			if (m.covers(node)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public Collection<SDGNode> getAttachedNodes() {
		Set<SDGNode> ret = new HashSet<SDGNode>();

		ret.addAll(getDeclarationNodes());

		for (SDGAttribute a : getAttributes()) {
			ret.addAll(a.getAttachedNodes());
		}

		for (SDGMethod m : getMethods()) {
			ret.addAll(m.getAttachedNodes());
		}

		return ret;
	}

	@Override
	public Collection<SDGNode> getAttachedSourceNodes() {
		Set<SDGNode> ret = new HashSet<SDGNode>();

		ret.addAll(getDeclarationNodes());

		for (SDGAttribute a : getAttributes()) {
			ret.addAll(a.getAttachedSourceNodes());
		}

		for (SDGMethod m : getMethods()) {
			ret.addAll(m.getAttachedSourceNodes());
		}

		return ret;
	}

	@Override
	public Collection<SDGNode> getAttachedSinkNodes() {
		Set<SDGNode> ret = new HashSet<SDGNode>();


		ret.addAll(getDeclarationNodes());

		for (SDGAttribute a : getAttributes()) {
			ret.addAll(a.getAttachedSinkNodes());
		}

		for (SDGMethod m : getMethods()) {
			ret.addAll(m.getAttachedSinkNodes());
		}

		return ret;
	}

	@Override
	public SDGProgramPart getCoveringComponent(SDGNode node) {

		for (SDGNode declNode: getDeclarationNodes()) {
			if (node.equals(declNode)) {
				return this;
			}
		}

		for (SDGAttribute a : getAttributes()) {
			if (a.covers(node)) {
				return a;
			}
		}

		for (SDGMethod m : getMethods()) {
			if (m.covers(node)) {
				return m.getCoveringComponent(node);
			}
		}

		return null;
	}


}
