/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.immutables;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAComparisonInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAConversionInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstanceofInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstruction.IVisitor;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;

import edu.kit.joana.deprecated.jsdg.util.Util;

/**
 * Some test code. Do not look at it too closely ;).
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ExtractImmutables {

	private final CallGraph cg;
	private final ClassHierarchy cha;
	private final HeapGraph hg;
	private final PointerAnalysis pta;
	private final Set<IClass> immutables;
	private final PureVisitor pureVisitor = new PureVisitor();

	private ExtractImmutables(CallGraph cg, ClassHierarchy cha, PointerAnalysis pta) {
		this.cg = cg;
		this.cha = cha;
		this.pta = pta;
		this.hg = pta.getHeapGraph();
		this.immutables = HashSetFactory.make();
	}

	public static Set<IClass> getImmutables(CallGraph cg, ClassHierarchy cha, PointerAnalysis pta) {
		ExtractImmutables ei = new ExtractImmutables(cg, cha, pta);
		Util.dumpCallGraph(cg, "ExtractImmutables", null);
		ei.compute();
		return ei.immutables;
	}

	private static class Class {
		private final IClass cls;
		private final Set<Method> contained = HashSetFactory.make();
		private final Set<Method> nowrites = HashSetFactory.make();

		private Class(final IClass cls) {
			this.cls = cls;
		}

		public Method getMethod(CGNode node) {
			return getMethod(node.getMethod());
		}

		public Method getMethod(IMethod m) {
			Method tmp = new Method(m);
			if (contained.contains(tmp)) {
				for (Method it : contained) {
					if (it.equals(tmp)) {
						return it;
					}
				}
			}

			return null;
		}

		private void addCGNode(CGNode node, boolean isLeaf) {
			IMethod m = node.getMethod();
			Method method = new Method(m);
			if (contained.contains(method)) {
				for (Method it : contained) {
					if (it.equals(method)) {
						method = it;
						break;
					}
				}
			} else {
				contained.add(method);
			}

			method.cgNodes.add(node);
			method.isLeaf &= isLeaf;
		}

		public String toString() {
			TypeName t = cls.getName();

			boolean isFinal;
			try {
				// see JVM specification section 4.3 for modifiers definitions
				int modifiers = cls.getModifiers();
				isFinal = (modifiers & 0x0010) != 0;
			} catch (UnsupportedOperationException op) {
				// fakeRoot / synthetic classes do not support getModifiers
				// as they are build during analysis they cannot be extended by
				// the base program and therefore are final
				isFinal = true;
			}

			String str = (isFinal ? "final " : "") + "class " + t.getPackage() + "." + t.getClassName() + "\n";

			for (Method m : contained) {
				str += "\t" + m + "\n";
			}

			return str;
		}

		public int hashCode() {
			return cls.hashCode() + 31337;
		}

		public boolean equals(Object obj) {
			return (obj instanceof Class) && cls.equals(((Class) obj).cls);
		}
	}

	private static class Method {
		private final IMethod method;
		private final Set<CGNode> cgNodes = HashSetFactory.make();
		private boolean isLeaf = true;
		private boolean isPure = false;

		private Method(final IMethod method) {
			this.method = method;
		}

		public String toString() {
			return (isLeaf ? "*" : "-") + (isPure ? "!" : "-") + Util.methodName(method);
		}

		public int hashCode() {
			return method.hashCode() + 4711;
		}

		public boolean equals(Object obj) {
			return (obj instanceof Method) && method.equals(((Method) obj).method);
		}
	}

	private void compute() {
		Map<IClass, Class> classes = HashMapFactory.make();
		Set<Method> leafs = HashSetFactory.make();
		Set<Method> noleafs = HashSetFactory.make();

		for (CGNode node : cg) {
			boolean isLeaf = cg.getSuccNodeCount(node) == 0;
			addMethod(classes, node, isLeaf);
		}

		for (IClass icls : classes.keySet()) {
			Class cls = classes.get(icls);
			for (Method m : cls.contained) {
				if (m.isLeaf) {
					leafs.add(m);
				} else {
					noleafs.add(m);
				}

				isPure(m);
			}
		}

		boolean changed = true;
		while (changed) {
			changed = false;

			for (CGNode node : cg) {
				Method m = getMethod(classes, node);
				if (!m.isLeaf) {
					boolean leaf = true;

					for (Iterator<? extends CGNode> it = cg.getSuccNodes(node); it.hasNext() && leaf; ) {
						CGNode succ = it.next();
						Method mSucc = getMethod(classes, succ);
						leaf &= mSucc.isLeaf && mSucc.isPure;
					}

					m.isLeaf = leaf;
					changed |= leaf;
				}
			}
		}


		for (IClass icls : classes.keySet()) {
			Class cls = classes.get(icls);
			System.out.println(cls);
		}


	}

	private Method getMethod(Map<IClass, Class> classes, CGNode node) {
		Class cls = classes.get(node.getMethod().getDeclaringClass());
		Method m = cls.getMethod(node);
		return m;
	}

	private void isPure(Method m) {
		boolean pure = true;

		// Constructors are per definition pure
		// Static method can not change the state of their this-object as they dont have any
		if (!m.method.isInit() && !m.method.isStatic()) {
			for (CGNode node : m.cgNodes) {
				pure &= isPure(node);
				if (!pure) {
					break;
				}
			}
		}


		m.isPure = pure;
	}

	private boolean isPure(CGNode node) {
		boolean pure = true;

		IR ir = node.getIR();
		if (ir != null) {
			for (Iterator<SSAInstruction> it = ir.iterateNormalInstructions(); it.hasNext() && pure;) {
				SSAInstruction instr = it.next();
				instr.visit(pureVisitor);
				pure &= pureVisitor.isPure;
			}
		}

		return pure;
	}

	private static class PureVisitor implements IVisitor {
		private boolean isPure = true;

		public void visitArrayLength(SSAArrayLengthInstruction instruction) {
			isPure = true;
		}

		public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
			isPure = true;
		}

		public void visitArrayStore(SSAArrayStoreInstruction instruction) {
			isPure = false;
		}

		public void visitBinaryOp(SSABinaryOpInstruction instruction) {
			isPure = true;
		}

		public void visitCheckCast(SSACheckCastInstruction instruction) {
			isPure = true;
		}

		public void visitComparison(SSAComparisonInstruction instruction) {
			isPure = true;
		}

		public void visitConditionalBranch(
				SSAConditionalBranchInstruction instruction) {
			isPure = true;
		}

		public void visitConversion(SSAConversionInstruction instruction) {
			isPure = true;
		}

		public void visitGet(SSAGetInstruction instruction) {
			isPure = true;
		}

		public void visitGetCaughtException(
				SSAGetCaughtExceptionInstruction instruction) {
			isPure = true;
		}

		public void visitGoto(SSAGotoInstruction instruction) {
			isPure = true;
		}

		public void visitInstanceof(SSAInstanceofInstruction instruction) {
			isPure = true;
		}

		public void visitInvoke(SSAInvokeInstruction instruction) {
			isPure = true;
		}

		public void visitMonitor(SSAMonitorInstruction instruction) {
			isPure = true;
		}

		public void visitNew(SSANewInstruction instruction) {
			isPure = true;
		}

		public void visitPhi(SSAPhiInstruction instruction) {
			isPure = true;
		}

		public void visitPi(SSAPiInstruction instruction) {
			isPure = true;
		}

		public void visitPut(SSAPutInstruction instruction) {
			// changes to static variables do not change object properties
			isPure = instruction.isStatic();
		}

		public void visitReturn(SSAReturnInstruction instruction) {
			isPure = true;
		}

		public void visitSwitch(SSASwitchInstruction instruction) {
			isPure = true;
		}

		public void visitThrow(SSAThrowInstruction instruction) {
			isPure = true;
		}

		public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
			isPure = true;
		}

		public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {
			isPure = true;
		}


	}

	private void addMethod(Map<IClass, Class> map, CGNode node, final boolean isLeaf) {
		IMethod m = node.getMethod();
		IClass cls = m.getDeclaringClass();
		Class cl = map.get(cls);
		if (cl == null) {
			cl = new Class(cls);
			map.put(cls, cl);
		}

		cl.addCGNode(node, isLeaf);
	}

}
