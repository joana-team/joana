/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import com.ibm.wala.cfg.exc.intra.MethodState;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.pruned.ApplicationLoaderPolicy;
import com.ibm.wala.ipa.callgraph.pruned.PruningPolicy;

import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.util.JoanaConstants;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.CGConsumer;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.core.params.objgraph.SideEffectDetectorConfig;
import edu.kit.joana.wala.util.pointsto.ObjSensZeroXCFABuilder;

public class SDGConfig {
	private PruningPolicy pruningPolicy = ApplicationLoaderPolicy.INSTANCE;
	private String classPath;
	private String thirdPartyLibsPath;
	private String entryMethod;
	private Stubs stubsPath;
	private String exclusions = SDGBuildPreparation.STD_EXCLUSION_REG_EXP;
	private ExceptionAnalysis exceptionAnalysis;
	private boolean ignoreIndirectFlows = false;
	private MethodState defaultExceptionMethodState = null;
	private FieldPropagation fieldPropagation;
	private PointsToPrecision pointsToPrecision;
	private boolean computeAccessPaths;
	private boolean computeInterferences;
	private MHPType mhpType = MHPType.NONE;
	private SideEffectDetectorConfig sideEffects;
	private ObjSensZeroXCFABuilder.MethodFilter methodFilter;
	private String nativesXML = JoanaConstants.DEFAULT_NATIVES_XML;
	private boolean computeAllocationSites = false;
	private CGConsumer cgConsumer = null;
	private ContextSelector ctxSelector;
	private ConstructionNotifier notifier = null;
	
	public SDGConfig(String classPath, String entryMethod, Stubs stubsPath) {
		this(classPath, entryMethod, stubsPath, ExceptionAnalysis.INTERPROC, FieldPropagation.OBJ_GRAPH, PointsToPrecision.INSTANCE_BASED, false, false, MHPType.NONE);
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
	 * @return the exclusions
	 */
	public String getExclusions() {
		return exclusions;
	}

	/**
	 * @param exclusions the exclusions to set
	 */
	public void setExclusions(String exclusions) {
		this.exclusions = (exclusions == null ? "" : exclusions);
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
	 * Returns the method filter used by this configuration. Can be {@code null}, e.g. if method filter was never set
	 * @return if not {@code null}, the method filter used by this configuration
	 */
	public ObjSensZeroXCFABuilder.MethodFilter getMethodFilter() {
		return methodFilter;
	}
	
	/**
	 * Set the method filter used by the SDG builder to distinguish methods by object instance.
	 * Only used if points-to precision is set to OBJECT_SENSITIVE
	 */
	public void setMethodFilter(ObjSensZeroXCFABuilder.MethodFilter methodFilter) {
		this.methodFilter = methodFilter;
	}
	
	/**
	 * Returns the name of the file which WALA will use to resolve calls to native methods.
	 * @return the name of the file which WALA will use to resolve calls to native methods
	 */
	public String getNativesXML() {
		return this.nativesXML;
	}
	
	/**
	 * Sets the name of the file WALA will use to resolve calls to native methods. Only call this method if you are absolutely sure
	 * what you are doing!
	 * @param nativesXML name of the XML file WALA will use to resolve calls to native methods
	 */
	public void setNativesXML(String nativesXML) {
		this.nativesXML = nativesXML;
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

	public boolean getIgnoreIndirectFlows() {
		return ignoreIndirectFlows;
	}

	public void setIgnoreIndirectFlows(boolean newValue) {
		this.ignoreIndirectFlows = newValue;
	}

	/**
	 * @param exceptionAnalysis the exceptionAnalysis to set
	 */
	public void setExceptionAnalysis(ExceptionAnalysis exceptionAnalysis) {
		this.exceptionAnalysis = exceptionAnalysis;
	}

	public MethodState getDefaultExceptionMethodState() {
		return defaultExceptionMethodState;
	}

	public void setDefaultExceptionMethodState(final MethodState defaultExceptionMethodState) {
		this.defaultExceptionMethodState = defaultExceptionMethodState;
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

	/**
	 * @param sideEffects the configuration of the side-effect detector that should be used. Set to null if no
	 * side-effect detector is needed.
	 */
	public void setSideEffectDetectorConfig(final SideEffectDetectorConfig sideEffects) {
		this.sideEffects = sideEffects;
	}
	
	/**
	 * @return the configuration of the side-effect detector.
	 */
	public SideEffectDetectorConfig getSideEffectDetectorConfig() {
		return sideEffects;
	}
	
	public void setPruningPolicy(PruningPolicy policy) {
		this.pruningPolicy = policy;
	}
	
	public PruningPolicy getPruningPolicy() {
		return this.pruningPolicy;
	}

	public String getThirdPartyLibsPath() {
		return thirdPartyLibsPath;
	}

	public void setThirdPartyLibsPath(String thirdPartyLibsPath) {
		this.thirdPartyLibsPath = thirdPartyLibsPath;
	}

	public boolean computeAllocationSites() {
		return computeAllocationSites;
	}

	public void setComputeAllocationSites(boolean computeAllocationSites) {
		this.computeAllocationSites = computeAllocationSites;
	}

	public CGConsumer getCGConsumer() {
		return cgConsumer;
	}

	public void setCGConsumer(CGConsumer cgConsumer) {
		this.cgConsumer = cgConsumer;
	}

	public ContextSelector getContextSelector() {
		return ctxSelector;
	}

	public void setContextSelector(ContextSelector ctxSelector) {
		this.ctxSelector = ctxSelector;
	}

	/**
	 * @return the notifier
	 */
	public ConstructionNotifier getNotifier() {
		return notifier;
	}

	/**
	 * @param notifier the notifier to set
	 */
	public void setNotifier(ConstructionNotifier notifier) {
		this.notifier = notifier;
	}
}
