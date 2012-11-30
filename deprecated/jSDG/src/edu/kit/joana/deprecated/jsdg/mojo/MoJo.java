/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.mojo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.PartialCallGraph;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.traverse.DFS;
import com.ibm.wala.util.graph.traverse.SCCIterator;
import com.ibm.wala.util.strings.StringStuff;

import edu.kit.joana.deprecated.jsdg.Analyzer;
import edu.kit.joana.deprecated.jsdg.Messages;
import edu.kit.joana.deprecated.jsdg.SDGFactory;
import edu.kit.joana.deprecated.jsdg.SDGFactory.Config;
import edu.kit.joana.deprecated.jsdg.mojo.interfac.GraphNode;
import edu.kit.joana.deprecated.jsdg.mojo.interfac.ParameterGraph;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.deprecated.jsdg.util.Debug;
import edu.kit.joana.deprecated.jsdg.util.ExtendedNodeDecorator;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.deprecated.jsdg.util.Util;
import edu.kit.joana.deprecated.jsdg.wala.viz.DotUtil;
import edu.kit.joana.wala.util.VerboseProgressMonitor;
import edu.kit.joana.wala.util.WatchDog;

/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public final class MoJo {

	private MoJo() {
	}

	public static void main(String[] args) throws IOException, IllegalArgumentException, CancelException, PDGFormatException, WalaException, InvalidClassFileException {
		WatchDog watchdog = null;

		// try catch block added due to problems with eclipse on mac osx
		// (exceptions got lost - program simply terminated)
		try {
			final IProgressMonitor progress = new VerboseProgressMonitor(System.out);

			String file = null;
			Integer timeout = null;
			boolean lazy = false;
			boolean verify = true;

			if (args != null && args.length > 0) {
				for (int i = 0; i < args.length; i++) {
					if (args[i].equals("-cfg")) {
						if (args.length > i + 1) {
							file = args[i+1];
							i++;
						} else {
							System.err.println("No config file provided as argument.");
						}
					} else if (args[i].equals("-timeout")) {
						if (args.length > i + 1) {
							try {
								timeout = Integer.parseInt(args[i+1]);
								i++;
							} catch (NumberFormatException nf) {
								System.err.println("Timeout value is no number - timeout not enabled: " + nf.getMessage());
								timeout = null;
							}
						} else {
							timeout = null;
							System.err.println("No timeout value provided - timeout not enabled.");
						}
					} else if (args[i].equals("-verify")) {
						verify = true;
					} else if (args[i].equals("-lazy")) {
						lazy = true;
					} else if (args[i].equals("-help")) {
						System.out.println("Usage: progname [-cfg <configfile>] [-timeout <minutes>] [-lazy] [-help]");
						return;
					}
				}
			}

			if (file != null) {
				System.out.println("Using config from file: " + file);
				Analyzer.cfg = SDGFactory.Config.readFrom(new FileInputStream(file));
			} else {
				System.out.println("Usage: progname [-cfg <configfile>] [-timeout <minutes>] [-lazy] [-help]");
				System.out.println("No configuration file selected - aborting...");
				return;
			}
			System.out.println(Analyzer.cfg.toString());
			System.out.println(Debug.getSettings());

			if (timeout != null) {
				System.out.println("TIMEOUT set to " + timeout + " minutes.");

				final long timeoutInMs = ((long) timeout) * 60L * 1000L;
				// give program 1 minute to exit when progress.cancel() was triggered
				final long timeToCleanup = 60L * 60L * 1000L;

				watchdog = new WatchDog(progress, timeoutInMs, timeToCleanup);
				watchdog.start();
			}

			computeWorstCase(Analyzer.cfg, progress);

			if (watchdog != null && watchdog.isAlive()) {
				// tell watchdog to stop
				System.out.println("Shutting down watchdog.");
				watchdog.done();
				watchdog.interrupt();
			}

		} finally {
			if (watchdog != null && watchdog.isAlive()) {
				// tell watchdog to stop
				System.out.println("Shutting down watchdog.");
				watchdog.done();
				watchdog.interrupt();
			}

			Date date = new Date();
			Log.info("Stopped Analysis at " + date);
		}
	}

	/**
	 * Computes PDG with maximal alias context.
	 * TODO: Disable effects of static fields and initializers.
	 * @param cfg
	 * @param progress
	 * @throws IOException
	 * @throws ClassHierarchyException
	 * @throws CallGraphBuilderCancelException
	 * @throws IllegalArgumentException
	 * @throws InvalidClassFileException
	 */
	private static void computeWorstCase(Config cfg, IProgressMonitor progress)
	throws IOException, ClassHierarchyException, IllegalArgumentException, CallGraphBuilderCancelException, InvalidClassFileException {
		progress.beginTask(Messages.getString("Analyzer.Task_Prepare_IR"), -1); //$NON-NLS-1$

		com.ibm.wala.ipa.callgraph.impl.Util.setNativeSpec(cfg.nativesXML);

		progress.subTask(Messages.getString("Analyzer.SubTask_Analysis_Scope")); //$NON-NLS-1$

		ClassLoader loader = MoJo.class.getClassLoader();
		AnalysisScope scope = Util.makeAnalysisScope(cfg, loader);
			//AnalysisScopeReader.makeJavaBinaryAnalysisScope(cfg.scopeFile, cfg.classpath, null);
		progress.done();

		ClassHierarchy cha = ClassHierarchy.make(scope, progress);

		Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, cfg.mainClass);
		AnalysisOptions options = new AnalysisOptions(scope, entrypoints);
	    AnalysisCache cache = new AnalysisCache();

	    progress.subTask(Messages.getString("Analyzer.SubTask_Call_Graph_Builder") + cfg.pointsTo); //$NON-NLS-1$
		SSAPropagationCallGraphBuilder builder = com.ibm.wala.ipa.callgraph.impl.Util.makeZeroCFABuilder(options, cache, cha, scope);

		progress.done();

		progress.subTask(Messages.getString("Analyzer.SubTask_Call_Graph")); //$NON-NLS-1$
		CallGraph cg = builder.makeCallGraph(options, progress);
		progress.done();

		// dump callgraph. Find sccs.
		dumpMoJoCallGraph(cg, "MoJo", progress);

		// check for strongly connected components aka recursive calls
		SCCIterator<CGNode> sccs = new SCCIterator<CGNode>(cg);
		int sccCount = 0;
		int sccNonTrivial = 0;
		while (sccs.hasNext()) {
			sccCount++;
			Set<CGNode> scc = sccs.next();
			if (scc.size() > 1) {
				sccNonTrivial++;
				System.out.println("SCC no. " + sccCount + " size: " + scc.size());
				for (CGNode node : scc) {
					System.out.println("\t" + node.getGraphNodeId() + "\t" + Util.methodName(node.getMethod()));
				}
			}
		}
		System.out.println(((double) sccNonTrivial / (double) sccCount * 100.00) + "% of SCCs are non trivial (>1 element)");

		// Show only nodes reachable from main - no initializer and static stuff.
		// ignore Object.init
		Collection<CGNode> roots =
			cg.getEntrypointNodes(); // no static initializers
			//new HashSet<CGNode>(1); roots.add(cg.getFakeRootNode()); // with static initializers
		Set<CGNode> reachable = DFS.getReachableNodes(cg, roots);
		MethodReference objectInit = StringStuff.makeMethodReference(Language.JAVA, "java/lang/Object.<init>()V");
		reachable.removeAll(cg.getNodes(objectInit));
		MethodReference stringLength = StringStuff.makeMethodReference(Language.JAVA, "java/lang/String.length()I");
		reachable.removeAll(cg.getNodes(stringLength));
		MethodReference stringEquals = StringStuff.makeMethodReference(Language.JAVA, "java/lang/String.equals(Ljava/lang/Object;)Z");
		reachable.removeAll(cg.getNodes(stringEquals));
		MethodReference stringBufferAppend = StringStuff.makeMethodReference(Language.JAVA, "java/lang/StringBuffer.append(Ljava/lang/String;)Ljava/lang/StringBuffer;");
		reachable.removeAll(cg.getNodes(stringBufferAppend));
		MethodReference stringBufferInit = StringStuff.makeMethodReference(Language.JAVA, "java/lang/StringBuffer.<init>(Ljava/lang/String;)V");
		reachable.removeAll(cg.getNodes(stringBufferInit));
		MethodReference stringBufferToString = StringStuff.makeMethodReference(Language.JAVA, "java/lang/StringBuffer.toString()Ljava/lang/String;");
		reachable.removeAll(cg.getNodes(stringBufferToString));
		MethodReference systemArraycopy = StringStuff.makeMethodReference(Language.JAVA, "java/lang/System.arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V");
		reachable.removeAll(cg.getNodes(systemArraycopy));

		// prune exception inits just for fun
		IClass throwable = cha.lookupClass(TypeReference.JavaLangThrowable);
		Set<CGNode> toRemove = new HashSet<CGNode>();
		for (CGNode node : reachable) {
			IMethod im = node.getMethod();
			if (im.isInit()) {
				IClass cls = im.getDeclaringClass();
				if (cha.isSubclassOf(cls, throwable)) {
					toRemove.add(node);
				}
			}
		}
		reachable.removeAll(toRemove);

		assert roots.size() == 1;
		PartialCallGraph pCg = PartialCallGraph.make(cg, roots, reachable);
		pCg = PartialCallGraph.make(pCg, roots);
		dumpMoJoCallGraph(pCg, "MoJo-partial", progress);

		dumpPartitionGraph(cg, progress);

		System.out.println("Search 10% most called of pruned call graph:");
		dumpMostCalled(pCg);
//		System.out.println("Search most called of original cg:");
//		dumpMostCalled(cg);

		// analysis order
		System.out.println("Compute analysis order:");
		SortedSet<CGNode> postOrder = DFS.sortByDepthFirstOrder(pCg, roots.iterator().next());
		for (CGNode node : postOrder) {
			System.out.println("\t-> (" + node.getGraphNodeId() + ") " + Util.methodName(node.getMethod()));
			dumpMaximalInterface(node);
		}
	}

	/**
	 * Tries to create a graph that can be partitioned easily for multithreaded
	 * computation. We cut off main entry and leaf nodes.
	 * @param cg
	 * @param progress
	 */
	private static void dumpPartitionGraph(CallGraph cg, IProgressMonitor progress) {
		// create graph without main and cut 1 level leaves
		Collection<CGNode> roots = new HashSet<CGNode>();
		CGNode fake = cg.getFakeRootNode();
		Iterator<CGNode> entries = cg.getSuccNodes(fake);
		while (entries.hasNext()) {
			CGNode entry = entries.next();

			Iterator<CGNode> succ = cg.getSuccNodes(entry);
			while (succ.hasNext()) {
				CGNode node = succ.next();
				roots.add(node);
			}
		}

		Collection<CGNode> leaves = new HashSet<CGNode>();
		for (CGNode node : cg) {
			if (cg.getSuccNodeCount(node) == 0) {
				leaves.add(node);
			}
		}

		Set<CGNode> reachable = DFS.getReachableNodes(cg, roots);
		reachable.removeAll(leaves);

		PartialCallGraph myCg = PartialCallGraph.make(cg, roots, reachable);
		dumpMoJoCallGraph(myCg, "MoJo-partition", progress);


		Graph<CGNode> g = new TransitiveGraph<CGNode>(myCg);
		dumpMoJoCallGraph(g, "MoJo-transitive-partition", progress);
	}

	private static void dumpMaximalInterface(CGNode node) {
		System.out.print("\t\t");

		IMethod im = node.getMethod();
		for (int i = 0; i < im.getNumberOfParameters(); i++) {
			TypeReference tref = im.getParameterType(i);
			if (tref.isPrimitiveType()) {
				System.out.print(" p" + i + "(1)");
			} else {
				IClassHierarchy cha = node.getClassHierarchy();
				Set<IClass> involved = getAllClassesInvolved(tref, cha);
				int fields = countFields(involved);
				System.out.print(" p" + i + "(" + fields + ")");
			}

			computeMaxParamGraph(im, i);
		}

		System.out.println();
	}

	public static void computeMaxParamGraph(IMethod im, int paramNum) {
		GraphNode root = GraphNode.makeMethodParam(im, paramNum);
		Set<GraphNode> known = new HashSet<GraphNode>();
		known.add(root);

		ParameterGraph graph = new ParameterGraph(root);
		GraphNode current = root;
	}

	private static int countFields(Set<IClass> classes) {
		int count = 0;

		for (IClass cls : classes) {
			if (cls.isArrayClass()) {
				count += 2; // array length && array contents [?]
			} else {
				count += cls.getDeclaredInstanceFields().size();
			}
		}

		return count;
	}

	private static Set<IClass> getAllClassesInvolved(TypeReference tref, IClassHierarchy cha) {
		Set<IClass> subs = new HashSet<IClass>();

		if (tref.isPrimitiveType()) {
			return subs;
		}

		IClass cls = cha.lookupClass(tref);
//		if (cls == null) {
//			System.err.println("No class for " + tref);
//			return subs;
//		}
//
		if (cls.isInterface()) {
			// search implementations
			Set<IClass> impls = cha.getImplementors(tref);
			for (IClass impl : impls) {
				// search subclasses
				Collection<IClass> tmp = cha.computeSubClasses(impl.getReference());
				subs.addAll(tmp);
			}
		} else {
			// search subclasses
			Collection<IClass> tmp = cha.computeSubClasses(tref);
			subs.addAll(tmp);
		}

		// add super classes
		IClass cur = cls;
		while (cur.getSuperclass() != null) {
			cur = cur.getSuperclass();
			subs.add(cur);
		}

		// add classes of array elements
		Set<IClass> arrayRefs = new HashSet<IClass>();
		for (IClass involved: subs) {
			if (involved.isArrayClass()) {
				TypeReference aref = involved.getReference().getArrayElementType();
				if (aref != null) {
					Set<IClass> tmp = getAllClassesInvolved(aref, cha);
					arrayRefs.addAll(tmp);
				}
			}
		}
		subs.addAll(arrayRefs);

		return subs;
	}

	private static void dumpMostCalled(CallGraph cg) {
		// search most called methods
		SortedSet<CountCalls> mostCalled = new TreeSet<CountCalls>();
		for (CGNode node : cg) {
			int count = cg.getPredNodeCount(node);
			if (count > 1) {
				mostCalled.add(new CountCalls(node, count));
			}
		}

		int tenPercent = 1 + (mostCalled.size() / 10);
		for (CountCalls cl : mostCalled) {
			System.out.println("\t" + cl.toString());
			tenPercent--;
			if (tenPercent < 0) {
				break;
			}
		}
	}

	private final static class CountCalls implements Comparable<CountCalls> {
		private final int id;
		private final int callsTo;
		private final String name;

		private CountCalls(CGNode node, int callsTo) {
			this.callsTo = callsTo;
			this.id = node.getGraphNodeId();
			this.name = Util.methodName(node.getMethod());
		}

		public String toString() {
			return callsTo + " calls to (" + id + ") " + name;
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(CountCalls o) {
			int result = o.callsTo - callsTo;
			if (result == 0) {
				result = name.compareTo(o.name);
			}

			return result;
		}
	}

	private static void dumpMoJoCallGraph(Graph<CGNode> cg, String mainClass, IProgressMonitor monitor) {
		monitor.subTask("Dumping CallGraph...");

		String outputDir = Analyzer.cfg.outputDir;
		File cfgOut = new File(outputDir + "/");
		if (!cfgOut.exists()) {
			cfgOut.mkdir();
		}

		String dotFile = outputDir + "/" + mainClass + ".callgraph.dot";
		try {
			DotUtil.dotify(cg, cg, new CGNodeDec(), dotFile, null, Util.DOT_EXEC, monitor);
		} catch (WalaException e) {
			e.printStackTrace();
		} catch (CancelException e) {
			e.printStackTrace();
		}

		monitor.done();
	}

	private static final class CGNodeDec implements ExtendedNodeDecorator {

		/* (non-Javadoc)
		 * @see edu.kit.joana.wala.util.ExtendedNodeDecorator#getColor(java.lang.Object)
		 */
		@Override
		public String getColor(Object o) throws WalaException {
			return DEFAULT.getColor(o);
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.wala.util.ExtendedNodeDecorator#getShape(java.lang.Object)
		 */
		@Override
		public String getShape(Object o) throws WalaException {
			return DEFAULT.getShape(o);
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.viz.NodeDecorator#getLabel(java.lang.Object)
		 */
		@Override
		public String getLabel(Object o) throws WalaException {
			if (o instanceof CGNode) {
				CGNode node = (CGNode) o;
				return Util.methodName(node.getMethod());
			} else {
				return DEFAULT.getLabel(o);
			}
		}

	}

}
