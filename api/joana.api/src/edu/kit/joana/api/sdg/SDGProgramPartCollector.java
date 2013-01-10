/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import java.util.Collection;

/**
 * TODO: @author Add your name here.
 */
public class SDGProgramPartCollector extends
		SDGProgramPartVisitor<Void, Collection<SDGProgramPart>> {

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitClass(edu.kit.joana.api.sdg.SDGClass, java.lang.Object)
	 */
	@Override
	protected Void visitClass(SDGClass cl, Collection<SDGProgramPart> base) {
		for (SDGAttribute a : cl.getAttributes()) {
			base.add(a);
			a.acceptVisitor(this, base);
		}
		
		for (SDGMethod m : cl.getMethods()) {
			base.add(m);
			m.acceptVisitor(this, base);
		}
		
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitAttribute(edu.kit.joana.api.sdg.SDGAttribute, java.lang.Object)
	 */
	@Override
	protected Void visitAttribute(SDGAttribute a, Collection<SDGProgramPart> base) {
		base.add(a);
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitMethod(edu.kit.joana.api.sdg.SDGMethod, java.lang.Object)
	 */
	@Override
	protected Void visitMethod(SDGMethod m, Collection<SDGProgramPart> base) {
		base.add(m);
		m.getExit().acceptVisitor(this, base);
		for (SDGParameter p : m.getParameters()) {
			p.acceptVisitor(this, base);
		}
		
		for (SDGInstruction i : m.getInstructions()) {
			i.acceptVisitor(this, base);
		}
		
		for (SDGPhi phi : m.getPhis()) {
			phi.acceptVisitor(this, base);
		}
		
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitParameter(edu.kit.joana.api.sdg.SDGParameter, java.lang.Object)
	 */
	@Override
	protected Void visitParameter(SDGParameter p, Collection<SDGProgramPart> base) {
		base.add(p);
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitExit(edu.kit.joana.api.sdg.SDGMethodExitNode, java.lang.Object)
	 */
	@Override
	protected Void visitExit(SDGMethodExitNode e, Collection<SDGProgramPart> base) {
		base.add(e);
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitInstruction(edu.kit.joana.api.sdg.SDGInstruction, java.lang.Object)
	 */
	@Override
	protected Void visitInstruction(SDGInstruction i, Collection<SDGProgramPart> base) {
		base.add(i);
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitPhi(edu.kit.joana.api.sdg.SDGPhi, java.lang.Object)
	 */
	@Override
	protected Void visitPhi(SDGPhi phi, Collection<SDGProgramPart> base) {
		base.add(phi);
		return null;
	}

}
