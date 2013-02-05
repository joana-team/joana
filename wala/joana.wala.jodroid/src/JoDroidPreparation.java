import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.scandroid.util.AndroidAnalysisScope;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dalvik.classLoader.DexIRFactory;
import com.ibm.wala.dalvik.ipa.callgraph.impl.DexEntryPoint;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXCFABuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.wala.core.ExternalCallCheck;
import edu.kit.joana.wala.core.Main;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.Config;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.core.SDGBuilder.StaticInitializationTreatment;
import edu.kit.joana.wala.core.pointsto.WalaPointsToUtil;
import edu.kit.joana.wala.flowless.pointsto.AliasGraph;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo;
import edu.kit.joana.wala.util.PrettyWalaNames;

// TODO: Where the heck is String.equals()?!
public class JoDroidPreparation {

	public static SDG buildAndroidSDG(String androidLib, String classPath) throws IOException, ClassHierarchyException,
			IllegalArgumentException, CancelException, UnsoundGraphException {
		com.ibm.wala.ipa.callgraph.impl.Util.setNativeSpec(null);
		AnalysisScope scope = AndroidAnalysisScope.setUpAndroidAnalysisScope(androidLib, classPath);
		IClassHierarchy cha = ClassHierarchy.make(scope);
		Config scfg = new Config();
		scfg.out = System.out;
		scfg.scope = scope;
		scfg.cache = new AnalysisCache((IRFactory<IMethod>) new DexIRFactory());
		scfg.cha = cha;
		scfg.entry = findEntry(cha);
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
		AnalysisOptions options = makeAnalysisOptions(scfg.entry, scfg.scope, scfg.cha);
		SSAPropagationCallGraphBuilder cgb = (ZeroXCFABuilder) WalaPointsToUtil.makeContextFreeType(options,
				scfg.cache, scfg.cha, scfg.scope);
		CallGraph walaCG = cgb.makeCallGraph(options);
		SDGBuilder builder = SDGBuilder.create(scfg, walaCG, cgb.getPointerAnalysis());

		return SDGBuilder.convertToJoana(System.out, builder, new NullProgressMonitor());
	}

	private static IMethod findMethod(IClassHierarchy cha, String mSig) {
		for (IClass c : cha) {
			for (IMethod m : c.getAllMethods()) {
				if (m.getSignature().toString().equals(mSig)) {
					return m;
				}
			}
		}

		return null;
	}

	private static IMethod findEntry(IClassHierarchy cha) {
		for (IClass cl : cha) {
			for (IMethod m : cl.getAllMethods()) {
				if (m.getReference().toString().contains("selectColumnAndRow(II)V")) {
					return m;
				}
			}
		}

		throw new IllegalStateException();
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

	private static AnalysisOptions makeAnalysisOptions(IMethod entry, AnalysisScope scope, IClassHierarchy cha) {
		List<Entrypoint> entrypoints = new LinkedList<Entrypoint>();
		entrypoints.add(new DexEntryPoint(entry, cha));
		AnalysisOptions analysisOptions = new AnalysisOptions(scope, entrypoints);
		analysisOptions.setReflectionOptions(ReflectionOptions.NO_STRING_CONSTANTS);
		return analysisOptions;
	}

	public static void main(String[] args) throws ClassHierarchyException, IllegalArgumentException, IOException,
			CancelException, UnsoundGraphException {
		SDG sdg = buildAndroidSDG("lib/android/android-2.3.7_r1.jar", "PROJECT_CUBE.apk");
		SDGSerializer.toPDGFormat(sdg, new FileOutputStream("ProjectCube.pdg"));
	}
}
