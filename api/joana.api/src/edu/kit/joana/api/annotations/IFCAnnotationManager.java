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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.sdg.SDGActualParameter;
import edu.kit.joana.api.sdg.SDGAttribute;
import edu.kit.joana.api.sdg.SDGCall;
import edu.kit.joana.api.sdg.SDGCallExceptionNode;
import edu.kit.joana.api.sdg.SDGCallReturnNode;
import edu.kit.joana.api.sdg.SDGClass;
import edu.kit.joana.api.sdg.SDGFieldOfParameter;
import edu.kit.joana.api.sdg.SDGFormalParameter;
import edu.kit.joana.api.sdg.SDGInstruction;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGMethodExceptionNode;
import edu.kit.joana.api.sdg.SDGMethodExitNode;
import edu.kit.joana.api.sdg.SDGPhi;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.api.sdg.SDGProgramPartVisitor;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

public class IFCAnnotationManager {

	private final IFCAnnotationApplicator app;
	private final Map<SDGProgramPart, IFCAnnotation> sourceAnnotations;
	private final Map<SDGProgramPart, IFCAnnotation> sinkAnnotations;
	private final Map<SDGProgramPart, IFCAnnotation> declassAnnotations;

	public IFCAnnotationManager(SDGProgram program, IFCAnalysis analysis) {
		this.sourceAnnotations = new HashMap<SDGProgramPart, IFCAnnotation>();
		this.sinkAnnotations = new HashMap<SDGProgramPart, IFCAnnotation>();
		this.declassAnnotations = new HashMap<SDGProgramPart, IFCAnnotation>();
		this.app = new IFCAnnotationApplicator(program, analysis);
	}

	public boolean isAnnotationLegal(IFCAnnotation ann) {
		return new AnnotationVerifier().verify(ann);
	}

	public void addAnnotation(IFCAnnotation ann) {
		if (!isAnnotationLegal(ann)) {
			throw new IllegalArgumentException();
		}
		if (ann.getType() == AnnotationType.SOURCE) {
			sourceAnnotations.put(ann.getProgramPart(), ann);
		} else if (ann.getType() == AnnotationType.SINK) {
			sinkAnnotations.put(ann.getProgramPart(), ann);
		} else {
			declassAnnotations.put(ann.getProgramPart(), ann);
		}
	}

	public void removeAnnotation(SDGProgramPart programPart) {
		sourceAnnotations.remove(programPart);
		sinkAnnotations.remove(programPart);
		declassAnnotations.remove(programPart);
	}

	public void removeAllAnnotations() {
		sourceAnnotations.clear();
		sinkAnnotations.clear();
		declassAnnotations.clear();
	}

	public void addSourceAnnotation(SDGProgramPart progPart, String level, SDGMethod context) {
		addAnnotation(new IFCAnnotation(AnnotationType.SOURCE, level, progPart, context));
	}

	public void addSinkAnnotation(SDGProgramPart progPart, String level, SDGMethod context) {
		addAnnotation(new IFCAnnotation(AnnotationType.SINK, level, progPart, context));
	}

	public void addDeclassification(SDGProgramPart progPart, String level1, String level2) {
		addAnnotation(new IFCAnnotation(level1, level2, progPart));
	}

	public Collection<IFCAnnotation> getAnnotations() {
		LinkedList<IFCAnnotation> ret = new LinkedList<IFCAnnotation>();
		ret.addAll(sourceAnnotations.values());
		ret.addAll(sinkAnnotations.values());
		ret.addAll(declassAnnotations.values());
		return ret;
	}

	public Map<SecurityNode, NodeAnnotationInfo> getAnnotatedNodes() {
		return app.getAnnotatedNodes();
	}

	public boolean isAnnotated(SDGProgramPart part) {
		return sourceAnnotations.keySet().contains(part) || sinkAnnotations.keySet().contains(part) || declassAnnotations.keySet().contains(part);
	}

	public Collection<IFCAnnotation> getSources() {
		return getAnnotationsOfType(AnnotationType.SOURCE);
	}

	public Collection<IFCAnnotation> getSinks() {
		return getAnnotationsOfType(AnnotationType.SINK);
	}

	public Collection<IFCAnnotation> getDeclassifications() {
		return getAnnotationsOfType(AnnotationType.DECLASS);
	}

	private Collection<IFCAnnotation> getAnnotationsOfType(AnnotationType type) {
		HashSet<IFCAnnotation> ret = new HashSet<IFCAnnotation>();
		for (IFCAnnotation ann : getAnnotations()) {
			if (ann.getType() == type)
				ret.add(ann);
		}
		return ret;
	}

	public void applyAllAnnotations() {
		app.applyAnnotations(getAnnotations());
	}

	public SDGProgramPart resolve(SecurityNode sNode) {
		return app.resolve(sNode);
	}

	public Collection<SDGNode> getSourceNodes() {
		return app.getSourceNodes();
	}

	public Collection<SDGNode> getSinkNodes() {
		return app.getSinkNodes();
	}

	public void unapplyAllAnnotations() {
		app.unapplyAnnotations(getAnnotations());
	}

}

class AnnotationVerifier extends SDGProgramPartVisitor<Boolean, IFCAnnotation> {

	public boolean verify(IFCAnnotation ann) {
		return ann.getProgramPart().acceptVisitor(this, ann);
	}


	@Override
	protected Boolean visitClass(SDGClass cl, IFCAnnotation data) {
		return data.getType() != AnnotationType.DECLASS;
	}

	@Override
	protected Boolean visitAttribute(SDGAttribute a, IFCAnnotation data) {
		return data.getType() != AnnotationType.DECLASS;
	}

	@Override
	protected Boolean visitMethod(SDGMethod m, IFCAnnotation data) {
		return true;
	}

	@Override
	protected Boolean visitParameter(SDGFormalParameter p, IFCAnnotation data) {
		return data.getType() != AnnotationType.DECLASS;
	}

	@Override
	protected Boolean visitExit(SDGMethodExitNode e, IFCAnnotation data) {
		return data.getType() != AnnotationType.DECLASS;
	}

	@Override
	protected Boolean visitInstruction(SDGInstruction i, IFCAnnotation data) {
		return true;
	}

	@Override
	protected Boolean visitPhi(SDGPhi phi, IFCAnnotation data) {
		return true;
	}


	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitActualParameter(edu.kit.joana.api.sdg.SDGActualParameter, java.lang.Object)
	 */
	@Override
	protected Boolean visitActualParameter(SDGActualParameter ap, IFCAnnotation data) {
		return true;
	}


	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitCall(edu.kit.joana.api.sdg.SDGCall, java.lang.Object)
	 */
	@Override
	protected Boolean visitCall(SDGCall c, IFCAnnotation data) {
		return visitInstruction(c, data);
	}


	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitCallReturnNode(edu.kit.joana.api.sdg.SDGCallReturnNode, java.lang.Object)
	 */
	@Override
	protected Boolean visitCallReturnNode(SDGCallReturnNode c, IFCAnnotation data) {
		return true;
	}


	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitCallExceptionNode(edu.kit.joana.api.sdg.SDGCallExceptionNode, java.lang.Object)
	 */
	@Override
	protected Boolean visitCallExceptionNode(SDGCallExceptionNode c, IFCAnnotation data) {
		return true;
	}


	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitException(edu.kit.joana.api.sdg.SDGMethodExceptionNode, java.lang.Object)
	 */
	@Override
	protected Boolean visitException(SDGMethodExceptionNode e, IFCAnnotation data) {
		return data.getType() != AnnotationType.DECLASS;
	}


	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitFieldOfParameter(edu.kit.joana.api.sdg.SDGFieldOfParameter, java.lang.Object)
	 */
	@Override
	protected Boolean visitFieldOfParameter(SDGFieldOfParameter fop, IFCAnnotation data) {
		return data.getType() != AnnotationType.DECLASS;
	}

}
