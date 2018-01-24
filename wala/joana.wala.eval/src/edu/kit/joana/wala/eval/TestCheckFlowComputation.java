package edu.kit.joana.wala.eval;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.JarStreamModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.config.SetOfClasses;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;
import edu.kit.joana.wala.core.ExternalCallCheck;
import edu.kit.joana.wala.core.Main;
import edu.kit.joana.wala.core.Main.Config;
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.core.SDGBuilder.StaticInitializationTreatment;
import edu.kit.joana.wala.core.accesspath.APResult;
import edu.kit.joana.wala.core.accesspath.AccessPath;
import edu.kit.joana.wala.dictionary.accesspath.AliasSDG;
import edu.kit.joana.wala.dictionary.accesspath.CheckFlowLessWithAlias;
import edu.kit.joana.wala.dictionary.accesspath.CheckFlowLessWithAlias.CheckFlowConfig;
import edu.kit.joana.wala.dictionary.accesspath.CheckFlowLessWithAlias.EntityNotFoundException;
import edu.kit.joana.wala.dictionary.accesspath.FlowCheckResultConsumer;
import edu.kit.joana.wala.dictionary.accesspath.FlowCheckResultConsumer.FlowStmtResult;
import edu.kit.joana.wala.dictionary.accesspath.FlowCheckResultConsumer.FlowStmtResultPart;
import edu.kit.joana.wala.dictionary.accesspath.FlowCheckResultConsumer.MethodResult;
import edu.kit.joana.wala.dictionary.accesspath.FlowLess2SDGMatcher;
import edu.kit.joana.wala.dictionary.accesspath.Matcher;
import edu.kit.joana.wala.flowless.MoJo;
import edu.kit.joana.wala.flowless.MoJo.CallGraphResult;
import edu.kit.joana.wala.flowless.pointsto.GraphAnnotater.Aliasing;
import edu.kit.joana.wala.flowless.pointsto.PointsToSetBuilder.PointsTo;
import edu.kit.joana.wala.flowless.spec.FlowLessSimplifier;
import edu.kit.joana.wala.flowless.spec.FlowLessSimplifier.BasicIFCStmt;
import edu.kit.joana.wala.flowless.spec.ast.ExplicitFlowStmt;
import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor.FlowAstException;
import edu.kit.joana.wala.flowless.spec.ast.IFCStmt;
import edu.kit.joana.wala.flowless.spec.ast.Parameter;
import edu.kit.joana.wala.flowless.spec.ast.PrimitiveAliasStmt;
import edu.kit.joana.wala.flowless.spec.java.ast.ClassInfo;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo;

public final class TestCheckFlowComputation {

	private static final PrintStream out = System.out;
	private static final String STD_OUT_DIR = "./out/eval-cflow/";
	private static final String EXCLUSION_REG_EXP = "java\\/awt\\/.*\n" + "javax\\/swing\\/.*\n" + "sun\\/awt\\/.*\n"
			+ "sun\\/swing\\/.*\n" + "com\\/sun\\/.*\n" + "sun\\/.*\n";
	@SuppressWarnings("unused")
	private static final String AGGRESSIVE_EXCLUSION_REG_EXP = EXCLUSION_REG_EXP + "java\\/nio\\/.*\n" + "javax\\/.*\n"
			+ "java\\/util\\/.*\n" + "java\\/security\\/.*\n" + "java\\/beans\\/.*\n" + "org\\/omg\\/.*\n"
			+ "apple\\/awt\\/.*\n" + "com\\/apple\\/.*\n";
	
	private static class Run {
		public String name;
		public String entryMethod;
		public String classpath;
		public String outputDir = STD_OUT_DIR;
		public IMethod im;
		public MethodResult m;
		public ExpR[] expected;
		public long timePreprareSDG = 0;
		public long numPreparedSDGs = 0;
		public long timeAdjustSDG = 0;
		public long numAdjustSDGs = 0;
		public long startPrepareTime, endPrepareTime;
		public long startAdjustTime, endAdjustTime;
		
		public Run(String name, String entryMethod, String classpath) {
			this.name = name;
			this.entryMethod = entryMethod;
			this.classpath = classpath;
		}
		
		public String toString() {
			return name + "(" + entryMethod + ")(" + classpath + ")";
		}
		
		public void addStats(final Run run) {
			timePreprareSDG += run.timePreprareSDG;
			numPreparedSDGs += run.numPreparedSDGs;
			timeAdjustSDG += run.timeAdjustSDG;
			numAdjustSDGs += run.numAdjustSDGs;
		}
	}
	
	private static Run OVERALL; 
	
	public TestCheckFlowComputation() {}

	public static String currentMethodName() {
		final Throwable t = new Throwable();
		final StackTraceElement e = t.getStackTrace()[1];
		return e.getMethodName();
	}
	
	private static final String WITH_EXC_SUFFIX = "cf_with_exc";
	private static final String LOG_FILE = STD_OUT_DIR + "checkflow.log";
	private static final String LIB_DIR = "../../contrib/lib/stubs/";
	private static final String BIN_DIR = "../joana.wala.testdata/bin";
	private static final String SRC_DIR = "../joana.wala.testdata/src";

	
	private static void prepareConfig() {
		final CheckFlowConfig cfc = new CheckFlowConfig(BIN_DIR, new String[] { SRC_DIR }, STD_OUT_DIR, LIB_DIR,
				CheckFlowLessWithAlias.createPrintStream(LOG_FILE), FlowCheckResultConsumer.STDOUT,
				NullProgressMonitor.INSTANCE);
		
		setup.cfc = cfc;
	}
	
	private static void createMoJo() throws IOException, ClassHierarchyException {
		final CheckFlowConfig cfc = setup.cfc;
		cfc.out.print("Parsing source files... ");
		final List<ClassInfo> clsInfos = new LinkedList<ClassInfo>();
		for (final String src : cfc.src) {
			List<ClassInfo> clsNfoTmp = MoJo.parseSourceFiles(src);
			clsInfos.addAll(clsNfoTmp);
		}
		cfc.out.println("done.");
		cfc.out.print("Checking for syntactic errors... ");
		final int errors = MoJo.prepareFlowLessStmts(clsInfos);
		if (errors > 0) {
			cfc.out.print("(" + errors + " errors) ");
		}
		cfc.out.println("done.");

		// set static field for later use
		setup.clsInfo = clsInfos;
		
		final Config cfg = new Config(WITH_EXC_SUFFIX);
		cfg.entryMethod = "<main entry not used>";
		cfg.classpath=  cfc.bin;
		cfg.pts = PointsToPrecision.INSTANCE_BASED;
		cfg.exceptions = ExceptionAnalysis.INTRAPROC;
		cfg.accessPath = true;
		cfg.exclusions = Main.STD_EXCLUSION_REG_EXP;
		cfg.nativesXML = cfc.libDir + "natives_empty.xml";
		cfg.stubs = cfc.libDir + "jSDG-stubs-jre1.4.jar";
		cfg.extern = ExternalCallCheck.EMPTY;
		cfg.outputDir = cfc.tmpDir;
		cfg.fieldPropagation = FieldPropagation.OBJ_TREE;

		setup.cfg = cfg;
		
		cfc.out.println(cfg);

		if (cfc.scope == null) {
			cfc.out.print("Setting up analysis scope... ");
			cfc.scope = createAnalysisScope(cfg);
		    cfc.out.println("done.");
		} else {
			cfc.out.println("Using provided analysis scope.");
		}

		cfc.out.print("Creating MoJo... (class hierarchy: ");
		final ClassHierarchy cha = ClassHierarchyFactory.make(cfc.scope);
		cfg.extern.setClassHierarchy(cha);
		cfc.out.print(cha.getNumberOfClasses() + " classes) ");
		final MoJo mojo = MoJo.create(cha, cfg.outputDir);
		cfc.out.println("done.");

		setup.mojo = mojo;
	}
	
	private static class Setup {
		private List<ClassInfo> clsInfo;
		private CheckFlowConfig cfc;
		private Config cfg;
		private MoJo mojo;
		private final boolean printStatistics = true;
		
		private void reset() {
			clsInfo = null;
			cfc = null;
			mojo = null;
		}
	}
	
	private static final Setup setup = new Setup();
	
    @BeforeClass
    public static void setUp() {
    	System.out.println("setting up");
    	OVERALL = new Run("overall", "<only stats>", "<no cp>");
    	prepareConfig();
    	try {
			createMoJo();
		} catch (ClassHierarchyException e) {
			Assert.fail(e.getMessage());
			e.printStackTrace(System.out);
		} catch (IOException e) {
			Assert.fail(e.getMessage());
			e.printStackTrace(System.out);
		}
    }

    @AfterClass
    public static void tearDown() {
    	System.out.println("tearing down...");
		System.out.println("\ttotal prepared SDGs     : " + OVERALL.numPreparedSDGs);
		System.out.println("\ttotal prepared SDGs time: " + OVERALL.timePreprareSDG);
		System.out.println("\ttotal adjusted SDGs     : " + OVERALL.numAdjustSDGs);
		System.out.println("\ttotal adjusted SDGs time: " + OVERALL.timeAdjustSDG);
		final long avgPrepare = (OVERALL.numPreparedSDGs > 0 ? (OVERALL.timePreprareSDG / OVERALL.numPreparedSDGs) : 0);
		final long avgAdjust = (OVERALL.numAdjustSDGs > 0 ? (OVERALL.timeAdjustSDG / OVERALL.numAdjustSDGs) : 0);
		System.out.println("\tavg. prepared SDGs time : " + avgPrepare);
		System.out.println("\tavg. adjusted SDGs time : " + avgAdjust);
		System.out.println("\tspeed gain by adjust: " + (avgAdjust > 0 ? (avgPrepare / avgAdjust) : 0) + "x faster");
    	setup.reset();
    	System.out.println("done.");
    }
	
    private static MethodInfo findMethod(final String name) {
    	if (setup.clsInfo == null) {
    		Assert.fail("No class info available.");
    	}
    	
		for (final ClassInfo cls : setup.clsInfo) {
			for (final MethodInfo m : cls.getMethods()) {
				if (m.getName().equals(name)) {
					return m;
				}
			}
		}
		
		return null;
    }
    
    private static final class ExpR {
    	private final String clause;
    	private final Res res;
    	
    	private ExpR(final String clause, final Res res) {
    		this.clause = clause;
    		this.res = res;
    	}
    	
    	public boolean matches(final IFCStmt ifc) {
    		return clause.equals(ifc.toString());
    	}
    	
    	public String check(final FlowStmtResult stmt) {
    		final IFCStmt ifc = stmt.getStmt();
    		Assert.assertTrue("'" + clause + "' doesn't match current stmt: '" + ifc.toString() + "'", matches(ifc));
    		switch (res) {
    		case ALWAYS_SATISFIED:
    			Assert.assertTrue("'" + clause + "' not always satisfied.", stmt.isAlwaysSatisfied());
    			return "always satisfied.";
    		case NEVER_SATISFIED:
    			Assert.assertTrue("'" + clause + "' should not be satisfied.", stmt.isNeverSatisfied());
    			return "never satisfied.";
    		case NO_EXC_SATISFIED:
    			Assert.assertTrue("'" + clause + "' not satisfied without exceptions.", stmt.isNoExceptionSatisfied());
    			return "satisfied without exceptions.";
    		case INFERRED_SATISFIED:
    			Assert.assertTrue("'" + clause + "' could not be inferred.", stmt.isInferredSatisfied());
    			
    			final StringBuffer sb = new StringBuffer("can be inferred.\n");
    			for (final FlowStmtResultPart part : stmt.getParts()) {
    				if (part.isSatisfied()) {
        				final String condition = part.getDescription();
    					sb.append("\t\t\t" + condition + ": " + (part.isSatisfied() ? "ok" : "fail") +  "\n");
    				}
    			}
    			sb.append("\t\tinference done.");
    			
    			return sb.toString();
    		case NO_EXC_INFERRED_SATISFIED:
    			Assert.assertTrue("'" + clause + "' could not be inferred without exceptions.", stmt.isInferredNoExcSatisfied());
    			return "can be inferred without exceptions.";
    		}
    		
    		return "weird should not happen.";
    	}
    	
    }
    
    private static enum Res { ALWAYS_SATISFIED, NEVER_SATISFIED, NO_EXC_SATISFIED, INFERRED_SATISFIED, NO_EXC_INFERRED_SATISFIED };
    
    private static String check(final FlowStmtResult result, final ExpR[] expected) {
    	final IFCStmt ifc = result.getStmt();
    	ExpR exp = null;
    	for (final ExpR cur : expected) {
    		if (cur.matches(ifc)) {
    			exp = cur;
    			break;
    		}
    	}
    	
    	Assert.assertNotNull("no expected outcome found for '" + ifc + "'", exp);
    	return exp.check(result);
    }
    
    private void checkRun(final Run run) {
    	try { 
			out.println(run.name + " starts...");
			final MethodInfo mnfo = findMethod(run.entryMethod);
			Assert.assertNotNull(mnfo);
			Assert.assertTrue(mnfo.hasIFCStmts());
			final IMethod start = setup.mojo.findMethod(mnfo);
			Assert.assertNotNull(start);
			run.im = start;
			final MethodResult mres = new MethodResult(mnfo, setup.cfc.tmpDir);
			run.m = mres;
			final AliasSDG alias = prepareForFlowLessCheck(run, NullProgressMonitor.INSTANCE);
	
			out.println("\t'" + mnfo + "' preparation done, ifc check starts...");
			
			boolean resetNeeded = false;
	
			for (final IFCStmt stmt : mres.getInfo().getIFCStmts()) {
				setup.cfc.out.print("ifc check '" + stmt + "': ");
				out.print("\t\tifc check '" + stmt + "': ");
				if (resetNeeded) {
					alias.reset();
					resetNeeded = false;
				}
	
				final FlowStmtResult stmtResult = mres.findOrCreateStmtResult(stmt);
				
				checkFlowLessForStatement(run, alias, stmtResult, NullProgressMonitor.INSTANCE);
	
				final String chk = check(stmtResult, run.expected);
				
				out.println(chk);
				
				resetNeeded = true;
			}
			
			out.println("\t'" + mnfo + "' ifc check done.");
	
			OVERALL.addStats(run);
			
			if (setup.printStatistics) {
				out.println("\ttotal prepared SDGs     : " + run.numPreparedSDGs);
				out.println("\ttotal prepared SDGs time: " + run.timePreprareSDG);
				out.println("\ttotal adjusted SDGs     : " + run.numAdjustSDGs);
				out.println("\ttotal adjusted SDGs time: " + run.timeAdjustSDG);
				final long avgPrepare = (run.numPreparedSDGs > 0 ? (run.timePreprareSDG / run.numPreparedSDGs) : 0);
				final long avgAdjust = (run.numAdjustSDGs > 0 ? (run.timeAdjustSDG / run.numAdjustSDGs) : 0);
				out.println("\tavg. prepared SDGs time : " + avgPrepare);
				out.println("\tavg. adjusted SDGs time : " + avgAdjust);
				out.println("\tspeed gain by adjust: " + (avgAdjust > 0 ? (avgPrepare / avgAdjust) : 0) + "x faster");
			}
	
			out.println(run.name + " done.");
		} catch (ClassHierarchyException | IllegalArgumentException | IOException | CancelException
				| UnsoundGraphException e) {
			throw new RuntimeException(e);
		}
    }
    
	@Test
	public void test_exceptionalFlow1() {
		final Run run = new Run(currentMethodName(),
			"exceptionalFlow1",
			"../../example/joana.example.many-small-progs/bin");
		run.expected = new ExpR[] { 
			new ExpR("!{a, b} => (a.x)-!>(b.x)", Res.ALWAYS_SATISFIED),
		};
		
		checkRun(run);
	}
	
	@Test
	public void test_foo1() {
		final Run run = new Run(currentMethodName(),
			"foo1",
			"../../example/joana.example.many-small-progs/bin");
		run.expected = new ExpR[] { 
			new ExpR("? => (b)-!>(\\result)", Res.INFERRED_SATISFIED),
		};
		
		checkRun(run);
	}
	
	@Test
	public void test_impossibleAlias() {
		final Run run = new Run(currentMethodName(),
			"impossibleAlias",
			"../../example/joana.example.many-small-progs/bin");
		run.expected = new ExpR[] { 
			new ExpR("? => (b)-!>(\\result)", Res.INFERRED_SATISFIED),
		};
		
		checkRun(run);
	}
	
	@Test
	public void test_indirectFoo1() {
		final Run run = new Run(currentMethodName(),
			"indirectFoo1",
			"../../example/joana.example.many-small-progs/bin");
		run.expected = new ExpR[] { 
			new ExpR("? => (b)-!>(\\result)", Res.INFERRED_SATISFIED),
		};
		
		checkRun(run);
	}
	
	@Test
	public void test_indirectRevFoo1() {
		final Run run = new Run(currentMethodName(),
			"indirectRevFoo1",
			"../../example/joana.example.many-small-progs/bin");
		run.expected = new ExpR[] { 
			new ExpR("? => (a)-!>(\\result)", Res.INFERRED_SATISFIED),
			new ExpR("? => (b)-!>(\\result)", Res.NEVER_SATISFIED),
		};
		
		checkRun(run);
	}
	
	@Test
	public void test_indirectMultipleFoo1() {
		final Run run = new Run(currentMethodName(),
			"indirectMultipleFoo1",
			"../../example/joana.example.many-small-progs/bin");
		run.expected = new ExpR[] { 
			new ExpR("? => (a)-!>(\\result)", Res.NEVER_SATISFIED),
			new ExpR("? => (b)-!>(\\result)", Res.INFERRED_SATISFIED),
		};
		
		checkRun(run);
	}

	@Test
	public void test_indirectSameFoo1() {
		final Run run = new Run(currentMethodName(),
			"indirectSameFoo1",
			"../../example/joana.example.many-small-progs/bin");
		run.expected = new ExpR[] { 
				new ExpR("? => (a)-!>(\\result)", Res.NEVER_SATISFIED),
				new ExpR("? => (b)-!>(\\result)", Res.INFERRED_SATISFIED),
		};
		
		checkRun(run);
	}

	@Test
	public void test_foo2() {
		final Run run = new Run(currentMethodName(),
			"foo2",
			"../../example/joana.example.many-small-progs/bin");
		run.expected = new ExpR[] { 
				new ExpR("? => (a)-!>(\\result)", Res.NEVER_SATISFIED),
				new ExpR("? => (b)-!>(\\result)", Res.INFERRED_SATISFIED),
		};
		
		checkRun(run);
	}

	@Test
	public void test_foo3() {
		final Run run = new Run(currentMethodName(),
			"foo3",
			"../../example/joana.example.many-small-progs/bin");
		run.expected = new ExpR[] { 
				new ExpR("? => (a)-!>(\\result)", Res.NEVER_SATISFIED),
				new ExpR("? => (b)-!>(\\result)", Res.INFERRED_SATISFIED),
		};
		
		checkRun(run);
	}

	@Test
	public void test_foo4() {
		final Run run = new Run(currentMethodName(),
			"foo4",
			"../../example/joana.example.many-small-progs/bin");
		run.expected = new ExpR[] { 
				new ExpR("(!{a.*, b.*}) && ((!{a.*, c.*}) && ((!{a.*, d.*}) && ((!{b.*, c.*}) && ((!{b.*, d.*}) && (!{c.*, d.*}))))) => (a)-!>(\\result)", Res.ALWAYS_SATISFIED),
				new ExpR("? => (a)-!>(\\result)", Res.INFERRED_SATISFIED),
				new ExpR("? => (b)-!>(\\result)", Res.INFERRED_SATISFIED),
				new ExpR("? => (c)-!>(\\result)", Res.INFERRED_SATISFIED),
				new ExpR("? => (d)-!>(\\result)", Res.NEVER_SATISFIED),
		};
		
		checkRun(run);
	}

	@Test
	public void test_foo5() {
		final Run run = new Run(currentMethodName(),
			"foo5",
			"../../example/joana.example.many-small-progs/bin");
		run.expected = new ExpR[] { 
				new ExpR("(!{a.*, b.*}) && ((!{a.*, c.*}) && ((!{a.*, d.*}) && ((!{b.*, c.*}) && ((!{b.*, d.*}) && (!{c.*, d.*}))))) => (a)-!>(\\result)", Res.ALWAYS_SATISFIED),
				new ExpR("? => (a)-!>(\\result)", Res.INFERRED_SATISFIED),
				new ExpR("? => (b)-!>(\\result)", Res.INFERRED_SATISFIED),
				new ExpR("? => (c)-!>(\\result)", Res.INFERRED_SATISFIED),
				new ExpR("? => (d)-!>(\\result)", Res.NEVER_SATISFIED),
		};
		
		checkRun(run);
	}

	@Test
	public void test_foo6() {
		final Run run = new Run(currentMethodName(),
			"foo6",
			"../../example/joana.example.many-small-progs/bin");
		run.expected = new ExpR[] { 
				new ExpR("!{a.*, b.*} => (a)-!>(b)", Res.ALWAYS_SATISFIED),
				new ExpR("? => (a)-!>(b)", Res.ALWAYS_SATISFIED),
				new ExpR("? => (a)-!>(\\result)", Res.NEVER_SATISFIED),
				new ExpR("? => (b)-!>(\\result)", Res.NEVER_SATISFIED),
		};
		
		checkRun(run);
	}

	@Test
	public void test_foo7() {
		final Run run = new Run(currentMethodName(),
			"foo7",
			"../../example/joana.example.many-small-progs/bin");
		run.expected = new ExpR[] { 
				new ExpR("!{a.*, b.*} => (a)-!>(b)", Res.ALWAYS_SATISFIED),
				new ExpR("? => (a)-!>(b)", Res.INFERRED_SATISFIED),
				new ExpR("? => (a)-!>(\\result)", Res.INFERRED_SATISFIED),
				new ExpR("? => (b)-!>(\\result)", Res.NEVER_SATISFIED),
		};
		
		checkRun(run);
	}

	@Test
	public void test_foo8() {
		final Run run = new Run(currentMethodName(),
			"foo8",
			"../../example/joana.example.many-small-progs/bin");
		run.expected = new ExpR[] { 
				new ExpR("!{a.*, b.*} => (a)-!>(b)", Res.ALWAYS_SATISFIED),
				new ExpR("? => (a)-!>(b)", Res.INFERRED_SATISFIED),
				new ExpR("? => (a)-!>(\\result)", Res.INFERRED_SATISFIED),
				new ExpR("? => (b)-!>(\\result)", Res.NEVER_SATISFIED),
		};
		
		checkRun(run);
	}
	
	@Test
	public void test_foo9() {
		final Run run = new Run(currentMethodName(),
			"foo9",
			"../../example/joana.example.many-small-progs/bin");
		run.expected = new ExpR[] { 
				new ExpR("!{a.*, b.*} => (a)-!>(b)", Res.NEVER_SATISFIED),
				new ExpR("? => (a)-!>(b)", Res.NEVER_SATISFIED),
				new ExpR("!{a.*, b.*} => (b)-!>(a)", Res.ALWAYS_SATISFIED),
				new ExpR("? => (b)-!>(a)", Res.INFERRED_SATISFIED),
				new ExpR("? => (a)-!>(\\result)", Res.NEVER_SATISFIED),
				new ExpR("? => (b)-!>(\\result)", Res.NEVER_SATISFIED),
		};
		
		checkRun(run);
	}
	
	@Test
	public void test_foo9b() {
		final Run run = new Run(currentMethodName(),
			"foo9b",
			"../../example/joana.example.many-small-progs/bin");
		run.expected = new ExpR[] { 
				new ExpR("!{a.*, b.*} => (a)-!>(b)", Res.NEVER_SATISFIED),
				new ExpR("? => (a)-!>(b)", Res.NEVER_SATISFIED),
				new ExpR("!{a.*, b.*} => (b)-!>(a)", Res.ALWAYS_SATISFIED),
				new ExpR("!{a.f.*, b.*} => (b)-!>(a)", Res.ALWAYS_SATISFIED),
				new ExpR("? => (b)-!>(a)", Res.INFERRED_SATISFIED),
				new ExpR("? => (a)-!>(\\result)", Res.NEVER_SATISFIED),
				new ExpR("? => (b)-!>(\\result)", Res.NEVER_SATISFIED),
		};
		
		checkRun(run);
	}
	
	@Test
	public void test_foo10() {
		final Run run = new Run(currentMethodName(),
			"foo10",
			"../../example/joana.example.many-small-progs/bin");
		run.expected = new ExpR[] { 
				new ExpR("!{a.*, b.*} => (a)-!>(b)", Res.NEVER_SATISFIED),
				new ExpR("? => (a)-!>(b)", Res.NEVER_SATISFIED),
				new ExpR("!{a.*, b.*} => (b)-!>(a)", Res.ALWAYS_SATISFIED),
				new ExpR("? => (b)-!>(a)", Res.INFERRED_SATISFIED),
		};
		
		checkRun(run);
	}
	
	@Test
	public void test_foo12() {
		final Run run = new Run(currentMethodName(),
			"foo12",
			"../../example/joana.example.many-small-progs/bin");
		run.expected = new ExpR[] { 
				new ExpR("!{a.*, b.*} => (a)-!>(b)", Res.NEVER_SATISFIED),
		};
		
		checkRun(run);
	}
	
	@Test
	public void test_foo13() {
		final Run run = new Run(currentMethodName(),
			"foo13",
			"../../example/joana.example.many-small-progs/bin");
		run.expected = new ExpR[] { 
				new ExpR("!{a.*, b.*} => (a)-!>(b)", Res.NEVER_SATISFIED),
		};
		
		checkRun(run);
	}
	
	@Test
	public void test_getField1() {
		final Run run = new Run(currentMethodName(),
			"getField1",
			"../../example/joana.example.many-small-progs/bin");

		run.expected = new ExpR[] { 
				new ExpR(" => (a)-!>(b)", Res.ALWAYS_SATISFIED),
				new ExpR("? => (a)-!>(\\result)", Res.NEVER_SATISFIED),
				new ExpR("? => (b)-!>(\\result)", Res.INFERRED_SATISFIED),
		};
		
		checkRun(run);
	}
	
	@Test
	public void test_aliasExcTest() {
		final Run run = new Run(currentMethodName(),
			"aliasExcTest",
			"../../example/joana.example.many-small-progs/bin");

		run.expected = new ExpR[] { 
				new ExpR("!{a, b} => (a)-!>(b)", Res.NO_EXC_SATISFIED),
				new ExpR("? => (a)-!>(b)", Res.INFERRED_SATISFIED),
				new ExpR(" => (a)-!>(b)", Res.NEVER_SATISFIED),
		};
		
		checkRun(run);
	}
	
	@Test
	public void test_aliasTest() {
		final Run run = new Run(currentMethodName(),
			"aliasTest",
			"../../example/joana.example.many-small-progs/bin");

		run.expected = new ExpR[] { 
				new ExpR("!{a, b} => (a)-!>(b)", Res.ALWAYS_SATISFIED),
				new ExpR("!{a, a} => (a)-!>(b)", Res.NEVER_SATISFIED),
				new ExpR("? => (a)-!>(b)", Res.INFERRED_SATISFIED),
		};
		
		checkRun(run);
	}
	
	@Test
	public void test_indirectAliasTest() {
		final Run run = new Run(currentMethodName(),
			"indirectAliasTest",
			"../../example/joana.example.many-small-progs/bin");

		run.expected = new ExpR[] { 
				new ExpR("!{a, b} => (a)-!>(b)", Res.ALWAYS_SATISFIED),
				new ExpR("!{a, a} => (a)-!>(b)", Res.NEVER_SATISFIED),
				new ExpR("? => (a)-!>(b)", Res.INFERRED_SATISFIED),
		};
		
		checkRun(run);
	}
	
	@Test
	public void test_invokeSingleParamAlias() {
		final Run run = new Run(currentMethodName(),
			"invokeSingleParamAlias",
			"../../example/joana.example.many-small-progs/bin");

		run.expected = new ExpR[] { 
				new ExpR(" => (a)-!>(\\result)", Res.NEVER_SATISFIED),
				new ExpR(" => (a.f2)-!>(\\result)", Res.ALWAYS_SATISFIED),
				new ExpR("!{a, a} => (a.f2.f3)-!>(\\result)", Res.ALWAYS_SATISFIED), // TODO only satisfied if a.f1 != a.f2
				new ExpR("!{a, a} => (a.f1.f3)-!>(\\result)", Res.NEVER_SATISFIED),
		};
		
		checkRun(run);
	}

	@Test
	public void test_invokeMultipleParamAlias() {
		final Run run = new Run(currentMethodName(),
			"invokeMultipleParamAlias",
			"../../example/joana.example.many-small-progs/bin");

		run.expected = new ExpR[] { 
				new ExpR("? => (d.f2)-!>(\\result)", Res.INFERRED_SATISFIED),
				new ExpR("(!{b, a}) && ((!{d, a}) && (!{a, c})) => (d.f2)-!>(\\result)", Res.ALWAYS_SATISFIED),
				new ExpR("(!{b, a}) && (!{d, a}) => (d.f2)-!>(\\result)", Res.NEVER_SATISFIED), 
				new ExpR("(!{b, a}) && ((!{d, a}) && ((!{a, c}) && (!{d, c}))) => (d.f2)-!>(\\result)", Res.ALWAYS_SATISFIED),
				new ExpR("(!{b, a}) && ((!{d, a}) && ((!{a, c}) && (!{a, a}))) => (a.f2.f3)-!>(\\result)", Res.ALWAYS_SATISFIED),
		};
		
		checkRun(run);
	}

	@Test
	public void test_invokeStringAndPrintln() {
		final Run run = new Run(currentMethodName(),
			"invokeStringAndPrintln",
			"../../example/joana.example.many-small-progs/bin");

		run.expected = new ExpR[] { 
				new ExpR(" => (s1)-!>(\\result)", Res.NO_EXC_SATISFIED),
				new ExpR("? => (a)-!>(\\result)", Res.NEVER_SATISFIED),
		};
		
		checkRun(run);
	}
	

	@Test
	public void test_compute() {
		final Run run = new Run(currentMethodName(),
			"compute",
			"../../example/joana.example.many-small-progs/bin");

		run.expected = new ExpR[] { 
				new ExpR(" => (d)-!>(\\result)", Res.NEVER_SATISFIED),
				new ExpR("!{a.*, b.*, c.*, d.*, e.*, g.*} => (d)-!>(\\result)", Res.ALWAYS_SATISFIED),
				new ExpR("!{a.*, b.*, c.*, d.*, e.*} => (d)-!>(\\result)", Res.NO_EXC_SATISFIED),
				new ExpR("? => (d)-!>(\\result)", Res.INFERRED_SATISFIED),
				new ExpR("(!{e, b}) && ((!{b, d}) && ((!{b, a}) && ((!{b, g}) && (!{b, c})))) => (d)-!>(\\result)", Res.ALWAYS_SATISFIED),
		};
		
		checkRun(run);
	}

	@Test
	public void test_callToCompute() {
		final Run run = new Run(currentMethodName(),
			"callToCompute",
			"../../example/joana.example.many-small-progs/bin");

		run.expected = new ExpR[] { 
				new ExpR(" => (d)-!>(\\result)", Res.NEVER_SATISFIED),
				new ExpR("!{a.*, b.*, c.*, d.*} => (d)-!>(\\result)", Res.ALWAYS_SATISFIED),
		};
		
		checkRun(run);
	}
	
	private AliasSDG prepareForFlowLessCheck(final Run run, final IProgressMonitor progress)
			throws IllegalArgumentException, CancelException, ClassHierarchyException, IOException, UnsoundGraphException {
		if (setup.printStatistics) { run.startPrepareTime = System.nanoTime(); }
		final CheckFlowConfig cfc = setup.cfc;
		final IMethod im = run.im;
		final MethodResult m = run.m;
		final MoJo mojo = setup.mojo;
		final Config cfg = setup.cfg;
		cfc.out.println("checking '" + m + "'");
		final Aliasing minMax = mojo.computeMinMaxAliasing(im);
		final PointsTo ptsMax = MoJo.computePointsTo(minMax.upperBound);
		final AnalysisOptions opt = mojo.createAnalysisOptionsWithPTS(ptsMax, im);
		final CallGraphResult cgr;
		switch (cfg.pts) {
		case TYPE_BASED:
			cgr = mojo.computeContextInsensitiveCallGraph(opt);
			break;
		case INSTANCE_BASED:
			cgr = mojo.computeContextSensitiveCallGraph(opt);
			break;
		case OBJECT_SENSITIVE:
			cgr = mojo.computeObjectSensitiveCallGraph(opt, cfg.objSensFilter);
			break;
		default:
			throw new IllegalStateException();
		}

		final SDGResult sdgResult = create(run, opt.getAnalysisScope(), cgr);
		final SDG sdg = sdgResult.sdg;

		final AliasSDG alias = AliasSDG.create(sdg, sdgResult.ap);
		alias.precomputeSummary(progress);

		if (setup.printStatistics) {
			run.endPrepareTime = System.nanoTime();
			run.numPreparedSDGs++;
			run.timePreprareSDG += (run.endPrepareTime - run.startPrepareTime);
		}
		
		return alias;
	}
	
	private void checkFlowLessForStatement(final Run run, final AliasSDG alias, final FlowStmtResult stmtResult,
			final IProgressMonitor progress) throws IllegalArgumentException, CancelException, ClassHierarchyException,
			IOException, UnsoundGraphException {
		final CheckFlowConfig cfc = setup.cfc;
		final Config cfg = setup.cfg;
		final SDG sdg = alias.getSDG();
		final IFCStmt stmt = stmtResult.getStmt();

		try {
			final List<BasicIFCStmt> simplified = FlowLessSimplifier.simplify(stmt);
			//FlowLess2SDGMatcher.printDebugMatches = true;
			final Matcher match = FlowLess2SDGMatcher.findMatchingNodes(sdg, sdg.getRoot(), stmt);
			if (simplified.isEmpty()) {
				cfc.out.println("ERROR(empty simplified statements)");
				stmtResult.addPart(new FlowStmtResultPart(null, "ERROR(empty simplified statements)",
						false, false, cfg.exceptions, sdg.getFileName()));
			} else {
				checkBasicIFCStmts(alias, match, simplified, run, stmtResult, progress);
			}
		} catch (FlowAstException e) {
			cfc.out.println("ERROR(" + e.getMessage() + ")");
			stmtResult.addPart(new FlowStmtResultPart(null, "ERROR(" + e.getMessage() + ")", false, false,
					cfg.exceptions, sdg.getFileName()));
		}
	}
	
	private void checkBasicIFCStmts(final AliasSDG alias, final Matcher match,
			final List<BasicIFCStmt> stmts, final Run run, final FlowStmtResult stmtResult,
			final IProgressMonitor progress) throws CancelException, EntityNotFoundException {
		final MethodInfo mInfo = run.m.getInfo();
		final CheckFlowConfig cfc = setup.cfc;
		final ExceptionAnalysis exc = setup.cfg.exceptions;
		
		for (final BasicIFCStmt s : stmts) {
			for (final PrimitiveAliasStmt noAlias : s.aMinus) {
				final Parameter[] noalias = noAlias.getParams().toArray(new Parameter[1]);
				for (int i = 0; i < noalias.length; i++) {
					assert match.hasMatchFor(noalias[i]);
					final Parameter n1p = noalias[i];
					final SDGNode n1 = match.getFineMatchIN(n1p);
					if (n1 == null) {
						throw new EntityNotFoundException("found no matching parameter for '" + noalias[i] + "' in "
								+ mInfo.toString());
					}
					for (int j = i + 1; j < noalias.length; j++) {
						assert match.hasMatchFor(noalias[j]);
						final Parameter n2p = noalias[j];
						final SDGNode n2 = match.getFineMatchIN(n2p);
						if (n2 == null) {
							throw new EntityNotFoundException("found no matching parameter for '" + noalias[j] + "' in "
									+ mInfo.toString());
						}
						
						alias.setNoAlias(n1.getId(), n1p.endsWithWildcard(), n2.getId(), n2p.endsWithWildcard());
//						alias.setNoAlias(n1.getId(), n2.getId());
					}
				}
			}
		}

		if (setup.printStatistics) { run.startAdjustTime = System.nanoTime(); }
		alias.adjustMaxSDG(progress);
		alias.recomputeSummary(progress);
		if (setup.printStatistics) {
			run.endAdjustTime = System.nanoTime();
			run.numAdjustSDGs++;
			run.timeAdjustSDG += (run.endAdjustTime - run.startAdjustTime);
		}

		final Set<SDGEdge.Kind> omit = new HashSet<SDGEdge.Kind>();
		final SummarySlicerBackward ssb = new SummarySlicerBackward(alias.getSDG(), omit);
		ssb.addToOmit(SDGEdge.Kind.SUMMARY); // use only SUMMARY_HEAP

		final List<FlowStmtResultPart> toInfere = new LinkedList<FlowStmtResultPart>();

		for (final BasicIFCStmt s : stmts) {
			for (final ExplicitFlowStmt fl : s.flow.fMinus) {
				if (CheckFlowLessWithAlias.hasIllegalFlow(fl, match, ssb, alias)) {
					final FlowStmtResultPart illegalPart = new FlowStmtResultPart(s, fl.toString(), false, false,
							exc, alias.getFileName());
					illegalPart.setAlias(alias.getNoAlias());
					stmtResult.addPart(illegalPart);
					cfc.out.println("illegal flow found:" + s.flow);
					if (s.shouldBeInferred) {
						toInfere.add(illegalPart);
					}
				} else {
					final FlowStmtResultPart okPart = new FlowStmtResultPart(s, fl.toString(), true, false, exc,
							alias.getFileName());
					okPart.setAlias(alias.getNoAlias());
					stmtResult.addPart(okPart);
					cfc.out.println("ok:" + s.flow);
				}
			}
		}

		if (!toInfere.isEmpty()) {
			cfc.stats = new CheckFlowConfig.Stats();
			
			for (final FlowStmtResultPart part : toInfere) {
				// special case => try inference of valid alias configurations
				CheckFlowLessWithAlias.inferValidAliasConfigurations(cfc, alias, part.getBasicStmt(), match, mInfo, stmtResult, exc, progress);
			}
			
			run.timeAdjustSDG += cfc.stats.totalTime;
			run.numAdjustSDGs += cfc.stats.adjustments;
		}
	}
	
	private static AnalysisScope createAnalysisScope(final Config cfg) throws IOException {
		final AnalysisScope scope = AnalysisScopeReader.makePrimordialScope(null);

		if (cfg.nativesXML != null) {
			com.ibm.wala.ipa.callgraph.impl.Util.setNativeSpec(cfg.nativesXML);
		}

		// if use stubs
		if (cfg.stubs != null) {
			scope.addToScope(ClassLoaderReference.Primordial, findJarModule(cfg.stubs));
		}

		// Nimmt unnoetige Klassen raus
		final SetOfClasses exclusions = new FileOfClasses(new ByteArrayInputStream(cfg.exclusions.getBytes()));
		scope.setExclusions(exclusions);

	    final ClassLoaderReference loader = scope.getLoader(AnalysisScope.APPLICATION);
	    AnalysisScopeReader.addClassPathToScope(cfg.classpath, scope, loader, cfg.classpathAddEntriesFromMANIFEST);

	    return scope;
	}

	/**
	 * Search file in filesystem. If not found, try to load from classloader (e.g. from inside the jarfile).
	 */
	private static Module findJarModule(final String path) throws IOException {
		final File f = new File(path);
		if (f.exists()) {
			return new JarFileModule(new JarFile(f));
		} else {
			final URL url = CheckFlowLessWithAlias.class.getClassLoader().getResource(path);
			if (url != null) {
				final URLConnection con = url.openConnection();
				final InputStream in = con.getInputStream();
				return new JarStreamModule(new JarInputStream(in));
			} else {
				// special fall-back for eclipse plug-in
				final URL url2 = new URL("platform:/plugin/joana.contrib.lib/stubs" + path);
				final InputStream in = url2.openConnection().getInputStream();
				return new JarStreamModule(in);
			}
		}
	}

	private static class SDGResult {
		public final SDG sdg;
		public final APResult ap;
		
		public SDGResult(final SDG sdg, final APResult ap) {
			this.sdg = sdg;
			this.ap = ap;
		}
	}
	
	private static SDGResult create(final Run run, final AnalysisScope scope, final CallGraphResult cg)
			throws IOException, ClassHierarchyException, UnsoundGraphException, CancelException {
		if (!Main.checkOrCreateOutputDir(run.outputDir)) {
			out.println("Could not access/create diretory '" + run.outputDir +"'");
			return null;
		}

		final MoJo mojo = setup.mojo;
		final IMethod im = run.im;
		
		out.print("\tbuilding system dependence graph... ");

		final ExternalCallCheck chk = ExternalCallCheck.EMPTY;
		final SDGBuilder.SDGBuilderConfig scfg = new SDGBuilder.SDGBuilderConfig();
		scfg.out = out;
		scfg.scope = scope;
		scfg.cache = cg.cache;
		scfg.cha = mojo.getHierarchy();
		scfg.entry = im;
		scfg.ext = chk;
		scfg.immutableNoOut = Main.IMMUTABLE_NO_OUT;
		scfg.immutableStubs = Main.IMMUTABLE_STUBS;
		scfg.ignoreStaticFields = Main.IGNORE_STATIC_FIELDS;
		scfg.exceptions = ExceptionAnalysis.IGNORE_ALL;
		scfg.accessPath = true;
		scfg.prunecg = Main.DEFAULT_PRUNE_CG;
		scfg.pts = PointsToPrecision.INSTANCE_BASED;
		scfg.debugAccessPath = true;
		scfg.debugAccessPathOutputDir = run.outputDir;
		scfg.computeInterference = false;
		scfg.staticInitializers = StaticInitializationTreatment.NONE;
		scfg.debugStaticInitializers = false;
		scfg.fieldPropagation = FieldPropagation.OBJ_TREE_AP;
		scfg.mergeFieldsOfPrunedCalls = false;
		scfg.debugManyGraphsDotOutput = false;

		final SDGBuilder sdg = SDGBuilder.create(scfg, cg.cg, cg.pts);
		
		final APResult apr = sdg.getAPResult();

		final SDG joanaSDG = SDGBuilder.convertToJoana(out, sdg, NullProgressMonitor.INSTANCE);

		AccessPath.computeMinMaxAliasSummaryEdges(out, sdg, sdg.getMainPDG(), joanaSDG, NullProgressMonitor.INSTANCE);

		out.print("\n\tsystem dependence graph done.");

		out.print("\n\twriting SDG to disk... ");
		joanaSDG.setFileName(run.name != null ? sdg.getMainMethodName() + "-" + run.name : sdg.getMainMethodName());
		final String fileName =	(run.outputDir.endsWith(File.separator)
			? run.outputDir : run.outputDir + File.separator) + joanaSDG.getFileName() + ".pdg";
		final File file = new File(fileName);
		out.print("(" + file.getAbsolutePath() + ") ");
		final PrintWriter pw = new PrintWriter(file);
		SDGSerializer.toPDGFormat(joanaSDG, pw);
		out.println("done.");

		return new SDGResult(joanaSDG, apr);
	}

}
