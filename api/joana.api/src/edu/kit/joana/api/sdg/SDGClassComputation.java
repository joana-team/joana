/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.api.annotations.AnnotationType;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Operation;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.ifc.sdg.util.JavaType;
import edu.kit.joana.ifc.sdg.util.JavaType.Format;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.util.Pair;
import gnu.trove.map.hash.TIntObjectHashMap;

public class SDGClassComputation {

	private static final Logger debug = Log.getLogger(Log.L_API_DEBUG);

	private final SDG sdg;

	// type |--> set of declaration sites of objects of that type
	private final Map<JavaType, Set<SDGNode>> declNodes = new HashMap<JavaType, Set<SDGNode>>();

	// type |--> (attribute name --> (sites where that attribute is accessed as
	// a source, sites where that attribute is accessed as a sink)
	private final Map<JavaType, Map<String, Pair<Set<SDGNode>, Set<SDGNode>>>> seenAttributes = new HashMap<JavaType, Map<String, Pair<Set<SDGNode>, Set<SDGNode>>>>();

	// type |--> entry nodes of methods implemented by that type
	private final Map<JavaType, Set<SDGNode>> seenMethods = new HashMap<JavaType, Set<SDGNode>>();

	public SDGClassComputation(SDG sdg) {
		this.sdg = sdg;
		compute();
	}

	public Set<SDGNode> getNodes(SDGClass cl) {
		Set<SDGNode> ret = new HashSet<SDGNode>();

		ret.addAll(getDeclarationNodes(cl));

		for (SDGAttribute a : cl.getAttributes()) {
			ret.addAll(getRootNodes(a));
		}

		for (SDGMethod m : cl.getMethods()) {
			ret.addAll(getRootNodes(m));
		}

		return ret;
	}

	public Set<SDGNode> getDeclarationNodes(SDGClass cl) {
		Set<SDGNode> ret = declNodes.get(cl.getTypeName());
		if (ret != null) {
			return ret;
		} else {
			return Collections.emptySet();
		}
	}

	public Set<SDGNode> getRootNodes(SDGAttribute a) {
		Set<SDGNode> ret = new HashSet<SDGNode>();
		ret.addAll(getSourceNodes(a));
		ret.addAll(getSinkNodes(a));
		return ret;
	}

	/**
	 * @param a
	 * @return
	 */
	public Set<SDGNode> getSourceNodes(SDGAttribute a) {
		return getSrcSnkNodes(a, AnnotationType.SOURCE);
	}

	/**
	 * @param a
	 * @return
	 */
	public Set<SDGNode> getSinkNodes(SDGAttribute a) {
		return getSrcSnkNodes(a, AnnotationType.SINK);
	}

	public Set<SDGNode> getSrcSnkNodes(SDGAttribute a, AnnotationType type) {
		// return seenAttributes.get(a.getType()).get(a.getName()).getFirst();
		Map<String, Pair<Set<SDGNode>, Set<SDGNode>>> saOfType = seenAttributes.get(a.getDeclaringType());
		if (saOfType != null) {
			Pair<Set<SDGNode>, Set<SDGNode>> accessesOfA = saOfType.get(a.getName());
			if (accessesOfA != null) {
				switch (type) {
				case SOURCE:
					return accessesOfA.getFirst();
				case SINK:
					return accessesOfA.getSecond();
				default:
					throw new IllegalArgumentException("method not supposed to be called with type = " + type);
				}
			}
		}
		return Collections.emptySet();
	}

	public Set<SDGNode> getRootNodes(SDGMethod m) {
		Set<SDGNode> ret = new HashSet<SDGNode>();
		ret.addAll(getEntries(m));
		for (SDGFormalParameter p : m.getParameters()) {
			ret.addAll(getInRoots(p));
			ret.addAll(getOutRoots(p));
		}
		ret.addAll(getExits(m.getExit()));
		return ret;
	}

	public Set<SDGNode> getEntries(SDGMethod m) {
		Set<SDGNode> declClassMethods = seenMethods.get(m.getSignature().getDeclaringType());
		if (declClassMethods != null) {
			Set<SDGNode> ret = new HashSet<SDGNode>();
			for (SDGNode nEntry : declClassMethods) {
				if (nEntry.getBytecodeMethod().equals(m.getSignature().toBCString())) {
					ret.add(nEntry);
				}
			}
			return ret;
		}
		return Collections.emptySet();
	}

	public Set<SDGNode> getInRoots(SDGFormalParameter param) {
		return getRoots(param, SDGNode.Kind.FORMAL_IN);
	}

	public Set<SDGNode> getOutRoots(SDGFormalParameter param) {
		return getRoots(param, SDGNode.Kind.FORMAL_OUT);
	}

	public Set<SDGNode> getExits(SDGMethodExitNode exit) {
		return getRoots(exit);
	}

	public Set<SDGNode> getExceptions(SDGMethodExceptionNode exc) {
		return getRoots(exc);
	}

	private Set<SDGNode> getRoots(SDGFormalParameter param, Kind k) {
		Set<SDGNode> ret = new HashSet<SDGNode>();
		for (SDGNode entry : getEntries(param.getOwningMethod())) {
			for (SDGEdge entryOut : sdg.outgoingEdgesOf(entry)) {
				SDGNode n = entryOut.getTarget();
				if (n.getKind() == k && BytecodeLocation.isNormalFormalParameter(n)) {
					int paramIndex = BytecodeLocation.getRootParamIndex(n.getBytecodeName());
					if (paramIndex == param.getIndex()) {
						ret.add(n);
						break;
					}
				}
			}
		}
		return ret;
	}

	private Set<SDGNode> getRoots(SDGMethodExitNode exit) {
		Set<SDGNode> ret = new HashSet<SDGNode>();
		for (SDGNode entry : getEntries(exit.getOwningMethod())) {
			for (SDGNode n : sdg.getNodesOfProcedure(entry)) {
				if (n.getKind() == SDGNode.Kind.EXIT) {
					ret.add(n);
					break;
				}
			}
		}
		return ret;
	}

	private Set<SDGNode> getRoots(SDGMethodExceptionNode exc) {
		Set<SDGNode> ret = new HashSet<SDGNode>();
		for (SDGNode entry : getEntries(exc.getOwningMethod())) {
			for (SDGNode n : sdg.getNodesOfProcedure(entry)) {
				if (n.getKind().equals(SDGNode.Kind.FORMAL_OUT) && BytecodeLocation.EXCEPTION_PARAM.equals(n.getBytecodeName())) {
					ret.add(n);
					break;
				}
			}
		}
		return ret;
	}

	public Set<SDGNode> getNodes(SDGInstruction i) {
		Set<SDGNode> ret = new HashSet<SDGNode>();
		for (SDGNode entry : getEntries(i.getOwningMethod())) {
			for (SDGNode n : sdg.getNodesOfProcedure(entry)) {
				if (n.getBytecodeIndex() == i.getBytecodeIndex()) {
					ret.add(n);
					break;
				}
			}
		}
		return ret;
	}

	public Set<SDGNode> getInRoots(SDGActualParameter param) {
		return getRoots(param, SDGNode.Kind.ACTUAL_IN);
	}

	public Set<SDGNode> getOutRoots(SDGActualParameter param) {
		return getRoots(param, SDGNode.Kind.ACTUAL_OUT);
	}

	private Set<SDGNode> getRoots(SDGActualParameter param, SDGNode.Kind k) {
		Set<SDGNode> ret = new HashSet<SDGNode>();
		for (SDGNode call : getNodes(param.getOwningCall())) {
			for (SDGEdge e : sdg.outgoingEdgesOf(call)) {
				SDGNode n = e.getTarget();
				if (n.getKind() == k && BytecodeLocation.getRootParamIndex(n.getBytecodeName()) == param.getIndex()) {
					ret.add(n);
					break;
				}
			}
		}
		return ret;
	}

	public Set<SDGNode> getNodes(SDGCallReturnNode callRet) {
		Set<SDGNode> ret = new HashSet<SDGNode>();
		for (SDGNode call : getNodes(callRet.getOwningCall())) {
			for (SDGEdge e : sdg.outgoingEdgesOf(call)) {
				SDGNode n = e.getTarget();
				if (n.getBytecodeName() != null && n.getBytecodeName().equals(BytecodeLocation.RETURN_PARAM)) {
					ret.add(n);
					break;
				}
			}
		}
		return ret;
	}

	public Set<SDGNode> getNodes(SDGCallExceptionNode exc) {
		Set<SDGNode> ret = new HashSet<SDGNode>();
		for (SDGNode call : getNodes(exc.getOwningCall())) {
			for (SDGEdge e : sdg.outgoingEdgesOf(call)) {
				SDGNode n = e.getTarget();
				if (n.getBytecodeName() != null && n.getBytecodeName().equals(BytecodeLocation.EXCEPTION_PARAM)) {
					ret.add(n);
					break;
				}
			}
		}
		return ret;
	}

	public List<SDGClass> compute() {
		declNodes.clear();
		seenAttributes.clear();
		seenMethods.clear();
		List<SDGClass> result = new ArrayList<SDGClass>();
		TIntObjectHashMap<HashSet<SDGNode>> sdgByProc = new TIntObjectHashMap<HashSet<SDGNode>>();
		for (SDGNode node : sdg.vertexSet()) {
			HashSet<SDGNode> nodesOfProc = sdgByProc.get(node.getProc());
			if (nodesOfProc == null) {
				nodesOfProc = new HashSet<SDGNode>();
				sdgByProc.put(node.getProc(), nodesOfProc);
			}
			nodesOfProc.add(node);
			switch (node.getKind()) {
			case ENTRY:
				seenMethod(node);
				break;
			default:
				switch (node.getOperation()) {
				case ASSIGN:
				case MODIFY:
				case REFERENCE:
					if (node.getBytecodeIndex() >= 0) {
						seenAttribute(node);
					}
					break;
				case DECLARATION:
					seenDeclaration(node);
					break;
				default:
					break;
				}
			}
		}

		for (JavaType typeName : declNodes.keySet()) {
			result.add(new SDGClass(typeName, declNodes.get(typeName), seenAttributes.get(typeName), seenMethods
					.get(typeName), sdg, sdgByProc));
		}

		return result;
	}

	private void seenDeclaration(SDGNode declNode) {
		if (debug.isEnabled()) {
			debug.outln("seen declaration node " + declNode + " of type " + declNode.getType());
		}
		JavaType type = JavaType.parseSingleTypeFromString(declNode.getType(), Format.BC);
		addDeclarationNode(type, declNode);

	}

	private void addDeclarationNode(JavaType type, SDGNode declNode) {
		seenClass(type);
		declNodes.get(type).add(declNode);
	}

	private void seenMethod(SDGNode entry) {
		// we need this to fill the cache for entry nodes
		sdg.getEntry(entry);
		
		if (entry.getBytecodeName() != null) {
			int offset = entry.getBytecodeName().lastIndexOf('.');
			if (offset >= 0) {
				JavaType typeName = JavaType.parseSingleTypeFromString(entry.getBytecodeName().substring(0, offset),
						Format.HR);
				assert typeName != null;
				seenClass(typeName);

				Set<SDGNode> declaredMethods;
				if (!seenMethods.containsKey(typeName)) {
					declaredMethods = new HashSet<SDGNode>();
					seenMethods.put(typeName, declaredMethods);
				} else {
					declaredMethods = seenMethods.get(typeName);
				}
				declaredMethods.add(entry);
			}
		}
	}

	private void seenAttribute(SDGNode node) {
		Set<SDGNode> fieldNodes = new HashSet<SDGNode>();
		for (SDGEdge e : sdg.outgoingEdgesOf(node)) {
			if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
				int bcIndex = e.getTarget().getBytecodeIndex();
				if (bcIndex == BytecodeLocation.STATIC_FIELD || bcIndex == BytecodeLocation.OBJECT_FIELD
						|| bcIndex == BytecodeLocation.ARRAY_FIELD) {
					fieldNodes.add(e.getTarget());
				}
			}
		}

		for (SDGNode fNode : fieldNodes) {
			String bcMethod = fNode.getBytecodeName();
			int offset = bcMethod.lastIndexOf('.');
			if (offset >= 0) {
				JavaType typeName = JavaType.parseSingleTypeFromString(bcMethod.substring(0, offset), Format.BC);
				String attrName = bcMethod.substring(offset + 1);
				addAttributeNode(typeName, attrName, fNode, AnnotationType.SOURCE);
				addAttributeNode(typeName, attrName, fNode, AnnotationType.SINK);
				addDDReachableSinkNodes(typeName, attrName, fNode);
			}
		}
	}

	// TODO: Why is this kind of semi-interprocedural data-forward-slice
	// sufficient???
	private void addDDReachableSinkNodes(JavaType declaringClass, String attrName, SDGNode start) {
		if (start.getBytecodeIndex() == BytecodeLocation.ARRAY_FIELD
				|| start.getBytecodeIndex() == BytecodeLocation.OBJECT_FIELD
				|| start.getBytecodeIndex() == BytecodeLocation.STATIC_FIELD) {

			LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
			Set<SDGNode> done = new HashSet<SDGNode>();
			worklist.add(start);
			while (!worklist.isEmpty()) {
				SDGNode next = worklist.poll();

				for (SDGEdge e : sdg.outgoingEdgesOf(next)) {
					SDGNode n = e.getTarget();
					switch (e.getKind()) {
					case DATA_ALIAS:
					case DATA_DEP:
					case DATA_DEP_EXPR_REFERENCE:
					case DATA_DEP_EXPR_VALUE:
					case DATA_HEAP:
					case DATA_LOOP:
					case SUMMARY:
					case PARAMETER_STRUCTURE:
						if (!done.contains(n)) {
							worklist.add(n);
						}
						if (n.getKind() == SDGNode.Kind.ACTUAL_OUT || n.getOperation() == Operation.MODIFY) {
							addAttributeNode(declaringClass, attrName, n, AnnotationType.SINK);
						}
						done.add(n);
						break;
					default:
						break;
					}
				}
			}
		}
	}

	private void seenClass(JavaType type) {
		if (!declNodes.containsKey(type)) {
			declNodes.put(type, new HashSet<SDGNode>());
		}
	}

	private void addAttributeNode(JavaType declaringClass, String attrName, SDGNode node, AnnotationType type) {
		seenClass(declaringClass);
		Map<String, Pair<Set<SDGNode>, Set<SDGNode>>> attrMap;
		Set<SDGNode> attrSrcs;
		Set<SDGNode> attrSnks;
		if (!seenAttributes.containsKey(declaringClass)) {
			attrMap = new HashMap<String, Pair<Set<SDGNode>, Set<SDGNode>>>();
			seenAttributes.put(declaringClass, attrMap);
		} else {
			attrMap = seenAttributes.get(declaringClass);
		}

		if (!attrMap.containsKey(attrName)) {
			attrSrcs = new HashSet<SDGNode>();
			attrSnks = new HashSet<SDGNode>();
			attrMap.put(attrName, Pair.pair(attrSrcs, attrSnks));
		} else {
			Pair<Set<SDGNode>, Set<SDGNode>> p = attrMap.get(attrName);
			attrSrcs = p.getFirst();
			attrSnks = p.getSecond();
		}

		if (type == AnnotationType.SOURCE)
			attrSrcs.add(node);
		else {
			attrSnks.add(node);
		}
	}
}