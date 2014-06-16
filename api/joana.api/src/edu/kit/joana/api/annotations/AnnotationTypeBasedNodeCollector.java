/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.annotations;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.api.sdg.SDGActualParameter;
import edu.kit.joana.api.sdg.SDGAttribute;
import edu.kit.joana.api.sdg.SDGCall;
import edu.kit.joana.api.sdg.SDGCallExceptionNode;
import edu.kit.joana.api.sdg.SDGCallReturnNode;
import edu.kit.joana.api.sdg.SDGClass;
import edu.kit.joana.api.sdg.SDGClassComputation;
import edu.kit.joana.api.sdg.SDGFormalParameter;
import edu.kit.joana.api.sdg.SDGInstruction;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGMethodExitNode;
import edu.kit.joana.api.sdg.SDGPhi;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.api.sdg.SDGProgramPartVisitor;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.util.Pair;

/**
 * @author Martin Mohr
 */
public class AnnotationTypeBasedNodeCollector extends SDGProgramPartVisitor<Set<SDGNode>, AnnotationType> {

	private SDG sdg;
	private final SDGClassComputation pp2NodeTrans;
	private final Map<Pair<SDGProgramPart, AnnotationType>, Set<SDGNode>> cache = new HashMap<Pair<SDGProgramPart, AnnotationType>, Set<SDGNode>>();
	private final Map<SDGNode, Set<SDGProgramPart>> coveringCandidates = new HashMap<SDGNode, Set<SDGProgramPart>>();

	public AnnotationTypeBasedNodeCollector(SDG sdg) {
		this(sdg, new SDGClassComputation(sdg));
	}

	public AnnotationTypeBasedNodeCollector(SDG sdg, SDGClassComputation pp2NodeTrans) {
		this.sdg = sdg;
		this.pp2NodeTrans = pp2NodeTrans;
	}

	public void init(SDGProgram program) {
		this.cache.clear();
		this.coveringCandidates.clear();
	}

	public Set<SDGNode> collectNodes(SDGProgramPart ppart, AnnotationType type) {
		Set<SDGNode> result = cache.get(Pair.pair(ppart, type));
		if (result == null) {
			result = ppart.acceptVisitor(this, type);
			cache.put(Pair.pair(ppart, type), result);
		}
		for (SDGNode n : result) {
			Set<SDGProgramPart> cands;
			if (coveringCandidates.containsKey(n)) {
				cands = coveringCandidates.get(n);
			} else {
				cands = new HashSet<SDGProgramPart>();
				coveringCandidates.put(n, cands);
			}
			cands.add(ppart);
		}
		return result;
	}

	public Set<SDGProgramPart> getCoveringCandidates(SDGNode n) {
		Set<SDGProgramPart> ret = coveringCandidates.get(n);
		if (ret != null) {
			return coveringCandidates.get(n);
		} else {
			return Collections.emptySet();
		}
	}
	@Override
	protected Set<SDGNode> visitParameter(SDGFormalParameter param, AnnotationType type) {
		assert !pp2NodeTrans.getOutRoots(param).isEmpty() || !pp2NodeTrans.getInRoots(param).isEmpty();
		Set<SDGNode> ret = new HashSet<SDGNode>();
		addAllAppropriateParameterNodesFrom(pp2NodeTrans.getInRoots(param), type, ret);
		if (pp2NodeTrans.getInRoots(param) != null && type == AnnotationType.SINK) {
			/**
			 *  also mark all act_out nodes of this parameters as source, to capture
			 *  possible information flows from this parameter to the environment
			 *  just marking the formal-in would not suffice
			 *  TODO: think about this!
			 **/
			addAllAppropriateParameterNodesFrom(getCorrespondingActOuts(param), type, ret);
		}

		addAllAppropriateParameterNodesFrom(pp2NodeTrans.getOutRoots(param), type, ret);
		return ret;
	}

	private Set<SDGNode> getCorrespondingActOuts(SDGFormalParameter param) {
		return getCorrespondingActuals(pp2NodeTrans.getOutRoots(param));
	}

	private Set<SDGNode> getCorrespondingActuals(Set<SDGNode> formals) {
		if (formals.isEmpty()) {
			return Collections.emptySet();
		} else {
			Set<SDGNode> ret = new HashSet<SDGNode>();
			for (SDGNode formal : formals) {
				switch (formal.getKind()) {
				case FORMAL_IN:
					ret.addAll(sdg.getActualIns(formal));
				case FORMAL_OUT:
					ret.addAll(sdg.getActualOuts(formal));
				default:
					throw new IllegalStateException();
				}
			}
			return ret;
		}
	}


	@Override
	protected Set<SDGNode> visitInstruction(SDGInstruction instr, AnnotationType type) {
		if (instr instanceof SDGCall) {
			throw new IllegalArgumentException(instr.toString());
		}
		return pp2NodeTrans.getNodes(instr);
	}

	@Override
	protected Set<SDGNode> visitClass(SDGClass cl, AnnotationType type) {
		Set<SDGNode> ret = new HashSet<SDGNode>();
		for (SDGAttribute a : cl.getAttributes()) {
			ret.addAll(visitAttribute(a, type));
		}

		for (SDGMethod m : cl.getMethods()) {
			ret.addAll(visitMethod(m, type));
		}

		return ret;
	}

	@Override
	protected Set<SDGNode> visitAttribute(SDGAttribute a, AnnotationType type) {
		Collection<SDGNode> toSelect;
		switch (type) {
		case SOURCE:
			toSelect = pp2NodeTrans.getSourceNodes(a);
			break;
		case SINK:
			toSelect =  pp2NodeTrans.getSinkNodes(a);
			break;
		default:
			throw new UnsupportedOperationException("not implemented yet!");
		}
		return new HashSet<SDGNode>(toSelect);
	}

	@Override
	protected Set<SDGNode> visitMethod(SDGMethod method, AnnotationType type) {
		Set<SDGNode> ret = new HashSet<SDGNode>();
		ret.addAll(pp2NodeTrans.getEntries(method));
		for (SDGFormalParameter fp : method.getParameters()) {
			ret.addAll(visitParameter(fp, type));
		}
		ret.addAll(visitExit(method.getExit(), type));
		return ret;
	}

	@Override
	protected Set<SDGNode> visitExit(SDGMethodExitNode exit, AnnotationType type) {
		Set<SDGNode> ret = new HashSet<SDGNode>();
		addAllAppropriateParameterNodesFrom(pp2NodeTrans.getExits(exit), type, ret);
		return ret;
	}

	@Override
	protected Set<SDGNode> visitPhi(SDGPhi phi, AnnotationType type) {
		return Collections.emptySet();
	}

	private static final boolean isActualNodeOfKind(SDGNode node, AnnotationType type) {
		return ((type == AnnotationType.SINK && node.getKind() == SDGNode.Kind.ACTUAL_IN) || (type == AnnotationType.SOURCE && node
				.getKind() == SDGNode.Kind.ACTUAL_OUT));
	}

	private static final boolean isFormalNodeOfKind(SDGNode node, AnnotationType type) {
		return (type == AnnotationType.SINK && node.getKind() == SDGNode.Kind.FORMAL_IN)
				|| (type == AnnotationType.SOURCE && (node.getKind() == SDGNode.Kind.EXIT || node.getKind() == SDGNode.Kind.FORMAL_OUT));
	}

	@SuppressWarnings("unused")
	private static final boolean compatibleNodes(SDGNode node1, SDGNode node2) {
		return compatibleNodesOfDirection(node1, node2, AnnotationType.SOURCE)
				|| compatibleNodesOfDirection(node1, node2, AnnotationType.SINK);
	}

	private static final boolean compatibleNodesOfDirection(SDGNode node1, SDGNode node2, AnnotationType dir) {
		if (dir == AnnotationType.SINK)
			return (node1.getKind() == SDGNode.Kind.FORMAL_IN && node2.getKind() == SDGNode.Kind.FORMAL_IN)
					|| (node1.getKind() == SDGNode.Kind.ACTUAL_OUT && node2.getKind() == SDGNode.Kind.ACTUAL_OUT);
		else
			return ((node1.getKind() == SDGNode.Kind.FORMAL_OUT || node1.getKind() == SDGNode.Kind.EXIT) && (node2
					.getKind() == SDGNode.Kind.FORMAL_OUT || node2.getKind() == SDGNode.Kind.EXIT))
					|| (node1.getKind() == SDGNode.Kind.ACTUAL_IN && node2.getKind() == SDGNode.Kind.ACTUAL_IN);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitActualParameter(edu.kit.joana.api.sdg.SDGActualParameter, java.lang.Object)
	 */
	@Override
	protected Set<SDGNode> visitActualParameter(SDGActualParameter ap, AnnotationType type) {
		Set<SDGNode> ret = new HashSet<SDGNode>();
		addAllAppropriateParameterNodesFrom(pp2NodeTrans.getInRoots(ap), type, ret);
		addAllAppropriateParameterNodesFrom(pp2NodeTrans.getOutRoots(ap), type, ret);
		return ret;
	}

	private void addAllAppropriateParameterNodesFrom(Set<SDGNode> start, AnnotationType type, Set<SDGNode> base) {
		LinkedList<SDGNode> toDo = new LinkedList<SDGNode>();
		toDo.addAll(start);
		Set<SDGNode> visited = new HashSet<SDGNode>();
		// add all parameter nodes of right type and reachable by PS edges
		while (!toDo.isEmpty()) {
			SDGNode next = toDo.poll();
			visited.add(next);
			if (isParameterNodeOfKind(next, type)) {
				base.add(next);
			}
			for (SDGEdge e : sdg.getOutgoingEdgesOfKind(next, SDGEdge.Kind.PARAMETER_STRUCTURE)) {
				SDGNode succNode = e.getTarget();
				if (!visited.contains(succNode)) {
					toDo.add(succNode);
				}
			}
		}

	}

	private boolean isParameterNodeOfKind(SDGNode param, AnnotationType type) {
		return isActualNodeOfKind(param, type) || isFormalNodeOfKind(param, type);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitCall(edu.kit.joana.api.sdg.SDGCall, java.lang.Object)
	 */
	@Override
	protected Set<SDGNode> visitCall(SDGCall c, AnnotationType type) {
		Set<SDGNode> ret = new HashSet<SDGNode>();
		ret.addAll(pp2NodeTrans.getNodes(c));
		for (SDGActualParameter ap : c.getActualParameters()) {
			ret.addAll(visitActualParameter(ap, type));
		}
		if (c.getReturn() != null) {
			ret.addAll(visitCallReturnNode(c.getReturn(), type));
		}

		if (c.getExceptionNode() != null) {
			ret.addAll(visitCallExceptionNode(c.getExceptionNode(), type));
		}
		return ret;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitCallReturnNode(edu.kit.joana.api.sdg.SDGCallReturnNode, java.lang.Object)
	 */
	@Override
	protected Set<SDGNode> visitCallReturnNode(SDGCallReturnNode c, AnnotationType type) {
		Set<SDGNode> ret = new HashSet<SDGNode>();
		addAllAppropriateParameterNodesFrom(pp2NodeTrans.getNodes(c), type, ret);
		return ret;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitCallExceptionNode(edu.kit.joana.api.sdg.SDGCallExceptionNode, java.lang.Object)
	 */
	@Override
	protected Set<SDGNode> visitCallExceptionNode(SDGCallExceptionNode c, AnnotationType type) {
		Set<SDGNode> ret = new HashSet<SDGNode>();
		addAllAppropriateParameterNodesFrom(pp2NodeTrans.getNodes(c), type, ret);
		return ret;
	}
}
