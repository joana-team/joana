/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

/**
 * TODO: @hecker
 */
public abstract class ThrowingSDGProgramPartVisitor<R, D>  extends SDGProgramPartVisitor<R, D> {

	private static <T> void fail(Class<T> c) {
		throw new IllegalArgumentException("Unhandled ProgramPart: " + c);
	}
	protected  R visitClass(SDGClass cl, D data) { fail(SDGClass.class); return null;}

	protected  R visitAttribute(SDGAttribute a, D data)  { fail(SDGAttribute.class); return null;}

	protected  R visitMethod(SDGMethod m, D data){ fail(SDGMethod.class); return null;}
	
	protected  R visitActualParameter(SDGActualParameter ap, D data){ fail(SDGActualParameter.class); return null;}

	protected  R visitParameter(SDGFormalParameter p, D data){ fail(SDGFormalParameter.class); return null;}

	protected  R visitExit(SDGMethodExitNode e, D data){ fail(SDGMethodExitNode.class); return null;}

	protected  R visitException(SDGMethodExceptionNode e, D data){ fail(SDGMethodExceptionNode.class); return null;}

	protected  R visitInstruction(SDGInstruction i, D data){ fail(SDGInstruction.class); return null;}

	protected  R visitCall(SDGCall c, D data){ fail(SDGCall.class); return null;}
	
	protected  R visitCallReturnNode(SDGCallReturnNode c, D data){ fail(SDGCallReturnNode.class); return null;}
	
	protected  R visitCallExceptionNode(SDGCallExceptionNode c, D data){ fail(SDGCallExceptionNode.class); return null;}

	protected  R visitPhi(SDGPhi phi, D data){ fail(SDGPhi.class); return null;}

	@Override
	protected R visitFieldOfParameter(SDGFieldOfParameter fop, D data) { fail(SDGFieldOfParameter.class); return null; }
}
