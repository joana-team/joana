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
import java.util.Set;

import edu.kit.joana.api.sdg.SDGAttribute;
import edu.kit.joana.api.sdg.SDGClass;
import edu.kit.joana.api.sdg.SDGInstruction;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGMethodExitNode;
import edu.kit.joana.api.sdg.SDGFormalParameter;
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
		switch (type) {
		case SOURCE:
			if (param.getInRoot() != null) {
				ret.addAll(annotateNodes(param.getInRoot(), type));

				/**
				 *  also mark all act_out nodes of this parameters as source, to capture
				 *  possible information flows from this parameter to the environment
				 *  just marking the formal-in would not suffice
				 *  TODO: think about this!
				 **/
				for (SDGNode actOut : getCorrespondingActOuts(param)) {
					ret.addAll(annotateNodes(actOut, type));
				}
			}
			break;
		case SINK:
			if (param.getInRoot() != null) {
				ret.addAll(annotateNodes(param.getInRoot(), type));
			}
			
			if (param.getOutRoot() != null) {
				ret.addAll(annotateNodes(param.getOutRoot(), type));
			} // also annotate formal-out parameter to also capture possible side effects
			
//			if (param.getOutRoot() != null) {
//				annotateNodes(param.getOutRoot(), ann);
//				for (SDGNode actIn : getCorrespondingActIns(param)) {
//					annotateNodes(actIn, ann);
//				}
//			}
			break;
		default:
			break;
		}
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
		return annotateNodes(instr.getNode(), type);
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
		Set<SDGNode> ret = new HashSet<SDGNode>();
		for (SDGNode n : toSelect) {
			ret.addAll(annotateNodes(n, type));
		}

		return ret;
	}

	@Override
	protected Set<SDGNode> visitMethod(SDGMethod method, AnnotationType type) {
		return annotateNodes(method.getEntry(), type);
	}

	@Override
	protected Set<SDGNode> visitExit(SDGMethodExitNode exit, AnnotationType type) {
		return annotateNodes(exit.getExitNode(), type);
	}

	private Set<SDGNode> annotateNodes(SDGNode start, AnnotationType type) {
		Set<SDGNode> nodes = new HashSet<SDGNode>();
		addNodes(start, type, nodes);
		return nodes;
	}

	private void addNodes(SDGNode start, AnnotationType type, Set<SDGNode> visited) {
		if (visited.contains(start))
			return;
		else {
			//annotateNode(start, ann);
			visited.add(start);

			switch (start.getKind()) {
			case FORMAL_IN:
			case FORMAL_OUT:
			case ACTUAL_IN:
			case ACTUAL_OUT:
			case EXIT:
				for (SDGEdge e : sdg.getOutgoingEdgesOfKind(start, SDGEdge.Kind.PARAMETER_STRUCTURE)) {
					SDGNode attNode = e.getTarget();
					if (attNode != null && compatibleNodes(start, attNode))
						addNodes(attNode, type, visited);
				}
				return;
			case CALL:
				for (SDGEdge e : sdg.getOutgoingEdgesOfKind(start, SDGEdge.Kind.CONTROL_DEP_EXPR)) {
					SDGNode suc = e.getTarget();
					if (suc != null && isActualNodeOfKind(suc, type))
						addNodes(suc, type, visited);
				}
				return;
			case ENTRY:
				for (SDGEdge e : sdg.getOutgoingEdgesOfKind(start, SDGEdge.Kind.CONTROL_DEP_EXPR)) {
					SDGNode suc = e.getTarget();
					if (suc != null && (isFormalNodeOfKind(suc, type)))
						addNodes(suc, type, visited);
				}
				return;
			default:
				if (type == AnnotationType.SINK) {

				}
			}
		}
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
}
