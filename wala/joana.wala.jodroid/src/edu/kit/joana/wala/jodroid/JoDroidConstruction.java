package edu.kit.joana.wala.jodroid;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarFile;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dalvik.classLoader.DexIRFactory;
import com.ibm.wala.dalvik.util.AndroidAnalysisScope;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.util.Maybe;
import edu.kit.joana.wala.core.ExternalCallCheck;
import edu.kit.joana.wala.core.Main;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.core.SDGBuilder.SDGBuilderConfig;
import edu.kit.joana.wala.core.SDGBuilder.StaticInitializationTreatment;
import edu.kit.joana.wala.flowless.pointsto.AliasGraph;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo;

public final class JoDroidConstruction {

	public static void buildAndroidSDGAndSave(String classPath, String androidLib, String entryMethod, String sdgFile)
			throws SDGConstructionException, IOException {
		SDG sdg = buildAndroidSDG(classPath, androidLib, Maybe.<String>nothing(), entryMethod);
		SDGSerializer.toPDGFormat(sdg, new FileOutputStream(sdgFile));
	}

	public static void buildAndroidSDGAndSave(String classPath, String androidLib, String stubsPath, String entryMethod, String sdgFile)
			throws SDGConstructionException, IOException {
		SDG sdg = buildAndroidSDG(classPath, androidLib, Maybe.just(stubsPath), entryMethod);
		SDGSerializer.toPDGFormat(sdg, new FileOutputStream(sdgFile));
	}

	public static IClassHierarchy computeCH(String classPath, String androidLib) throws IOException,
			ClassHierarchyException {
		AnalysisScope scope = AndroidAnalysisScope.setUpAndroidAnalysisScope(androidLib, classPath);
		return ClassHierarchy.make(scope);
	}

	public static SDG buildAndroidSDG(String classPath, String androidLib, Maybe<String> stubsPath, String entryMethod)
			throws SDGConstructionException, IOException {
		AnalysisScope scope = AndroidAnalysisScope.setUpAndroidAnalysisScope(androidLib, classPath);
		if (stubsPath.isJust()) {
			String stubs = stubsPath.extract();
			scope.addToScope(ClassLoaderReference.Primordial, new JarFile(stubs));
		}
		IClassHierarchy cha;
		try {
			cha = ClassHierarchy.make(scope);
			return buildAndroidSDG(findMethod(cha, entryMethod));
		} catch (ClassHierarchyException e) {
			throw new SDGConstructionException(e);
		} catch (MethodNotFoundException e) {
			throw new SDGConstructionException(e);
		}
	}

	public static SDG buildAndroidSDG(String classPath, String androidLib, String entryMethod)
			throws SDGConstructionException, IOException {
		return buildAndroidSDG(classPath, androidLib, Maybe.<String>nothing(), entryMethod);
	}

	public static SDG buildAndroidSDG(IMethod entryMethod)
			throws SDGConstructionException, IOException {
		// com.ibm.wala.ipa.callgraph.impl.Util.setNativeSpec(null);
		AnalysisScope scope = entryMethod.getClassHierarchy().getScope();
		IClassHierarchy cha = entryMethod.getClassHierarchy();
		SDGBuilderConfig scfg = new SDGBuilderConfig();
		scfg.out = System.out;
		scfg.scope = scope;
		scfg.cache = new AnalysisCache((IRFactory<IMethod>) new DexIRFactory());
		scfg.cha = cha;
		scfg.entry = entryMethod;
		scfg.ext = makeStandardExternalCallCheck();
		scfg.immutableNoOut = Main.IMMUTABLE_NO_OUT;
		scfg.immutableStubs = Main.IMMUTABLE_STUBS;
		scfg.ignoreStaticFields = Main.IGNORE_STATIC_FIELDS;
		scfg.exceptions = ExceptionAnalysis.ALL_NO_ANALYSIS;
		scfg.accessPath = false;
		scfg.prunecg = Main.DEFAULT_PRUNE_CG;
		scfg.pts = PointsToPrecision.CONTEXT_SENSITIVE;
		scfg.staticInitializers = StaticInitializationTreatment.SIMPLE;
		scfg.fieldPropagation = FieldPropagation.OBJ_GRAPH;
		scfg.debugManyGraphsDotOutput = false;
		scfg.computeInterference = false;

		try {
			return SDGBuilder.build(scfg);
		} catch (UnsoundGraphException e) {
			throw new SDGConstructionException(e);
		} catch (CancelException e) {
			throw new SDGConstructionException(e);
		}
	}

	private static IMethod findMethod(IClassHierarchy cha, String mSig) throws MethodNotFoundException {
		int off = mSig.lastIndexOf('.');
		String type = "L" + mSig.substring(0, off).replace('.', '/');
		String mSel = mSig.substring(off + 1);
		TypeReference tRef = TypeReference.findOrCreate(ClassLoaderReference.Application, type);
		MethodReference mRef = MethodReference.findOrCreate(tRef, Selector.make(mSel));
		IMethod cand = cha.resolveMethod(mRef);
		if (cand != null) {
			return cand;
		} else {
			for (IClass c : cha) {
				for (IMethod m : c.getAllMethods()) {
					if (m.getSignature().equals(mSig)) {
						return m;
					}
				}
			}

			throw new MethodNotFoundException(mSig);
		}
	}

	private static ExternalCallCheck makeStandardExternalCallCheck() {
		return new ExternalCallCheck() {
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
	}
}
