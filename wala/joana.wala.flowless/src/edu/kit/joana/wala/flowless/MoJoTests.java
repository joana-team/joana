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
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.viz.DotUtil;
import edu.kit.joana.wala.flowless.pointsto.GraphAnnotater;
import edu.kit.joana.wala.flowless.pointsto.MatchPTSWithFlowLess;
import edu.kit.joana.wala.flowless.pointsto.PointsToSetBuilder;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter;
import edu.kit.joana.wala.flowless.pointsto.SideEffectApproximator;
import edu.kit.joana.wala.flowless.pointsto.AliasGraph.MayAliasGraph;
import edu.kit.joana.wala.flowless.pointsto.AliasGraph.NoMayAliasGraph;
import edu.kit.joana.wala.flowless.pointsto.GraphAnnotater.Aliasing;
import edu.kit.joana.wala.flowless.pointsto.PointsToSetBuilder.PointsTo;
import edu.kit.joana.wala.flowless.pointsto.PointsToSetBuilder.PtsElement;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter.RootParameter;
import edu.kit.joana.wala.flowless.spec.FlowLessSimpleSemantic;
import edu.kit.joana.wala.flowless.spec.FlowLessSimplifier;
import edu.kit.joana.wala.flowless.spec.FlowLessSimplifier.BasicIFCStmt;
import edu.kit.joana.wala.flowless.spec.ast.IFCStmt;
import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor.FlowAstException;
import edu.kit.joana.wala.flowless.spec.java.LightweightParser;
import edu.kit.joana.wala.flowless.spec.java.ast.ClassInfo;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo;
import edu.kit.joana.wala.flowless.util.ExtendedNodeDecorator;
import edu.kit.joana.wala.flowless.util.Util;
import edu.kit.joana.wala.flowless.wala.EntryUtil;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class MoJoTests {

	private static final boolean NO_SUBCLASSES = true;

	public static void analyze(String pathToSrc, String pathToBin) throws IOException, FlowAstException, ClassHierarchyException, IllegalArgumentException, CancelException {
		// parse all java files in src
		List<ClassInfo> allInfo = MoJo.parseSourceFiles(pathToSrc);
		MoJo.prepareFlowLessStmts(allInfo);

		// run analysis on bin
		System.out.print("Running class hierarchy analysis... ");
		MoJo mojo = MoJo.create(pathToBin);
//		AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(pathToBin, null);
//		ClassHierarchy cha = ClassHierarchy.make(scope);
		System.out.println("done. - " + mojo.getHierarchy().getNumberOfClasses() + " classes found.");

		// search for annotated methods
		for (ClassInfo cls : allInfo) {
			System.out.println("Running analysis on " + cls);
			for (MethodInfo method : cls.getMethods()) {
				if (method.hasIFCStmts()) {
					System.out.println("\tFound annotated method: " + method);
					try {
						runAnalyisForMethod(method, mojo.getHierarchy());
					} catch (FlowAstException exc) {
						System.out.println("ERROR: " + exc.getMessage());
						System.out.println("Skipping " + method);
					}
				}
			}
		}
	}


	private static void runAnalyisForMethod(MethodInfo method, IClassHierarchy cha) throws FlowAstException, IllegalArgumentException, CancelException {
		final AnalysisScope scope = cha.getScope();

		// running simple semantics phase to match parameter roots with parameter numbers
		System.out.println("Running simple semantic check on '" + method.getName() + "'");
		FlowLessSimpleSemantic.check(method);

		// search method in hierarchy
		TypeName clsName = TypeName.findOrCreate(method.getClassInfo().getWalaBytecodeName());
		TypeReference clsRef = TypeReference.findOrCreate(ClassLoaderReference.Application, clsName);
		System.out.print("Looking for class " + clsRef + "... ");
		IClass cls = cha.lookupClass(clsRef);
		if (cls != null) {
			System.out.println("found it! ");
		} else {
			System.out.println("not found :( - aborting.");
			return;
		}

		// search for method in class
		System.out.println("Looking for methods named '" + method.getName() + "'");
		List<IMethod> possible = findPossibleMatches(cls.getAllMethods(), method.getName());
		for (IMethod m : possible) {
			System.out.println("\tFound possible match: " + m.toString());
			Set<IMethod> possibleReferences = cha.getPossibleTargets(cls, m.getReference());
			if (possibleReferences.size() > 1) {
				System.out.println("\tThis method has been overwritten by a subclass. Dont know how to handle this. Aborting...");
				return;
			}

			System.out.print("\tComputing maximal input parameter tree for method...");
			SideEffectApproximator pts = new SideEffectApproximator(cha);
			RootParameter[] params = pts.createInputParamTrees(m);
			System.out.println("done.");
			for (RootParameter root : params) {
				System.out.println("\t\t" + root);
			}

			NoMayAliasGraph aliases = pts.createNoMayAliasGraph(params, m.isStatic());
			writeDotFile(aliases, m, "initial");
			MayAliasGraph maximalAliases = aliases.constructNegated();
			writeDotFile(maximalAliases, m, "negated");

			{
				System.out.print("\tComputing maximal output parameter tree for method...");
				RootParameter[] paramsOut = pts.createOutputParamTrees(m);
				System.out.println("done.");
				for (RootParameter rootOut : paramsOut) {
					System.out.println("\t\t" + rootOut);
				}

				NoMayAliasGraph aliasesOut = pts.createNoMayAliasGraph(paramsOut, m.isStatic());
				writeDotFile(aliasesOut, m, "out-initial");
				MayAliasGraph invertedOut = aliasesOut.constructNegated();
				writeDotFile(invertedOut, m, "out-negated");
			}



			System.out.println("\tSearching matching nodes in alias graph....");
			// check param matching
			for (IFCStmt ifc : method.getIFCStmts()) {
				System.out.println("\t\tMatching " + ifc);
				MatchPTSWithFlowLess.findMatchingParams(aliases, ifc);
			}

			String methodStr = method.getName() + "_ifc";
			int ifcCounter = 0;
			for (IFCStmt ifc : method.getIFCStmts()) {
				ifcCounter++;
				final String prefix = methodStr + ifcCounter;
				System.out.println("\tWorking on: " + ifc);
				List<BasicIFCStmt> simplified = FlowLessSimplifier.simplify(ifc);
				BasicIFCStmt upperBound = FlowLessSimplifier.upperBound(simplified);
				System.out.println("\t\tUpper bound: " + upperBound);
				BasicIFCStmt lowerBound = FlowLessSimplifier.lowerBound(simplified);
				System.out.println("\t\tLower bound: " + lowerBound);
				{
					Aliasing resultUpper = GraphAnnotater.computeBounds(maximalAliases, upperBound);
					writeDotFile(resultUpper.upperBound, m, prefix + "-upper-up");
					writeDotFile(resultUpper.lowerBound, m, prefix + "-upper-low");
				}
				{
					Aliasing resultLower = GraphAnnotater.computeBounds(maximalAliases, lowerBound);
					writeDotFile(resultLower.upperBound, m, prefix + "-lower-up");
					writeDotFile(resultLower.lowerBound, m, prefix + "-lower-low");
				}

				Aliasing result = GraphAnnotater.computeBounds(maximalAliases, simplified);
				writeDotFile(result.upperBound, m, prefix + "-up");
				writeDotFile(result.lowerBound, m, prefix + "-low");
				NoMayAliasGraph noMayUpper = result.upperBound.constructNegated();
				writeDotFile(noMayUpper, m, prefix + "-negated-up");

				NoMayAliasGraph mergedNoMayUpper = PointsToSetBuilder.createMergedNoAlias(result.upperBound);
				writeDotFile(mergedNoMayUpper, m, prefix + "-merged-negated-up");

				MayAliasGraph mergedMayUpper = mergedNoMayUpper.constructNegated();
				writeDotFile(mergedMayUpper, m, prefix + "-merged-up");

				System.out.println("Building upper bound...");
				computePointsToCallGraphAndIR(result.upperBound, scope, cha, m);
				System.out.println("Building lower bound...");
				computePointsToCallGraphAndIR(result.lowerBound, scope, cha, m);
			}

		}

	}

	private static void computePointsToCallGraphAndIR(MayAliasGraph aliases, AnalysisScope scope, IClassHierarchy cha, IMethod m) throws IllegalArgumentException, CancelException {
		PointsTo pointsTo = PointsToSetBuilder.compute(aliases);
		for (PtsParameter p : aliases) {
			Set<PtsElement> ptsOfP = pointsTo.getPointsTo(p);
			System.out.print("PTS(" + p + ") = ");
			for (PtsElement elem : ptsOfP) {
				System.out.print(elem + " ");
			}
			System.out.println();
		}

		AnalysisOptions options = EntryUtil.createAnalysisOptionsWithPTS(cha, m, pointsTo, NO_SUBCLASSES);
		AnalysisCache cache = new AnalysisCache();
		SSAPropagationCallGraphBuilder builder =
			com.ibm.wala.ipa.callgraph.impl.Util.makeVanillaZeroOneCFABuilder(options, cache, cha, scope);

		System.out.print("Computing call graph with annotated root method... ");
		CallGraph cg = builder.makeCallGraph(options);
		System.out.println("done.");

		IR fakeIR = cg.getFakeRootNode().getIR();
		Util.dumpSSA(fakeIR, System.out);
	}

	private static List<IMethod> findPossibleMatches(Collection<IMethod> methods, String methodName) {
		List<IMethod> matches = new LinkedList<IMethod>();

		for (IMethod m : methods) {
			if (m.getName().toString().equals(methodName)) {
				matches.add(m);
			}
		}

		return matches;
	}

	public static void main(String[] args) throws IOException, FlowAstException, ClassHierarchyException, IllegalArgumentException, CancelException {
		MoJoTests.analyze("examples/project1", "bin");
	}

	private static String OUT_DIR = "out/";

	private static void writeDotFile(Graph<PtsParameter> graph, IMethod m, String nameSuffix) {
		final IClass cls = m.getDeclaringClass();

		try {
			String name = cls.getName().getClassName() + "." + m.getSelector().getName() + "_"
				+ m.getNumberOfParameters() + (nameSuffix == null ? "" : "_" + nameSuffix);
			DotUtil.writeDotFile(graph, new ExtendedNodeDecorator.DefaultImpl<PtsParameter>(),
				graph.getClass().getSimpleName() + " of " + name, OUT_DIR + name + ".dot");
		} catch (WalaException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
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
			System.out.println("<<<<<<<< Parsing " + f.getAbsolutePath() + File.separator + file);
			result.addAll(LightweightParser.parseFile(f.getAbsolutePath() + File.separator + file));
		}

		return result;
	}

}
