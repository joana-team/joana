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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.api.sdg.SDGActualParameter;
import edu.kit.joana.api.sdg.SDGAttribute;
import edu.kit.joana.api.sdg.SDGCall;
import edu.kit.joana.api.sdg.SDGClass;
import edu.kit.joana.api.sdg.SDGFormalParameter;
import edu.kit.joana.api.sdg.SDGInstruction;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGMethodExitNode;
import edu.kit.joana.api.sdg.SDGPhi;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.api.sdg.SDGProgramPartVisitor;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * @author Martin Mohr
 */
public class AnnotationTypeBasedNodeCollector extends SDGProgramPartVisitor<Set<SDGNode>, AnnotationType> {
	
	private SDG sdg;
	
	public AnnotationTypeBasedNodeCollector(SDG sdg) {
		this.sdg = sdg;
	}
	
	public Set<SDGNode> collectNodes(SDGProgramPart ppart, AnnotationType type) {
		return ppart.acceptVisitor(this, type);
	}

	@Override
	protected Set<SDGNode> visitParameter(SDGFormalParameter param, AnnotationType type) {
		assert param.getOutRoot() != null || param.getInRoot() != null;
		Set<SDGNode> ret = new HashSet<SDGNode>();
		addAllAppropriateParameterNodesFrom(param.getInRoot(), type, ret);
		if (param.getInRoot() != null && type == AnnotationType.SINK) {
			for (SDGNode actOut : getCorrespondingActOuts(param)) {
				/**
				 *  also mark all act_out nodes of this parameters as source, to capture
				 *  possible information flows from this parameter to the environment
				 *  just marking the formal-in would not suffice
				 *  TODO: think about this!
				 **/
				addAllAppropriateParameterNodesFrom(actOut, type, ret);
			}
		}
		
		addAllAppropriateParameterNodesFrom(param.getOutRoot(), type, ret);
		return ret;
	}
	
	private Collection<SDGNode> getCorrespondingActOuts(SDGFormalParameter param) {
		return getCorrespondingActuals(param.getOutRoot());
	}
	
	private Collection<SDGNode> getCorrespondingActuals(SDGNode formal) {
		if (formal == null) {
			return Collections.emptyList();
		} else {
			switch (formal.getKind()) {
			case FORMAL_IN:
				return sdg.getActualIns(formal);
			case FORMAL_OUT:
				return sdg.getActualOuts(formal);
			default:
				throw new IllegalStateException();
			}
		}
	}


	@Override
	protected Set<SDGNode> visitInstruction(SDGInstruction instr, AnnotationType type) {
		if (instr instanceof SDGCall) {
			throw new IllegalArgumentException(instr.toString());
		}
		return Collections.singleton(instr.getNode());
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
			toSelect = a.getAttachedSourceNodes();
			break;
		case SINK:
			toSelect = a.getAttachedSinkNodes();
			break;
		default:
			throw new UnsupportedOperationException("not implemented yet!");
		}
		return new HashSet<SDGNode>(toSelect);
	}

	@Override
	protected Set<SDGNode> visitMethod(SDGMethod method, AnnotationType type) {
		Set<SDGNode> ret = new HashSet<SDGNode>();
		ret.add(method.getEntry());
		for (SDGFormalParameter fp : method.getParameters()) {
			ret.addAll(visitParameter(fp, type));
		}
		ret.addAll(visitExit(method.getExit(), type));
		return ret;
	}

	@Override
	protected Set<SDGNode> visitExit(SDGMethodExitNode exit, AnnotationType type) {
		Set<SDGNode> ret = new HashSet<SDGNode>();
		addAllAppropriateParameterNodesFrom(exit.getExitNode(), type, ret);
		return ret;
	}
 
	@Override
	protected Set<SDGNode> visitPhi(SDGPhi phi, AnnotationType type) {
		throw new UnsupportedOperationException("not implemented yet!");
	}
	
	private static final boolean isActualNodeOfKind(SDGNode node, AnnotationType type) {
		return ((type == AnnotationType.SINK && node.getKind() == SDGNode.Kind.ACTUAL_IN) || (type == AnnotationType.SOURCE && node
				.getKind() == SDGNode.Kind.ACTUAL_OUT));
	}

	private static final boolean isFormalNodeOfKind(SDGNode node, AnnotationType type) {
		return (type == AnnotationType.SINK && node.getKind() == SDGNode.Kind.FORMAL_IN)
				|| (type == AnnotationType.SOURCE && (node.getKind() == SDGNode.Kind.EXIT || node.getKind() == SDGNode.Kind.FORMAL_OUT));
	}

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
		addAllAppropriateParameterNodesFrom(ap.getInRoot(), type, ret);
		addAllAppropriateParameterNodesFrom(ap.getOutRoot(), type, ret);
		return ret;
	}
	
	private void addAllAppropriateParameterNodesFrom(SDGNode start, AnnotationType type, Set<SDGNode> base) {
		if (start != null && isParameterNodeOfKind(start, type)) {
			LinkedList<SDGNode> toDo = new LinkedList<SDGNode>();
			toDo.add(start);
			
			// add all parameter nodes of right type and reachable by PS edges
			while (!toDo.isEmpty()) {
				SDGNode next = toDo.poll();
				base.add(next);
				for (SDGEdge e : sdg.getOutgoingEdgesOfKind(next, SDGEdge.Kind.PARAMETER_STRUCTURE)) {
					SDGNode succNode = e.getTarget();
					if (compatibleNodes(next, succNode)) {
						toDo.add(succNode);
					}
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
		ret.add(c.getNode());
		for (SDGActualParameter ap : c.getActualParameters()) {
			ret.addAll(visitActualParameter(ap, type));
		}
		addAllAppropriateParameterNodesFrom(c.getReturn(), type, ret);
		addAllAppropriateParameterNodesFrom(c.getExceptionNode(), type, ret);
		return ret;
	}
}
