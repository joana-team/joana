/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.test.util;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Operation;
import edu.kit.joana.ifc.sdg.graph.slicer.IntraproceduralDataSlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.IntraproceduralSlicerBackward;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;

/**
 * Lightweight mechanism to intelligently select nodes in an SDG. It is closer
 * to the SDG than e.g. the joana.api-approach.
 * 
 * @author Martin Mohr &lt;martin.mohr@kit.edu&gt;
 */
public class SDGAnalyzer {

	private final SDG sdg;

	public SDGAnalyzer(SDG sdg) {
		this.sdg = sdg;
	}

	/**
	 * Returns all nodes in the given sdg which correspond to modifications or
	 * assignments of a given field in a given method.
	 * 
	 * @param sdg
	 *            sdg to be searched
	 * @param methodName
	 *            name of method to collect nodes in
	 * @param fieldName
	 *            name of field which modifications and assignments are to be
	 *            searched for
	 * @return all nodes in the given sdg which correspond to modifications or
	 *         assignments of a given field in a given method
	 */
	public Collection<SDGNode> collectModificationsAndAssignmentsInMethod(
			String methodName, String fieldName) {
		return collectOperatingNodesInMethod(methodName, fieldName,
				EnumSet.of(SDGNode.Operation.ASSIGN, SDGNode.Operation.MODIFY));
	}

	private Collection<SDGNode> collectOperatingNodesInMethod(
			String methodName, String fieldName,
			Set<SDGNode.Operation> allowedOps) {
		SDGNode entry = locateEntryOf(methodName);
		if (entry == null) {
			return Collections.emptyList();
		} else {
			return collectOperatingNodes(sdg.getNodesOfProcedure(entry),
					fieldName, allowedOps);
		}
	}

	/**
	 * Returns the entry node of a method with the given name, or {@code null}
	 * if no such node could be found.
	 * 
	 * @param methodName
	 *            name of method to locate
	 * @return the entry node of the method with the given name, or {@code null}
	 *         if no such node could be found
	 */
	public SDGNode locateEntryOf(String methodName) {
		Map<SDGNode, Set<SDGNode>> byProc = sdg.sortByProcedures();
		for (SDGNode nEntry : byProc.keySet()) {
			if (nEntry.getBytecodeMethod().equals(methodName)) {
				return nEntry;
			}
		}

		return null;
	}

	/**
	 * Returns the entry nodes of all methods with the given name. The returned
	 * collection is empty, if no such node could be found.
	 * 
	 * @param methodName
	 *            name of method to locate
	 * @return the entry nodes of all methods with the given name, or
	 *         {@code null} if no such node could be found
	 */
	public Collection<SDGNode> locateAllEntriesOf(String methodName) {
		Map<SDGNode, Set<SDGNode>> byProc = sdg.sortByProcedures();
		Collection<SDGNode> ret = new LinkedList<SDGNode>();
		for (SDGNode nEntry : byProc.keySet()) {
			if (nEntry.getBytecodeMethod().equals(methodName)) {
				ret.add(nEntry);
			}
		}

		return ret;
	}

	public boolean isLocatable(String methodName) {
		Map<SDGNode, Set<SDGNode>> byProc = sdg.sortByProcedures();
		for (SDGNode nEntry : byProc.keySet()) {
			if (nEntry.getBytecodeMethod().equals(methodName)) {
				return true;
			}
		}

		return false;
	}

	private Collection<SDGNode> collectOperatingNodes(
			Collection<SDGNode> nodes, String fieldName,
			Set<Operation> allowedOps) {
		List<SDGNode> ret = new LinkedList<SDGNode>();
		for (SDGNode n : nodes) {
			if (allowedOps.contains(n.getOperation())
					&& refersTo(nodes, n, fieldName)) {
				ret.add(n);
			} else if (n.getKind() == SDGNode.Kind.ACTUAL_OUT) {
				IntraproceduralSlicerBackward slicer = new IntraproceduralDataSlicerBackward(sdg);
				Collection<SDGNode> s = slicer.slice(n);
				for (SDGNode n0 : s) {
					if (allowedOps.contains(n0.getOperation()) && refersTo(nodes, n, fieldName)) {
						ret.add(n);
						break;
					}
				}
			}
		}

		return ret;
	}

	private boolean refersTo(Collection<SDGNode> nodes, SDGNode n,
			String fieldName) {
		for (SDGEdge e : sdg.getOutgoingEdgesOfKind(n,
				SDGEdge.Kind.CONTROL_DEP_EXPR)) {
			SDGNode np = e.getTarget();
			if (nodes.contains(np)) {
				int bcIndex = np.getBytecodeIndex();
				if (bcIndex == BytecodeLocation.STATIC_FIELD
						|| bcIndex == BytecodeLocation.OBJECT_FIELD
						|| bcIndex == BytecodeLocation.ARRAY_FIELD) {
					String bcName = np.getBytecodeName();
					return bcName.contains(fieldName);
				}
			}
		}

		return false;
	}

	public Collection<SDGNode> collectReferencesInMethod(String methodName,
			String fieldName) {
		return collectOperatingNodesInMethod(methodName, fieldName,
				EnumSet.of(SDGNode.Operation.REFERENCE));
	}

	public Collection<String> collectAllMethodNames() {
		List<String> ret = new LinkedList<String>();
		for (SDGNode n : sdg.vertexSet()) {
			if (n.getKind() == SDGNode.Kind.ENTRY) {
				ret.add(n.getBytecodeMethod());
			}
		}
		return ret;
	}

	public Collection<SDGNode> collectCalls(String calleeMethodName) {
		Collection<SDGNode> ret = new LinkedList<SDGNode>();
		Collection<SDGNode> calleeEntries = locateAllEntriesOf(calleeMethodName);
		for (SDGNode calleeEntry : calleeEntries) {
			for (SDGNode n : sdg.vertexSet()) {
				if (n.getKind() == SDGNode.Kind.ENTRY) {
					ret.addAll(collectCalls(n, calleeEntry));
				}
			}
		}

		return ret;
	}

	public Collection<SDGNode> collectCallsInMethod(String callerMethodName,
			String calleeMethodName) {
		SDGNode callerEntry = locateEntryOf(callerMethodName);
		SDGNode calleeEntry = locateEntryOf(calleeMethodName);
		if (callerEntry == null || calleeEntry == null) {
			return Collections.emptyList();
		} else {
			return collectCalls(callerEntry, calleeEntry);
		}
	}

	public Collection<SDGNode> collectAllCallsInMethods(String callerMethodName,
			String calleeMethodName) {
		Collection<SDGNode> ret = new LinkedList<SDGNode>();
		Collection<SDGNode> callerEntries = locateAllEntriesOf(callerMethodName);
		Collection<SDGNode> calleeEntries = locateAllEntriesOf(calleeMethodName);
		for (SDGNode callerEntry : callerEntries) {
			for (SDGNode calleeEntry : calleeEntries) {
				ret.addAll(collectCalls(callerEntry, calleeEntry));
			}
		}
		
		return ret;
	}

	private Collection<SDGNode> collectCalls(SDGNode callerEntry,
			SDGNode calleeEntry) {
		
		Collection<SDGNode> ret = new LinkedList<SDGNode>();
		for (SDGNode callNode : sdg.getCallers(calleeEntry)) {
			if (callNode.getProc() == callerEntry.getProc()) {
				ret.add(callNode);
			}
		}
		return ret;
	}
}
