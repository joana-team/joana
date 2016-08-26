/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import java.util.Comparator;

/**
 * @author Martin Mohr
 */
public class SDGPPConcretenessEvaluator extends SDGProgramPartVisitor<Integer, Void> {
	
	public static SDGPPConcretenessEvaluator INSTANCE = new SDGPPConcretenessEvaluator();
	public static ConcretenessComparator COMP = new ConcretenessComparator();
	
	private static class ConcretenessComparator implements Comparator<SDGProgramPart> {

		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(SDGProgramPart o1, SDGProgramPart o2) {
			return INSTANCE.evaluate(o1) - INSTANCE.evaluate(o2);
		}
		
	}
	
	public static ConcretenessComparator getComparator() {
		return COMP;
	}
	
	public static SDGProgramPart max(SDGProgramPart o1, SDGProgramPart o2) {
		int c = compare(o1, o2);
		if (c > 0) {
			return o1;
		} else if (c < 0) {
			return o2;
		} else {
			return null;
		}
	}

	public int evaluate(SDGProgramPart pp) {
		return pp.acceptVisitor(this, null);
	}

	public static int compare(SDGProgramPart o1, SDGProgramPart o2) {
		return COMP.compare(o1, o2);
	}

	@Override
	protected Integer visitClass(SDGClass cl, Void data) {
		return 0;
	}

	@Override
	protected Integer visitAttribute(SDGAttribute a, Void data) {
		return 1;
	}

	@Override
	protected Integer visitMethod(SDGMethod m, Void data) {
		return 1;
	}

	@Override
	protected Integer visitParameter(SDGFormalParameter p, Void data) {
		return 2;
	}

	@Override
	protected Integer visitExit(SDGMethodExitNode e, Void data) {
		return 3;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitException(edu.kit.joana.api.sdg.SDGMethodExceptionNode, java.lang.Object)
	 */
	@Override
	protected Integer visitException(SDGMethodExceptionNode e, Void data) {
		return 3;
	}

	@Override
	protected Integer visitPhi(SDGPhi phi, Void data) {
		return 3;
	}

	@Override
	protected Integer visitInstruction(SDGInstruction i, Void data) {
		return 4;
	}

	@Override
	protected Integer visitCall(SDGCall c, Void data) {
		return 4;
	}

	@Override
	protected Integer visitActualParameter(SDGActualParameter ap, Void data) {
		return 5;
	}

	@Override
	protected Integer visitCallReturnNode(SDGCallReturnNode c, Void data) {
		return 5;
	}

	@Override
	protected Integer visitCallExceptionNode(SDGCallExceptionNode c, Void data) {
		return 5;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPartVisitor#visitFieldOfParameter(edu.kit.joana.api.sdg.SDGFieldOfParameter, java.lang.Object)
	 */
	@Override
	protected Integer visitFieldOfParameter(SDGFieldOfParameter fop, Void data) {
		return 6;
	}
}
