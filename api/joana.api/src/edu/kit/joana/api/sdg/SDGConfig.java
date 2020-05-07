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
import com.ibm.wala.ipa.callgraph.InterfaceImplementationOptions;
import com.ibm.wala.ipa.callgraph.UninitializedFieldHelperOptions;
import com.ibm.wala.ipa.callgraph.pruned.ApplicationLoaderPolicy;
import com.ibm.wala.ipa.callgraph.pruned.DoNotPrune;
import com.ibm.wala.ipa.callgraph.pruned.PruningPolicy;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.util.LogUtil;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.CGConsumer;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.*;
import edu.kit.joana.wala.core.ThreadAwareApplicationLoaderPolicy;
import edu.kit.joana.wala.core.params.objgraph.SideEffectDetectorConfig;
import edu.kit.joana.wala.summary.SummaryComputationType;
import edu.kit.joana.wala.util.pointsto.ObjSensZeroXCFABuilder;

import java.util.Collection;

public class SDGConfig {
  private PruningPolicy pruningPolicy = ApplicationLoaderPolicy.INSTANCE;
	private String classPath;
	private boolean classpathAddEntriesFromMANIFEST;
	private String thirdPartyLibsPath;
	private String entryMethod;
	private Stubs stubs;
	private String exclusions = SDGBuilder.STD_EXCLUSION_REG_EXP;
	private ExceptionAnalysis exceptionAnalysis;
	private boolean ignoreIndirectFlows = false;
	private MethodState defaultExceptionMethodState = null;
	private FieldPropagation fieldPropagation;
	private PointsToPrecision pointsToPrecision;
	private boolean computeAccessPaths;
	private boolean localKillingDefs = true;
	private boolean computeInterferences;
	private MHPType mhpType = MHPType.NONE;
	private SideEffectDetectorConfig sideEffects;
	private ObjSensZeroXCFABuilder.MethodFilter methodFilter;
	private boolean computeAllocationSites = false;
	private CGConsumer cgConsumer = null;
	private ContextSelector ctxSelector;
	private ConstructionNotifier notifier = null;
	private DynamicDispatchHandling ddisp = DynamicDispatchHandling.SIMPLE;
	private boolean computeSummaryEdges = true;
	private SummaryComputationType summaryComputationType = SummaryComputationType.DEFAULT;
	private boolean skipSDGProgramPart = false;
	private ControlDependenceVariant controlDependenceVariant = SDGBuilder.defaultControlDependenceVariant;
	private boolean isParallel = true;
	private UninitializedFieldHelperOptions fieldHelperOptions = UninitializedFieldHelperOptions.createEmpty();
	private InterfaceImplementationOptions interfaceImplOptions = InterfaceImplementationOptions.createEmpty();
	private Collection<String> additionalEntryMethods;
	private boolean annotateOverloadingMethods;

	public SDGConfig(String classPath, String entryMethod, Stubs stubsPath) {
		this(classPath, true, entryMethod, stubsPath, ExceptionAnalysis.INTERPROC, FieldPropagation.OBJ_GRAPH, PointsToPrecision.INSTANCE_BASED, false, false, MHPType.NONE);
	}

	public SDGConfig(String classPath, boolean classpathAddEntriesFromMANIFEST, String entryMethod, Stubs stubsPath, ExceptionAnalysis exceptionAnalysis, FieldPropagation fieldPropagation,
			PointsToPrecision pointsToPrecision, boolean computeAccessPaths, boolean computeInterferences, MHPType mhpType) {
		this.classPath = classPath;
		this.classpathAddEntriesFromMANIFEST = classpathAddEntriesFromMANIFEST;
		this.entryMethod = entryMethod;
		this.stubs = stubsPath;
		this.exceptionAnalysis = exceptionAnalysis;
		this.fieldPropagation = fieldPropagation;
		this.pointsToPrecision = pointsToPrecision;
		this.computeAccessPaths = computeAccessPaths;
		this.computeInterferences = computeInterferences;
		this.mhpType = mhpType;
	}
	
	public void setComputeSummaryEdges(final boolean value) {
		this.computeSummaryEdges = value;
	}
	
	public boolean isComputeSummaryEdges() {
		return this.computeSummaryEdges;
	}
	
	public void setSummaryComputationType(SummaryComputationType summaryComputationType) {
		this.summaryComputationType = summaryComputationType;
	}
	
	public SummaryComputationType getSummaryComputationType() {
		return summaryComputationType;
	}

	public void setSkipSDGProgramPart(final boolean value) {
		this.skipSDGProgramPart = value;
	}
	
	public boolean isSkipSDGProgramPart() {
		return this.skipSDGProgramPart;
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
	 * @return whether "Class-Path:" entries in MANIFEST.MF files shall be recursively added to the classPath 
	 */
	public boolean getClasspathAddEntriesFromMANIFEST() {
		return classpathAddEntriesFromMANIFEST;
	}
	
	/**
	 * @param classpathAddEntriesFromMANIFEST whether "Class-Path:" entries in MANIFEST.MF files shall be recursively added to the classPath
	 */
	public void setClasspathAddEntriesFromMANIFEST(boolean classpathAddEntriesFromMANIFEST) {
		this.classpathAddEntriesFromMANIFEST = classpathAddEntriesFromMANIFEST;
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
	public Stubs getStubs() {
		return stubs;
	}

	/**
	 * @param stubsPath the stubsPath to set
	 */
	public void setStubs(Stubs stubs) {
		this.stubs = stubs;
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
	 * @return the localKillingDefs
	 */
	public boolean localKillingDefs() {
		return localKillingDefs;
	}
	
	/**
	 * @param localKillingDefs the localKillingDefs to set
	 */
	public void setLocalKillingDefs(boolean localKillingDefs) {
		this.localKillingDefs = localKillingDefs;
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

	static PruningPolicy convertPruningPolicy(edu.kit.joana.ui.annotations.PruningPolicy policy){
		switch (policy) {
		case APPLICATION:
			return ApplicationLoaderPolicy.INSTANCE;
		case THREAD_AWARE:
			return ThreadAwareApplicationLoaderPolicy.INSTANCE;
		case DO_NOT_PRUNE:
			return DoNotPrune.INSTANCE;
		}
		throw new IllegalArgumentException(policy.toString());
	}

	public void setPruningPolicy(edu.kit.joana.ui.annotations.PruningPolicy policy) {
		this.pruningPolicy = convertPruningPolicy(policy);
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

	public DynamicDispatchHandling getDynamicDispatchHandling() {
		return ddisp;
	}

	public void setDynamicDispatchHandling(DynamicDispatchHandling ddisp) {
		this.ddisp = ddisp;
	}
	
	public String toString() {
		return LogUtil.attributesToString(this);
	}

	public ControlDependenceVariant getControlDependenceVariant() {
		return controlDependenceVariant;
	}

	public void setControlDependenceVariant(ControlDependenceVariant controlDependenceVariant) {
		this.controlDependenceVariant = controlDependenceVariant;
	}

	public boolean isParallel() {
		return isParallel;
	}

	public void setParallel(boolean isParallel) {
		this.isParallel = isParallel;
	}

	public UninitializedFieldHelperOptions getFieldHelperOptions() {
		return fieldHelperOptions;
	}

	public void setFieldHelperOptions(UninitializedFieldHelperOptions fieldHelperOptions) {
		this.fieldHelperOptions = fieldHelperOptions;
	}

	public Collection<String> getAdditionalEntryMethods() {
		return additionalEntryMethods;
	}

	public void setAdditionalEntryMethods(Collection<String> additionalEntryMethods) {
		this.additionalEntryMethods = additionalEntryMethods;
	}

	public InterfaceImplementationOptions getInterfaceImplOptions() {
		return interfaceImplOptions;
	}

	public void setInterfaceImplOptions(InterfaceImplementationOptions interfaceImplOptions) {
		this.interfaceImplOptions = interfaceImplOptions;
	}

	public boolean isAnnotatingOverloadingMethods() {
		return annotateOverloadingMethods;
	}

	public void setAnnotateOverloadingMethods(boolean annotateOverloadingMethods) {
		this.annotateOverloadingMethods = annotateOverloadingMethods;
	}
}
