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
import edu.kit.joana.ifc.sdg.graph.SDGNode;

public class IFCAnnotationManager {

	private final IFCAnnotationApplicator app;
	private final Map<SDGProgramPart, IFCAnnotation> annotations;


	public IFCAnnotationManager(SDGProgram program) {
		this.annotations = new HashMap<SDGProgramPart, IFCAnnotation>();
		this.app = new IFCAnnotationApplicator(program);
	}

	public boolean isAnnotationLegal(IFCAnnotation ann) {
		return new AnnotationVerifier().verify(ann);
	}

	public void addAnnotation(IFCAnnotation ann) {
		if (!isAnnotationLegal(ann)) {
			throw new IllegalArgumentException();
		}
		annotations.put(ann.getProgramPart(), ann);
	}

	public void removeAnnotation(SDGProgramPart programPart) {
		annotations.remove(programPart);
	}

	public void removeAllAnnotations() {
		annotations.clear();
	}

	public void addSourceAnnotation(SDGProgramPart progPart, String level, SDGMethod context) {
		addAnnotation(new IFCAnnotation(Type.SOURCE, level, progPart, context));
	}

	public void addSinkAnnotation(SDGProgramPart progPart, String level, SDGMethod context) {
		addAnnotation(new IFCAnnotation(Type.SINK, level, progPart, context));
	}

	public void addDeclassification(SDGProgramPart progPart, String level1, String level2) {
		addAnnotation(new IFCAnnotation(level1, level2, progPart));
	}

	public Collection<IFCAnnotation> getAnnotations() {
		return new LinkedList<IFCAnnotation>(annotations.values());
	}

	public boolean isAnnotated(SDGProgramPart part) {
		return annotations.keySet().contains(part);
	}

	public Collection<IFCAnnotation> getSources() {
		return getAnnotationsOfType(Type.SOURCE);
	}

	public Collection<IFCAnnotation> getSinks() {
		return getAnnotationsOfType(Type.SINK);
	}

	public Collection<IFCAnnotation> getDeclassifications() {
		return getAnnotationsOfType(Type.DECLASS);
	}

	private Collection<IFCAnnotation> getAnnotationsOfType(Type type) {
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
		return data.getType() != Type.DECLASS;
	}

	@Override
	protected Boolean visitAttribute(SDGAttribute a, IFCAnnotation data) {
		return data.getType() != Type.DECLASS;
	}

	@Override
	protected Boolean visitMethod(SDGMethod m, IFCAnnotation data) {
		return true;
	}

	@Override
	protected Boolean visitParameter(SDGParameter p, IFCAnnotation data) {
		return data.getType() != Type.DECLASS;
	}

	@Override
	protected Boolean visitExit(SDGMethodExitNode e, IFCAnnotation data) {
		return data.getType() != Type.DECLASS;
	}

	@Override
	protected Boolean visitInstruction(SDGInstruction i, IFCAnnotation data) {
		return true;
	}

	@Override
	protected Boolean visitPhi(SDGPhi phi, IFCAnnotation data) {
		return true;
	}

}
