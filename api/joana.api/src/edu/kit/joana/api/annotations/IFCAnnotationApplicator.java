/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.annotations;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;

public class IFCAnnotationApplicator {

	private static final Logger debug = Log.getLogger(Log.L_API_DEBUG);
	private static final Logger annotationDebug = Log.getLogger(Log.L_API_ANNOTATION_DEBUG);

	private final SDGProgram program;
	private final Map<SecurityNode, NodeAnnotationInfo> annotatedNodes = new HashMap<SecurityNode, NodeAnnotationInfo>();

	public IFCAnnotationApplicator(SDGProgram program) {
		this.program = program;
	}

	public void applyAnnotations(Collection<IFCAnnotation> anns) {
		AnnotationTypeBasedNodeCollector collector = new AnnotationTypeBasedNodeCollector(program.getSDG());
		for (IFCAnnotation ann : anns) {
			if (ann.getType() == AnnotationType.SOURCE || ann.getType() == AnnotationType.SINK) {
				annotationDebug.outln(String.format("Annnotation nodes for %s '%s' of security level %s...", ann.getType().toString(), ann.getProgramPart(), ann.getLevel1()));
			}
//			coll.setNodeFilter(ann.getType().getNodeFilter());
//			Collection<SDGNode> toAnnotate = coll.collectNodes(ann.getProgramPart());
//			for (SDGNode n : toAnnotate) {
//				annotateNode(n, ann);
//			}
			Set<SDGNode> toAnnotate = collector.collectNodes(ann.getProgramPart(), ann.getType());
			for (SDGNode n : toAnnotate) {
				annotateNode(n, ann);
			}
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
				annotationDebug.outln(String.format("Annotated node %s of kind %s as SOURCE of level '%s'", node.toString(), node.getKind(), ann.getLevel1()));
				nai = new NodeAnnotationInfo(sNode, ann, NodeAnnotationInfo.PROV);
				break;
			case SINK:
				sNode.setRequired(ann.getLevel1());
				annotationDebug.outln(String.format("Annotated node %s of kind %s as SINK of level '%s'", node.toString(), node.getKind(), ann.getLevel1()));
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
