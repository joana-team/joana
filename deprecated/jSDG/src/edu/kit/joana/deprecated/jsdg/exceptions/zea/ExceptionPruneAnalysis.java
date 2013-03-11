/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.exceptions.zea;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cfg.EdgeFilter;
import com.ibm.wala.ipa.cfg.PrunedCFG;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.strings.StringStuff;

import edu.kit.joana.deprecated.jsdg.exceptions.ExceptionPrunedCFGAnalysis;
import edu.kit.joana.deprecated.jsdg.util.Log;

public class ExceptionPruneAnalysis extends ExceptionPrunedCFGAnalysis<SSAInstruction, IExplodedBasicBlock> {

    private static final String exclusionRegExp =
        "java\\/awt\\/.*\n"
        + "java\\/io\\/.*\n"
        + "javax\\/swing\\/.*\n"
        + "sun\\/awt\\/.*\n"
        + "sun\\/swing\\/.*\n"
        + "com\\/sun\\/.*\n"
        + "sun\\/.*\n";

    private static final String classPath = "bin/";

    private static final TypeReference[] ignoreExceptions = {
            TypeReference.JavaLangOutOfMemoryError,
            TypeReference.JavaLangExceptionInInitializerError,
            TypeReference.JavaLangNegativeArraySizeException
    };

    private final Map<CGNode, FlowGraph> cg2graph = new HashMap<CGNode, FlowGraph>();
    private final Map<CGNode, ExplodedControlFlowGraph> cg2origcfg = new HashMap<CGNode, ExplodedControlFlowGraph>();

    public static final boolean DEBUG = false;

    public ExceptionPruneAnalysis(CallGraph cg, PointerAnalysis pta, AnalysisCache cache) {
    	super(cg, pta, cache);
    }

    public static ExceptionPruneAnalysis create(String methodSig) throws WalaException, IOException, IllegalArgumentException, CallGraphBuilderCancelException {
    	AnalysisScope scope = AnalysisScopeReader.makePrimordialScope(null);

        SetOfClasses exclusions = new FileOfClasses(new ByteArrayInputStream(exclusionRegExp.getBytes()));
        scope.setExclusions(exclusions);

        ClassLoaderReference loader = scope.getLoader(AnalysisScope.APPLICATION);
        AnalysisScopeReader.addClassPathToScope(classPath, scope, loader);

        // Klassenhierarchie berechnen
        ClassHierarchy cha = ClassHierarchy.make(scope);

        AnalysisCache cache = new AnalysisCache();

        MethodReference mr = StringStuff.makeMethodReference(Language.JAVA, methodSig);
        IMethod m = cha.resolveMethod(mr);
        if (m == null) {
            throw new IllegalStateException("Could not resolve " + mr);
        }

        Set<Entrypoint> entries = HashSetFactory.make();
        entries.add(new DefaultEntrypoint(m, cha));
        AnalysisOptions options = new AnalysisOptions();
        options.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
        options.setEntrypoints(entries);
        CallGraphBuilder builder = Util.makeZeroOneContainerCFABuilder(options, cache, cha, scope);
        CallGraph cg = builder.makeCallGraph(options, null);

        PointerAnalysis pta = builder.getPointerAnalysis();

        ExceptionPruneAnalysis mCFG = new ExceptionPruneAnalysis(cg, pta, cache);

        return mCFG;
    }

    public static ExceptionPruneAnalysis createBarcodeTest() throws WalaException, IOException, IllegalArgumentException, CallGraphBuilderCancelException {
    	com.ibm.wala.ipa.callgraph.impl.Util.setNativeSpec("test_lib/natives_empty.xml");

    	AnalysisScope scope = AnalysisScopeReader.makePrimordialScope(null);


        SetOfClasses exclusions = new FileOfClasses(new ByteArrayInputStream("com/sun/.*".getBytes()));
        scope.setExclusions(exclusions);

        ClassLoaderReference primordial = scope.getLoader(AnalysisScope.PRIMORDIAL);
        AnalysisScopeReader.addClassPathToScope("test_lib/jSDG-stubs-j2me2.0.jar", scope, primordial);
        AnalysisScopeReader.addClassPathToScope("test_lib/primordial.jar", scope, primordial);
        ClassLoaderReference loader = scope.getLoader(AnalysisScope.APPLICATION);
        AnalysisScopeReader.addClassPathToScope(classPath, scope, loader);

        // Klassenhierarchie berechnen
        System.out.println("Creating class hierarchy...");
        ClassHierarchy cha = ClassHierarchy.make(scope);
        System.out.println("Done.");

        AnalysisCache cache = new AnalysisCache();

        Iterable<Entrypoint> entries = Util.makeMainEntrypoints(scope, cha, "LMainEmulator");
//        MethodReference mr = StringStuff.makeMethodReference(Language.JAVA, methodSig);
//        IMethod m = cha.resolveMethod(mr);
        if (entries == null || !entries.iterator().hasNext()) {
            throw new IllegalStateException("Could not find main method in MainEmulator");
        }

//        Set<Entrypoint> entries = HashSetFactory.make();
//        entries.add(new DefaultEntrypoint(m, cha));
        AnalysisOptions options = new AnalysisOptions();
        options.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
        options.setEntrypoints(entries);
        CallGraphBuilder builder = Util.makeZeroCFABuilder(options, cache, cha, scope);

        System.out.println("Creating call graph...");
        CallGraph cg = builder.makeCallGraph(options, null);
        System.out.println("Done.");

        PointerAnalysis pta = builder.getPointerAnalysis();

        ExceptionPruneAnalysis mCFG = new ExceptionPruneAnalysis(cg, pta, cache);

        return mCFG;
    }

    public static void main(String[] args) throws IllegalArgumentException, CallGraphBuilderCancelException, IOException, UnsoundGraphException, WalaException  {
        final String METHOD = "edu.kit.ipd.wala.tests.A.foo()V";
        //final String METHOD = "edu.kit.ipd.wala.method.ExceptionPruneAnalysis.foo()V";

        //ExceptionPruneAnalysis mCFG = ExceptionPruneAnalysis.createBarcodeTest();
        ExceptionPruneAnalysis mCFG = create(METHOD);
        System.out.println("Starting mcfg");
        mCFG.run();
    }

    @SuppressWarnings("unused")
	public void run() throws UnsoundGraphException {
    	Set<CGNode> allCalled = findAllCalledMethods(cg.getEntrypointNodes());

    	for (CGNode method : allCalled) {
    		IMethod im = method.getMethod();
    		if (!im.isNative() && !im.isAbstract() && !im.isSynthetic()) {
		        ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> cfg = getPruned(method, null);
    		}
    	}
    }

    public Set<CGNode> findAllCalledMethods(Collection<CGNode> start) {
    	Deque<CGNode> worklist = new LinkedList<CGNode>(start);
    	Set<CGNode> visited = new HashSet<CGNode>();

    	while (!worklist.isEmpty()) {
    		CGNode node = worklist.removeFirst();
    		if (!visited.contains(node)) {
    			visited.add(node);
    			for (Iterator<CGNode> it = cg.getSuccNodes(node); it.hasNext();) {
    				CGNode succ = it.next();
    				if (!visited.contains(succ)) {
    					worklist.add(succ);
    				}
    			}
    		}
    	}

    	return visited;
    }

	/* (non-Javadoc)
	 * @see edu.kit.ipd.wala.ExceptionPrunedCFG#getPruned(com.ibm.wala.ipa.callgraph.CGNode)
	 */
	@Override
	public ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> getPruned(CGNode method, IProgressMonitor progress)
	throws UnsoundGraphException {
		FlowGraph graph = cg2graph.get(method);
		ExplodedControlFlowGraph origCfg = cg2origcfg.get(method);

		if (graph == null) {
			if (DEBUG) {
		        String fileName = method.getMethod().getSignature();
		        fileName = fileName.replace('/', '.');
		        System.out.println("--- Starting " + fileName + " ---");

		        MethodCFG mCFG = new MethodCFG(method, ignoreExceptions);
				if (mCFG.init(cache)) {
                    mCFG.write(mCFG.getOriginalGraph(), "out/" + fileName + ".dot");
                    System.out.println("Exceptions in original graph: " + mCFG.getOriginalGraph().countExceptions());

                    mCFG.purgeExceptions();

                    graph = mCFG.getGraph();
                    mCFG.write(graph, "out/" + fileName + ".filter.dot");
			        System.out.println("Exceptions in final graph: " + graph.countExceptions());

			        cg2graph.put(method, graph);
			        origCfg = mCFG.getECFG();
			        cg2origcfg.put(method, origCfg);
				}
			} else {
			    MethodCFG mCFG = new MethodCFG(method, ignoreExceptions);
				if (mCFG.init(cache)) {
					mCFG.purgeExceptions();
					graph = mCFG.getGraph();
			        cg2graph.put(method, graph);
			        origCfg = mCFG.getECFG();
			        cg2origcfg.put(method, origCfg);
				}
			}

			printDetails(method, origCfg, graph);
		}

		if (graph != null) {
			IgnoreEdgeListFilter filter =
				new IgnoreEdgeListFilter(origCfg, graph);
			PrunedCFG<SSAInstruction, IExplodedBasicBlock> pCFG = PrunedCFG.make(origCfg, filter);

			return pCFG;
		} else {
			return null;
		}
	}

	private static void printDetails(CGNode method, ExplodedControlFlowGraph cfg, FlowGraph pruned) {
		int deletedEdges = 0;
		int originalEdges = 0;
		for (IExplodedBasicBlock node : cfg) {
			originalEdges += cfg.getSuccNodeCount(node);

			Iterator<IExplodedBasicBlock> it = cfg.getSuccNodes(node);

			int srcNum = node.getNumber();
			while (it.hasNext()) {
				IExplodedBasicBlock dst = it.next();
				int dstNum = dst.getNumber();
				if (pruned.hasEdge(srcNum, dstNum)) {
					deletedEdges++;
				}
			}
		}

		DecimalFormat df = new DecimalFormat("00.00");

		double percent = ((double) deletedEdges / (double) originalEdges) * 100.0;
		Log.info("EXC: " + df.format(percent) + "% - edges: " + originalEdges + " deleted: " + deletedEdges
				+ " - " + edu.kit.joana.deprecated.jsdg.util.Util.methodName(method.getMethod()));
	}

	private final static class IgnoreEdgeListFilter implements EdgeFilter<IExplodedBasicBlock> {
		@SuppressWarnings("unused")
		private final ExplodedControlFlowGraph originalGraph;
		private final FlowGraph finalGraph;

		public IgnoreEdgeListFilter(ExplodedControlFlowGraph orig, FlowGraph deleted) {
			this.originalGraph = orig;
			this.finalGraph = deleted;
		}

		public boolean hasExceptionalEdge(IExplodedBasicBlock src, IExplodedBasicBlock dst) {
		    return finalGraph.hasEdge(src.getNumber(), dst.getNumber());
		}

		public boolean hasNormalEdge(IExplodedBasicBlock src, IExplodedBasicBlock dst) {
            return finalGraph.hasEdge(src.getNumber(), dst.getNumber());
		}

	}

	/* (non-Javadoc)
	 * @see edu.kit.ipd.wala.ExceptionPrunedCFG#getOriginal(com.ibm.wala.ipa.callgraph.CGNode)
	 */
	@Override
	public ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> getOriginal(CGNode method) throws UnsoundGraphException {
		ExplodedControlFlowGraph origCfg = cg2origcfg.get(method);

		if (origCfg == null) {
	        MethodCFG mCFG = new MethodCFG(method, ignoreExceptions);
			if (mCFG.init(cache)) {
		        origCfg = mCFG.getECFG();
		        cg2origcfg.put(method, origCfg);
			}
		}

		return origCfg;
	}

}
