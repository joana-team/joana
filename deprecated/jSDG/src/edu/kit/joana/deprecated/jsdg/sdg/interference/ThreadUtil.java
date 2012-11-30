/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.interference;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;

import edu.kit.joana.deprecated.jsdg.util.Log;

/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ThreadUtil {

	public static Map<CGNode, Set<CGNode>> findThreads(CallGraph cg, IProgressMonitor progress) throws CancelException {
		Map<CGNode, Set<CGNode>> start2thread = new HashMap<CGNode, Set<CGNode>>();

		final IMethod threadStart = findThreadStart(cg);

		Set<CGNode> entryPoints = getListOfAllThreadEntryPoints(cg, threadStart);

		for (CGNode threadRun : entryPoints) {
			IntSet transitiveCalled = getTransitiveCallsFromSameThread(cg, threadRun, threadStart);

			Set<CGNode> sameThread = new HashSet<CGNode>();
			for (IntIterator it = transitiveCalled.intIterator(); it.hasNext(); ) {
				int cgnodeId = it.next();
				CGNode node = cg.getNode(cgnodeId);
				sameThread.add(node);
			}

			start2thread.put(threadRun, sameThread);

			if (progress.isCanceled()) {
				throw CancelException.make("Computing thread ids canceled.");
			}

			progress.worked(1);
		}

		return start2thread;
	}

	private static Set<CGNode> getListOfAllThreadEntryPoints(CallGraph cg, IMethod threadStart) {
		//TODO make it more precise! The possible targets of Thread.start are
		// somehow very conservative. We only want those whose start() has been
		// triggered.
		Set<CGNode> entryPoints = HashSetFactory.make();

		/* when no thread start method is in the sdg it simply means that thread.start
		 * is never called -> so no threads except the main thread exists
		 *
		 * We search for all successors of the method threadStart (Thread.start())
		 * in the callgraph. These are the possible entrypoints of the controlflow
		 * of a new thread.
		 */
		if (threadStart != null) {
			Set<CGNode> threadStarts = HashSetFactory.make();
			Set<CGNode> startNodes = cg.getNodes(threadStart.getReference());
			for (CGNode start : startNodes) {
				if (start.getMethod().equals(threadStart)) {
					threadStarts.add(start);
				}
			}

			for (CGNode cgThread : threadStarts) {
				// what has been called by thread.start
				for (Iterator<? extends CGNode> it = cg.getSuccNodes(cgThread); it.hasNext();) {
					CGNode callee = it.next();
					entryPoints.add(callee);
				}
			}
		}

		// add main to runnables - this thread is always there:
		// fakerootnode contains call to main and to all clinits -> so we do not have to treat
		// clinits seperately
		CGNode cgMain = cg.getFakeRootNode();

		entryPoints.add(cgMain);

		return entryPoints;
	}

	private static IMethod findThreadStart(CallGraph cg) {
		//first we search for the thread.start method in our sdg
		IMethod thread = null;

		TypeReference tRefThread =
			TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/Thread");

		IClass threadClass = cg.getClassHierarchy().lookupClass(tRefThread);
		if (threadClass != null) {
			for (IMethod method : threadClass.getDeclaredMethods()) {
				if (isThreadStart(method)) {
					if (thread == null) {
						thread = method;
					} else {
						Log.error("Found another method implementing Thread.start() - this is weird.");
						Log.error("Version 1: " + thread);
						Log.error("Version 2: " + method);
					}
				}
			}
		} else {
			Log.info("No java.lang.Thread class has been found.");
		}

		if (thread == null) {
			Log.info("No reference to Thread.start() has been found in the analyzed program.");
		} else {
			Log.info("Thread.start() has been found. Starting interference analysis.");
		}

		return thread;
	}

	/**
	 * Computes a set of all pdgs whose method may be called subsequently by the method
	 * provided as threadRun. It stops traversation when a new thread is created.
	 * The result is stored in an int set where each entry is
	 * the pdg-id of a pdg that may be called.
	 * @param threadRun
	 * @return intset of pdg ids
	 */
	private static IntSet getTransitiveCallsFromSameThread(CallGraph cg, CGNode threadRun, IMethod threadStart) {
		MutableIntSet called = new BitVectorIntSet();

		called.add(cg.getNumber(threadRun));

		searchCalleesSameThread(cg, threadRun, called, threadStart);

		return called;
	}

	private static void searchCalleesSameThread(CallGraph cg, CGNode caller, MutableIntSet called, IMethod threadStart) {
		for (Iterator<? extends CGNode> it = cg.getSuccNodes(caller); it.hasNext();) {
			CGNode node = it.next();
			int pdgCurId = cg.getNumber(node);

			if (!called.contains(pdgCurId)) {
				called.add(pdgCurId);
				// only look further if no new thread has been created
				if (node.getMethod() != threadStart) {
					searchCalleesSameThread(cg, node, called, threadStart);
				}
			}
		}
	}

	public static boolean isThreadStart(IMethod method) {
		return "java.lang.Thread.start()V".equals(method.getSignature());
	}

}
