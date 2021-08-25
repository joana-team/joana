package edu.kit.joana.api.sdg.opt.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.TypePath;

/**
 * Can be used to delete all instructions in a method (keeps parameters, annotations, …)
 */
public class DeletingMethodVisitor extends org.objectweb.asm.MethodVisitor {

  private final boolean deleteInstructions;

  public DeletingMethodVisitor(int api, boolean deleteInstructions) {
    super(api);
    this.deleteInstructions = deleteInstructions;
  }

  public DeletingMethodVisitor(int api, org.objectweb.asm.MethodVisitor methodVisitor, boolean deleteInstructions) {
    super(api, methodVisitor);
    this.deleteInstructions = deleteInstructions;
  }

  @Override public void visitCode() {
    super.visitCode();
  }

  @Override public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
    super.visitFrame(type, numLocal, local, numStack, stack);
  }

  @Override public void visitInsn(int opcode) {
    if (!deleteInstructions) {
      mv.visitInsn(opcode);
    }
  }

  @Override public void visitIntInsn(int opcode, int operand) {
    if (!deleteInstructions) {
      mv.visitIntInsn(opcode, operand);
    }
  }

  @Override public void visitVarInsn(int opcode, int var) {
    if (!deleteInstructions) {
      super.visitVarInsn(opcode, var);
    }
  }

  @Override public void visitTypeInsn(int opcode, String type) {
    if (!deleteInstructions) {
      super.visitTypeInsn(opcode, type);
    }
  }

  @Override public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
    if (!deleteInstructions) {
      super.visitFieldInsn(opcode, owner, name, descriptor);
    }
  }

  @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    if (!deleteInstructions) {
      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }
  }

  @Override public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
      Object... bootstrapMethodArguments) {
    if (!deleteInstructions) {
      super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }
  }

  @Override public void visitJumpInsn(int opcode, Label label) {
    if (!deleteInstructions) {
      super.visitJumpInsn(opcode, label);
    }
  }

  @Override public void visitLabel(Label label) {
    super.visitLabel(label);
  }

  @Override public void visitLdcInsn(Object value) {
    if (!deleteInstructions) {
      super.visitLdcInsn(value);
    }
  }

  @Override public void visitIincInsn(int var, int increment) {
    if (!deleteInstructions) {
      super.visitIincInsn(var, increment);
    }
  }

  @Override public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
    if (!deleteInstructions) {
      super.visitTableSwitchInsn(min, max, dflt, labels);
    }
  }

  @Override public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    if (!deleteInstructions) {
      super.visitLookupSwitchInsn(dflt, keys, labels);
    }
  }

  @Override public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
    if (!deleteInstructions) {
      super.visitMultiANewArrayInsn(descriptor, numDimensions);
    }
  }

  @Override public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
    return super.visitInsnAnnotation(typeRef, typePath, descriptor, visible);
  }

  @Override public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
    if (!deleteInstructions) {
      super.visitTryCatchBlock(start, end, handler, type);
    }
  }

  @Override public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
    return super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);
  }

  @Override public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
    if (!deleteInstructions) {
      super.visitLocalVariable(name, descriptor, signature, start, end, index);
    }
  }

  @Override public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end,
      int[] index, String descriptor, boolean visible) {
    return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible);
  }

  @Override public void visitLineNumber(int line, Label start) {
    super.visitLineNumber(line, start);
  }

  @Override public void visitMaxs(int maxStack, int maxLocals) {
    super.visitMaxs(maxStack, maxLocals);
  }

  @Override public void visitEnd() {
    super.visitEnd();
  }
}
