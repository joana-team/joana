/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ifc.sdg.util.JavaType;
import edu.kit.joana.ifc.sdg.util.JavaType.Format;
import edu.kit.joana.util.Pair;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.*;




public class SDGClass implements SDGProgramPart {

	private final JavaType typeName;
	private final Set<SDGAttribute> attributes;
	private final Set<SDGMethod> methods;

	public SDGClass(JavaType typeName, Collection<SDGNode> declNodes, Map<String, Pair<Set<SDGNode>, Set<SDGNode>>> attributeNodes,
      Set<SDGNode> methodEntryNodes, SDG sdg, TIntObjectHashMap<HashSet<SDGNode>> sdgByProc) {
		this.typeName = typeName;
		this.attributes = createSDGAttributes(attributeNodes);
		this.methods = createSDGMethods(methodEntryNodes, sdg, sdgByProc);
	}

	private Set<SDGAttribute> createSDGAttributes(Map<String, Pair<Set<SDGNode>, Set<SDGNode>>> attributeNodes) {
		Set<SDGAttribute> ret = new HashSet<SDGAttribute>();
		if (attributeNodes != null) {
			for (Map.Entry<String, Pair<Set<SDGNode>, Set<SDGNode>>> entry : attributeNodes.entrySet()) {
				Pair<Set<SDGNode>, Set<SDGNode>> accesses = entry.getValue();
				SDGNode node;
				if (!accesses.getFirst().isEmpty()) {
					node = accesses.getFirst().iterator().next();
				} else if (!accesses.getSecond().isEmpty()) {
					node = accesses.getSecond().iterator().next();
				} else {
					throw new IllegalStateException("dangling attribute!");
				}
				JavaType type = JavaType.parseSingleTypeFromString(node.getType(), Format.BC);
				ret.add(new SDGAttribute(this, entry.getKey(), type));
			}
		}
		return ret;
	}

	private Set<SDGMethod> createSDGMethods(Set<SDGNode> methodEntryNodes, SDG sdg,
			TIntObjectHashMap<HashSet<SDGNode>> sdgByProc) {
		Set<SDGMethod> ret = new HashSet<SDGMethod>();
		if (methodEntryNodes != null) {
			for (SDGNode entry : methodEntryNodes) {
				boolean isStatic = true; // we have a static method unless there is a formal-in parameter with index 0 (--> this-pointer)
				for (SDGNode formalIn : sdg.getFormalInsOfProcedure(entry)) {
					if (formalIn.getBytecodeName() != null) {
						int paramIndex = BytecodeLocation.getRootParamIndex(formalIn.getBytecodeName());
						if (paramIndex == 0) {
							isStatic = false;
							break;
						}
					}
				}
				SDGMethod m = new SDGMethod(JavaMethodSignature.fromString(entry.getBytecodeMethod()), entry.getClassLoader(), isStatic);
				ret.add(m);
				for (SDGNode nInstr : sdgByProc.get(entry.getProc())) {
					if (nInstr.getBytecodeIndex() >= 0) {
						if (nInstr.getKind() == SDGNode.Kind.CALL) {
							m.addCall(newCall(m, nInstr, sdg));
						} else {
							m.addInstruction(new SDGInstruction(m, nInstr.getBytecodeIndex(), nInstr.getLabel(), nInstr.getType(), nInstr.getOperation().toString()));
						}
					} else if (BytecodeLocation.isPhiNode(nInstr)) {
						m.addPhi(new SDGPhi(m, nInstr));
					}
				}

				final Set<String> localVariables = new HashSet<>();
				for (SDGNode node : sdgByProc.get(entry.getProc())) {
					if (node.getLocalDefNames() != null) {
						for (String localVariable : node.getLocalDefNames()) {
							localVariables.add(localVariable);
						}
					}
					if (node.getLocalUseNames() != null) {
						for (String localVariable : node.getLocalUseNames()) {
							localVariables.add(localVariable);
						}
					}
				}
				for (String localVariable : localVariables) {
					m.addLocalVariable(new SDGLocalVariable(m, localVariable));
				}
			}
		}
		return ret;
	}

	private SDGCall newCall(SDGMethod owner, SDGNode n,  SDG sdg) {
		SDGCall newCall = new SDGCall(owner, n.getBytecodeIndex(), n.getLabel(), n.getType(), n.getOperation().toString());

		// add actual parameters
		for (SDGEdge e : sdg.getOutgoingEdgesOfKindUnsafe(n, SDGEdge.Kind.CONTROL_DEP_EXPR)) {
			SDGNode node = e.getTarget();
			if (node.getKind() == SDGNode.Kind.ACTUAL_IN || node.getKind() == SDGNode.Kind.ACTUAL_OUT) {
				newCall.addActualParameter(node);
			}
		}

		// add possible call targets
		List<SDGEdge> callEdges = sdg.getOutgoingEdgesOfKindUnsafe(n, SDGEdge.Kind.CALL);
		if (callEdges.isEmpty() && n.getUnresolvedCallTarget() != null) {
			newCall.addPossibleCallTarget(JavaMethodSignature.fromString(n.getUnresolvedCallTarget()));
		} else {
			for (SDGEdge callEdge : callEdges) {
				SDGNode tgt = callEdge.getTarget();
				newCall.addPossibleCallTarget(JavaMethodSignature.fromString(tgt.getBytecodeMethod()));
			}
		}
		return newCall;
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

	public Set<SDGAttribute> getAttributes() {
		return attributes;
	}

	public Set<SDGMethod> getMethods() {
		return methods;
	}

	@Override
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
		return result;
	}

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
		if (typeName == null) {
			if (other.typeName != null) {
				return false;
			}
		} else if (!typeName.equals(other.typeName)) {
			return false;
		}
		return true;
	}
}
