/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

import edu.kit.joana.wala.flowless.pointsto.AliasGraph.MayAliasGraph;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo;
import edu.kit.joana.wala.flowless.util.AliasGraphIO;
import edu.kit.joana.wala.util.PrettyWalaNames;

public interface ExternalCallCheck {

	boolean resolveReflection();

	boolean isCallToModule(SSAInvokeInstruction invk);

	MethodInfo checkForModuleMethod(IMethod im);

	void registerAliasContext(SSAInvokeInstruction invk, int callNodeId, MayAliasGraph context);

	void setClassHierarchy(IClassHierarchy cha);

	public static ExternalCallCheck EMPTY = new ExternalCallCheck() {

		@Override
		public boolean resolveReflection() {
			return false;
		}

		@Override
		public boolean isCallToModule(SSAInvokeInstruction invk) {
			return false;
		}

		@Override
		public MethodInfo checkForModuleMethod(IMethod im) {
			return null;
		}

		@Override
		public void registerAliasContext(SSAInvokeInstruction invk,
				int callNodeId, MayAliasGraph context) {
		}

		@Override
		public void setClassHierarchy(IClassHierarchy cha) {
		}

	};

	public static class MethodListCheck implements ExternalCallCheck {

		private final Set<String> methods = new HashSet<String>();
		private final List<MethodInfo> mnfo = new LinkedList<MethodInfo>();

		private final String outDir;
		private final boolean debugOutput;
		private IClassHierarchy cha = null;

		public MethodListCheck(final Collection<String> list, final String outDir, final boolean debugOutput) {
			this.debugOutput = debugOutput;

			if (list != null) {
				for (String m : list) {
					this.methods.add(m);
				}
			}

			if (outDir.endsWith(File.separator)) {
				this.outDir = outDir + "ext_calls" + File.separator;
			} else {
				this.outDir = outDir + File.separator + "ext_calls" + File.separator;
			}
		}

		public void setClassHierarchy(IClassHierarchy cha) {
			this.cha = cha;
		}

		public MethodInfo checkForModuleMethod(IMethod im) {
			for (MethodInfo m : mnfo) {
				if (mayMatchCall(m, im.getReference(), im.isStatic())) {
					return m;
				}
			}

			return null;
		}

		public boolean isCallToModule(SSAInvokeInstruction invk) {
			final String call = invk.getDeclaredTarget().getSignature();

			if (methods.contains(call)) {
				return true;
			}

			for (MethodInfo m : mnfo) {
				if (mayMatchCall(m, invk.getDeclaredTarget(), invk.isStatic())) {
					return true;
				}
			}

			return false;
		}

		private boolean mayMatchCall(final MethodInfo m, final MethodReference ref, final boolean isStatic) {
			if (cha == null) {
				throw new IllegalStateException("Please set class hierarchy first!");
			}

			if (isStatic != m.isStatic()) {
				if (debugOutput) System.err.println("Uncomparable static vs. non-static method: '" + m.getName() + "' - '" + ref.getName().toString() + "'");
				return false;
			} else if (!ref.getName().toString().equals(m.getName())) {
				if (debugOutput) System.err.println("Names do not match: '" + m.getName() + "' - '" + ref.getName().toString() + "'");
				return false;
			} else if (m.getParameters().size() != ((isStatic ? 0 : 1) + ref.getNumberOfParameters())) {
				if (debugOutput) System.err.println("Parametercount does not match: '" + m.getName() + "':" + m.getParameters().size()
						+ " - '" + ref.getName().toString() + "':" + ref.getNumberOfParameters());
				return false;
			}

			final String clsName = m.getClassInfo().getWalaBytecodeName();
			TypeReference tref = TypeReference.find(ClassLoaderReference.Primordial, clsName);
			if (tref == null) {
				tref = TypeReference.find(ClassLoaderReference.Application, clsName);
			}

			if (tref == null) {
				throw new IllegalStateException("Could not find class with name " + clsName);
			}

			// lookup class
			final IClass cls = cha.lookupClass(tref);
			if (cls == null) {
				if (tref.getName().getClassName().toString().equals(m.getClassInfo().getName())) {
					if (debugOutput) System.err.println("Match: " + tref.getName().getClassName().toString() + " - " + m.getClassInfo().getName());
					return isReferencedMethod(m, ref);
				} else {
					if (debugOutput) System.err.println("No match: " + tref.getName().getClassName().toString() + " - " + m.getClassInfo().getName());
					return false;
				}
			}

			if (isStatic) {
				if (cls.isInterface() || cls.isAbstract()) {
					return false;
				}

				final IMethod im = cha.resolveMethod(ref);

				return isSameMethod(m, im);
			} else {
				final Set<IMethod> impls = cha.getPossibleTargets(cls, ref);

				for (final IMethod im : impls) {
					if (isSameMethod(m, im)) {
						return true;
					}
				}

				return false;
			}
		}

		private static boolean isReferencedMethod(final MethodInfo m, final MethodReference mr) {
			if (!isSameType(m.getReturnType(), mr.getReturnType())) {
				return false;
			}

			for (int pNum = 0; pNum < mr.getNumberOfParameters(); pNum++) {
				final TypeReference pType = mr.getParameterType(pNum);
				// method reference does not include the this-pointer
				final int p = (m.isStatic() ? pNum : pNum + 1);
				final String pTypeStr = m.getParameters().get(p).type;

				if (!isSameType(pTypeStr, pType)) {
					return false;
				}
			}

			return true;
		}

		private static boolean isSameMethod(final MethodInfo m, final IMethod im) {
			if (!isSameType(m.getReturnType(), im.getReturnType())) {
				return false;
			}

			for (int pNum = 0; pNum < im.getNumberOfParameters(); pNum++) {
				final TypeReference pType = im.getParameterType(pNum);
				final String pTypeStr = m.getParameters().get(pNum).type;

				if (pNum == 0 && !im.isStatic()) {
					// nonstatic methods do not need to have the same type on the first parameter, because the this
					// pointer is always a subclass of the this pointer of the parent.
					continue;
				}

				if (!isSameType(pTypeStr, pType)) {
					return false;
				}
			}

			return true;
		}

		private static Map<String, String> primitive2name = new HashMap<String, String>();
		static {
			primitive2name.put(TypeReference.BooleanName.toString(), "boolean");
			primitive2name.put(TypeReference.ByteName.toString(), "byte");
			primitive2name.put(TypeReference.CharName.toString(), "char");
			primitive2name.put(TypeReference.DoubleName.toString(), "double");
			primitive2name.put(TypeReference.FloatName.toString(), "float");
			primitive2name.put(TypeReference.IntName.toString(), "int");
			primitive2name.put(TypeReference.LongName.toString(), "long");
			primitive2name.put(TypeReference.ShortName.toString(), "short");
			primitive2name.put(TypeReference.VoidName.toString(), "void");
		}

		private static boolean isSameType(String st, final TypeReference tr) {
			final String trefName;

			if (tr.isPrimitiveType()) {
				trefName = primitive2name.get(tr.getName().toString());
			} else if (tr.isArrayType()) {
				final TypeReference elem = tr.getInnermostElementType();
				if (elem.isPrimitiveType()) {
					trefName = primitive2name.get(elem.getName().toString());
				} else {
					trefName = elem.toString();
				}

				if (!st.contains("[")) {
					return false;
				}

				st = st.replace('[', ' ');
				st = st.replace(']', ' ');
				st = st.trim();
			} else {
				trefName = tr.getName().toString();
			}

			return trefName.contains(st.replace('.', '/'));
		}

		public void addMethod(final MethodInfo method) {
			mnfo.add(method);
		}

		public void addMethod(final String signature) {
			methods.add(signature);
		}

		public void addMethod(final MethodReference ref) {
			methods.add(ref.getSignature());
		}

		@Override
		public void registerAliasContext(final SSAInvokeInstruction invk, final int callNodeId, final MayAliasGraph context) {
			if (!Main.checkOrCreateOutputDir(outDir)) {
				return;
			}

			final String dir = outDir + "c_" + callNodeId;
			if (!Main.checkOrCreateOutputDir(dir)) {
				return;
			}

			final String file = dir + File.separator + PrettyWalaNames.methodName(invk.getDeclaredTarget()) + ".alias";

			try {
				AliasGraphIO.writeToFile(context, file);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		@Override
		public boolean resolveReflection() {
			return true;
		}
	}

}
