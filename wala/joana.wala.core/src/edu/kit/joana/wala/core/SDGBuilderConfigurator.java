/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.

 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import java.io.PrintStream;

import com.ibm.wala.cfg.exc.intra.MethodState;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.pruned.PruningPolicy;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.DefaultIRFactory;

import edu.kit.joana.util.ReflectiveWatchdog;
import edu.kit.joana.wala.core.CGConsumer;
import edu.kit.joana.wala.core.ExternalCallCheck;
import edu.kit.joana.wala.core.ParameterPointsToConsumer;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.ControlDependenceVariant;
import edu.kit.joana.wala.core.SDGBuilder.DynamicDispatchHandling;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.core.SDGBuilder.SDGBuilderConfig;
import edu.kit.joana.wala.core.SDGBuilder.StaticInitializationTreatment;
import edu.kit.joana.wala.core.params.objgraph.SideEffectDetector;
import edu.kit.joana.wala.util.WALAUtils;
import edu.kit.joana.wala.util.pointsto.ObjSensZeroXCFABuilder;

/**
 * @author Martin Mohr &lt;martin.mohr@kit.edu&gt;
 */
public abstract class SDGBuilderConfigurator<C extends SDGBuilderConfigurator<C>> {
	protected ReflectiveWatchdog<SDGBuilder.SDGBuilderConfig> rwd;
	protected AnalysisScope scope;
	protected IClassHierarchy cha;
	public SDGBuilderConfigurator() {
		SDGBuilderConfig scfg = new SDGBuilder.SDGBuilderConfig();
		this.rwd = new ReflectiveWatchdog<SDGBuilder.SDGBuilderConfig>(scfg);
		rwd.set("options", null); // field 'options' will be set later by SDGBuilder -- here we ensure that it cannot be set by user 
	}
	public abstract C thisActually();
	public C setScopeAndCHA(AnalysisScope scope, IClassHierarchy cha) {
		rwd.set("scope", scope);
		rwd.set("cha", cha);
		this.scope = scope;
		this.cha = cha;
		return thisActually();
	}
	public C configureForJavaBytecode(String mainClass) {
		rwd.set("entry", WALAUtils.findMainMethod(cha, mainClass));
		rwd.set("additionalContextSelector", null);
		rwd.set("additionalContextInterpreter", null);
		rwd.set("cache", new AnalysisCacheImpl());
		return thisActually();
	}
	public C setPruningPolicy(PruningPolicy policy, int depth) {
		rwd.set("pruningPolicy", policy);
		rwd.set("prunecg", depth);
		return thisActually();
	}
	public C standardImmutableSettings() {
		return setImmutableNoOut(SDGBuilder.IMMUTABLE_NO_OUT)
		      .setImmutableStubs(SDGBuilder.IMMUTABLE_STUBS)
		      .setIgnoreStaticFields(SDGBuilder.IMMUTABLE_STUBS);
	}
	public C standardDebugSettings() {
		return setDebugManyGraphsDotOutput(false)
	          .setDebugAccessPath(false)
	          .setDebugAccessPathOutputDir(null)
	          .setDebugCallGraphDotOutput(false)
	          .setDebugStaticInitializers(false);
	}
	public C standardsForNotSoImportantItems() {
		return setOut(System.out)
		      .setExt(ExternalCallCheck.EMPTY)
		      .standardImmutableSettings()
		      .standardDebugSettings()
		      .setPruneDDEdgesToDanglingExceptionNodes(true)
		      .setDefaultExceptionMethodState(MethodState.DEFAULT)
		      .setAccessPath(false)
		      .setStaticInitializers(StaticInitializationTreatment.SIMPLE)
		      .setAbortAfterCG(false)
		      .setAdditionalNativeSpec(null)
		      .setLocalKillingDefs(true)
		      .setKeepPhiNodes(true)
		      .setMergeFieldsOfPrunedCalls(true)
		      .setNoBasePointerDependency(true)
		      .setAssociateLocalNames(false)
		      .setShowTypeNameInValue(false)
		      .setMethodTargetSelector(null)
		      .setSideEffects(null)
		      .setDoParallel(false);
	}
	public C forSequentialPrograms() {
		return setComputeInterference(false)
		      .setComputeAllocationSites(false);
	}
	public C forMultithreadedPrograms() {
		return setComputeInterference(true)
		      .setComputeAllocationSites(true);
	}
	public C usefulStandardsForImportantItems() {
		return forSequentialPrograms()
		      .setFieldPropagation(FieldPropagation.OBJ_GRAPH_NO_MERGE_AT_ALL)
		      .setDynDisp(DynamicDispatchHandling.PRECISE)
		      .setComputeSummary(true)
		      .setControlDependenceVariant(SDGBuilder.defaultControlDependenceVariant);
	}
	public C setOut(PrintStream out) {
		rwd.set("out", out);
		return thisActually();
	}
	public C setExt(ExternalCallCheck ext) {
		rwd.set("ext", ext);
		return thisActually();
	}
	public C setImmutableNoOut(String[] immutableNoOut) {
		rwd.set("immutableNoOut", immutableNoOut);
		return thisActually();
	}
	public C setImmutableStubs(String[] immutableStubs) {
		rwd.set("immutableStubs", immutableStubs);
		return thisActually();
	}
	public C setIgnoreStaticFields(String[] ignoreStaticFields) {
		rwd.set("ignoreStaticFields", ignoreStaticFields);
		return thisActually();
	}
	public C setExceptions(ExceptionAnalysis exc) {
		rwd.set("exceptions", exc);
		return thisActually();
	}
	public C setPruneDDEdgesToDanglingExceptionNodes(boolean setting) {
		rwd.set("pruneDDEdgesToDanglingExceptionNodes", setting);
		return thisActually();
	}
	public C setDefaultExceptionMethodState(MethodState state) {
		rwd.set("defaultExceptionMethodState", state);
		return thisActually();
	}
	public C setAccessPath(boolean setting) {
		rwd.set("accessPath", setting);
		return thisActually();
	}
	public C setSideEffects(SideEffectDetector detector) {
		rwd.set("sideEffects", detector);
		return thisActually();
	}
	public C setStaticInitializers(StaticInitializationTreatment treatment) {
		rwd.set("staticInitializers", treatment);
		return thisActually();
	}
	public C setFieldPropagation(FieldPropagation fieldProp) {
		rwd.set("fieldPropagation", fieldProp);
		return thisActually();
	}
	public C setDebugManyGraphsDotOutput(boolean setting) {
		rwd.set("debugManyGraphsDotOutput", setting);
		return thisActually();
	}
	public C setComputeInterference(boolean setting) {
		rwd.set("computeInterference", setting);
		return thisActually();
	}
	public C setComputeAllocationSites(boolean setting) {
		rwd.set("computeAllocationSites", setting);
		return thisActually();
	}
	public C setDynDisp(DynamicDispatchHandling handling) {
		rwd.set("dynDisp", handling);
		return thisActually();
	}
	public C setComputeSummary(boolean setting) {
		rwd.set("computeSummary", setting);
		return thisActually();
	}
	public C setAbortAfterCG(boolean setting) {
		rwd.set("abortAfterCG", setting);
		return thisActually();
	}
	public C setAdditionalNativeSpec(String additionalNativeSpec) {
		rwd.set("additionalNativeSpec", additionalNativeSpec);
		return thisActually();
	}
	public C setLocalKillingDefs(boolean setting) {
		rwd.set("localKillingDefs", setting);
		return thisActually();
	}
	public C setKeepPhiNodes(boolean setting) {
		rwd.set("keepPhiNodes", setting);
		return thisActually();
	}
	public C setMergeFieldsOfPrunedCalls(boolean setting) {
		rwd.set("mergeFieldsOfPrunedCalls", setting);
		return thisActually();
	}
	public C setNoBasePointerDependency(boolean setting) {
		rwd.set("noBasePointerDependency", setting);
		return thisActually();
	}
	public C setDebugAccessPath(boolean setting) {
		rwd.set("debugAccessPath", setting);
		return thisActually();
	}
	public C setDebugAccessPathOutputDir(String dir) {
		rwd.set("debugAccessPathOutputDir", dir);
		return thisActually();
	}
	public C setDebugCallGraphDotOutput(boolean setting) {
		rwd.set("debugCallGraphDotOutput", setting);
		return thisActually();
	}
	public C setDebugStaticInitializers(boolean setting) {
		rwd.set("debugStaticInitializers", setting);
		return thisActually();
	}
	public C setAssociateLocalNames(boolean setting) {
		rwd.set("associateLocalNames", setting);
		return thisActually();
	}
	public C setShowTypeNameInValue(boolean setting) {
		rwd.set("showTypeNameInValue", setting);
		return thisActually();
	}
	public C setMethodTargetSelector(MethodTargetSelector mts) {
		rwd.set("methodTargetSelector", mts);
		return thisActually();
	}
	public C setControlDependenceVariant(ControlDependenceVariant cdv) {
		rwd.set("controlDependenceVariant", cdv);
		return thisActually();
	}
	public C setDoParallel(boolean setting) {
		rwd.set("doParallel", setting);
		return thisActually();
	}
	public <D extends SDGBuilderConfigurator<D>> D switchTo(D target) {
		target.rwd = rwd;
		return target.thisActually();
	}
	public C withPointsToPrecision(PointsToPrecision ptsPrec) {
		rwd.set("pts", ptsPrec);
		if (ptsPrec != PointsToPrecision.CUSTOM) {
			rwd.set("customCGBFactory", null);
		}
		return thisActually();
	}

	public C installObjSensFilter(ObjSensZeroXCFABuilder.MethodFilter filter) {
		rwd.set("objSensFilter", filter);
		return thisActually();
	}
	public C noCallGraphHook() {
		rwd.set("cgConsumer", null);
		return thisActually();
	}
	public C installCallGraphHook(CGConsumer hook) {
		rwd.set("cgConsumer", hook);
		return thisActually();
	}

	public C installParameterPTSConsumer(ParameterPointsToConsumer consumer) {
		rwd.set("parameterPTSConsumer", consumer);
		return thisActually();
	}
	public C noParameterPTSConsumer() {
		rwd.set("parameterPTSConsumer", null);
		return thisActually();
	}

	public C restStaysAsIs() {
		rwd.leaveRestAsItIs();
		return thisActually();
	}
	public SDGBuilder.SDGBuilderConfig getResult() {
		return rwd.getResult();
	}
	public static Impl start() {
		return new Impl();
	}
	public static class Impl extends SDGBuilderConfigurator<Impl> {
		@Override
		public Impl thisActually() {
			return this;
		}
	}
}
