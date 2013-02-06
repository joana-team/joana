import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dalvik.classLoader.DexIRFactory;
import com.ibm.wala.dalvik.ipa.callgraph.impl.DexEntryPoint;
import com.ibm.wala.dalvik.util.AndroidAnalysisScope;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
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
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
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

// TODO: Where the heck is String.equals()?!
public class JoDroidPreparation {

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

	private static final String JODROID_INVOCATION_NAME = "java -jar jodroid.jar";
	private static final String PARAMETERS = "<classpath> <android lib> <entry method> <sdgfile>";

	private static void printUsage() {
		System.out.println("Usage: " + JODROID_INVOCATION_NAME + " " + PARAMETERS + " where ");
		System.out.println("<classpath>:    specifies .apk file to analyze.");
		System.out
				.println("<android lib>:  specifies the path to the .jar or .dex which contains the android library.");
		System.out
				.println("<entry method>: specifies the method at which the environment enters the app under analysis. Note that the name of the method must be fully qualified");
		System.out.println("                and its signature has to be given in bytecode notation.");
		System.out.println("                Examples: ");
		System.out.println("                \tcom.foo.bar.AClass.main([Ljava/lang/String;)V");
		System.out.println("                \tcom.foo.bar.AClass.addTwoInts(II)I");
		System.out.println("                \tcom.foo.bar.AClass$AnInnerClass.isTroodles()Z");
		System.out
				.println("<sdgfile>:      specifies the path to the file in which the resulting SDG is to be written");

	}

	public static void main(String[] args) throws ClassHierarchyException, IllegalArgumentException, IOException,
			CancelException, UnsoundGraphException {
		if (args.length != 4) {
			printUsage();
		} else {
			String classPath = args[0];
			String androidLib = args[1];
			String entryMethod = args[2];
			String sdgFile = args[3];
			try {
				buildAndroidSDG(classPath, androidLib, entryMethod, sdgFile);
			} catch (MethodNotFoundException m) {
				System.out.println("Entry method not found: " + m.getMessage());
				return;
			}
		}
	}

	private static void buildAndroidSDG(String classPath, String androidLib, String entryMethod, String sdgFile)
			throws IOException, ClassHierarchyException, IllegalArgumentException, CancelException,
			UnsoundGraphException, MethodNotFoundException {
		com.ibm.wala.ipa.callgraph.impl.Util.setNativeSpec(null);
		AnalysisScope scope = AndroidAnalysisScope.setUpAndroidAnalysisScope(androidLib, classPath);
		IClassHierarchy cha = ClassHierarchy.make(scope);
		Config scfg = new Config();
		scfg.out = System.out;
		scfg.scope = scope;
		scfg.cache = new AnalysisCache((IRFactory<IMethod>) new DexIRFactory());
		scfg.cha = cha;
		scfg.entry = findMethod(cha, entryMethod);
		scfg.ext = makeStandardExternalCallCheck();
		scfg.immutableNoOut = Main.IMMUTABLE_NO_OUT;
		scfg.immutableStubs = Main.IMMUTABLE_STUBS;
		scfg.ignoreStaticFields = Main.IGNORE_STATIC_FIELDS;
		scfg.exceptions = ExceptionAnalysis.ALL_NO_ANALYSIS;
		scfg.accessPath = false;
		scfg.prunecg = Main.DEFAULT_PRUNE_CG;
		scfg.pts = PointsToPrecision.TYPE;
		scfg.staticInitializers = StaticInitializationTreatment.SIMPLE;
		scfg.fieldPropagation = FieldPropagation.OBJ_GRAPH;
		scfg.debugManyGraphsDotOutput = false;
		scfg.computeInterference = false;
		AnalysisOptions options = makeAnalysisOptions(scfg.entry, scfg.scope, scfg.cha);
		SSAPropagationCallGraphBuilder cgb = (ZeroXCFABuilder) WalaPointsToUtil.makeContextFreeType(options,
				scfg.cache, scfg.cha, scfg.scope);
		CallGraph walaCG = cgb.makeCallGraph(options);
		SDGBuilder builder = SDGBuilder.create(scfg, walaCG, cgb.getPointerAnalysis());
		SDG sdg = SDGBuilder.convertToJoana(System.out, builder, new NullProgressMonitor());
		SDGSerializer.toPDGFormat(sdg, new FileOutputStream(sdgFile));
	}

	private static IMethod findMethod(IClassHierarchy cha, String mSig) throws MethodNotFoundException {
		System.out.println("trying to resolve method fast...");
		int off = mSig.lastIndexOf('.');
		String type = "L" + mSig.substring(0, off).replace('.', '/');
		String mSel = mSig.substring(off + 1);
		TypeReference tRef = TypeReference.findOrCreate(ClassLoaderReference.Application, type);
		MethodReference mRef = MethodReference.findOrCreate(tRef, Selector.make(mSel));
		IMethod cand = cha.resolveMethod(mRef);
		if (cand != null) {
			return cand;
		} else {
			System.out.println("failed. Trying slow approach...");
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

}
