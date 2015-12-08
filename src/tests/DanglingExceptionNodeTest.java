package tests;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.jar.JarInputStream;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.cfg.exc.intra.MethodState;
import com.ibm.wala.classLoader.BinaryDirectoryTreeModule;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JarStreamModule;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.pruned.DoNotPrune;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.DefaultIRFactory;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstruction.Visitor;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.SDGBuildPreparation;
import edu.kit.joana.api.sdg.SDGInstruction;
import edu.kit.joana.api.sdg.SDGParameterUtils;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.wala.core.CGConsumer;
import edu.kit.joana.wala.core.ExternalCallCheck;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.DynamicDispatchHandling;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.core.SDGBuilder.StaticInitializationTreatment;


public class DanglingExceptionNodeTest {
	public static final String CLASS_PATH = "example-bin";
	public static final String MAIN_CLASS = "LBenignExceptionA";
	public static boolean DUMP_PDG = true;
	public static final String PDG_FILE_DANGLING = "benignException-dangling.pdg";
	public static final String PDG_FILE_NODANGLING = "benignException-no-dangling.pdg";

	@Test
	public void testWithDangling() throws IOException, ClassHierarchyException, UnsoundGraphException, CancelException {
		CGAndPDGKeeper k = buildSDG(false, DUMP_PDG, PDG_FILE_DANGLING);
		IFCAnalysis ana = annotate(k);
		Collection<? extends IViolation<SecurityNode>> vios = ana.doIFC();
		Assert.assertFalse(vios.isEmpty());
		SDG sdg = k.getPDG();
		Checker c = new Checker(sdg);
		Assert.assertEquals(1, sdg.edgeSet().stream().filter(c::isSummaryEdge).filter(c::isFromFirstActual).filter(c::isToExceptionOut).filter(c::isInsideMain).filter(c::callsFoo).count());

//		/**
//		 * At the call from main to foo, there is a summary edge from the first actual-in
//		 * parameter node to the exception actual-out node
//		 */
//		boolean success = false;
//		SDG sdg = k.getPDG();
//		for (SDGEdge e : sdg.edgeSet()) {
//			if (e.getKind() != SDGEdge.Kind.SUMMARY) continue;
//			SDGNode m = e.getSource();
//			SDGNode n = e.getTarget();
//			if (BytecodeLocation.ROOT_PARAMETER != m.getBytecodeIndex()) continue;
//			if (BytecodeLocation.getRootParamIndex(m.getBytecodeName()) != 1) continue;
//			if (!BytecodeLocation.EXCEPTION_PARAM.equals(n.getBytecodeName())) continue;
//			// we have a summary edge from a first actual-in to an exception actual-out parameter
//			Assert.assertEquals(m.getProc(), n.getProc());
//			SDGNode entry = sdg.getEntry(m);
//			if (!entry.getBytecodeMethod().contains("main")) continue;
//			// the call site is in the main method
//			SDGNode call = SDGParameterUtils.locateCall(m, sdg);
//			for (SDGNode calleeEntry : sdg.getPossibleTargets(call)) {
//				if (calleeEntry.getBytecodeMethod().contains("foo")) {
//					// the called method is foo
//					// now, all preconditions are satisfied.
//					success = true;
//					break;
//				}
//			}
//			if (success) {
//				break;
//			}
//		}
//		Assert.assertTrue(success);
	}

	@Test
	public void testWithoutDangling() throws IOException, ClassHierarchyException, UnsoundGraphException, CancelException {
		CGAndPDGKeeper k = buildSDG(true, DUMP_PDG, PDG_FILE_NODANGLING);
		IFCAnalysis ana = annotate(k);
		Collection<? extends IViolation<SecurityNode>> vios = ana.doIFC();
		Assert.assertTrue(vios.isEmpty());
		/**
		 * At the call from main to foo, there is no summary edge from the first actual-in
		 * parameter node to the exception actual-out node
		 */
		SDG sdg = k.getPDG();
		Checker c = new Checker(sdg);
		Assert.assertEquals(0, sdg.edgeSet().stream().filter(c::isSummaryEdge).filter(c::isFromFirstActual).filter(c::isToExceptionOut).filter(c::isInsideMain).filter(c::callsFoo).count());
//		for (SDGEdge e : sdg.edgeSet()) {
//			if (e.getKind() != SDGEdge.Kind.SUMMARY) continue;
//			SDGNode m = e.getSource();
//			SDGNode n = e.getTarget();
//			if (BytecodeLocation.ROOT_PARAMETER != m.getBytecodeIndex()) continue;
//			if (BytecodeLocation.getRootParamIndex(m.getBytecodeName()) != 1) continue;
//			if (!BytecodeLocation.EXCEPTION_PARAM.equals(n.getBytecodeName())) continue;
//			// we have a summary edge from a first actual-in to an exception actual-out parameter
//			Assert.assertEquals(m.getProc(), n.getProc());
//			SDGNode entry = sdg.getEntry(m);
//			if (!entry.getBytecodeMethod().contains("main")) continue;
//			// the call site is in the main method
//			SDGNode call = SDGParameterUtils.locateCall(m, sdg);
//			for (SDGNode calleeEntry : sdg.getPossibleTargets(call)) {
//				if (calleeEntry.getBytecodeMethod().contains("foo")) {
//					// the called method is foo
//					// With the optimization, this edge should not be there!
//					Assert.fail();
//				}
//			}
//		}
	}

	protected CGAndPDGKeeper buildSDG(boolean pruneDanglingExceptionNodes, boolean dumpPDG, String pdgFileName) throws IOException, ClassHierarchyException, UnsoundGraphException, CancelException {
		final CGAndPDGKeeper cgKeeper = new CGAndPDGKeeper();
		final SDGBuilder.SDGBuilderConfig scfg = new SDGBuilder.SDGBuilderConfig();
		scfg.out = System.out;
		scfg.scope = makeMinimalScope(CLASS_PATH);
		scfg.cache = new AnalysisCache(new DefaultIRFactory());
		scfg.cha = ClassHierarchy.make(scfg.scope);
		scfg.entry = findMethod(scfg.cha);
		scfg.ext = ExternalCallCheck.EMPTY;
		scfg.immutableNoOut = SDGBuildPreparation.IMMUTABLE_NO_OUT;
		scfg.immutableStubs = SDGBuildPreparation.IMMUTABLE_STUBS;
		scfg.ignoreStaticFields = SDGBuildPreparation.IGNORE_STATIC_FIELDS;
		scfg.exceptions = ExceptionAnalysis.INTERPROC;
		scfg.pruneDDEdgesToDanglingExceptionNodes = pruneDanglingExceptionNodes;
		scfg.defaultExceptionMethodState = MethodState.DEFAULT;
		scfg.accessPath = false;
		scfg.sideEffects = null;
		scfg.prunecg = SDGBuilder.DO_NOT_PRUNE;
		scfg.pruningPolicy = new DoNotPrune();
		scfg.pts = PointsToPrecision.INSTANCE_BASED;
		scfg.customCGBFactory = null;
		scfg.staticInitializers = StaticInitializationTreatment.SIMPLE;
		scfg.fieldPropagation = FieldPropagation.OBJ_GRAPH_NO_MERGE_AT_ALL;
		scfg.debugManyGraphsDotOutput = false;
		scfg.computeInterference = false;
		scfg.computeAllocationSites = false;
		scfg.cgConsumer = cgKeeper;
		scfg.additionalContextSelector = null;
		scfg.dynDisp = DynamicDispatchHandling.PRECISE;
		scfg.debugManyGraphsDotOutput = true;
		SDG sdg = SDGBuilder.build(scfg);
		if (dumpPDG) {
			PrintWriter pw = new PrintWriter(pdgFileName);
			SDGSerializer.toPDGFormat(sdg, pw);
			pw.close();
		}
		cgKeeper.setPDG(sdg);
		return cgKeeper;
	}

	private static IMethod findMethod(IClassHierarchy cha) {
		IClass cl = cha.lookupClass(TypeReference.findOrCreate(
				ClassLoaderReference.Application, MAIN_CLASS));
		IMethod m = cl.getMethod(Selector.make("main([Ljava/lang/String;)V"));
		return m;
	}

	private static AnalysisScope makeMinimalScope(String appClassPath)
			throws IOException {
		AnalysisScope scope = AnalysisScope.createJavaAnalysisScope();
		scope.addToScope(ClassLoaderReference.Application,
				new BinaryDirectoryTreeModule(new File(appClassPath)));
		final URL url = JoanaRunner.class.getClassLoader().getResource("jSDG-stubs-jre1.4.jar");
		final URLConnection con = url.openConnection();
		final InputStream in = con.getInputStream();
		scope.addToScope(ClassLoaderReference.Primordial, new JarStreamModule(
				new JarInputStream(in)));
		return scope;
	}

	private IFCAnalysis annotate(CGAndPDGKeeper k) {
		SDGProgram program = new SDGProgram(k.getPDG());
		IFCAnalysis ana = new IFCAnalysis(program);
		for (CGNode n : k.getCallGraph()) {
			if (n.getIR() == null) continue;
			if (!n.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application)) continue;
			if (!(n.getMethod() instanceof IBytecodeMethod)) continue;
			IBytecodeMethod bcMethod = (IBytecodeMethod) n.getMethod();
			n.getIR().visitAllInstructions(new Visitor() {
				@Override
				public void visitGet(SSAGetInstruction get) {
					if (get.isStatic()) {
						if (get.getDeclaredField().getName().toString().equals("SECRET")) {
							for (SDGInstruction i : locateInPDG(get)) {
								ana.addSourceAnnotation(i, BuiltinLattices.STD_SECLEVEL_HIGH);
							}
						}
					}
				}
				@Override
				public void visitPut(SSAPutInstruction put) {
					if (put.isStatic()) {
						if (put.getDeclaredField().getName().toString().equals("PUBLIC")) {
							for (SDGInstruction i : locateInPDG(put)) {
								ana.addSinkAnnotation(i, BuiltinLattices.STD_SECLEVEL_LOW);
							}
						}
					}
				}
				Collection<SDGInstruction> locateInPDG(SSAInstruction i) {
					int bcIndex;
					try {
						bcIndex = bcMethod.getBytecodeIndex(i.iindex);
						String signature = bcMethod.getSignature();
						return program.getInstruction(JavaMethodSignature.fromString(signature), bcIndex);
					} catch (InvalidClassFileException e) {
						Assert.fail();
						return null;
					}
				}
			});
		}
		Assert.assertEquals(1, ana.getSources().size());
		Assert.assertEquals(1, ana.getSinks().size());
		return ana;
	}

	private static class CGAndPDGKeeper implements CGConsumer {
		private CallGraph cg;
		private SDG pdg;

		@Override
		public void consume(CallGraph cg, PointerAnalysis<? extends InstanceKey> pts) {
			this.cg = cg;
		}

		public void setPDG(SDG pdg) {
			this.pdg = pdg;
		}

		public CallGraph getCallGraph() {
			return cg;
		}

		public SDG getPDG() {
			return pdg;
		}
	}

	private static class Checker {
		private SDG sdg;
		public Checker(SDG sdg) {
			this.sdg = sdg;
		}
		public boolean isFromFirstActual(SDGEdge e) {
			return e.getSource().getKind() == SDGNode.Kind.ACTUAL_IN && e.getSource().getBytecodeIndex() == BytecodeLocation.ROOT_PARAMETER && BytecodeLocation.getRootParamIndex(e.getSource().getBytecodeName()) == 1;
		}
		public boolean isToExceptionOut(SDGEdge e) {
			return e.getTarget().getKind() == SDGNode.Kind.ACTUAL_OUT && BytecodeLocation.EXCEPTION_PARAM.equals(e.getTarget().getBytecodeName());
		}
		public boolean isSummaryEdge(SDGEdge e) {
			return e.getKind() == SDGEdge.Kind.SUMMARY;
		}
		public boolean isInsideMain(SDGEdge e) {
			if (e.getSource().getProc() != e.getTarget().getProc()) {
				return false;
			} else {
				SDGNode entry = sdg.getEntry(e.getSource());
				return entry.getBytecodeMethod().contains("main");
			}
		}
		public boolean callsFoo(SDGEdge e) {
			SDGNode call = SDGParameterUtils.locateCall(e.getSource(), sdg);
			if (call == null) {
				throw new IllegalArgumentException("wrong edge kind " + e.getKind());
			}
			for (SDGNode calleeEntry : sdg.getPossibleTargets(call)) {
				if (calleeEntry.getBytecodeMethod().contains("foo(I)I")) {
					return true;
				}
			}
			return false;
		}
	}
}