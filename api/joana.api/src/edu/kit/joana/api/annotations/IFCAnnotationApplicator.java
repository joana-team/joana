/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.annotations;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;

import java.util.*;

public class IFCAnnotationApplicator {

	private static final Logger debug = Log.getLogger(Log.L_API_DEBUG);
	private static final Logger annotationDebug = Log.getLogger(Log.L_API_ANNOTATION_DEBUG);

	private final SDGProgram program;
	private final IFCAnalysis analysis;
	private final Map<SecurityNode, Set<NodeAnnotationInfo>> annotatedNodes = new HashMap<SecurityNode, Set<NodeAnnotationInfo>>();

	public IFCAnnotationApplicator(SDGProgram program, IFCAnalysis analysis) {
		this.program = program;
		this.analysis = analysis;
	}

	public void applyAnnotations(Collection<IFCAnnotation> anns) {
		AnnotationTypeBasedNodeCollector collector = program.getNodeCollector();
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

	public Map<SecurityNode, Set<NodeAnnotationInfo>> getAnnotatedNodes() {
		return new HashMap<SecurityNode, Set<NodeAnnotationInfo>>(annotatedNodes);
	}

	Collection<SDGNode> getSourceNodes() {
		return getNodes(NodeAnnotationInfo.PROV);
	}

	Collection<SDGNode> getSinkNodes() {
		return getNodes(NodeAnnotationInfo.REQ);
	}

	private Collection<SDGNode> getNodes(String which) {
		final Collection<SDGNode> ret = new LinkedList<SDGNode>();
		for (Map.Entry<SecurityNode, Set<NodeAnnotationInfo>> e : annotatedNodes.entrySet()) {
			SecurityNode sNode = e.getKey();
			for (NodeAnnotationInfo nai : e.getValue()) {
				if (nai.getWhich().equals(which)) {
					ret.add(sNode);
				}
			}
		}
		return ret;
	}

	public void unapplyAnnotations(Collection<IFCAnnotation> anns) {
		List<SecurityNode> toDelete = new LinkedList<SecurityNode>();
		for (Set<NodeAnnotationInfo> nais : annotatedNodes.values()) {
			for (NodeAnnotationInfo nai : nais) {
				if (anns.contains(nai.getAnnotation())) {
					nai.getNode().setRequired(null);
					nai.getNode().setProvided(null);
					toDelete.add(nai.getNode());
				}
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
			case SOURCE: {
				String newLevel;
				if (sNode.getProvided() != null) {
					newLevel = analysis.getLattice().leastUpperBound(ann.getLevel1(), sNode.getProvided());
				} else {
					newLevel = ann.getLevel1();
				}
				if (sNode.getRequired() != null) throw new IllegalStateException(String.format("Error while annotating node %s: Cannot set required level if provided level is already set. Use a declassification!", sNode));
				sNode.setProvided(newLevel);
				annotationDebug.outln(String.format("Annotated node %s of kind %s as SOURCE of level '%s'", node.toString(), node.getKind(), newLevel));
				nai = new NodeAnnotationInfo(sNode, ann, NodeAnnotationInfo.PROV);
				break;
			}
			case SINK: {
				String newLevel;
				if (sNode.getRequired() != null) {
					newLevel = analysis.getLattice().greatestLowerBound(ann.getLevel1(), sNode.getRequired());
				} else {
					newLevel = ann.getLevel1();
				}
				if (sNode.getProvided() != null) throw new IllegalStateException(String.format("Error while annotating node %s with %s: Cannot set provided level if required level is already set. Use a declassification!", sNode, ann));
				sNode.setRequired(newLevel);
				annotationDebug.outln(String.format("Annotated node %s of kind %s as SINK of level '%s'", node.toString(), node.getKind(), newLevel));
				nai = new NodeAnnotationInfo(sNode, ann, NodeAnnotationInfo.REQ);
				break;
			}
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
			Set<NodeAnnotationInfo> nais = annotatedNodes.get(sNode);
			if (nais == null) {
				nais = new LinkedHashSet<NodeAnnotationInfo>();
				annotatedNodes.put(sNode, nais);
			}
			nais.add(nai);
		}
	}
}
