/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.pointsto;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;

import edu.kit.joana.wala.flowless.pointsto.AliasGraph.MayAliasGraph;
import edu.kit.joana.wala.flowless.pointsto.AliasGraph.NoMayAliasGraph;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter.ArrayFieldParameter;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter.FieldParameter;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter.NormalFieldParameter;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter.RootParameter;
import edu.kit.joana.wala.util.ParamNum;
import edu.kit.joana.wala.util.ParamNum.PType;

/**
 * This class contains methods to compute maximal side-effects for a given method.
 * Its is also used to compute the maximal and minimal aliasing configurations (using
 * the type system as restriction) of parameter nodes that are used to represent
 * method side-effects.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public final class SideEffectApproximator {

	private final IClassHierarchy cha;

	/**
	 * Creates a new instance of the points-to util class. A class hierarchy has
	 * to be provided.
	 * @param cha The class hierarchy that is going to be used for computation of
	 * method side-effects etc.
	 */
	public SideEffectApproximator(IClassHierarchy cha) {
		if (cha == null) {
			throw new IllegalArgumentException("class hierarchy is null");
		}

		this.cha = cha;
	}

	/**
	 * Approximates for a given method which parameters and fields can be accessed
	 * during method execution. It computes a RootParameter for each method parameter.
	 * A RootParameter may have children that correspond to object fields that may
	 * be accessed through this objects. Recursive field referenced are cut off on their
	 * second appearance.
	 * @param method The method we are going to approximate all accessible objects and fields for.
	 * @return An array of RootParameters. One for each method parameter (including this pointer).
	 */
	public RootParameter[] createInputParamTrees(IMethod method) {
		RootParameter[] params = new RootParameter[method.getNumberOfParameters()];

		for (int i = 0; i < method.getNumberOfParameters(); i++) {
			params[i] = createParamTree(method, i);
		}

		return params;
	}

	/**
	 * Approximates for a given method which fields could be changed during method
	 * invocation. This includes all fields reachable through the method parameters
	 * as well through the return value and exceptions.
	 * @param method The method we are going to approximate all potentially modified
	 * objects and fields for.
	 * @return An array of RootParameters. One for each method parameter (including
	 * this pointer) as well as the return and exception value.
	 */
	public RootParameter[] createOutputParamTrees(IMethod method) {
		final int numAdditionalParams = (method.getReturnType() != TypeReference.Void ? 2 : 1); /* exc & return value */
		RootParameter[] params = new RootParameter[method.getNumberOfParameters() + numAdditionalParams];

		for (int i = 0; i < method.getNumberOfParameters(); i++) {
			params[i] = createParamTree(method, i);
		}

		if (method.getReturnType() != TypeReference.Void) {
			RootParameter result = createResultParamTree(method);
			params[params.length - 2] = result;
		}

		RootParameter exc = createExceptionParamTree(method);
		params[params.length - 1] = exc;

		return params;
	}

	/**
	 * Creates a parameter tree for the result value of the given method. It includes
	 * all object fields that may have been accessed by the method.
	 * @param method The method we compute the return value parameter tree for.
	 * @return The RootParameter for the return value of the given method.
	 */
	public RootParameter createResultParamTree(IMethod method) {
		return doCreateParamTree(method, ParamNum.createSpecial(PType.RESULT_VAL));
	}

	/**
	 * Creates a parameter tree for all exceptions that may be thrown by the given
	 * method.
	 * @param method The method we compute the exception value parameter tree for.
	 * @return The RootParameter for the exception value of the given method.
	 */
	public RootParameter createExceptionParamTree(IMethod method) {
		if (method == null) {
			throw new IllegalArgumentException();
		}

		// No precise trees for exceptions. Its not worth the effort...
//		return doCreateParamTree(method, SimpleParameter.EXCEPTION_VAL);
		return new RootParameter(method, ParamNum.createSpecial(PType.EXCEPTION_VAL));
	}

	private RootParameter createParamTree(IMethod method, int imParamNum) {
		if (method == null || imParamNum < 0 || imParamNum >= method.getNumberOfParameters()) {
			throw new IllegalArgumentException("Parameter number not in range...");
		}

		final ParamNum paramNum = ParamNum.fromIMethod(method, imParamNum);
		
		return doCreateParamTree(method, paramNum);
	}

	/**
	 * The parameter paramNum must already be converted into the ParamNummberUtil format.
	 */
	private RootParameter doCreateParamTree(IMethod method, ParamNum paramNum) {
		RootParameter root = new RootParameter(method, paramNum);

		List<PtsParameter> work = new LinkedList<PtsParameter>();
		work.add(root);

		while (!work.isEmpty()) {
			PtsParameter current = work.remove(0);
			TypeReference type = current.getType();

			if (!type.isPrimitiveType()) {
				if (type.isArrayType()) {
					assert (current instanceof FieldParameter) || (current instanceof RootParameter);
					if (current instanceof FieldParameter) {
						FieldParameter fParam = (FieldParameter) current;
						ArrayFieldParameter arrayParam = new ArrayFieldParameter(fParam, type.getArrayElementType());
						work.add(arrayParam);
					} else {
						RootParameter rParam = (RootParameter) current;
						ArrayFieldParameter arrayParam = new ArrayFieldParameter(rParam, type.getArrayElementType());
						work.add(arrayParam);
					}
				} else {
					IClass cls = cha.lookupClass(type);
					if (cls != null) {
						final Collection<IField> fields = cls.getAllInstanceFields();
						if (fields != null) {
							for (IField field : fields) {
								if (!current.hasParent(field)) {
									NormalFieldParameter fParam = new NormalFieldParameter(current, field);
									work.add(fParam);
								} else {
									// we cut at level 1
								}
							}
						}
					}
				}
			}
		}

		return root;
	}

	/**
	 * Creates a graph containing all parameter fields of the provided root parameters.
	 * Nodes are connected iff aliasing between them IS possible.
	 * This initial information is derived from the type information.
	 * @param params Array of root parameters from a certain method
	 * @return A graph showing the aliasing relations that are possible.
	 */
	public MayAliasGraph createMayAliasGraph(RootParameter[] params, boolean isStaticMethod) {
		NoMayAliasGraph noMay = createNoMayAliasGraph(params, isStaticMethod);
		return noMay.constructNegated();
	}

	/**
	 * Creates a graph containing all parameter fields of the provided root parameters.
	 * Nodes are connected iff aliasing between them IS NOT possible.
	 * This initial information is derived from the type information.
	 * @param params Array of root parameters from a certain method
	 * @return A graph showing the aliasing relations that are definitely not possible.
	 */
	public NoMayAliasGraph createNoMayAliasGraph(RootParameter[] params, boolean isStaticMethod) {
		NoMayAliasGraph graph = new NoMayAliasGraph(isStaticMethod);

		List<PtsParameter> work = new LinkedList<PtsParameter>();
		for (RootParameter root : params) {
			work.add(root);
		}

		while (!work.isEmpty()) {
			PtsParameter current = work.remove(0);
			graph.addNode(current);

			if (current.hasChildren()) {
				work.addAll(current.getChildren());
			}
		}

		// annotate impossible aliases inferred from type information
		for (PtsParameter param : graph) {

			final boolean isPrimitiveRoot = param.isRoot() && param.getType().isPrimitiveType();

			for (PtsParameter other : graph) {
				if (param == other) {
					// no self references as parameter are obviously always aliased to themself...
					continue;
				}

				if (isPrimitiveRoot || other.isRoot() && other.getType().isPrimitiveType()) {
					// primitive method parameter can not be aliased
					graph.addEdge(param, other);
				} else if (!param.isRoot() && !other.isRoot()
						&& param.getType().isPrimitiveType() && other.getType().isPrimitiveType()) {
					// special treatment for primitive fields
					// they may only alias iff their types match AND if the types of their parents match...
					if (!paramTypesMayAlias(param, other)
							|| !paramTypesMayAlias(param.getParent(), other.getParent())) {
						graph.addEdge(param, other);
					}
				} else if (!paramTypesMayAlias(param, other)) {
					graph.addEdge(param, other);
				}
			}
		}

		return graph;
	}

	private boolean paramTypesMayAlias(PtsParameter param, PtsParameter other) {
		TypeReference paramType = param.getType();
		TypeReference otherType = other.getType();

		return typesMayAlias(paramType, otherType);
	}

	private boolean typesMayAlias(final TypeReference paramType, final TypeReference otherType) {
		if (paramType == otherType) {
			return true;
		} else if (paramType.isClassType() && otherType.isClassType()) {
			IClass paramClass = cha.lookupClass(paramType);
			IClass otherClass = cha.lookupClass(otherType);
			if (paramClass == null || otherClass == null) {
				return false;
			} else {
				return cha.isSubclassOf(otherClass, paramClass);
			}
		} else if (paramType.isPrimitiveType() && otherType.isPrimitiveType()) {
			return paramType.equals(otherType);
		} else if (paramType.isArrayType() && otherType.isArrayType()) {
			// switch param and other as the subclass relation is invers for arrays.
			return typesMayAlias(otherType.getArrayElementType(), paramType.getArrayElementType());
		}

		return false;
	}

}
