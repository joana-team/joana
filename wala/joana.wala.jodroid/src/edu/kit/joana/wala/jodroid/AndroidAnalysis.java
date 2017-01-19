package edu.kit.joana.wala.jodroid;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.jar.JarFile;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.dalvik.classLoader.DexFileModule;
import com.ibm.wala.dalvik.classLoader.DexIRFactory;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.AndroidModel;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.DefaultInstantiationBehavior;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.structure.LoopKillAndroidModel;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint.ExecutionOrder;
import com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentContextInterpreter;
import com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentContextSelector;
import com.ibm.wala.dalvik.ipa.callgraph.pruned.PruneAndroidSupport;
import com.ibm.wala.dalvik.util.AndroidEntryPointLocator;
import com.ibm.wala.dalvik.util.AndroidEntryPointLocator.LocatorFlags;
import com.ibm.wala.dalvik.util.AndroidEntryPointManager;
import com.ibm.wala.dalvik.util.AndroidManifestXMLReader;
import com.ibm.wala.dalvik.util.AndroidPreFlightChecks;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.pruned.ApplicationLoaderPolicy;
import com.ibm.wala.ipa.callgraph.pruned.ConjunctivePruningPolicy;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.FileOfClasses;

import edu.kit.joana.util.io.IOFactory;
import edu.kit.joana.wala.core.CGConsumer;
import edu.kit.joana.wala.core.ExternalCallCheck;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.core.SDGBuilder.StaticInitializationTreatment;
import edu.kit.joana.wala.flowless.pointsto.AliasGraph;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo;
import edu.kit.joana.wala.jodroid.io.AppSpec;

public class AndroidAnalysis {
	public final static String STD_EXCLUSION_REG_EXP =
			"sun\\/awt\\/.*\n"
			+ "sun\\/swing\\/.*\n"
			+ "com\\/sun\\/.*\n"
			+ "sun\\/.*\n"
			+ "apple\\/awt\\/.*\n"
			+ "com\\/apple\\/.*\n"
			+ "org\\/omg\\/.*\n";
	private static TypeReference JavaLangRunnable = TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/Runnable");
	private static Selector run = Selector.make("run()V");
	public static class CallGraphKeeper implements CGConsumer {
		private CallGraph callGraph;

		@Override
		public void consume(CallGraph cg, PointerAnalysis<? extends InstanceKey> pts) {
			this.callGraph = cg;
		}

		public CallGraph getCallGraph() {
			return callGraph;
		}
	}
	public SDGBuilder.SDGBuilderConfig makeSDGBuilderConfig(AppSpec appSpec, AnalysisScope scope, IClassHierarchy cha, CGConsumer consumer, boolean silent, boolean onlyCG) throws ClassHierarchyException, IOException, CancelException {
		AnalysisCache cache = new AnalysisCache(new DexIRFactory());
		AnalysisOptions options = configureOptions(scope, cha);
		populateEntryPoints(cha);
		if (appSpec.manifestFile != null) {
			new AndroidManifestXMLReader(appSpec.manifestFile);
		}
		IMethod lifecycle;
		new AndroidPreFlightChecks(AndroidEntryPointManager.MANAGER, options, cha).all();
		AndroidEntryPointManager.MANAGER.setModelBehavior(LoopKillAndroidModel.class);
		final AndroidModel modeller = new AndroidModel(cha, options, cache);
		lifecycle = modeller.getMethodEncap();
		final SDGBuilder.SDGBuilderConfig scfg = new SDGBuilder.SDGBuilderConfig();
		scfg.scope = scope;
		scfg.cache = cache;
		scfg.cha = cha;
		scfg.entry = lifecycle;
		scfg.ext = new ExternalCallCheck() {
			@Override
			public boolean isCallToModule(SSAInvokeInstruction invk) {
				return false;
			}

			@Override
			public void registerAliasContext(SSAInvokeInstruction invk, int callNodeId, AliasGraph.MayAliasGraph context) {
			}

			@Override
			public void setClassHierarchy(IClassHierarchy cha) {
			}

			@Override
			public MethodInfo checkForModuleMethod(IMethod im) {
				return null;
			}

			@Override
			public boolean resolveReflection() {
				return false;
			}
		};
		scfg.out = silent?new PrintStream(new ByteArrayOutputStream()):System.out;
		scfg.exceptions = ExceptionAnalysis.INTERPROC;
		scfg.prunecg = 1;
		scfg.pruningPolicy = new ConjunctivePruningPolicy(ApplicationLoaderPolicy.INSTANCE, PruneAndroidSupport.INSTANCE);
		scfg.pts = PointsToPrecision.INSTANCE_BASED;
		scfg.staticInitializers = StaticInitializationTreatment.SIMPLE;
		scfg.fieldPropagation = FieldPropagation.OBJ_GRAPH;
		scfg.computeInterference = false;
		scfg.computeAllocationSites = false;
		scfg.cgConsumer = consumer;
		scfg.additionalContextSelector = new IntentContextSelector(cha);
		scfg.additionalContextInterpreter = new IntentContextInterpreter(cha, options, cache);
		scfg.localKillingDefs = false;
		scfg.abortAfterCG = onlyCG;
		return scfg;
	}
	public SDGBuilder.SDGBuilderConfig makeSDGBuilderConfig(AppSpec appSpec, AnalysisScope scope, CGConsumer consumer, boolean silent, boolean onlyCG) throws ClassHierarchyException, IOException, CancelException {
		return makeSDGBuilderConfig(appSpec, scope, ClassHierarchy.make(scope), consumer, silent, onlyCG);
	}

	public static AnalysisScope makeMinimalScope(AppSpec appSpec, String pathToJDK, String pathToAndroidLib) throws IOException {
		AnalysisScope scope = AnalysisScope.createJavaAnalysisScope();
		scope.setLoaderImpl(ClassLoaderReference.Primordial, "com.ibm.wala.dalvik.classLoader.WDexClassLoaderImpl");
		scope.setLoaderImpl(ClassLoaderReference.Application, "com.ibm.wala.dalvik.classLoader.WDexClassLoaderImpl");
		scope.addToScope(ClassLoaderReference.Primordial, new JarFileModule(new JarFile(pathToJDK)));
		scope.addToScope(ClassLoaderReference.Primordial, new JarFileModule(new JarFile(pathToAndroidLib)));
		scope.addToScope(ClassLoaderReference.Application, DexFileModule.make(appSpec.apkFile));
		scope.setExclusions(new FileOfClasses(new ByteArrayInputStream(IOFactory.createUTF8Bytes(STD_EXCLUSION_REG_EXP))));
		return scope;
	}

	private AnalysisOptions configureOptions(AnalysisScope scope, IClassHierarchy cha) {
		AnalysisOptions options = new AnalysisOptions(scope, null);
		options.setReflectionOptions(ReflectionOptions.FULL);
		AndroidEntryPointManager.reset();
		AndroidEntryPointManager.MANAGER.setInstantiationBehavior(new DefaultInstantiationBehavior(cha));
		AndroidEntryPointManager.MANAGER.setDoBootSequence(false);
		Util.addDefaultSelectors(options, cha);
		Util.addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha);
		return options;
	}

	private void populateEntryPoints(IClassHierarchy cha) {
		Set<AndroidEntryPointLocator.LocatorFlags> entrypointLocatorFlags = EnumSet.noneOf(AndroidEntryPointLocator.LocatorFlags.class);
		entrypointLocatorFlags.add(LocatorFlags.INCLUDE_CALLBACKS);
		entrypointLocatorFlags.add(LocatorFlags.CB_HEURISTIC);
		final AndroidEntryPointLocator epl = new AndroidEntryPointLocator(entrypointLocatorFlags);
		AndroidEntryPointManager.ENTRIES = epl.getEntryPoints(cha);
		for (IClass cl : cha.getImplementors(AndroidAnalysis.JavaLangRunnable)) {
			if (cl.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
				IMethod runMethod = cl.getMethod(AndroidAnalysis.run);
				if (runMethod != null && !runMethod.getDeclaringClass().getName().toString().startsWith("Landroid/support")) {
					System.out.println("additional: " + runMethod + ", class: " + cl);
					AndroidEntryPointManager.ENTRIES.add(new AndroidEntryPoint(ExecutionOrder.MULTIPLE_TIMES_IN_LOOP, runMethod, cha));
				}
			}
		}
		Collections.sort(AndroidEntryPointManager.ENTRIES, new AndroidEntryPoint.ExecutionOrderComperator());
		AndroidEntryPointManager.ENTRIES = Collections.unmodifiableList(AndroidEntryPointManager.ENTRIES);
	}

}
