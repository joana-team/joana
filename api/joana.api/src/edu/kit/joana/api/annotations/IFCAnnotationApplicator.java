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
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.api.annotations.IFCAnnotation.Type;
import edu.kit.joana.api.sdg.SDGAttribute;
import edu.kit.joana.api.sdg.SDGClass;
import edu.kit.joana.api.sdg.SDGInstruction;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGMethodExitNode;
import edu.kit.joana.api.sdg.SDGParameter;
import edu.kit.joana.api.sdg.SDGPhi;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.api.sdg.SDGProgramPartVisitor;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;

public class IFCAnnotationApplicator extends SDGProgramPartVisitor<Void, IFCAnnotation> {

	private static final Logger debug = Log.getLogger(Log.L_API_DEBUG);
	private static final Logger annotationDebug = Log.getLogger(Log.L_API_ANNOTATION_DEBUG);

	private final SDGProgram program;
	private final Map<SecurityNode, NodeAnnotationInfo> annotatedNodes = new HashMap<SecurityNode, NodeAnnotationInfo>();

	public IFCAnnotationApplicator(SDGProgram program) {
		this.program = program;
	}

	public void applyAnnotations(Collection<IFCAnnotation> anns) {
		for (IFCAnnotation ann : anns) {
			if (ann.getType() == Type.SOURCE || ann.getType() == Type.SINK) {
				annotationDebug.outln(String.format("Annnotation nodes for %s '%s' of security level %s...", ann.getType().toString(), ann.getProgramPart(), ann.getLevel1()));
			}
			ann.getProgramPart().acceptVisitor(this, ann);
		}
	}
	
	/**
	 * Given an annotated security node, retrieves the program part to which the security node belongs.
	 * @param sNode node to find out program part for
	 * @return the program part to which the security node belongs, or {@code null}, if no such program part could be found
	 */
	public SDGProgramPart resolve(SecurityNode sNode) {
		if (!annotatedNodes.containsKey(sNode)) {
			debug.outln("Tried to resolve node " + sNode + " and failed.");
			debug.outln("Resolvable nodes: " + annotatedNodes.keySet());
			if (sNode.isInformationSource()) {
				debug.outln(sNode + " was annotated as source.");
			} else if (sNode.isInformationSink()) {
				debug.outln(sNode + " was annotated as sink.");
			} else if (sNode.isDeclassification()) {
				debug.outln(sNode + " was annotated as declassification.");
			} else {
				debug.outln(sNode + " was not annotated.");
			}
			SDGProgramPart fallback = program.findCoveringProgramPart(sNode);
			if (fallback == null) {
				debug.outln("Also failed to find a covering program part.");
			}
			return fallback;
		} else {
			NodeAnnotationInfo nai = annotatedNodes.get(sNode);
			IFCAnnotation ann = nai.getAnnotation();
			SDGProgramPart ppart = ann.getProgramPart();
			return ppart;
		}
	}
	
	Collection<SDGNode> getSourceNodes() {
		return getNodes(NodeAnnotationInfo.PROV);
	}
	
	Collection<SDGNode> getSinkNodes() {
		return getNodes(NodeAnnotationInfo.REQ);
	}
	
	private Collection<SDGNode> getNodes(String which) {
		final Collection<SDGNode> ret = new LinkedList<SDGNode>();
		for (Map.Entry<SecurityNode, NodeAnnotationInfo> e : annotatedNodes.entrySet()) {
			SecurityNode sNode = e.getKey();
			NodeAnnotationInfo nai = e.getValue();
			if (nai.getWhich().equals(which)) {
				ret.add(sNode);
			}
		}
		return ret;
	}

	public void unapplyAnnotations(Collection<IFCAnnotation> anns) {
		List<SecurityNode> toDelete = new LinkedList<SecurityNode>();
		for (NodeAnnotationInfo nai : annotatedNodes.values()) {
			if (anns.contains(nai.getAnnotation())) {
				nai.getNode().setRequired(null);
				nai.getNode().setProvided(null);
				toDelete.add(nai.getNode());
			}
		}

		for (SecurityNode sn : toDelete) {
			annotatedNodes.remove(sn);
		}
	}

	private Collection<SDGNode> getCorrespondingActuals(SDGNode formal) {
		if (formal == null) {
			return Collections.emptyList();
		} else {
			SDG sdg = program.getSDG();
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

	@SuppressWarnings("unused")
	private Collection<SDGNode> getCorrespondingActIns(SDGParameter param) {
		return getCorrespondingActuals(param.getInRoot());
	}

	private Collection<SDGNode> getCorrespondingActOuts(SDGParameter param) {
		return getCorrespondingActuals(param.getOutRoot());
	}

	@Override
	protected Void visitParameter(SDGParameter param, IFCAnnotation ann) {
		assert param.getOutRoot() != null || param.getInRoot() != null;
		switch (ann.getType()) {
		case SOURCE:
			if (param.getInRoot() != null) {
				annotateNodes(param.getInRoot(), ann);

				/**
				 *  also mark all act_out nodes of this parameters as source, to capture
				 *  possible information flows from this parameter to the environment
				 *  just marking the formal-in would not suffice
				 *  TODO: think about this!
				 **/
				for (SDGNode actOut : getCorrespondingActOuts(param)) {
					annotateNodes(actOut, ann);
				}
			}
			break;
		case SINK:
			if (param.getInRoot() != null) {
				annotateNodes(param.getInRoot(), ann);
			}
			
			if (param.getOutRoot() != null) {
				annotateNodes(param.getOutRoot(), ann);
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
		return null;
	}

	@Override
	protected Void visitInstruction(SDGInstruction instr, IFCAnnotation ann) {
		annotateNodes(instr.getNode(), ann);
		return null;
	}

	@Override
	protected Void visitClass(SDGClass cl, IFCAnnotation ann) {
		for (SDGAttribute a : cl.getAttributes()) {
			visitAttribute(a, ann);
		}

		for (SDGMethod m : cl.getMethods()) {
			visitMethod(m, ann);
		}

		return null;
	}

	@Override
	protected Void visitAttribute(SDGAttribute a, IFCAnnotation ann) {
		Collection<SDGNode> toSelect;
		switch (ann.getType()) {
		case SOURCE:
			toSelect = a.getAttachedSourceNodes();
			break;
		case SINK:
			toSelect = a.getAttachedSinkNodes();
			break;
		default:
			throw new UnsupportedOperationException("not implemented yet!");
		}
		for (SDGNode n : toSelect) {
			annotateNodes(n, ann);
		}

		return null;
	}

	@Override
	protected Void visitMethod(SDGMethod method, IFCAnnotation ann) {
		annotateNodes(method.getEntry(), ann);
		return null;
	}

	@Override
	protected Void visitExit(SDGMethodExitNode exit, IFCAnnotation ann) {
		annotateNodes(exit.getExitNode(), ann);
		return null;
	}

	private void annotateNodes(SDGNode start, IFCAnnotation ann) {
		Set<SDGNode> nodes = new HashSet<SDGNode>();
		annotateNodes(start, ann, nodes);
	}

	private void annotateNodes(SDGNode start, IFCAnnotation ann, Set<SDGNode> visited) {
		if (visited.contains(start))
			return;
		else {
			annotateNode(start, ann);
			visited.add(start);

			switch (start.getKind()) {
			case FORMAL_IN:
			case FORMAL_OUT:
			case ACTUAL_IN:
			case ACTUAL_OUT:
			case EXIT:
				for (SDGEdge e : program.getSDG().getOutgoingEdgesOfKind(start, SDGEdge.Kind.PARAMETER_STRUCTURE)) {
					SDGNode attNode = e.getTarget();
					if (attNode != null && compatibleNodes(start, attNode))
						annotateNodes(attNode, ann, visited);
				}
				return;
			case CALL:
				for (SDGEdge e : program.getSDG().getOutgoingEdgesOfKind(start, SDGEdge.Kind.CONTROL_DEP_EXPR)) {
					SDGNode suc = e.getTarget();
					if (suc != null && isActualNodeOfKind(suc, ann.getType()))
						annotateNodes(suc, ann, visited);
				}
				return;
			case ENTRY:
				for (SDGEdge e : program.getSDG().getOutgoingEdgesOfKind(start, SDGEdge.Kind.CONTROL_DEP_EXPR)) {
					SDGNode suc = e.getTarget();
					if (suc != null && (isFormalNodeOfKind(suc, ann.getType())))
						annotateNodes(suc, ann, visited);
				}
				return;
			default:
				if (ann.getType() == Type.SINK) {

				}
			}
		}
	}

	private Collection<SDGMethod> obtainMethods(SDGNode node) {
		return program.getMethods(JavaMethodSignature.fromString(program.getSDG().getEntry(node).getBytecodeMethod()));
	}

	private void annotateNode(SDGNode node, IFCAnnotation ann) {
		if (ann.getContext() == null || obtainMethods(node).contains(ann.getContext())) {
			SecurityNode sNode = (SecurityNode) node;
			NodeAnnotationInfo nai;
			switch (ann.getType()) {
			case SOURCE:
				sNode.setProvided(ann.getLevel1());
				annotationDebug.outln(String.format("Annotated node %s as SOURCE of level '%s'", node.toString(), ann.getLevel1()));
				nai = new NodeAnnotationInfo(sNode, ann, NodeAnnotationInfo.PROV);
				break;
			case SINK:
				sNode.setRequired(ann.getLevel1());
				annotationDebug.outln(String.format("Annotated node %s as SINK of level '%s'", node.toString(), ann.getLevel1()));
				nai = new NodeAnnotationInfo(sNode, ann, NodeAnnotationInfo.REQ);
				break;
			case DECLASS:
				sNode.setRequired(ann.getLevel1());
				sNode.setProvided(ann.getLevel2());
				nai = new NodeAnnotationInfo(sNode, ann, NodeAnnotationInfo.BOTH);
				break;
			default:
				throw new IllegalStateException();
			}
			
			if (debug.isEnabled()) {
				debug.outln("Annotated node " + nai.getNode() + " as " + nai.getAnnotation().getLevel1() + " "
					+ nai.getAnnotation().getType());
			}
			annotatedNodes.put(sNode, nai);
		}
	}

	private static final boolean isActualNodeOfKind(SDGNode node, IFCAnnotation.Type type) {
		return ((type == IFCAnnotation.Type.SINK && node.getKind() == SDGNode.Kind.ACTUAL_IN) || (type == IFCAnnotation.Type.SOURCE && node
				.getKind() == SDGNode.Kind.ACTUAL_OUT));
	}

	private static final boolean isFormalNodeOfKind(SDGNode node, IFCAnnotation.Type type) {
		return (type == IFCAnnotation.Type.SINK && node.getKind() == SDGNode.Kind.FORMAL_IN)
				|| (type == IFCAnnotation.Type.SOURCE && (node.getKind() == SDGNode.Kind.EXIT || node.getKind() == SDGNode.Kind.FORMAL_OUT));
	}

	private static final boolean compatibleNodes(SDGNode node1, SDGNode node2) {
		return compatibleNodesOfDirection(node1, node2, IFCAnnotation.Type.SOURCE)
				|| compatibleNodesOfDirection(node1, node2, IFCAnnotation.Type.SINK);
	}

	private static final boolean compatibleNodesOfDirection(SDGNode node1, SDGNode node2, IFCAnnotation.Type dir) {
		if (dir == IFCAnnotation.Type.SINK)
			return (node1.getKind() == SDGNode.Kind.FORMAL_IN && node2.getKind() == SDGNode.Kind.FORMAL_IN)
					|| (node1.getKind() == SDGNode.Kind.ACTUAL_OUT && node2.getKind() == SDGNode.Kind.ACTUAL_OUT);
		else
			return ((node1.getKind() == SDGNode.Kind.FORMAL_OUT || node1.getKind() == SDGNode.Kind.EXIT) && (node2
					.getKind() == SDGNode.Kind.FORMAL_OUT || node2.getKind() == SDGNode.Kind.EXIT))
					|| (node1.getKind() == SDGNode.Kind.ACTUAL_IN && node2.getKind() == SDGNode.Kind.ACTUAL_IN);
	}

	@Override
	protected Void visitPhi(SDGPhi phi, IFCAnnotation ann) {
		throw new UnsupportedOperationException("not implemented yet!");
	}

}

class NodeAnnotationInfo {
	private final SecurityNode node;
	private final IFCAnnotation annotation;
	private final String which;

	public static final String PROV = "provided";
	public static final String REQ = "required";
	public static final String BOTH = "both";

	public NodeAnnotationInfo(SecurityNode node, IFCAnnotation annotation, String which) {
		this.node = node;
		this.annotation = annotation;
		this.which = which;
	}

	/**
	 * @return the node
	 */
	public SecurityNode getNode() {
		return node;
	}

	/**
	 * @return the annotation
	 */
	public IFCAnnotation getAnnotation() {
		return annotation;
	}

	/**
	 * @return the which
	 */
	public String getWhich() {
		return which;
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
		result = prime * result + ((annotation == null) ? 0 : annotation.hashCode());
		result = prime * result + ((node == null) ? 0 : node.hashCode());
		result = prime * result + ((which == null) ? 0 : which.hashCode());
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
		if (!(obj instanceof NodeAnnotationInfo)) {
			return false;
		}
		NodeAnnotationInfo other = (NodeAnnotationInfo) obj;
		if (annotation == null) {
			if (other.annotation != null) {
				return false;
			}
		} else if (!annotation.equals(other.annotation)) {
			return false;
		}
		if (node == null) {
			if (other.node != null) {
				return false;
			}
		} else if (!node.equals(other.node)) {
			return false;
		}
		if (which == null) {
			if (other.which != null) {
				return false;
			}
		} else if (!which.equals(other.which)) {
			return false;
		}
		return true;
	}
}
