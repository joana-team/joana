/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DefaultSSAInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.viz.DotUtil;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.wala.flowless.pointsto.AliasGraph.AliasGraphException;
import edu.kit.joana.wala.flowless.pointsto.AliasGraph.MayAliasGraph;
import edu.kit.joana.wala.flowless.pointsto.AliasGraph.NoMayAliasGraph;
import edu.kit.joana.wala.flowless.pointsto.GraphAnnotater;
import edu.kit.joana.wala.flowless.pointsto.GraphAnnotater.Aliasing;
import edu.kit.joana.wala.flowless.pointsto.MatchPTSWithFlowLess;
import edu.kit.joana.wala.flowless.pointsto.MatchPTSWithFlowLess.ParameterMatchException;
import edu.kit.joana.wala.flowless.pointsto.PointsToSetBuilder;
import edu.kit.joana.wala.flowless.pointsto.PointsToSetBuilder.PointsTo;
import edu.kit.joana.wala.flowless.pointsto.PointsToSetBuilder.PtsElement;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter.RootParameter;
import edu.kit.joana.wala.flowless.pointsto.SideEffectApproximator;
import edu.kit.joana.wala.flowless.spec.FlowLessBuilder;
import edu.kit.joana.wala.flowless.spec.FlowLessBuilder.FlowError;
import edu.kit.joana.wala.flowless.spec.FlowLessSimpleSemantic;
import edu.kit.joana.wala.flowless.spec.FlowLessSimplifier;
import edu.kit.joana.wala.flowless.spec.FlowLessSimplifier.BasicIFCStmt;
import edu.kit.joana.wala.flowless.spec.ast.BooleanAliasStmt;
import edu.kit.joana.wala.flowless.spec.ast.ExplicitFlowStmt;
import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor;
import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor.FlowAstException;
import edu.kit.joana.wala.flowless.spec.ast.FlowStmt;
import edu.kit.joana.wala.flowless.spec.ast.IFCStmt;
import edu.kit.joana.wala.flowless.spec.ast.InferableAliasStmt;
import edu.kit.joana.wala.flowless.spec.ast.Parameter;
import edu.kit.joana.wala.flowless.spec.ast.ParameterOptList;
import edu.kit.joana.wala.flowless.spec.ast.PrimitiveAliasStmt;
import edu.kit.joana.wala.flowless.spec.ast.PureStmt;
import edu.kit.joana.wala.flowless.spec.ast.SimpleParameter;
import edu.kit.joana.wala.flowless.spec.ast.SimpleParameter.Part;
import edu.kit.joana.wala.flowless.spec.ast.UniqueStmt;
import edu.kit.joana.wala.flowless.spec.java.LightweightParser;
import edu.kit.joana.wala.flowless.spec.java.ast.ClassInfo;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo;
import edu.kit.joana.wala.flowless.util.ExtendedNodeDecorator;
import edu.kit.joana.wala.flowless.util.GraphWriter;
import edu.kit.joana.wala.flowless.util.Util;
import edu.kit.joana.wala.flowless.wala.EntryUtil;
import edu.kit.joana.wala.util.ParamNum;
import edu.kit.joana.wala.util.pointsto.ObjSensContextSelector;
import edu.kit.joana.wala.util.pointsto.ObjSensZeroXCFABuilder;

/**
 * This class is the main entrypoint for the Modular Joana (MoJo) framework. It lets you
 * parse java sourcecode files for FlowLess (tm) ifc annotations and it provides tools
 * to compute alias configurations from these annotations.
 * It further provides tools to convert these alias configurations into concrete points-to
 * sets and lets you build callgraphs that use this information as their initial
 * points-to configuration.
 *
 * A normal usage of this class would look like this:
 * <pre>
 * List<ClassInfo> clsInfos = MoJo.parseSourceFiles("src/");
 * MoJo.checkForSyntacticErrors(clsInfos);
 * MoJo mojo = MoJo.create("bin/");
 * for (ClassInfo cls : clsInfos) {
 *     for (MethodInfo m : cls) {
 *         IMethod method = mojo.findMethod(m);
 *
 *         if (m.hasIFCStmts()) {
 *             for (IFCStmt ifc : m) {
 *                 GraphAnnotater.Aliasing mayAlias = mojo.computeMayAliasGraphs(m, ifc);
 *
 *                 PointsTo ptsMin = MoJo.computePointsTo(mayAlias.lowerBound);
 *                 AnalysisOptions optPtsMin = mojo.createAnalysisOptionsWithPTS(ptsMin, method);
 *                 CallGraphResult minCG = mojo.computeContextSensitiveCallGraph(optPtsMin);
 *                 // run analysis on callgraph with minimal alias configuration
 *                 ...
 *
 *                 PointsTo ptsMax = MoJo.computePointsTo(mayAlias.upperBound);
 *                 AnalysisOptions optPtsMax = mojo.createAnalysisOptionsWithPTS(ptsMax, method);
 *                 CallGraphResult maxCG = mojo.computeContextSensitiveCallGraph(optPtsMax);
 *                 // run analysis on callgraph with maximal alias configuration
 *                 ...
 *             }
 *         }
 *     }
 * }
 * </pre>
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class MoJo {

	private static final boolean NO_SUBCLASSES = true;
	private static final String DEFAULT_OUT_DIR = "out/";

	private static final Logger debug = Log.getLogger(Log.L_MOJO_DEBUG);
	
	private final IClassHierarchy cha;
	private final String outDir;

	private MoJo(IClassHierarchy cha, String outDir) {
		this.cha = cha;
		this.outDir = outDir;
	}

	/**
	 * Parses all .java files in the given directory and all its subdirectories.
	 * @param srcDir Directory where the .java files are.
	 * @return A list of ClassInfo elements that contain the parsed FlowLess ifc
	 * annotations for each method.
	 * @throws IOException
	 */
	public static List<ClassInfo> parseSourceFiles(String srcDir) throws IOException {
		return parseJavaFilesInAllSubDirs(srcDir);
	}

	/**
	 * Parses ifc statements and checks them for simple errors, like references to parameter
	 * names that do not exists.
	 * @param info List of ClassInfo objects that is checked for errors.
	 * @return The number of errors found during parsing.
	 */
	public static int prepareFlowLessStmts(List<ClassInfo> info) {
		int errorsFound = 0;

		assert debug("Parsing flow statements...");

		FlowLessBuilder.clearErrors();
		FlowLessBuilder.checkForFlowStatements(info);

		if (FlowLessBuilder.hadErrors()) {
			assert debug("Found " + FlowLessBuilder.numberOfErrors() + " errors in flow statements.");
			errorsFound += FlowLessBuilder.numberOfErrors();
		}

		for (ClassInfo cls : info) {
			for (MethodInfo method : cls.getMethods()) {
				// running simple semantics phase to match parameter roots with parameter numbers
				assert debug("Running simple semantic check on '" + method.getName() + "'");

				try {
					FlowLessSimpleSemantic.check(method);
				} catch (FlowAstException exc) {
					errorsFound++;
					FlowError err = new FlowError(method, exc);
					method.addError(err);
				}
			}
		}

		return errorsFound;
	}

	/**
	 * Create a new instance of MoJo for a program. This may take some time, as a class hierarchy analysis
	 * is run internally.
	 * @param binPath Path to the .class files of the program.
	 * @return A new instance of MoJo.
	 * @throws IOException If something during reading of the .class files goes wrong.
	 * @throws ClassHierarchyException If the class hierarchy analysis goes wrong.
	 */
	public static MoJo create(String binPath) throws IOException, ClassHierarchyException {
		return create(binPath, DEFAULT_OUT_DIR);
	}

	/**
	 * Create a new instance of MoJo for a program. This may take some time, as a class hierarchy analysis
	 * is run internally.
	 * @param binPath Path to the .class files of the program.
	 * @param outPath Path where the intermediate computation results are stored. E.g. like alias graphs.
	 * @return A new instance of MoJo.
	 * @throws IOException If something during reading of the .class files goes wrong.
	 * @throws ClassHierarchyException If the class hierarchy analysis goes wrong.
	 */
	public static MoJo create(String binPath, String outPath) throws IOException, ClassHierarchyException {
		assert debug("Running class hierarchy analysis... ");

		AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(binPath, null);
		ClassHierarchy cha = ClassHierarchy.make(scope);

		assert debug("done. - " + cha.getNumberOfClasses() + " classes found.");

		return create(cha, outPath);
	}

	/**
	 * Create a new instance of MoJo for a program defined by its class hierarchy.
	 * @param cha The class hierarchy of the program.
	 * @return A new instance of MoJo.
	 */
	public static MoJo create(IClassHierarchy cha) {
		return create(cha, DEFAULT_OUT_DIR);
	}

	/**
	 * Create a new instance of MoJo for a program defined by its class hierarchy.
	 * @param cha The class hierarchy of the program.
	 * @param outDir Path where the intermediate computation results are stored. E.g. like alias graphs.
	 * @return A new instance of MoJo.
	 */
	public static MoJo create(IClassHierarchy cha, String outDir) {
		return new MoJo(cha, outDir);
	}

	/**
	 * Compute the maximal and minimal alias configurations that are implied through the
	 * given alias annotation of the given method. This methods writes the upper and lower bound
	 * graphs to the path that can be configured when MoJo is created. Per default it is "out/".
	 *
	 * @param method The method an alias configuration is computed for.
	 * @param stmt The ifc statement that implies the alias configuration.
	 * @return The result contains the maximal and minimal alias configurations in form of alias graphs.
	 * @throws NoSuchElementException Occurs if the method could not be found in the class hierarchy.
	 * @throws FlowAstException Occurs if the annotations do not make sense.
	 * @throws AliasGraphException
	 */
	public GraphAnnotater.Aliasing computeMayAliasGraphs(final MethodInfo method, final IFCStmt stmt) throws NoSuchElementException, FlowAstException, AliasGraphException {
		final IMethod im = findMethod(method);

		assert debug("Found possible match: " + im);
		{
			// check no override
			Set<IMethod> possibleReferences = cha.getPossibleTargets(im.getDeclaringClass(), im.getReference());

			if (possibleReferences.size() > 1) {
				throw new IllegalStateException("This method has been overwritten by a subclass. Dont know how to handle this. Aborting...");
			}
		}

		checkReturnAndExceptionStmts(im, stmt);

		assert debug("\tComputing maximal input parameter tree for method...");

		final SideEffectApproximator pts = new SideEffectApproximator(cha);
		final RootParameter[] params = pts.createInputParamTrees(im);

		assert debug("done.");
		if (debug.isEnabled()) {
			for (RootParameter root : params) {
				assert debug("\t\t" + root);
			}
		}

		int indexOfstmt = 0;
		for (IFCStmt ifc : method.getIFCStmts()) {
			indexOfstmt++;
			if (ifc.equals(stmt)) {
				break;
			}
		}

		final String prefix = method.getName() + "-" + indexOfstmt;
		assert debug("Working on: " + stmt);

		List<BasicIFCStmt> simplified = FlowLessSimplifier.simplify(stmt);

		NoMayAliasGraph aliases = pts.createNoMayAliasGraph(params, im.isStatic());
		MayAliasGraph maximalAliases = aliases.constructNegated();
		maximalAliases.verify();
		Aliasing result = GraphAnnotater.computeBounds(maximalAliases, simplified);

		assert debug("Bounds created. Writing them to disk.");

		writeDotFile(result.upperBound, im, prefix + "-up");
		writeDotFile(result.lowerBound, im, prefix + "-low");

		assert debug("done.");

		return result;
	}

	public GraphAnnotater.Aliasing computeMinMaxAliasing(IMethod im) {
		assert debug("\tComputing maximal input parameter tree for method...");

		final SideEffectApproximator pts = new SideEffectApproximator(cha);
		final RootParameter[] params = pts.createInputParamTrees(im);

		assert debug("done.");
		if (debug.isEnabled()) {
			for (RootParameter root : params) {
				assert debug("\t\t" + root);
			}
		}

		NoMayAliasGraph aliases = pts.createNoMayAliasGraph(params, im.isStatic());
		MayAliasGraph maximalAliases = aliases.constructNegated();

		Aliasing minMax = GraphAnnotater.computeBounds(maximalAliases);

		return minMax;
	}

	private void checkReturnAndExceptionStmts(final IMethod im, IFCStmt ifc) throws FlowAstException {
		FlowAstVisitor checkRetAndExc = new FlowAstVisitor() {

			private boolean fieldsAreOk(TypeReference type, SimpleParameter param) {
				if (type.isPrimitiveType()) {
					if (param.getParts().size() == 1) {
						return true;
					} else {
						return false;
					}
				}

				IClass cls = cha.lookupClass(type);
				List<Part> l = new LinkedList<Part>(param.getParts());
				l.remove(0);

				while (!l.isEmpty()) {
					final Part p = l.remove(0);

					if (p.getType() == Part.Type.WILDCARD) {
						return true;
					}

					final Atom fieldName = Atom.findOrCreate(p.name.getBytes());
					IField field = cls.getField(fieldName);

					while (field == null) {
						// lookup in superclass
						cls = cls.getSuperclass();
						if (cls == null) {
							return false;
						} else {
							field = cls.getField(fieldName);
						}
					}

					final TypeReference fieldType = field.getFieldTypeReference();
					cls = cha.lookupClass(fieldType);
				}

				return true;
			}

			private void checkReturnFieldsOk(SimpleParameter param) throws ParameterMatchException {
				final TypeReference retType = im.getReturnType();
				if (retType == null || retType == TypeReference.Void) {
					throw new MatchPTSWithFlowLess.ParameterMatchException(param, "This method has a void return type.");
				}

				if (!fieldsAreOk(retType, param)) {
					throw new MatchPTSWithFlowLess.ParameterMatchException(param, "Could not resolve all parameter fields.");
				}
			}

			private void checkExceptionFieldsOk(SimpleParameter param) throws ParameterMatchException,
					UnsupportedOperationException, InvalidClassFileException {
				final TypeReference[] excs = im.getDeclaredExceptions();
				if (excs != null) {
					boolean fieldsOk = false;
					for (TypeReference exc : excs) {
						if (fieldsAreOk(exc, param)) {
							fieldsOk = true;
							break;
						}
					}

					if (!fieldsOk) {
						throw new MatchPTSWithFlowLess.ParameterMatchException(param,
								"Could not find specified fields of exception value.");
					}
				} else if (param.getParts().size() > 1) {
					throw new MatchPTSWithFlowLess.ParameterMatchException(param, "Could not find the exception value.");
				}
			}

			@Override
			public void visit(PrimitiveAliasStmt alias) throws FlowAstException {}

			@Override
			public void visit(UniqueStmt unique) throws FlowAstException {
				for (Parameter sp : unique.getParams()) {
					sp.accept(this);
				}
			}

			@Override
			public void visit(BooleanAliasStmt alias) throws FlowAstException {}

			@Override
			public void visit(SimpleParameter param) throws FlowAstException {
				final ParamNum pn = param.getMappedTo();
				if (pn.isException()) {
					try {
						checkExceptionFieldsOk(param);
					} catch (UnsupportedOperationException e) {
						throw new MatchPTSWithFlowLess.ParameterMatchException(param, e);
					} catch (InvalidClassFileException e) {
						throw new MatchPTSWithFlowLess.ParameterMatchException(param, e);
					}
				} else if (pn.isResult()) {
					checkReturnFieldsOk(param);
				}
			}

			@Override
			public void visit(ParameterOptList param) throws FlowAstException {
				for (SimpleParameter p : param.getParams()) {
					p.accept(this);
				}
			}

			@Override
			public void visit(IFCStmt ifc) throws FlowAstException {
				for (FlowStmt flow : ifc.getFlowStmts()) {
					flow.accept(this);
				}
			}

			@Override
			public void visit(ExplicitFlowStmt ifc) throws FlowAstException {
				for (SimpleParameter sp : ifc.getTo()) {
					sp.accept(this);
				}
			}

			@Override
			public void visit(PureStmt ifc) throws FlowAstException {
				for (SimpleParameter sp : ifc.getParams()) {
					sp.accept(this);
				}
			}

			@Override
			public void visit(InferableAliasStmt alias) throws FlowAstException {
			}

		};

		for (FlowStmt flow : ifc.getFlowStmts()) {
			flow.accept(checkRetAndExc);
		}
	}

	/**
	 * Computes a points-to set for each node (parameter or field) of the alias
	 * graph that conforms to its alias specification. Iff a <-> b are connected in the
	 * may alias graph <=> points-to(a) and points-to(b) share at least a single element.
	 *
	 * @param graph The alias graph that specifies the alias configuration.
	 * @return A points-to set configuration that conforms to the alias specification.
	 */
	public static PointsTo computePointsTo(final MayAliasGraph graph) {
		return computePointsTo(graph, new GraphWriter.NoOutput<PtsParameter>());
	}

	/**
	 * Computes a points-to set for each node (parameter or field) of the alias
	 * graph that conforms to its alias specification. Iff a <-> b are connected in the
	 * may alias graph <=> points-to(a) and points-to(b) share at least a single element.
	 *
	 * @param graph The alias graph that specifies the alias configuration.
	 * @param gWriter A graph writer that can be used to output the created merged alias graphs.
	 * @return A points-to set configuration that conforms to the alias specification.
	 */
	public static PointsTo computePointsTo(final MayAliasGraph graph, final GraphWriter<PtsParameter> gWriter) {
		final PointsTo pointsTo = PointsToSetBuilder.compute(graph, gWriter);

		if (debug.isEnabled()) {
			for (PtsParameter p : graph) {
				Set<PtsElement> ptsOfP = pointsTo.getPointsTo(p);
				debug.out("PTS(" + p + ") = ");
				for (PtsElement elem : ptsOfP) {
					debug.out(elem + " ");
				}
				debug.outln("");
			}
		}

		return pointsTo;
	}

	/**
	 * Creates analysis options that may be used for call graph creation for a given method
	 * and a given initial points-to configuration. The resulting alias options will trigger
	 * the creation of initial code that resembles the points-to configuration.
	 * @param pts The points-to configuration.
	 * @param method The method that should be analyzed.
	 * @return The analysis options that contain the method and the initial points-to configuration.
	 */
	public AnalysisOptions createAnalysisOptionsWithPTS(PointsTo pts, IMethod method) {
		final AnalysisOptions options = EntryUtil.createAnalysisOptionsWithPTS(cha, method, pts, NO_SUBCLASSES);

		return options;
	}

	public static class CallGraphResult {
		public final CallGraph cg;
		public final PointerAnalysis<InstanceKey> pts;
		public final AnalysisCache cache;

		public CallGraphResult(CallGraph cg, PointerAnalysis<InstanceKey> pts, AnalysisCache cache) {
			this.cg = cg;
			this.cache = cache;
			this.pts = pts;
		}
	}

	/**
	 * Creates a context insensitive call graph for the given analysis options. It uses the
	 * 0-CFA points-to analysis.
	 * @param options The analysis options that should be used.
	 * @return The call graph.
	 * @throws IllegalArgumentException
	 * @throws CancelException
	 */
	public CallGraphResult computeContextInsensitiveCallGraph(AnalysisOptions options) throws IllegalArgumentException, CancelException {
		AnalysisCache cache = new AnalysisCache();
		SSAPropagationCallGraphBuilder builder =
			com.ibm.wala.ipa.callgraph.impl.Util.makeZeroCFABuilder(options, cache, cha, cha.getScope());

		CallGraph cg = computeCallGraph(options, builder);

		CallGraphResult result = new CallGraphResult(cg, builder.getPointerAnalysis(), builder.getAnalysisCache());

		return result;
	}

	/**
	 * Creates a context sensitive call graph for the given analysis options. It uses the
	 * vanilla-0-1-CFA points-to analysis.
	 * @param options The analysis options that should be used.
	 * @return The call graph.
	 * @throws IllegalArgumentException
	 * @throws CancelException
	 */
	public CallGraphResult computeContextSensitiveCallGraph(AnalysisOptions options) throws IllegalArgumentException, CancelException {
		AnalysisCache cache = new AnalysisCache();
		SSAPropagationCallGraphBuilder builder =
			com.ibm.wala.ipa.callgraph.impl.Util.makeVanillaZeroOneCFABuilder(options, cache, cha, cha.getScope());

		CallGraph cg = computeCallGraph(options, builder);

		CallGraphResult result = new CallGraphResult(cg, builder.getPointerAnalysis(), builder.getAnalysisCache());

		return result;
	}

	/**
	 * Creates an object sensitive call graph for the given analysis options. It uses the
	 * vanilla-0-1-CFA points-to analysis as a basis and clones methods for each distinguishable
	 * receiver object.
	 * @param options The analysis options that should be used.
	 * @return The call graph.
	 * @throws IllegalArgumentException
	 * @throws CancelException
	 */
	public CallGraphResult computeObjectSensitiveCallGraph(AnalysisOptions options,
			ObjSensZeroXCFABuilder.MethodFilter filter) throws IllegalArgumentException, CancelException {
		AnalysisCache cache = new AnalysisCache();
		SSAPropagationCallGraphBuilder builder = makeObjectSens(options, cache, cha, cha.getScope(), filter);

		CallGraph cg = computeCallGraph(options, builder);

		CallGraphResult result = new CallGraphResult(cg, builder.getPointerAnalysis(), builder.getAnalysisCache());

		return result;
	}

	private static SSAPropagationCallGraphBuilder makeObjectSens(AnalysisOptions options, AnalysisCache cache,
		      IClassHierarchy cha, AnalysisScope scope, ObjSensZeroXCFABuilder.MethodFilter filter) {

	    if (options == null) {
	      throw new IllegalArgumentException("options is null");
	    }
	    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha);
	    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha);

	    final ContextSelector defaultSelector = new DefaultContextSelector(options, cha);

	    return ObjSensZeroXCFABuilder.make(cha, options, cache, new ObjSensContextSelector(defaultSelector, filter),
				new DefaultSSAInterpreter(options, cache),
				ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.CONSTANT_SPECIFIC | ZeroXInstanceKeys.SMUSH_MANY
	    		| ZeroXInstanceKeys.SMUSH_THROWABLES);
	}

	/**
	 * Creates a context sensitive call graph for the given analysis options. It uses the
	 * vanilla-0-1-container-CFA points-to analysis that contains optimizations for container classes.
	 * @param options The analysis options that should be used.
	 * @return The call graph.
	 * @throws IllegalArgumentException
	 * @throws CancelException
	 */
	public CallGraphResult computeContextSensitiveWithContainerOptimizationCallGraph(AnalysisOptions options) throws IllegalArgumentException, CancelException {
		AnalysisCache cache = new AnalysisCache();
		SSAPropagationCallGraphBuilder builder =
			com.ibm.wala.ipa.callgraph.impl.Util.makeVanillaZeroOneContainerCFABuilder(options, cache, cha, cha.getScope());

		CallGraph cg = computeCallGraph(options, builder);

		CallGraphResult result = new CallGraphResult(cg, builder.getPointerAnalysis(), builder.getAnalysisCache());

		return result;
	}

	public IClassHierarchy getHierarchy() {
		return cha;
	}

	private CallGraph computeCallGraph(AnalysisOptions options, SSAPropagationCallGraphBuilder builder) throws IllegalArgumentException, CancelException {
		assert debug("Computing call graph with annotated root method... ");

		CallGraph cg = builder.makeCallGraph(options);

		assert debug("done.");

		if (debug.isEnabled()) {
			IR fakeIR = cg.getFakeRootNode().getIR();
			Util.dumpSSA(fakeIR, System.out);
		}

		return cg;
	}

	public IMethod findMethod(final String method) throws NoSuchElementException {
		for (IClass cls : cha) {
			if (!cls.getClassLoader().toString().contains("Primordial") && !cls.isInterface()) {
				for (IMethod im : cls.getDeclaredMethods()) {
					final String sig = im.getSignature();
					if (!im.isAbstract() && method.equals(sig)) {
						return im;
					}
				}
			}
		}

		return null;
	}

	public IMethod findMethod(final MethodInfo method) throws NoSuchElementException {
		// search method in hierarchy
		TypeName clsName = TypeName.findOrCreate(method.getClassInfo().getWalaBytecodeName());
		TypeReference clsRef = TypeReference.findOrCreate(ClassLoaderReference.Application, clsName);

		assert debug("Looking for class " + clsRef + "... ");

		IClass cls = cha.lookupClass(clsRef);
		assert debug((cls != null ? "found it! " : "not found :( - aborting."));

		if (cls == null) {
			throw new NoSuchElementException("A class named " + clsRef + " does not exist.");
		}

		assert debug("Looking for methods named '" + method.getName() + "'");
		List<IMethod> possible = findPossibleMatches(cls.getAllMethods(), method);
		if (possible.size() != 1) {
			if (possible.size() == 0) {
				throw new NoSuchElementException("A method named " + method.getName() + " with "
						+ method.getParameters().size() + " params does not exist.");
			} else {
				throw new NoSuchElementException("A method named " + method.getName() + " with "
						+ method.getParameters().size() + " params exists more then once (" + possible.size()
						+ "). Dont know what to do.");
			}
		}

		return possible.get(0);
	}

	private static List<ClassInfo> parseJavaFilesInAllSubDirs(String dir) throws IOException {
		File f = new File(dir);
		if (!f.exists() || !f.isDirectory() | !f.canRead()) {
			throw new IllegalArgumentException(dir + " is not an existing and readable directory.");
		}

		List<ClassInfo> result = new LinkedList<ClassInfo>();

		result.addAll(parseJavaFilesInDir(dir));

		for (File fl : f.listFiles()) {
			if (fl.isDirectory()) {
				result.addAll(parseJavaFilesInAllSubDirs(fl.getAbsolutePath()));
			}
		}

		return result;
	}

	private static List<ClassInfo> parseJavaFilesInDir(String dir) throws IOException {
		File f = new File(dir);
		if (!f.exists() || !f.isDirectory() | !f.canRead()) {
			throw new IllegalArgumentException(dir + " is not an existing and readable directory.");
		}

		String[] files = f.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".java") && (new File(dir + File.separator + name)).isFile();
			}
		});

		List<ClassInfo> result = new LinkedList<ClassInfo>();

		for (String file : files) {
			assert debug("<<<<<<<< Parsing " + f.getAbsolutePath() + File.separator + file);

			result.addAll(LightweightParser.parseFile(f.getAbsolutePath() + File.separator + file));
		}

		return result;
	}

	private static List<IMethod> findPossibleMatches(Collection<IMethod> methods, MethodInfo method) {
		String methodName = method.getName();
		List<IMethod> matches = new LinkedList<IMethod>();

		for (IMethod m : methods) {
			if (m.getName().toString().equals(methodName)) {
				assert debug("Found method with name " + methodName + " -> checking for matching parameters.");

				if (m.getNumberOfParameters() == method.getParameters().size()) {
					assert debug("Params match, adding " + m);

					matches.add(m);
				}
			}
		}

		return matches;
	}

	private void writeDotFile(Graph<PtsParameter> graph, IMethod m, String nameSuffix) {
		final IClass cls = m.getDeclaringClass();

		try {
			String name = cls.getName().getClassName() + "." + m.getSelector().getName() + "_"
				+ m.getNumberOfParameters() + (nameSuffix == null ? "" : "_" + nameSuffix);
			DotUtil.writeDotFile(graph, new ExtendedNodeDecorator.DefaultImpl<PtsParameter>(), graph.getClass().getSimpleName()
				+ " of " + name, outDir + name + ".dot");
		} catch (WalaException e) {
			e.printStackTrace();
		}
	}


	private static boolean debug(Object obj) {
		debug.outln("[MoJo] " + obj);

		return true;
	}
}
