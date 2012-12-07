/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.wala;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.impl.AbstractRootMethod;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.wala.flowless.pointsto.PointsToSetBuilder.MergedPtsParameter;
import edu.kit.joana.wala.flowless.pointsto.PointsToSetBuilder.PointsTo;
import edu.kit.joana.wala.flowless.pointsto.PointsToSetBuilder.PtsElement;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter.ArrayFieldParameter;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter.NormalFieldParameter;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter.RootParameter;
import edu.kit.joana.wala.flowless.pointsto.PtsParameterVisitor;
import edu.kit.joana.wala.util.ParamNum;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class EntrypointWithPTS extends DefaultEntrypoint {

	private final PointsTo pts;
	private final boolean noSubclasses;

	public EntrypointWithPTS(IMethod method, IClassHierarchy cha, PointsTo initalPTS, boolean noSubclasses) {
		super(method, cha);
		this.pts = initalPTS;
		this.noSubclasses = noSubclasses;
	}

	public EntrypointWithPTS(MethodReference method, IClassHierarchy cha, PointsTo initalPTS, boolean noSubclasses) {
		super(method, cha);
		this.pts = initalPTS;
		this.noSubclasses = noSubclasses;
	}

	private AbstractRootMethod last = null;
	private TObjectIntHashMap<PtsElement> pts2ssa = null;

	/**
	 * This is called for each parameter. At the first call we have to create the neccessary
	 * commands that create all objects and set the fields.
	 *
	 * Add allocation statements to the fake root method for each possible value
	 * of parameter i. If necessary, add a phi to combine the values.
	 *
	 * @return value number holding the parameter to the call; -1 if there was
	 *         some error
	 */
	protected int makeArgument(final AbstractRootMethod m, final int i) {
		// BEWARE: m is not the method that is called, but the root method where the call to this method (this.method)
		// is inserted. So we have to add object creation instructions according to the points-to set to m first.
		if (last != m) {
			// create initial objects from pts.
			pts2ssa = createObjectInstancesMatchingPTS(pts, m, noSubclasses);
			last = m;
		}


		// use points-to info to create parameter initialization code.
		final TypeReference[] p = getParameterTypes(i);

		final ParamNum ptsParamNumId = ParamNum.fromIMethod(method, i);
		final RootParameter root = pts.getRootParameter(ptsParamNumId);

		if (p.length == 0) {
			// no matching types found -> error
			return -1;
		} else if (p.length == 1 && p[0].isPrimitiveType()) {
			// hurray only a single possible type. is is going to be easy
			return m.addLocal();
		} else {
			// booooo! so many possibilities.

			Set<PtsElement> rootPts = pts.getPointsTo(root);
			int[] values = new int[rootPts.size()];

			{
				int index = 0;
				for (PtsElement pElem : rootPts) {
					if (pts2ssa.contains(pElem)) {
						int ssaVar = pts2ssa.get(pElem);
						values[index] = ssaVar;
						index++;
					} else {
						System.err.println("No ssa var for " + pElem + " - " + root.getName() + " : " + root.getType());
					}
				}
			}

			final int rootSsaVar = m.addPhi(values);

			if (root.hasChildren()) {
				initializeChildren(m, rootSsaVar, root.getChildren());
			}

			//TODO whats that for? It is never used!

//			TypeAbstraction a;
//			if (p[0].isPrimitiveType()) {
//				a = PrimitiveType.getPrimitive(p[0]);
//				for (i = 1; i < p.length; i++) {
//					a = a.meet(PrimitiveType.getPrimitive(p[i]));
//				}
//			} else {
//				IClassHierarchy cha = m.getClassHierarchy();
//				IClass p0 = cha.lookupClass(p[0]);
//				a = new ConeType(p0);
//				for (i = 1; i < p.length; i++) {
//					IClass pi = cha.lookupClass(p[i]);
//					a = a.meet(new ConeType(pi));
//				}
//			}

			return rootSsaVar;
		}
	}

	private void initializeChildren(final AbstractRootMethod method, final int baseSsaVar, Collection<PtsParameter> children) {
		FieldInitializeVisitor visitor = new FieldInitializeVisitor(method, baseSsaVar);

		for (PtsParameter child : children) {
			if (child.isRoot()) {
				throw new IllegalStateException();
			}

			child.accept(visitor);
		}
	}

	/**
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	private class FieldInitializeVisitor implements PtsParameterVisitor {

		private final Stack<Integer> baseSsaVars = new Stack<Integer>();
		private final AbstractRootMethod method;

		private FieldInitializeVisitor(AbstractRootMethod method, final int baseSsaVar) {
			this.method = method;
			this.baseSsaVars.push(baseSsaVar);
		}

		private int getBaseVar() {
			if (baseSsaVars.isEmpty()) {
				throw new IllegalStateException();
			}

			return baseSsaVars.peek();
		}

		private int getValueVar(final PtsParameter p) {
			int fieldValueVarNum = -1;
			Set<PtsElement> fieldPts = pts.getPointsTo(p);
			if (p.isPrimitive()) {
				fieldValueVarNum = method.addLocal();
			} else if (fieldPts.size() > 1) {
				// create a phi node.
				final int[] values = new int[fieldPts.size()];
				int index = 0;
				for (PtsElement ptsElem : fieldPts) {
					final int ptsElemValNum = pts2ssa.get(ptsElem);
					assert ptsElemValNum > 0 : "No pts elem val num for " + ptsElem + " of " + p;
					values[index] = ptsElemValNum;
					index++;
				}

				fieldValueVarNum = method.addPhi(values);
			} else if (fieldPts.size() == 1) {
				PtsElement ptsElem = fieldPts.iterator().next();
				final int ptsElemValNum = pts2ssa.get(ptsElem);
				assert ptsElemValNum > 0;
				fieldValueVarNum = ptsElemValNum;
			} else {
				throw new IllegalStateException("Parameter " + p + " has no points-to element.");
			}

			assert fieldValueVarNum > 0;

			return fieldValueVarNum;
		}

		@Override
		public void visit(ArrayFieldParameter array) {
			final int baseSsaVar = getBaseVar();
			final int fieldValueVarNum = getValueVar(array);

			final int index = method.addLocal();

			method.addSetArrayField(array.getType(), baseSsaVar, index, fieldValueVarNum);

			if (array.hasChildren()) {
				final int baseArray = method.addGetArrayField(array.getType(), baseSsaVar, index);
				baseSsaVars.push(baseArray);

				for (PtsParameter param : array.getChildren()) {
					param.accept(this);
				}

				assert baseSsaVars.peek() == baseArray;
				baseSsaVars.pop();
			}
		}

		@Override
		public void visit(NormalFieldParameter field) {
			final int baseSsaVar = getBaseVar();
			final int fieldValueVarNum = getValueVar(field);

			method.addSetInstance(field.getFieldRef(), baseSsaVar, fieldValueVarNum);

			if (field.hasChildren()) {
				final int baseField = method.addGetInstance(field.getFieldRef(), baseSsaVar);
				baseSsaVars.push(baseField);

				for (PtsParameter param : field.getChildren()) {
					param.accept(this);
				}

				assert baseSsaVars.peek() == baseField;
				baseSsaVars.pop();
			}
		}

		@Override
		public void visit(MergedPtsParameter merged) {
			final int baseSsaVar = getBaseVar();
			final int fieldValueVarNum = getValueVar(merged);

			int index = -1;

			for (PtsParameter p : merged.getMembers()) {
				if (p instanceof NormalFieldParameter) {
					NormalFieldParameter field = (NormalFieldParameter) p;
					method.addSetInstance(field.getFieldRef(), baseSsaVar, fieldValueVarNum);
				} else if (p instanceof ArrayFieldParameter) {
					ArrayFieldParameter array = (ArrayFieldParameter) p;
					if (index <= 0) {
						index = method.addLocal();
					}
					method.addSetArrayField(array.getType(), baseSsaVar, index, fieldValueVarNum);
				} else {
					throw new IllegalStateException("Found a merged parameter inside a merged parameter....");
				}
			}

			if (merged.hasChildren()) {
				// perhaps we should create get field instructions for all merged params...
				baseSsaVars.push(fieldValueVarNum);

				for (PtsParameter p : merged.getChildren()) {
					p.accept(this);
				}

				baseSsaVars.pop();
			}
		}

		@Override
		public void visit(RootParameter root) {
			throw new IllegalStateException("No root parameter as field possible.");
		}

	}

	private static TObjectIntHashMap<PtsElement> createObjectInstancesMatchingPTS(final PointsTo pts,
			final AbstractRootMethod m, boolean noSubclasses) {
		final Logger debug = Log.getLogger(Log.L_WALA_CORE_DEBUG);
		debug.outln("Initializing object instance mapping.");

		// maps point-to element to the number of the ssa variable that contains the reference to the representative object instance(s).
		TObjectIntHashMap<PtsElement> pts2ssaVar = new TObjectIntHashMap<PtsElement>();

		final IClassHierarchy cha = m.getClassHierarchy();

		for (PtsElement elem : pts.getAllPointsToElements()) {
			Set<PtsParameter> params = pts.getAllParamsPointingTo(elem);

			debug.out("\t" + elem + ": ");

			IClass commonSubtype = cha.lookupClass(TypeReference.JavaLangObject);

			for (PtsParameter p : params) {
				TypeReference pType = p.getType();

				debug.out(p + " | ");

				if (pType.isPrimitiveType()) {
					// no object creation for a primitive type needed -> skip it..
					commonSubtype = null;
					break;
				} else {
					IClass pClass = cha.lookupClass(pType);

					if (pClass == null || pClass.getReference() == null) {
						continue;
					}

					if (cha.isSubclassOf(commonSubtype, pClass)) {
						// everything is fine. commonSubtype is subtype of pClass.
					} else if (cha.isSubclassOf(pClass, commonSubtype)) {
						// we have to switch, as pClass is a subclass of commonSubclass
						commonSubtype = pClass;
					} else {
						// if commonSubclass is not a subclass of pClass
						throw new IllegalStateException("Found incompatible classes " + commonSubtype + " <-> " + pClass);
					}
				}
			}

			if (commonSubtype != null) {
				debug.outln("-> " + commonSubtype.getName());

				// assert - recheck if commonSubtype is really a subtype of all referencing parameter types
				for (PtsParameter p : params) {
					TypeReference pType = p.getType();
					IClass pClass = cha.lookupClass(pType);

					if (pClass == null || pClass.getReference() == null) {
						continue;
					}

					if (!cha.isSubclassOf(commonSubtype, pClass)) {
						throw new IllegalStateException(commonSubtype + " is not a subclass of " + pClass);
					}
				}

				if (noSubclasses && !commonSubtype.isAbstract()) {
					// all non-primitive types are instantiated
					SSANewInstruction newObj = m.addAllocationWithoutCtor(commonSubtype.getReference());
					final int ssaVar = newObj.getDef();
					if (ssaVar == 0) {
						throw new IllegalStateException();
					}
					pts2ssaVar.put(elem, ssaVar);
				} else {
					Collection<IClass> subTypes = null;
					if (commonSubtype.isInterface()) {
						Set<IClass> impls = cha.getImplementors(commonSubtype.getReference());
						subTypes = new HashSet<IClass>();
						for (IClass cls : impls) {
							subTypes.addAll(cha.computeSubClasses(cls.getReference()));
						}
					} else {
						subTypes = cha.computeSubClasses(commonSubtype.getReference());
					}

					List<Integer> ssaVars = new ArrayList<Integer>();
					for (IClass cls : subTypes) {
						if (cls.isAbstract()) {
							continue;
						}

						SSANewInstruction newObj = m.addAllocationWithoutCtor(cls.getReference());
						ssaVars.add(newObj.getDef());
					}

					if (ssaVars.size() == 0) {
						System.err.println("No implementing subtypes found for " + commonSubtype);
						final int ssaVar = m.addLocal();
						if (ssaVar == 0) {
							throw new IllegalStateException();
						}
						pts2ssaVar.put(elem, ssaVar);
					} else {
						int[] vars = new int[ssaVars.size()];
						for (int i = 0; i < vars.length; i++) {
							vars[i] = ssaVars.get(i);
						}
						final int ssaVar = m.addPhi(vars);

						if (ssaVar == 0) {
							throw new IllegalStateException();
						}
						pts2ssaVar.put(elem, ssaVar);
					}
				}
			} else {
				debug.outln("-> primitive");
			}
		}

		return pts2ssaVar;
	}

}
