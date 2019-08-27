/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 * <p>
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

public class SDGProgramPartVisitor2<R, D> extends SDGProgramPartVisitor<R, D> {

  protected R visitClass(SDGClass cl, D data) {
    return null;
  }

  protected R visitAttribute(SDGAttribute a, D data) {
    return null;
  }

  protected R visitMethod(SDGMethod m, D data) {
    return null;
  }

  protected R visitActualParameter(SDGActualParameter ap, D data) {
    return null;
  }

  protected R visitParameter(SDGFormalParameter p, D data) {
    return null;
  }

  protected R visitExit(SDGMethodExitNode e, D data) {
    return null;
  }

  protected R visitException(SDGMethodExceptionNode e, D data) {
    return null;
  }

  protected R visitInstruction(SDGInstruction i, D data) {
    return null;
  }

  protected R visitCall(SDGCall c, D data) {
    return null;
  }

  protected R visitCallReturnNode(SDGCallReturnNode c, D data) {
    return null;
  }

  protected R visitCallExceptionNode(SDGCallExceptionNode c, D data) {
    return null;
  }

  protected R visitPhi(SDGPhi phi, D data) {
    return null;
  }

  protected R visitFieldOfParameter(SDGFieldOfParameter fop, D data) {
    return null;
  }

  protected R visitLocalVariable(SDGLocalVariable local, D data) {
    return null;
  }
}
