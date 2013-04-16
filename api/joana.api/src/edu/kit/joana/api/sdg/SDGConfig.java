/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;

public class SDGConfig {

	private String classPath;
	private String entryMethod;
	private Stubs stubsPath;
	private ExceptionAnalysis exceptionAnalysis;
	private FieldPropagation fieldPropagation;
	private PointsToPrecision pointsToPrecision;
	private boolean computeAccessPaths;
	private boolean computeInterferences;
	private MHPType mhpType = MHPType.NONE;

	public SDGConfig(String classPath, String entryMethod, Stubs stubsPath) {
		this(classPath, entryMethod, stubsPath, ExceptionAnalysis.INTERPROC, FieldPropagation.OBJ_GRAPH, PointsToPrecision.CONTEXT_SENSITIVE, false, false, MHPType.NONE);
	}

	public SDGConfig(String classPath, String entryMethod, Stubs stubsPath, ExceptionAnalysis exceptionAnalysis, FieldPropagation fieldPropagation,
			PointsToPrecision pointsToPrecision, boolean computeAccessPaths, boolean computeInterferences, MHPType mhpType) {
		this.classPath = classPath;
		this.entryMethod = entryMethod;
		this.stubsPath = stubsPath;
		this.exceptionAnalysis = exceptionAnalysis;
		this.fieldPropagation = fieldPropagation;
		this.pointsToPrecision = pointsToPrecision;
		this.computeAccessPaths = computeAccessPaths;
		this.computeInterferences = computeInterferences;
		this.mhpType = mhpType;
	}

	/**
	 * @return the classPath
	 */
	public String getClassPath() {
		return classPath;
	}

	/**
	 * @param classPath the classPath to set
	 */
	public void setClassPath(String classPath) {
		this.classPath = classPath;
	}

	/**
	 * @return the entry method
	 */
	public String getEntryMethod() {
		return entryMethod;
	}

	/**
	 * @param entryMethod the entry method to set
	 */
	public void setEntryMethod(String entryMethod) {
		this.entryMethod = entryMethod;
	}

	/**
	 * @return the stubsPath
	 */
	public Stubs getStubsPath() {
		return stubsPath;
	}

	/**
	 * @param stubsPath the stubsPath to set
	 */
	public void setStubsPath(Stubs stubsPath) {
		this.stubsPath = stubsPath;
	}

	/**
	 * @return the fieldPropagation
	 */
	public FieldPropagation getFieldPropagation() {
		return fieldPropagation;
	}

	/**
	 * @param fieldPropagation the fieldPropagation to set
	 */
	public void setFieldPropagation(FieldPropagation fieldPropagation) {
		this.fieldPropagation = fieldPropagation;
	}

	/**
	 * @return the pointsToPrecision
	 */
	public PointsToPrecision getPointsToPrecision() {
		return pointsToPrecision;
	}

	/**
	 * @param pointsToPrecision the pointsToPrecision to set
	 */
	public void setPointsToPrecision(PointsToPrecision pointsToPrecision) {
		this.pointsToPrecision = pointsToPrecision;
	}

	/**
	 * @return the mhpType
	 */
	public MHPType getMhpType() {
		return mhpType;
	}

	/**
	 * @param mhpType the mhpType to set
	 */
	public void setMhpType(MHPType mhpType) {
		this.mhpType = mhpType;
	}

	/**
	 * @return the computeInterferences
	 */
	public boolean computeInterferences() {
		return computeInterferences;
	}

	/**
	 * @param computeInterferences the computeInterferences to set
	 */
	public void setComputeInterferences(boolean computeInterferences) {
		this.computeInterferences = computeInterferences;
	}

	/**
	 * @return the exceptionAnalysis
	 */
	public ExceptionAnalysis getExceptionAnalysis() {
		return exceptionAnalysis;
	}

	/**
	 * @param exceptionAnalysis the exceptionAnalysis to set
	 */
	public void setExceptionAnalysis(ExceptionAnalysis exceptionAnalysis) {
		this.exceptionAnalysis = exceptionAnalysis;
	}

	/**
	 * @return the computeAccessPaths
	 */
	public boolean computeAccessPaths() {
		return computeAccessPaths;
	}

	/**
	 * @param computeAccessPaths the computeAccessPaths to set
	 */
	public void setComputeAccessPaths(boolean computeAccessPaths) {
		this.computeAccessPaths = computeAccessPaths;
	}


}
