/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 * <p>
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

public abstract class SDGProgramPartVisitorWithDefault<R, D> extends SDGProgramPartVisitor<R, D> {

  protected abstract R visitProgramPart(SDGProgramPart programPart, D data);

  @Override protected R visitClass(SDGClass cl, D data) {
    return visitProgramPart(cl, data);
  }

  protected R visitAttribute(SDGAttribute a, D data) {
    return visitProgramPart(a, data);
  }

  protected R visitMethod(SDGMethod m, D data) {
    return visitProgramPart(m, data);
  }

  protected R visitActualParameter(SDGActualParameter ap, D data) {
    return visitProgramPart(ap, data);
  }

  protected R visitParameter(SDGFormalParameter p, D data) {
    return visitProgramPart(p, data);
  }

  protected R visitExit(SDGMethodExitNode e, D data) {
    return visitProgramPart(e, data);
  }

  protected R visitException(SDGMethodExceptionNode e, D data) {
    return visitProgramPart(e, data);
  }

  protected R visitInstruction(SDGInstruction i, D data) {
    return visitProgramPart(i, data);
  }

  protected R visitCall(SDGCall c, D data) {
    return visitProgramPart(c, data);
  }

  protected R visitCallReturnNode(SDGCallReturnNode c, D data) {
    return visitProgramPart(c, data);
  }

  protected R visitCallExceptionNode(SDGCallExceptionNode c, D data) {
    return visitProgramPart(c, data);
  }

  protected R visitPhi(SDGPhi phi, D data) {
    return visitProgramPart(phi, data);
  }

  protected R visitFieldOfParameter(SDGFieldOfParameter fop, D data) {
    return visitProgramPart(fop, data);
  }

  protected R visitLocalVariable(SDGLocalVariable local, D data) {
    return visitProgramPart(local, data);
  }
}
