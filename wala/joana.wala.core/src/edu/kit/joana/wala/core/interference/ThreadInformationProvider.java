/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.

 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.interference;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.strings.Atom;

import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.SDGBuilder;
import gnu.trove.set.TIntSet;

/**
 * Generic methods which could maybe integrated into SDGBuilder.
 * @author Juergen Graf, Martin Mohr

 */
final class Utility {

	public static final Set<CGNode> getCGNodesForMethod(SDGBuilder builder, IMethod m) {
		Set<CGNode> ret = new HashSet<CGNode>();
		for (PDG pdg : builder.getAllPDGs()) {
			CGNode cgNode = pdg.cgNode;
			if (m.equals(cgNode.getMethod())) {
				ret.add(cgNode);
			}
		}
		return ret;
	}

}

/**
 * Locates all occurences of the Thread.start() method.
 * @author Juergen Graf, Martin Mohr

 */
class ThreadStartLocator {

	private boolean searchForThreadStartsDone = false;
	private final Set<CGNode> threadStarts;
	private final SDGBuilder builder;

	ThreadStartLocator(SDGBuilder builder) {
		this.builder = builder;
		this.threadStarts = new HashSet<CGNode>();
	}

	private final void findThreadStartsIfNotDoneYet() {
		if (!this.searchForThreadStartsDone) {
			//first we search for the thread.start method in our sdg
			for (PDG methPDG : builder.getAllPDGs()) {
				CGNode cgNode = methPDG.cgNode;
				IMethod m = cgNode.getMethod();
				if (isThreadStart(m)) {
					threadStarts.add(cgNode);
				}
			}
			this.searchForThreadStartsDone = true;
		}
	}

	public final boolean threadStartExists() {
		findThreadStartsIfNotDoneYet();
		return !this.threadStarts.isEmpty();
	}

	public final IMethod getThreadStartMethod() {
		findThreadStartsIfNotDoneYet();
		return threadStarts.iterator().next().getMethod();
	}

	final Set<CGNode> getThreadStartsInCallGraph() {
		if (!threadStartExists()) {
			return HashSetFactory.make();
		} else {
			return new HashSet<CGNode>(threadStarts);
		}
	}



	public static boolean isCallToTarget(SDGBuilder builder, PDG pdg, PDGNode call, MethodReference target) {
		final SSAInvokeInstruction invk = (SSAInvokeInstruction) pdg.getInstruction(call);
		/**
		 * Only check calls of "start" methods - apart from "Thread.start()", this can also be
		 * any start() method of a sub class of Thread. Thus, checking for "Thread.start()" only
		 * would be not sufficient.
		 * Why do we check this? Because SDGBuilder.getPossibleTargets() is relatively slow and we do not want to
		 * call it very often.
		 */
		boolean rightTargetFound = false;
		if (invk.getDeclaredTarget().getName().equals(target.getName())) {
			/**
			 * Check if one of the possible call targets is Thread.start()
			 */
			for (PDG callee : builder.getPossibleTargets(call)) {
				if (callee.getMethod().getReference().equals(target)) {
					rightTargetFound = true;
				}
			}
		}

		return rightTargetFound;
	}

	private static boolean isThreadStart(IMethod method) {
		return method.getReference().equals(ThreadInformationProvider.JavaLangThreadStart);
	}
}

/**
 * Collects all the locations of thread entry points in the callgraph.
 * @author Juergen Graf, Martin Mohr

 */
class ThreadEntryLocator {

	private static final Logger debug = Log.getLogger(Log.L_WALA_INTERFERENCE_DEBUG);

	private final SDGBuilder builder;
	private final ThreadStartLocator threadStartLocator;
	private final Set<CGNode> threadEntries;
	private boolean computationDone = false;

	ThreadEntryLocator(SDGBuilder builder, ThreadStartLocator threadStartLocator) {
		this.builder = builder;
		this.threadStartLocator = threadStartLocator;
		this.threadEntries = new HashSet<CGNode>();
	}

	final Set<CGNode> getAllThreadEntryNodesInCallGraph() {
		if (!computationDone) {
			computeAllThreadEntriesNodesInCallGraph();
		}

		return threadEntries;
	}

	private final void computeAllThreadEntriesNodesInCallGraph() {
		//TODO make it more precise! The possible targets of Thread.start are
		// somehow very conservative. We only want those whose start() has been
		// triggered.
		if (!threadStartLocator.threadStartExists()) {
			/**
			 * If there is not Thread.start() method, then there are not thread entries to be computed.
			 */
			return;
		} else {

			/* when no thread start method is in the sdg it simply means that thread.start
			 * is never called -> so no threads except the main thread exists
			 *
			 * We search for all successors of the method threadStart (Thread.start())
			 * in the callgraph. These are the possible entrypoints of the controlflow
			 * of a new thread.
			 */

			Set<CGNode> threadStarts = threadStartLocator.getThreadStartsInCallGraph();
			for (CGNode cgThread : threadStarts) {
				// what has been called by thread.start
				for (Iterator<? extends CGNode> it = getCallGraph().getSuccNodes(cgThread); it.hasNext();) {
					CGNode callee = it.next();
					if (overwritesThreadRun(getCallGraph(), callee)) {
						threadEntries.add(callee);
					} else {
						debug.outln("Skipping call from Thread.start to " + callee);
					}
				}
			}

			// add main to runnables - this thread is always there:
			if (builder.getEntry() == null) {
				threadEntries.add(getCallGraph().getFakeRootNode());
			} else {
				Set<CGNode> cgMain = Utility.getCGNodesForMethod(builder, builder.getEntry());

				assert (cgMain != null);
				assert (cgMain.size() == 1) : "More then one main method in callgraph - this is weird!: " + cgMain;

				threadEntries.addAll(cgMain);
			}
		}
	}

	public static boolean overwritesThreadRun(CallGraph callGraph, CGNode node) {
		IMethod method = node.getMethod();
		Selector sel = method.getSelector();

		if (ThreadInformationProvider.JavaLangThreadRun.getSelector().equals(sel)) {
			IClassHierarchy cha = callGraph.getClassHierarchy();
			IClass javaLangThread = cha.lookupClass(TypeReference.JavaLangThread);
			IClass klass = method.getDeclaringClass();
			return cha.isSubclassOf(klass, javaLangThread);
		} else {
			return false;
		}
	}

	private CallGraph getCallGraph() {
		return builder.getNonPrunedWalaCallGraph();
	}
}

/**
 * Provides information about threads occuring in the analyzed program.
 * @author Martin Mohr

 */
public class ThreadInformationProvider {

	public static final Atom START = Atom.findOrCreateUnicodeAtom("start");
	private static final Atom RUN = Atom.findOrCreateUnicodeAtom("run");
	public static final Atom JOIN = Atom.findOrCreateUnicodeAtom("join");


	public static final MethodReference JavaLangThreadStart =
		MethodReference.findOrCreate(TypeReference.JavaLangThread, START,
				Descriptor.findOrCreateUTF8(Language.JAVA, "()V"));
	public static final MethodReference JavaLangThreadJoin =
			MethodReference.findOrCreate(TypeReference.JavaLangThread, JOIN,
					Descriptor.findOrCreateUTF8(Language.JAVA, "()V"));
	public static final MethodReference JavaLangThreadRun =
			MethodReference.findOrCreate(TypeReference.JavaLangThread, RUN,
					Descriptor.findOrCreateUTF8(Language.JAVA, "()V"));



	private final static TypeName JavaLangRunnableName = TypeName.string2TypeName("Ljava/lang/Runnable");
	public static final TypeReference JavaLangRunnable =
		TypeReference.findOrCreate(ClassLoaderReference.Primordial, JavaLangRunnableName);
	public static final MethodReference JavaLangRunnableRun =
		MethodReference.findOrCreate(JavaLangRunnable, RUN, Descriptor.findOrCreateUTF8(Language.JAVA, "()V"));

	/** locates the Thread.start() method */
	private final ThreadStartLocator threadStartLocator;

	/** collects all locations of thread entry points in the call graph */
	private final ThreadEntryLocator threadEntryLocator;

	private final ThreadAllocationSiteFinder threadAllocationSiteFinder;


	public ThreadInformationProvider(SDGBuilder builder) {
		this.threadStartLocator = new ThreadStartLocator(builder);
		this.threadEntryLocator = new ThreadEntryLocator(builder, threadStartLocator);
		this.threadAllocationSiteFinder = new ThreadAllocationSiteFinder(builder);
	}

	public boolean hasThreadStartMethod() {
		return threadStartLocator.threadStartExists();
	}

	public IMethod getThreadStartMethod() {
		return threadStartLocator.getThreadStartMethod();
	}

	public Set<CGNode> getAllThreadStartNodesInCallGraph() {
		return threadStartLocator.getThreadStartsInCallGraph();
	}

	public Set<CGNode> getAllThreadEntryNodesInCallGraph() {
		return threadEntryLocator.getAllThreadEntryNodesInCallGraph();
	}

	public Map<PDGNode, TIntSet> getAllocationSitesForThreadStartCalls() {
		return threadAllocationSiteFinder.getAllocationSites();
	}

	public boolean isCallOfThreadRunOverriding(PDG callingCtx, PDGNode call) {
		return threadAllocationSiteFinder.callOfThreadRunOverriding(callingCtx, call);
	}
}
