/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.matchers;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Operation;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;

/**
 * Matcher for field accesses - a node matches it if it represents an access of the given
 * kind (read/write) to a given field.
 * @author Martin Mohr &lt;martin.mohr@kit.edu&gt;
 */
public abstract class EveryAccess implements Matcher {
	/** field name of accesses to be matched */
	protected final String fieldName;

	/** operation to be matched */
	protected final SDGNode.Operation op;

	public EveryAccess(String fieldName, SDGNode.Operation op) {
		this.fieldName = fieldName;
		this.op = op;
	}

	@Override
	public boolean matches(SDGNode n, SDG sdg) {
		if (BytecodeLocation.OBJECT_FIELD != n.getBytecodeIndex() && BytecodeLocation.STATIC_FIELD != n.getBytecodeIndex()) return false;
		if (!fieldName.equals(n.getBytecodeName())) return false;
		return (ceClosureContains(n, sdg, op));
	}

	private static boolean ceClosureContains(SDGNode n, SDG sdg, Operation op) {
		Stack<SDGNode> toDo = new Stack<SDGNode>();
		Set<SDGNode> visited = new HashSet<SDGNode>();
		toDo.push(n);
		while (!toDo.isEmpty()) {
			SDGNode next = toDo.pop();
			if (visited.contains(next)) continue;
			visited.add(next);
			if (next.getOperation() == op) return true;
			for (SDGEdge eIn : sdg.getIncomingEdgesOfKind(n, SDGEdge.Kind.CONTROL_DEP_EXPR)) {
				toDo.push(eIn.getSource());
			}
			for (SDGEdge eOut : sdg.getOutgoingEdgesOfKindUnsafe(n, SDGEdge.Kind.CONTROL_DEP_EXPR)) {
				toDo.push(eOut.getTarget());
			}
		}
		return false;
	}
}
